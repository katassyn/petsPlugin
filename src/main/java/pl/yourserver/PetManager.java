package pl.yourserver;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
// Fixed imports to match actual package structure
// import pl.yourserver.petplugin.PetPlugin;
// import pl.yourserver.petplugin.models.Pet;
// import pl.yourserver.petplugin.models.PetType;
// import pl.yourserver.petplugin.utils.ItemBuilder;
// import pl.yourserver.petplugin.utils.TextUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetManager {

    private final PetPlugin plugin;
    private final Map<UUID, Pet> activePets;
    private final Map<UUID, List<Pet>> playerPets;
    private final LibsDisguisesHook libsDisguisesHook;

    public PetManager(PetPlugin plugin) {
        this.plugin = plugin;
        this.activePets = new ConcurrentHashMap<>();
        this.playerPets = new ConcurrentHashMap<>();
        this.libsDisguisesHook = new LibsDisguisesHook(plugin);
    }

    // Spawn peta w świecie
    public void spawnPet(Player owner, Pet pet) {
        if (pet == null || !pet.isActive()) {
            return;
        }

        // Despawn poprzedniego peta jeśli istnieje
        despawnPet(owner);

        Location spawnLoc = owner.getLocation().add(1, 0, 1);
        Entity entity;
        boolean disguised = false;

        if (isLibsDisguisesEnabled()) {
            entity = spawnWolfBase(owner, spawnLoc);
            disguised = entity != null;
        } else {
            entity = spawnLegacyPet(owner, pet, spawnLoc);
        }

        if (entity == null) {
            return;
        }

        configurePetEntity(owner, pet, entity);

        if (disguised) {
            libsDisguisesHook.applyDisguise(entity, pet);
        }
    }

    // Despawn peta
    public void despawnPet(Player owner) {
        Pet pet = activePets.get(owner.getUniqueId());
        if (pet != null && pet.getEntity() != null && !pet.getEntity().isDead()) {
            Entity entity = pet.getEntity();

            if (isLibsDisguisesEnabled() && libsDisguisesHook.isDisguised(entity)) {
                libsDisguisesHook.undisguise(entity);
            }

            entity.remove();
            pet.setEntity(null);

            // Wyczyść pozycję peta z PetFollowTask
            if (plugin.getPetFollowTask() != null) {
                plugin.getPetFollowTask().clearPetPosition(pet.getUuid());
            }
        }
        activePets.remove(owner.getUniqueId());
    }

    private Entity spawnWolfBase(Player owner, Location spawnLoc) {
        Entity entity = owner.getWorld().spawnEntity(spawnLoc, EntityType.WOLF);
        if (entity instanceof Wolf wolf) {
            wolf.setTamed(true);
            wolf.setOwner(owner);
            wolf.setSitting(false);
            wolf.setAngry(false);
            wolf.setAdult();
            wolf.setAware(true);
            wolf.setCanPickupItems(false);
            try {
                if (wolf.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
                    wolf.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(48.0);
                }
            } catch (Exception ignored) {
            }
        }
        return entity;
    }

    private Entity spawnLegacyPet(Player owner, Pet pet, Location spawnLoc) {
        EntityType entityType = pet.getType().getEntityType();

        if (entityType == EntityType.GIANT) {
            entityType = EntityType.ZOMBIE;
        }

        Entity entity = owner.getWorld().spawnEntity(spawnLoc, entityType);

        if (entity instanceof Ageable ageable) {
            ageable.setBaby();
            ageable.setAgeLock(true);
        } else if (entity instanceof Zombie zombie) {
            if (pet.getType() == PetType.ZOMBIE) {
                zombie.setBaby(true);
            }
        } else if (entity instanceof Slime slime) {
            slime.setSize(1);
        } else if (entity instanceof Phantom phantom) {
            phantom.setSize(1);
        }

        return entity;
    }

    private void configurePetEntity(Player owner, Pet pet, Entity entity) {
        entity.setMetadata("pet", new FixedMetadataValue(plugin, true));
        entity.setMetadata("petOwner", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));
        entity.setMetadata("petUUID", new FixedMetadataValue(plugin, pet.getUuid().toString()));

        String petName = TextUtil.colorize(
                pet.getRarity().getColor() + pet.getType().getDisplayName() + " &7[Lv." + pet.getLevel() + "]"
        );
        entity.setCustomName(petName);
        entity.setCustomNameVisible(true);

        if (entity instanceof LivingEntity living) {
            living.setAI(true);
            living.setInvulnerable(true);
            living.setCollidable(true);
            living.setSilent(true);
            living.setRemoveWhenFarAway(false);

            try {
                if (living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                    living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
                }
            } catch (Exception ignored) {
            }

            try {
                if (living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
                    living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(48.0);
                }
            } catch (Exception ignored) {
            }

            if (living instanceof Mob mob) {
                mob.setTarget(null);
            }

            living.setFireTicks(0);
            entity.setVisualFire(false);

            if (pet.getRarity() == PetRarity.MYTHIC) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
            }
        }

        pet.setEntity(entity);
        activePets.put(owner.getUniqueId(), pet);
    }

    private boolean isLibsDisguisesEnabled() {
        return libsDisguisesHook.canUseDisguises();
    }

    // Przełączanie aktywnego peta
    public void togglePet(Player owner, Pet pet) {
        if (pet.isActive()) {
            // Deaktywuj peta
            pet.setActive(false);
            despawnPet(owner);
        } else {
            // Sprawdź limit petów - uwzględnij sloty które da aktywowany pet
            int activeCount = getActivePetCount(owner);
            int currentMaxPets = getMaxPetSlots(owner);

            // Sprawdź ile slotów da nam ten pet
            int bonusSlots = 0;
            if (pet.getType() == PetType.ENDER_DRAGON) {
                bonusSlots = 1;
            } else if (pet.getType() == PetType.WARDEN) {
                bonusSlots = 2;
                if (pet.getLevel() >= 100) {
                    bonusSlots = 3; // +1 dodatkowy na poziomie 100
                }
            }

            // Sprawdź czy po aktywacji peta przekroczymy limit
            int maxPetsAfterActivation = currentMaxPets + bonusSlots;

            if (activeCount >= maxPetsAfterActivation) {
                owner.sendMessage(TextUtil.colorize("&cYou have reached your maximum pet limit! (" + activeCount + "/" + currentMaxPets + ")"));
                return;
            }

            // Sprawdź czy gracz nie ma już tego samego typu peta
            for (Pet activePet : getActivePets(owner)) {
                if (activePet.getType() == pet.getType()) {
                    owner.sendMessage(TextUtil.colorize("&cYou already have this type of pet active!"));
                    return;
                }
            }

            // Aktywuj peta
            pet.setActive(true);
            spawnPet(owner, pet);
        }
    }

    // Pobierz maksymalną ilość slotów na pety
    public int getMaxPetSlots(Player player) {
        int baseSlots = 1;

        // Sprawdź czy gracz ma AKTYWNE Ender Dragon lub Warden pety
        for (Pet pet : getActivePets(player)) {
            if (pet.getType() == PetType.ENDER_DRAGON) {
                baseSlots += 1; // +1 slot

                // Dodatkowy bonus na poziomie 100
                if (pet.getLevel() >= 100) {
                    // W pets.yml jest "double-duplication: true" ale nie dodatkowe sloty
                    // Pozostaw bez zmian - tylko +1 slot
                }
            } else if (pet.getType() == PetType.WARDEN) {
                baseSlots += 2; // +2 sloty

                // Dodatkowy bonus na poziomie 100
                if (pet.getLevel() >= 100) {
                    baseSlots += 1; // +1 dodatkowy slot (razem +3)
                }
            }
        }

        // Nie pozwól przekroczyć maksymalnego limitu z konfiguracji
        int configMaxSlots = plugin.getConfig().getInt("pet-slots.max-slots", 4);
        return Math.min(baseSlots, configMaxSlots);
    }

    // Pobierz ilość aktywnych petów
    public int getActivePetCount(Player player) {
        return (int) getPlayerPets(player).stream()
                .filter(Pet::isActive)
                .count();
    }

    // Pobierz aktywne pety gracza
    public List<Pet> getActivePets(Player player) {
        List<Pet> active = new ArrayList<>();
        for (Pet pet : getPlayerPets(player)) {
            if (pet.isActive()) {
                active.add(pet);
            }
        }
        return active;
    }

    // Utworz głowę peta
    private ItemStack createPetHead(Pet pet) {
        ItemStack baseHead = plugin.getHeadManager().getPetHead(pet.getType());
        return baseHead != null ? baseHead : new ItemStack(Material.PLAYER_HEAD);
    }

    // Dodaj peta do gracza
    public void addPet(Player player, Pet pet) {
        List<Pet> pets = playerPets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

        // Sprawdź czy gracz nie ma już tego typu peta
        for (Pet existingPet : pets) {
            if (existingPet.getType() == pet.getType()) {
                player.sendMessage(TextUtil.colorize("&cYou already own this type of pet!"));
                return;
            }
        }

        pets.add(pet);
        plugin.getPetDataManager().savePet(pet);
    }

    // Usuń peta
    public void removePet(Player player, Pet pet) {
        List<Pet> pets = playerPets.get(player.getUniqueId());
        if (pets != null) {
            pets.remove(pet);
            if (pet.isActive()) {
                despawnPet(player);
            }
            plugin.getPetDataManager().deletePet(pet);
        }
    }

    // Pobierz pety gracza
    public List<Pet> getPlayerPets(Player player) {
        return playerPets.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    // Ustaw pety gracza (używane przy async loading)
    public void setPlayerPets(Player player, List<Pet> pets) {
        playerPets.put(player.getUniqueId(), pets);
    }

    // Pobierz aktywnego peta
    public Pet getActivePet(Player player) {
        return activePets.get(player.getUniqueId());
    }

    // Wczytaj pety gracza (thread-safe version - nie spawnuje petów)
    public void loadPlayerPets(Player player) {
        List<Pet> pets = plugin.getPetDataManager().loadPlayerPets(player);
        playerPets.put(player.getUniqueId(), pets);
    }

    // Wczytaj i spawnuj pety gracza (tylko na main thread)
    public void loadAndSpawnPlayerPets(Player player) {
        List<Pet> pets = plugin.getPetDataManager().loadPlayerPets(player);
        playerPets.put(player.getUniqueId(), pets);

        // Spawn aktywnych petów (tylko na main thread)
        for (Pet pet : pets) {
            if (pet.isActive()) {
                spawnPet(player, pet);
            }
        }
    }

    // Zapisz pety gracza
    public void savePlayerPets(Player player) {
        List<Pet> pets = playerPets.get(player.getUniqueId());
        if (pets != null) {
            for (Pet pet : pets) {
                plugin.getPetDataManager().savePet(pet);
            }
        }
    }

    // Wyczyść pety gracza z pamięci
    public void unloadPlayerPets(Player player) {
        despawnPet(player);
        playerPets.remove(player.getUniqueId());
        activePets.remove(player.getUniqueId());
    }

    // Sprawdź czy gracz ma aktywnego peta określonego typu
    public boolean hasActivePet(Player player, PetType petType) {
        List<Pet> activePets = getActivePets(player);
        return activePets.stream().anyMatch(pet -> pet.getType() == petType);
    }
}
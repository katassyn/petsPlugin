package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PetDropManager {
    private final PetPlugin plugin;
    private final Random random;
    private final ConfigManager configManager;

    public PetDropManager(PetPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.random = new Random();
    }

    public void handleMobDeath(LivingEntity entity, Player killer) {
        if (!configManager.getConfig().getBoolean("pet-drops.enabled", true)) {
            return;
        }

        if (killer == null) {
            return;
        }

        if (shouldDropPet()) {
            PetRarity rarity = determineDropRarity();
            dropPetBlock(entity.getLocation(), rarity);
        }
    }

    private boolean shouldDropPet() {
        boolean testing = configManager.getConfig().getBoolean("pet-drops.testing", false);

        if (testing) {
            double testingChance = configManager.getConfig().getDouble("pet-drops.testing-drop-chance", 50.0);
            return random.nextDouble() * 100.0 < testingChance;
        } else {
            double baseChance = configManager.getConfig().getDouble("pet-drops.base-drop-chance", 0.1);
            return random.nextDouble() * 100.0 < baseChance;
        }
    }

    private PetRarity determineDropRarity() {
        boolean testing = configManager.getConfig().getBoolean("pet-drops.testing", false);
        boolean equalRarity = configManager.getConfig().getBoolean("pet-drops.testing-equal-rarity", true);

        if (testing && equalRarity) {
            PetRarity[] rarities = {PetRarity.MAGIC, PetRarity.EXTRAORDINARY, PetRarity.LEGENDARY, PetRarity.UNIQUE, PetRarity.MYTHIC};
            return rarities[random.nextInt(rarities.length)];
        }

        double totalWeight = 0.0;
        double magicChance = configManager.getConfig().getDouble("pet-drops.drop-chances.magic", 40.0);
        double extraordinaryChance = configManager.getConfig().getDouble("pet-drops.drop-chances.extraordinary", 30.0);
        double legendaryChance = configManager.getConfig().getDouble("pet-drops.drop-chances.legendary", 20.0);
        double uniqueChance = configManager.getConfig().getDouble("pet-drops.drop-chances.unique", 8.0);
        double mythicChance = configManager.getConfig().getDouble("pet-drops.drop-chances.mythic", 2.0);

        totalWeight = magicChance + extraordinaryChance + legendaryChance + uniqueChance + mythicChance;
        double randomValue = random.nextDouble() * totalWeight;

        if (randomValue < magicChance) {
            return PetRarity.MAGIC;
        } else if (randomValue < magicChance + extraordinaryChance) {
            return PetRarity.EXTRAORDINARY;
        } else if (randomValue < magicChance + extraordinaryChance + legendaryChance) {
            return PetRarity.LEGENDARY;
        } else if (randomValue < magicChance + extraordinaryChance + legendaryChance + uniqueChance) {
            return PetRarity.UNIQUE;
        } else {
            return PetRarity.MYTHIC;
        }
    }

    private void dropPetBlock(Location location, PetRarity rarity) {
        Material blockMaterial = getBlockMaterial(rarity);
        ItemStack petBlock = createPetBlock(blockMaterial, rarity);

        location.getWorld().dropItemNaturally(location, petBlock);
    }

    private Material getBlockMaterial(PetRarity rarity) {
        String materialName;
        switch (rarity) {
            case MAGIC:
                materialName = configManager.getConfig().getString("pet-drops.drop-blocks.magic", "LAPIS_BLOCK");
                break;
            case EXTRAORDINARY:
                materialName = configManager.getConfig().getString("pet-drops.drop-blocks.extraordinary", "AMETHYST_BLOCK");
                break;
            case LEGENDARY:
                materialName = configManager.getConfig().getString("pet-drops.drop-blocks.legendary", "DIAMOND_BLOCK");
                break;
            case UNIQUE:
                materialName = configManager.getConfig().getString("pet-drops.drop-blocks.unique", "EMERALD_BLOCK");
                break;
            case MYTHIC:
                materialName = configManager.getConfig().getString("pet-drops.drop-blocks.mythic", "REDSTONE_BLOCK");
                break;
            default:
                materialName = "LAPIS_BLOCK";
                break;
        }

        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material for " + rarity + " pet block: " + materialName);
            return Material.LAPIS_BLOCK;
        }
    }

    private ItemStack createPetBlock(Material material, PetRarity rarity) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = configManager.getConfig().getString("pet-drops.block-names." + rarity.name().toLowerCase(),
                                                                 "&9&lPet Block");
        meta.setDisplayName(TextUtil.colorize(displayName));

        List<String> lore = new ArrayList<>();
        List<String> configLore = configManager.getConfig().getStringList("pet-drops.block-lore");
        for (String line : configLore) {
            lore.add(TextUtil.colorize(line.replace("%rarity%", rarity.getDisplayName())));
        }
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(
            plugin.getNamespacedKey("pet_rarity"),
            org.bukkit.persistence.PersistentDataType.STRING,
            rarity.name()
        );

        item.setItemMeta(meta);
        return item;
    }

    public void broadcastPickupMessage(Player player, PetRarity rarity) {
        String message = configManager.getConfig().getString("pet-drops.drop-messages.pet-dropped",
                                                            "&8[&6DROP&8] &7%player% &7has dropped a %rarity% &7pet!");
        message = message.replace("%player%", player.getName())
                         .replace("%rarity%", rarity.getDisplayName());

        Bukkit.broadcastMessage(TextUtil.colorize(message));
    }

    public PetType getRandomPetOfRarity(PetRarity rarity) {
        List<PetType> petsOfRarity = new ArrayList<>();

        for (PetType petType : PetType.values()) {
            if (petType.getDefaultRarity() == rarity) {
                petsOfRarity.add(petType);
            }
        }

        if (petsOfRarity.isEmpty()) {
            return PetType.COW;
        }

        return petsOfRarity.get(random.nextInt(petsOfRarity.size()));
    }

    public PetPlugin getPlugin() {
        return plugin;
    }

    // This method is no longer used - pet blocks are handled in PetBlockListener
    public boolean handleBlockPickup(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.getNamespacedKey("pet_rarity"))) {
            return false;
        }

        String rarityName = meta.getPersistentDataContainer().get(
            plugin.getNamespacedKey("pet_rarity"),
            org.bukkit.persistence.PersistentDataType.STRING
        );

        try {
            PetRarity rarity = PetRarity.valueOf(rarityName);
            PetType petType = getRandomPetOfRarity(rarity);

            Pet pet = new Pet(player.getUniqueId(), petType, rarity);
            plugin.getPetManager().addPet(player, pet);

            // Pet drop message removed - message will show when player picks up the block
            return true;

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid rarity in pet block: " + rarityName);
            return false;
        }
    }
}

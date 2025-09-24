package pl.yourserver;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.block.Action;

import java.util.UUID;

public class PhysicalPetListener implements Listener {
    private final PetPlugin plugin;

    public PhysicalPetListener(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Sprawdź czy to fizyczny pet item (głowka peta)
        if (!container.has(plugin.getNamespacedKey("pet_type"), PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);

        try {
            // Pobierz dane peta z NBT
            String petTypeStr = container.get(plugin.getNamespacedKey("pet_type"), PersistentDataType.STRING);
            String petRarityStr = container.get(plugin.getNamespacedKey("pet_rarity"), PersistentDataType.STRING);
            String petUuidStr = container.get(plugin.getNamespacedKey("pet_uuid"), PersistentDataType.STRING);
            Integer level = container.get(plugin.getNamespacedKey("pet_level"), PersistentDataType.INTEGER);
            Double experience = container.get(plugin.getNamespacedKey("pet_experience"), PersistentDataType.DOUBLE);

            PetType petType = PetType.valueOf(petTypeStr);
            PetRarity petRarity = PetRarity.valueOf(petRarityStr);
            UUID petUuid = UUID.fromString(petUuidStr);

            // Sprawdź czy gracz już ma tego peta
            for (Pet existingPet : plugin.getPetManager().getPlayerPets(player)) {
                if (existingPet.getUuid().equals(petUuid)) {
                    player.sendMessage(TextUtil.colorize("&cYou already have this pet!"));
                    return;
                }
                if (existingPet.getType() == petType) {
                    player.sendMessage(TextUtil.colorize("&cYou already own a " + petType.getDisplayName() + " pet!"));
                    return;
                }
            }

            // Nie ma limitu na ilość posiadanych petów, tylko na aktywne pety i typ peta

            // Stwórz peta i dodaj do gracza
            Pet pet = new Pet(petUuid, player.getUniqueId(), petType, petRarity, level, experience, false, System.currentTimeMillis(), 0);
            plugin.getPetManager().addPet(player, pet);

            // Usuń item z ręki
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) {
                player.getInventory().remove(item);
            }

            player.sendMessage(TextUtil.colorize("&aYou have added " + petRarity.getDisplayName() + " " + petType.getDisplayName() + "&a to your pets!"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        } catch (Exception e) {
            plugin.getLogger().warning("Error while using physical pet item: " + e.getMessage());
            player.sendMessage(TextUtil.colorize("&cError: Invalid pet item!"));
        }
    }
}
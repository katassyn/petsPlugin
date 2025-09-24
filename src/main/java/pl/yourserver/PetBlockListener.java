package pl.yourserver;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PetBlockListener implements Listener {
    private final PetDropManager petDropManager;

    public PetBlockListener(PetDropManager petDropManager) {
        this.petDropManager = petDropManager;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();

        if (isPetBlock(item)) {
            petDropManager.getPlugin().getLogger().info("Player " + player.getName() + " picked up a pet block!");

            // Show pickup message immediately
            ItemMeta meta = item.getItemMeta();
            String rarityName = meta.getPersistentDataContainer().get(
                petDropManager.getPlugin().getNamespacedKey("pet_rarity"),
                org.bukkit.persistence.PersistentDataType.STRING
            );

            try {
                PetRarity rarity = PetRarity.valueOf(rarityName);
                PetType petType = petDropManager.getRandomPetOfRarity(rarity);
                String message = petDropManager.getPlugin().getConfigManager().getConfig().getString("pet-drops.drop-messages.pet-obtained",
                                                               "&aYou obtained a %rarity% %pet%!");
                message = message.replace("%rarity%", rarity.getDisplayName())
                                 .replace("%pet%", petType.getDisplayName());
                player.sendMessage(TextUtil.colorize(message));
            } catch (IllegalArgumentException e) {
                player.sendMessage(TextUtil.colorize("&aYou obtained a pet block!"));
            }

            // Transform pet block directly to pet head in inventory
            ItemStack petHead = transformBlockToPetHead(player, item);
            if (petHead != null) {
                // Add pet head to inventory
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(petHead);
                    event.getItem().remove();
                    event.setCancelled(true);
                } else {
                    player.sendMessage(TextUtil.colorize("&cYour inventory is full! Cannot pick up pet block."));
                }
            }
        }
    }

    private boolean isPetBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(petDropManager.getPlugin().getNamespacedKey("pet_rarity"));
    }

    private ItemStack transformBlockToPetHead(Player player, ItemStack petBlock) {
        if (!isPetBlock(petBlock)) {
            return null;
        }

        ItemMeta meta = petBlock.getItemMeta();
        String rarityName = meta.getPersistentDataContainer().get(
            petDropManager.getPlugin().getNamespacedKey("pet_rarity"),
            org.bukkit.persistence.PersistentDataType.STRING
        );

        try {
            PetRarity rarity = PetRarity.valueOf(rarityName);
            PetType petType = petDropManager.getRandomPetOfRarity(rarity);

            // Create pet object for the player
            Pet pet = new Pet(player.getUniqueId(), petType, rarity);

            // Create physical pet head item
            return createPhysicalPetHead(pet, player);

        } catch (IllegalArgumentException e) {
            petDropManager.getPlugin().getLogger().warning("Invalid rarity in pet block: " + rarityName);
            return null;
        }
    }

    private ItemStack createPhysicalPetHead(Pet pet, Player player) {
        List<String> lore = new ArrayList<>();

        // Pet information
        lore.add("");
        lore.add(TextUtil.colorize("&7Level: &f" + pet.getLevel() + "/" + pet.getRarity().getMaxLevel()));
        lore.add(TextUtil.colorize("&7Experience: &f" + String.format("%.1f", pet.getExperience()) + "/" + String.format("%.1f", pet.getRequiredExperience())));
        lore.add(TextUtil.colorize("&fRarity: &f&l" + pet.getRarity().name()));
        lore.add(TextUtil.colorize("&8-------------------"));

        // Pet effect
        String effect = petDropManager.getPlugin().getPetEffectManager().getPetEffectDescription(pet);
        lore.add(TextUtil.colorize("&7Effect: " + effect));

        if (pet.hasSpecialEffect()) {
            lore.add(TextUtil.colorize("&6✦ Special Effect Unlocked!"));
        }

        lore.add("");
        lore.add(TextUtil.colorize("&eRight-click to add to your pets!"));

        // Always use player heads with custom textures for all pets
        ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                .setName(TextUtil.colorize(pet.getRarity().getColor() + pet.getType().getDisplayName()))
                .setLore(lore)
                .setSkullTexture(pet.getType().getSkullTexture())
                .build();

        // Add NBT data for identification
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.getPersistentDataContainer().set(
            petDropManager.getPlugin().getNamespacedKey("pet_uuid"),
            org.bukkit.persistence.PersistentDataType.STRING,
            pet.getUuid().toString()
        );

        itemMeta.getPersistentDataContainer().set(
            petDropManager.getPlugin().getNamespacedKey("pet_type"),
            org.bukkit.persistence.PersistentDataType.STRING,
            pet.getType().name()
        );

        itemMeta.getPersistentDataContainer().set(
            petDropManager.getPlugin().getNamespacedKey("pet_rarity"),
            org.bukkit.persistence.PersistentDataType.STRING,
            pet.getRarity().name()
        );

        itemMeta.getPersistentDataContainer().set(
            petDropManager.getPlugin().getNamespacedKey("pet_level"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            pet.getLevel()
        );

        itemMeta.getPersistentDataContainer().set(
            petDropManager.getPlugin().getNamespacedKey("pet_experience"),
            org.bukkit.persistence.PersistentDataType.DOUBLE,
            pet.getExperience()
        );

        item.setItemMeta(itemMeta);

        return item;
    }

}
package pl.yourserver;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
// Fixed imports to match actual package structure

import java.util.ArrayList;
import java.util.List;

// InventoryClickListener.java
public class InventoryClickListener implements Listener {

    private final PetPlugin plugin;
    private final PetGUI petGUI;

    // Pet slots (same as in PetGUI)
    private static final int[] PET_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Row 1 (7 slots)
        19, 20, 21, 22, 23, 24, 25,  // Row 2 (7 slots)
        28, 29, 30, 31, 32, 33, 34,  // Row 3 (7 slots)
        37, 38, 39, 40, 41, 42, 43,  // Row 4 (7 slots)
        46, 47, 48, 49, 50, 51, 52   // Row 5 (7 slots) = 35 total slots
    };

    public InventoryClickListener(PetPlugin plugin) {
        this.plugin = plugin;
        this.petGUI = new PetGUI(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Sprawdź czy to nasze GUI
        if (!title.contains("Pet")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        int slot = event.getRawSlot();

        // Główne menu petów
        if (title.equals(TextUtil.colorize("&8Pet Management"))) {
            handleMainMenuClick(player, clicked, slot);
        }
        // Menu konkretnego peta
        else if (title.startsWith(TextUtil.colorize("&8Pet:"))) {
            handlePetMenuClick(player, clicked, slot, event.getClick());
        }
    }

    private void handleMainMenuClick(Player player, ItemStack clicked, int slot) {
        // Przycisk zamknięcia (nowa pozycja)
        if (slot == 53 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Kliknięcie na peta (sprawdź czy to slot peta)
        boolean isPetSlot = false;
        for (int petSlot : PET_SLOTS) {
            if (slot == petSlot) {
                isPetSlot = true;
                break;
            }
        }

        if (isPetSlot && clicked != null && clicked.getType() != Material.BLACK_STAINED_GLASS_PANE) {
            String itemName = TextUtil.stripColors(clicked.getItemMeta().getDisplayName());

            // Znajdź peta po nazwie
            Pet selectedPet = null;
            for (Pet pet : plugin.getPetManager().getPlayerPets(player)) {
                if (pet.getType().getDisplayName().equals(itemName)) {
                    selectedPet = pet;
                    break;
                }
            }

            if (selectedPet != null) {
                petGUI.openPetMenu(player, selectedPet);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    private void handlePetMenuClick(Player player, ItemStack clicked, int slot, ClickType clickType) {
        // Znajdź peta z tytułu
        String title = player.getOpenInventory().getTitle();
        String petName = TextUtil.stripColors(title.substring(title.indexOf(":") + 2));

        Pet pet = null;
        for (Pet p : plugin.getPetManager().getPlayerPets(player)) {
            if (p.getType().getDisplayName().equals(petName)) {
                pet = p;
                break;
            }
        }

        if (pet == null) {
            player.closeInventory();
            return;
        }

        // Aktywacja/Deaktywacja peta (slot 29)
        if (slot == 29) {
            plugin.getPetManager().togglePet(player, pet);
            player.playSound(player.getLocation(),
                    pet.isActive() ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.ENTITY_ITEM_BREAK,
                    1.0f, 1.0f);
            petGUI.openPetMenu(player, pet); // Odśwież menu
        }
        // Karmienie peta (slot 31)
        else if (slot == 31 && clicked.getType() == Material.SMALL_AMETHYST_BUD) {
            handlePetFeeding(player, pet);
        }
        // Release peta do inventory (slot 33)
        else if (slot == 33 && clicked.getType() == Material.FIRE_CHARGE) {
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                releasePetToInventory(player, pet);
            } else {
                player.sendMessage(TextUtil.colorize("&eShift-Click to release pet to inventory!"));
            }
        }
        // Powrót (slot 40)
        else if (slot == 40 && clicked.getType() == Material.ARROW) {
            petGUI.openMainMenu(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private void handlePetFeeding(Player player, Pet pet) {
        if (!pet.needsFeeding()) {
            player.sendMessage(TextUtil.colorize("&cThis pet doesn't need feeding right now!"));
            return;
        }

        int requiredAmount = pet.getRequiredFeedAmount();

        // Sprawdź czy gracz ma Andermant w ekwipunku
        int andermantCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SMALL_AMETHYST_BUD) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String name = item.getItemMeta().getDisplayName();
                    if (name.contains("Andermant")) {
                        andermantCount += item.getAmount();
                    }
                }
            }
        }

        // Sprawdź też w IngredientPouch jeśli dostępny
        if (plugin.getIntegrationManager() != null) {
            andermantCount += plugin.getIntegrationManager().getAndermantFromPouch(player);
        }

        if (andermantCount < requiredAmount) {
            player.sendMessage(TextUtil.colorize(
                    "&cYou need &5" + requiredAmount + "x Andermant &cto feed this pet! " +
                            "(You have: " + andermantCount + ")"
            ));
            return;
        }

        // Usuń Andermant
        int toRemove = requiredAmount;

        // Najpierw spróbuj usunąć z IngredientPouch
        if (plugin.getIntegrationManager() != null) {
            int removedFromPouch = Math.min(toRemove, plugin.getIntegrationManager().getAndermantFromPouch(player));
            if (plugin.getIntegrationManager().removeAndermantFromPouch(player, removedFromPouch)) {
                toRemove -= removedFromPouch;
            }
        }

        // Usuń pozostałe z ekwipunku
        if (toRemove > 0) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (toRemove <= 0) break;

                if (item != null && item.getType() == Material.SMALL_AMETHYST_BUD) {
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        String name = item.getItemMeta().getDisplayName();
                        if (name.contains("Andermant")) {
                            int removeFromStack = Math.min(toRemove, item.getAmount());
                            item.setAmount(item.getAmount() - removeFromStack);
                            toRemove -= removeFromStack;
                        }
                    }
                }
            }
        }

        // Nakarm peta
        pet.feed(requiredAmount);
        plugin.getPetDataManager().savePet(pet);

        player.sendMessage(TextUtil.colorize(
                plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("pet-fed")
        ));
        player.playSound(player.getLocation(),
                Sound.valueOf(plugin.getConfigManager().getSound("pet-feed")),
                1.0f, 1.0f);

        // Odśwież menu
        petGUI.openPetMenu(player, pet);
    }

    private void releasePetToInventory(Player player, Pet pet) {
        // Stwórz fizyczny item peta
        ItemStack petItem = createPhysicalPetItem(pet);

        // Sprawdź czy gracz ma miejsce w inventory
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(TextUtil.colorize("&cYour inventory is full! Cannot release pet."));
            return;
        }

        // Usuń peta z managera
        plugin.getPetManager().removePet(player, pet);

        // Dodaj item do inventory
        player.getInventory().addItem(petItem);

        player.sendMessage(TextUtil.colorize("&aYour " + pet.getRarity().getDisplayName() + " " + pet.getType().getDisplayName() + "&a has been released to your inventory!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        petGUI.openMainMenu(player);
    }

    private ItemStack createPhysicalPetItem(Pet pet) {
        List<String> lore = new ArrayList<>();

        // Informacje o pecie
        lore.add("");
        lore.add(TextUtil.colorize("&7Level: &f" + pet.getLevel() + "/" + pet.getRarity().getMaxLevel()));
        lore.add(TextUtil.colorize("&7Experience: &f" + String.format("%.1f", pet.getExperience()) + "/" + String.format("%.1f", pet.getRequiredExperience())));
        lore.add(TextUtil.colorize("&fRarity: &f&l" + pet.getRarity().name()));
        lore.add(TextUtil.colorize("&8-------------------"));

        // Efekt peta
        String effect = plugin.getPetEffectManager().getPetEffectDescription(pet);
        lore.add(TextUtil.colorize("&7Effect: " + effect));

        if (pet.hasSpecialEffect()) {
            lore.add(TextUtil.colorize("&6✦ Special Effect Unlocked!"));
        }

        lore.add("");
        lore.add(TextUtil.colorize("&eRight-click to use this pet!"));

        ItemStack baseHead = plugin.getHeadManager().getPetHead(pet.getType());
        ItemStack item = (baseHead != null ? new ItemBuilder(baseHead) : new ItemBuilder(Material.PLAYER_HEAD))
                .setName(TextUtil.colorize(pet.getRarity().getColor() + pet.getType().getDisplayName()))
                .setLore(lore)
                .build();

        // Dodaj NBT tag do identyfikacji
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.getPersistentDataContainer().set(
            plugin.getNamespacedKey("pet_uuid"),
            org.bukkit.persistence.PersistentDataType.STRING,
            pet.getUuid().toString()
        );

        itemMeta.getPersistentDataContainer().set(
            plugin.getNamespacedKey("pet_type"),
            org.bukkit.persistence.PersistentDataType.STRING,
            pet.getType().name()
        );

        itemMeta.getPersistentDataContainer().set(
            plugin.getNamespacedKey("pet_rarity"),
            org.bukkit.persistence.PersistentDataType.STRING,
            pet.getRarity().name()
        );

        itemMeta.getPersistentDataContainer().set(
            plugin.getNamespacedKey("pet_level"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            pet.getLevel()
        );

        itemMeta.getPersistentDataContainer().set(
            plugin.getNamespacedKey("pet_experience"),
            org.bukkit.persistence.PersistentDataType.DOUBLE,
            pet.getExperience()
        );

        item.setItemMeta(itemMeta);
        return item;
    }
}

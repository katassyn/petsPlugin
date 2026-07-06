package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the Teleport Cage feature - burning pets to transfer their EXP to another pet.
 */
public class TeleportCageManager {

    private final PetPlugin plugin;
    private final DecimalFormat df = new DecimalFormat("#.##");

    // Session storage for DC protection
    private final Map<UUID, TeleportCageSession> activeSessions = new ConcurrentHashMap<>();

    // GUI titles
    public static final String SACRIFICE_GUI_TITLE = "Teleport Cage - Select Pet to Burn";
    public static final String TARGET_GUI_TITLE = "Teleport Cage - Select Target Pet";
    public static final String CONFIRM_GUI_TITLE = "Teleport Cage - Confirm Transfer";

    // Pet slots (same as in PetGUI)
    private static final int[] PET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,  // Row 1 (7 slots)
            19, 20, 21, 22, 23, 24, 25,  // Row 2 (7 slots)
            28, 29, 30, 31, 32, 33, 34,  // Row 3 (7 slots)
            37, 38, 39, 40, 41, 42, 43,  // Row 4 (7 slots)
            46, 47, 48, 49, 50, 51, 52   // Row 5 (7 slots) = 35 total slots
    };

    public TeleportCageManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Inner class to store session data for DC protection.
     */
    public static class TeleportCageSession {
        private final UUID playerId;
        private Pet sacrificePet;
        private Pet targetPet;
        private boolean awaitingConfirmation;
        private final long timestamp;

        public TeleportCageSession(UUID playerId) {
            this.playerId = playerId;
            this.awaitingConfirmation = false;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public Pet getSacrificePet() {
            return sacrificePet;
        }

        public void setSacrificePet(Pet sacrificePet) {
            this.sacrificePet = sacrificePet;
        }

        public Pet getTargetPet() {
            return targetPet;
        }

        public void setTargetPet(Pet targetPet) {
            this.targetPet = targetPet;
        }

        public boolean isAwaitingConfirmation() {
            return awaitingConfirmation;
        }

        public void setAwaitingConfirmation(boolean awaitingConfirmation) {
            this.awaitingConfirmation = awaitingConfirmation;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public TeleportCageSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void createSession(Player player) {
        activeSessions.put(player.getUniqueId(), new TeleportCageSession(player.getUniqueId()));
    }

    public void clearSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    /**
     * Called when player disconnects - cleanup session.
     */
    public void handlePlayerQuit(Player player) {
        TeleportCageSession session = activeSessions.remove(player.getUniqueId());
        if (session != null && plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Cleared Teleport Cage session for disconnected player: " + player.getName());
        }
    }

    // ==================== EXP CALCULATION ====================

    /**
     * Calculates the TOTAL experience a pet has accumulated (all levels + current exp).
     */
    public double calculateTotalExp(Pet pet) {
        double totalExp = pet.getExperience(); // Current exp towards next level

        // Add exp required for all previous levels
        // Create a temporary pet to calculate required exp for each level
        for (int level = 1; level < pet.getLevel(); level++) {
            totalExp += calculateRequiredExpForLevel(level);
        }

        return totalExp;
    }

    /**
     * Calculates required exp to go from given level to level+1.
     * Uses the same formula as Pet.getRequiredExperience()
     */
    private double calculateRequiredExpForLevel(int level) {
        double baseExp = 5;
        double levelMultiplier = Math.pow(level, 2.2);
        double additionalGrowth = level * 10;
        return baseExp * levelMultiplier + additionalGrowth;
    }

    /**
     * Simulates adding exp to a pet and returns predicted level and remaining exp.
     */
    public int[] predictLevelAfterTransfer(Pet targetPet, double addedExp) {
        int currentLevel = targetPet.getLevel();
        double currentExp = targetPet.getExperience();
        int maxLevel = targetPet.getRarity().getMaxLevel();

        double totalExp = currentExp + addedExp;

        while (currentLevel < maxLevel) {
            double requiredExp = calculateRequiredExpForLevel(currentLevel);
            if (totalExp >= requiredExp) {
                totalExp -= requiredExp;
                currentLevel++;
            } else {
                break;
            }
        }

        if (currentLevel >= maxLevel) {
            totalExp = 0;
        }

        return new int[]{currentLevel, (int) totalExp};
    }

    // ==================== GUI METHODS ====================

    /**
     * Opens the first GUI - Select pet to sacrifice (burn).
     */
    public void openSacrificePetSelection(Player player) {
        createSession(player);

        Inventory gui = Bukkit.createInventory(null, 54, TextUtil.colorize("&8" + SACRIFICE_GUI_TITLE));

        // Fill background
        ItemStack background = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Info book at slot 4
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .setName(TextUtil.colorize("&6Teleport Cage"))
                .addLore("")
                .addLore(TextUtil.colorize("&7Burn a pet to transfer its"))
                .addLore(TextUtil.colorize("&7experience to another pet."))
                .addLore("")
                .addLore(TextUtil.colorize("&c&lWARNING: &cThis action is permanent!"))
                .addLore(TextUtil.colorize("&cThe burned pet will be destroyed."))
                .addLore("")
                .addLore(TextUtil.colorize("&eSelect a pet to burn below."))
                .addLore(TextUtil.colorize("&7(Active pets cannot be burned)"))
                .build();
        gui.setItem(4, infoItem);

        // Add player's pets
        List<Pet> pets = plugin.getPetManager().getPlayerPets(player);
        int slot = 0;

        for (Pet pet : pets) {
            if (slot >= PET_SLOTS.length) break;

            ItemStack petItem;
            if (pet.isActive()) {
                // Active pets shown as gray glass with warning
                petItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .setName(TextUtil.colorize("&8" + pet.getType().getDisplayName()))
                        .addLore("")
                        .addLore(TextUtil.colorize("&c&lCANNOT BURN"))
                        .addLore(TextUtil.colorize("&7This pet is currently active."))
                        .addLore(TextUtil.colorize("&7Deactivate it first in Pet Menu."))
                        .build();
            } else {
                // Inactive pets can be selected
                double totalExp = calculateTotalExp(pet);
                petItem = createSacrificePetItem(pet, totalExp);
            }

            gui.setItem(PET_SLOTS[slot], petItem);
            slot++;
        }

        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setName(TextUtil.colorize("&7Back to Pet Menu"))
                .build();
        gui.setItem(45, backItem);

        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .setName(TextUtil.colorize("&cCancel"))
                .build();
        gui.setItem(53, closeItem);

        player.openInventory(gui);
    }

    /**
     * Opens the second GUI - Select target pet to receive exp.
     */
    public void openTargetPetSelection(Player player, Pet sacrificePet) {
        TeleportCageSession session = getSession(player);
        if (session == null) {
            createSession(player);
            session = getSession(player);
        }
        session.setSacrificePet(sacrificePet);

        double totalExp = calculateTotalExp(sacrificePet);

        Inventory gui = Bukkit.createInventory(null, 54, TextUtil.colorize("&8" + TARGET_GUI_TITLE));

        // Fill background
        ItemStack background = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Info item showing sacrifice pet info
        ItemStack infoItem = new ItemBuilder(Material.CAMPFIRE)
                .setName(TextUtil.colorize("&c&lBurning: &f" + sacrificePet.getType().getDisplayName()))
                .addLore("")
                .addLore(TextUtil.colorize("&7Level: &f" + sacrificePet.getLevel()))
                .addLore(TextUtil.colorize("&7Rarity: " + sacrificePet.getRarity().getDisplayName()))
                .addLore("")
                .addLore(TextUtil.colorize("&6Total EXP to transfer: &e" + df.format(totalExp)))
                .addLore("")
                .addLore(TextUtil.colorize("&eSelect target pet below."))
                .build();
        gui.setItem(4, infoItem);

        // Add player's pets (excluding sacrifice pet)
        List<Pet> pets = plugin.getPetManager().getPlayerPets(player);
        int slot = 0;

        for (Pet pet : pets) {
            if (slot >= PET_SLOTS.length) break;

            // Skip the sacrifice pet
            if (pet.getUuid().equals(sacrificePet.getUuid())) {
                continue;
            }

            // Show pet with predicted exp after transfer
            int[] predicted = predictLevelAfterTransfer(pet, totalExp);
            ItemStack petItem = createTargetPetItem(pet, totalExp, predicted[0], predicted[1]);

            gui.setItem(PET_SLOTS[slot], petItem);
            slot++;
        }

        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setName(TextUtil.colorize("&7Back to Pet Selection"))
                .build();
        gui.setItem(45, backItem);

        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .setName(TextUtil.colorize("&cCancel"))
                .build();
        gui.setItem(53, closeItem);

        player.openInventory(gui);
    }

    /**
     * Opens the confirmation GUI.
     */
    public void openConfirmationGUI(Player player, Pet sacrificePet, Pet targetPet) {
        TeleportCageSession session = getSession(player);
        if (session == null) {
            createSession(player);
            session = getSession(player);
        }
        session.setSacrificePet(sacrificePet);
        session.setTargetPet(targetPet);
        session.setAwaitingConfirmation(false);

        double totalExp = calculateTotalExp(sacrificePet);
        int[] predicted = predictLevelAfterTransfer(targetPet, totalExp);

        Inventory gui = Bukkit.createInventory(null, 45, TextUtil.colorize("&8" + CONFIRM_GUI_TITLE));

        // Fill background
        ItemStack background = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 45; i++) {
            gui.setItem(i, background);
        }

        // Sacrifice pet info (left side)
        ItemStack sacrificeItem = plugin.getHeadManager().getPetHead(sacrificePet.getType());
        if (sacrificeItem == null) {
            sacrificeItem = new ItemStack(Material.PLAYER_HEAD);
        }
        sacrificeItem = new ItemBuilder(sacrificeItem)
                .setName(TextUtil.colorize("&c&lBURNING: " + sacrificePet.getRarity().getColor() + sacrificePet.getType().getDisplayName()))
                .addLore("")
                .addLore(TextUtil.colorize("&7Level: &f" + sacrificePet.getLevel()))
                .addLore(TextUtil.colorize("&7Rarity: " + sacrificePet.getRarity().getDisplayName()))
                .addLore("")
                .addLore(TextUtil.colorize("&6Total EXP: &e" + df.format(totalExp)))
                .addLore("")
                .addLore(TextUtil.colorize("&c&lWILL BE DESTROYED"))
                .build();
        gui.setItem(11, sacrificeItem);

        // Arrow in the middle
        ItemStack arrowItem = new ItemBuilder(Material.SPECTRAL_ARROW)
                .setName(TextUtil.colorize("&e&l>>> TRANSFER >>>"))
                .addLore("")
                .addLore(TextUtil.colorize("&7EXP will be transferred"))
                .addLore(TextUtil.colorize("&7from left to right."))
                .build();
        gui.setItem(13, arrowItem);

        // Target pet info (right side)
        ItemStack targetItem = plugin.getHeadManager().getPetHead(targetPet.getType());
        if (targetItem == null) {
            targetItem = new ItemStack(Material.PLAYER_HEAD);
        }
        targetItem = new ItemBuilder(targetItem)
                .setName(TextUtil.colorize("&a&lRECEIVING: " + targetPet.getRarity().getColor() + targetPet.getType().getDisplayName()))
                .addLore("")
                .addLore(TextUtil.colorize("&7Current Level: &f" + targetPet.getLevel()))
                .addLore(TextUtil.colorize("&7Current EXP: &f" + df.format(targetPet.getExperience())))
                .addLore("")
                .addLore(TextUtil.colorize("&a&lAFTER TRANSFER:"))
                .addLore(TextUtil.colorize("&7Level: &a" + predicted[0] + " &7(&e+" + (predicted[0] - targetPet.getLevel()) + "&7)"))
                .addLore(TextUtil.colorize("&7EXP: &a" + predicted[1]))
                .build();
        gui.setItem(15, targetItem);

        // Warning sign
        ItemStack warningItem = new ItemBuilder(Material.REDSTONE_TORCH)
                .setName(TextUtil.colorize("&c&lWARNING"))
                .addLore("")
                .addLore(TextUtil.colorize("&c&lThis action cannot be undone!"))
                .addLore(TextUtil.colorize("&7The burned pet will be permanently"))
                .addLore(TextUtil.colorize("&7destroyed. Only the EXP transfers."))
                .build();
        gui.setItem(22, warningItem);

        // Confirm button
        ItemStack confirmItem = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setName(TextUtil.colorize("&a&lCLICK TO CONFIRM"))
                .addLore("")
                .addLore(TextUtil.colorize("&7Click once to confirm."))
                .addLore(TextUtil.colorize("&7Click again to execute."))
                .build();
        gui.setItem(29, confirmItem);

        // Cancel button
        ItemStack cancelItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(TextUtil.colorize("&c&lCANCEL"))
                .addLore("")
                .addLore(TextUtil.colorize("&7Click to cancel and go back."))
                .build();
        gui.setItem(33, cancelItem);

        player.openInventory(gui);
    }

    // ==================== CLICK HANDLERS ====================

    /**
     * Handles clicks in the sacrifice pet selection GUI.
     */
    public void handleSacrificeSelectionClick(Player player, int slot, ItemStack clicked) {
        // Back button
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            clearSession(player);
            new PetGUI(plugin).openMainMenu(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Cancel button
        if (slot == 53 && clicked.getType() == Material.BARRIER) {
            clearSession(player);
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Check if it's a pet slot
        boolean isPetSlot = false;
        for (int petSlot : PET_SLOTS) {
            if (slot == petSlot) {
                isPetSlot = true;
                break;
            }
        }

        if (!isPetSlot) return;

        // Ignore gray glass (active pets)
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            player.sendMessage(TextUtil.colorize("&cThis pet is active! Deactivate it first in the Pet Menu."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Ignore background
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        // Find selected pet
        String itemName = TextUtil.stripColors(clicked.getItemMeta().getDisplayName());
        Pet selectedPet = null;
        for (Pet pet : plugin.getPetManager().getPlayerPets(player)) {
            if (pet.getType().getDisplayName().equals(itemName) && !pet.isActive()) {
                selectedPet = pet;
                break;
            }
        }

        if (selectedPet != null) {
            openTargetPetSelection(player, selectedPet);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    /**
     * Handles clicks in the target pet selection GUI.
     */
    public void handleTargetSelectionClick(Player player, int slot, ItemStack clicked) {
        TeleportCageSession session = getSession(player);
        if (session == null || session.getSacrificePet() == null) {
            player.closeInventory();
            return;
        }

        // Back button
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            openSacrificePetSelection(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Cancel button
        if (slot == 53 && clicked.getType() == Material.BARRIER) {
            clearSession(player);
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Check if it's a pet slot
        boolean isPetSlot = false;
        for (int petSlot : PET_SLOTS) {
            if (slot == petSlot) {
                isPetSlot = true;
                break;
            }
        }

        if (!isPetSlot) return;

        // Ignore background
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        // Find selected pet
        String itemName = TextUtil.stripColors(clicked.getItemMeta().getDisplayName());
        Pet selectedPet = null;
        for (Pet pet : plugin.getPetManager().getPlayerPets(player)) {
            if (pet.getType().getDisplayName().equals(itemName)) {
                // Don't select the same pet as sacrifice
                if (!pet.getUuid().equals(session.getSacrificePet().getUuid())) {
                    selectedPet = pet;
                    break;
                }
            }
        }

        if (selectedPet != null) {
            openConfirmationGUI(player, session.getSacrificePet(), selectedPet);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    /**
     * Handles clicks in the confirmation GUI.
     */
    public void handleConfirmationClick(Player player, int slot, ItemStack clicked) {
        TeleportCageSession session = getSession(player);
        if (session == null || session.getSacrificePet() == null || session.getTargetPet() == null) {
            player.closeInventory();
            return;
        }

        // Cancel button
        if (slot == 33 && clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            openTargetPetSelection(player, session.getSacrificePet());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Confirm button
        if (slot == 29 && clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            if (!session.isAwaitingConfirmation()) {
                // First click - show warning state
                session.setAwaitingConfirmation(true);

                // Update the confirm button to show "CLICK AGAIN TO BURN"
                Inventory inv = player.getOpenInventory().getTopInventory();
                ItemStack newConfirmItem = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                        .setName(TextUtil.colorize("&c&lCLICK AGAIN TO BURN!"))
                        .addLore("")
                        .addLore(TextUtil.colorize("&c&lFINAL WARNING!"))
                        .addLore(TextUtil.colorize("&7Click again to destroy the pet"))
                        .addLore(TextUtil.colorize("&7and transfer the experience."))
                        .build();
                inv.setItem(29, newConfirmItem);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                player.sendMessage(TextUtil.colorize("&c&lClick again to confirm the burn!"));
            } else {
                // Second click - execute burn
                executeBurn(player, session);
            }
            return;
        }

        // Also handle if they click the orange button (after first click)
        if (slot == 29 && clicked.getType() == Material.ORANGE_STAINED_GLASS_PANE) {
            if (session.isAwaitingConfirmation()) {
                executeBurn(player, session);
            }
        }
    }

    // ==================== BURN EXECUTION ====================

    /**
     * Executes the pet burn - transfers exp and removes sacrifice pet.
     */
    private void executeBurn(Player player, TeleportCageSession session) {
        Pet sacrificePet = session.getSacrificePet();
        Pet targetPet = session.getTargetPet();

        // Final validation
        if (sacrificePet == null || targetPet == null) {
            player.sendMessage(TextUtil.colorize("&cError: Could not find pets. Please try again."));
            clearSession(player);
            player.closeInventory();
            return;
        }

        // Check if sacrifice pet is still valid (not active, still owned)
        List<Pet> playerPets = plugin.getPetManager().getPlayerPets(player);
        boolean sacrificeValid = playerPets.stream()
                .anyMatch(p -> p.getUuid().equals(sacrificePet.getUuid()) && !p.isActive());
        boolean targetValid = playerPets.stream()
                .anyMatch(p -> p.getUuid().equals(targetPet.getUuid()));

        if (!sacrificeValid) {
            player.sendMessage(TextUtil.colorize("&cError: Sacrifice pet is no longer valid!"));
            clearSession(player);
            player.closeInventory();
            return;
        }

        if (!targetValid) {
            player.sendMessage(TextUtil.colorize("&cError: Target pet is no longer valid!"));
            clearSession(player);
            player.closeInventory();
            return;
        }

        // Calculate total exp
        double totalExp = calculateTotalExp(sacrificePet);

        // Get the actual pets from the manager (to ensure we're modifying the right instances)
        Pet actualSacrificePet = playerPets.stream()
                .filter(p -> p.getUuid().equals(sacrificePet.getUuid()))
                .findFirst().orElse(null);
        Pet actualTargetPet = playerPets.stream()
                .filter(p -> p.getUuid().equals(targetPet.getUuid()))
                .findFirst().orElse(null);

        if (actualSacrificePet == null || actualTargetPet == null) {
            player.sendMessage(TextUtil.colorize("&cError: Pet data corrupted. Please try again."));
            clearSession(player);
            player.closeInventory();
            return;
        }

        // Store old level for message
        int oldLevel = actualTargetPet.getLevel();

        // Add experience to target pet
        // We need to bypass the normal addExperience logic since it checks for feeding
        // Instead we'll manually calculate and set level/exp
        int[] predicted = predictLevelAfterTransfer(actualTargetPet, totalExp);
        actualTargetPet.setLevel(predicted[0]);
        actualTargetPet.setExperience(predicted[1]);

        // Remove sacrifice pet
        plugin.getPetManager().removePet(player, actualSacrificePet);

        // Save target pet
        plugin.getPetDataManager().savePet(actualTargetPet);

        // Clear session
        clearSession(player);

        // Close inventory
        player.closeInventory();

        // Play effects
        playBurnEffects(player);

        // Send messages
        int levelsGained = predicted[0] - oldLevel;
        String sacrificeName = sacrificePet.getRarity().getColor() + sacrificePet.getType().getDisplayName();
        String targetName = targetPet.getRarity().getColor() + targetPet.getType().getDisplayName();

        player.sendMessage(TextUtil.colorize("&6[Pets] &aSuccessfully transferred &e" + df.format(totalExp) + " exp &afrom " + sacrificeName + " &ato " + targetName + "&a!"));

        if (levelsGained > 0) {
            player.sendMessage(TextUtil.colorize("&6[Pets] " + targetName + " &agained &e" + levelsGained + " &alevels!"));
        }

        // Send title
        player.sendTitle(
                TextUtil.colorize(sacrificeName + " &7burned!"),
                TextUtil.colorize("&e+" + df.format(totalExp) + " exp &7to " + targetName),
                10, 60, 20
        );
    }

    /**
     * Plays visual and sound effects for the burn.
     */
    private void playBurnEffects(Player player) {
        // Sounds
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.8f, 1.0f);

        // Particles
        player.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                player.getLocation().add(0, 1, 0),
                50,
                0.5, 0.5, 0.5,
                0.05
        );
        player.getWorld().spawnParticle(
                Particle.FLAME,
                player.getLocation().add(0, 1, 0),
                30,
                0.3, 0.3, 0.3,
                0.03
        );
    }

    // ==================== ITEM CREATION ====================

    /**
     * Creates an item representing a pet available for sacrifice.
     */
    private ItemStack createSacrificePetItem(Pet pet, double totalExp) {
        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(TextUtil.colorize("&7Level: &f" + pet.getLevel() + "/" + pet.getRarity().getMaxLevel()));
        lore.add(TextUtil.colorize("&7Rarity: " + pet.getRarity().getDisplayName()));
        lore.add(TextUtil.colorize("&8-------------------"));
        lore.add(TextUtil.colorize("&6Total EXP: &e" + df.format(totalExp)));
        lore.add("");
        lore.add(TextUtil.colorize("&eClick to select for burning"));

        ItemStack baseHead = plugin.getHeadManager().getPetHead(pet.getType());
        ItemBuilder builder = baseHead != null
                ? new ItemBuilder(baseHead)
                : new ItemBuilder(Material.PLAYER_HEAD);

        return builder
                .setName(TextUtil.colorize(pet.getRarity().getColor() + pet.getType().getDisplayName()))
                .setLore(lore)
                .build();
    }

    /**
     * Creates an item representing a target pet with predicted exp gain.
     */
    private ItemStack createTargetPetItem(Pet pet, double expToAdd, int predictedLevel, int predictedExp) {
        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(TextUtil.colorize("&7Current Level: &f" + pet.getLevel() + "/" + pet.getRarity().getMaxLevel()));
        lore.add(TextUtil.colorize("&7Current EXP: &f" + df.format(pet.getExperience())));
        lore.add(TextUtil.colorize("&7Rarity: " + pet.getRarity().getDisplayName()));
        lore.add(TextUtil.colorize("&8-------------------"));
        lore.add(TextUtil.colorize("&a&lAFTER TRANSFER:"));
        int levelsGained = predictedLevel - pet.getLevel();
        String levelGainText = levelsGained > 0 ? " &7(&e+" + levelsGained + "&7)" : "";
        lore.add(TextUtil.colorize("&7Level: &a" + predictedLevel + levelGainText));
        lore.add(TextUtil.colorize("&7EXP: &a" + predictedExp));
        lore.add("");
        lore.add(TextUtil.colorize("&eClick to select as target"));

        ItemStack baseHead = plugin.getHeadManager().getPetHead(pet.getType());
        ItemBuilder builder = baseHead != null
                ? new ItemBuilder(baseHead)
                : new ItemBuilder(Material.PLAYER_HEAD);

        return builder
                .setName(TextUtil.colorize(pet.getRarity().getColor() + pet.getType().getDisplayName()))
                .setLore(lore)
                .build();
    }
}

package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
// Fixed imports to match actual package structure

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PetGUI {

    private final PetPlugin plugin;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private static final int[] PET_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Row 1 (7 slots)
        19, 20, 21, 22, 23, 24, 25,  // Row 2 (7 slots)
        28, 29, 30, 31, 32, 33, 34,  // Row 3 (7 slots)
        37, 38, 39, 40, 41, 42, 43,  // Row 4 (7 slots)
        46, 47, 48, 49, 50, 51, 52   // Row 5 (7 slots) = 35 total slots
    };

    public PetGUI(PetPlugin plugin) {
        this.plugin = plugin;
    }

    // Otwórz główne GUI petów
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, TextUtil.colorize("&8Pet Management"));

        // Wypełnij tło
        ItemStack background = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Dodaj pety gracza
        List<Pet> pets = plugin.getPetManager().getPlayerPets(player);
        int slot = 0;

        for (Pet pet : pets) {
            if (slot >= PET_SLOTS.length) break;

            ItemStack petItem = createPetItem(pet);
            gui.setItem(PET_SLOTS[slot], petItem);
            slot++;
        }

        // Informacje o slotach (nowa pozycja)
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .setName(TextUtil.colorize("&ePet Information"))
                .addLore("")
                .addLore(TextUtil.colorize("&7Active Pets: &f" + plugin.getPetManager().getActivePetCount(player) + "/" + plugin.getPetManager().getMaxPetSlots(player)))
                .addLore(TextUtil.colorize("&7Total Pets: &f" + pets.size()))
                .addLore("")
                .addLore(TextUtil.colorize("&eClick on a pet to manage it!"))
                .build();
        gui.setItem(45, infoItem);

        // Przycisk zamknięcia (nowa pozycja)
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .setName(TextUtil.colorize("&cClose"))
                .build();
        gui.setItem(53, closeItem);

        player.openInventory(gui);
    }

    // Otwórz menu zarządzania konkretnym petem
    public void openPetMenu(Player player, Pet pet) {
        Inventory gui = Bukkit.createInventory(null, 45, TextUtil.colorize("&8Pet: " + pet.getRarity().getColor() + pet.getType().getDisplayName()));

        // Tło
        ItemStack background = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 45; i++) {
            gui.setItem(i, background);
        }

        // Informacje o pecie
        ItemStack petInfo = createDetailedPetItem(pet);
        gui.setItem(13, petInfo);

        // Przycisk aktywacji/deaktywacji
        Material toggleMaterial = pet.isActive() ? Material.ENDER_PEARL : Material.EMERALD;
        String toggleName = pet.isActive() ? "&5Deactivate Pet" : "&aActivate Pet";

        ItemStack toggleItem = new ItemBuilder(toggleMaterial)
                .setName(TextUtil.colorize(toggleName))
                .addLore(TextUtil.colorize(pet.isActive() ? "&7Click to despawn this pet" : "&7Click to spawn this pet"))
                .build();
        gui.setItem(29, toggleItem);

        // Przycisk karmienia (jeśli potrzebne)
        if (pet.needsFeeding()) {
            ItemStack feedItem = new ItemBuilder(Material.SMALL_AMETHYST_BUD)
                    .setName(TextUtil.colorize("&5Feed Pet"))
                    .addLore(TextUtil.colorize("&7Your pet needs feeding to level up!"))
                    .addLore(TextUtil.colorize("&7Required: &5" + pet.getRequiredFeedAmount() + "x Andermant"))
                    .addLore("")
                    .addLore(TextUtil.colorize("&eClick with Andermant in inventory!"))
                    .build();
            gui.setItem(31, feedItem);
        }

        // Przycisk release peta do inventory
        ItemStack releaseItem = new ItemBuilder(Material.FIRE_CHARGE)
                .setName(TextUtil.colorize("&c&lRelease Pet"))
                .addLore(TextUtil.colorize("&7Release pet to your inventory"))
                .addLore(TextUtil.colorize("&7You can use it again later"))
                .addLore("")
                .addLore(TextUtil.colorize("&eShift-Click to release"))
                .build();
        gui.setItem(33, releaseItem);

        // Powrót
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setName(TextUtil.colorize("&7Back"))
                .build();
        gui.setItem(40, backItem);

        player.openInventory(gui);
    }

    // Stwórz item reprezentujący peta
    private ItemStack createPetItem(Pet pet) {
        List<String> lore = new ArrayList<>();

        // Podstawowe informacje
        lore.add("");
        lore.add(TextUtil.colorize("&7Level: &f" + pet.getLevel() + "/" + pet.getRarity().getMaxLevel()));
        lore.add(createExpBar(pet));
        lore.add(TextUtil.colorize("&7Rarity: " + pet.getRarity().getDisplayName()));
        lore.add(TextUtil.colorize("&8-------------------"));

        // Efekty peta
        String effect = plugin.getPetEffectManager().getPetEffectDescription(pet);
        lore.add(TextUtil.colorize("&7Effect: " + effect));

        // Specjalny efekt
        if (pet.hasSpecialEffect()) {
            lore.add(TextUtil.colorize("&6✦ Special Effect Unlocked!"));
        }

        // Status
        lore.add("");
        if (pet.isActive()) {
            lore.add(TextUtil.colorize("&a✓ Active"));
        } else {
            lore.add(TextUtil.colorize("&7○ Inactive"));
        }

        if (pet.needsFeeding()) {
            lore.add(TextUtil.colorize("&c⚠ Needs feeding!"));
        }

        ItemStack baseHead = plugin.getHeadManager().getPetHead(pet.getType());
        ItemBuilder builder = baseHead != null
                ? new ItemBuilder(baseHead)
                : new ItemBuilder(Material.PLAYER_HEAD);

        return builder
                .setName(TextUtil.colorize(pet.getRarity().getColor() + pet.getType().getDisplayName()))
                .setLore(lore)
                .build();
    }

    // Get appropriate material for pet head as fallback
    private Material getPetHeadMaterial(PetType petType) {
        switch (petType) {
            case COW: return Material.LEATHER;
            case PIG: return Material.PORKCHOP;
            case SHEEP: return Material.WHITE_WOOL;
            case CHICKEN: return Material.FEATHER;
            case DONKEY: return Material.CHEST;
            case SNOW_GOLEM: return Material.SNOWBALL;
            case IRON_GOLEM: return Material.IRON_INGOT;
            case SQUID: return Material.INK_SAC;
            case TURTLE: return Material.SCUTE;
            case LLAMA: return Material.LEAD;
            case ENDERMAN: return Material.ENDER_PEARL;
            case WITCH: return Material.POTION;
            case HUSK: return Material.ROTTEN_FLESH;
            case MOOSHROOM: return Material.RED_MUSHROOM;
            case FROG: return Material.LILY_PAD;
            case WOLF: return Material.BONE;
            case BEE: return Material.HONEY_BOTTLE;
            case WANDERING_TRADER: return Material.EMERALD;
            case PANDA: return Material.BAMBOO;
            case ZOMBIE: return Material.ZOMBIE_HEAD;
            case SKELETON: return Material.SKELETON_SKULL;
            case SPIDER: return Material.SPIDER_EYE;
            case CREEPER: return Material.CREEPER_HEAD;
            case SLIME: return Material.SLIME_BALL;
            case PHANTOM: return Material.PHANTOM_MEMBRANE;
            case GLOW_SQUID: return Material.GLOW_INK_SAC;
            case GUARDIAN: return Material.PRISMARINE_SHARD;
            case SNIFFER: return Material.TORCHFLOWER_SEEDS;
            case WITHER_SKELETON: return Material.WITHER_SKELETON_SKULL;
            case ENDER_DRAGON: return Material.DRAGON_HEAD;
            case WARDEN: return Material.ECHO_SHARD;
            case WITHER: return Material.WITHER_SKELETON_SKULL;
            case GIANT: return Material.ZOMBIE_HEAD;
            default: return Material.PLAYER_HEAD;
        }
    }

    // Stwórz szczegółowy item peta
    private ItemStack createDetailedPetItem(Pet pet) {
        List<String> lore = new ArrayList<>();

        // Statystyki
        lore.add("");
        lore.add(TextUtil.colorize("&7Level: &f" + pet.getLevel() + "/" + pet.getRarity().getMaxLevel()));
        lore.add(createExpBar(pet));
        lore.add(TextUtil.colorize("&7Experience: &f" + df.format(pet.getExperience()) + "/" + df.format(pet.getRequiredExperience())));
        lore.add(TextUtil.colorize("&7Rarity: " + pet.getRarity().getDisplayName()));
        lore.add(TextUtil.colorize("&8-------------------"));

        // Efekty
        String baseEffect = plugin.getPetEffectManager().getPetEffectDescription(pet);
        double multiplier = pet.getEffectMultiplier();
        lore.add(TextUtil.colorize("&7Base Effect: " + baseEffect));
        lore.add(TextUtil.colorize("&7Effect Power: &a+" + df.format((multiplier - 1) * 100) + "%"));

        // Progi poziomów
        lore.add("");
        lore.add(TextUtil.colorize("&6Level Milestones:"));
        addMilestone(lore, 25, pet.getLevel());
        addMilestone(lore, 50, pet.getLevel());
        if (pet.getRarity() == PetRarity.UNIQUE || pet.getRarity() == PetRarity.MYTHIC) {
            addMilestone(lore, 75, pet.getLevel());
        }
        if (pet.getRarity() == PetRarity.MYTHIC) {
            addMilestone(lore, 100, pet.getLevel());
        }

        // Specjalny efekt
        if (pet.getRarity() == PetRarity.UNIQUE && pet.getLevel() >= 75) {
            lore.add("");
            lore.add(TextUtil.colorize("&6✦ Special Effect (Lv.75): &aUnlocked!"));
            lore.add(TextUtil.colorize("  " + plugin.getPetEffectManager().getSpecialEffectDescription(pet)));
        } else if (pet.getRarity() == PetRarity.MYTHIC && pet.getLevel() >= 100) {
            lore.add("");
            lore.add(TextUtil.colorize("&c✦ Mythic Effect (Lv.100): &aUnlocked!"));
            lore.add(TextUtil.colorize("  " + plugin.getPetEffectManager().getSpecialEffectDescription(pet)));
        }

        // Status
        lore.add("");
        if (pet.needsFeeding()) {
            lore.add(TextUtil.colorize("&c⚠ Requires feeding to level up!"));
            lore.add(TextUtil.colorize("&7Andermant needed: &5" + pet.getRequiredFeedAmount()));
        }

        ItemStack baseHead = plugin.getHeadManager().getPetHead(pet.getType());
        ItemBuilder builder = baseHead != null
                ? new ItemBuilder(baseHead)
                : new ItemBuilder(Material.PLAYER_HEAD);

        return builder
                .setName(TextUtil.colorize(pet.getRarity().getColor() + "&l" + pet.getType().getDisplayName()))
                .setLore(lore)
                .build();
    }

    // Stwórz pasek doświadczenia
    private String createExpBar(Pet pet) {
        int barLength = 20;
        double progress = pet.getExperienceProgress();
        int filled = (int) (barLength * progress);

        StringBuilder bar = new StringBuilder("&7[&a");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("■");
            } else {
                bar.append("&8■");
            }
            if (i == filled - 1 && filled < barLength) {
                bar.append("&a");
            }
        }
        bar.append("&7] &f").append(df.format(progress * 100)).append("%");

        return TextUtil.colorize(bar.toString());
    }

    // Dodaj informację o kamieniu milowym
    private void addMilestone(List<String> lore, int level, int currentLevel) {
        String status = currentLevel >= level ? "&a✓" : "&7○";
        String bonus = "";

        switch (level) {
            case 25:
                bonus = "Large effect boost";
                break;
            case 50:
                bonus = "Major effect boost";
                break;
            case 75:
                bonus = "Special effect unlock";
                break;
            case 100:
                bonus = "Mythic power unlock";
                break;
        }

        lore.add(TextUtil.colorize("  " + status + " Level " + level + ": &7" + bonus));
    }
}
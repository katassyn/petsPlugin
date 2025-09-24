package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
// Fixed imports to match actual package structure

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PetCommand implements CommandExecutor, TabCompleter {

    private final PetPlugin plugin;
    private final PetGUI petGUI;

    public PetCommand(PetPlugin plugin) {
        this.plugin = plugin;
        this.petGUI = new PetGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            petGUI.openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelpMessage(sender);
                break;

            case "give":
                handleGiveCommand(sender, args);
                break;

            case "admin":
                handleAdminCommand(sender, args);
                break;

            case "spawn":
                handleSpawnCommand(sender, args);
                break;

            case "despawn":
                handleDespawnCommand(sender, args);
                break;

            case "list":
                handleListCommand(sender, args);
                break;

            case "level":
                handleLevelCommand(sender, args);
                break;

            case "exp":
                handleExpCommand(sender, args);
                break;

            case "reload":
                handleReloadCommand(sender);
                break;

            case "backpack":
                handleBackpackCommand(sender);
                break;

            case "debug":
                handleDebugCommand(sender, args);
                break;

            case "debugtextures":
                handleDebugTexturesCommand(sender, args);
                break;

            default:
                sender.sendMessage(TextUtil.colorize("&cUnknown command. Use /pet help for help."));
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(TextUtil.colorize("&6&l=== Pet Plugin Help ==="));
        sender.sendMessage(TextUtil.colorize("&e/pet &7- Open pet management GUI"));
        sender.sendMessage(TextUtil.colorize("&e/pet help &7- Show this help message"));

        if (sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&c&lAdmin Commands:"));
            sender.sendMessage(TextUtil.colorize("&e/pet give <player> <type> [rarity] &7- Give a pet"));
            sender.sendMessage(TextUtil.colorize("&e/pet admin give all <player> &7- Give all pets to player"));
            sender.sendMessage(TextUtil.colorize("&e/pet spawn <player> <type> &7- Force spawn a pet"));
            sender.sendMessage(TextUtil.colorize("&e/pet despawn <player> &7- Force despawn pets"));
            sender.sendMessage(TextUtil.colorize("&e/pet list [player] &7- List pets"));
            sender.sendMessage(TextUtil.colorize("&e/pet level <player> <type> <level> &7- Set pet level"));
            sender.sendMessage(TextUtil.colorize("&e/pet exp <player> <type> <amount> &7- Add exp to pet"));
            sender.sendMessage(TextUtil.colorize("&e/pet reload &7- Reload configuration"));
            sender.sendMessage(TextUtil.colorize("&e/pet debug <player> &7- Show debug info"));
            sender.sendMessage(TextUtil.colorize("&e/pet debugtextures &7- Debug pet skull textures"));
        }

        if (sender.hasPermission("pet.backpack")) {
            sender.sendMessage(TextUtil.colorize("&e/pet backpack &7- Open donkey pet backpack"));
        }
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet give <player> <type> [rarity]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
            return;
        }

        PetType type;
        try {
            type = PetType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(TextUtil.colorize("&cInvalid pet type! Available types:"));
            sender.sendMessage(Arrays.stream(PetType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
            return;
        }

        PetRarity rarity = type.getDefaultRarity();
        if (args.length >= 4) {
            try {
                rarity = PetRarity.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(TextUtil.colorize("&cInvalid rarity! Available rarities:"));
                sender.sendMessage(Arrays.stream(PetRarity.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")));
                return;
            }
        }

        // Sprawdź czy gracz nie ma już tego peta
        for (Pet existingPet : plugin.getPetManager().getPlayerPets(target)) {
            if (existingPet.getType() == type) {
                sender.sendMessage(TextUtil.colorize("&cPlayer already has this pet type!"));
                return;
            }
        }

        Pet pet = new Pet(target.getUniqueId(), type, rarity);
        plugin.getPetManager().addPet(target, pet);

        sender.sendMessage(TextUtil.colorize("&aGave " + rarity.getColor() + type.getDisplayName() +
                " &ato " + target.getName() + "!"));
        target.sendMessage(TextUtil.colorize("&aYou received a " + rarity.getColor() +
                type.getDisplayName() + " &apet!"));
    }

    private void handleSpawnCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet spawn <player> <type>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
            return;
        }

        PetType type;
        try {
            type = PetType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(TextUtil.colorize("&cInvalid pet type!"));
            return;
        }

        Pet pet = null;
        for (Pet p : plugin.getPetManager().getPlayerPets(target)) {
            if (p.getType() == type) {
                pet = p;
                break;
            }
        }

        if (pet == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer doesn't have this pet!"));
            return;
        }

        pet.setActive(true);
        plugin.getPetManager().spawnPet(target, pet);
        sender.sendMessage(TextUtil.colorize("&aSpawned pet for " + target.getName() + "!"));
    }

    private void handleDespawnCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet despawn <player>"));
            return;
        }

        plugin.getPetManager().despawnPet(target);
        for (Pet pet : plugin.getPetManager().getPlayerPets(target)) {
            pet.setActive(false);
        }

        sender.sendMessage(TextUtil.colorize("&aDespawned all pets for " + target.getName() + "!"));
    }

    private void handleListCommand(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("pet.admin")) {
                sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
                return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet list [player]"));
            return;
        }

        List<Pet> pets = plugin.getPetManager().getPlayerPets(target);
        if (pets.isEmpty()) {
            sender.sendMessage(TextUtil.colorize("&c" + target.getName() + " has no pets!"));
            return;
        }

        sender.sendMessage(TextUtil.colorize("&6&l" + target.getName() + "'s Pets:"));
        for (Pet pet : pets) {
            String status = pet.isActive() ? "&a✓ Active" : "&7○ Inactive";
            sender.sendMessage(TextUtil.colorize("&7- " + pet.getRarity().getColor() +
                    pet.getType().getDisplayName() + " &7[Lv." + pet.getLevel() + "] " + status));
        }
    }

    private void handleLevelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet level <player> <type> <level>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
            return;
        }

        PetType type;
        try {
            type = PetType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(TextUtil.colorize("&cInvalid pet type!"));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtil.colorize("&cInvalid level!"));
            return;
        }

        Pet pet = null;
        for (Pet p : plugin.getPetManager().getPlayerPets(target)) {
            if (p.getType() == type) {
                pet = p;
                break;
            }
        }

        if (pet == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer doesn't have this pet!"));
            return;
        }

        pet.setLevel(level);
        pet.setExperience(0);
        plugin.getPetDataManager().savePet(pet);

        sender.sendMessage(TextUtil.colorize("&aSet " + type.getDisplayName() +
                " level to " + level + " for " + target.getName() + "!"));
    }

    private void handleExpCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet exp <player> <type> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
            return;
        }

        PetType type;
        try {
            type = PetType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(TextUtil.colorize("&cInvalid pet type!"));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtil.colorize("&cInvalid amount!"));
            return;
        }

        Pet pet = null;
        for (Pet p : plugin.getPetManager().getPlayerPets(target)) {
            if (p.getType() == type) {
                pet = p;
                break;
            }
        }

        if (pet == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer doesn't have this pet!"));
            return;
        }

        pet.addExperience(amount);
        plugin.getPetDataManager().savePet(pet);

        sender.sendMessage(TextUtil.colorize("&aAdded " + amount + " exp to " +
                type.getDisplayName() + " for " + target.getName() + "!"));
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        plugin.getConfigManager().reload();
        sender.sendMessage(TextUtil.colorize("&aConfiguration reloaded!"));
    }

    private void handleBackpackCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }

        Player player = (Player) sender;

        // Sprawdź czy gracz ma aktywnego Donkey peta
        boolean hasDonkey = false;
        for (Pet pet : plugin.getPetManager().getActivePets(player)) {
            if (pet.getType() == PetType.DONKEY && pet.isActive()) {
                hasDonkey = true;
                break;
            }
        }

        if (!hasDonkey) {
            player.sendMessage(TextUtil.colorize("&cYou need an active Donkey pet to use this!"));
            return;
        }

        // Otwórz dodatkowy ekwipunek
        // TODO: Implementacja dodatkowego ekwipunku
        player.sendMessage(TextUtil.colorize("&eOpening donkey backpack..."));
    }

    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet admin give all <player>"));
            return;
        }

        if (!args[1].equalsIgnoreCase("give") || !args[2].equalsIgnoreCase("all")) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet admin give all <player>"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet admin give all <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[3]);
        if (target == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
            return;
        }

        int givenCount = 0;
        int skippedCount = 0;

        // Iteruj przez wszystkie typy petów
        for (PetType petType : PetType.values()) {
            // Sprawdź czy gracz już ma tego peta
            boolean hasThisPet = false;
            for (Pet existingPet : plugin.getPetManager().getPlayerPets(target)) {
                if (existingPet.getType() == petType) {
                    hasThisPet = true;
                    break;
                }
            }

            if (!hasThisPet) {
                // Daj peta z domyślną rzadkością
                Pet pet = new Pet(target.getUniqueId(), petType, petType.getDefaultRarity());
                plugin.getPetManager().addPet(target, pet);
                givenCount++;
            } else {
                skippedCount++;
            }
        }

        sender.sendMessage(TextUtil.colorize("&aGave &e" + givenCount + " &apets to " + target.getName() + "!"));
        if (skippedCount > 0) {
            sender.sendMessage(TextUtil.colorize("&7Skipped &e" + skippedCount + " &7pets (already owned)"));
        }

        target.sendMessage(TextUtil.colorize("&aYou received &e" + givenCount + " &anew pets! Use /pet to manage them."));
    }

    private void handleDebugCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(TextUtil.colorize("&cPlayer not found!"));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(TextUtil.colorize("&cUsage: /pet debug [player]"));
            return;
        }

        sender.sendMessage(TextUtil.colorize("&6&l=== Debug Info for " + target.getName() + " ==="));
        sender.sendMessage(TextUtil.colorize("&eActive Pets: &f" +
                plugin.getPetManager().getActivePetCount(target) + "/" +
                plugin.getPetManager().getMaxPetSlots(target)));

        for (Pet pet : plugin.getPetManager().getPlayerPets(target)) {
            sender.sendMessage(TextUtil.colorize("&7" + pet.getType().name() + ":"));
            sender.sendMessage(TextUtil.colorize("  &7UUID: &f" + pet.getUuid()));
            sender.sendMessage(TextUtil.colorize("  &7Level: &f" + pet.getLevel() + "/" + pet.getRarity().getMaxLevel()));
            sender.sendMessage(TextUtil.colorize("  &7Exp: &f" + pet.getExperience() + "/" + pet.getRequiredExperience()));
            sender.sendMessage(TextUtil.colorize("  &7Multiplier: &f" + pet.getEffectMultiplier()));
            sender.sendMessage(TextUtil.colorize("  &7Active: &f" + pet.isActive()));
            sender.sendMessage(TextUtil.colorize("  &7Needs Feed: &f" + pet.needsFeeding()));
        }
    }

    private void handleDebugTexturesCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pet.admin")) {
            sender.sendMessage(TextUtil.colorize("&cYou don't have permission!"));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(TextUtil.colorize("&cThis command can only be used by players!"));
            return;
        }

        Player player = (Player) sender;
        plugin.debugPetTextures(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
            if (sender.hasPermission("pet.admin")) {
                completions.addAll(Arrays.asList("give", "admin", "spawn", "despawn", "list", "level", "exp", "reload", "debug", "debugtextures"));
            }
            if (sender.hasPermission("pet.backpack")) {
                completions.add("backpack");
            }
        } else if (args.length == 2 && sender.hasPermission("pet.admin")) {
            if (args[0].equalsIgnoreCase("admin")) {
                completions.add("give");
            } else if (Arrays.asList("give", "spawn", "despawn", "list", "level", "exp", "debug").contains(args[0].toLowerCase())) {
                return null; // Player names
            }
        } else if (args.length == 3 && sender.hasPermission("pet.admin")) {
            if (args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
                completions.add("all");
            } else if (Arrays.asList("give", "spawn", "level", "exp").contains(args[0].toLowerCase())) {
                return Arrays.stream(PetType.values())
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            }
        } else if (args.length == 4 && sender.hasPermission("pet.admin")) {
            if (args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give") && args[2].equalsIgnoreCase("all")) {
                return null; // Player names
            } else if (args[0].equalsIgnoreCase("give")) {
                return Arrays.stream(PetRarity.values())
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
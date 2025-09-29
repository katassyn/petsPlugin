package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetInventoryManager implements Listener {

    private final PetPlugin plugin;
    private final Map<UUID, Inventory> playerBackpacks;

    public PetInventoryManager(PetPlugin plugin) {
        this.plugin = plugin;
        this.playerBackpacks = new HashMap<>();
    }

    public void openDonkeyInventory(Player player) {
        // Sprawdź czy gracz ma aktywnego DONKEY peta
        if (!plugin.getPetManager().hasActivePet(player, PetType.DONKEY)) {
            player.sendMessage(TextUtil.colorize("&cYou need an active Donkey pet to use extra storage!"));
            return;
        }

        // Pobierz aktywnego osła i wylicz sloty
        Pet activeDonkey = getActiveDonkey(player);
        int slots = DonkeyBackpackUtil.resolveSlots(activeDonkey);

        // Stwórz lub pobierz istniejący backpack
        Inventory backpack = playerBackpacks.get(player.getUniqueId());
        if (backpack == null) {
            Inventory loaded = plugin.getDatabaseManager()
                    .loadBackpackInventory(player.getUniqueId(), slots);
            if (loaded != null) {
                backpack = resizeBackpack(loaded, slots, player);
            } else {
                backpack = createBackpack(slots);
            }
            playerBackpacks.put(player.getUniqueId(), backpack);
        } else if (backpack.getSize() != slots) {
            backpack = resizeBackpack(backpack, slots, player);
            playerBackpacks.put(player.getUniqueId(), backpack);
        }

        // Otwórz inventory
        player.openInventory(backpack);
        // Donkey storage opened - no chat message
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        if (command.equals("/donkey") || command.equals("/donkey_storage") || command.equals("/backpack")) {
            event.setCancelled(true);
            openDonkeyInventory(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // Sprawdź czy to Donkey storage
        String title = event.getView().getTitle();
        if (title.contains("Donkey Storage")) {
            // Sprawdź czy gracz nadal ma aktywnego DONKEY peta
            Player player = (Player) event.getWhoClicked();
            if (!plugin.getPetManager().hasActivePet(player, PetType.DONKEY)) {
                event.setCancelled(true);
                player.closeInventory();
                // Donkey pet no longer active - no chat message
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (title.contains("Donkey Storage")) {
            Player player = (Player) event.getPlayer();
            // Zapisz zawartość backpack'a w pamięci i bazie danych
            playerBackpacks.put(player.getUniqueId(), event.getInventory());
            savePlayerBackpack(player);
        }
    }

    public void savePlayerBackpack(Player player) {
        Inventory backpack = playerBackpacks.get(player.getUniqueId());
        if (backpack != null) {
            plugin.getDatabaseManager().saveBackpack(player.getUniqueId(), backpack);
        }
    }

    public void loadPlayerBackpack(Player player) {
        Pet donkey = getHighestLevelDonkey(player);
        if (donkey == null) {
            return;
        }

        int slots = DonkeyBackpackUtil.resolveSlots(donkey);
        Inventory backpack = plugin.getDatabaseManager()
                .loadBackpackInventory(player.getUniqueId(), slots);
        if (backpack != null) {
            Inventory adjusted = resizeBackpack(backpack, slots, player);
            playerBackpacks.put(player.getUniqueId(), adjusted);
        }
    }

    private Pet getActiveDonkey(Player player) {
        for (Pet pet : plugin.getPetManager().getActivePets(player)) {
            if (pet.getType() == PetType.DONKEY && pet.isActive()) {
                return pet;
            }
        }
        return getHighestLevelDonkey(player);
    }

    private Pet getHighestLevelDonkey(Player player) {
        Pet highest = null;
        for (Pet pet : plugin.getPetManager().getPlayerPets(player)) {
            if (pet.getType() != PetType.DONKEY) {
                continue;
            }
            if (highest == null || pet.getLevel() > highest.getLevel()) {
                highest = pet;
            }
        }
        return highest;
    }

    private Inventory createBackpack(int slots) {
        return Bukkit.createInventory(null, slots,
                TextUtil.colorize("&5Donkey Storage &7(" + slots + " slots)"));
    }

    private Inventory resizeBackpack(Inventory original, int targetSlots, Player player) {
        if (original == null) {
            return createBackpack(targetSlots);
        }

        Inventory resized = createBackpack(targetSlots);
        int copyLimit = Math.min(original.getSize(), targetSlots);
        for (int i = 0; i < copyLimit; i++) {
            ItemStack item = original.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                resized.setItem(i, item.clone());
            }
        }

        if (original.getSize() > targetSlots) {
            List<ItemStack> overflow = new ArrayList<>();
            for (int i = targetSlots; i < original.getSize(); i++) {
                ItemStack item = original.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    overflow.add(item.clone());
                }
            }
            handleOverflow(player, overflow);
        }

        return resized;
    }

    private void handleOverflow(Player player, List<ItemStack> overflow) {
        if (overflow.isEmpty()) {
            return;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory()
                .addItem(overflow.toArray(new ItemStack[0]));
        leftovers.values().forEach(item ->
                player.getWorld().dropItemNaturally(player.getLocation(), item));

        player.sendMessage(TextUtil.colorize(plugin.getConfigManager().getPrefix()
                + "&eYour donkey backpack was resized. Extra items were moved to your inventory or dropped at your feet."));
    }
}

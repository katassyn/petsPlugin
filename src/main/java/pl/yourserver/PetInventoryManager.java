package pl.yourserver;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
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

        // Pobierz liczbę slotów z PlaceholderAPI
        String slotsStr = PlaceholderAPI.setPlaceholders(player, "%petplugin_donkey_extra_storage%");
        int slots;
        try {
            slots = Integer.parseInt(slotsStr);
        } catch (NumberFormatException e) {
            slots = 9; // Domyślnie 9 slotów
        }

        // Stwórz lub pobierz istniejący backpack
        Inventory backpack = playerBackpacks.get(player.getUniqueId());
        if (backpack == null) {
            backpack = Bukkit.createInventory(null, slots,
                TextUtil.colorize("&5Donkey Storage &7(" + slots + " slots)"));
            playerBackpacks.put(player.getUniqueId(), backpack);
        }

        // Otwórz inventory
        player.openInventory(backpack);
        // Donkey storage opened - no chat message
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        if (command.equals("/donkey") || command.equals("/storage") || command.equals("/backpack")) {
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
        Inventory backpack = plugin.getDatabaseManager().loadBackpackInventory(player.getUniqueId());
        if (backpack != null) {
            playerBackpacks.put(player.getUniqueId(), backpack);
        }
    }
}
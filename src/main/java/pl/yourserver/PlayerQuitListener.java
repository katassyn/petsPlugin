
// PlayerQuitListener.java
package pl.yourserver;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
// Fixed imports to match actual package structure

public class PlayerQuitListener implements Listener {

    private final PetPlugin plugin;

    public PlayerQuitListener(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Zapisz pety gracza asynchronicznie
        plugin.getPetDataManager().savePlayerPetsAsync(player).thenRun(() -> {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Saved pets for player: " + player.getName());
            }
        });

        // Despawn pety
        plugin.getPetManager().despawnPet(player);

        // Wyczyść pozycje petów z PetFollowTask
        if (plugin.getPetFollowTask() != null) {
            plugin.getPetFollowTask().clearPlayerPets(player);
        }

        // Wyczyść dane z pamięci
        plugin.getPetManager().unloadPlayerPets(player);

        // Wyczyść efekty
        plugin.getPetEffectManager().clearEffects(player);

        // Usuń bonusy petów i zapisz backpack
        plugin.getPetHealthManager().removeHealthBonus(player);
        plugin.getPetLuckManager().removeLuckBonus(player);
        plugin.getPetInventoryManager().savePlayerBackpack(player);
        plugin.getPetCombatManager().savePlayerCooldowns(player);

        // Wyczyść placeholdery
        plugin.getIntegrationManager().clearPlayerPlaceholders(player);
    }
}
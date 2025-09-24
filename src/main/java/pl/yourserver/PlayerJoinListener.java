package pl.yourserver;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;
// Fixed imports to match actual package structure

// PlayerJoinListener.java
public class PlayerJoinListener implements Listener {

    private final PetPlugin plugin;

    public PlayerJoinListener(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Wczytaj pety gracza asynchronicznie (bez spawnu), następnie spawn na main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                // Wczytaj dane petów asynchronicznie
                List<Pet> pets = plugin.getPetDataManager().loadPlayerPets(player);
                plugin.getPetManager().setPlayerPets(player, pets);

                // Spawn aktywnych petów na main thread
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Pet pet : pets) {
                            if (pet.isActive()) {
                                plugin.getPetManager().spawnPet(player, pet);
                            }
                        }
                    }
                }.runTask(plugin);

                // Powiadomienie o petach
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        int petCount = plugin.getPetManager().getPlayerPets(player).size();
                        int activeCount = plugin.getPetManager().getActivePetCount(player);

                        if (petCount > 0) {
                            player.sendMessage(TextUtil.colorize(
                                    plugin.getConfigManager().getPrefix() +
                                            "&eYou have &6" + petCount + " &epets! &7(" + activeCount + " active)"
                            ));

                            // Sprawdź czy jakiś pet wymaga karmienia
                            plugin.getPetManager().getPlayerPets(player).forEach(pet -> {
                                if (pet.needsFeeding()) {
                                    player.sendMessage(TextUtil.colorize(
                                            "&c⚠ Your " + pet.getType().getDisplayName() +
                                                    " needs feeding! Required: &5" + pet.getRequiredFeedAmount() + "x Andermant"
                                    ));
                                }
                            });
                        } else if (player.hasPlayedBefore()) {
                            player.sendMessage(TextUtil.colorize(
                                    plugin.getConfigManager().getPrefix() +
                                            "&7You don't have any pets yet! Get your first pet to begin your adventure!"
                            ));
                        }
                    }
                }.runTaskLater(plugin, 40L); // 2 sekundy po dołączeniu
            }
        }.runTaskAsynchronously(plugin);

        // Aktualizuj placeholdery
        plugin.getIntegrationManager().updatePlayerPlaceholders(player);

        // Aktualizuj bonusy petów i wczytaj backpack
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getPetHealthManager().updatePlayerHealth(player);
                plugin.getPetLuckManager().updatePlayerLuck(player);
                plugin.getPetInventoryManager().loadPlayerBackpack(player);
                plugin.getPetCombatManager().loadPlayerCooldowns(player);
            }
        }.runTaskLater(plugin, 20L);
    }
}
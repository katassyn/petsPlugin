package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages COW pet health bonuses
 */
public class PetHealthManager implements Listener {

    private final PetPlugin plugin;
    private final Map<UUID, Double> appliedHealthBonus = new HashMap<>();

    public PetHealthManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Delay health application to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updatePlayerHealth(event.getPlayer());
        }, 20L); // 1 second delay
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        appliedHealthBonus.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Update player's health based on COW pet bonus
     */
    public void updatePlayerHealth(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        // Get COW pet health bonus
        String placeholder = PlaceholderAPI.setPlaceholders(player, "%petplugin_cow_health%");
        double healthBonus = 0.0;
        try {
            healthBonus = Double.parseDouble(placeholder);
        } catch (NumberFormatException e) {
            healthBonus = 0.0;
        }

        UUID playerId = player.getUniqueId();
        Double currentBonus = appliedHealthBonus.get(playerId);

        // Only update if bonus has changed
        if (currentBonus == null || Math.abs(currentBonus - healthBonus) > 0.1) {
            applyHealthBonus(player, healthBonus);
            appliedHealthBonus.put(playerId, healthBonus);
        }
    }

    /**
     * Apply health bonus to player
     */
    private void applyHealthBonus(Player player, double healthBonus) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute == null) {
            return;
        }

        // Remove old modifier if exists
        healthAttribute.getModifiers().forEach(modifier -> {
            if (modifier.getName().equals("cow_pet_health")) {
                healthAttribute.removeModifier(modifier);
            }
        });

        // Add new modifier if bonus > 0
        if (healthBonus > 0) {
            org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                    "cow_pet_health",
                    healthBonus,
                    org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
            );
            healthAttribute.addModifier(modifier);

            // Heal player to new max health if needed
            double maxHealth = healthAttribute.getValue();
            if (player.getHealth() < maxHealth) {
                player.setHealth(Math.min(maxHealth, player.getHealth() + healthBonus));
            }
        }
    }

    /**
     * Remove health bonus from player (called on quit)
     */
    public void removeHealthBonus(Player player) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute == null) {
            return;
        }

        // Remove pet health modifier
        healthAttribute.getModifiers().forEach(modifier -> {
            if (modifier.getName().equals("cow_pet_health")) {
                healthAttribute.removeModifier(modifier);
            }
        });

        // Remove from tracking
        appliedHealthBonus.remove(player.getUniqueId());
    }

    /**
     * Update all online players' health (called when pets change)
     */
    public void updateAllPlayersHealth() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerHealth(player);
        }
    }
}
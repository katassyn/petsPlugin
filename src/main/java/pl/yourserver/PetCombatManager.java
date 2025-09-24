package pl.yourserver;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetCombatManager implements Listener {

    private final PetPlugin plugin;
    private final Map<UUID, Long> lastPowerStrike;

    public PetCombatManager(PetPlugin plugin) {
        this.plugin = plugin;
        this.lastPowerStrike = new HashMap<>();
    }

    // Wczytaj cooldown gracza z bazy danych
    public void loadPlayerCooldowns(Player player) {
        long ironGolemStrike = plugin.getDatabaseManager().loadCooldown(player.getUniqueId(), "iron_golem_strike");
        if (ironGolemStrike > 0) {
            lastPowerStrike.put(player.getUniqueId(), ironGolemStrike);
        }
    }

    // Zapisz cooldown gracza do bazy danych
    public void savePlayerCooldowns(Player player) {
        Long lastStrike = lastPowerStrike.get(player.getUniqueId());
        if (lastStrike != null) {
            plugin.getDatabaseManager().saveCooldown(player.getUniqueId(), "iron_golem_strike", lastStrike);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player player = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();

        // IRON_GOLEM power strike
        if (plugin.getPetManager().hasActivePet(player, PetType.IRON_GOLEM)) {
            handleIronGolemPowerStrike(player, event);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // TURTLE damage reduction
        if (plugin.getPetManager().hasActivePet(player, PetType.TURTLE)) {
            handleTurtleDamageReduction(player, event);
        }
    }

    private void handleIronGolemPowerStrike(Player player, EntityDamageByEntityEvent event) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastStrike = lastPowerStrike.getOrDefault(playerId, 0L);

        // Sprawdź cooldown (30 sekund = 30000ms)
        if (currentTime - lastStrike < 30000) {
            return;
        }

        // Pobierz mnożnik obrażeń z PlaceholderAPI
        String multiplierStr = PlaceholderAPI.setPlaceholders(player, "%petplugin_irongolem_damage_boost%");
        double multiplier;
        try {
            multiplier = Double.parseDouble(multiplierStr);
        } catch (NumberFormatException e) {
            multiplier = 2.0; // Domyślny mnożnik
        }

        // Aplikuj power strike
        double newDamage = event.getDamage() * multiplier;
        event.setDamage(newDamage);

        // Zapisz czas ostatniego power strike
        lastPowerStrike.put(playerId, currentTime);

        // Iron Golem Power Strike - no chat message
    }

    private void handleTurtleDamageReduction(Player player, EntityDamageEvent event) {
        // Tylko dla obrażeń od mobów
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            return;
        }

        // Pobierz redukcję obrażeń z PlaceholderAPI
        String reductionStr = PlaceholderAPI.setPlaceholders(player, "%petplugin_turtle_damage_reduction%");
        double reduction;
        try {
            reduction = Double.parseDouble(reductionStr);
        } catch (NumberFormatException e) {
            reduction = 1.0; // Domyślna redukcja 1%
        }

        // Aplikuj redukcję obrażeń
        double currentDamage = event.getDamage();
        double newDamage = currentDamage * (1.0 - (reduction / 100.0));
        event.setDamage(newDamage);
    }
}
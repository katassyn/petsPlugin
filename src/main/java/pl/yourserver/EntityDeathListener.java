package pl.yourserver;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
// Fixed imports to match actual package structure
// import pl.yourserver.petplugin.PetPlugin;
// import pl.yourserver.petplugin.models.Pet;
// import pl.yourserver.petplugin.models.PetType;
// import pl.yourserver.petplugin.utils.TextUtil;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EntityDeathListener implements Listener {

    private final PetPlugin plugin;
    private Economy economy;

    public EntityDeathListener(PetPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) return;

        // Sprawdź czy gracz ma aktywne pety
        List<Pet> activePets = plugin.getPetManager().getActivePets(killer);
        if (activePets.isEmpty()) return;

        // Dodaj exp do petów
        double baseExp = plugin.getConfigManager().getExpPerKill();
        double expMultiplier = plugin.getConfigManager().getExpMultiplier();
        double totalExp = baseExp * expMultiplier;

        // Specjalne bonusy exp dla niektórych mobów
        if (entity.getCustomName() != null && entity.getCustomName().contains("Boss")) {
            totalExp *= 5; // x5 exp za bossy
        }

        for (Pet pet : activePets) {
            boolean leveledUp = false;
            int oldLevel = pet.getLevel();

            // Dodaj exp do peta
            pet.addExperience(totalExp);

            // Sprawdź czy pet wbił level
            if (pet.getLevel() > oldLevel) {
                leveledUp = true;
                onPetLevelUp(killer, pet, oldLevel);
            }

            // Efekt Pig - szansa na pieniądze
            if (pet.getType() == PetType.PIG && economy != null) {
                double moneyChance = plugin.getConfigManager().getPigMoneyChance() * pet.getEffectMultiplier();
                if (ThreadLocalRandom.current().nextDouble(100) < moneyChance) {
                    double amount = ThreadLocalRandom.current().nextDouble(1, 10);
                    economy.depositPlayer(killer, amount);
                    // Pig money found - no chat message
                }
            }
        }

        // Aktualizuj placeholdery
        plugin.getIntegrationManager().updatePlayerPlaceholders(killer);
    }

    private void onPetLevelUp(Player player, Pet pet, int oldLevel) {
        int newLevel = pet.getLevel();

        // Wiadomość o level up (bez dźwięku dla zwykłych poziomów)
        String message = plugin.getConfigManager().getMessage("pet-level-up")
                .replace("%pet%", pet.getType().getDisplayName())
                .replace("%level%", String.valueOf(newLevel));
        // Pet level up - no chat message

        // Dźwięk level up TYLKO na milestones (25, 50, 75, 100)
        boolean isMilestone = newLevel == 25 || newLevel == 50 || newLevel == 75 || newLevel == 100;
        if (isMilestone) {
            player.playSound(player.getLocation(),
                    Sound.valueOf(plugin.getConfigManager().getSound("pet-levelup")),
                    1.0f, 1.0f);
        }

        // Milestone reached - no chat messages

        // Pet needs feeding - no chat message

        // Max level reached - no chat messages
        if (newLevel >= pet.getRarity().getMaxLevel()) {
            // Specjalny dźwięk dla max level
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
        }

        // Update entity name
        if (pet.getEntity() != null && !pet.getEntity().isDead()) {
            String petName = TextUtil.colorize(
                    pet.getRarity().getColor() + pet.getType().getDisplayName() +
                            " &7[Lv." + newLevel + "]"
            );
            pet.getEntity().setCustomName(petName);
        }
    }
}
// PetEffectTask.java
package pl.yourserver;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
// Fixed imports to match actual package structure

import java.util.List;

public class PetEffectTask extends BukkitRunnable {

    private final PetPlugin plugin;
    private int ticks = 0;

    public PetEffectTask(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ticks++;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            List<Pet> activePets = plugin.getPetManager().getActivePets(player);

            if (activePets.isEmpty()) {
                // Wyczyść efekty jeśli gracz nie ma petów
                plugin.getPetEffectManager().clearEffects(player);
                continue;
            }

            // Aplikuj efekty petów
            for (Pet pet : activePets) {
                plugin.getPetEffectManager().applyPetEffect(player, pet);

                // Particle effects co 5 sekund
                if (ticks % 5 == 0 && plugin.getConfigManager().getConfig().getBoolean("settings.enable-particles", true)) {
                    if (pet.getEntity() != null && !pet.getEntity().isDead()) {
                        // Specjalne cząsteczki dla petów z special effect
                        if (pet.hasSpecialEffect()) {
                            pet.getEntity().getWorld().spawnParticle(
                                    Particle.valueOf(plugin.getConfigManager().getParticle("pet-special")),
                                    pet.getEntity().getLocation().add(0, 1, 0),
                                    10, 0.3, 0.3, 0.3, 0.05
                            );
                        }
                        // Normalne cząsteczki idle
                        else if (ticks % 10 == 0) {
                            Particle particle = Particle.valueOf(plugin.getConfigManager().getParticle("pet-idle"));
                            pet.getEntity().getWorld().spawnParticle(
                                    particle,
                                    pet.getEntity().getLocation().add(0, 0.5, 0),
                                    5, 0.2, 0.2, 0.2, 0.02
                            );
                        }
                    }
                }
            }

            // Aktualizuj placeholdery i bonusy
            if (ticks % 20 == 0) { // Co sekundę
                plugin.getIntegrationManager().updatePlayerPlaceholders(player);
                plugin.getPetHealthManager().updatePlayerHealth(player);
                plugin.getPetLuckManager().updatePlayerLuck(player);
            }
        }

        // Reset licznika co minutę
        if (ticks >= 1200) {
            ticks = 0;
        }
    }
}
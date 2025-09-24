package pl.yourserver;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class PetSafetyTask extends BukkitRunnable {

    private static final double WARDEN_CLEANSE_RADIUS = 20.0;

    private final PetPlugin plugin;

    public PetSafetyTask(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            List<Pet> pets = plugin.getPetManager().getActivePets(player);
            if (pets.isEmpty()) {
                continue;
            }

            for (Pet pet : pets) {
                // Ensure hostile mobs never keep a combat target
                if (pet.getEntity() instanceof org.bukkit.entity.Mob) {
                    org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) pet.getEntity();
                    if (mob.getTarget() != null) {
                        mob.setTarget(null);
                    }
                }

                if (pet.getType() != PetType.WARDEN) {
                    continue;
                }

                if (pet.getEntity() == null || pet.getEntity().isDead()) {
                    continue;
                }

                pet.getEntity().getWorld().getNearbyPlayers(pet.getEntity().getLocation(), WARDEN_CLEANSE_RADIUS)
                        .forEach(nearby -> {
                            if (nearby.hasPotionEffect(PotionEffectType.DARKNESS)) {
                                nearby.removePotionEffect(PotionEffectType.DARKNESS);
                            }
                            if (nearby.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                                nearby.removePotionEffect(PotionEffectType.BLINDNESS);
                            }
                        });
            }
        }
    }
}

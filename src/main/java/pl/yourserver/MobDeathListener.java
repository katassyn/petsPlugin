package pl.yourserver;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDeathListener implements Listener {
    private final PetDropManager petDropManager;

    public MobDeathListener(PetDropManager petDropManager) {
        this.petDropManager = petDropManager;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        petDropManager.handleMobDeath(entity, killer);
    }
}
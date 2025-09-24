package pl.yourserver;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

public class PetTargetListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();

        if (entity.hasMetadata("pet")) {
            if (entity instanceof Mob) {
                ((Mob) entity).setTarget(null);
            }
            event.setCancelled(true);
            event.setTarget(null);
            return;
        }

        Entity target = event.getTarget();
        if (target != null && target.hasMetadata("pet")) {
            event.setCancelled(true);
            event.setTarget(null);

            if (entity instanceof Mob) {
                ((Mob) entity).setTarget(null);
            }
        }
    }
}

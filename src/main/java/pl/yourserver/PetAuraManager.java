package pl.yourserver;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class PetAuraManager extends BukkitRunnable {

    private final PetPlugin plugin;

    public PetAuraManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // SNOW_GOLEM slow aura
            if (plugin.getPetManager().hasActivePet(player, PetType.SNOW_GOLEM)) {
                applySnowGolemAura(player);
            }
        }
    }

    private void applySnowGolemAura(Player player) {
        // Pobierz zasięg aury z PlaceholderAPI
        String rangeStr = PlaceholderAPI.setPlaceholders(player, "%petplugin_snowgolem_slow%");
        double range;
        try {
            range = Double.parseDouble(rangeStr);
        } catch (NumberFormatException e) {
            range = 5.0; // Domyślny zasięg
        }

        // Znajdź wszystkie wrogie moby w zasięgu
        Collection<LivingEntity> entities = player.getWorld().getLivingEntities();
        for (LivingEntity entity : entities) {
            if (!(entity instanceof Monster)) continue;
            if (entity.getLocation().distance(player.getLocation()) > range) continue;

            // Aplikuj efekt spowolnienia (Slowness II na 3 sekundy)
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1, false, false));
        }
    }
}
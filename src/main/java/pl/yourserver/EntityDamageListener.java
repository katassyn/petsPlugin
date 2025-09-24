package pl.yourserver;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

public class EntityDamageListener implements Listener {

    private static final double WOLF_LIFESTEAL_PERCENT = 0.15D;
    private static final String WITHER_DOT_METADATA = "pet_wither_dot_expiry";

    private final PetPlugin plugin;
    private final Map<UUID, BukkitTask> witherDotTasks = new HashMap<>();

    public EntityDamageListener(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!isPetEntity(entity)) {
            return;
        }

        event.setCancelled(true);

        DamageCause cause = event.getCause();
        if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.LAVA) {
            entity.setFireTicks(0);
            entity.setVisualFire(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();

        if (isPetEntity(victim) || isPetEntity(damager)) {
            event.setCancelled(true);
            return;
        }

        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity shooter && isPetEntity(shooter)) {
                event.setCancelled(true);
                return;
            }

            if (source instanceof Player shooter) {
                handlePlayerAttack(shooter, event);
            }
        } else if (damager instanceof Player attacker) {
            handlePlayerAttack(attacker, event);
        }

        if (victim instanceof Player defender) {
            handlePlayerDefense(defender, event);
        }
    }

    private boolean isPetEntity(Entity entity) {
        return entity != null && entity.hasMetadata("pet");
    }

    private void handlePlayerAttack(Player attacker, EntityDamageByEntityEvent event) {
        List<Pet> activePets = plugin.getPetManager().getActivePets(attacker);
        if (activePets.isEmpty()) {
            return;
        }

        double damageMultiplier = 1.0D;
        double lifestealPercent = 0.0D;
        Entity victim = event.getEntity();

        for (Pet pet : activePets) {
            double petMultiplier = pet.getEffectMultiplier();
            switch (pet.getType()) {
                case IRON_GOLEM -> {
                    if (plugin.getPetEffectManager().hasEffectStack(attacker)) {
                        double multiplier = plugin.getConfigManager().getIronGolemDamageMultiplier() * petMultiplier;
                        damageMultiplier *= Math.max(1.0D, multiplier);
                        plugin.getPetEffectManager().useEffectStack(attacker);

                        if (victim instanceof LivingEntity livingEntity) {
                            livingEntity.setVelocity(livingEntity.getVelocity().setY(0.5D));
                        }
                    }
                }
                case LLAMA -> {
                    if (!(victim instanceof Player)) {
                        double bonus = plugin.getConfigManager().getLlamaMobDamage() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);
                    }
                }
                case WOLF -> {
                    if (victim instanceof Player) {
                        double bonus = plugin.getConfigManager().getWolfPvpDamage() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);

                        if (pet.hasSpecialEffect()) {
                            lifestealPercent = Math.max(lifestealPercent, WOLF_LIFESTEAL_PERCENT * petMultiplier);
                        }
                    }
                }
                case ZOMBIE -> {
                    if (isDungeonQ1orQ3(attacker)) {
                        double bonus = plugin.getConfigManager().getDungeonDamageBonus() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);
                    }
                }
                case SKELETON -> {
                    if (isDungeonQ6orQ7(attacker)) {
                        double bonus = plugin.getConfigManager().getDungeonDamageBonus() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);
                    }
                }
                case SPIDER -> {
                    if (isDungeonQ2orQ4(attacker)) {
                        double bonus = plugin.getConfigManager().getDungeonDamageBonus() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);
                    }
                }
                case CREEPER -> {
                    if (isDungeonQ5orQ8(attacker)) {
                        double bonus = plugin.getConfigManager().getDungeonDamageBonus() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);
                    }
                }
                case SLIME -> {
                    if (isDungeonQ9orQ10(attacker)) {
                        double bonus = plugin.getConfigManager().getDungeonDamageBonus() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);
                    }
                }
                case WITHER -> {
                    if (isBoss(victim)) {
                        double bonus = plugin.getConfigManager().getWitherBossDamage() / 100.0D;
                        damageMultiplier *= 1.0D + (bonus * petMultiplier);

                        if (pet.hasSpecialEffect() && victim instanceof LivingEntity livingEntity) {
                            applyWitherSpecial(attacker, livingEntity);
                        }
                    }
                }
                default -> {
                }
            }
        }

        if (damageMultiplier > 1.0D) {
            double modifiedDamage = event.getDamage() * damageMultiplier;
            event.setDamage(modifiedDamage);

            if (lifestealPercent > 0.0D) {
                applyLifesteal(attacker, modifiedDamage * lifestealPercent);
            }
        } else if (lifestealPercent > 0.0D) {
            applyLifesteal(attacker, event.getDamage() * lifestealPercent);
        }
    }

    private void handlePlayerDefense(Player defender, EntityDamageByEntityEvent event) {
        List<Pet> activePets = plugin.getPetManager().getActivePets(defender);
        if (activePets.isEmpty()) {
            return;
        }

        double damageMultiplier = 1.0D;
        Entity damager = event.getDamager();

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        for (Pet pet : activePets) {
            if (pet.getType() == PetType.TURTLE && !(damager instanceof Player)) {
                double reduction = plugin.getConfigManager().getTurtleDamageReduction() / 100.0D;
                double petMultiplier = pet.getEffectMultiplier();
                damageMultiplier *= Math.max(0.0D, 1.0D - (reduction * petMultiplier));
            } else if (pet.getType() == PetType.WOLF && damager instanceof Player) {
                double reduction = plugin.getConfigManager().getWolfPvpDamage() / 100.0D;
                double petMultiplier = pet.getEffectMultiplier();
                damageMultiplier *= Math.max(0.0D, 1.0D - (reduction * petMultiplier));
            }
        }

        if (damageMultiplier < 1.0D) {
            event.setDamage(event.getDamage() * damageMultiplier);
        }
    }

    private void applyWitherSpecial(Player attacker, LivingEntity target) {
        if (target instanceof Player) {
            return;
        }

        double damagePerSecond = plugin.getConfigManager().getWitherSpecialDamage();
        int durationSeconds = plugin.getConfigManager().getWitherSpecialDuration();
        if (damagePerSecond <= 0.0D || durationSeconds <= 0) {
            return;
        }

        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        target.setMetadata(WITHER_DOT_METADATA, new FixedMetadataValue(plugin, expiryTime));

        BukkitTask existingTask = witherDotTasks.remove(target.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) {
                    clear();
                    return;
                }

                long currentExpiry = getWitherExpiry(target);
                if (currentExpiry <= System.currentTimeMillis()) {
                    clear();
                    return;
                }

                target.damage(damagePerSecond, attacker);
            }

            private void clear() {
                cancel();
                witherDotTasks.remove(target.getUniqueId());
                target.removeMetadata(WITHER_DOT_METADATA, plugin);
            }
        };

        witherDotTasks.put(target.getUniqueId(), task.runTaskTimer(plugin, 0L, 20L));
    }

    private long getWitherExpiry(LivingEntity entity) {
        if (!entity.hasMetadata(WITHER_DOT_METADATA)) {
            return 0L;
        }

        for (MetadataValue value : entity.getMetadata(WITHER_DOT_METADATA)) {
            if (value.getOwningPlugin() == plugin) {
                return value.asLong();
            }
        }

        return 0L;
    }

    private void applyLifesteal(Player player, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = attribute != null ? attribute.getValue() : 20.0D;
        double targetHealth = Math.min(maxHealth, player.getHealth() + amount);
        player.setHealth(targetHealth);
    }

    private boolean isBoss(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        if (hasConfiguredBossName(livingEntity)) {
            return true;
        }

        if (isMythicBoss(livingEntity)) {
            return true;
        }

        EntityType type = livingEntity.getType();
        return type == EntityType.ENDER_DRAGON
                || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.WARDEN
                || type == EntityType.WITHER;
    }

    private boolean hasConfiguredBossName(LivingEntity entity) {
        String customName = entity.getCustomName();
        if (customName == null) {
            return false;
        }

        List<String> configuredBosses = plugin.getConfigManager().getBossList();
        if (configuredBosses.isEmpty()) {
            return false;
        }

        String lowerName = customName.toLowerCase(Locale.ROOT);
        for (String bossName : configuredBosses) {
            if (bossName != null && lowerName.contains(bossName.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean isMythicBoss(LivingEntity entity) {
        if (!plugin.getConfigManager().hasMythicBosses()) {
            return false;
        }

        if (!plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            return false;
        }

        try {
            Object mobManager = MythicBukkit.inst().getMobManager();
            Method getActiveMob = mobManager.getClass().getMethod("getActiveMob", UUID.class);
            Object optional = getActiveMob.invoke(mobManager, entity.getUniqueId());

            if (optional instanceof Optional<?> opt && opt.isPresent()) {
                Object activeMob = opt.get();
                Method getType = activeMob.getClass().getMethod("getType");
                Object mythicMob = getType.invoke(activeMob);

                if (mythicMob != null) {
                    Method getInternalName = mythicMob.getClass().getMethod("getInternalName");
                    Object internalName = getInternalName.invoke(mythicMob);

                    if (internalName instanceof String name) {
                        return plugin.getConfigManager().isMythicBoss(name);
                    }
                }
            } else {
                Method isMythicMob = mobManager.getClass().getMethod("isMythicMob", Entity.class);
                Object result = isMythicMob.invoke(mobManager, entity);
                if (result instanceof Boolean bool && bool) {
                    return plugin.getConfigManager().isMythicBoss(entity.getType().name());
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean isDungeonQ1orQ3(Player player) {
        return isPlayerInWorld(player, "q1", "q3");
    }

    private boolean isDungeonQ2orQ4(Player player) {
        return isPlayerInWorld(player, "q2", "q4");
    }

    private boolean isDungeonQ5orQ8(Player player) {
        return isPlayerInWorld(player, "q5", "q8");
    }

    private boolean isDungeonQ6orQ7(Player player) {
        return isPlayerInWorld(player, "q6", "q7");
    }

    private boolean isDungeonQ9orQ10(Player player) {
        return isPlayerInWorld(player, "q9", "q10");
    }

    private boolean isPlayerInWorld(Player player, String... markers) {
        String worldName = player.getWorld().getName().toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            if (worldName.contains(marker.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}

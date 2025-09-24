package pl.yourserver;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.api.mobs.MythicMob;
// Removed unused BukkitAdapter import
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
// Fixed imports to match actual package structure
// import pl.yourserver.petplugin.PetPlugin;
// import pl.yourserver.petplugin.models.Pet;
// import pl.yourserver.petplugin.models.PetType;
// import pl.yourserver.petplugin.utils.TextUtil;

import java.util.List;
import java.util.Optional;

public class EntityDamageListener implements Listener {

    private final PetPlugin plugin;

    public EntityDamageListener(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        // Protect pets from all damage
        Entity entity = event.getEntity();
        if (entity.hasMetadata("pet")) {
            event.setCancelled(true);

            // Additional protection from fire/sunlight
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                entity.setFireTicks(0);
                entity.setVisualFire(false);
            }
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Protect pets from all damage
        Entity entity = event.getEntity();
        if (entity.hasMetadata("pet")) {
            event.setCancelled(true);
            return;
        }

        // Prevent pets from damaging anything
        Entity damager = event.getDamager();
        if (damager.hasMetadata("pet")) {
            event.setCancelled(true);
            return;
        }

        // Check if attacker is a player
        if (damager instanceof Player) {
            Player attacker = (Player) damager;
            handlePlayerAttack(attacker, event);
        }

        // Check if defender is a player
        if (entity instanceof Player) {
            Player defender = (Player) entity;
            handlePlayerDefense(defender, event);
        }
    }

    private void handlePlayerAttack(Player attacker, EntityDamageByEntityEvent event) {
        List<Pet> activePets = plugin.getPetManager().getActivePets(attacker);
        if (activePets.isEmpty()) return;

        double damageMultiplier = 1.0;
        Entity victim = event.getEntity();

        for (Pet pet : activePets) {
            double petMultiplier = pet.getEffectMultiplier();

            switch (pet.getType()) {
                case IRON_GOLEM:
                    // Check if player has an effect stack
                    if (plugin.getPetEffectManager().hasEffectStack(attacker)) {
                        damageMultiplier *= plugin.getConfigManager().getIronGolemDamageMultiplier() * petMultiplier;
                        plugin.getPetEffectManager().useEffectStack(attacker);

                        // PodrzuÄ‚â€žĂ˘â‚¬Ë‡ cel
                        if (victim instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) victim;
                            living.setVelocity(living.getVelocity().setY(0.5));
                        }

                        // Power Strike activated - no chat message
                    }
                        // Launch the target upwards

                case LLAMA:
                    // ZwiÄ‚â€žĂ˘â€žËkszone obraĂ„Ä…Ă„Ëťenia na moby (nie graczy)
                    if (!(victim instanceof Player)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getLlamaMobDamage() / 100.0 * petMultiplier);
                    // Increased damage against mobs (not players)
                    break;

                case WOLF:
                    // ZwiÄ‚â€žĂ˘â€žËkszone obraĂ„Ä…Ă„Ëťenia PvP
                    if (victim instanceof Player) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getWolfPvpDamage() / 100.0 * petMultiplier);
                    // Increased PvP damage
                        // Lifesteal jeĂ„Ä…Ă˘â‚¬Ĺźli ma special effect
                        if (pet.hasSpecialEffect()) {
                            double healAmount = event.getDamage() * 0.15;
                        // Lifesteal if special ability is active
                            // Lifesteal - no chat message
                        }
                    }
                    break;

                // Dungeon damage bonuses
                case ZOMBIE:
                    if (isDungeonQ1orQ3(attacker)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getDungeonDamageBonus() / 100.0 * petMultiplier);
                    }
                    break;

                case SKELETON:
                    if (isDungeonQ6orQ7(attacker)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getDungeonDamageBonus() / 100.0 * petMultiplier);
                    }
                    break;

                case SPIDER:
                    if (isDungeonQ2orQ4(attacker)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getDungeonDamageBonus() / 100.0 * petMultiplier);
                    }
                    break;

                case CREEPER:
                    if (isDungeonQ5orQ8(attacker)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getDungeonDamageBonus() / 100.0 * petMultiplier);
                    }
                    break;

                case SLIME:
                    if (isDungeonQ9orQ10(attacker)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getDungeonDamageBonus() / 100.0 * petMultiplier);
                    }
                    break;

                case WITHER:
                    // Increased damage against bosses
                    if (isBoss(victim)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getWitherBossDamage() / 100.0 * petMultiplier);

                        // Apply wither effect when special ability is active
                        if (pet.hasSpecialEffect() && victim instanceof LivingEntity) {
                            LivingEntity boss = (LivingEntity) victim;
                            boss.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                        }
                    }
                    break;
            }
        }

        // Apply modified damage
        if (damageMultiplier > 1.0) {
            event.setDamage(event.getDamage() * damageMultiplier);
        }
    }

    private void handlePlayerDefense(Player defender, EntityDamageByEntityEvent event) {
        List<Pet> activePets = plugin.getPetManager().getActivePets(defender);
        if (activePets.isEmpty()) return;

        double damageReduction = 1.0;

        for (Pet pet : activePets) {
            double petMultiplier = pet.getEffectMultiplier();

            if (pet.getType() == PetType.TURTLE) {
                // Zmniejszone obraĂ„Ä…Ă„Ëťenia od mobĂ„â€šÄąâ€šw (nie graczy)
                if (!(event.getDamager() instanceof Player)) {
                    damageReduction *= 1.0 - (plugin.getConfigManager().getTurtleDamageReduction() / 100.0 * petMultiplier);
                }
                // Reduced incoming damage from mobs (not players)
        }

        // Aplikuj redukcjÄ‚â€žĂ˘â€žË obraĂ„Ä…Ă„ËťeĂ„Ä…Ă˘â‚¬Ĺľ
        if (damageReduction < 1.0) {
            event.setDamage(event.getDamage() * damageReduction);
        }
        // Apply damage reduction

    // SprawdĂ„Ä…ÄąĹş czy entity jest bossem
    private boolean isBoss(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;

    // Determine if entity is a boss
        if (entity.getCustomName() != null) {
            String name = entity.getCustomName();
        // Check for custom name match
            // SprawdĂ„Ä…ÄąĹş listÄ‚â€žĂ˘â€žË bossĂ„â€šÄąâ€šw z configu
            for (String bossName : plugin.getConfigManager().getBossList()) {
                if (name.contains(bossName)) {
            // Compare against configured boss list
                }
            }
        }

        // Check MythicMobs integration if available
        if (plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            try {
                if (plugin.getConfigManager().hasMythicBosses()) {
                    Optional<ActiveMob> activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
                    if (activeMob.isPresent()) {
                        MythicMob mythicMob = activeMob.get().getType();
                        if (mythicMob != null && plugin.getConfigManager().isMythicBoss(mythicMob.getInternalName())) {
                            return true;
                        }
                    }
                } else if (MythicBukkit.inst().getMobManager().isMythicMob(entity)) {
        // Fallback: treat vanilla bosses as bosses
                }
            } catch (Exception e) {
                // Ignore MythicMobs API errors
            }
        }

    // Dungeon helper methods (placeholder - requires MyDungeonTeleportPlugin integration)
        switch (entity.getType()) {
            case ENDER_DRAGON:
        // Check if player is in Q1 or Q3
            case ELDER_GUARDIAN:
            case WARDEN:
                return true;
            default:
                return false;
        // Check if player is in Q2 or Q4
    }

    // Metody sprawdzajÄ‚â€žĂ˘â‚¬Â¦ce dungeony (placeholder - wymaga integracji z MyDungeonTeleportPlugin)
    private boolean isDungeonQ1orQ3(Player player) {
        // TODO: Integracja z MyDungeonTeleportPlugin
        // Check if player is in Q5 or Q8
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.contains("q1") || worldName.contains("q3");
    }

    private boolean isDungeonQ2orQ4(Player player) {
        // Check if player is in Q6 or Q7
        return worldName.contains("q2") || worldName.contains("q4");
    }

    private boolean isDungeonQ5orQ8(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        // Check if player is in Q9 or Q10
    }

    private boolean isDungeonQ6orQ7(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.contains("q6") || worldName.contains("q7");
    }

    private boolean isDungeonQ9orQ10(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.contains("q9") || worldName.contains("q10");
    }
}

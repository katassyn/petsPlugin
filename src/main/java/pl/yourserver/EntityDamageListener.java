package pl.yourserver;

import io.lumine.mythic.bukkit.MythicBukkit;
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

        // Sprawdź czy atakujący to gracz
        if (damager instanceof Player) {
            Player attacker = (Player) damager;
            handlePlayerAttack(attacker, event);
        }

        // Sprawdź czy obrońca to gracz
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
                    // Sprawdź czy gracz ma stack efektu
                    if (plugin.getPetEffectManager().hasEffectStack(attacker)) {
                        damageMultiplier *= plugin.getConfigManager().getIronGolemDamageMultiplier() * petMultiplier;
                        plugin.getPetEffectManager().useEffectStack(attacker);

                        // Podrzuć cel
                        if (victim instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) victim;
                            living.setVelocity(living.getVelocity().setY(0.5));
                        }

                        // Power Strike activated - no chat message
                    }
                    break;

                case LLAMA:
                    // Zwiększone obrażenia na moby (nie graczy)
                    if (!(victim instanceof Player)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getLlamaMobDamage() / 100.0 * petMultiplier);
                    }
                    break;

                case WOLF:
                    // Zwiększone obrażenia PvP
                    if (victim instanceof Player) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getWolfPvpDamage() / 100.0 * petMultiplier);

                        // Lifesteal jeśli ma special effect
                        if (pet.hasSpecialEffect()) {
                            double healAmount = event.getDamage() * 0.15;
                            attacker.setHealth(Math.min(attacker.getHealth() + healAmount, attacker.getMaxHealth()));
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
                    // Zwiększone obrażenia na bossy
                    if (isBoss(victim)) {
                        damageMultiplier *= 1.0 + (plugin.getConfigManager().getWitherBossDamage() / 100.0 * petMultiplier);

                        // Wither effect jeśli ma special effect
                        if (pet.hasSpecialEffect() && victim instanceof LivingEntity) {
                            LivingEntity boss = (LivingEntity) victim;
                            boss.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                        }
                    }
                    break;
            }
        }

        // Aplikuj zmodyfikowane obrażenia
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
                // Zmniejszone obrażenia od mobów (nie graczy)
                if (!(event.getDamager() instanceof Player)) {
                    damageReduction *= 1.0 - (plugin.getConfigManager().getTurtleDamageReduction() / 100.0 * petMultiplier);
                }
            }
        }

        // Aplikuj redukcję obrażeń
        if (damageReduction < 1.0) {
            event.setDamage(event.getDamage() * damageReduction);
        }
    }

    // Sprawdź czy entity jest bossem
    private boolean isBoss(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;

        // Sprawdź custom name
        if (entity.getCustomName() != null) {
            String name = entity.getCustomName();

            // Sprawdź listę bossów z configu
            for (String bossName : plugin.getConfigManager().getBossList()) {
                if (name.contains(bossName)) {
                    return true;
                }
            }
        }

        // Sprawdź MythicMobs jeśli dostępny
        if (plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            try {
                return MythicBukkit.inst().getMobManager().isMythicMob(entity);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Sprawdź czy to vanilla boss
        switch (entity.getType()) {
            case ENDER_DRAGON:
            case WITHER:
            case ELDER_GUARDIAN:
            case WARDEN:
                return true;
            default:
                return false;
        }
    }

    // Metody sprawdzające dungeony (placeholder - wymaga integracji z MyDungeonTeleportPlugin)
    private boolean isDungeonQ1orQ3(Player player) {
        // TODO: Integracja z MyDungeonTeleportPlugin
        // Sprawdź czy gracz jest w Q1 lub Q3
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.contains("q1") || worldName.contains("q3");
    }

    private boolean isDungeonQ2orQ4(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.contains("q2") || worldName.contains("q4");
    }

    private boolean isDungeonQ5orQ8(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.contains("q5") || worldName.contains("q8");
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
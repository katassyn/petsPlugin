package pl.yourserver;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetFollowTask extends BukkitRunnable {

    private final PetPlugin plugin;
    // Track last pet positions for smoother movement
    private final Map<UUID, Location> lastPetPositions = new HashMap<>();
    private final Map<UUID, Integer> stuckTicks = new HashMap<>();
    private static final int STUCK_TICK_THRESHOLD = 20;
    private static final double MIN_MOVEMENT_SQ = 0.04;

    public PetFollowTask(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            List<Pet> activePets = plugin.getPetManager().getActivePets(player);

            if (activePets.isEmpty()) {
                continue;
            }

            int petCount = activePets.size();
            int petIndex = 0;

            for (Pet pet : activePets) {
                if (pet.getEntity() == null || pet.getEntity().isDead()) {
                    // Respawn pet if it died
                    plugin.getPetManager().spawnPet(player, pet);
                    continue;
                }

                Entity entity = pet.getEntity();
                Location playerLoc = player.getLocation();
                Location petLoc = entity.getLocation();

                if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (mob.getTarget() != null) {
                        mob.setTarget(null);
                    }
                }

                double distance = playerLoc.distance(petLoc);
                double followDistance = plugin.getConfigManager().getFollowDistance();
                double teleportDistance = plugin.getConfigManager().getTeleportDistance();

                Location previousPosition = lastPetPositions.get(pet.getUuid());
                if (distance > followDistance) {
                    if (previousPosition != null
                            && previousPosition.getWorld() != null
                            && previousPosition.getWorld().equals(petLoc.getWorld())) {
                        if (petLoc.distanceSquared(previousPosition) < MIN_MOVEMENT_SQ) {
                            stuckTicks.put(pet.getUuid(), stuckTicks.getOrDefault(pet.getUuid(), 0) + 1);
                        } else {
                            stuckTicks.remove(pet.getUuid());
                        }
                    } else {
                        stuckTicks.remove(pet.getUuid());
                    }
                } else {
                    stuckTicks.remove(pet.getUuid());
                }

                Location targetLoc = getOffsetLocation(player, petIndex, petCount);

                boolean shouldTeleport = distance > teleportDistance
                        && stuckTicks.getOrDefault(pet.getUuid(), 0) >= STUCK_TICK_THRESHOLD;

                if (shouldTeleport) {
                    entity.teleport(targetLoc);
                    stuckTicks.remove(pet.getUuid());
                    lastPetPositions.put(pet.getUuid(), targetLoc.clone());

                    if (plugin.getConfigManager().getConfig().getBoolean("settings.enable-particles", true)) {
                        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                    petIndex++;
                    continue;
                }

                if (distance > followDistance) {
                    if (entity instanceof Mob) {
                        Mob mob = (Mob) entity;

                        float playerSpeed = player.getWalkSpeed();
                        double petSpeed = Math.max(0.35, Math.min(playerSpeed * 1.8, 0.8));

                        if (mob instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) mob;
                            try {
                                living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)
                                        .setBaseValue(petSpeed);
                            } catch (Exception ignored) {}
                        }

                        mob.setTarget(null);
                        mob.getPathfinder().moveTo(targetLoc, petSpeed);

                        if (pet.getRarity() == PetRarity.MYTHIC &&
                                plugin.getConfigManager().getConfig().getBoolean("settings.enable-particles", true)) {
                            entity.getWorld().spawnParticle(
                                    Particle.valueOf(plugin.getConfigManager().getParticle("pet-idle")),
                                    entity.getLocation().add(0, 0.5, 0),
                                    3, 0.2, 0.2, 0.2, 0.05
                            );
                        }

                    } else {
                        Vector direction = targetLoc.toVector().subtract(petLoc.toVector()).normalize();
                        double speed = Math.min(player.getWalkSpeed() * 1.2, 0.5);

                        Location newLoc = petLoc.clone().add(direction.multiply(speed));
                        entity.teleport(newLoc);
                    }
                } else {
                    if (entity instanceof Mob) {
                        Mob mob = (Mob) entity;
                        mob.getPathfinder().stopPathfinding();

                        Location lookLoc = playerLoc.clone();
                        lookLoc.setY(entity.getLocation().getY());
                        Vector lookDirection = lookLoc.toVector().subtract(entity.getLocation().toVector());
                        if (lookDirection.length() > 0) {
                            Location newLoc = entity.getLocation().clone();
                            newLoc.setDirection(lookDirection);
                            entity.teleport(newLoc);
                        }
                    }
                }

                lastPetPositions.put(pet.getUuid(), entity.getLocation().clone());
                petIndex++;
            }
        }
    }

    private Location getOffsetLocation(Player player, int index, int totalPets) {
        Location loc = player.getLocation();

        // Offset different pet counts for nicer positioning
        double offsetDistance;
        double angle;

        switch (totalPets) {
            case 1:
                // Single pet behind the player
                angle = player.getLocation().getYaw() + 180;
                offsetDistance = 2.0;
                break;

            case 2:
                // Two pets to the player sides
                angle = player.getLocation().getYaw() + (index == 0 ? 90 : -90);
                offsetDistance = 2.5;
                break;

            case 3:
                // Triangle pattern when three pets are active
                double baseAngle = player.getLocation().getYaw() + 180;
                angle = baseAngle + (index - 1) * 120;
                offsetDistance = 2.5;
                break;

            case 4:
                // Four pets form a square around the player
                angle = player.getLocation().getYaw() + 45 + (index * 90);
                offsetDistance = 3.0;
                break;

            default:
                // More than four pets use a circle spread
                angle = (Math.PI * 2 * index / totalPets) * (180 / Math.PI);
                offsetDistance = 3.0 + (totalPets - 4) * 0.5;
                break;
        }

        // Convert angle to radians
        double radians = Math.toRadians(angle);

        // Apply offset relative to the player
        double offsetX = Math.cos(radians) * offsetDistance;
        double offsetZ = Math.sin(radians) * offsetDistance;

        loc.add(offsetX, 0, offsetZ);

        // Raise pets slightly for visibility
        loc.add(0, 0.1, 0);

        return loc;
    }

    // Clear single pet state when it is removed
    public void clearPetPosition(UUID petUuid) {
        lastPetPositions.remove(petUuid);
        stuckTicks.remove(petUuid);
    }

    // Clear cached state for all player pets
    public void clearPlayerPets(Player player) {
        List<Pet> pets = plugin.getPetManager().getPlayerPets(player);
        for (Pet pet : pets) {
            lastPetPositions.remove(pet.getUuid());
            stuckTicks.remove(pet.getUuid());
        }
    }
}



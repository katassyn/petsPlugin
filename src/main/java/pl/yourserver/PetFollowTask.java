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
    // Przechowuj ostatnie pozycje petów dla płynniejszego ruchu
    private final Map<UUID, Location> lastPetPositions = new HashMap<>();

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
                    // Respawn peta jeśli zginął
                    plugin.getPetManager().spawnPet(player, pet);
                    continue;
                }

                Entity entity = pet.getEntity();
                Location playerLoc = player.getLocation();
                Location petLoc = entity.getLocation();

                // Oblicz dystans
                double distance = playerLoc.distance(petLoc);

                // Teleportuj jeśli za daleko (tylko w ostateczności)
                if (distance > plugin.getConfigManager().getTeleportDistance()) {
                    Location teleportLoc = getOffsetLocation(player, petIndex, petCount);
                    entity.teleport(teleportLoc);
                    lastPetPositions.put(pet.getUuid(), teleportLoc);

                    // Efekt teleportacji
                    if (plugin.getConfigManager().getConfig().getBoolean("settings.enable-particles", true)) {
                        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                    petIndex++;
                    continue;
                }

                // Pety biegają za graczem używając AI pathfinding
                if (distance > plugin.getConfigManager().getFollowDistance()) {
                    // Oblicz docelową pozycję dla tego peta
                    Location targetLoc = getOffsetLocation(player, petIndex, petCount);

                    if (entity instanceof Mob) {
                        Mob mob = (Mob) entity;

                        // Dostosuj prędkość do prędkości gracza
                        float playerSpeed = player.getWalkSpeed();
                        double petSpeed = Math.max(0.2, Math.min(playerSpeed * 1.5, 0.6)); // 1.5x prędkość gracza, max 0.6

                        // Ustaw prędkość ruchu peta
                        if (mob instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) mob;
                            try {
                                living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)
                                        .setBaseValue(petSpeed);
                            } catch (Exception ignored) {}
                        }

                        // Wyczyść target żeby nie atakował innych mobów
                        mob.setTarget(null);

                        // Użyj pathfinding do poruszania się do celu
                        mob.getPathfinder().moveTo(targetLoc, petSpeed);

                        // Zapisz target location
                        lastPetPositions.put(pet.getUuid(), targetLoc);

                        // Efekt ruchu dla mythic petów
                        if (pet.getRarity() == PetRarity.MYTHIC &&
                                plugin.getConfigManager().getConfig().getBoolean("settings.enable-particles", true)) {
                            entity.getWorld().spawnParticle(
                                    Particle.valueOf(plugin.getConfigManager().getParticle("pet-idle")),
                                    entity.getLocation().add(0, 0.5, 0),
                                    3, 0.2, 0.2, 0.2, 0.05
                            );
                        }

                    } else {
                        // Fallback dla entity które nie są Mob - użyj starej logiki z mniejszą teleportacją
                        Vector direction = targetLoc.toVector().subtract(petLoc.toVector()).normalize();
                        double speed = Math.min(player.getWalkSpeed() * 1.2, 0.5);

                        Location newLoc = petLoc.clone().add(direction.multiply(speed));
                        entity.teleport(newLoc);
                        lastPetPositions.put(pet.getUuid(), newLoc);
                    }
                } else {
                    // Pet jest blisko - zatrzymaj pathfinding i zostań w miejscu
                    if (entity instanceof Mob) {
                        Mob mob = (Mob) entity;
                        mob.getPathfinder().stopPathfinding();

                        // Patrz na gracza
                        Location lookLoc = playerLoc.clone();
                        lookLoc.setY(entity.getLocation().getY()); // Tą sama wysokość
                        Vector lookDirection = lookLoc.toVector().subtract(entity.getLocation().toVector());
                        if (lookDirection.length() > 0) {
                            Location newLoc = entity.getLocation().clone();
                            newLoc.setDirection(lookDirection);
                            entity.teleport(newLoc);
                        }
                    }

                    lastPetPositions.put(pet.getUuid(), petLoc);
                }

                petIndex++;
            }
        }
    }

    private Location getOffsetLocation(Player player, int index, int totalPets) {
        Location loc = player.getLocation();

        // Specjalne pozycjonowanie dla różnej liczby petów
        double offsetDistance;
        double angle;

        switch (totalPets) {
            case 1:
                // Pojedynczy pet - z tyłu gracza
                angle = player.getLocation().getYaw() + 180;
                offsetDistance = 2.0;
                break;

            case 2:
                // Dwa pety - po bokach gracza
                angle = player.getLocation().getYaw() + (index == 0 ? 90 : -90);
                offsetDistance = 2.5;
                break;

            case 3:
                // Trzy pety - trójkąt za graczem
                double baseAngle = player.getLocation().getYaw() + 180;
                angle = baseAngle + (index - 1) * 120;
                offsetDistance = 2.5;
                break;

            case 4:
                // Cztery pety - kwadrat wokół gracza
                angle = player.getLocation().getYaw() + 45 + (index * 90);
                offsetDistance = 3.0;
                break;

            default:
                // Więcej niż 4 pety (na wszelki wypadek)
                angle = (Math.PI * 2 * index / totalPets) * (180 / Math.PI);
                offsetDistance = 3.0 + (totalPets - 4) * 0.5;
                break;
        }

        // Konwertuj kąt na radiany
        double radians = Math.toRadians(angle);

        // Oblicz offset
        double offsetX = Math.cos(radians) * offsetDistance;
        double offsetZ = Math.sin(radians) * offsetDistance;

        loc.add(offsetX, 0, offsetZ);

        // Lekko unieś pety dla lepszej widoczności
        loc.add(0, 0.1, 0);

        return loc;
    }

    // Wyczyść zapisane pozycje gdy pet jest usuwany
    public void clearPetPosition(UUID petUuid) {
        lastPetPositions.remove(petUuid);
    }

    // Wyczyść wszystkie pozycje gracza
    public void clearPlayerPets(Player player) {
        List<Pet> pets = plugin.getPetManager().getPlayerPets(player);
        for (Pet pet : pets) {
            lastPetPositions.remove(pet.getUuid());
        }
    }
}
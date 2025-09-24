package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
// Fixed imports to match actual package structure

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetLevelManager {

    private final PetPlugin plugin;
    private final Map<Integer, Double> experienceCache;

    public PetLevelManager(PetPlugin plugin) {
        this.plugin = plugin;
        this.experienceCache = new HashMap<>();
        cacheExperienceValues();
    }

    // Cache experience requirements dla optymalizacji
    private void cacheExperienceValues() {
        double baseExp = plugin.getConfig().getDouble("experience.base-exp", 5);
        double levelMultiplier = plugin.getConfig().getDouble("experience.level-multiplier", 2.2);
        double additionalPerLevel = plugin.getConfig().getDouble("experience.additional-per-level", 10);

        for (int level = 1; level <= 100; level++) {
            double required = baseExp * Math.pow(level, levelMultiplier) + (level * additionalPerLevel);
            experienceCache.put(level, required);
        }
    }

    // Pobierz wymagane doświadczenie dla poziomu
    public double getRequiredExperience(int level) {
        return experienceCache.getOrDefault(level, 99999.0);
    }

    // Oblicz całkowite doświadczenie potrzebne do poziomu
    public double getTotalExperienceForLevel(int targetLevel) {
        double total = 0;
        for (int level = 1; level < targetLevel; level++) {
            total += getRequiredExperience(level);
        }
        return total;
    }

    // Oblicz procent postępu do następnego poziomu
    public double getLevelProgress(Pet pet) {
        if (pet.getLevel() >= pet.getRarity().getMaxLevel()) {
            return 1.0; // Max level
        }

        double required = getRequiredExperience(pet.getLevel());
        return pet.getExperience() / required;
    }

    // Sprawdź czy pet może wbić poziom
    public boolean canLevelUp(Pet pet) {
        // Sprawdź max level
        if (pet.getLevel() >= pet.getRarity().getMaxLevel()) {
            return false;
        }

        // Sprawdź czy wymaga karmienia
        if (pet.needsFeeding()) {
            return false;
        }

        // Sprawdź exp
        double required = getRequiredExperience(pet.getLevel());
        return pet.getExperience() >= required;
    }

    // Procesuj level up
    public void processLevelUp(Player player, Pet pet) {
        int oldLevel = pet.getLevel();

        // Sprawdź czy może levelować
        while (canLevelUp(pet)) {
            pet.setLevel(pet.getLevel() + 1);
            pet.setExperience(pet.getExperience() - getRequiredExperience(oldLevel));

            // Aktualizuj statystyki
            plugin.getDatabaseManager().updatePetStats(pet.getUuid(), "times_leveled", 1);
        }

        int newLevel = pet.getLevel();

        if (newLevel > oldLevel) {
            // Zapisz peta
            plugin.getPetDataManager().savePet(pet);

            // Efekty wizualne dla kamieni milowych
            if (isMilestone(newLevel)) {
                celebrateMilestone(player, pet, newLevel);
            }

            // Powiadomienia o specjalnych efektach
            checkSpecialEffects(player, pet, newLevel);
        }
    }

    // Sprawdź czy poziom jest kamieniem milowym
    private boolean isMilestone(int level) {
        return level == 25 || level == 50 || level == 75 || level == 100;
    }

    // Celebruj osiągnięcie kamienia milowego
    private void celebrateMilestone(Player player, Pet pet, int level) {
        Location loc = player.getLocation();

        // Fajerwerki
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();

        Color color;
        switch (pet.getRarity()) {
            case MAGIC:
                color = Color.BLUE;
                break;
            case EXTRAORDINARY:
                color = Color.PURPLE;
                break;
            case LEGENDARY:
                color = Color.ORANGE;
                break;
            case UNIQUE:
                color = Color.YELLOW;
                break;
            case MYTHIC:
                color = Color.RED;
                break;
            default:
                color = Color.WHITE;
                break;
        }

        FireworkEffect effect = FireworkEffect.builder()
                .flicker(true)
                .withColor(color)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.STAR)
                .trail(true)
                .build();

        fwm.addEffect(effect);
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);

        // Broadcast dla mythic petów na max level
        if (pet.getRarity() == PetRarity.MYTHIC && level == 100) {
            Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() +
                    "§c§l" + player.getName() + " §ehas maxed out their §c§lMYTHIC " +
                    pet.getType().getDisplayName() + "§e! §6(Level 100)");
        }
    }

    // Sprawdź specjalne efekty
    private void checkSpecialEffects(Player player, Pet pet, int level) {
        // Special effects unlocked - no chat messages
    }

    // Pobierz mnożnik mocy dla poziomu
    public double getPowerMultiplier(int level) {
        double baseMultiplier = 1.0;

        // +1% za każdy poziom
        baseMultiplier += level * plugin.getConfig().getDouble("scaling.per-level-bonus", 0.01);

        // Mnożniki na progach
        if (level >= 25) {
            baseMultiplier *= plugin.getConfig().getDouble("scaling.level-25-multiplier", 2.0);
        }
        if (level >= 50) {
            baseMultiplier *= plugin.getConfig().getDouble("scaling.level-50-multiplier", 2.0);
        }
        if (level >= 75) {
            baseMultiplier *= plugin.getConfig().getDouble("scaling.level-75-multiplier", 2.0);
        }
        if (level >= 100) {
            baseMultiplier *= plugin.getConfig().getDouble("scaling.level-100-multiplier", 2.0);
        }

        return baseMultiplier;
    }

    // Formatuj informacje o poziomie
    public String formatLevelInfo(Pet pet) {
        StringBuilder info = new StringBuilder();

        info.append("§7Level: §f").append(pet.getLevel())
                .append("/").append(pet.getRarity().getMaxLevel());

        if (pet.getLevel() < pet.getRarity().getMaxLevel()) {
            info.append(" §7(§e")
                    .append(String.format("%.1f%%", getLevelProgress(pet) * 100))
                    .append("§7)");
        } else {
            info.append(" §6§lMAX");
        }

        return info.toString();
    }

    // Pobierz opis następnego kamienia milowego
    public String getNextMilestone(Pet pet) {
        int level = pet.getLevel();

        if (level < 25) {
            return "§7Next milestone: §6Level 25 §7(2x power boost)";
        } else if (level < 50) {
            return "§7Next milestone: §6Level 50 §7(4x total power)";
        } else if (level < 75 && (pet.getRarity() == PetRarity.UNIQUE || pet.getRarity() == PetRarity.MYTHIC)) {
            return "§7Next milestone: §6Level 75 §7(8x power + special effect)";
        } else if (level < 100 && pet.getRarity() == PetRarity.MYTHIC) {
            return "§7Next milestone: §6Level 100 §7(16x power + mythic effect)";
        } else {
            return "§6Maximum power achieved!";
        }
    }

    // Reload cache
    public void reloadCache() {
        experienceCache.clear();
        cacheExperienceValues();
    }
}
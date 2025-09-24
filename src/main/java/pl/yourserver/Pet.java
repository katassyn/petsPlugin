package pl.yourserver;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.UUID;

public class Pet {

    private final UUID uuid;
    private final UUID ownerUUID;
    private PetType type;
    private PetRarity rarity;
    private int level;
    private double experience;
    private boolean active;
    private Entity entity;
    private long lastFeedTime;
    private int feedCount;

    // Konstruktor dla nowego peta
    public Pet(UUID ownerUUID, PetType type, PetRarity rarity) {
        this.uuid = UUID.randomUUID();
        this.ownerUUID = ownerUUID;
        this.type = type;
        this.rarity = rarity;
        this.level = 1;
        this.experience = 0;
        this.active = false;
        this.lastFeedTime = System.currentTimeMillis();
        this.feedCount = 0;
    }

    // Konstruktor do wczytywania z bazy
    public Pet(UUID uuid, UUID ownerUUID, PetType type, PetRarity rarity,
               int level, double experience, boolean active, long lastFeedTime, int feedCount) {
        this.uuid = uuid;
        this.ownerUUID = ownerUUID;
        this.type = type;
        this.rarity = rarity;
        this.level = level;
        this.experience = experience;
        this.active = active;
        this.lastFeedTime = lastFeedTime;
        this.feedCount = feedCount;
    }

    // Metoda do dodawania doświadczenia
    public boolean addExperience(double amount) {
        int maxLevel = rarity.getMaxLevel();
        if (level >= maxLevel) {
            return false;
        }

        // Sprawdź czy pet wymaga karmienia przed dodaniem exp
        if (needsFeeding()) {
            // Pet wymaga karmienia - nie dodawaj exp
            return false;
        }

        experience += amount;
        double requiredExp = getRequiredExperience();

        while (experience >= requiredExp && level < maxLevel) {
            // Sprawdź czy następny level wymaga karmienia
            if (requiresFeedingBeforeLevel(level + 1) && feedCount == 0) {
                // Zatrzymaj level up - wymaga karmienia na poziomie 5, 10, 15, itd.
                experience = requiredExp;
                break;
            }

            experience -= requiredExp;
            level++;

            // Reset licznika karmienia po osiągnięciu poziomu wymagającego karmienia
            if (requiresFeedingBeforeLevel(level)) {
                feedCount = 0;
            }

            requiredExp = getRequiredExperience();
        }

        if (level >= maxLevel) {
            experience = 0;
        }

        return true;
    }

    // Obliczanie wymaganego doświadczenia do następnego poziomu
    public double getRequiredExperience() {
        // Bardzo progresywna formuła: bazowy exp * (poziom^2.2) + dodatkowy wzrost
        double baseExp = 5;
        double levelMultiplier = Math.pow(level, 2.2);
        double additionalGrowth = level * 10;

        // Dla przykładu:
        // Lv 1->2: 5 * 1^2.2 + 10 = 15 exp
        // Lv 10->11: 5 * 10^2.2 + 100 = ~258 exp
        // Lv 25->26: 5 * 25^2.2 + 250 = ~1813 exp
        // Lv 50->51: 5 * 50^2.2 + 500 = ~8788 exp
        // Lv 99->100: 5 * 99^2.2 + 990 = ~48636 exp

        return baseExp * levelMultiplier + additionalGrowth;
    }

    // Karmienie peta
    public boolean feed(int andermantAmount) {
        if (!needsFeeding()) {
            return false; // Nie wymaga karmienia
        }

        int requiredAmount = getRequiredFeedAmount();
        if (andermantAmount >= requiredAmount) {
            feedCount = 1;
            lastFeedTime = System.currentTimeMillis();

            // Automatyczny level up po nakarmieniu jeśli ma wystarczająco exp
            if (experience >= getRequiredExperience()) {
                // Ręczny level up z poprawkami
                experience -= getRequiredExperience();
                level++;

                // Reset licznika karmienia po osiągnięciu poziomu
                if (requiresFeedingBeforeLevel(level)) {
                    feedCount = 0;
                }
            }

            return true;
        }
        return false;
    }

    // Ilość Andermantu wymagana do karmienia
    public int getRequiredFeedAmount() {
        // Co 5 poziomów wymaga więcej
        return (level / 5 + 1) * 100;
    }

    // Sprawdzenie czy pet wymaga karmienia
    public boolean needsFeeding() {
        // Pet wymaga karmienia na poziomach 5, 10, 15, 20, itd.
        // Gdy ma wystarczająco exp do następnego poziomu i następny poziom to wielokrotność 5
        int nextLevel = level + 1;
        return nextLevel <= rarity.getMaxLevel()
                && feedCount == 0
                && experience >= getRequiredExperience()
                && requiresFeedingBeforeLevel(nextLevel);
    }

    private boolean requiresFeedingBeforeLevel(int targetLevel) {
        int maxLevel = rarity.getMaxLevel();
        if (targetLevel <= 0 || targetLevel > maxLevel) {
            return false;
        }
        if (targetLevel == maxLevel) {
            return true;
        }
        return targetLevel % 5 == 0;
    }


    // Nowa mechanika obliczania efektów z ustalonymi wartościami
    public double calculateEffectValue(double baseValue, double perLevelValue) {
        // Dla większości efektów: base + (level * per-level), potem +1% per level i podwojenia
        double totalValue = baseValue + (level * perLevelValue);

        // Dodanie bonusu za każdy poziom (+1%) - zaczyna się od poziomu 2
        totalValue *= (1.0 + ((level - 1) * 0.01));

        // Podwojenie mocy na progach
        if (level >= 25) totalValue *= 2.0;  // x2 na 25 lvl
        if (level >= 50) totalValue *= 2.0;  // x4 całkowicie na 50 lvl
        if (level >= 75) totalValue *= 2.0;  // x8 całkowicie na 75 lvl
        if (level >= 100) totalValue *= 2.0; // x16 całkowicie na 100 lvl

        return totalValue;
    }

    // Obliczanie efektu dla specjalnych przypadków (np. COW HP, SHEEP heal)
    public double calculateSpecialEffect(double baseValue, double perLevelValue) {
        // Dla efektów jak COW HP: base + ((level - 1) * per-level), potem podwojenia na progach
        // Pets startują na poziomie 1, więc level 1 = tylko base value
        double totalValue = baseValue + ((level - 1) * perLevelValue);

        // Podwojenie całości na progach (bez +1% per level)
        if (level >= 25) totalValue *= 2.0;
        if (level >= 50) totalValue *= 2.0;
        if (level >= 75) totalValue *= 2.0;
        if (level >= 100) totalValue *= 2.0;

        return totalValue;
    }

    // Metoda dla efektów tylko z base value (bez per-level)
    public double calculateBaseEffect(double baseValue) {
        // Dla efektów bez per-level bonusu
        double totalValue = baseValue;

        // Dodanie bonusu za każdy poziom (+1%) - zaczyna się od poziomu 2
        totalValue *= (1.0 + ((level - 1) * 0.01));

        // Podwojenie mocy na progach
        if (level >= 25) totalValue *= 2.0;
        if (level >= 50) totalValue *= 2.0;
        if (level >= 75) totalValue *= 2.0;
        if (level >= 100) totalValue *= 2.0;

        return totalValue;
    }

    // Stara metoda dla kompatybilności
    public double getEffectMultiplier() {
        double baseMultiplier = 1.0;
        baseMultiplier += (level - 1) * 0.01;

        if (level >= 25) baseMultiplier *= 2.0;
        if (level >= 50) baseMultiplier *= 2.0;
        if (level >= 75) baseMultiplier *= 2.0;
        if (level >= 100) baseMultiplier *= 2.0;

        return baseMultiplier;
    }

    // Sprawdzenie czy pet ma specjalny efekt
    public boolean hasSpecialEffect() {
        return (rarity == PetRarity.UNIQUE && level >= 75) ||
                (rarity == PetRarity.MYTHIC && level >= 100);
    }

    // Gettery i Settery
    public UUID getUuid() {
        return uuid;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public PetType getType() {
        return type;
    }

    public PetRarity getRarity() {
        return rarity;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.min(level, rarity.getMaxLevel());
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public long getLastFeedTime() {
        return lastFeedTime;
    }

    public int getFeedCount() {
        return feedCount;
    }

    public double getExperienceProgress() {
        return experience / getRequiredExperience();
    }
}
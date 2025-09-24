// PetRarity.java
package pl.yourserver;

public enum PetRarity {
    COMMON("&7Common", "&7", 10),
    UNCOMMON("&aUncommon", "&a", 15),
    MAGIC("&9Magic", "&9", 25), // Magic pets start here - renamed from RARE
    EXTRAORDINARY("&5Extraordinary", "&5", 40),
    LEGENDARY("&6Legendary", "&6", 50),
    UNIQUE("&e&lUnique", "&e", 75),
    MYTHIC("&c&lMYTHIC", "&c&l", 100);

    private final String displayName;
    private final String color;
    private final int maxLevel;

    PetRarity(String displayName, String color, int maxLevel) {
        this.displayName = displayName;
        this.color = color;
        this.maxLevel = maxLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getColoredName() {
        return color + displayName;
    }
}
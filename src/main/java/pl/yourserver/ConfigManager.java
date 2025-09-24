package pl.yourserver;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
// import pl.yourserver.petplugin.PetPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {

    private final PetPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration petsConfig;
    private File petsFile;

    // Config values cache
    private boolean debug;
    private double expPerKill;
    private double expMultiplier;
    private int maxPetsDefault;
    private double followDistance;
    private double teleportDistance;

    // HeadDatabase integration
    private boolean headDatabaseEnabled;
    private Map<PetType, String> headDatabaseIds = new EnumMap<>(PetType.class);

    // Effect values
    private double cowBonusHealth;
    private double pigMoneyChance;
    private double sheepHealAmount;
    private double squidChestBonus;
    private double turtleDamageReduction;
    private double llamaMobDamage;
    private double endermanFreeTpChance;
    private double witchPotionDuration;
    private double huskMobsphereChance;
    private double mooshroomFarmSpeed;
    private double frogHoneySpeed;
    private double wolfPvpDamage;
    private double beeHoneyQuality;
    private double beeRareHoneyChance;
    private double traderCostReduction;
    private double pandaYieldBonus;
    private double dungeonDamageBonus;
    private double phantomTreasureChance;
    private double glowSquidRareOreChance;
    private double guardianFishingBonus;
    private double snifferMiningBonus;
    private double witherSkeletonSpecialChance;
    private double witherBossDamage;
    private double witherSpecialDamage;
    private int witherSpecialDuration;
    private double giantMythicChance;
    private double ironGolemDamageMultiplier;

    // Boss detection configuration
    private List<String> bossList = new ArrayList<>();
    private Map<String, Integer> mythicBossXp = new HashMap<>();

    public ConfigManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Main config
        config = plugin.getConfig();

        // Custom pets config
        createPetsConfig();

        // Cache values
        loadCachedValues();
    }

    private void createPetsConfig() {
        petsFile = new File(plugin.getDataFolder(), "pets.yml");
        if (!petsFile.exists()) {
            petsFile.getParentFile().mkdirs();
            plugin.saveResource("pets.yml", false);
        }

        petsConfig = YamlConfiguration.loadConfiguration(petsFile);
    }

    private void loadCachedValues() {
        // General settings
        debug = config.getBoolean("debug", false);
        expPerKill = config.getDouble("settings.exp-per-kill", 1.0);
        expMultiplier = config.getDouble("settings.exp-multiplier", 1.0);
        maxPetsDefault = config.getInt("settings.max-pets-default", 1);
        followDistance = config.getDouble("settings.follow-distance", 10.0);
        teleportDistance = config.getDouble("settings.teleport-distance", 20.0);

        headDatabaseEnabled = config.getBoolean("head-database.enabled", true);
        headDatabaseIds = new EnumMap<>(PetType.class);
        ConfigurationSection headSection = config.getConfigurationSection("head-database.ids");
        for (PetType petType : PetType.values()) {
            String id = headSection != null ? headSection.getString(petType.name(), "") : "";
            headDatabaseIds.put(petType, id != null ? id : "");
        }

        // Boss configuration
        loadBossConfiguration();

        // Effect configuration
        loadEffectValues();
    }

    private void loadBossConfiguration() {
        bossList = new ArrayList<>();
        mythicBossXp = new HashMap<>();

        if (config.isList("bosses")) {
            bossList.addAll(config.getStringList("bosses"));
            return;
        }

        ConfigurationSection bossSection = config.getConfigurationSection("bosses");
        if (bossSection == null) {
            return;
        }

        List<String> customNames = bossSection.getStringList("custom-names");
        if (customNames == null || customNames.isEmpty()) {
            customNames = bossSection.getStringList("display-names");
        }
        if (customNames != null && !customNames.isEmpty()) {
            bossList.addAll(customNames);
        }

        ConfigurationSection mythicSection = bossSection.getConfigurationSection("mythic-mobs");
        if (mythicSection != null) {
            for (String key : mythicSection.getKeys(false)) {
                String lowered = key.toLowerCase(Locale.ROOT);
                ConfigurationSection mobSection = mythicSection.getConfigurationSection(key);
                int xpPerMob = mobSection != null ? mobSection.getInt("xp-per-mob", 0) : mythicSection.getInt(key, 0);
                mythicBossXp.put(lowered, xpPerMob);
            }
        }
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        petsConfig = YamlConfiguration.loadConfiguration(petsFile);
        loadCachedValues();
    }

    public void savePetsConfig() {
        try {
            petsConfig.save(petsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pets.yml!");
            e.printStackTrace();
        }
    }

    // Getters
    public boolean isDebug() {
        return debug;
    }

    public double getExpPerKill() {
        return expPerKill;
    }

    public double getExpMultiplier() {
        return expMultiplier;
    }

    public int getMaxPetsDefault() {
        return maxPetsDefault;
    }

    public double getFollowDistance() {
        return followDistance;
    }

    public double getTeleportDistance() {
        return teleportDistance;
    }

    public boolean isHeadDatabaseEnabled() {
        return headDatabaseEnabled;
    }

    public String getHeadDatabaseId(PetType petType) {
        return headDatabaseIds.getOrDefault(petType, "");
    }

    public double getCowBonusHealth() {
        return cowBonusHealth;
    }

    public double getPigMoneyChance() {
        return pigMoneyChance;
    }

    public double getSheepHealAmount() {
        return sheepHealAmount;
    }

    public double getSquidChestBonus() {
        return squidChestBonus;
    }

    public double getTurtleDamageReduction() {
        return turtleDamageReduction;
    }

    public double getLlamaMobDamage() {
        return llamaMobDamage;
    }

    public double getEndermanFreeTpChance() {
        return endermanFreeTpChance;
    }

    public double getWitchPotionDuration() {
        return witchPotionDuration;
    }

    public double getHuskMobsphereChance() {
        return huskMobsphereChance;
    }

    public double getMooshroomFarmSpeed() {
        return mooshroomFarmSpeed;
    }

    public double getFrogHoneySpeed() {
        return frogHoneySpeed;
    }

    public double getWolfPvpDamage() {
        return wolfPvpDamage;
    }

    public double getBeeHoneyQuality() {
        return beeHoneyQuality;
    }

    public double getBeeRareHoneyChance() {
        return beeRareHoneyChance;
    }

    public double getTraderCostReduction() {
        return traderCostReduction;
    }

    public double getPandaYieldBonus() {
        return pandaYieldBonus;
    }

    public double getDungeonDamageBonus() {
        return dungeonDamageBonus;
    }

    public double getPhantomTreasureChance() {
        return phantomTreasureChance;
    }

    public double getGlowSquidRareOreChance() {
        return glowSquidRareOreChance;
    }

    public double getGuardianFishingBonus() {
        return guardianFishingBonus;
    }

    public double getSnifferMiningBonus() {
        return snifferMiningBonus;
    }

    public double getWitherSkeletonSpecialChance() {
        return witherSkeletonSpecialChance;
    }

    public double getWitherBossDamage() {
        return witherBossDamage;
    }

    public double getWitherSpecialDamage() {
        return witherSpecialDamage;
    }

    public int getWitherSpecialDuration() {
        return witherSpecialDuration;
    }

    public double getGiantMythicChance() {
        return giantMythicChance;
    }

    public double getIronGolemDamageMultiplier() {
        return ironGolemDamageMultiplier;
    }

    public List<String> getBossList() {
        return bossList;
    }

    public boolean isMythicBoss(String internalName) {
        if (internalName == null) {
            return false;
        }
        return mythicBossXp.containsKey(internalName.toLowerCase(Locale.ROOT));
    }

    private void loadEffectValues() {
        cowBonusHealth = getEffectBase("COW", "health-bonus", 10.0);
        pigMoneyChance = getEffectBase("PIG", "money-chance", 5.0);
        sheepHealAmount = getEffectBase("SHEEP", "heal-amount", 10.0);
        squidChestBonus = getEffectBase("SQUID", "fishing-chest-bonus", 15.0);
        turtleDamageReduction = getEffectBase("TURTLE", "mob-damage-reduction", 1.0);
        llamaMobDamage = getEffectBase("LLAMA", "mob-damage-bonus", 2.0);
        endermanFreeTpChance = getEffectBase("ENDERMAN", "free-tp-chance", 7.5);
        witchPotionDuration = getEffectBase("WITCH", "potion-duration", 15.0);
        huskMobsphereChance = getEffectBase("HUSK", "mobsphere-chance", 12.0);
        mooshroomFarmSpeed = getEffectBase("MOOSHROOM", "farm-speed", 20.0);
        frogHoneySpeed = getEffectBase("FROG", "honey-speed", 15.0);
        wolfPvpDamage = getEffectBase("WOLF", "pvp-damage", 5.0);
        beeHoneyQuality = getEffectBase("BEE", "honey-quality", 25.0);
        beeRareHoneyChance = getSpecialValue("BEE", "double-honey-chance", 25.0);
        traderCostReduction = getEffectBase("WANDERING_TRADER", "craft-cost-reduction", 1.43);
        pandaYieldBonus = getEffectBase("PANDA", "farm-storage", 30.0);
        dungeonDamageBonus = getEffectBase("ZOMBIE", "dungeon-damage", 3.0);
        phantomTreasureChance = getEffectBase("PHANTOM", "ocean-treasure", 10.0);
        glowSquidRareOreChance = getEffectBase("GLOW_SQUID", "rare-ore-chance", 18.0);
        guardianFishingBonus = getEffectBase("GUARDIAN", "all-fishing", 10.0);
        snifferMiningBonus = getEffectBase("SNIFFER", "all-mining", 18.0);
        witherSkeletonSpecialChance = getEffectBase("WITHER_SKELETON", "special-materials", 6.25);
        witherBossDamage = getEffectBase("WITHER", "boss-damage", 5.0);
        witherSpecialDamage = getSpecialValue("WITHER", "wither-damage", 250.0);
        witherSpecialDuration = (int) Math.round(getSpecialValue("WITHER", "wither-duration", 5.0));
        giantMythicChance = getEffectBase("GIANT", "mythic-items", 5.0);
        ironGolemDamageMultiplier = getEffectBase("IRON_GOLEM", "power-strike", 2.0);
    }

    private double getEffectBase(String petKey, String effectKey, double defaultValue) {
        if (petsConfig == null) {
            return defaultValue;
        }

        String effectPath = "pets." + petKey + ".effects." + effectKey;
        if (petsConfig.contains(effectPath + ".base")) {
            return petsConfig.getDouble(effectPath + ".base", defaultValue);
        }

        return petsConfig.getDouble(effectPath, defaultValue);
    }

    private double getSpecialValue(String petKey, String key, double defaultValue) {
        if (petsConfig == null) {
            return defaultValue;
        }

        String path = "pets." + petKey + ".special-effect." + key;
        return petsConfig.getDouble(path, defaultValue);
    }

    public int getMythicBossXp(String internalName) {
        if (internalName == null) {
            return 0;
        }
        return mythicBossXp.getOrDefault(internalName.toLowerCase(Locale.ROOT), 0);
    }

    public boolean hasMythicBosses() {
        return !mythicBossXp.isEmpty();
    }

    // Messages
    public String getMessage(String key) {
        return config.getString("messages." + key, "&cMessage not found: " + key);
    }

    public String getPrefix() {
        return config.getString("messages.prefix", "&8[&6Pets&8] &7");
    }

    // Sounds
    public String getSound(String key) {
        return config.getString("sounds." + key, "UI_BUTTON_CLICK");
    }

    // Particles
    public String getParticle(String key) {
        return config.getString("particles." + key, "VILLAGER_HAPPY");
    }

    public FileConfiguration getPetsConfig() {
        return petsConfig;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}

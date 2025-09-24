package pl.yourserver;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
// Fixed imports to match actual package structure
// import pl.yourserver.petplugin.PetPlugin;
// import pl.yourserver.petplugin.models.Pet;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IntegrationManager extends PlaceholderExpansion {

    private final PetPlugin plugin;
    private final Map<String, Double> placeholderValues = new HashMap<>();
    private final Map<UUID, Map<String, Double>> playerPlaceholders = new HashMap<>();

    // Plugin instances
    private Object vaultPlugin;
    private Object fishingPlugin;
    private Object dungeonPlugin;
    private Object alchemyPlugin;
    private Object minePlugin;
    private Object farmingPlugin;
    private Object beekeeperPlugin;
    private Object craftingPlugin;
    private Object mythicMobsPlugin;
    private Object ingredientPouchPlugin;

    // API instances
    private Object pouchAPI;
    private boolean libsDisguisesEnabled;

    public IntegrationManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadIntegrations() {
        // Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
            plugin.getLogger().info("§aVault integration loaded!");
        }

        // FishingPlugin
        if (Bukkit.getPluginManager().getPlugin("FishingPlugin") != null) {
            fishingPlugin = Bukkit.getPluginManager().getPlugin("FishingPlugin");
            plugin.getLogger().info("§aFishingPlugin integration loaded!");
        }

        // MyDungeonTeleportPlugin
        if (Bukkit.getPluginManager().getPlugin("MyDungeonTeleportPlugin") != null) {
            dungeonPlugin = Bukkit.getPluginManager().getPlugin("MyDungeonTeleportPlugin");
            plugin.getLogger().info("§aMyDungeonTeleportPlugin integration loaded!");
        }

        // MyExperiencePlugin (Alchemy)
        if (Bukkit.getPluginManager().getPlugin("MyExperiencePlugin") != null) {
            alchemyPlugin = Bukkit.getPluginManager().getPlugin("MyExperiencePlugin");
            plugin.getLogger().info("§aMyExperiencePlugin (Alchemy) integration loaded!");
        }

        // mineSystemPlugin
        if (Bukkit.getPluginManager().getPlugin("mineSystemPlugin") != null) {
            minePlugin = Bukkit.getPluginManager().getPlugin("mineSystemPlugin");
            plugin.getLogger().info("§amineSystemPlugin integration loaded!");
        }

        // farmingPlugin
        if (Bukkit.getPluginManager().getPlugin("farmingPlugin") != null) {
            farmingPlugin = Bukkit.getPluginManager().getPlugin("farmingPlugin");
            plugin.getLogger().info("§afarmingPlugin integration loaded!");
        }

        // beesPlugin (corrected name)
        if (Bukkit.getPluginManager().getPlugin("beesPlugin") != null) {
            beekeeperPlugin = Bukkit.getPluginManager().getPlugin("beesPlugin");
            plugin.getLogger().info("§abeesPlugin integration loaded!");
        }

        // MyCraftingPlugin2
        if (Bukkit.getPluginManager().getPlugin("MyCraftingPlugin2") != null) {
            craftingPlugin = Bukkit.getPluginManager().getPlugin("MyCraftingPlugin2");
            plugin.getLogger().info("§aMyCraftingPlugin2 integration loaded!");
        }

        // MythicMobs
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            mythicMobsPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
            plugin.getLogger().info("§aMythicMobs integration loaded!");
        }

        // IngredientPouchPlugin (using API)
        if (Bukkit.getPluginManager().getPlugin("IngredientPouchPlugin") != null) {
            ingredientPouchPlugin = Bukkit.getPluginManager().getPlugin("IngredientPouchPlugin");
            try {
                // Get the PouchAPI instance through reflection
                Class<?> pouchClass = ingredientPouchPlugin.getClass();
                Method getAPIMethod = pouchClass.getMethod("getAPI");
                pouchAPI = getAPIMethod.invoke(ingredientPouchPlugin);
                plugin.getLogger().info("§aIngredientPouchPlugin integration loaded with API!");
            } catch (Exception e) {
                plugin.getLogger().warning("§cFailed to load IngredientPouchPlugin API: " + e.getMessage());
                pouchAPI = null;
            }
        }

        // LibsDisguises
        libsDisguisesEnabled = Bukkit.getPluginManager().getPlugin("LibsDisguises") != null;
        if (libsDisguisesEnabled) {
            plugin.getLogger().info("§aLibsDisguises integration loaded!");
        } else {
            plugin.getLogger().warning("§cLibsDisguises plugin not found! Pet disguises will be unavailable.");
        }

        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.register();
            plugin.getLogger().info("§aPlaceholderAPI integration loaded!");
        }
    }

    public boolean isLibsDisguisesEnabled() {
        return libsDisguisesEnabled;
    }

    // PlaceholderAPI methods
    @Override
    public @NotNull String getIdentifier() {
        return "pet";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        Map<String, Double> playerValues = playerPlaceholders.get(player.getUniqueId());
        if (playerValues == null) {
            updatePlayerPlaceholders(player);
            playerValues = playerPlaceholders.get(player.getUniqueId());
        }

        // Podstawowe placeholdery
        switch (identifier) {
            case "active_count":
                return String.valueOf(plugin.getPetManager().getActivePetCount(player));
            case "max_slots":
                return String.valueOf(plugin.getPetManager().getMaxPetSlots(player));
            case "total_pets":
                return String.valueOf(plugin.getPetManager().getPlayerPets(player).size());
        }

        // Placeholdery efektów petów
        if (playerValues != null && playerValues.containsKey(identifier)) {
            return String.format("%.2f", playerValues.get(identifier));
        }

        // Placeholdery dla integracji
        if (identifier.startsWith("effect_")) {
            String effect = identifier.substring(7);
            Double value = playerValues != null ? playerValues.get(effect) : 0.0;
            return value != null ? String.format("%.2f", value) : "0";
        }

        return "0";
    }

    // Aktualizuj placeholdery gracza
    public void updatePlayerPlaceholders(Player player) {
        Map<String, Double> values = new HashMap<>();
        List<Pet> activePets = plugin.getPetManager().getActivePets(player);

        for (Pet pet : activePets) {
            double multiplier = pet.getEffectMultiplier();

            switch (pet.getType()) {
                // MAGIC Pets
                case PIG:
                    values.put("pig_money_chance", plugin.getConfigManager().getPigMoneyChance() * multiplier);
                    break;

                // EXTRAORDINARY Pets
                case DONKEY:
                    values.put("donkey_extra_storage", 1.0);
                    break;

                case IRON_GOLEM:
                    values.put("irongolem_damage_boost", plugin.getConfigManager().getIronGolemDamageMultiplier() * multiplier);
                    break;

                // LEGENDARY Pets
                case SQUID:
                    values.put("fishing_chest_bonus", plugin.getConfigManager().getSquidChestBonus() * multiplier);
                    break;

                case TURTLE:
                    values.put("turtle_damage_reduction", plugin.getConfigManager().getTurtleDamageReduction() * multiplier);
                    break;

                case LLAMA:
                    values.put("llama_mob_damage", plugin.getConfigManager().getLlamaMobDamage() * multiplier);
                    break;

                case ENDERMAN:
                    values.put("dungeon_free_tp_chance", plugin.getConfigManager().getEndermanFreeTpChance() * multiplier);
                    break;

                case WITCH:
                    values.put("alchemy_potion_duration_bonus", plugin.getConfigManager().getWitchPotionDuration() * multiplier);
                    break;

                case HUSK:
                    values.put("mine_mobsphere_chance", plugin.getConfigManager().getHuskMobsphereChance() * multiplier);
                    break;

                case MOOSHROOM:
                    values.put("farm_growth_speed", plugin.getConfigManager().getMooshroomFarmSpeed() * multiplier);
                    break;

                case FROG:
                    values.put("beekeeper_honey_speed", plugin.getConfigManager().getFrogHoneySpeed() * multiplier);
                    break;

                // UNIQUE Pets
                case WOLF:
                    values.put("wolf_pvp_damage", plugin.getConfigManager().getWolfPvpDamage() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("wolf_pvp_lifesteal", 0.15);
                    }
                    break;

                case BEE:
                    values.put("beekeeper_honey_quality", plugin.getConfigManager().getBeeHoneyQuality() * multiplier);
                    values.put("beekeeper_rare_honey_chance", plugin.getConfigManager().getBeeRareHoneyChance() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("beekeeper_auto_collect", 1.0);
                    }
                    break;

                case WANDERING_TRADER:
                    values.put("crafting_cost_reduction", plugin.getConfigManager().getTraderCostReduction() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("crafting_refund_chance", 0.10);
                    }
                    break;

                case PANDA:
                    values.put("farm_yield_bonus", plugin.getConfigManager().getPandaYieldBonus() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("farm_double_harvest_chance", 0.25);
                    }
                    break;

                // Dungeon damage pets
                case ZOMBIE:
                    values.put("dungeon_q1_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    values.put("dungeon_q3_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    break;

                case SKELETON:
                    values.put("dungeon_q6_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    values.put("dungeon_q7_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    break;

                case SPIDER:
                    values.put("dungeon_q2_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    values.put("dungeon_q4_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    break;

                case CREEPER:
                    values.put("dungeon_q5_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    values.put("dungeon_q8_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    break;

                case SLIME:
                    values.put("dungeon_q9_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    values.put("dungeon_q10_damage", plugin.getConfigManager().getDungeonDamageBonus() * multiplier);
                    break;

                case PHANTOM:
                    values.put("fishing_ocean_treasure_chance", plugin.getConfigManager().getPhantomTreasureChance() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("fishing_water_walking", 1.0);
                    }
                    break;

                case GLOW_SQUID:
                    values.put("mine_rare_ore_chance", plugin.getConfigManager().getGlowSquidRareOreChance() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("mine_xray_vision", 1.0);
                    }
                    break;

                // MYTHIC Pets
                case GUARDIAN:
                    values.put("fishing_all_bonuses", plugin.getConfigManager().getGuardianFishingBonus() * multiplier);
                    values.put("fishing_ocean_treasure_chance", 30.0 * multiplier);
                    values.put("fishing_chest_bonus", 25.0 * multiplier);
                    values.put("fishing_rune_chance", 15.0 * multiplier);
                    values.put("fishing_map_chance", 20.0 * multiplier);
                    break;

                case SNIFFER:
                    values.put("mine_all_bonuses", plugin.getConfigManager().getSnifferMiningBonus() * multiplier);
                    values.put("mine_mob_spawn_chance", 25.0 * multiplier);
                    values.put("mine_rare_ore_chance", 30.0 * multiplier);
                    values.put("mine_rare_sphere_chance", 35.0 * multiplier);
                    break;

                case WITHER_SKELETON:
                    values.put("farm_special_material_chance", plugin.getConfigManager().getWitherSkeletonSpecialChance() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("farm_guaranteed_rare", 1.0);
                    }
                    break;

                case ENDER_DRAGON:
                    values.put("pet_slot_bonus", 1.0);
                    values.put("effect_duplicate", 1.0);
                    break;

                case WARDEN:
                    values.put("pet_slot_bonus", 2.0);
                    break;

                case WITHER:
                    values.put("mythicmobs_boss_damage", plugin.getConfigManager().getWitherBossDamage() * multiplier);
                    if (pet.hasSpecialEffect()) {
                        values.put("mythicmobs_boss_wither", 1.0);
                    }
                    break;

                case GIANT:
                    values.put("mythicmobs_item_chance", plugin.getConfigManager().getGiantMythicChance() * multiplier);
                    values.put("mythicmobs_no_normal_drops", 1.0);
                    break;
            }
        }

        playerPlaceholders.put(player.getUniqueId(), values);
    }

    // Ustaw wartość placeholdera (dla kompatybilności wstecznej)
    public void setPlaceholder(String key, double value) {
        placeholderValues.put(key, value);
    }

    // Sprawdź Andermant w IngredientPouch przez API
    public int getAndermantFromPouch(Player player) {
        if (pouchAPI == null) return 0;

        try {
            Class<?> apiClass = pouchAPI.getClass();
            Method getQuantityMethod = apiClass.getMethod("getItemQuantity", String.class, String.class);
            Object result = getQuantityMethod.invoke(pouchAPI, player.getUniqueId().toString(), "andermant");
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("§cError getting andermant from pouch: " + e.getMessage());
            }
        }

        return 0;
    }

    // Usuń Andermant z IngredientPouch przez API
    public boolean removeAndermantFromPouch(Player player, int amount) {
        if (pouchAPI == null) return false;

        try {
            Class<?> apiClass = pouchAPI.getClass();
            Method updateQuantityMethod = apiClass.getMethod("updateItemQuantity", String.class, String.class, int.class);
            Object result = updateQuantityMethod.invoke(pouchAPI, player.getUniqueId().toString(), "andermant", -amount);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("§cError removing andermant from pouch: " + e.getMessage());
            }
        }

        return false;
    }

    // Dodaj Andermant do IngredientPouch przez API (utility method)
    public boolean addAndermantToPouch(Player player, int amount) {
        if (pouchAPI == null) return false;

        try {
            Class<?> apiClass = pouchAPI.getClass();
            Method updateQuantityMethod = apiClass.getMethod("updateItemQuantity", String.class, String.class, int.class);
            Object result = updateQuantityMethod.invoke(pouchAPI, player.getUniqueId().toString(), "andermant", amount);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("§cError adding andermant to pouch: " + e.getMessage());
            }
        }

        return false;
    }

    // Sprawdź czy gracz ma otwarte GUI paucha
    public boolean hasPouchOpen(Player player) {
        if (pouchAPI == null) return false;

        try {
            Class<?> apiClass = pouchAPI.getClass();
            Method hasPouchOpenMethod = apiClass.getMethod("hasPouchOpen", Player.class);
            Object result = hasPouchOpenMethod.invoke(pouchAPI, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("§cError checking pouch GUI: " + e.getMessage());
            }
        }

        return false;
    }

    // Wyczyść placeholdery gracza
    public void clearPlayerPlaceholders(Player player) {
        playerPlaceholders.remove(player.getUniqueId());
    }

    // Sprawdź czy MythicMobs jest załadowany
    public boolean isMythicMobsLoaded() {
        return mythicMobsPlugin != null;
    }

    // Pobierz instancję MythicMobs
    public Object getMythicMobsPlugin() {
        return mythicMobsPlugin;
    }
}
package pl.yourserver;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import java.util.List;

public class PetPlaceholderExpansion extends PlaceholderExpansion {

    private final PetPlugin plugin;

    public PetPlaceholderExpansion(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "petplugin";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        List<Pet> activePets = plugin.getPetManager().getActivePets(player);
        if (activePets.isEmpty()) return "0";

        // COW placeholders
        if (params.equals("cow_health")) {
            return String.valueOf(getCowHealthBonus(activePets));
        }

        // PIG placeholders
        if (params.equals("pig_money_chance")) {
            return String.valueOf(getPigMoneyChance(activePets));
        }
        if (params.equals("pig_money_min")) {
            return "100";
        }
        if (params.equals("pig_money_max")) {
            return "200";
        }

        // SHEEP placeholders
        if (params.equals("sheep_heal_amount")) {
            return String.valueOf(getSheepHealAmount(activePets));
        }

        // CHICKEN placeholders
        if (params.equals("chicken_luck")) {
            return String.valueOf(getChickenLuck(activePets));
        }

        // DONKEY placeholders
        if (params.equals("donkey_extra_storage")) {
            return String.valueOf(getDonkeyStorage(activePets));
        }

        // IRON_GOLEM placeholders
        if (params.equals("irongolem_damage_boost")) {
            return String.valueOf(getIronGolemDamageBoost(activePets));
        }

        // LEGENDARY pets placeholders
        if (params.equals("fishing_chest_bonus") || params.equals("fisherman_chest_chance")) {
            return String.valueOf(getFishingChestBonus(activePets));
        }
        if (params.equals("turtle_damage_reduction")) {
            return String.valueOf(getTurtleDamageReduction(activePets));
        }
        if (params.equals("llama_mob_damage")) {
            return String.valueOf(getLlamaMobDamage(activePets));
        }
        if (params.equals("dungeon_free_tp_chance")) {
            return String.valueOf(getDungeonFreeTpChance(activePets));
        }
        if (params.equals("alchemy_potion_duration_bonus")) {
            return String.valueOf(getPotionDurationBonus(activePets));
        }
        if (params.equals("mine_mobsphere_chance")) {
            return String.valueOf(getMobsphereChance(activePets));
        }
        if (params.equals("farm_growth_speed")) {
            return String.valueOf(getFarmGrowthSpeed(activePets));
        }
        if (params.equals("beekeeper_honey_speed")) {
            return String.valueOf(getHoneySpeed(activePets));
        }

        // UNIQUE pets placeholders
        if (params.equals("wolf_pvp_damage")) {
            return String.valueOf(getWolfPvpDamage(activePets));
        }
        if (params.equals("wolf_pvp_damage_reduction")) {
            return String.valueOf(getWolfPvpDamageReduction(activePets));
        }
        if (params.equals("wolf_pvp_lifesteal")) {
            return String.valueOf(getWolfPvpLifesteal(activePets));
        }
        if (params.equals("beekeeper_honey_quality")) {
            return String.valueOf(getHoneyQuality(activePets));
        }
        if (params.equals("beekeeper_double_honey")) {
            return String.valueOf(getDoubleHoneyChance(activePets));
        }
        if (params.equals("crafting_cost_reduction")) {
            return String.valueOf(getCraftingCostReduction(activePets));
        }
        if (params.equals("crafting_refund_chance")) {
            return String.valueOf(getCraftingRefundChance(activePets));
        }
        if (params.equals("farm_storage_bonus")) {
            return String.valueOf(getFarmStorageBonus(activePets));
        }
        if (params.equals("farm_double_harvest_chance")) {
            return String.valueOf(getFarmDoubleHarvest(activePets));
        }

        // Dungeon damage placeholders
        if (params.startsWith("dungeon_q") && params.endsWith("_damage")) {
            return String.valueOf(getDungeonDamage(activePets, params));
        }
        if (params.equals("dungeon_execute_threshold")) {
            return String.valueOf(getDungeonExecuteThreshold(activePets));
        }

        // Fishing placeholders
        if (params.equals("fishing_ocean_treasure_chance") || params.equals("ocean_treasure_chance")) {
            return String.valueOf(getOceanTreasureChance(activePets));
        }
        if (params.equals("fishing_rune_chance") || params.equals("rune_chance")) {
            return String.valueOf(getFishingRuneChance(activePets));
        }
        if (params.equals("fishing_map_chance") || params.equals("treasure_map_chance")) {
            return String.valueOf(getFishingMapChance(activePets));
        }
        if (params.equals("fishing_rune_bonus")) {
            return String.valueOf(getRuneBonus(activePets));
        }

        // Mining placeholders
        if (params.equals("mine_rare_ore_chance") || params.equals("legendary_ore_chance")) {
            return String.valueOf(getRareOreChance(activePets));
        }
        if (params.equals("mine_ore_duplication") || params.equals("ore_duplication_chance")) {
            return String.valueOf(getOreDuplication(activePets));
        }
        if (params.equals("mine_rare_sphere_chance") || params.equals("sphere_double_chance")) {
            return String.valueOf(getMineRareSphereChance(activePets));
        }

        // MYTHIC pets placeholders
        if (params.equals("fishing_all_bonuses")) {
            return String.valueOf(getAllFishingBonuses(activePets));
        }
        if (params.equals("fishing_sell_multiplier")) {
            return String.valueOf(getFishingSellMultiplier(activePets));
        }
        if (params.equals("mine_all_bonuses")) {
            return String.valueOf(getAllMiningBonuses(activePets));
        }
        if (params.equals("mine_ore_sell_multiplier")) {
            return String.valueOf(getOreSellMultiplier(activePets));
        }
        if (params.equals("farm_special_material_chance")) {
            return String.valueOf(getSpecialMaterialChance(activePets));
        }
        if (params.equals("farm_fruit_sell_multiplier")) {
            return String.valueOf(getFruitSellMultiplier(activePets));
        }
        if (params.equals("pet_slot_bonus")) {
            return String.valueOf(getPetSlotBonus(activePets));
        }
        if (params.equals("boss_damage_bonus")) {
            return String.valueOf(getBossDamageBonus(activePets));
        }
        if (params.equals("boss_wither_damage")) {
            return String.valueOf(getBossWitherDamage(activePets));
        }
        if (params.equals("boss_wither_duration")) {
            return String.valueOf(getBossWitherDuration(activePets));
        }
        if (params.equals("mythic_item_chance")) {
            return String.valueOf(getMythicItemChance(activePets));
        }
        if (params.equals("mythic_item_double_chance")) {
            return String.valueOf(getMythicItemDoubleChance(activePets));
        }

        return null;
    }

    // Helper methods for calculating pet effects
    private double getCowHealthBonus(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.COW) {
                return pet.calculateSpecialEffect(12.0, 2.0);
            }
        }
        return 0;
    }

    private double getPigMoneyChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.PIG) {
                return pet.calculateBaseEffect(5.0);
            }
        }
        return 0;
    }

    private double getSheepHealAmount(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.SHEEP) {
                return pet.calculateSpecialEffect(12.0, 2.0);
            }
        }
        return 0;
    }

    private double getChickenLuck(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.CHICKEN) {
                return pet.calculateEffectValue(0.0, 0.25);
            }
        }
        return 0;
    }

    private int getDonkeyStorage(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.DONKEY) {
                return pet.getLevel() >= 25 ? 27 : 9;
            }
        }
        return 0;
    }

    private double getIronGolemDamageBoost(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.IRON_GOLEM) {
                return pet.calculateBaseEffect(2.0);
            }
        }
        return 0;
    }

    // LEGENDARY pets methods
    private double getFishingChestBonus(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.SQUID) {
                return pet.calculateBaseEffect(15.0);
            }
        }
        return 0;
    }

    private double getFishingRuneChance(List<Pet> pets) {
        return getAllFishingBonuses(pets);
    }

    private double getFishingMapChance(List<Pet> pets) {
        return getAllFishingBonuses(pets);
    }

    private double getTurtleDamageReduction(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.TURTLE) {
                return pet.calculateBaseEffect(1.0);
            }
        }
        return 0;
    }

    private double getLlamaMobDamage(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.LLAMA) {
                return pet.calculateBaseEffect(2.0);
            }
        }
        return 0;
    }

    private double getDungeonFreeTpChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.ENDERMAN) {
                return pet.calculateBaseEffect(7.5);
            }
        }
        return 0;
    }

    private double getPotionDurationBonus(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WITCH) {
                return pet.calculateBaseEffect(15.0);
            }
        }
        return 0;
    }

    private double getMobsphereChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.HUSK) {
                return pet.calculateBaseEffect(12.0);
            }
        }
        return 0;
    }

    private double getFarmGrowthSpeed(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.MOOSHROOM) {
                return pet.calculateBaseEffect(20.0);
            }
        }
        return 0;
    }

    private double getHoneySpeed(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.FROG) {
                return pet.calculateBaseEffect(15.0);
            }
        }
        return 0;
    }

    // UNIQUE pets methods
    private double getWolfPvpDamage(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WOLF) {
                return pet.calculateBaseEffect(5.0);
            }
        }
        return 0;
    }

    private double getWolfPvpDamageReduction(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WOLF) {
                return pet.calculateBaseEffect(5.0);
            }
        }
        return 0;
    }

    private double getWolfPvpLifesteal(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WOLF && pet.hasSpecialEffect()) {
                return 15.0;
            }
        }
        return 0;
    }

    private double getHoneyQuality(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.BEE) {
                return pet.calculateBaseEffect(25.0);
            }
        }
        return 0;
    }

    private double getDoubleHoneyChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.BEE && pet.hasSpecialEffect()) {
                return 25.0;
            }
        }
        return 0;
    }

    private double getCraftingCostReduction(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WANDERING_TRADER) {
                return pet.calculateBaseEffect(1.43);
            }
        }
        return 0;
    }

    private double getCraftingRefundChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WANDERING_TRADER && pet.hasSpecialEffect()) {
                return 5.0;
            }
        }
        return 0;
    }

    private double getFarmStorageBonus(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.PANDA) {
                return pet.calculateBaseEffect(30.0);
            }
        }
        return 0;
    }

    private double getFarmDoubleHarvest(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.PANDA && pet.hasSpecialEffect()) {
                return 25.0;
            }
        }
        return 0;
    }

    private double getDungeonDamage(List<Pet> pets, String param) {
        for (Pet pet : pets) {
            switch (pet.getType()) {
                case ZOMBIE:
                    if (param.equals("dungeon_q1_damage") || param.equals("dungeon_q3_damage")) {
                        return pet.calculateBaseEffect(3.0);
                    }
                    break;
                case SKELETON:
                    if (param.equals("dungeon_q6_damage") || param.equals("dungeon_q7_damage")) {
                        return pet.calculateBaseEffect(3.0);
                    }
                    break;
                case SPIDER:
                    if (param.equals("dungeon_q2_damage") || param.equals("dungeon_q4_damage")) {
                        return pet.calculateBaseEffect(3.0);
                    }
                    break;
                case CREEPER:
                    if (param.equals("dungeon_q5_damage") || param.equals("dungeon_q8_damage")) {
                        return pet.calculateBaseEffect(3.0);
                    }
                    break;
                case SLIME:
                    if (param.equals("dungeon_q9_damage") || param.equals("dungeon_q10_damage")) {
                        return pet.calculateBaseEffect(3.0);
                    }
                    break;
            }
        }
        return 0;
    }

    private double getDungeonExecuteThreshold(List<Pet> pets) {
        for (Pet pet : pets) {
            if ((pet.getType() == PetType.ZOMBIE || pet.getType() == PetType.SKELETON ||
                 pet.getType() == PetType.SPIDER || pet.getType() == PetType.CREEPER ||
                 pet.getType() == PetType.SLIME) && pet.hasSpecialEffect()) {
                return 10.0;
            }
        }
        return 0;
    }

    private double getOceanTreasureChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.PHANTOM) {
                return pet.calculateBaseEffect(10.0);
            }
        }
        return 0;
    }

    private double getRuneBonus(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.PHANTOM && pet.hasSpecialEffect()) {
                return 5.0;
            }
        }
        return 0;
    }

    private double getRareOreChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.GLOW_SQUID) {
                return pet.calculateBaseEffect(18.0);
            }
        }
        return 0;
    }

    private double getMineRareSphereChance(List<Pet> pets) {
        return getAllMiningBonuses(pets);
    }

    private double getOreDuplication(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.GLOW_SQUID && pet.hasSpecialEffect()) {
                return 25.0;
            }
        }
        return 0;
    }

    // MYTHIC pets methods
    private double getAllFishingBonuses(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.GUARDIAN) {
                return pet.calculateBaseEffect(10.0);
            }
        }
        return 0;
    }

    private double getFishingSellMultiplier(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.GUARDIAN && pet.hasSpecialEffect()) {
                return 100.0;
            }
        }
        return 1.0;
    }

    private double getAllMiningBonuses(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.SNIFFER) {
                return pet.calculateBaseEffect(18.0);
            }
        }
        return 0;
    }

    private double getOreSellMultiplier(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.SNIFFER && pet.hasSpecialEffect()) {
                return 2.0;
            }
        }
        return 1.0;
    }

    private double getSpecialMaterialChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WITHER_SKELETON) {
                return pet.calculateBaseEffect(6.25);
            }
        }
        return 0;
    }

    private double getFruitSellMultiplier(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WITHER_SKELETON && pet.hasSpecialEffect()) {
                return 3.0;
            }
        }
        return 1.0;
    }

    private int getPetSlotBonus(List<Pet> pets) {
        int bonus = 0;
        for (Pet pet : pets) {
            if (pet.getType() == PetType.ENDER_DRAGON) {
                bonus += 1;
            } else if (pet.getType() == PetType.WARDEN) {
                bonus += pet.hasSpecialEffect() ? 3 : 2;
            }
        }
        return bonus;
    }

    private double getBossDamageBonus(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WITHER) {
                return pet.calculateBaseEffect(5.0);
            }
        }
        return 0;
    }

    private double getBossWitherDamage(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WITHER && pet.hasSpecialEffect()) {
                return plugin.getConfigManager().getWitherSpecialDamage();
            }
        }
        return 0;
    }

    private double getBossWitherDuration(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.WITHER && pet.hasSpecialEffect()) {
                return plugin.getConfigManager().getWitherSpecialDuration();
            }
        }
        return 0;
    }

    private double getMythicItemChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.GIANT) {
                return pet.calculateBaseEffect(5.0);
            }
        }
        return 0;
    }

    private double getMythicItemDoubleChance(List<Pet> pets) {
        for (Pet pet : pets) {
            if (pet.getType() == PetType.GIANT && pet.hasSpecialEffect()) {
                return 1.0;
            }
        }
        return 0;
    }
}
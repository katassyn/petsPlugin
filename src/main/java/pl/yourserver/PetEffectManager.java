package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
// Fixed imports to match actual package structure
// import pl.yourserver.petplugin.PetPlugin;
// import pl.yourserver.petplugin.models.Pet;
// import pl.yourserver.petplugin.models.PetType;
// import pl.yourserver.petplugin.integrations.IntegrationManager;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PetEffectManager {

    private final PetPlugin plugin;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> effectStacks = new HashMap<>();

    public PetEffectManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    // Aplikuj efekt peta
    public void applyPetEffect(Player player, Pet pet) {
        if (!pet.isActive()) return;

        double multiplier = pet.getEffectMultiplier();
        IntegrationManager integrations = plugin.getIntegrationManager();

        switch (pet.getType()) {
            // MAGIC Pets - Nowe wartości
            case COW:
                // +HP: Base 10, +2 per level, special calculation
                double bonusHp = pet.calculateSpecialEffect(10.0, 2.0);
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 + bonusHp);
                break;

            case PIG:
                // Szansa na $ przy zabijaniu: Base 5%, $100-200
                // Obsługiwane w EntityDeathListener z nowymi wartościami
                double moneyChance = pet.calculateBaseEffect(5.0);
                integrations.setPlaceholder("pig_money_chance", moneyChance);
                integrations.setPlaceholder("pig_money_min", 100);
                integrations.setPlaceholder("pig_money_max", 200);
                break;

            case SHEEP:
                // Regeneracja HP: Base 10, +2 per level, co 20s
                if (shouldTriggerEffect(player, 20)) {
                    double healAmount = pet.calculateSpecialEffect(12.0, 2.0);
                    player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                    // Sheep healing - no chat message
                }
                break;

            // EXTRAORDINARY Pets - Nowe wartości
            case CHICKEN:
                // +Luck attribute: +0.25 per level
                double luckBonus = pet.calculateEffectValue(0.0, 0.25);
                player.getAttribute(Attribute.GENERIC_LUCK).setBaseValue(luckBonus);
                break;

            case DONKEY:
                // Dodatkowy schowek: 9 slotów → 27 na 25lvl
                int extraSlots = pet.getLevel() >= 25 ? 27 : 9;
                integrations.setPlaceholder("donkey_extra_storage", extraSlots);
                break;

            case SNOW_GOLEM:
                // Spowalnia wrogów: Base 5 bloków radius
                double radius = pet.calculateBaseEffect(5.0);
                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        LivingEntity mob = (LivingEntity) entity;
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0, false, false));
                    }
                }
                break;

            case IRON_GOLEM:
                // Power strike: Base 2x damage co 30s
                if (shouldTriggerEffect(player, 30)) {
                    double damageMultiplier = pet.calculateBaseEffect(2.0);
                    effectStacks.put(player.getUniqueId(), (int) damageMultiplier);
                    // Iron Golem power charge - no chat message
                }
                break;

            // LEGENDARY Pets - Nowe wartości
            case SQUID:
                // Zwiększa wagę skrzyń rybaka: Base 15%
                double fishingChestBonus = pet.calculateBaseEffect(15.0);
                integrations.setPlaceholder("fishing_chest_bonus", fishingChestBonus);
                break;

            case TURTLE:
                // Zmniejsza dmg od mobów: Base 1%
                double mobDamageReduction = pet.calculateBaseEffect(1.0);
                integrations.setPlaceholder("turtle_damage_reduction", mobDamageReduction);
                break;

            case LLAMA:
                // Zwiększa dmg na moby: Base 2%
                double mobDamageBonus = pet.calculateBaseEffect(2.0);
                integrations.setPlaceholder("llama_mob_damage", mobDamageBonus);
                break;

            case ENDERMAN:
                // Szansa na zerowy koszt tp: Base 7.5%
                double freeTpChance = pet.calculateBaseEffect(7.5);
                integrations.setPlaceholder("dungeon_free_tp_chance", freeTpChance);
                break;

            case WITCH:
                // Wydłuża działanie mikstur: Base 15%
                double potionDurationBonus = pet.calculateBaseEffect(15.0);
                integrations.setPlaceholder("potion_duration_bonus", potionDurationBonus);
                break;

            case HUSK:
                // Większa waga mob sphere: Base 12%
                double mobsphereChance = pet.calculateBaseEffect(12.0);
                integrations.setPlaceholder("mine_mobsphere_chance", mobsphereChance);
                break;

            case MOOSHROOM:
                // Zwiększa szybkość plonu: Base 20%
                double farmSpeed = pet.calculateBaseEffect(20.0);
                integrations.setPlaceholder("farm_growth_speed", farmSpeed);
                break;

            case FROG:
                // Zwiększa prędkość produkcji miodu: Base 15%
                double honeySpeed = pet.calculateBaseEffect(15.0);
                integrations.setPlaceholder("honey_production_speed", honeySpeed);
                break;

            // UNIQUE Pets - Nowe wartości + specjalne efekty
            case WOLF:
                // PvP damage i damage reduction: Base 5%
                double pvpDamage = pet.calculateBaseEffect(5.0);
                double pvpReduction = pet.calculateBaseEffect(5.0);
                integrations.setPlaceholder("wolf_pvp_damage", pvpDamage);
                integrations.setPlaceholder("wolf_pvp_damage_reduction", pvpReduction);

                // Specjalny efekt na 75lvl: 15% lifesteal
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("wolf_pvp_lifesteal", 15.0);
                }
                break;

            case BEE:
                // Zwiększa wagę quality miodu: Base 25%
                double honeyQuality = pet.calculateBaseEffect(25.0);
                integrations.setPlaceholder("honey_quality_bonus", honeyQuality);

                // Specjalny efekt na 75lvl: 25% double honey
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("bee_double_honey_chance", 25.0);
                }
                break;

            case WANDERING_TRADER:
                // Zmniejsza koszt w $: Base 1.43% (max 20% na 75lvl)
                double costReduction = pet.calculateBaseEffect(1.43);
                integrations.setPlaceholder("crafting_cost_reduction", costReduction);

                // Specjalny efekt na 75lvl: 5% full refund
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("crafting_refund_chance", 5.0);
                }
                break;

            case PANDA:
                // Zwiększa storage farming: Base 30%
                double farmStorage = pet.calculateBaseEffect(30.0);
                integrations.setPlaceholder("farm_storage_bonus", farmStorage);

                // Specjalny efekt na 75lvl: 25% double harvest
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("farm_double_harvest_chance", 25.0);
                }
                break;

            // Dungeon damage pets: Base 3%
            case ZOMBIE:
                double zombieDamage = pet.calculateBaseEffect(3.0);
                integrations.setPlaceholder("dungeon_q1_damage", zombieDamage);
                integrations.setPlaceholder("dungeon_q3_damage", zombieDamage);

                // Specjalny efekt na 75lvl: execute at 10% HP
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("dungeon_execute_threshold", 10.0);
                    integrations.setPlaceholder("dungeon_execute_zones", 1.0); // q1,q3
                }
                break;

            case SKELETON:
                double skeletonDamage = pet.calculateBaseEffect(3.0);
                integrations.setPlaceholder("dungeon_q6_damage", skeletonDamage);
                integrations.setPlaceholder("dungeon_q7_damage", skeletonDamage);

                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("dungeon_execute_threshold", 10.0);
                    integrations.setPlaceholder("dungeon_execute_zones", 2.0); // q6,q7
                }
                break;

            case SPIDER:
                double spiderDamage = pet.calculateBaseEffect(3.0);
                integrations.setPlaceholder("dungeon_q2_damage", spiderDamage);
                integrations.setPlaceholder("dungeon_q4_damage", spiderDamage);

                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("dungeon_execute_threshold", 10.0);
                    integrations.setPlaceholder("dungeon_execute_zones", 3.0); // q2,q4
                }
                break;

            case CREEPER:
                double creeperDamage = pet.calculateBaseEffect(3.0);
                integrations.setPlaceholder("dungeon_q5_damage", creeperDamage);
                integrations.setPlaceholder("dungeon_q8_damage", creeperDamage);

                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("dungeon_execute_threshold", 10.0);
                    integrations.setPlaceholder("dungeon_execute_zones", 4.0); // q5,q8
                }
                break;

            case SLIME:
                double slimeDamage = pet.calculateBaseEffect(3.0);
                integrations.setPlaceholder("dungeon_q9_damage", slimeDamage);
                integrations.setPlaceholder("dungeon_q10_damage", slimeDamage);

                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("dungeon_execute_threshold", 10.0);
                    integrations.setPlaceholder("dungeon_execute_zones", 5.0); // q9,q10
                }
                break;

            case PHANTOM:
                // Zwiększa wagę ocean treasure: Base 10%
                double oceanTreasure = pet.calculateBaseEffect(10.0);
                integrations.setPlaceholder("fishing_ocean_treasure_chance", oceanTreasure);

                // Specjalny efekt na 75lvl: +5% waga run
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("fishing_rune_bonus", 5.0);
                }
                break;

            case GLOW_SQUID:
                // Zwiększa szansę na rzadsze rudy: Base 18%
                double rareOreChance = pet.calculateBaseEffect(18.0);
                integrations.setPlaceholder("mine_rare_ore_chance", rareOreChance);

                // Specjalny efekt na 75lvl: 25% ore duplication
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("mine_ore_duplication", 25.0);
                }
                break;

            // MYTHIC Pets - Nowe wartości + specjalne efekty
            case GUARDIAN:
                // Wszystkie fishing bonusy: Base 10%
                double guardianFishingBonus = pet.calculateBaseEffect(10.0);
                integrations.setPlaceholder("fishing_treasure_chance", guardianFishingBonus);
                integrations.setPlaceholder("fishing_chest_bonus", guardianFishingBonus);
                integrations.setPlaceholder("fishing_rune_chance", guardianFishingBonus);
                integrations.setPlaceholder("fishing_map_chance", guardianFishingBonus);

                // Specjalny efekt na 100lvl: 100x fish sell price
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("fishing_sell_multiplier", 100.0);
                }
                break;

            case SNIFFER:
                // Wszystkie mining bonusy: Base 18%
                double snifferMiningBonus = pet.calculateBaseEffect(18.0);
                integrations.setPlaceholder("mine_mob_spawn_chance", snifferMiningBonus);
                integrations.setPlaceholder("mine_rare_ore_chance", snifferMiningBonus);
                integrations.setPlaceholder("mine_rare_sphere_chance", snifferMiningBonus);

                // Specjalny efekt na 100lvl: 2x ore sell price
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("mine_ore_sell_multiplier", 2.0);
                }
                break;

            case WITHER_SKELETON:
                // Special materials chance: Base 6.25% (100% na 100lvl)
                double specialMaterials = pet.calculateBaseEffect(6.25);
                integrations.setPlaceholder("farm_special_material_chance", specialMaterials);

                // Specjalny efekt na 100lvl: 3x fruit sell price
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("farm_fruit_sell_multiplier", 3.0);
                }
                break;

            case ENDER_DRAGON:
                // +1 pet slot + duplikuje efekt drugiego peta
                integrations.setPlaceholder("pet_slot_bonus", 1);

                // Duplikacja efektu drugiego peta
                List<Pet> activePets = plugin.getPetManager().getActivePets(player);
                for (Pet otherPet : activePets) {
                    if (otherPet != pet && otherPet.getType() != PetType.ENDER_DRAGON) {
                        applyPetEffect(player, otherPet); // Duplikuj efekt

                        // Specjalny efekt na 100lvl: podwójna duplikacja
                        if (pet.hasSpecialEffect()) {
                            applyPetEffect(player, otherPet); // Druga kopia
                        }
                        break;
                    }
                }
                break;

            case WARDEN:
                // +2 sloty na pety (3 na 100lvl)
                int wardenSlots = pet.hasSpecialEffect() ? 3 : 2;
                integrations.setPlaceholder("pet_slot_bonus", wardenSlots);
                break;

            case WITHER:
                // Boss damage: Base 5%
                double bossDamage = pet.calculateBaseEffect(5.0);
                integrations.setPlaceholder("boss_damage_bonus", bossDamage);

                // Specjalny efekt na 100lvl: special wither 250dmg/s for 5s
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("boss_wither_damage", 250.0);
                    integrations.setPlaceholder("boss_wither_duration", 5.0);
                }
                break;

            case GIANT:
                // Mythic items waga: Base 5%
                double mythicItemChance = pet.calculateBaseEffect(5.0);
                integrations.setPlaceholder("mythic_item_chance", mythicItemChance);
                integrations.setPlaceholder("boss_no_normal_drops", 1.0);

                // Specjalny efekt na 100lvl: double mythic chance
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("mythic_item_double_chance", 1.0);
                }
                break;
        }

        // Aplikuj specjalne efekty
        if (pet.hasSpecialEffect()) {
            applySpecialEffect(player, pet);
        }
    }

    // Aplikuj specjalny efekt peta (75/100 lvl)
    private void applySpecialEffect(Player player, Pet pet) {
        IntegrationManager integrations = plugin.getIntegrationManager();

        switch (pet.getType()) {
            // Unique pets - special effect at level 75
            case WOLF:
                // Lifesteal z PvP
                integrations.setPlaceholder("pvp_lifesteal", 0.15);
                break;

            case BEE:
                // Auto-collect miód w promieniu
                integrations.setPlaceholder("bee_auto_collect", 1.0);
                break;

            case WANDERING_TRADER:
                // Szansa na zwrot materiałów przy craftingu
                integrations.setPlaceholder("crafting_refund_chance", 0.10);
                break;

            // Mythic pets - special effect at level 100
            case GUARDIAN:
                // Immune to drowning + water breathing
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
                break;

            case SNIFFER:
                // X-ray vision na rzadkie rudy (glowing effect)
                integrations.setPlaceholder("mine_xray_vision", 1.0);
                break;

            case ENDER_DRAGON:
                // Ender Dragon gives +1 slot and duplicates effects (handled in PetManager)
                break;

            case WITHER:
                // Wither effect na atakowanych bossach
                integrations.setPlaceholder("boss_wither_effect", 1.0);
                break;
        }
    }

    // Sprawdź czy efekt powinien się aktywować
    private boolean shouldTriggerEffect(Player player, int cooldownSeconds) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(uuid, 0L);

        if (currentTime - lastUse >= cooldownSeconds * 1000L) {
            cooldowns.put(uuid, currentTime);
            return true;
        }
        return false;
    }

    // Pobierz opis efektu peta
    public String getPetEffectDescription(Pet pet) {
        double multiplier = pet.getEffectMultiplier();

        switch (pet.getType()) {
            // MAGIC
            case COW:
                double cowHp = pet.calculateSpecialEffect(12.0, 2.0);
                return "&c+" + df.format(cowHp) + " Maximum Health";
            case PIG:
                double pigChance = pet.calculateBaseEffect(5.0);
                return "&6" + df.format(pigChance) + "% chance for $100-200 on mob kill";
            case SHEEP:
                double sheepHeal = pet.calculateSpecialEffect(12.0, 2.0);
                return "&d+" + df.format(sheepHeal) + " HP regeneration every 20 seconds";

            // EXTRAORDINARY
            case CHICKEN:
                double chickenLuck = pet.calculateEffectValue(0.0, 0.25);
                return "&a+" + df.format(chickenLuck) + " Luck attribute bonus";
            case DONKEY:
                int donkeySlots = pet.getLevel() >= 25 ? 27 : 9;
                return "&e+" + donkeySlots + " extra inventory slots";
            case SNOW_GOLEM:
                double snowRadius = pet.calculateBaseEffect(5.0);
                return "&bSlows enemies in " + df.format(snowRadius) + " block radius";
            case IRON_GOLEM:
                double ironDamage = pet.calculateBaseEffect(2.0);
                return "&7+" + df.format(ironDamage) + "x damage power strike every 30s";

            // LEGENDARY
            case SQUID:
                double squidChest = pet.calculateBaseEffect(15.0);
                return "&3+" + df.format(squidChest) + "% fishing chest chance";
            case TURTLE:
                double turtleReduction = pet.calculateBaseEffect(1.0);
                return "&2-" + df.format(turtleReduction) + "% damage taken from mobs";
            case LLAMA:
                double llamaDamage = pet.calculateBaseEffect(2.0);
                return "&c+" + df.format(llamaDamage) + "% damage dealt to mobs";
            case ENDERMAN:
                double endermanTp = pet.calculateBaseEffect(7.5);
                return "&5" + df.format(endermanTp) + "% chance for free dungeon teleport";
            case WITCH:
                double witchDuration = pet.calculateBaseEffect(15.0);
                return "&d+" + df.format(witchDuration) + "% potion effect duration";
            case HUSK:
                double huskSphere = pet.calculateBaseEffect(12.0);
                return "&6+" + df.format(huskSphere) + "% mob sphere drop chance";
            case MOOSHROOM:
                double mooshroomSpeed = pet.calculateBaseEffect(20.0);
                return "&a+" + df.format(mooshroomSpeed) + "% farming growth speed";
            case FROG:
                double frogHoney = pet.calculateBaseEffect(15.0);
                return "&e+" + df.format(frogHoney) + "% honey production speed";

            // UNIQUE
            case WOLF:
                double wolfPvp = pet.calculateBaseEffect(5.0);
                String wolfExtra = pet.hasSpecialEffect() ? " + 15% lifesteal" : "";
                return "&c+" + df.format(wolfPvp) + "% PvP damage & damage reduction" + wolfExtra;
            case BEE:
                double beeQuality = pet.calculateBaseEffect(25.0);
                String beeExtra = pet.hasSpecialEffect() ? " + 25% double honey" : "";
                return "&6+" + df.format(beeQuality) + "% honey quality chance" + beeExtra;
            case WANDERING_TRADER:
                double traderCost = pet.calculateBaseEffect(1.43);
                String traderExtra = pet.hasSpecialEffect() ? " + 5% full refund" : "";
                return "&a-" + df.format(traderCost) + "% crafting cost" + traderExtra;
            case PANDA:
                double pandaYield = pet.calculateBaseEffect(30.0);
                String pandaExtra = pet.hasSpecialEffect() ? " + 25% double harvest" : "";
                return "&2+" + df.format(pandaYield) + "% farm storage bonus" + pandaExtra;
            case ZOMBIE:
                double zombieDmg = pet.calculateBaseEffect(3.0);
                String zombieExtra = pet.hasSpecialEffect() ? " + Execute at 10% HP" : "";
                return "&4+" + df.format(zombieDmg) + "% damage in Dungeon Q1/Q3" + zombieExtra;
            case SKELETON:
                double skeletonDmg = pet.calculateBaseEffect(3.0);
                String skeletonExtra = pet.hasSpecialEffect() ? " + Execute at 10% HP" : "";
                return "&7+" + df.format(skeletonDmg) + "% damage in Dungeon Q6/Q7" + skeletonExtra;
            case SPIDER:
                double spiderDmg = pet.calculateBaseEffect(3.0);
                String spiderExtra = pet.hasSpecialEffect() ? " + Execute at 10% HP" : "";
                return "&8+" + df.format(spiderDmg) + "% damage in Dungeon Q2/Q4" + spiderExtra;
            case CREEPER:
                double creeperDmg = pet.calculateBaseEffect(3.0);
                String creeperExtra = pet.hasSpecialEffect() ? " + Execute at 10% HP" : "";
                return "&a+" + df.format(creeperDmg) + "% damage in Dungeon Q5/Q8" + creeperExtra;
            case SLIME:
                double slimeDmg = pet.calculateBaseEffect(3.0);
                String slimeExtra = pet.hasSpecialEffect() ? " + Execute at 10% HP" : "";
                return "&2+" + df.format(slimeDmg) + "% damage in Dungeon Q9/Q10" + slimeExtra;
            case PHANTOM:
                double phantomTreasure = pet.calculateBaseEffect(10.0);
                String phantomExtra = pet.hasSpecialEffect() ? " + 5% rune bonus" : "";
                return "&b+" + df.format(phantomTreasure) + "% ocean treasure chance" + phantomExtra;
            case GLOW_SQUID:
                double glowOre = pet.calculateBaseEffect(18.0);
                String glowExtra = pet.hasSpecialEffect() ? " + 25% ore duplication" : "";
                return "&e+" + df.format(glowOre) + "% rare ore chance" + glowExtra;

            // MYTHIC
            case GUARDIAN:
                double guardianBonus = pet.calculateBaseEffect(10.0);
                String guardianExtra = pet.hasSpecialEffect() ? " + 100x fish sell price" : "";
                return "&b+" + df.format(guardianBonus) + "% all fishing bonuses" + guardianExtra;
            case SNIFFER:
                double snifferBonus = pet.calculateBaseEffect(18.0);
                String snifferExtra = pet.hasSpecialEffect() ? " + 2x ore sell price" : "";
                return "&6+" + df.format(snifferBonus) + "% all mining bonuses" + snifferExtra;
            case WITHER_SKELETON:
                double witherMaterials = pet.calculateBaseEffect(6.25);
                String witherExtra = pet.hasSpecialEffect() ? " + 3x fruit sell price" : "";
                return "&5+" + df.format(witherMaterials) + "% special material chance" + witherExtra;
            case ENDER_DRAGON:
                String dragonExtra = pet.hasSpecialEffect() ? " + doubles duplication effect" : "";
                return "&d+1 pet slot & duplicates other pet effects" + dragonExtra;
            case WARDEN:
                int wardenSlots = pet.hasSpecialEffect() ? 3 : 2;
                return "&8+" + wardenSlots + " additional pet slots";
            case WITHER:
                double witherBoss = pet.calculateBaseEffect(5.0);
                String witherBossExtra = pet.hasSpecialEffect() ? " + 250 wither damage/s for 5s" : "";
                return "&4+" + df.format(witherBoss) + "% boss damage" + witherBossExtra;
            case GIANT:
                double giantMythic = pet.calculateBaseEffect(5.0);
                String giantExtra = pet.hasSpecialEffect() ? " + double mythic chance" : "";
                return "&c+" + df.format(giantMythic) + "% mythic item drop chance" + giantExtra;

            default: return "&7Unknown effect";
        }
    }

    // Pobierz opis specjalnego efektu
    public String getSpecialEffectDescription(Pet pet) {
        switch (pet.getType()) {
            // Unique special effects (75 lvl)
            case WOLF: return "&cLifesteal from PvP damage";
            case BEE: return "&6Auto-collects honey nearby";
            case WANDERING_TRADER: return "&a10% material refund chance";
            case PANDA: return "&2Chance for double harvest";
            case ZOMBIE:
            case SKELETON:
            case SPIDER:
            case CREEPER:
            case SLIME: return "&4Boss execute at 10% HP";
            case PHANTOM: return "&bWater walking ability";
            case GLOW_SQUID: return "&eOre X-ray vision";

            // Mythic special effects (100 lvl)
            case GUARDIAN: return "&bInfinite water breathing";
            case SNIFFER: return "&6Highlights rare ores";
            case WITHER_SKELETON: return "&5Guaranteed rare drops";
            case ENDER_DRAGON: return "&dDoubles the duplication effect (2x copy of 2nd pet)";
            case WARDEN: return "&8Intimidation aura";
            case WITHER: return "&4Applies wither to bosses";
            case GIANT: return "&cDoubles mythic item chance";

            default: return "&7No special effect";
        }
    }

    // Wyczyść efekty gracza
    public void clearEffects(Player player) {
        cooldowns.remove(player.getUniqueId());
        effectStacks.remove(player.getUniqueId());

        // Reset atrybutów
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);

        // Usuń efekty (flight removed - should not be managed by pets)
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.LUCK);
    }

    // Sprawdź czy gracz ma stack efektu (np. Iron Golem)
    public boolean hasEffectStack(Player player) {
        return effectStacks.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    // Użyj stack efektu
    public void useEffectStack(Player player) {
        int stacks = effectStacks.getOrDefault(player.getUniqueId(), 0);
        if (stacks > 0) {
            effectStacks.put(player.getUniqueId(), stacks - 1);
        }
    }
}
package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
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
            // MAGIC Pets - Nowe wartoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşci
            case COW:
                // +HP: Base 10, +2 per level, special calculation
                double bonusHp = pet.calculateSpecialEffect(10.0, 2.0);
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 + bonusHp);
                break;

            case PIG:
                // Szansa na $ przy zabijaniu: Base 5%, $100-200
                // ObsÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąË‡ugiwane w EntityDeathListener z nowymi wartoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşciami
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

            // EXTRAORDINARY Pets - Nowe wartoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşci
            case CHICKEN:
                // +Luck attribute: +0.25 per level
                double luckBonus = pet.calculateEffectValue(0.0, 0.25);
                player.getAttribute(Attribute.GENERIC_LUCK).setBaseValue(luckBonus);
                break;

            case DONKEY:
                // Dodatkowy schowek: 9 slotÄ‚â€žĂ˘â‚¬ĹˇĂ„Ä…Ă˘â‚¬Ĺˇw Ă„â€šĂ‹ÂÄ‚ËĂ˘â€šÂ¬Ă‚Â Ä‚ËĂ˘â€šÂ¬Ă˘â€žË 27 na 25lvl
                int extraSlots = pet.getLevel() >= 25 ? 27 : 9;
                integrations.setPlaceholder("donkey_extra_storage", extraSlots);
                break;

            case SNOW_GOLEM:
                // Spowalnia wrogÄ‚â€žĂ˘â‚¬ĹˇĂ„Ä…Ă˘â‚¬Ĺˇw: Base 5 blokÄ‚â€žĂ˘â‚¬ĹˇĂ„Ä…Ă˘â‚¬Ĺˇw radius
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

            // LEGENDARY Pets - Nowe wartoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşci
            case SQUID:
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza wagĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Â skrzyÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąÄľ rybaka: Base 15%
                double fishingChestBonus = pet.calculateBaseEffect(15.0);
                integrations.setPlaceholder("fishing_chest_bonus", fishingChestBonus);
                break;

            case TURTLE:
                // Zmniejsza dmg od mobÄ‚â€žĂ˘â‚¬ĹˇĂ„Ä…Ă˘â‚¬Ĺˇw: Base 1%
                double mobDamageReduction = pet.calculateBaseEffect(1.0);
                integrations.setPlaceholder("turtle_damage_reduction", mobDamageReduction);
                break;

            case LLAMA:
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza dmg na moby: Base 2%
                double mobDamageBonus = pet.calculateBaseEffect(2.0);
                integrations.setPlaceholder("llama_mob_damage", mobDamageBonus);
                break;

            case ENDERMAN:
                // Szansa na zerowy koszt tp: Base 7.5%
                double freeTpChance = pet.calculateBaseEffect(7.5);
                integrations.setPlaceholder("dungeon_free_tp_chance", freeTpChance);
                break;

            case WITCH:
                // WydÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąË‡uÄ‚â€žĂ„â€¦Ä‚â€žĂ‹ĹĄa dziaÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąË‡anie mikstur: Base 15%
                double potionDurationBonus = pet.calculateBaseEffect(15.0);
                integrations.setPlaceholder("potion_duration_bonus", potionDurationBonus);
                break;

            case HUSK:
                // WiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza waga mob sphere: Base 12%
                double mobsphereChance = pet.calculateBaseEffect(12.0);
                integrations.setPlaceholder("mine_mobsphere_chance", mobsphereChance);
                break;

            case MOOSHROOM:
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza szybkoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â€šÂ¬Ă‹â€ˇ plonu: Base 20%
                double farmSpeed = pet.calculateBaseEffect(20.0);
                integrations.setPlaceholder("farm_growth_speed", farmSpeed);
                break;

            case FROG:
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza prĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹ÂdkoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â€šÂ¬Ă‹â€ˇ produkcji miodu: Base 15%
                double honeySpeed = pet.calculateBaseEffect(15.0);
                integrations.setPlaceholder("honey_production_speed", honeySpeed);
                break;

            // UNIQUE Pets - Nowe wartoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşci + specjalne efekty
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
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza wagĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Â quality miodu: Base 25%
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
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza storage farming: Base 30%
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
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza wagĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Â ocean treasure: Base 10%
                double oceanTreasure = pet.calculateBaseEffect(10.0);
                integrations.setPlaceholder("fishing_ocean_treasure_chance", oceanTreasure);

                // Specjalny efekt na 75lvl: +5% waga run
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("fishing_rune_bonus", 5.0);
                }
                break;

            case GLOW_SQUID:
                // ZwiĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Âksza szansĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Â na rzadsze rudy: Base 18%
                double rareOreChance = pet.calculateBaseEffect(18.0);
                integrations.setPlaceholder("mine_rare_ore_chance", rareOreChance);

                // Specjalny efekt na 75lvl: 25% ore duplication
                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("mine_ore_duplication", 25.0);
                }
                break;

            // MYTHIC Pets - Nowe wartoÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąĹşci + specjalne efekty
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

                        // Specjalny efekt na 100lvl: podwÄ‚â€žĂ˘â‚¬ĹˇĂ„Ä…Ă˘â‚¬Ĺˇjna duplikacja
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

                if (pet.hasSpecialEffect()) {
                    integrations.setPlaceholder("boss_wither_damage", plugin.getConfigManager().getWitherSpecialDamage());
                    integrations.setPlaceholder("boss_wither_duration", plugin.getConfigManager().getWitherSpecialDuration());
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
                // Auto-collect miÄ‚â€žĂ˘â‚¬ĹˇĂ„Ä…Ă˘â‚¬Ĺˇd w promieniu
                integrations.setPlaceholder("bee_auto_collect", 1.0);
                break;

            case WANDERING_TRADER:
                // Szansa na zwrot materiaÄ‚â€žĂ„â€¦Ä‚ËĂ˘â€šÂ¬ÄąË‡Ä‚â€žĂ˘â‚¬ĹˇĂ„Ä…Ă˘â‚¬Ĺˇw przy craftingu
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


    private String formatEffectDescription(Pet pet, double primaryValue, String fallback, Map<String, Object> extras) {
        String basePath = "pets." + pet.getType().name() + ".effects";
        ConfigurationSection root = plugin.getConfigManager().getPetsConfig().getConfigurationSection(basePath);
        ConfigurationSection effectSection = null;
        String template = null;
        if (root != null) {
            for (String key : root.getKeys(false)) {
                effectSection = root.getConfigurationSection(key);
                if (effectSection != null) {
                    template = effectSection.getString("description");
                    if (template != null && !template.isEmpty()) {
                        break;
                    }
                }
            }
        }
        if (template == null || template.isEmpty()) {
            return TextUtil.colorize(fallback);
        }
        Map<String, String> replacements = buildPlaceholders(effectSection, primaryValue, extras);
        return applyPlaceholders(template, replacements, fallback);
    }

    private String formatSpecialEffectDescription(Pet pet) {
        String path = "pets." + pet.getType().name() + ".special-effect";
        ConfigurationSection section = plugin.getConfigManager().getPetsConfig().getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        String template = section.getString("description");
        if (template == null || template.isEmpty()) {
            return null;
        }
        Map<String, String> replacements = buildPlaceholders(section, null, null);
        return applyPlaceholders(template, replacements, null);
    }

    private Map<String, String> buildPlaceholders(ConfigurationSection section, Double mainValue, Map<String, Object> extras) {
        Map<String, String> replacements = new HashMap<>();
        if (mainValue != null) {
            replacements.put("%value%", formatNumber(mainValue));
        }
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if ("description".equalsIgnoreCase(key)) {
                    continue;
                }
                Object raw = section.get(key);
                if (raw instanceof ConfigurationSection) {
                    continue;
                }
                String placeholder = "%" + key.replace('-', '_') + "%";
                replacements.put(placeholder, formatObject(raw));
            }
        }
        if (extras != null) {
            for (Map.Entry<String, Object> entry : extras.entrySet()) {
                String placeholder = entry.getKey();
                if (!placeholder.startsWith("%")) {
                    placeholder = "%" + placeholder;
                }
                if (!placeholder.endsWith("%")) {
                    placeholder = placeholder + "%";
                }
                replacements.put(placeholder, formatObject(entry.getValue()));
            }
        }
        return replacements;
    }

    private String formatObject(Object raw) {
        if (raw instanceof Number) {
            return formatNumber((Number) raw);
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw ? "true" : "false";
        }
        return String.valueOf(raw);
    }

    private String formatNumber(Number number) {
        return formatNumber(number.doubleValue());
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 1e-6) {
            return String.valueOf(Math.round(value));
        }
        return df.format(value);
    }

    private String applyPlaceholders(String template, Map<String, String> replacements, String fallbackColorSource) {
        String result = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        if (!hasColorCodes(result)) {
            String prefix = extractLeadingColorCodes(fallbackColorSource);
            if (!prefix.isEmpty()) {
                result = prefix + result;
            }
        }
        return TextUtil.colorize(result);
    }

    private boolean hasColorCodes(String text) {
        if (text == null) {
            return false;
        }
        return text.indexOf('&') >= 0 || text.indexOf('\u00A7') >= 0;
    }

    private String extractLeadingColorCodes(String source) {
        if (source == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int length = source.length();
        for (int i = 0; i < length - 1; i++) {
            char current = source.charAt(i);
            char next = source.charAt(i + 1);
            if ((current == '&' || current == '\u00A7') && isColorCodeChar(next)) {
                builder.append('&').append(Character.toLowerCase(next));
                i++;
                continue;
            }
            if (Character.isWhitespace(current)) {
                continue;
            }
            break;
        }
        return builder.toString();
    }

    private boolean isColorCodeChar(char c) {
        c = Character.toLowerCase(c);
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'k' && c <= 'o') || c == 'r';
    }

    private String ensureColored(String text, String fallbackRaw) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String candidate = trimmed;
        if (!hasColorCodes(candidate)) {
            String prefix = extractLeadingColorCodes(fallbackRaw);
            if (!prefix.isEmpty()) {
                candidate = prefix + candidate;
            }
        }
        return TextUtil.colorize(candidate);
    }

    private String getDefaultSpecialEffectRaw(PetType type) {
        if (type == null) {
            return "&7No special effect";
        }
        switch (type) {
            case WOLF:
                return "&cLifesteal from PvP damage";
            case BEE:
                return "&6Auto-collects honey nearby";
            case WANDERING_TRADER:
                return "&a10% material refund chance";
            case PANDA:
                return "&2Chance for double harvest";
            case ZOMBIE:
            case SKELETON:
            case SPIDER:
            case CREEPER:
            case SLIME:
                return "&4Boss execute at 10% HP";
            case PHANTOM:
                return "&bWater walking ability";
            case GLOW_SQUID:
                return "&eOre X-ray vision";
            case GUARDIAN:
                return "&bInfinite water breathing";
            case SNIFFER:
                return "&6Highlights rare ores";
            case WITHER_SKELETON:
                return "&5Guaranteed rare drops";
            case ENDER_DRAGON:
                return "&dDoubles the duplication effect (2x copy of 2nd pet)";
            case WARDEN:
                return "&8Intimidation aura";
            case WITHER:
                return "&4Applies wither to bosses";
            case GIANT:
                return "&cDoubles mythic item chance";
            default:
                return "&7No special effect";
        }
    }

    public String getSpecialEffectDescription(Pet pet) {
        String defaultRaw = getDefaultSpecialEffectRaw(pet.getType());
        String description = ensureColored(formatSpecialEffectDescription(pet), defaultRaw);
        if (description != null && !description.isEmpty()) {
            return description;
        }
        return TextUtil.colorize(defaultRaw != null ? defaultRaw : "&7No special effect");
    }
   
    // WyczyÄąâ€şĂ„â€ˇ efekty gracza
    public void clearEffects(Player player) {
        cooldowns.remove(player.getUniqueId());
        effectStacks.remove(player.getUniqueId());

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);

        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.LUCK);
    }
 // SprawdÄ‚â€žĂ„â€¦Ă„Ä…ÄąĹź czy efekt powinien siĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â‚¬ĹľĂ‹Â aktywowaĂ„â€šĂ˘â‚¬ĹľÄ‚ËĂ˘â€šÂ¬Ă‹â€ˇ
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
        String description;
        switch (pet.getType()) {
            case COW:
                double cowHp = pet.calculateSpecialEffect(12.0, 2.0);
                description = formatEffectDescription(pet, cowHp,
                        "&c+" + df.format(cowHp) + " Maximum Health", null);
                break;
            case PIG:
                double pigChance = pet.calculateBaseEffect(5.0);
                double minMoney = plugin.getConfigManager().getPetsConfig().getDouble("pets.PIG.effects.money-chance.min-money", 100);
                double maxMoney = plugin.getConfigManager().getPetsConfig().getDouble("pets.PIG.effects.money-chance.max-money", 200);
                Map<String, Object> pigExtras = new HashMap<>();
                pigExtras.put("%min_money%", minMoney);
                pigExtras.put("%max_money%", maxMoney);
                description = formatEffectDescription(pet, pigChance,
                        "&6" + df.format(pigChance) + "% chance for $" + formatNumber(minMoney) + "-" + formatNumber(maxMoney) + " on mob kill",
                        pigExtras);
                break;
            case SHEEP:
                double sheepHeal = pet.calculateSpecialEffect(12.0, 2.0);
                description = formatEffectDescription(pet, sheepHeal,
                        "&d+" + df.format(sheepHeal) + " HP every 20s", null);
                break;
            case CHICKEN:
                double chickenLuck = pet.calculateEffectValue(0.0, 0.25);
                description = formatEffectDescription(pet, chickenLuck,
                        "&a+" + df.format(chickenLuck) + " Luck attribute", null);
                break;
            case DONKEY:
                double donkeySlots = pet.getLevel() >= 25 ? 27 : 9;
                description = formatEffectDescription(pet, donkeySlots,
                        "&eExtra inventory (" + formatNumber(donkeySlots) + " slots)", null);
                break;
            case SNOW_GOLEM:
                double snowRadius = pet.calculateBaseEffect(5.0);
                description = formatEffectDescription(pet, snowRadius,
                        "&bSlows enemies in " + df.format(snowRadius) + " block radius", null);
                break;
            case IRON_GOLEM:
                double ironDamage = pet.calculateBaseEffect(2.0);
                description = formatEffectDescription(pet, ironDamage,
                        "&7" + df.format(ironDamage) + "x damage every 30s", null);
                break;
            case SQUID:
                double squidChest = pet.calculateBaseEffect(15.0);
                description = formatEffectDescription(pet, squidChest,
                        "&3+" + df.format(squidChest) + "% fishing chest chance", null);
                break;
            case TURTLE:
                double turtleReduction = pet.calculateBaseEffect(1.0);
                description = formatEffectDescription(pet, turtleReduction,
                        "&2-" + df.format(turtleReduction) + "% damage taken from mobs", null);
                break;
            case LLAMA:
                double llamaDamage = pet.calculateBaseEffect(2.0);
                description = formatEffectDescription(pet, llamaDamage,
                        "&c+" + df.format(llamaDamage) + "% damage dealt to mobs", null);
                break;
            case ENDERMAN:
                double endermanTp = pet.calculateBaseEffect(7.5);
                description = formatEffectDescription(pet, endermanTp,
                        "&5" + df.format(endermanTp) + "% free Q teleport", null);
                break;
            case WITCH:
                double witchDuration = pet.calculateBaseEffect(15.0);
                description = formatEffectDescription(pet, witchDuration,
                        "&d+" + df.format(witchDuration) + "% potion duration", null);
                break;
            case HUSK:
                double huskChance = pet.calculateBaseEffect(12.0);
                description = formatEffectDescription(pet, huskChance,
                        "&6+" + df.format(huskChance) + "% mob sphere drop chance", null);
                break;
            case MOOSHROOM:
                double mooshroomSpeed = pet.calculateBaseEffect(20.0);
                description = formatEffectDescription(pet, mooshroomSpeed,
                        "&a+" + df.format(mooshroomSpeed) + "% farming growth speed", null);
                break;
            case FROG:
                double frogHoney = pet.calculateBaseEffect(15.0);
                description = formatEffectDescription(pet, frogHoney,
                        "&e+" + df.format(frogHoney) + "% honey production speed", null);
                break;
            case WOLF:
                double wolfPvp = pet.calculateBaseEffect(5.0);
                description = formatEffectDescription(pet, wolfPvp,
                        "&c+" + df.format(wolfPvp) + "% PvP damage & damage reduction", null);
                break;
            case BEE:
                double beeQuality = pet.calculateBaseEffect(25.0);
                description = formatEffectDescription(pet, beeQuality,
                        "&6+" + df.format(beeQuality) + "% honey quality chance", null);
                break;
            case WANDERING_TRADER:
                double traderCost = pet.calculateBaseEffect(1.43);
                description = formatEffectDescription(pet, traderCost,
                        "&a-" + df.format(traderCost) + "% crafting cost", null);
                break;
            case PANDA:
                double pandaStorage = pet.calculateBaseEffect(30.0);
                description = formatEffectDescription(pet, pandaStorage,
                        "&2+" + df.format(pandaStorage) + "% farm storage bonus", null);
                break;
            case ZOMBIE:
                double zombieDamage = pet.calculateBaseEffect(3.0);
                description = formatEffectDescription(pet, zombieDamage,
                        "&4+" + df.format(zombieDamage) + "% damage in Dungeon Q1/Q3", null);
                break;
            case SKELETON:
                double skeletonDamage = pet.calculateBaseEffect(3.0);
                description = formatEffectDescription(pet, skeletonDamage,
                        "&7+" + df.format(skeletonDamage) + "% damage in Dungeon Q6/Q7", null);
                break;
            case SPIDER:
                double spiderDamage = pet.calculateBaseEffect(3.0);
                description = formatEffectDescription(pet, spiderDamage,
                        "&8+" + df.format(spiderDamage) + "% damage in Dungeon Q2/Q4", null);
                break;
            case CREEPER:
                double creeperDamage = pet.calculateBaseEffect(3.0);
                description = formatEffectDescription(pet, creeperDamage,
                        "&a+" + df.format(creeperDamage) + "% damage in Dungeon Q5/Q8", null);
                break;
            case SLIME:
                double slimeDamage = pet.calculateBaseEffect(3.0);
                description = formatEffectDescription(pet, slimeDamage,
                        "&2+" + df.format(slimeDamage) + "% damage in Dungeon Q9/Q10", null);
                break;
            case PHANTOM:
                double phantomTreasure = pet.calculateBaseEffect(10.0);
                description = formatEffectDescription(pet, phantomTreasure,
                        "&b+" + df.format(phantomTreasure) + "% ocean treasure chance", null);
                break;
            case GLOW_SQUID:
                double glowChance = pet.calculateBaseEffect(18.0);
                description = formatEffectDescription(pet, glowChance,
                        "&e+" + df.format(glowChance) + "% rare ore chance", null);
                break;
            case GUARDIAN:
                double guardianBonus = pet.calculateBaseEffect(10.0);
                description = formatEffectDescription(pet, guardianBonus,
                        "&b+" + df.format(guardianBonus) + "% all fishing bonuses", null);
                break;
            case SNIFFER:
                double snifferBonus = pet.calculateBaseEffect(18.0);
                description = formatEffectDescription(pet, snifferBonus,
                        "&6+" + df.format(snifferBonus) + "% all mining bonuses", null);
                break;
            case WITHER_SKELETON:
                double witherMaterials = pet.calculateBaseEffect(6.25);
                description = formatEffectDescription(pet, witherMaterials,
                        "&5+" + df.format(witherMaterials) + "% special material chance", null);
                break;
            case ENDER_DRAGON:
                double extraSlot = 1.0;
                description = formatEffectDescription(pet, extraSlot,
                        "&d+1 pet slot & duplicates other pet effects", null);
                break;
            case WARDEN:
                double wardenSlots = pet.hasSpecialEffect() ? 3 : 2;
                description = formatEffectDescription(pet, wardenSlots,
                        "&8+" + df.format(wardenSlots) + " additional pet slots", null);
                break;
            case WITHER:
                double witherDamage = pet.calculateBaseEffect(5.0);
                description = formatEffectDescription(pet, witherDamage,
                        "&4+" + df.format(witherDamage) + "% boss damage", null);
                break;
            case GIANT:
                double giantChance = pet.calculateBaseEffect(5.0);
                description = formatEffectDescription(pet, giantChance,
                        "&c+" + df.format(giantChance) + "% mythic item drop chance", null);
                break;
            default:
                description = TextUtil.colorize("&7Unknown effect");
        }
        return appendSpecialDescription(pet, description);
    }

    private String appendSpecialDescription(Pet pet, String baseDescription) {
        if (!pet.hasSpecialEffect()) {
            return baseDescription;
        }
        String defaultRaw = getDefaultSpecialEffectRaw(pet.getType());
        String special = ensureColored(formatSpecialEffectDescription(pet), defaultRaw);
        if (special == null || special.isEmpty()) {
            if (defaultRaw == null) {
                return baseDescription;
            }
            special = TextUtil.colorize(defaultRaw);
        }
        if (baseDescription == null || baseDescription.isEmpty()) {
            return special;
        }
        return baseDescription + " " + special;
    }

    // SprawdÄ‚â€žĂ„â€¦Ă„Ä…ÄąĹź czy gracz ma stack efektu (np. Iron Golem)
    public boolean hasEffectStack(Player player) {
        return effectStacks.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    // UÄ‚â€žĂ„â€¦Ä‚â€žĂ‹ĹĄyj stack efektu
    public void useEffectStack(Player player) {
        int stacks = effectStacks.getOrDefault(player.getUniqueId(), 0);
        if (stacks > 0) {
            effectStacks.put(player.getUniqueId(), stacks - 1);
        }
    }
}

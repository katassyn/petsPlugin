package pl.yourserver;

import org.bukkit.entity.Player;
import pl.yourserver.api.PetDungeonApi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runtime service that exposes dungeon-related pet effects for other plugins.
 */
public class PetDungeonIntegration implements PetDungeonApi {

    private static final Map<String, PetType> QUEST_TO_PET = new HashMap<>();

    static {
        register("q1", PetType.ZOMBIE);
        register("q3", PetType.ZOMBIE);
        register("q2", PetType.SPIDER);
        register("q4", PetType.SPIDER);
        register("q5", PetType.CREEPER);
        register("q8", PetType.CREEPER);
        register("q6", PetType.SKELETON);
        register("q7", PetType.SKELETON);
        register("q9", PetType.SLIME);
        register("q10", PetType.SLIME);
    }

    private final PetPlugin plugin;

    public PetDungeonIntegration(PetPlugin plugin) {
        this.plugin = plugin;
    }

    private static void register(String questId, PetType type) {
        QUEST_TO_PET.put(questId.toLowerCase(Locale.ROOT), type);
    }

    @Override
    public double getEndermanFreeTeleportChance(Player player) {
        if (player == null) {
            return 0.0;
        }
        return plugin.getPetManager().getActivePets(player).stream()
                .filter(pet -> pet.getType() == PetType.ENDERMAN)
                .findFirst()
                .map(pet -> pet.calculateBaseEffect(7.5))
                .orElse(0.0);
    }

    @Override
    public boolean shouldGrantFreeTeleport(Player player) {
        double chance = getEndermanFreeTeleportChance(player);
        return chance > 0 && ThreadLocalRandom.current().nextDouble(100.0) < chance;
    }

    @Override
    public double getDungeonQuestDamageBonus(Player player, String questId) {
        PetType type = resolveQuestPet(questId);
        if (player == null || type == null) {
            return 0.0;
        }
        return plugin.getPetManager().getActivePets(player).stream()
                .filter(pet -> pet.getType() == type)
                .findFirst()
                .map(pet -> pet.calculateBaseEffect(3.0))
                .orElse(0.0);
    }

    @Override
    public boolean hasDungeonExecuteEffect(Player player, String questId) {
        PetType type = resolveQuestPet(questId);
        if (player == null || type == null) {
            return false;
        }
        return plugin.getPetManager().getActivePets(player).stream()
                .anyMatch(pet -> pet.getType() == type && pet.hasSpecialEffect());
    }

    private PetType resolveQuestPet(String questId) {
        if (questId == null || questId.isEmpty()) {
            return null;
        }
        String normalized = questId.toLowerCase(Locale.ROOT);
        int underscore = normalized.indexOf('_');
        if (underscore > 0) {
            normalized = normalized.substring(0, underscore);
        }
        return QUEST_TO_PET.get(normalized);
    }
}


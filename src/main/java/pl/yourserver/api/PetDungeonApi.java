package pl.yourserver.api;

import org.bukkit.entity.Player;

/**
 * Public API exposed by PetPlugin for dungeon-related integrations.
 */
public interface PetDungeonApi {

    /**
     * Get the Enderman pet free teleport chance for the given player.
     *
     * @param player target player
     * @return chance percentage (0-100+)
     */
    double getEndermanFreeTeleportChance(Player player);

    /**
     * Roll whether the player should receive a free teleport based on active pets.
     *
     * @param player target player
     * @return true if teleport should be free
     */
    boolean shouldGrantFreeTeleport(Player player);

    /**
     * Get the cumulative damage bonus for a dungeon quest identifier (q1-q10).
     *
     * @param player target player
     * @param questId quest identifier (e.g. "q1")
     * @return bonus percentage
     */
    double getDungeonQuestDamageBonus(Player player, String questId);

    /**
     * Check if the player has the execute-at-threshold special effect for the provided quest.
     *
     * @param player target player
     * @param questId quest identifier (e.g. "q1")
     * @return true if execution effect is active
     */
    boolean hasDungeonExecuteEffect(Player player, String questId);
}

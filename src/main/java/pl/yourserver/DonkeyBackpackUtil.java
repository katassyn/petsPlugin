package pl.yourserver;

/**
 * Utility helpers for handling donkey backpack slot progression.
 */
public final class DonkeyBackpackUtil {

    public static final int BASE_SLOTS = 9;
    public static final int MID_SLOTS = 18;
    public static final int MAX_SLOTS = 27;
    private static final int MID_LEVEL_THRESHOLD = 20;

    private DonkeyBackpackUtil() {
    }

    public static int resolveSlots(Pet pet) {
        if (pet == null || pet.getType() != PetType.DONKEY) {
            return BASE_SLOTS;
        }
        return resolveSlots(pet.getLevel(), pet.getType().getDefaultRarity().getMaxLevel());
    }

    public static int resolveSlots(int petLevel, int maxLevel) {
        if (petLevel >= maxLevel) {
            return MAX_SLOTS;
        }
        if (petLevel >= MID_LEVEL_THRESHOLD) {
            return MID_SLOTS;
        }
        return BASE_SLOTS;
    }
}

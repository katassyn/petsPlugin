package pl.yourserver;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class PetLuckManager {

    private final PetPlugin plugin;
    private static final String LUCK_MODIFIER_NAME = "petplugin_chicken_luck";

    public PetLuckManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    public void updatePlayerLuck(Player player) {
        // Sprawdź czy gracz ma aktywnego CHICKEN peta
        if (!plugin.getPetManager().hasActivePet(player, PetType.CHICKEN)) {
            removeLuckBonus(player);
            return;
        }

        // Pobierz wartość luck z PlaceholderAPI
        String luckStr = PlaceholderAPI.setPlaceholders(player, "%petplugin_chicken_luck%");
        double luckBonus;
        try {
            luckBonus = Double.parseDouble(luckStr);
        } catch (NumberFormatException e) {
            return;
        }

        applyLuckBonus(player, luckBonus);
    }

    private void applyLuckBonus(Player player, double luckBonus) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_LUCK);
        if (attribute == null) return;

        // Usuń poprzedni modifier
        attribute.getModifiers().stream()
                .filter(mod -> mod.getName().equals(LUCK_MODIFIER_NAME))
                .forEach(attribute::removeModifier);

        // Dodaj nowy modifier jeśli bonus > 0
        if (luckBonus > 0) {
            AttributeModifier modifier = new AttributeModifier(
                    LUCK_MODIFIER_NAME,
                    luckBonus,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            attribute.addModifier(modifier);
        }
    }

    public void removeLuckBonus(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_LUCK);
        if (attribute == null) return;

        // Usuń modifier
        attribute.getModifiers().stream()
                .filter(mod -> mod.getName().equals(LUCK_MODIFIER_NAME))
                .forEach(attribute::removeModifier);
    }
}
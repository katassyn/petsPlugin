package pl.yourserver;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PetHealingManager extends BukkitRunnable {

    private final PetPlugin plugin;

    public PetHealingManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Sprawdź czy gracz ma aktywnego SHEEP peta
            if (!plugin.getPetManager().hasActivePet(player, PetType.SHEEP)) {
                continue;
            }

            // Pobierz wartość leczenia z PlaceholderAPI
            String healAmountStr = PlaceholderAPI.setPlaceholders(player, "%petplugin_sheep_heal_amount%");
            double healAmount;
            try {
                healAmount = Double.parseDouble(healAmountStr);
            } catch (NumberFormatException e) {
                continue;
            }

            // Sprawdź aktualny poziom zdrowia gracza
            double currentHealth = player.getHealth();
            double maxHealth = player.getMaxHealth();

            // Ulecz gracza jeśli nie ma pełnego życia
            if (currentHealth < maxHealth) {
                double newHealth = Math.min(currentHealth + healAmount, maxHealth);
                player.setHealth(newHealth);

                // Sheep healing - no chat message
            }
        }
    }
}
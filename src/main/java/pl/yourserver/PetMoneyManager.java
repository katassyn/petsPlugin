package pl.yourserver;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Random;

public class PetMoneyManager implements Listener {

    private final PetPlugin plugin;
    private Economy economy;
    private final Random random;

    public PetMoneyManager(PetPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null || economy == null) {
            return;
        }

        // Sprawdź czy gracz ma aktywnego PIG peta
        if (!plugin.getPetManager().hasActivePet(killer, PetType.PIG)) {
            return;
        }

        // Pobierz szansę na pieniądze z PlaceholderAPI
        String chanceStr = PlaceholderAPI.setPlaceholders(killer, "%petplugin_pig_money_chance%");
        double chance;
        try {
            chance = Double.parseDouble(chanceStr);
        } catch (NumberFormatException e) {
            return;
        }

        // Sprawdź czy wylosowano nagrodzenie
        if (random.nextDouble() * 100 < chance) {
            // Pobierz kwoty z konfiguracji
            int minMoney = plugin.getConfigManager().getPetsConfig().getInt("pets.PIG.effects.money-chance.min-money", 100);
            int maxMoney = plugin.getConfigManager().getPetsConfig().getInt("pets.PIG.effects.money-chance.max-money", 200);

            // Wylosuj kwotę
            int moneyAmount = random.nextInt(maxMoney - minMoney + 1) + minMoney;

            // Dodaj pieniądze
            economy.depositPlayer(killer, moneyAmount);

            // Powiadomienie
            killer.sendMessage(TextUtil.colorize(
                "&a[PET] &eYour Pig found &6$" + moneyAmount + "&e!"
            ));
        }
    }
}
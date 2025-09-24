package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
// Fixed imports to match actual package structure
// import pl.yourserver.petplugin.commands.PetCommand;
// import pl.yourserver.petplugin.config.ConfigManager;
// import pl.yourserver.petplugin.listeners.*;
// import pl.yourserver.petplugin.managers.*;
// import pl.yourserver.petplugin.tasks.PetFollowTask;
// import pl.yourserver.petplugin.tasks.PetEffectTask;
// import pl.yourserver.petplugin.integrations.IntegrationManager;
// import pl.yourserver.petplugin.database.DatabaseManager;

public class PetPlugin extends JavaPlugin {

    private static PetPlugin instance;
    private ConfigManager configManager;
    private PetManager petManager;
    private PetDataManager petDataManager;
    private PetEffectManager petEffectManager;
    private PetLevelManager petLevelManager;
    private IntegrationManager integrationManager;
    private DatabaseManager databaseManager;
    private PetHealthManager petHealthManager;
    private PetMoneyManager petMoneyManager;
    private PetHealingManager petHealingManager;
    private PetLuckManager petLuckManager;
    private PetInventoryManager petInventoryManager;
    private PetAuraManager petAuraManager;
    private PetCombatManager petCombatManager;
    private PetDropManager petDropManager;
    private PetFollowTask petFollowTask;
    private HeadManager headManager;

    @Override
    public void onEnable() {
        instance = this;

        // Inicjalizacja konfiguracji
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        headManager = new HeadManager(this);
        headManager.initialize();

        // Inicjalizacja bazy danych
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Inicjalizacja managerów
        petDataManager = new PetDataManager(this);
        petEffectManager = new PetEffectManager(this);
        petLevelManager = new PetLevelManager(this);
        petHealthManager = new PetHealthManager(this);
        petMoneyManager = new PetMoneyManager(this);
        petHealingManager = new PetHealingManager(this);
        petLuckManager = new PetLuckManager(this);
        petInventoryManager = new PetInventoryManager(this);
        petAuraManager = new PetAuraManager(this);
        petCombatManager = new PetCombatManager(this);
        petDropManager = new PetDropManager(this, configManager);
        petManager = new PetManager(this);

        // Integracje z innymi pluginami
        integrationManager = new IntegrationManager(this);
        integrationManager.loadIntegrations();

        // Rejestracja PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PetPlaceholderExpansion(this).register();
            getLogger().info("§a[PetPlugin] PlaceholderAPI hooked successfully!");
        }

        // Rejestracja komend
        getCommand("pet").setExecutor(new PetCommand(this));
        getCommand("pets").setExecutor(new PetCommand(this));

        // Rejestracja listenerów
        registerListeners();

        // Uruchomienie tasków
        startTasks();

        // Wczytanie petów graczy
        Bukkit.getOnlinePlayers().forEach(player -> {
            petDataManager.loadPlayerPets(player);
        });

        getLogger().info("§a[PetPlugin] Plugin został włączony pomyślnie!");
    }

    @Override
    public void onDisable() {
        // Zapisanie wszystkich petów
        if (petDataManager != null) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                petDataManager.savePlayerPets(player);
                petManager.despawnPet(player);
            });
        }

        // Zamknięcie bazy danych
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("§c[PetPlugin] Plugin został wyłączony!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PetInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        getServer().getPluginManager().registerEvents(petMoneyManager, this);
        getServer().getPluginManager().registerEvents(petInventoryManager, this);
        getServer().getPluginManager().registerEvents(petCombatManager, this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(petDropManager), this);
        getServer().getPluginManager().registerEvents(new PetBlockListener(petDropManager), this);
        getServer().getPluginManager().registerEvents(new PhysicalPetListener(this), this);
    }

    private void startTasks() {
        // Task do podążania petów za graczami
        petFollowTask = new PetFollowTask(this);
        petFollowTask.runTaskTimer(this, 0L, 5L); // Co 5 ticków (4 razy na sekundę) - lepsze dla pathfinding

        // Task do aplikowania efektów petów
        new PetEffectTask(this).runTaskTimer(this, 0L, 20L);

        // Task do leczenia SHEEP petów (co 20 sekund = 400 ticków)
        petHealingManager.runTaskTimer(this, 0L, 400L);

        // Task do aur petów (co 3 sekundy = 60 ticków)
        petAuraManager.runTaskTimer(this, 0L, 60L);
    }

    public static PetPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public PetDataManager getPetDataManager() {
        return petDataManager;
    }

    public PetEffectManager getPetEffectManager() {
        return petEffectManager;
    }

    public PetLevelManager getPetLevelManager() {
        return petLevelManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PetHealthManager getPetHealthManager() {
        return petHealthManager;
    }

    public PetMoneyManager getPetMoneyManager() {
        return petMoneyManager;
    }

    public PetLuckManager getPetLuckManager() {
        return petLuckManager;
    }

    public PetCombatManager getPetCombatManager() {
        return petCombatManager;
    }

    public PetInventoryManager getPetInventoryManager() {
        return petInventoryManager;
    }

    public PetDropManager getPetDropManager() {
        return petDropManager;
    }

    public PetFollowTask getPetFollowTask() {
        return petFollowTask;
    }

    public HeadManager getHeadManager() {
        return headManager;
    }

    public org.bukkit.NamespacedKey getNamespacedKey(String key) {
        return new org.bukkit.NamespacedKey(this, key);
    }

    public void debugPetTextures(org.bukkit.entity.Player player) {
        getLogger().info("=== Pet Textures Debug ===");

        getLogger().info("HeadDatabase available: " + headManager.isHeadDatabaseAvailable());

        for (PetType petType : PetType.values()) {
            String configuredId = configManager.getHeadDatabaseId(petType);
            if (configuredId == null || configuredId.isEmpty()) {
                getLogger().info(petType.name() + " - HeadDatabase ID: <not configured>");
            } else {
                getLogger().info(petType.name() + " - HeadDatabase ID: " + configuredId);
            }

            try {
                ItemStack headItem = headManager.getPetHead(petType);
                if (headItem != null && headItem.getType() == Material.PLAYER_HEAD && headItem.hasItemMeta()) {
                    getLogger().info(petType.name() + " - Head item generated successfully.");
                } else if (headItem != null) {
                    getLogger().warning(petType.name() + " - Head item generated with unexpected material: " + headItem.getType());
                } else {
                    getLogger().warning(petType.name() + " - Head item generation returned null.");
                }
            } catch (Exception exception) {
                getLogger().log(Level.WARNING, petType.name() + " - Exception while generating head item", exception);
            }

            String texture = petType.getSkullTexture();
            if (texture != null && !texture.isEmpty()) {
                getLogger().info(petType.name() + " - Fallback texture length: " + texture.length());
            } else {
                getLogger().info(petType.name() + " - No fallback texture configured.");
            }
        }

        player.sendMessage(TextUtil.colorize("&aDebug completed! Check console for results."));
    }
}
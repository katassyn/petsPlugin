package pl.yourserver;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
// Fixed imports to match actual package structure

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final PetPlugin plugin;
    private HikariDataSource dataSource;
    private String tablePrefix = "pets_";

    public DatabaseManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String type = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();

        HikariConfig config = new HikariConfig();

        if (type.equals("MYSQL")) {
            // MySQL configuration
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "pets");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            // SQLite configuration
            config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/pets.db");
            config.setDriverClassName("org.sqlite.JDBC");
        }

        // HikariCP settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("PetPlugin-Pool");

        dataSource = new HikariDataSource(config);

        // Create tables
        createTables();

        plugin.getLogger().info("§aDatabase connection established successfully!");
    }

    private void createTables() {
        String petsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "pets (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "type VARCHAR(50) NOT NULL," +
                "rarity VARCHAR(20) NOT NULL," +
                "level INT DEFAULT 1," +
                "experience DOUBLE DEFAULT 0," +
                "active BOOLEAN DEFAULT FALSE," +
                "last_feed_time BIGINT DEFAULT 0," +
                "feed_count INT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_owner (owner_uuid)," +
                "INDEX idx_active (active)" +
                ")";

        String backpackTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "backpacks (" +
                "owner_uuid VARCHAR(36) PRIMARY KEY," +
                "inventory TEXT," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String statsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "stats (" +
                "pet_uuid VARCHAR(36) PRIMARY KEY," +
                "mobs_killed INT DEFAULT 0," +
                "total_exp_gained DOUBLE DEFAULT 0," +
                "times_fed INT DEFAULT 0," +
                "special_effects_triggered INT DEFAULT 0," +
                "FOREIGN KEY (pet_uuid) REFERENCES " + tablePrefix + "pets(uuid) ON DELETE CASCADE" +
                ")";

        String cooldownTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "cooldowns (" +
                "player_uuid VARCHAR(36) PRIMARY KEY," +
                "iron_golem_strike BIGINT DEFAULT 0," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Dla SQLite trzeba nieco zmodyfikować składnię
            if (conn.getMetaData().getURL().contains("sqlite")) {
                petsTable = petsTable.replace("INDEX idx_owner (owner_uuid),", "");
                petsTable = petsTable.replace("INDEX idx_active (active)", "");
                petsTable = petsTable.replace("BOOLEAN", "INTEGER");

                stmt.execute(petsTable);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner ON " + tablePrefix + "pets(owner_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_active ON " + tablePrefix + "pets(active)");
            } else {
                stmt.execute(petsTable);
            }

            stmt.execute(backpackTable);
            stmt.execute(statsTable);
            stmt.execute(cooldownTable);

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables!");
            e.printStackTrace();
        }
    }

    // Zapisz peta do bazy
    public void savePet(Pet pet) {
        String query = "INSERT INTO " + tablePrefix + "pets " +
                "(uuid, owner_uuid, type, rarity, level, experience, active, last_feed_time, feed_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "level = VALUES(level), " +
                "experience = VALUES(experience), " +
                "active = VALUES(active), " +
                "last_feed_time = VALUES(last_feed_time), " +
                "feed_count = VALUES(feed_count), " +
                "updated_at = CURRENT_TIMESTAMP";

        // SQLite używa innej składni
        if (dataSource.getJdbcUrl().contains("sqlite")) {
            query = query.replace("ON DUPLICATE KEY UPDATE", "ON CONFLICT(uuid) DO UPDATE SET");
            query = query.replace("VALUES(level)", "excluded.level");
            query = query.replace("VALUES(experience)", "excluded.experience");
            query = query.replace("VALUES(active)", "excluded.active");
            query = query.replace("VALUES(last_feed_time)", "excluded.last_feed_time");
            query = query.replace("VALUES(feed_count)", "excluded.feed_count");
            query = query.replace(", updated_at = CURRENT_TIMESTAMP", "");
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, pet.getUuid().toString());
            stmt.setString(2, pet.getOwnerUUID().toString());
            stmt.setString(3, pet.getType().name());
            stmt.setString(4, pet.getRarity().name());
            stmt.setInt(5, pet.getLevel());
            stmt.setDouble(6, pet.getExperience());
            stmt.setBoolean(7, pet.isActive());
            stmt.setLong(8, pet.getLastFeedTime());
            stmt.setInt(9, pet.getFeedCount());

            stmt.executeUpdate();

            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Saved pet: " + pet.getUuid() + " for player: " + pet.getOwnerUUID());
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save pet: " + pet.getUuid());
            e.printStackTrace();
        }
    }

    // Wczytaj pety gracza
    public List<Pet> loadPlayerPets(UUID playerUUID) {
        List<Pet> pets = new ArrayList<>();
        String query = "SELECT * FROM " + tablePrefix + "pets WHERE owner_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                PetType type = PetType.valueOf(rs.getString("type"));
                PetRarity rarity = PetRarity.valueOf(rs.getString("rarity"));
                int level = rs.getInt("level");
                double experience = rs.getDouble("experience");
                boolean active = rs.getBoolean("active");
                long lastFeedTime = rs.getLong("last_feed_time");
                int feedCount = rs.getInt("feed_count");

                Pet pet = new Pet(uuid, playerUUID, type, rarity, level, experience, active, lastFeedTime, feedCount);
                pets.add(pet);
            }

            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Loaded " + pets.size() + " pets for player: " + playerUUID);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load pets for player: " + playerUUID);
            e.printStackTrace();
        }

        return pets;
    }

    // Usuń peta
    public void deletePet(UUID petUUID) {
        String query = "DELETE FROM " + tablePrefix + "pets WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, petUUID.toString());
            stmt.executeUpdate();

            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Deleted pet: " + petUUID);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete pet: " + petUUID);
            e.printStackTrace();
        }
    }

    // Zapisz backpack (dla Donkey peta)
    public void saveBackpack(UUID playerUUID, String inventoryData) {
        String query = "INSERT INTO " + tablePrefix + "backpacks (owner_uuid, inventory) " +
                "VALUES (?, ?) ON DUPLICATE KEY UPDATE inventory = VALUES(inventory), updated_at = CURRENT_TIMESTAMP";

        if (dataSource.getJdbcUrl().contains("sqlite")) {
            query = query.replace("ON DUPLICATE KEY UPDATE", "ON CONFLICT(owner_uuid) DO UPDATE SET");
            query = query.replace("VALUES(inventory)", "excluded.inventory");
            query = query.replace(", updated_at = CURRENT_TIMESTAMP", "");
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, inventoryData);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save backpack for player: " + playerUUID);
            e.printStackTrace();
        }
    }

    // Wczytaj backpack
    public String loadBackpack(UUID playerUUID) {
        String query = "SELECT inventory FROM " + tablePrefix + "backpacks WHERE owner_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("inventory");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load backpack for player: " + playerUUID);
            e.printStackTrace();
        }

        return null;
    }

    // Serializuj inventory do Base64
    private String serializeInventory(Inventory inventory) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Zapisz rozmiar inventory
            dataOutput.writeInt(inventory.getSize());

            // Zapisz wszystkie itemy
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to serialize inventory");
            e.printStackTrace();
            return "";
        }
    }

    // Deserializuj inventory z Base64
    private Inventory deserializeInventory(String data, int slots) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            int size = dataInput.readInt();
            Inventory inventory = Bukkit.createInventory(null, slots, TextUtil.colorize("&5Donkey Storage"));

            // Wczytaj wszystkie itemy
            for (int i = 0; i < size && i < slots; i++) {
                ItemStack item = (ItemStack) dataInput.readObject();
                if (item != null) {
                    inventory.setItem(i, item);
                }
            }

            dataInput.close();
            return inventory;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deserialize inventory");
            e.printStackTrace();
            return Bukkit.createInventory(null, slots, TextUtil.colorize("&5Donkey Storage"));
        }
    }

    // Zapisz backpack do bazy danych
    public void saveBackpack(UUID playerUUID, Inventory inventory) {
        String inventoryData = serializeInventory(inventory);
        saveBackpack(playerUUID, inventoryData);
    }

    // Wczytaj backpack z bazy danych
    public Inventory loadBackpackInventory(UUID playerUUID) {
        String data = loadBackpack(playerUUID);
        if (data != null && !data.isEmpty()) {
            return deserializeInventory(data, 27); // Domyślnie 27 slotów
        }
        return null;
    }

    // Zapisz cooldown
    public void saveCooldown(UUID playerUUID, String cooldownType, long timestamp) {
        String query = "INSERT INTO " + tablePrefix + "cooldowns (player_uuid, " + cooldownType + ") VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE " + cooldownType + " = ?";

        if (dataSource.getJdbcUrl().contains("sqlite")) {
            query = "INSERT INTO " + tablePrefix + "cooldowns (player_uuid, " + cooldownType + ") VALUES (?, ?) " +
                    "ON CONFLICT(player_uuid) DO UPDATE SET " + cooldownType + " = ?";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerUUID.toString());
            stmt.setLong(2, timestamp);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save cooldown for player: " + playerUUID);
            e.printStackTrace();
        }
    }

    // Wczytaj cooldown
    public long loadCooldown(UUID playerUUID, String cooldownType) {
        String query = "SELECT " + cooldownType + " FROM " + tablePrefix + "cooldowns WHERE player_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(cooldownType);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load cooldown for player: " + playerUUID);
            e.printStackTrace();
        }

        return 0;
    }

    // Aktualizuj statystyki peta
    public void updatePetStats(UUID petUUID, String stat, int increment) {
        String query = "INSERT INTO " + tablePrefix + "stats (pet_uuid, " + stat + ") VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE " + stat + " = " + stat + " + ?";

        if (dataSource.getJdbcUrl().contains("sqlite")) {
            query = "INSERT INTO " + tablePrefix + "stats (pet_uuid, " + stat + ") VALUES (?, ?) " +
                    "ON CONFLICT(pet_uuid) DO UPDATE SET " + stat + " = " + stat + " + ?";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, petUUID.toString());
            stmt.setInt(2, increment);
            stmt.setInt(3, increment);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update stats for pet: " + petUUID);
            e.printStackTrace();
        }
    }

    // Pobierz top pety
    public List<Pet> getTopPets(int limit) {
        List<Pet> pets = new ArrayList<>();
        String query = "SELECT * FROM " + tablePrefix + "pets ORDER BY level DESC, experience DESC LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                UUID ownerUUID = UUID.fromString(rs.getString("owner_uuid"));
                PetType type = PetType.valueOf(rs.getString("type"));
                PetRarity rarity = PetRarity.valueOf(rs.getString("rarity"));
                int level = rs.getInt("level");
                double experience = rs.getDouble("experience");
                boolean active = rs.getBoolean("active");
                long lastFeedTime = rs.getLong("last_feed_time");
                int feedCount = rs.getInt("feed_count");

                Pet pet = new Pet(uuid, ownerUUID, type, rarity, level, experience, active, lastFeedTime, feedCount);
                pets.add(pet);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get top pets!");
            e.printStackTrace();
        }

        return pets;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
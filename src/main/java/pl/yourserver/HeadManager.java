package pl.yourserver;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

public class HeadManager {

    private final PetPlugin plugin;
    private HeadDatabaseAPI headDatabaseAPI;
    private final Map<PetType, ItemStack> petHeadCache = new EnumMap<>(PetType.class);

    public HeadManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        petHeadCache.clear();

        if (!plugin.getConfigManager().isHeadDatabaseEnabled()) {
            headDatabaseAPI = null;
            plugin.getLogger().info("HeadDatabase integration disabled in configuration. Using internal textures.");
            return;
        }

        Plugin headDatabasePlugin = Bukkit.getPluginManager().getPlugin("HeadDatabase");
        if (headDatabasePlugin == null) {
            headDatabaseAPI = null;
            plugin.getLogger().warning("HeadDatabase plugin not found. Falling back to bundled pet textures.");
            return;
        }

        try {
            String version = headDatabasePlugin.getDescription().getVersion();
            plugin.getLogger().info("Hooked into HeadDatabase v" + version + "");
            headDatabaseAPI = new HeadDatabaseAPI();
        } catch (Throwable throwable) {
            headDatabaseAPI = null;
            plugin.getLogger().log(Level.WARNING, "Failed to initialize HeadDatabase API. Falling back to bundled textures.", throwable);
        }
    }

    public void reload() {
        initialize();
    }

    public boolean isHeadDatabaseAvailable() {
        return headDatabaseAPI != null;
    }

    public ItemStack getHeadById(String id) {
        if (!isHeadDatabaseAvailable() || id == null || id.isEmpty()) {
            return null;
        }

        try {
            ItemStack itemStack = headDatabaseAPI.getItemHead(id);
            return itemStack != null ? itemStack.clone() : null;
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Failed to fetch head with id '" + id + "' from HeadDatabase.", throwable);
            return null;
        }
    }

    public ItemStack getPetHead(PetType petType) {
        if (petType == null) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        ItemStack cached = petHeadCache.computeIfAbsent(petType, this::loadPetHead);
        return cached.clone();
    }

    private ItemStack loadPetHead(PetType petType) {
        String configuredId = plugin.getConfigManager().getHeadDatabaseId(petType);
        if (configuredId != null && !configuredId.isEmpty()) {
            ItemStack apiHead = getHeadById(configuredId);
            if (apiHead != null) {
                return apiHead;
            }
            plugin.getLogger().warning("HeadDatabase head '" + configuredId + "' for pet " + petType.name() + " could not be loaded. Using fallback texture.");
        }

        String texture = petType.getSkullTexture();
        if (texture != null && !texture.isEmpty()) {
            try {
                return new ItemBuilder(Material.PLAYER_HEAD).setSkullTexture(texture).build();
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to build fallback skull texture for pet " + petType.name() + ".", exception);
            }
        }

        return new ItemStack(Material.PLAYER_HEAD);
    }
}

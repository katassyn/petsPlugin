package pl.yourserver;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private static final PetPlugin plugin = PetPlugin.getInstance();
    private ItemStack itemStack;
    private ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder setName(String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(TextUtil.colorize(name));
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (itemMeta != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(TextUtil.colorize(line));
            }
            itemMeta.setLore(coloredLore);
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    public ItemBuilder addLore(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.getLore() != null ? itemMeta.getLore() : new ArrayList<>();
            lore.add(TextUtil.colorize(line));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder addItemFlag(ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        if (itemMeta instanceof SkullMeta) {
            ((SkullMeta) itemMeta).setOwner(owner);
        }
        return this;
    }

    public ItemBuilder setSkullTexture(String base64) {
        if (itemMeta instanceof SkullMeta && base64 != null && !base64.isEmpty()) {
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            try {
                // Use Bukkit's ProfileProperty instead of Mojang's
                // This approach works better across different server versions

                // First, try to create a profile using Bukkit's built-in methods
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);

                // Create GameProfile without importing Mojang classes
                Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                Object profile = gameProfileClass.getDeclaredConstructor(UUID.class, String.class)
                        .newInstance(UUID.randomUUID(), null);

                // Get the properties
                Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
                Object properties = getPropertiesMethod.invoke(profile);

                // Create Property
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                Object property = propertyClass.getDeclaredConstructor(String.class, String.class)
                        .newInstance("textures", base64);

                // Add property to properties using correct method
                Method putMethod = null;
                for (Method m : properties.getClass().getMethods()) {
                    if (m.getName().equals("put") && m.getParameterCount() == 2) {
                        putMethod = m;
                        break;
                    }
                }

                if (putMethod != null) {
                    putMethod.invoke(properties, "textures", property);
                }

                // Set the profile
                profileField.set(skullMeta, profile);

                // Update the item meta
                this.itemMeta = skullMeta;

            } catch (Exception e) {
                // If reflection fails, try alternative method
                try {
                    // Alternative: Use PlayerProfile (Paper/Spigot 1.12+)
                    Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
                    Method createProfileMethod = org.bukkit.Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class);
                    Object playerProfile = createProfileMethod.invoke(null, UUID.randomUUID(), "CustomHead");

                    // Set texture property
                    Method getTexturesMethod = playerProfile.getClass().getMethod("getTextures");
                    Object textures = getTexturesMethod.invoke(playerProfile);
                    Method setTextureMethod = textures.getClass().getMethod("setSkin", URL.class);

                    // Decode base64 to get URL
                    String decoded = new String(java.util.Base64.getDecoder().decode(base64));
                    if (decoded.contains("\"url\":\"")) {
                        String url = decoded.split("\"url\":\"")[1].split("\"")[0];
                        setTextureMethod.invoke(textures, new URL(url));
                    }

                    // Apply profile to skull
                    Method setPlayerProfileMethod = skullMeta.getClass().getMethod("setOwnerProfile", playerProfileClass);
                    setPlayerProfileMethod.invoke(skullMeta, playerProfile);

                    this.itemMeta = skullMeta;

                } catch (Exception e2) {
                    // Final fallback - try SkullMeta methods available in 1.20+
                    try {
                        // Try Bukkit 1.20+ PlayerProfile API
                        org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(UUID.randomUUID());

                        // Decode the base64 texture to get the URL
                        String decoded = new String(java.util.Base64.getDecoder().decode(base64));
                        if (decoded.contains("\"url\":\"")) {
                            String urlString = decoded.split("\"url\":\"")[1].split("\"")[0];
                            URL url = new URL(urlString);

                            org.bukkit.profile.PlayerTextures textures = profile.getTextures();
                            textures.setSkin(url);
                            profile.setTextures(textures);
                        }

                        skullMeta.setOwnerProfile(profile);
                        this.itemMeta = skullMeta;

                    } catch (Exception e3) {
                        // Last resort - just set a player name
                        if (plugin != null) {
                            plugin.getLogger().warning("All skull texture methods failed for base64: " + base64.substring(0, Math.min(20, base64.length())) + "...");
                        }
                        skullMeta.setOwner("MHF_Question");
                    }
                }
            }
        }
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        if (itemMeta != null) {
            itemMeta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder hideAllFlags() {
        if (itemMeta != null) {
            for (ItemFlag flag : ItemFlag.values()) {
                itemMeta.addItemFlags(flag);
            }
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}
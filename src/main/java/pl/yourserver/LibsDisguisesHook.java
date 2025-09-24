package pl.yourserver;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.logging.Level;

/**
 * Reflection based integration with LibsDisguises so the plugin can compile
 * without the API present while still supporting it at runtime when available.
 */
public class LibsDisguisesHook {

    private final PetPlugin plugin;

    private static final EnumSet<PetType> BABY_DISGUISES = EnumSet.of(
            PetType.COW,
            PetType.PIG,
            PetType.SHEEP,
            PetType.CHICKEN,
            PetType.DONKEY,
            PetType.LLAMA,
            PetType.TURTLE,
            PetType.MOOSHROOM,
            PetType.PANDA
    );

    private boolean attemptedInitialization;
    private boolean available;

    private Class<?> disguiseTypeClass;
    private Constructor<?> mobDisguiseConstructor;
    private boolean mobDisguiseUsesAdultFlag;

    private Method getTypeMethod;
    private Method valueOfMethod;
    private Method setEntityMethod;
    private Method setReplaceSoundsMethod;
    private Method setKeepDisguiseOnTeleportMethod;
    private Method getWatcherMethod;
    private Method watcherSetCustomNameMethod;
    private Method watcherSetCustomNameVisibleMethod;
    private Method flagWatcherSetYModifierMethod;
    private Method flagWatcherSetNameYModifierMethod;
    private Method disguiseEntityMethod;
    private Method isDisguisedMethod;
    private Method undisguiseToAllMethod;

    private Class<?> ageableWatcherClass;
    private Method ageableSetBabyMethod;

    private Class<?> zombieWatcherClass;
    private Method zombieSetBabyMethod;

    private Class<?> slimeWatcherClass;
    private Method slimeSetSizeMethod;

    private Class<?> phantomWatcherClass;
    private Method phantomSetSizeMethod;

    private Class<?> enderDragonWatcherClass;
    private Method enderDragonSetPhaseMethod;

    public LibsDisguisesHook(PetPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canUseDisguises() {
        if (!attemptedInitialization) {
            initialize();
        }
        return available;
    }

    public boolean isDisguised(Entity entity) {
        if (!canUseDisguises() || entity == null) {
            return false;
        }
        try {
            Object result = isDisguisedMethod.invoke(null, entity);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Failed to query LibsDisguises state: " + throwable.getMessage(), throwable);
            return false;
        }
    }

    public void undisguise(Entity entity) {
        if (!canUseDisguises() || entity == null) {
            return;
        }
        try {
            undisguiseToAllMethod.invoke(null, entity);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Failed to undisguise entity via LibsDisguises: " + throwable.getMessage(), throwable);
        }
    }

    public boolean applyDisguise(Entity entity, Pet pet) {
        if (!canUseDisguises() || entity == null || pet == null || pet.getType() == null) {
            return false;
        }

        try {
            Object disguiseType = resolveDisguiseType(pet.getType().getEntityType());
            if (disguiseType == null) {
                plugin.getLogger().warning("Unable to resolve LibsDisguises disguise type for pet " + pet.getType().name());
                return false;
            }

            Object disguise = mobDisguiseUsesAdultFlag
                    ? mobDisguiseConstructor.newInstance(disguiseType, Boolean.FALSE)
                    : mobDisguiseConstructor.newInstance(disguiseType);

            invokeOptional(setEntityMethod, disguise, entity);
            invokeOptional(setReplaceSoundsMethod, disguise, Boolean.TRUE);
            invokeOptional(setKeepDisguiseOnTeleportMethod, disguise, Boolean.TRUE);

            Object watcher = invokeOptionalWithResult(getWatcherMethod, disguise);
            if (watcher != null) {
                invokeOptional(watcherSetCustomNameMethod, watcher, entity.getCustomName());
                invokeOptional(watcherSetCustomNameVisibleMethod, watcher, entity.isCustomNameVisible());

                applyWatcherTweaks(pet, watcher);
            }

            disguiseEntityMethod.invoke(null, entity, disguise);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Failed to apply LibsDisguises disguise: " + throwable.getMessage(), throwable);
            return false;
        }
    }

    private void initialize() {
        attemptedInitialization = true;

        if (Bukkit.getPluginManager().getPlugin("LibsDisguises") == null) {
            available = false;
            return;
        }

        try {
            Class<?> disguiseAPIClass = Class.forName("me.libraryaddict.disguise.DisguiseAPI");
            disguiseTypeClass = Class.forName("me.libraryaddict.disguise.disguisetypes.DisguiseType");
            Class<?> disguiseClass = Class.forName("me.libraryaddict.disguise.disguisetypes.Disguise");
            Class<?> mobDisguiseClass = Class.forName("me.libraryaddict.disguise.disguisetypes.MobDisguise");
            Class<?> flagWatcherClass = resolveWatcherBaseClass();

            if (flagWatcherClass == null) {
                plugin.getLogger().warning("Unable to locate the LibsDisguises watcher base class. Some pet features may be limited.");
            }

            try {
                mobDisguiseConstructor = mobDisguiseClass.getConstructor(disguiseTypeClass);
                mobDisguiseUsesAdultFlag = false;
            } catch (NoSuchMethodException ignored) {
                mobDisguiseConstructor = mobDisguiseClass.getConstructor(disguiseTypeClass, boolean.class);
                mobDisguiseUsesAdultFlag = true;
            }

            try {
                getTypeMethod = disguiseTypeClass.getMethod("getType", EntityType.class);
            } catch (NoSuchMethodException ignored) {
                getTypeMethod = null;
            }
            if (disguiseTypeClass.isEnum()) {
                valueOfMethod = disguiseTypeClass.getMethod("valueOf", String.class);
            }

            setEntityMethod = findMethod(mobDisguiseClass, "setEntity", Entity.class);
            setReplaceSoundsMethod = findMethod(mobDisguiseClass, "setReplaceSounds", boolean.class);
            setKeepDisguiseOnTeleportMethod = findMethod(mobDisguiseClass, "setKeepDisguiseOnPlayerTeleport", boolean.class);
            getWatcherMethod = findMethod(mobDisguiseClass, "getWatcher");
            watcherSetCustomNameMethod = findMethod(flagWatcherClass, "setCustomName", String.class);
            watcherSetCustomNameVisibleMethod = findMethod(flagWatcherClass, "setCustomNameVisible", boolean.class);
            flagWatcherSetYModifierMethod = findMethod(flagWatcherClass, "setYModifier", float.class);
            flagWatcherSetNameYModifierMethod = findMethod(flagWatcherClass, "setNameYModifier", float.class);

            ageableWatcherClass = findClass("me.libraryaddict.disguise.disguisetypes.watchers.AgeableWatcher");
            if (ageableWatcherClass != null) {
                ageableSetBabyMethod = findMethod(ageableWatcherClass, "setBaby", boolean.class);
            }

            zombieWatcherClass = findClass("me.libraryaddict.disguise.disguisetypes.watchers.ZombieWatcher");
            if (zombieWatcherClass != null) {
                zombieSetBabyMethod = findMethod(zombieWatcherClass, "setBaby", boolean.class);
            }

            slimeWatcherClass = findClass("me.libraryaddict.disguise.disguisetypes.watchers.SlimeWatcher");
            if (slimeWatcherClass != null) {
                slimeSetSizeMethod = findMethod(slimeWatcherClass, "setSize", int.class);
            }

            phantomWatcherClass = findClass("me.libraryaddict.disguise.disguisetypes.watchers.PhantomWatcher");
            if (phantomWatcherClass != null) {
                phantomSetSizeMethod = findMethod(phantomWatcherClass, "setSize", int.class);
            }

            enderDragonWatcherClass = findClass("me.libraryaddict.disguise.disguisetypes.watchers.EnderDragonWatcher");
            if (enderDragonWatcherClass != null) {
                enderDragonSetPhaseMethod = findMethod(enderDragonWatcherClass, "setPhase", int.class);
            }

            disguiseEntityMethod = disguiseAPIClass.getMethod("disguiseEntity", Entity.class, disguiseClass);
            isDisguisedMethod = disguiseAPIClass.getMethod("isDisguised", Entity.class);
            undisguiseToAllMethod = disguiseAPIClass.getMethod("undisguiseToAll", Entity.class);

            available = true;
            plugin.getLogger().info("LibsDisguises API hooked successfully via reflection.");
        } catch (Throwable throwable) {
            available = false;
            plugin.getLogger().log(Level.WARNING, "Failed to initialize LibsDisguises reflection bridge: " + throwable.getMessage(), throwable);
        }
    }

    private Object resolveDisguiseType(EntityType entityType) {
        if (entityType == null) {
            return null;
        }

        try {
            if (getTypeMethod != null) {
                return getTypeMethod.invoke(null, entityType);
            }
            if (valueOfMethod != null) {
                return valueOfMethod.invoke(null, entityType.name());
            }
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Failed to resolve disguise type for " + entityType.name() + ": " + throwable.getMessage(), throwable);
        }
        return null;
    }

    private void applyWatcherTweaks(Pet pet, Object watcher) {
        if (pet == null || watcher == null) {
            return;
        }

        PetType type = pet.getType();
        if (type == null) {
            return;
        }

        if (ageableWatcherClass != null
                && ageableWatcherClass.isInstance(watcher)
                && BABY_DISGUISES.contains(type)) {
            invokeOptional(ageableSetBabyMethod, watcher, Boolean.TRUE);
        }

        if (type == PetType.ZOMBIE
                && zombieWatcherClass != null
                && zombieWatcherClass.isInstance(watcher)) {
            invokeOptional(zombieSetBabyMethod, watcher, Boolean.TRUE);
        }

        if (slimeWatcherClass != null && slimeWatcherClass.isInstance(watcher)) {
            invokeOptional(slimeSetSizeMethod, watcher, 1);
        }

        if (phantomWatcherClass != null && phantomWatcherClass.isInstance(watcher)) {
            invokeOptional(phantomSetSizeMethod, watcher, 1);
        }

        if (flagWatcherSetYModifierMethod != null) {
            switch (type) {
                case ENDER_DRAGON:
                    invokeOptional(flagWatcherSetYModifierMethod, watcher, -1.35F);
                    invokeOptional(flagWatcherSetNameYModifierMethod, watcher, -0.45F);
                    if (enderDragonWatcherClass != null
                            && enderDragonWatcherClass.isInstance(watcher)) {
                        invokeOptional(enderDragonSetPhaseMethod, watcher, 0);
                    }
                    break;
                case WITHER:
                    invokeOptional(flagWatcherSetYModifierMethod, watcher, -0.3F);
                    break;
                case WARDEN:
                    invokeOptional(flagWatcherSetYModifierMethod, watcher, -0.2F);
                    break;
                default:
                    break;
            }
        }
    }

    private Class<?> resolveWatcherBaseClass() {
        String[] watcherCandidates = new String[]{
                "me.libraryaddict.disguise.disguisetypes.watchers.FlagWatcher",
                "me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher",
                "me.libraryaddict.disguise.disguisetypes.watchers.DisguiseWatcher"
        };

        for (String className : watcherCandidates) {
            Class<?> watcherClass = findClass(className);
            if (watcherClass != null) {
                return watcherClass;
            }
        }

        return null;
    }

    private Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private Method findMethod(Class<?> owner, String name, Class<?>... parameters) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private void invokeOptional(Method method, Object target, Object... arguments) {
        if (method == null || target == null) {
            return;
        }
        try {
            method.invoke(target, arguments);
        } catch (Throwable ignored) {
            // Silently ignore optional invocation failures.
        }
    }

    private Object invokeOptionalWithResult(Method method, Object target, Object... arguments) {
        if (method == null || target == null) {
            return null;
        }
        try {
            return method.invoke(target, arguments);
        } catch (Throwable ignored) {
            return null;
        }
    }
}


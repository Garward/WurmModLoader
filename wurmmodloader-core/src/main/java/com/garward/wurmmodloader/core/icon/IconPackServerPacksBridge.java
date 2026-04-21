package com.garward.wurmmodloader.core.icon;

import com.garward.wurmmodloader.modloader.internal.ReflectionUtil;
import com.garward.wurmmodloader.modloader.internal.interfaces.ModEntry;
import com.garward.wurmmodloader.modloader.internal.interfaces.ModListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Bridge mod to register icon packs with ServerPacks.
 *
 * This is an internal framework mod that implements ModListener
 * to receive ServerPacks instance when it loads, avoiding classloader isolation.
 *
 * Based on Iconzz's pattern (line 35, 97-114).
 *
 * @since 1.0.0
 */
public class IconPackServerPacksBridge implements WurmServerMod, ModListener {

    private static final Logger logger = Logger.getLogger(IconPackServerPacksBridge.class.getName());
    private static Object serverPacksInstance = null;
    private static Object optionPrepend = null;
    private static Object optionForce = null;
    private static Class<?> serverPackOptions = null;

    @Override
    public void modInitialized(ModEntry<?> entry) {
        if (entry == null || !"serverpacks".equals(entry.getName())) {
            return;
        }

        try {
            // Get ServerPacks instance and enum options (bdew's pattern)
            serverPacksInstance = entry.getWurmMod();

            serverPackOptions = entry.getModClassLoader()
                .loadClass("com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks$ServerPackOptions");

            for (Object enumConstant : serverPackOptions.getEnumConstants()) {
                String name = enumConstant.toString();
                if ("PREPEND".equals(name)) {
                    optionPrepend = enumConstant;
                } else if ("FORCE".equals(name)) {
                    optionForce = enumConstant;
                }
            }

            logger.info("ServerPacks bridge initialized - icon packs can now be registered");
            logger.info(String.format("ServerPacks instance: %s", serverPacksInstance.getClass().getName()));
            logger.info(String.format("Options class: %s", serverPackOptions.getName()));

        } catch (Exception e) {
            logger.warning("Failed to initialize ServerPacks bridge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register an icon pack with ServerPacks using Iconzz's working pattern.
     * Uses byte array signature (reads JAR into memory) and ReflectionUtil.
     */
    public static void registerIconPack(Path jarFile) {
        if (serverPacksInstance == null || optionPrepend == null || optionForce == null || serverPackOptions == null) {
            logger.fine("ServerPacks not yet available, will register later");
            return;
        }

        try {
            // Read JAR into memory (Iconzz's pattern - line 224)
            byte[] packData = java.nio.file.Files.readAllBytes(jarFile);

            // Create options array with PREPEND and FORCE
            Object opts = java.lang.reflect.Array.newInstance(serverPackOptions, 2);
            java.lang.reflect.Array.set(opts, 0, optionPrepend);
            java.lang.reflect.Array.set(opts, 1, optionForce);

            // Use ReflectionUtil.getMethod with byte array signature (Iconzz line 223)
            ReflectionUtil.getMethod(serverPacksInstance.getClass(),
                "addServerPack",
                new Class[]{String.class, byte[].class, opts.getClass()})
                .invoke(serverPacksInstance, "iconpack", packData, opts);

            logger.info("✓ Registered icon pack with serverpacks (PREPEND + FORCE): " + jarFile.getFileName() + " (" + packData.length + " bytes)");

        } catch (Exception e) {
            logger.warning("Failed to register icon pack with serverpacks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // WurmServerMod implementation

    @Override
    public void preInit() {
        // No pre-initialization needed
    }

    @Override
    public void init() {
        logger.info("IconPack-ServerPacks Bridge initialized - waiting for ServerPacks to load");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}

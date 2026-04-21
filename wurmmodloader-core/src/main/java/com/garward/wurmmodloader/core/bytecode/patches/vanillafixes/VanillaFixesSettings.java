package com.garward.wurmmodloader.core.bytecode.patches.vanillafixes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Feature flags and tunables for the vanilla-fix patch family.
 *
 * <p>Loaded from {@code <server-root>/config/wurmmodloader-vanilla-fixes.properties}
 * (alongside vanilla Wurm's {@code config/logging.properties}). If the file is absent,
 * defaults apply — stock deployments get every fix enabled with no config file needed.</p>
 *
 * <p>Any value may be overridden at runtime by a matching system property (e.g.
 * {@code -Dwurmmodloader.vanilla_fixes.nextint_guards=false}) for diagnostics.</p>
 *
 * <p>Every patch under {@code patches.vanillafixes} is a non-opinionated bug fix for a
 * known vanilla defect — a crash, exception spam, or data-corruption case with a
 * citable stack trace. Behavior change is strictly "don't crash" or "don't produce
 * nonsense"; balance and feel are out of scope.</p>
 */
public final class VanillaFixesSettings {

    private static final Logger LOGGER = Logger.getLogger(VanillaFixesSettings.class.getName());

    private static final String MASTER_KEY = "enabled";
    private static final String PROP_PREFIX = "wurmmodloader.vanilla_fixes";
    private static final String CONFIG_FILENAME = "wurmmodloader-vanilla-fixes.properties";

    private static volatile Properties loaded;

    private VanillaFixesSettings() {}

    /** Master toggle: disables every vanilla fix when false. Default {@code true}. */
    public static boolean isMasterEnabled() {
        return getBool(MASTER_KEY, true);
    }

    /**
     * Per-fix toggle. Returns true only if the master is enabled AND the per-fix key is
     * enabled (default true).
     */
    public static boolean isEnabled(String key) {
        if (!isMasterEnabled()) return false;
        return getBool(key, true);
    }

    /** Minimum spell casting time in seconds after action-timer scaling. */
    public static int actionTimerMinSpell() {
        return getInt("action_timer.min_spell", 2);
    }

    /** Minimum pick-action timer in seconds; 0 disables the cap. */
    public static int actionTimerMinPick() {
        return getInt("action_timer.min_pick", 0);
    }

    /** Minimum breed-action timer in seconds; 0 disables the cap. */
    public static int actionTimerMinBreed() {
        return getInt("action_timer.min_breed", 0);
    }

    private static boolean getBool(String key, boolean defaultValue) {
        String sys = System.getProperty(PROP_PREFIX + "." + key);
        if (sys != null) return Boolean.parseBoolean(sys);
        String file = props().getProperty(key);
        if (file == null || file.trim().isEmpty()) return defaultValue;
        return Boolean.parseBoolean(file.trim());
    }

    private static int getInt(String key, int defaultValue) {
        String sys = System.getProperty(PROP_PREFIX + "." + key);
        String raw = sys != null ? sys : props().getProperty(key);
        if (raw == null || raw.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            LOGGER.warning("[VanillaFixes] Invalid integer for " + key + "=" + raw + "; using " + defaultValue);
            return defaultValue;
        }
    }

    private static Properties props() {
        Properties p = loaded;
        if (p != null) return p;
        synchronized (VanillaFixesSettings.class) {
            if (loaded != null) return loaded;
            loaded = load();
            return loaded;
        }
    }

    private static Properties load() {
        Path path = resolveConfigPath();
        Properties p = new Properties();
        if (path == null || !Files.exists(path)) {
            LOGGER.info("[VanillaFixes] No config/" + CONFIG_FILENAME + " found; using defaults");
            return p;
        }
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            LOGGER.info("[VanillaFixes] Loaded " + path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "[VanillaFixes] Failed to read " + path + "; using defaults", e);
        }
        return p;
    }

    private static Path resolveConfigPath() {
        String serverDir = System.getProperty("wurmmodloader.serverdir");
        if (serverDir == null || serverDir.isEmpty()) {
            serverDir = System.getProperty("user.dir", ".");
        }
        return Paths.get(serverDir).resolve("config").resolve(CONFIG_FILENAME);
    }
}

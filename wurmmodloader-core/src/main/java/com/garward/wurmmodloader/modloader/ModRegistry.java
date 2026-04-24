package com.garward.wurmmodloader.modloader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.interfaces.ModEntry;
import com.garward.wurmmodloader.modloader.interfaces.Reloadable;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modloader.internal.ModInfo;

/**
 * Process-wide registry of loaded mods. Populated by {@link ModLoader} once
 * discovery + init completes; consumed by the {@code #reloadmods} console
 * command to re-read each mod's {@code .properties} / {@code .config} pair
 * and invoke {@link com.garward.wurmmodloader.modloader.interfaces.Configurable}
 * and {@link Reloadable} hooks without restarting the server.
 *
 * <p>Entries are kept in boot order. The registry intentionally avoids
 * tearing down a mod on reload — event subscriptions, bytecode patches, and
 * other one-shot initialization state persist. Mods that want live-refresh
 * semantics should implement {@link Reloadable} and make the relevant work
 * idempotent.</p>
 */
public final class ModRegistry {

    private static final Logger LOGGER = Logger.getLogger(ModRegistry.class.getName());
    private static final ModRegistry INSTANCE = new ModRegistry();

    private final List<ModEntry<WurmServerMod>> entries = new ArrayList<>();

    private ModRegistry() {}

    public static ModRegistry getInstance() {
        return INSTANCE;
    }

    /** Record the fully-loaded mod list after the framework finishes boot. */
    public synchronized void register(List<? extends ModEntry<WurmServerMod>> mods) {
        entries.clear();
        for (ModEntry<WurmServerMod> e : mods) {
            entries.add(e);
        }
        LOGGER.info("[ModRegistry] Registered " + entries.size() + " mod(s) for live reload.");
    }

    public synchronized List<ModEntry<WurmServerMod>> snapshot() {
        return new ArrayList<>(entries);
    }

    /**
     * Reload every registered mod. Returns a name → status map preserving
     * boot order. Status is either {@code "ok"}, {@code "skipped"} (nothing
     * to reload — not Configurable and not Reloadable), or an error message.
     */
    public Map<String, String> reloadAll(Consumer<String> sink) {
        Map<String, String> results = new LinkedHashMap<>();
        for (ModEntry<WurmServerMod> entry : snapshot()) {
            results.put(entry.getName(), reloadOne(entry, sink));
        }
        return results;
    }

    /** Reload a single mod by name. Returns the same status strings as {@link #reloadAll}. */
    public String reloadByName(String name, Consumer<String> sink) {
        for (ModEntry<WurmServerMod> entry : snapshot()) {
            if (entry.getName().equalsIgnoreCase(name)) {
                return reloadOne(entry, sink);
            }
        }
        return "not-found";
    }

    private String reloadOne(ModEntry<WurmServerMod> entry, Consumer<String> sink) {
        Object mod = entry.getWurmMod();
        boolean isConfigurable = (mod instanceof com.garward.wurmmodloader.modloader.interfaces.Configurable)
                || (mod instanceof com.garward.wurmmodloader.modloader.internal.interfaces.Configurable);
        boolean isReloadable = mod instanceof Reloadable;

        if (!isConfigurable && !isReloadable) {
            return "skipped";
        }

        try {
            Properties refreshed = rereadProperties(entry);
            if (isConfigurable) {
                if (mod instanceof com.garward.wurmmodloader.modloader.interfaces.Configurable) {
                    ((com.garward.wurmmodloader.modloader.interfaces.Configurable) mod).configure(refreshed);
                } else {
                    ((com.garward.wurmmodloader.modloader.internal.interfaces.Configurable) mod).configure(refreshed);
                }
            }
            if (isReloadable) {
                ((Reloadable) mod).onReload();
            }
            return "ok";
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[ModRegistry] Reload failed for " + entry.getName(), t);
            if (sink != null) {
                sink.accept("  " + entry.getName() + ": ERROR — " + t.getMessage());
            }
            return "error: " + t.getMessage();
        }
    }

    /**
     * Rebuild the Properties view for a mod. Starts from the existing merged
     * set (retains jar-embedded classname/classpath + any runtime keys like
     * steamVersion) and overlays fresh contents of the external
     * {@code .properties} / {@code .config} files.
     */
    private Properties rereadProperties(ModEntry<WurmServerMod> entry) throws java.io.IOException {
        Properties merged = new Properties();
        merged.putAll(entry.getProperties());

        Path propsFile = pathFrom(entry, "propsFile");
        if (propsFile != null && Files.exists(propsFile, LinkOption.NOFOLLOW_LINKS)) {
            try (InputStream in = Files.newInputStream(propsFile)) {
                Properties fileProps = new Properties();
                fileProps.load(in);
                merged.putAll(fileProps);
            }
        }

        Path configFile = pathFrom(entry, "configFile");
        if (configFile != null && Files.exists(configFile, LinkOption.NOFOLLOW_LINKS)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                merged.load(in);
            }
        }

        // Mutate the live properties reference so subsequent lookups see the
        // refreshed values — the old Properties object is the one handed to
        // every caller via ModEntry.getProperties().
        if (entry instanceof ModInfo) {
            ((ModInfo) entry).setProperties(merged);
        } else {
            // Fallback for non-ModInfo entry types: mutate the live map in place.
            entry.getProperties().clear();
            for (String key : merged.stringPropertyNames()) {
                entry.getProperties().setProperty(key, merged.getProperty(key));
            }
        }
        return merged;
    }

    private Path pathFrom(ModEntry<WurmServerMod> entry, String field) {
        if (entry instanceof ModInfo) {
            ModInfo info = (ModInfo) entry;
            return "propsFile".equals(field) ? info.getPropsFile() : info.getConfigFile();
        }
        return null;
    }

    /** Human-readable line for a status map entry. */
    public static String formatStatus(String name, String status) {
        return String.format("  %-30s %s", name, status);
    }

    public static List<String> summary(Map<String, String> results) {
        int ok = 0, skipped = 0, errors = 0;
        for (String status : results.values()) {
            if ("ok".equals(status)) ok++;
            else if ("skipped".equals(status)) skipped++;
            else errors++;
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.format("  reloaded=%d  skipped=%d  errors=%d", ok, skipped, errors));
        return Collections.unmodifiableList(lines);
    }
}

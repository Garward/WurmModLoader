package com.garward.wurmmodloader.core.worldseed;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads {@link WorldSeedConfig} from {@code config/wurmmodloader-world-seed.yaml}
 * via SnakeYAML. Returns defaults when the file is missing or malformed — the
 * seeder must never crash boot for a bad config.
 */
public final class WorldSeedConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(WorldSeedConfigLoader.class.getName());
    private static final String CONFIG_RELATIVE_PATH = "config" + File.separator + "wurmmodloader-world-seed.yaml";

    private WorldSeedConfigLoader() {}

    /** Load config from server root (CWD at boot). Defaults on any failure. */
    public static WorldSeedConfig load() {
        return load(new File(CONFIG_RELATIVE_PATH));
    }

    static WorldSeedConfig load(File file) {
        if (!file.exists()) {
            LOGGER.info("[WorldSeed] No " + file.getPath() + " found — using default policy (center strategy).");
            return new WorldSeedConfig();
        }

        try (InputStream in = new FileInputStream(file)) {
            LoaderOptions opts = new LoaderOptions();
            opts.setNestingDepthLimit(10);
            opts.setTagInspector(new TagInspector() {
                @Override public boolean isGlobalTagAllowed(org.yaml.snakeyaml.nodes.Tag tag) {
                    String n = tag.getClassName();
                    return n.startsWith("com.garward.wurmmodloader.core.worldseed.")
                        || n.startsWith("java.lang.")
                        || n.startsWith("java.util.");
                }
            });
            Constructor ctor = new Constructor(WorldSeedConfig.class, opts);
            Yaml yaml = new Yaml(ctor);
            WorldSeedConfig cfg = yaml.load(in);
            if (cfg == null) {
                LOGGER.warning("[WorldSeed] Config file was empty — using defaults.");
                return new WorldSeedConfig();
            }
            LOGGER.info("[WorldSeed] Loaded config: enabled=" + cfg.enabled
                + " strategy=" + cfg.strategy
                + " waterBuffer=" + cfg.centerWaterBuffer
                + " flatten=" + cfg.flattenFootprint);
            return cfg;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[WorldSeed] Failed to parse " + file.getPath()
                + " — falling back to defaults. Error: " + e.getMessage(), e);
            return new WorldSeedConfig();
        }
    }
}

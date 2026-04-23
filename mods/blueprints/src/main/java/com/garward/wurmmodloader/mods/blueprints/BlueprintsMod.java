package com.garward.wurmmodloader.mods.blueprints;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.server.WorldSeededEvent;
import com.garward.wurmmodloader.api.events.server.WorldSeedResult;
import com.garward.wurmmodloader.core.event.EventBus;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Blueprints mod — imports DeedPlanner-2 {@code .dpl} files as in-world
 * structures anchored to the world seeder's picked tile.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Subscribe to {@link WorldSeededEvent} to learn the anchor tile.</li>
 *   <li>Load {@code blueprints/<filename>.dpl} from the mod config dir.</li>
 *   <li>Parse DeedPlanner XML (GZIP/base64/plain — see {@link BlueprintLoader}).</li>
 *   <li>Translate tile-relative coords → absolute WU coords and materialize
 *       walls, floors, roofs, objects via vanilla APIs (not yet implemented —
 *       this iteration just logs the parse tree).</li>
 * </ol>
 *
 * <p>Source format reference: DeedPlanner-2
 * ({@code ModSources/deedplanner-2/src/main/java/pl/wurmonline/deedplanner/data/Map.java}).
 */
public class BlueprintsMod implements WurmServerMod, Configurable {

    private static final Logger LOGGER = Logger.getLogger(BlueprintsMod.class.getName());

    private String blueprintFile = "center.dpl";
    private boolean enabled = true;
    private boolean placeOnSeeded = true;
    private Path blueprintsDir;

    @Override
    public void configure(Properties properties) {
        this.enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
        this.blueprintFile = properties.getProperty("blueprintFile", "center.dpl");
        this.placeOnSeeded = Boolean.parseBoolean(properties.getProperty("placeOnSeeded", "true"));

        String serverRoot = System.getProperty("user.dir", ".");
        this.blueprintsDir = Paths.get(serverRoot, "mods", "blueprints");
        LOGGER.info("[Blueprints] configured: enabled=" + enabled
            + " placeOnSeeded=" + placeOnSeeded
            + " file=" + blueprintFile
            + " dir=" + blueprintsDir);
    }

    /** WurmModLoader lifecycle: server is booting, mod is initializing. */
    public void onServerStarted() {
        if (!enabled) return;
        EventBus.getInstance().register(this);
        LOGGER.info("[Blueprints] Registered on EventBus — waiting for WorldSeededEvent.");
    }

    @SubscribeEvent
    public void onServerStartedEvent(ServerStartedEvent e) {
        // no-op: WorldSeededEvent fires right after this, carries the data we need
    }

    @SubscribeEvent
    public void onWorldSeeded(WorldSeededEvent e) {
        if (!placeOnSeeded) return;
        WorldSeedResult r = e.getResult();
        if (!r.isSeeded()) {
            LOGGER.info("[Blueprints] WorldSeed outcome was " + r.getOutcome()
                + " — not placing blueprint this boot.");
            return;
        }

        File bp = blueprintsDir.resolve(blueprintFile).toFile();
        if (!bp.isFile()) {
            LOGGER.warning("[Blueprints] Blueprint file not found: " + bp
                + " — drop a DeedPlanner .dpl export here to auto-place on seed.");
            return;
        }

        try {
            BlueprintLoader.Blueprint parsed = BlueprintLoader.load(bp);
            LOGGER.info("[Blueprints] Parsed '" + bp.getName() + "': "
                + parsed.summarize() + " anchor=tile(" + r.getCenterTileX()
                + "," + r.getCenterTileY() + ") — placement not yet implemented.");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                "[Blueprints] Failed to parse " + bp + " — " + t.getMessage(), t);
        }
    }
}

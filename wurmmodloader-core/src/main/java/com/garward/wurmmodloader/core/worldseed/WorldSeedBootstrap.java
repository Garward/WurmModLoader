package com.garward.wurmmodloader.core.worldseed;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstrap that fills the vanilla-server gap for custom-map owners: seeds
 * starter-town / altar / spawn rows into {@code wurmzones.db} +
 * {@code wurmitems.db} when the loaded map has no usable seed (empty or
 * out-of-bounds village rows).
 *
 * <p>Invoked directly from
 * {@link com.garward.wurmmodloader.modloader.server.ServerHook#fireOnServerPreInit()}
 * so writes land before {@code Villages.loadVillages()} reads them. The
 * corresponding {@link com.garward.wurmmodloader.api.events.server.ServerPreInitEvent}
 * is still posted on the event bus for mod subscribers.
 *
 * <h2>Default placement policy (authoritative)</h2>
 * <ul>
 *   <li>One starter town at the <b>geometric center</b> of the map
 *       ({@code worldTileSizeX/2}, {@code worldTileSizeY/2}), regardless of
 *       map dimensions. Shared spawn for all three kingdoms — not per-kingdom
 *       towns.</li>
 *   <li><b>Water-safety buffer:</b> target tile plus every tile within a
 *       20-tile radius must be dry land (height &gt; 0). If the raw center
 *       fails the check, spiral outward and place at the first dry spot
 *       that satisfies the same 20-tile buffer.</li>
 *   <li>Altar of Three + Bone Altar placed adjacent to the starter town.</li>
 *   <li><b>Optional footprint flattening</b> (off by default): level the
 *       village tile plus a configurable border (default 3 tiles each side)
 *       to the center-tile height — but only if every tile in the footprint
 *       is within {@code flattenMaxSlope} height units of center. If any tile
 *       exceeds the threshold (i.e. the town would be carved into a mountain
 *       or cliff), flattening is skipped with a warning and the raw terrain
 *       is preserved.</li>
 *   <li>Owner can override via {@code world-seed.yaml} for richer per-kingdom
 *       layouts.</li>
 * </ul>
 *
 * <h2>Status</h2>
 * Stub only. Logs intent; does not yet perform DB writes. Implementation
 * tracked in the {@code project_world_seeding_plan} memory.
 *
 * <h2>Research</h2>
 * {@code docs/research/world-generation-and-fixture-placement.md} — why this
 * exists, what vanilla does, why custom-map owners see the NW-corner cluster.
 */
public final class WorldSeedBootstrap {

    private static final Logger LOGGER = Logger.getLogger(WorldSeedBootstrap.class.getName());

    private WorldSeedBootstrap() {}

    /** Runs at {@code ServerPreInitEvent} — before {@code Villages.loadVillages()}. */
    public static void run() {
        WorldSeedConfig cfg = WorldSeedConfigLoader.load();
        if (!cfg.enabled || "off".equalsIgnoreCase(cfg.strategy)) {
            LOGGER.info("[WorldSeed] Disabled via config — skipping.");
            publish(com.garward.wurmmodloader.api.events.server.WorldSeedResult.Outcome.SKIPPED_DISABLED, 0, 0, 0, 0, 0);
            return;
        }

        int wsx;
        int wsy;
        try {
            Class<?> zones = Class.forName("com.wurmonline.server.zones.Zones");
            wsx = zones.getField("worldTileSizeX").getInt(null);
            wsy = zones.getField("worldTileSizeY").getInt(null);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Could not read Zones.worldTileSizeX/Y — skipping seeder. " + t.getMessage(), t);
            publish(com.garward.wurmmodloader.api.events.server.WorldSeedResult.Outcome.FAILED_INFRASTRUCTURE, 0, 0, 0, 0, 0);
            return;
        }

        VillagesGate.Result gate = VillagesGate.evaluate(wsx, wsy);
        if (!gate.shouldSeed) {
            // Existing permanent in-bounds village present. Don't seed a new one —
            // but always guarantee SERVERS spawn points point at that village, so a
            // fresh world never has (0,0) or out-of-bounds spawn coords. This is
            // also the force=true path: user wants spawn aligned to the actual
            // seeded starter village, not to a freshly-picked empty tile.
            VillagesGate.PrimaryVillage primary = VillagesGate.findPrimaryInBoundsVillage(wsx, wsy);
            if (primary != null) {
                LOGGER.info("[WorldSeed] Aligning SERVERS spawn points to existing primary village '"
                    + primary.name + "' (id=" + primary.id + ") at tile ("
                    + primary.centerTileX + ", " + primary.centerTileY + ")"
                    + (cfg.force ? " [force=true]" : "") + ".");
                SpawnPointWriter.updateAllKingdoms(primary.centerTileX, primary.centerTileY);
            } else {
                LOGGER.warning("[WorldSeed] Gate said SKIP but no primary village row was returned — "
                    + "SERVERS spawn points left untouched.");
            }
            publish(com.garward.wurmmodloader.api.events.server.WorldSeedResult.Outcome.SKIPPED_ALREADY_SEEDED, 0, 0, 0, wsx, wsy);
            return;
        }

        int seedX;
        int seedY;
        TerrainScan.Centroid centroid = TerrainScan.computeLandmassCentroid(wsx, wsy);
        if (centroid != null) {
            seedX = centroid.tileX;
            seedY = centroid.tileY;
            LOGGER.info("[WorldSeed] Landmass centroid: (" + seedX + ", " + seedY + ")"
                + " from " + centroid.dryTiles + " dry tiles"
                + " [geometric center was (" + (wsx / 2) + ", " + (wsy / 2) + ")].");
        } else {
            seedX = wsx / 2;
            seedY = wsy / 2;
            LOGGER.warning("[WorldSeed] Could not compute landmass centroid — falling back to geometric center.");
        }

        int searchCap = Math.max(256, (wsx * wsy) / 64);
        TerrainScan.Pick pick = TerrainScan.findNearestDry(
            seedX, seedY, cfg.centerWaterBuffer, wsx, wsy, searchCap);

        if (pick == null) {
            LOGGER.warning("[WorldSeed] No dry spot satisfying " + cfg.centerWaterBuffer
                + "-tile water buffer found within " + searchCap + " scanned tiles — seeder giving up for this boot.");
            publish(com.garward.wurmmodloader.api.events.server.WorldSeedResult.Outcome.FAILED_NO_DRY_SPOT, 0, 0, 0, wsx, wsy);
            return;
        }

        LOGGER.info("[WorldSeed] Chose starter-town center tile ("
            + pick.tileX + ", " + pick.tileY + ") h=" + pick.centerHeight
            + " [offset from landmass centroid: dx=" + (pick.tileX - seedX)
            + ", dy=" + (pick.tileY - seedY) + ", spiralStep=" + pick.stepsSearched + "].");

        // Phase 4a: update SERVERS fallback spawn coords (works even without a village).
        SpawnPointWriter.updateAllKingdoms(pick.tileX, pick.tileY);

        // Phase 4b: optional starter-town import. Raw SQL, runs *before* vanilla
        // loads Structures/Walls/Floors/Items — so inserts appear naturally in
        // memory after load without a restart. STRUCTURES.VILLAGE is stamped
        // with -1 here and backfilled by WorldSeedCompletionHandler once the
        // village row exists.
        StarterTownImporter.run(pick.tileX, pick.tileY, wsx, wsy, cfg);

        // Phase 4c (village + token + altars) runs at ServerStartedEvent via
        // WorldSeedCompletionHandler, once vanilla item/village APIs are available.
        publish(com.garward.wurmmodloader.api.events.server.WorldSeedResult.Outcome.SEEDED,
            pick.tileX, pick.tileY, pick.centerHeight, wsx, wsy);
    }

    private static void publish(com.garward.wurmmodloader.api.events.server.WorldSeedResult.Outcome outcome,
                                int tileX, int tileY, int height, int wsx, int wsy) {
        com.garward.wurmmodloader.api.events.server.WorldSeedResult r =
            new com.garward.wurmmodloader.api.events.server.WorldSeedResult(
                outcome, tileX, tileY, height, wsx, wsy);
        com.garward.wurmmodloader.api.worldseed.WorldSeedAPI.publish(r);
    }
}

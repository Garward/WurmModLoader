package com.garward.wurmmodloader.api.events.server;

/**
 * Immutable snapshot of what the framework's world seeder decided to do on
 * this boot. Available to mods via {@link WorldSeededEvent} and via the
 * static holder attached by core.
 *
 * <p>Use cases for mods:
 * <ul>
 *   <li><b>Enemy-town placer</b> (NPC-raider mods) reads the center tile
 *       and places hostile settlements well away from it.</li>
 *   <li><b>Mission/quest mods</b> anchor starter quests at the seed point.</li>
 *   <li><b>Merchant / NPC spawner mods</b> add their own entities near the
 *       seeded town without needing to duplicate the terrain-scan logic.</li>
 * </ul>
 */
public final class WorldSeedResult {

    /** Why the seeder behaved the way it did on this boot. */
    public enum Outcome {
        /** Seeded this boot (either fresh world or forced). */
        SEEDED,
        /** Skipped — world already had an in-bounds permanent village. */
        SKIPPED_ALREADY_SEEDED,
        /** Skipped — explicitly disabled via config. */
        SKIPPED_DISABLED,
        /** Attempted but no dry spot satisfied the water buffer. */
        FAILED_NO_DRY_SPOT,
        /** Attempted but the terrain / DB / server state couldn't be read. */
        FAILED_INFRASTRUCTURE
    }

    private final Outcome outcome;
    private final int centerTileX;
    private final int centerTileY;
    private final int centerHeight;
    private final int worldTileSizeX;
    private final int worldTileSizeY;

    public WorldSeedResult(Outcome outcome, int centerTileX, int centerTileY,
                           int centerHeight, int worldTileSizeX, int worldTileSizeY) {
        this.outcome = outcome;
        this.centerTileX = centerTileX;
        this.centerTileY = centerTileY;
        this.centerHeight = centerHeight;
        this.worldTileSizeX = worldTileSizeX;
        this.worldTileSizeY = worldTileSizeY;
    }

    public Outcome getOutcome()        { return outcome; }
    public boolean isSeeded()          { return outcome == Outcome.SEEDED; }
    public int  getCenterTileX()       { return centerTileX; }
    public int  getCenterTileY()       { return centerTileY; }
    public int  getCenterHeight()      { return centerHeight; }
    public int  getWorldTileSizeX()    { return worldTileSizeX; }
    public int  getWorldTileSizeY()    { return worldTileSizeY; }

    /** Convenience: tile coord → meter coord (Wurm convention: 1 tile = 4m, centered with +2). */
    public float getCenterMeterX() { return (centerTileX << 2) + 2.0f; }
    public float getCenterMeterY() { return (centerTileY << 2) + 2.0f; }

    @Override
    public String toString() {
        return "WorldSeedResult{" + outcome + " @tile(" + centerTileX + "," + centerTileY
            + ") h=" + centerHeight + " map=" + worldTileSizeX + "x" + worldTileSizeY + "}";
    }
}

package com.garward.wurmmodloader.core.worldseed;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema POJO for {@code config/wurmmodloader-world-seed.yaml}.
 *
 * <p>Public fields + no-arg constructor: required by SnakeYAML's bean-style
 * constructor. Defaults match the "zero-config works on any map" policy
 * documented in {@link WorldSeedBootstrap}.
 */
public class WorldSeedConfig {

    public boolean enabled = true;

    /**
     * Bypass the idempotency gate — seed even if the world already has an
     * in-bounds permanent village. Intended for testing or for rebuilding
     * after deleting a broken seed. Leave off for normal operation.
     */
    public boolean force = false;

    /** {@code center} | {@code manual} | {@code off}. */
    public String strategy = "center";

    /** Tile radius that must be dry land around the center pick. */
    public int centerWaterBuffer = 20;

    /** Altar names placed adjacent to the center town. Empty = no altars. */
    public List<String> centerAltars = defaultAltars();

    /**
     * Village-creation knobs for the center strategy. Power users who want to
     * materialize their own town via a mod (subscribing to {@code WorldSeededEvent})
     * should set {@code createVillage=false} — the framework will still update
     * SERVERS spawn points + scan for a dry tile, but won't touch VILLAGES / ITEMS.
     */
    public boolean createVillage = true;
    public String centerTownName = "Freedom Landing";
    public int centerTownHalfSize = 14;
    public boolean centerTownPermanent = true;
    /** Founder NPC name saved in VILLAGES.FOUNDER + MAYOR columns. */
    public String founderNpcName = "Settler";
    /** Despawn the founder NPC after village creation. Village keeps the name string. */
    public boolean despawnFounderNpc = true;

    /**
     * After creating the starter village, widen the Everybody (non-citizen) role
     * so visiting players can pass gates, lead/tame animals, harvest grass and
     * sprouts, forage, drop/pick up their own items, and push/pull/turn props —
     * basic friendly-town interactions. Does NOT grant build, terraform, destroy,
     * manage, invite, expand, or lock-related perms; the town stays uncontrollable
     * by outsiders. Turn off if a mod wants to manage the role itself.
     */
    public boolean starterTownPublicAccess = true;

    /** Manual town list, used when {@code strategy == manual}. */
    public List<Town> towns = new ArrayList<>();

    /** Optional footprint flattening under the seeded town. */
    public boolean flattenFootprint = false;
    public int flattenBorderTiles = 3;
    public int flattenMaxSlope = 40;

    /**
     * Import the bundled Winkshir starter-town snapshot (Adventure's original
     * starter village with three named buildings, fences, and furniture) and
     * relocate it to the picked tile. When false, only a bare token+deed is
     * placed — which is fine for power users who want to build their own
     * starter via a mod subscribing to {@code WorldSeededEvent}.
     *
     * <p>Gated off by default until the importer is smoke-tested end-to-end.
     */
    public boolean importStarterTown = false;
    /** Snapshot bundled name under {@code resources/worldseed/}. */
    public String starterTownSnapshot = "winkshir";

    /** Per-kingdom fallback respawn coords. Null = use center coords. */
    public FallbackSpawns fallbackSpawns;

    private static List<String> defaultAltars() {
        List<String> list = new ArrayList<>(2);
        list.add("altar_of_three");
        list.add("bone_altar");
        return list;
    }

    public static class Town {
        public String name;
        public String kingdom;
        public String spawnKingdom;
        public int tileX;
        public int tileY;
        public int size = 14;
        public boolean permanent = true;
        public List<String> altars = new ArrayList<>();
    }

    public static class FallbackSpawns {
        public Coord jennKellon;
        public Coord molRehan;
        public Coord hots;
    }

    public static class Coord {
        public int x;
        public int y;
    }
}

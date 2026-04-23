package com.garward.wurmmodloader.mods.gmtools;

import com.garward.wurmmodloader.api.events.eventlogic.area.TileAreaHandler;
import com.garward.wurmmodloader.api.events.structure.SurfaceTileChangedEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Instant-apply terrain ops on the 4 corners of a target tile. All handlers
 * are GM-gated via {@link GmToolsMod#minPower}. Broadcasts a 3×3 client
 * update window so neighbouring tiles re-render correctly, and posts a
 * {@link SurfaceTileChangedEvent} per logical edit (cache consumers like
 * livemap listen for this).
 *
 * <p>Raise/Lower dedupe shared corners: adjacent tiles share corner vertices
 * (tile (tx,ty)'s NE corner is tile (tx+1,ty)'s NW corner), so naive per-tile
 * 4-corner mutation applied additive deltas 2–4× on interior corners. Level
 * was unaffected because it sets an absolute target (idempotent). Dedupe is a
 * time-windowed {@link ThreadLocal} map keyed by corner coord — one action
 * batch completes far inside the window even at radius 7.
 */
final class TerrainHandlers {

    private TerrainHandlers() {}

    private static final int[][] CORNER_OFFSETS = {{0,0}, {1,0}, {0,1}, {1,1}};

    /**
     * Time window for dedupe. Must be longer than a single instant-timer batch (sub-100ms
     * even at r=7) but shorter than any plausible user re-click. Too long and a
     * raise-then-lower on the same tiles no-ops because corners are still "claimed" from
     * the previous action.
     */
    private static final long DEDUPE_WINDOW_MS = 200L;

    private static final ThreadLocal<Map<Long, Long>> RECENT_CORNERS =
            ThreadLocal.withInitial(HashMap::new);

    private static boolean claimCorner(int cx, int cy) {
        long key = ((long) cx << 32) | (cy & 0xFFFFFFFFL);
        Map<Long, Long> map = RECENT_CORNERS.get();
        long now = System.currentTimeMillis();
        Long prev = map.get(key);
        if (prev != null && now - prev < DEDUPE_WINDOW_MS) return false;
        map.put(key, now);
        if (map.size() > 2048) {
            map.entrySet().removeIf(e -> now - e.getValue() > DEDUPE_WINDOW_MS);
        }
        return true;
    }

    private static boolean allowed(Creature performer) {
        return performer != null && performer.getPower() >= GmToolsMod.minPower;
    }

    private static short clampHeight(int h) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Server.getMaxHeight(), h));
    }

    /** Mutate every corner of the target tile once, regardless of how many times the tile is visited in a batch. */
    private static void mutateCornersDedupe(int tx, int ty, HeightOp op) {
        for (int[] off : CORNER_OFFSETS) {
            int cx = tx + off[0];
            int cy = ty + off[1];
            if (!claimCorner(cx, cy)) continue;
            int raw = Server.surfaceMesh.getTile(cx, cy);
            short curH = Tiles.decodeHeight(raw);
            byte type = Tiles.decodeType(raw);
            byte data = Tiles.decodeData(raw);
            short newH = clampHeight(op.apply(curH));
            Server.setSurfaceTile(cx, cy, newH, type, data);
        }
    }

    /** Level is idempotent — no dedupe needed; re-hitting a corner with the same target is a no-op. */
    private static void setCornersTo(int tx, int ty, short target) {
        for (int[] off : CORNER_OFFSETS) {
            int cx = tx + off[0];
            int cy = ty + off[1];
            int raw = Server.surfaceMesh.getTile(cx, cy);
            byte type = Tiles.decodeType(raw);
            byte data = Tiles.decodeData(raw);
            Server.setSurfaceTile(cx, cy, target, type, data);
        }
    }

    private static void broadcast(int tx, int ty, boolean onSurface) {
        Players.getInstance().sendChangedTiles(tx - 1, ty - 1, 3, 3, onSurface, true);
        EventBus.getInstance().post(new SurfaceTileChangedEvent(tx, ty, onSurface));
    }

    private interface HeightOp { short apply(short cur); }

    private static abstract class Base implements TileAreaHandler {
        @Override public boolean checkSkill(Creature performer, float needed) { return allowed(performer); }
        @Override public boolean canStartOn(Creature performer, Item source, int tx, int ty, boolean surf, int tile) { return allowed(performer); }
        @Override public boolean canActOn(Creature performer, Item source, int tx, int ty, boolean surf, int tile, boolean msg) { return allowed(performer) && surf; }
        /** GM tools are instant — tiny non-zero value satisfies the framework's >0 totalTime gate. */
        @Override public float getActionTime(Creature performer, Item source, int tx, int ty, boolean surf, int tile) { return 0.001f; }
        @Override public boolean actionStarted(Creature performer, Item source, int tx, int ty, boolean surf, int tile) { return true; }
    }

    /** Level every tile corner to the performer's standing height (Z*10 → mesh short). */
    static final class Level extends Base {
        @Override
        public boolean actionCompleted(Creature performer, Item source, int tx, int ty, boolean surf, int tile, byte rarity) {
            short target = clampHeight(Math.round(performer.getPositionZ() * 10f));
            setCornersTo(tx, ty, target);
            broadcast(tx, ty, surf);
            return true;
        }
    }

    /** Raise by {@code delta} mesh units per corner. 1 unit = 1 vanilla dig (0.1m). */
    static final class Raise extends Base {
        private final int delta;
        Raise(int units) { this.delta = units; }
        @Override
        public boolean actionCompleted(Creature performer, Item source, int tx, int ty, boolean surf, int tile, byte rarity) {
            mutateCornersDedupe(tx, ty, cur -> (short) (cur + delta));
            broadcast(tx, ty, surf);
            return true;
        }
    }

    static final class Lower extends Base {
        private final int delta;
        Lower(int units) { this.delta = units; }
        @Override
        public boolean actionCompleted(Creature performer, Item source, int tx, int ty, boolean surf, int tile, byte rarity) {
            mutateCornersDedupe(tx, ty, cur -> (short) (cur - delta));
            broadcast(tx, ty, surf);
            return true;
        }
    }
}

package com.garward.wurmmodloader.core.worldseed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the surface heightmap via reflection on {@code Server.surfaceMesh} +
 * {@code Tiles.decodeHeight} so the framework module stays free of direct
 * {@code com.wurmonline.*} imports. Exposes a single operation: find the
 * nearest dry tile to a target point whose {@code bufferRadius} neighbourhood
 * is entirely above water (height &gt; 0).
 */
public final class TerrainScan {

    private static final Logger LOGGER = Logger.getLogger(TerrainScan.class.getName());

    /** Picked location, or null if no dry spot found within the search limit. */
    public static final class Pick {
        public final int tileX;
        public final int tileY;
        public final int centerHeight;
        public final int stepsSearched;
        Pick(int x, int y, int h, int steps) {
            this.tileX = x; this.tileY = y; this.centerHeight = h; this.stepsSearched = steps;
        }
    }

    public static final class Centroid {
        public final int tileX;
        public final int tileY;
        public final long dryTiles;
        Centroid(int x, int y, long n) { this.tileX = x; this.tileY = y; this.dryTiles = n; }
    }

    private TerrainScan() {}

    /**
     * Compute the centroid of above-water tiles — the "center of mass" of the
     * playable landmass. Far more useful than the raw geometric map center on
     * maps with skewed continents (e.g. Riverweave, where playable land sits
     * SE of the geometric middle).
     *
     * <p>Returns null if the mesh is unreachable or the map has no land.
     * One full O(wsx·wsy) pass — run once at boot.
     */
    public static Centroid computeLandmassCentroid(int worldSizeX, int worldSizeY) {
        Mesh mesh;
        try {
            mesh = Mesh.open();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[WorldSeed] TerrainScan.computeLandmassCentroid couldn't access mesh — " + t.getMessage(), t);
            return null;
        }
        long sumX = 0;
        long sumY = 0;
        long n = 0;
        for (int x = 0; x < worldSizeX; x++) {
            for (int y = 0; y < worldSizeY; y++) {
                if (mesh.heightAt(x, y) > 0) {
                    sumX += x;
                    sumY += y;
                    n++;
                }
            }
        }
        if (n == 0) return null;
        return new Centroid((int) (sumX / n), (int) (sumY / n), n);
    }

    /**
     * Spiral outward from ({@code centerX}, {@code centerY}) until a tile is
     * found whose {@code [-bufferRadius..+bufferRadius]} square neighbourhood
     * is all above water.
     *
     * @param maxSteps give up after this many tiles scanned
     * @return a valid pick or null if no spot passes within {@code maxSteps}
     */
    public static Pick findNearestDry(int centerX, int centerY, int bufferRadius,
                                      int worldSizeX, int worldSizeY, int maxSteps) {
        Mesh mesh;
        try {
            mesh = Mesh.open();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[WorldSeed] TerrainScan couldn't access Server.surfaceMesh — " + t.getMessage(), t);
            return null;
        }

        int[][] spiral = spiralOffsets(maxSteps);
        for (int i = 0; i < spiral.length; i++) {
            int tx = centerX + spiral[i][0];
            int ty = centerY + spiral[i][1];
            if (tx - bufferRadius < 0 || ty - bufferRadius < 0
                || tx + bufferRadius >= worldSizeX || ty + bufferRadius >= worldSizeY) continue;

            if (neighbourhoodDry(mesh, tx, ty, bufferRadius)) {
                int h = mesh.heightAt(tx, ty);
                return new Pick(tx, ty, h, i);
            }
        }
        return null;
    }

    private static boolean neighbourhoodDry(Mesh mesh, int cx, int cy, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                if (mesh.heightAt(cx + dx, cy + dy) <= 0) return false;
            }
        }
        return true;
    }

    /**
     * Build a square-spiral offset list of up to {@code cap} entries, starting
     * at (0,0). Good enough for "nearest-ish" search — not strictly nearest by
     * euclidean distance but close enough for this purpose and cheap.
     */
    private static int[][] spiralOffsets(int cap) {
        int[][] out = new int[cap][2];
        int x = 0, y = 0, dx = 0, dy = -1, n = 0;
        int bound = (int) Math.ceil(Math.sqrt(cap));
        for (int i = 0; i < bound * bound && n < cap; i++) {
            if (Math.abs(x) <= bound / 2 && Math.abs(y) <= bound / 2) {
                out[n][0] = x; out[n][1] = y; n++;
            }
            if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1 - y)) {
                int t = dx; dx = -dy; dy = t;
            }
            x += dx; y += dy;
        }
        if (n < cap) {
            int[][] trimmed = new int[n][2];
            System.arraycopy(out, 0, trimmed, 0, n);
            return trimmed;
        }
        return out;
    }

    /** Reflective wrapper around {@code Server.surfaceMesh} + {@code Tiles.decodeHeight}. */
    private static final class Mesh {
        private final Object meshIO;
        private final Method getTile;
        private final Method decodeHeight;

        private Mesh(Object meshIO, Method getTile, Method decodeHeight) {
            this.meshIO = meshIO;
            this.getTile = getTile;
            this.decodeHeight = decodeHeight;
        }

        static Mesh open() throws Exception {
            Class<?> server = Class.forName("com.wurmonline.server.Server");
            Field surface = server.getField("surfaceMesh");
            Object mesh = surface.get(null);
            if (mesh == null) throw new IllegalStateException("Server.surfaceMesh is null — terrain not yet initialized");
            Method gt = mesh.getClass().getMethod("getTile", int.class, int.class);
            Class<?> tiles = Class.forName("com.wurmonline.mesh.Tiles");
            Method dh = tiles.getMethod("decodeHeight", int.class);
            return new Mesh(mesh, gt, dh);
        }

        int heightAt(int x, int y) {
            try {
                int tile = (int) getTile.invoke(meshIO, x, y);
                return ((Short) decodeHeight.invoke(null, tile)).intValue();
            } catch (Exception e) {
                return Integer.MIN_VALUE; // treat as water — conservative
            }
        }
    }
}

package com.garward.wurmmodloader.core.worldseed;

import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Idempotency + bounds check for the world seeder.
 *
 * <p>Answers: does the current world already have at least one permanent,
 * non-disbanded village whose tile coords fit within the active map bounds?
 * If yes, the seeder skips — the map already has a usable starter town. If
 * no (empty DB, or inherited out-of-bounds rows from a stock Adventure seed),
 * the seeder proceeds.
 *
 * <p>Queries the {@code VILLAGES} table on the active world's zones database
 * via {@link DatabaseConnectionUtil#getZonesDbConnection()} — no hardcoded
 * paths, and backend-agnostic (works with the Postgres swap too).
 */
public final class VillagesGate {

    private static final Logger LOGGER = Logger.getLogger(VillagesGate.class.getName());

    private VillagesGate() {}

    /** Result bundle so callers can log why the seeder decided to run or skip. */
    public static final class Result {
        public final int totalPermanent;
        public final int inBoundsPermanent;
        public final boolean shouldSeed;

        Result(int totalPermanent, int inBoundsPermanent, boolean shouldSeed) {
            this.totalPermanent = totalPermanent;
            this.inBoundsPermanent = inBoundsPermanent;
            this.shouldSeed = shouldSeed;
        }
    }

    /**
     * @param worldTileSizeX map width in tiles
     * @param worldTileSizeY map height in tiles
     * @return result describing what's in the DB and whether to seed
     */
    public static Result evaluate(int worldTileSizeX, int worldTileSizeY) {
        int total = 0;
        int inBounds = 0;
        try {
            // DbConnector returns a shared/pooled connection — do NOT close it,
            // or subsequent callers hit "database connection closed".
            Connection c = DatabaseConnectionUtil.getZonesDbConnection();
            total = countPermanent(c);
            inBounds = countInBoundsPermanent(c, worldTileSizeX, worldTileSizeY);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Could not query VILLAGES for idempotency gate — skipping seeder to avoid duplicate inserts. "
                + "Error: " + e.getMessage(), e);
            return new Result(-1, -1, false);
        }
        boolean shouldSeed = inBounds == 0;
        LOGGER.info("[WorldSeed] VILLAGES gate: totalPermanent=" + total
            + " inBoundsPermanent=" + inBounds
            + " worldSize=" + worldTileSizeX + "x" + worldTileSizeY
            + " → " + (shouldSeed ? "SEED" : "SKIP"));
        return new Result(total, inBounds, shouldSeed);
    }

    private static int countPermanent(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM VILLAGES WHERE PERMANENT=1 AND (DISBANDED IS NULL OR DISBANDED=0)");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Locates a representative in-bounds permanent village to align SERVERS
     * spawn points against. Used when the seeder is skipping creation — we
     * still want the respawn target to land on an actual town rather than
     * stale out-of-bounds coords from a stock-Adventure seed.
     *
     * <p>Chosen as the village with the lowest ID (deterministic, usually the
     * first one inserted — matches the map owner's expected "starter town").
     *
     * @return token coords for the village, or {@code null} if none exists.
     */
    public static PrimaryVillage findPrimaryInBoundsVillage(int worldTileSizeX, int worldTileSizeY) {
        // DbConnector returns a shared/pooled connection — close only our own
        // PreparedStatement/ResultSet, never the connection itself.
        Connection c;
        try {
            c = DatabaseConnectionUtil.getZonesDbConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Could not query VILLAGES for primary-village lookup — " + e.getMessage(), e);
            return null;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT ID, NAME, STARTX, ENDX, STARTY, ENDY FROM VILLAGES "
                + "WHERE PERMANENT=1 AND (DISBANDED IS NULL OR DISBANDED=0) "
                + "AND STARTX >= 0 AND STARTX < ? AND STARTY >= 0 AND STARTY < ? "
                + "AND ENDX   >= 0 AND ENDX   < ? AND ENDY   >= 0 AND ENDY   < ? "
                + "ORDER BY ID ASC LIMIT 1")) {
            ps.setInt(1, worldTileSizeX);
            ps.setInt(2, worldTileSizeY);
            ps.setInt(3, worldTileSizeX);
            ps.setInt(4, worldTileSizeY);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                int sx = rs.getInt("STARTX");
                int ex = rs.getInt("ENDX");
                int sy = rs.getInt("STARTY");
                int ey = rs.getInt("ENDY");
                // Mirrors Village.getTokenX() fallback — midpoint of village footprint.
                int cx = sx + (ex - sx) / 2;
                int cy = sy + (ey - sy) / 2;
                return new PrimaryVillage(id, name, cx, cy);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Could not query VILLAGES for primary-village lookup — " + e.getMessage(), e);
            return null;
        }
    }

    /** Coord bundle for the spawn-alignment path. */
    public static final class PrimaryVillage {
        public final int id;
        public final String name;
        public final int centerTileX;
        public final int centerTileY;

        PrimaryVillage(int id, String name, int centerTileX, int centerTileY) {
            this.id = id;
            this.name = name;
            this.centerTileX = centerTileX;
            this.centerTileY = centerTileY;
        }
    }

    private static int countInBoundsPermanent(Connection c, int wsx, int wsy) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM VILLAGES "
                + "WHERE PERMANENT=1 AND (DISBANDED IS NULL OR DISBANDED=0) "
                + "AND STARTX >= 0 AND STARTX < ? AND STARTY >= 0 AND STARTY < ? "
                + "AND ENDX   >= 0 AND ENDX   < ? AND ENDY   >= 0 AND ENDY   < ?")) {
            ps.setInt(1, wsx);
            ps.setInt(2, wsy);
            ps.setInt(3, wsx);
            ps.setInt(4, wsy);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}

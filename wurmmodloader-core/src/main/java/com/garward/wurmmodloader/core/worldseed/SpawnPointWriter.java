package com.garward.wurmmodloader.core.worldseed;

import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes fallback kingdom spawn points (JENN-KELLON, MOL-REHAN, HOTS/LIB)
 * into the {@code SERVERS} row on {@code wurmlogin.db}.
 *
 * <p>These coords are the last-resort respawn target when a kingdom has no
 * permanent village. Updating them to a sensible in-bounds tile is the
 * single most valuable fix for custom maps — even without a seeded village
 * the world becomes playable (you spawn, you land on dry ground).
 *
 * <p>Stores TILE coordinates (not meters). All three kingdoms share the
 * same spawn when seeded by the center-strategy default.
 */
public final class SpawnPointWriter {

    private static final Logger LOGGER = Logger.getLogger(SpawnPointWriter.class.getName());

    private SpawnPointWriter() {}

    /** @return true if at least one SERVERS row was updated. */
    public static boolean updateAllKingdoms(int tileX, int tileY) {
        // DbConnector returns a shared/pooled connection — do not close it.
        Connection c;
        try {
            c = DatabaseConnectionUtil.getLoginDbConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "[WorldSeed] Failed to update SERVERS spawn points — " + e.getMessage(), e);
            return false;
        }
        try {
            int before = logCurrent(c);
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE SERVERS SET "
                + "SPAWNPOINTJENNX=?, SPAWNPOINTJENNY=?, "
                + "SPAWNPOINTLIBX=?,  SPAWNPOINTLIBY=?, "
                + "SPAWNPOINTMOLX=?,  SPAWNPOINTMOLY=?")) {
                ps.setInt(1, tileX); ps.setInt(2, tileY);
                ps.setInt(3, tileX); ps.setInt(4, tileY);
                ps.setInt(5, tileX); ps.setInt(6, tileY);
                int rows = ps.executeUpdate();
                LOGGER.info("[WorldSeed] SERVERS spawn points updated (" + rows + " row(s)): "
                    + "all kingdoms → tile (" + tileX + ", " + tileY + ") "
                    + "= meters (" + ((tileX << 2) + 2) + ", " + ((tileY << 2) + 2) + ")."
                    + " Previously-seen rows: " + before);
                return rows > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "[WorldSeed] Failed to update SERVERS spawn points — " + e.getMessage(), e);
            return false;
        }
    }

    private static int logCurrent(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT SERVER, NAME, SPAWNPOINTJENNX, SPAWNPOINTJENNY, SPAWNPOINTMOLX, SPAWNPOINTMOLY, SPAWNPOINTLIBX, SPAWNPOINTLIBY FROM SERVERS");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                n++;
                LOGGER.info("[WorldSeed]   (before) SERVERS #" + rs.getInt("SERVER") + " '" + rs.getString("NAME") + "' "
                    + "JENN=(" + rs.getInt("SPAWNPOINTJENNX") + "," + rs.getInt("SPAWNPOINTJENNY") + ") "
                    + "MOL=(" + rs.getInt("SPAWNPOINTMOLX") + "," + rs.getInt("SPAWNPOINTMOLY") + ") "
                    + "LIB=(" + rs.getInt("SPAWNPOINTLIBX") + "," + rs.getInt("SPAWNPOINTLIBY") + ")");
            }
            return n;
        }
    }
}

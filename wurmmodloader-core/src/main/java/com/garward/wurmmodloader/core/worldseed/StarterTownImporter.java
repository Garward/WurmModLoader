package com.garward.wurmmodloader.core.worldseed;

import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads a bundled starter-town snapshot (extracted offline by
 * {@code tools/worldseed/extract_winkshir.py}) and relocates it onto the
 * {@link WorldSeedBootstrap}-picked tile.
 *
 * <p>Runs at {@code ServerPreInitEvent} from {@link WorldSeedBootstrap#run} —
 * before {@code Structures.loadStructures()} / {@code Zones.loadZones()} /
 * {@code Items.loadItems()} read the tables. Inserts land in memory naturally
 * on the next load pass, no restart needed.
 *
 * <h2>Coord translation</h2>
 * For every row: {@code newTileX = rowTileX + (pickedX - origin.tileX)}, and
 * similarly for Y. Items use meter coords — tile*4 + meter-fraction preserved.
 *
 * <h2>ID remap</h2>
 * Snapshot WurmIDs must be replaced with fresh IDs from {@code WurmId} at
 * import time — otherwise they'll collide with runtime-generated IDs. A
 * {@code Map<Long, Long>} (old → new) is built as structure rows are created;
 * foreign-key columns (e.g. BUILDTILES.STRUCTUREID) are rewritten from the map.
 *
 * <h2>Wall/floor/fence/door IDs are position-derived</h2>
 * Wurm computes {@code Wall.getId()} / {@code Fence.getId()} / {@code Floor.getId()}
 * on the fly via {@link com.wurmonline.mesh.Tiles#getHouseWallId} etc. — the DB
 * row's {@code ID} column is just an autoincrement surrogate. So coord
 * translation is sufficient; the runtime-visible ID changes for free.
 *
 * <h2>ZONEID recomputation</h2>
 * Zones are 64-tile blocks on a {@code (wsx>>6) x (wsy>>6)} grid, created in
 * column-major order, so {@code id = (tileX>>6) * (wsy>>6) + (tileY>>6)} for
 * surface zones. FENCES and ITEMS carry ZONEID and must be re-derived against
 * the current world's dimensions.
 *
 * <p>STRUCTURES.VILLAGE is stamped with {@code -1} here; {@link #backfillVillageId}
 * rewrites it at {@code ServerStartedEvent} once the VILLAGES row exists.
 */
public final class StarterTownImporter {

    private static final Logger LOGGER = Logger.getLogger(StarterTownImporter.class.getName());

    /** WurmIDs of structures inserted during {@link #run} — used by {@link #backfillVillageId}
     *  to stamp the VILLAGE fk once the village row is created at ServerStartedEvent. */
    private static final List<Long> importedStructureIds = new ArrayList<>();

    private StarterTownImporter() {}

    /** Called from {@link WorldSeedBootstrap#run} at PreInit — before vanilla table loads. */
    public static void run(int pickedTileX, int pickedTileY, int wsx, int wsy, WorldSeedConfig cfg) {
        if (!cfg.importStarterTown) return;

        String name = cfg.starterTownSnapshot == null ? "winkshir" : cfg.starterTownSnapshot;
        String resourcePath = "/worldseed/" + name + ".snapshot.json";

        JsonObject snap = loadSnapshot(resourcePath);
        if (snap == null) {
            LOGGER.warning("[WorldSeed] importStarterTown=true but snapshot '" + resourcePath
                + "' could not be loaded — skipping.");
            return;
        }

        JsonObject origin = snap.getAsJsonObject("origin");
        int originX = origin.get("tileX").getAsInt();
        int originY = origin.get("tileY").getAsInt();
        int deltaX = pickedTileX - originX;
        int deltaY = pickedTileY - originY;

        JsonArray structuresArr = arrayOrEmpty(snap, "structures");
        JsonArray buildtilesArr = arrayOrEmpty(snap, "buildtiles");
        JsonArray wallsArr      = arrayOrEmpty(snap, "walls");
        JsonArray floorsArr     = arrayOrEmpty(snap, "floors");
        JsonArray fencesArr     = arrayOrEmpty(snap, "fences");
        JsonArray doorsArr      = arrayOrEmpty(snap, "doors");
        JsonArray itemsArr      = arrayOrEmpty(snap, "items");

        LOGGER.info("[WorldSeed] Starter-town snapshot '" + name + "' loaded "
            + "(structures=" + structuresArr.size() + " walls=" + wallsArr.size()
            + " floors=" + floorsArr.size() + " fences=" + fencesArr.size()
            + " doors=" + doorsArr.size() + " buildtiles=" + buildtilesArr.size()
            + " items=" + itemsArr.size() + ").");
        LOGGER.info("[WorldSeed] Translation: snapshot origin (" + originX + ", " + originY
            + ") → picked tile (" + pickedTileX + ", " + pickedTileY
            + "), delta=(" + deltaX + ", " + deltaY + ").");

        importedStructureIds.clear();

        // DbConnector returns a shared/pooled connection — do not close it.
        Connection conn;
        try {
            conn = DatabaseConnectionUtil.getZonesDbConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "[WorldSeed] Starter-town import failed — " + e.getMessage(), e);
            return;
        }
        try {
            boolean prevAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Map<Long, Long> structureIdRemap =
                    writeStructures(conn, structuresArr, deltaX, deltaY);
                int buildtilesWritten =
                    writeBuildtiles(conn, buildtilesArr, structureIdRemap, deltaX, deltaY);
                int wallsWritten =
                    writeWalls(conn, wallsArr, structureIdRemap, deltaX, deltaY);
                int floorsWritten =
                    writeFloors(conn, floorsArr, structureIdRemap, deltaX, deltaY);
                int fencesWritten =
                    writeFences(conn, fencesArr, deltaX, deltaY, wsy);
                int doorsWritten =
                    writeDoors(conn, doorsArr, wallsArr, structureIdRemap, deltaX, deltaY);
                conn.commit();

                // Items live in wurmitems.db (separate connection + separate txn).
                int itemsWritten = writeItemsInItemDb(itemsArr, deltaX, deltaY, wsy);

                LOGGER.info("[WorldSeed] Imported " + structureIdRemap.size() + " structure(s), "
                    + buildtilesWritten + " buildtile(s), " + wallsWritten + " wall(s), "
                    + floorsWritten + " floor(s), " + fencesWritten + " fence(s), "
                    + doorsWritten + " door(s), " + itemsWritten + " item(s). "
                    + "VILLAGE fk stamped at ServerStartedEvent.");
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                importedStructureIds.clear();
                throw e;
            } finally {
                try { conn.setAutoCommit(prevAuto); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "[WorldSeed] Starter-town import failed — " + e.getMessage(), e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                "[WorldSeed] Starter-town import aborted — " + e.getMessage(), e);
        }

    }

    /** Surface zone id = (tileX>>6) * (wsy>>6) + (tileY>>6). */
    private static int surfaceZoneId(int tileX, int tileY, int wsy) {
        return (tileX >> 6) * (wsy >> 6) + (tileY >> 6);
    }

    private static Map<Long, Long> writeStructures(
            Connection conn, JsonArray structures, int deltaX, int deltaY) throws SQLException {
        Map<Long, Long> remap = new HashMap<>();
        if (structures.size() == 0) return remap;

        final String sql =
            "INSERT INTO STRUCTURES (WURMID, CENTERX, CENTERY, ROOF, FINISHED, FINFINISHED, "
            + "SURFACED, NAME, WRITID, ALLOWSALLIES, ALLOWSVILLAGERS, ALLOWSKINGDOM, "
            + "STRUCTURETYPE, PLANNER, OWNERID, SETTINGS, VILLAGE) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonElement el : structures) {
                JsonObject row = el.getAsJsonObject();
                long oldId = row.get("WURMID").getAsLong();
                long newId = nextStructureId();

                ps.setLong(1, newId);
                ps.setInt(2, row.get("CENTERX").getAsInt() + deltaX);
                ps.setInt(3, row.get("CENTERY").getAsInt() + deltaY);
                ps.setInt(4, intOrZero(row, "ROOF"));
                ps.setInt(5, intOrZero(row, "FINISHED"));
                ps.setInt(6, intOrZero(row, "FINFINISHED"));
                ps.setInt(7, intOrZero(row, "SURFACED"));
                ps.setString(8, stringOrEmpty(row, "NAME"));
                ps.setLong(9, longOrZero(row, "WRITID"));
                ps.setInt(10, intOrZero(row, "ALLOWSALLIES"));
                ps.setInt(11, intOrZero(row, "ALLOWSVILLAGERS"));
                ps.setInt(12, intOrZero(row, "ALLOWSKINGDOM"));
                ps.setInt(13, intOrZero(row, "STRUCTURETYPE"));
                ps.setString(14, stringOrEmpty(row, "PLANNER"));
                ps.setLong(15, longOrZero(row, "OWNERID"));
                ps.setLong(16, longOrZero(row, "SETTINGS"));
                ps.setInt(17, -1); // VILLAGE — backfilled post-createVillage
                ps.addBatch();

                remap.put(oldId, newId);
                importedStructureIds.add(newId);
            }
            ps.executeBatch();
        }
        return remap;
    }

    private static int writeBuildtiles(
            Connection conn, JsonArray buildtiles, Map<Long, Long> structureRemap,
            int deltaX, int deltaY) throws SQLException {
        if (buildtiles.size() == 0) return 0;

        final String sql =
            "INSERT INTO BUILDTILES (STRUCTUREID, TILEX, TILEY, LAYER) VALUES (?, ?, ?, ?)";

        int count = 0;
        int skipped = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonElement el : buildtiles) {
                JsonObject row = el.getAsJsonObject();
                long oldStructId = row.get("STRUCTUREID").getAsLong();
                Long newStructId = structureRemap.get(oldStructId);
                if (newStructId == null) {
                    skipped++;
                    continue;
                }
                ps.setLong(1, newStructId);
                ps.setInt(2, row.get("TILEX").getAsInt() + deltaX);
                ps.setInt(3, row.get("TILEY").getAsInt() + deltaY);
                ps.setInt(4, intOrZero(row, "LAYER"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        if (skipped > 0) {
            LOGGER.warning("[WorldSeed] " + skipped + " buildtile(s) skipped — "
                + "STRUCTUREID not in remap (orphaned rows in snapshot).");
        }
        return count;
    }

    private static int writeWalls(
            Connection conn, JsonArray walls, Map<Long, Long> structureRemap,
            int deltaX, int deltaY) throws SQLException {
        if (walls.size() == 0) return 0;

        // WALLS.ID is INTEGER PRIMARY KEY — omit from INSERT to let SQLite autoassign.
        final String sql =
            "INSERT INTO WALLS (STRUCTURE, TYPE, LASTMAINTAINED, ORIGINALQL, CURRENTQL, DAMAGE, "
            + "TILEX, TILEY, STARTX, STARTY, ENDX, ENDY, OUTERWALL, STATE, COLOR, MATERIAL, "
            + "ISINDOOR, HEIGHTOFFSET, LAYER, WALLORIENTATION, SETTINGS) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int count = 0;
        int skipped = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonElement el : walls) {
                JsonObject row = el.getAsJsonObject();
                Long newStructId = structureRemap.get(row.get("STRUCTURE").getAsLong());
                if (newStructId == null) { skipped++; continue; }

                ps.setLong(1, newStructId);
                ps.setInt(2, intOrZero(row, "TYPE"));
                ps.setLong(3, longOrZero(row, "LASTMAINTAINED"));
                ps.setDouble(4, doubleOrZero(row, "ORIGINALQL"));
                ps.setDouble(5, doubleOrZero(row, "CURRENTQL"));
                ps.setDouble(6, doubleOrZero(row, "DAMAGE"));
                ps.setInt(7, row.get("TILEX").getAsInt() + deltaX);
                ps.setInt(8, row.get("TILEY").getAsInt() + deltaY);
                ps.setInt(9, row.get("STARTX").getAsInt() + deltaX);
                ps.setInt(10, row.get("STARTY").getAsInt() + deltaY);
                ps.setInt(11, row.get("ENDX").getAsInt() + deltaX);
                ps.setInt(12, row.get("ENDY").getAsInt() + deltaY);
                ps.setInt(13, intOrZero(row, "OUTERWALL"));
                ps.setInt(14, intOrZero(row, "STATE"));
                ps.setInt(15, intOrZero(row, "COLOR"));
                ps.setInt(16, intOrZero(row, "MATERIAL"));
                ps.setInt(17, intOrZero(row, "ISINDOOR"));
                ps.setInt(18, intOrZero(row, "HEIGHTOFFSET"));
                ps.setInt(19, intOrZero(row, "LAYER"));
                ps.setInt(20, intOrZero(row, "WALLORIENTATION"));
                ps.setInt(21, intOrZero(row, "SETTINGS"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        if (skipped > 0) {
            LOGGER.warning("[WorldSeed] " + skipped + " wall(s) skipped — STRUCTURE not in remap.");
        }
        return count;
    }

    private static int writeFloors(
            Connection conn, JsonArray floors, Map<Long, Long> structureRemap,
            int deltaX, int deltaY) throws SQLException {
        if (floors.size() == 0) return 0;

        // FLOORS.ID is INTEGER PRIMARY KEY — omit from INSERT.
        final String sql =
            "INSERT INTO FLOORS (STRUCTURE, TYPE, LASTMAINTAINED, ORIGINALQL, CURRENTQL, DAMAGE, "
            + "TILEX, TILEY, STATE, COLOR, MATERIAL, HEIGHTOFFSET, LAYER, DIR, SLOPE, "
            + "STAGECOUNT, SETTINGS) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int count = 0;
        int skipped = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonElement el : floors) {
                JsonObject row = el.getAsJsonObject();
                Long newStructId = structureRemap.get(row.get("STRUCTURE").getAsLong());
                if (newStructId == null) { skipped++; continue; }

                ps.setLong(1, newStructId);
                ps.setInt(2, intOrZero(row, "TYPE"));
                ps.setLong(3, longOrZero(row, "LASTMAINTAINED"));
                ps.setDouble(4, doubleOrZero(row, "ORIGINALQL"));
                ps.setDouble(5, doubleOrZero(row, "CURRENTQL"));
                ps.setDouble(6, doubleOrZero(row, "DAMAGE"));
                ps.setInt(7, row.get("TILEX").getAsInt() + deltaX);
                ps.setInt(8, row.get("TILEY").getAsInt() + deltaY);
                ps.setInt(9, intOrZero(row, "STATE"));
                ps.setInt(10, intOrZero(row, "COLOR"));
                ps.setInt(11, intOrZero(row, "MATERIAL"));
                ps.setInt(12, intOrZero(row, "HEIGHTOFFSET"));
                ps.setInt(13, intOrZero(row, "LAYER"));
                ps.setInt(14, intOrZero(row, "DIR"));
                ps.setInt(15, intOrZero(row, "SLOPE"));
                ps.setInt(16, intOrZero(row, "STAGECOUNT"));
                ps.setInt(17, intOrZero(row, "SETTINGS"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        if (skipped > 0) {
            LOGGER.warning("[WorldSeed] " + skipped + " floor(s) skipped — STRUCTURE not in remap.");
        }
        return count;
    }

    private static int writeFences(
            Connection conn, JsonArray fences, int deltaX, int deltaY, int wsy) throws SQLException {
        if (fences.size() == 0) return 0;

        // FENCES.ID is INTEGER PRIMARY KEY — omit to let SQLite autoassign.
        // Snapshot only captures surface fences (Winkshir is outdoor).
        final String sql =
            "INSERT INTO FENCES (TYPE, LASTMAINTAINED, ORIGINALQL, CURRENTQL, DAMAGE, "
            + "TILEX, TILEY, ZONEID, DIR, STATE, COLOR, HEIGHTOFFSET, LAYER, SETTINGS) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonElement el : fences) {
                JsonObject row = el.getAsJsonObject();
                int newTileX = row.get("TILEX").getAsInt() + deltaX;
                int newTileY = row.get("TILEY").getAsInt() + deltaY;
                ps.setInt(1, intOrZero(row, "TYPE"));
                ps.setLong(2, longOrZero(row, "LASTMAINTAINED"));
                ps.setDouble(3, doubleOrZero(row, "ORIGINALQL"));
                ps.setDouble(4, doubleOrZero(row, "CURRENTQL"));
                ps.setDouble(5, doubleOrZero(row, "DAMAGE"));
                ps.setInt(6, newTileX);
                ps.setInt(7, newTileY);
                ps.setInt(8, surfaceZoneId(newTileX, newTileY, wsy));
                ps.setInt(9, intOrZero(row, "DIR"));
                ps.setInt(10, intOrZero(row, "STATE"));
                ps.setInt(11, intOrZero(row, "COLOR"));
                ps.setInt(12, intOrZero(row, "HEIGHTOFFSET"));
                ps.setInt(13, intOrZero(row, "LAYER"));
                ps.setInt(14, intOrZero(row, "SETTINGS"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        return count;
    }

    private static int writeDoors(
            Connection conn, JsonArray doors, JsonArray walls,
            Map<Long, Long> structureRemap, int deltaX, int deltaY) throws SQLException {
        if (doors.size() == 0) return 0;

        // Wall IDs are position-derived via Tiles.getHouseWallId(x, y, heightOffset, layer, dir)
        // — same formula at load time, so we just translate the wall's anchor tile and
        // recompute to get the runtime INNERWALL value after import.
        Map<Long, JsonObject> oldWallIdIndex = new HashMap<>();
        for (JsonElement el : walls) {
            JsonObject w = el.getAsJsonObject();
            Long oldId = computeHouseWallId(w, 0, 0);
            if (oldId == null) continue;
            oldWallIdIndex.put(oldId, w);
        }

        final String sql =
            "INSERT INTO DOORS (LOCKID, NAME, SETTINGS, STRUCTURE, INNERWALL) VALUES (?, ?, ?, ?, ?)";

        int count = 0;
        int skipped = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonElement el : doors) {
                JsonObject row = el.getAsJsonObject();
                Long newStructId = structureRemap.get(row.get("STRUCTURE").getAsLong());
                if (newStructId == null) { skipped++; continue; }

                long oldInnerWall = row.get("INNERWALL").getAsLong();
                JsonObject wallRow = oldWallIdIndex.get(oldInnerWall);
                if (wallRow == null) {
                    skipped++;
                    continue;
                }
                Long newInnerWall = computeHouseWallId(wallRow, deltaX, deltaY);
                if (newInnerWall == null) { skipped++; continue; }

                ps.setLong(1, longOrValue(row, "LOCKID", -10L));
                ps.setString(2, stringOrEmpty(row, "NAME"));
                ps.setInt(3, intOrZero(row, "SETTINGS"));
                ps.setLong(4, newStructId);
                ps.setLong(5, newInnerWall);
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        if (skipped > 0) {
            LOGGER.warning("[WorldSeed] " + skipped + " door(s) skipped — "
                + "wall anchor not resolvable via Tiles.getHouseWallId.");
        }
        return count;
    }

    private static Long computeHouseWallId(JsonObject wallRow, int deltaX, int deltaY) {
        int sx = wallRow.get("STARTX").getAsInt();
        int sy = wallRow.get("STARTY").getAsInt();
        int ex = wallRow.get("ENDX").getAsInt();
        int ey = wallRow.get("ENDY").getAsInt();
        int anchorX, anchorY;
        byte dir;
        if (sy == ey) {
            anchorX = Math.min(sx, ex);
            anchorY = sy;
            dir = 0;
        } else if (sx == ex) {
            anchorX = sx;
            anchorY = Math.min(sy, ey);
            dir = 1;
        } else {
            return null; // malformed wall
        }
        int h = intOrZero(wallRow, "HEIGHTOFFSET");
        byte layer = (byte) intOrZero(wallRow, "LAYER");
        try {
            Class<?> tiles = Class.forName("com.wurmonline.mesh.Tiles");
            return (long) tiles.getMethod("getHouseWallId",
                    int.class, int.class, int.class, byte.class, byte.class)
                .invoke(null, anchorX + deltaX, anchorY + deltaY, h, layer, dir);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Tiles.getHouseWallId unavailable", e);
        }
    }

    /**
     * Template IDs we never import — {@code WorldSeedCompletionHandler} owns these.
     * <ul>
     *   <li>236 settlement token + 663 settlement deed — {@code Villages.createVillage} places them.</li>
     *   <li>327 altar_of_three + 328 bone_altar — {@code placeAltars()} handles them.</li>
     * </ul>
     */
    private static final java.util.Set<Integer> BLACKLIST_TEMPLATES =
        new java.util.HashSet<>(java.util.Arrays.asList(236, 663, 327, 328));

    private static int writeItems(
            Connection conn, JsonArray items, int deltaX, int deltaY, int wsy) throws SQLException {
        if (items.size() == 0) return 0;

        final String sql =
            "INSERT INTO ITEMS (WURMID, TEMPLATEID, NAME, QUALITYLEVEL, ORIGINALQUALITYLEVEL, "
            + "LASTMAINTAINED, OWNERID, SIZEX, SIZEY, SIZEZ, ZONEID, DAMAGE, ROTATION, PARENTID, "
            + "WEIGHT, MATERIAL, LOCKID, DESCRIPTION, BLESS, ENCHANT, TEMPERATURE, PRICE, BANKED, "
            + "AUXDATA, CREATIONDATE, CREATIONSTATE, REALTEMPLATE, WORNARMOUR, COLOR, COLOR2, "
            + "PLACE, POSX, POSY, POSZ, CREATOR, FEMALE, MAILED, MAILTIMES, RARITY, ONBRIDGE, "
            + "LASTOWNERID, SETTINGS) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        float deltaMeterX = deltaX * 4f;
        float deltaMeterY = deltaY * 4f;

        int count = 0;
        int skippedBlacklist = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonElement el : items) {
                JsonObject row = el.getAsJsonObject();
                int template = intOrZero(row, "TEMPLATEID");
                if (BLACKLIST_TEMPLATES.contains(template)) {
                    skippedBlacklist++;
                    continue;
                }
                float newPosX = (float) doubleOrZero(row, "POSX") + deltaMeterX;
                float newPosY = (float) doubleOrZero(row, "POSY") + deltaMeterY;
                int newTileX = (int) (newPosX / 4f);
                int newTileY = (int) (newPosY / 4f);

                long newWurmId;
                try {
                    Class<?> wurmId = Class.forName("com.wurmonline.server.WurmId");
                    newWurmId = (long) wurmId.getMethod("getNextItemId").invoke(null);
                } catch (ReflectiveOperationException e) {
                    throw new SQLException("WurmId.getNextItemId unavailable", e);
                }

                int idx = 1;
                ps.setLong(idx++, newWurmId);
                ps.setInt(idx++, template);
                ps.setString(idx++, stringOrEmpty(row, "NAME"));
                ps.setFloat(idx++, (float) doubleOrZero(row, "QUALITYLEVEL"));
                ps.setFloat(idx++, (float) doubleOrZero(row, "ORIGINALQUALITYLEVEL"));
                ps.setLong(idx++, longOrZero(row, "LASTMAINTAINED"));
                ps.setLong(idx++, longOrValue(row, "OWNERID", -10L));
                ps.setInt(idx++, intOrZero(row, "SIZEX"));
                ps.setInt(idx++, intOrZero(row, "SIZEY"));
                ps.setInt(idx++, intOrZero(row, "SIZEZ"));
                ps.setInt(idx++, surfaceZoneId(newTileX, newTileY, wsy));
                ps.setFloat(idx++, (float) doubleOrZero(row, "DAMAGE"));
                ps.setFloat(idx++, (float) doubleOrZero(row, "ROTATION"));
                // PARENTID: snapshot items are filtered to OWNERID=-10 (system-owned
                // at the ground). Their source PARENTIDs don't resolve here, so we
                // plant everything top-level; any container-contents got filtered
                // out by extract_winkshir.py anyway.
                ps.setLong(idx++, -10L);
                ps.setInt(idx++, intOrZero(row, "WEIGHT"));
                ps.setInt(idx++, intOrZero(row, "MATERIAL"));
                ps.setLong(idx++, longOrValue(row, "LOCKID", -10L));
                ps.setString(idx++, stringOrEmpty(row, "DESCRIPTION"));
                ps.setInt(idx++, intOrZero(row, "BLESS"));
                ps.setInt(idx++, intOrZero(row, "ENCHANT"));
                ps.setFloat(idx++, (float) doubleOrZero(row, "TEMPERATURE"));
                ps.setInt(idx++, intOrZero(row, "PRICE"));
                ps.setInt(idx++, intOrZero(row, "BANKED"));
                ps.setInt(idx++, intOrZero(row, "AUXDATA"));
                ps.setLong(idx++, longOrZero(row, "CREATIONDATE"));
                ps.setInt(idx++, intOrZero(row, "CREATIONSTATE"));
                ps.setInt(idx++, intOrValue(row, "REALTEMPLATE", -10));
                ps.setInt(idx++, intOrZero(row, "WORNARMOUR"));
                ps.setInt(idx++, intOrValue(row, "COLOR", -1));
                ps.setInt(idx++, intOrValue(row, "COLOR2", -1));
                ps.setInt(idx++, intOrZero(row, "PLACE"));
                ps.setFloat(idx++, newPosX);
                ps.setFloat(idx++, newPosY);
                ps.setFloat(idx++, (float) doubleOrZero(row, "POSZ"));
                ps.setString(idx++, stringOrEmpty(row, "CREATOR"));
                ps.setInt(idx++, intOrZero(row, "FEMALE"));
                ps.setInt(idx++, intOrZero(row, "MAILED"));
                ps.setInt(idx++, intOrZero(row, "MAILTIMES"));
                ps.setInt(idx++, intOrZero(row, "RARITY"));
                ps.setInt(idx++, intOrValue(row, "ONBRIDGE", -10));
                ps.setLong(idx++, longOrValue(row, "LASTOWNERID", -10L));
                ps.setInt(idx++, intOrZero(row, "SETTINGS"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        if (skippedBlacklist > 0) {
            LOGGER.info("[WorldSeed] " + skippedBlacklist + " item(s) blacklisted "
                + "(tokens/deeds/altars owned by WorldSeedCompletionHandler).");
        }
        return count;
    }

    private static int writeItemsInItemDb(JsonArray items, int deltaX, int deltaY, int wsy) {
        if (items.size() == 0) return 0;
        // DbConnector returns a shared/pooled connection — do not close it.
        Connection itemConn;
        try {
            itemConn = DatabaseConnectionUtil.getItemDbConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Could not open wurmitems.db connection — item import skipped: "
                    + e.getMessage(), e);
            return 0;
        }
        try {
            boolean prevAuto = itemConn.getAutoCommit();
            itemConn.setAutoCommit(false);
            try {
                int written = writeItems(itemConn, items, deltaX, deltaY, wsy);
                itemConn.commit();
                return written;
            } catch (SQLException | RuntimeException e) {
                try { itemConn.rollback(); } catch (SQLException ignored) {}
                LOGGER.log(Level.WARNING,
                    "[WorldSeed] Item import failed — " + e.getMessage(), e);
                return 0;
            } finally {
                try { itemConn.setAutoCommit(prevAuto); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Could not open wurmitems.db connection — item import skipped: "
                    + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Stamps {@code STRUCTURES.VILLAGE = villageId} for every structure row we
     * inserted during {@link #run}. Called from {@link WorldSeedCompletionHandler}
     * once the village row exists.
     */
    public static void backfillVillageId(int villageId) {
        if (importedStructureIds.isEmpty()) {
            return;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < importedStructureIds.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        final String sql = "UPDATE STRUCTURES SET VILLAGE = ? WHERE WURMID IN (" + placeholders + ")";

        // DbConnector returns a shared/pooled connection — do not close it.
        Connection conn;
        try {
            conn = DatabaseConnectionUtil.getZonesDbConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Failed to backfill STRUCTURES.VILLAGE — " + e.getMessage(), e);
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, villageId);
            for (int i = 0; i < importedStructureIds.size(); i++) {
                ps.setLong(2 + i, importedStructureIds.get(i));
            }
            int updated = ps.executeUpdate();
            LOGGER.info("[WorldSeed] Backfilled STRUCTURES.VILLAGE=" + villageId
                + " on " + updated + " of " + importedStructureIds.size() + " imported row(s).");

            // Mirror the UPDATE into live in-memory Structure objects so villagers
            // can interact without a restart. Best-effort: if the reflection path
            // drifts, DB is still correct and a restart fixes the runtime view.
            applyVillageIdToLiveStructures(villageId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Failed to backfill STRUCTURES.VILLAGE — " + e.getMessage(), e);
        }
    }

    private static void applyVillageIdToLiveStructures(int villageId) {
        try {
            Class<?> structures = Class.forName("com.wurmonline.server.structures.Structures");
            Object instance = structures.getMethod("getInstance").invoke(null);
            java.lang.reflect.Method getStruct = structures.getMethod("getStructure", long.class);

            for (long wurmId : importedStructureIds) {
                try {
                    Object s = getStruct.invoke(instance, wurmId);
                    if (s == null) continue;
                    java.lang.reflect.Method setVillage;
                    try {
                        setVillage = s.getClass().getMethod("setVillage", int.class);
                    } catch (NoSuchMethodException nsme) {
                        setVillage = s.getClass().getMethod("setVillageId", int.class);
                    }
                    setVillage.invoke(s, villageId);
                } catch (ReflectiveOperationException perRow) {
                    // Non-fatal: DB is source of truth.
                }
            }
        } catch (ReflectiveOperationException outer) {
            LOGGER.log(Level.FINE,
                "[WorldSeed] Could not mirror VILLAGE fk to live Structures (DB is authoritative): "
                    + outer.getMessage());
        }
    }

    private static long nextStructureId() {
        try {
            Class<?> wurmId = Class.forName("com.wurmonline.server.WurmId");
            return (long) wurmId.getMethod("getNextStructureId").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("WurmId.getNextStructureId() unavailable", e);
        }
    }

    private static JsonObject loadSnapshot(String resourcePath) {
        try (InputStream in = StarterTownImporter.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            JsonElement el = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "[WorldSeed] Failed to load snapshot " + resourcePath
                + " — " + e.getMessage(), e);
            return null;
        }
    }

    private static JsonArray arrayOrEmpty(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
    }

    private static double doubleOrZero(JsonObject row, String key) {
        JsonElement el = row.get(key);
        return el == null || el.isJsonNull() ? 0.0 : el.getAsDouble();
    }

    private static int intOrZero(JsonObject row, String key) {
        JsonElement el = row.get(key);
        return el == null || el.isJsonNull() ? 0 : el.getAsInt();
    }

    private static long longOrZero(JsonObject row, String key) {
        JsonElement el = row.get(key);
        return el == null || el.isJsonNull() ? 0L : el.getAsLong();
    }

    private static long longOrValue(JsonObject row, String key, long fallback) {
        JsonElement el = row.get(key);
        return el == null || el.isJsonNull() ? fallback : el.getAsLong();
    }

    private static int intOrValue(JsonObject row, String key, int fallback) {
        JsonElement el = row.get(key);
        return el == null || el.isJsonNull() ? fallback : el.getAsInt();
    }

    private static String stringOrEmpty(JsonObject row, String key) {
        JsonElement el = row.get(key);
        return el == null || el.isJsonNull() ? "" : el.getAsString();
    }
}

package com.garward.wurmmodloader.config;

import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Syncs ServerConfig values to the SERVERS table in the database.
 *
 * <p><strong>Key Behavior:</strong>
 * <ul>
 *   <li>Only writes to database if config values DIFFER from current database values</li>
 *   <li>Logs all modifications for transparency</li>
 *   <li>Uses world-folder-agnostic database connections</li>
 *   <li>Does NOT depend on Wurm static fields</li>
 * </ul>
 *
 * <p><strong>Classloader Isolation:</strong> This class is loaded in the modloader's classloader.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ServerConfigSync {

    private static final Logger logger = Logger.getLogger(ServerConfigSync.class.getName());

    private static final String UPDATE_SERVER = "UPDATE SERVERS SET " +
        "NAME=?, MOTD=?, STEAMPW=?, KINGDOM=?, " +
        "PVP=?, EPIC=?, CHALLENGE=?, HOMESERVER=?, RANDOMSPAWNS=?, " +
        "ENTRYSERVER=?, LOGINSERVER=?, ISTEST=?, LOCAL=?, MAPNAME=?, CAHELPGROUP=?, " +
        "EXTERNALIP=?, EXTERNALPORT=?, INTRASERVERADDRESS=?, INTRASERVERPORT=?, " +
        "RMIPORT=?, REGISTRATIONPORT=?, INTRASERVERPASSWORD=?, MAXPLAYERS=?, " +
        "SKILLGAINRATE=?, SKILLBASICSTART=?, SKILLMINDLOGICSTART=?, SKILLFIGHTINGSTART=?, " +
        "SKILLBODYCONTROLSTART=?, SKILLOVERALLSTART=?, " +
        "ACTIONTIMER=?, CRMOD=?, HOTADELAY=?, " +
        "MAXCREATURES=?, PERCENT_AGG_CREATURES=?, BREEDING=?, " +
        "TREEGROWTH=?, FIELDGROWTH=?, TUNNELING=?, " +
        "UPKEEP=?, MAXDEED=?, FREEDEEDS=?, TRADERMAX=?, TRADERINIT=?, KINGSMONEY=?, " +
        "SPAWNPOINTJENNX=?, SPAWNPOINTJENNY=?, SPAWNPOINTMOLX=?, SPAWNPOINTMOLY=?, SPAWNPOINTLIBX=?, SPAWNPOINTLIBY=? " +
        "WHERE SERVER=?";

    private static final String GET_SERVER = "SELECT * FROM SERVERS WHERE SERVER=?";

    /**
     * Sync config values to database.
     *
     * @param config The config to sync
     * @param serverId The server ID
     * @return true if sync completed successfully, false if skipped due to database not ready
     * @throws SQLException if database update fails
     */
    public static boolean syncToDatabase(ServerConfig config, int serverId) throws SQLException {
        // Check if database is ready before attempting sync
        if (!DatabaseConnectionUtil.isDatabaseReady()) {
            logger.warning("[ServerConfigSync] Database not ready yet - applying config directly to server memory");

            // Instead of failing, apply the config directly to the running server using reflection
            // This bypasses the database entirely and sets the values the server already loaded
            try {
                applyConfigToServerMemory(config, serverId);
                logger.info("[ServerConfigSync] Configuration applied directly to server memory");
                logger.info("[ServerConfigSync] Note: Database values NOT updated - will retry database sync");
                return false; // Return false so retry continues until database is actually synced
            } catch (Exception e) {
                logger.log(java.util.logging.Level.WARNING,
                    "[ServerConfigSync] Failed to apply config to server memory: " + e.getMessage(), e);
                logger.info("[ServerConfigSync] Will retry database sync in a few seconds");
                return false;
            }
        }

        logger.info("[ServerConfigSync] Syncing config to database for serverId=" + serverId);

        List<String> changes = new ArrayList<>();

        // The Postgres backend's dialect-rewriter proxy returns connections
        // to the pool after statement/result-set close, so reusing one
        // `conn` across multiple read/write operations has been observed
        // to throw "connection closed". Each step below opens its own.

        boolean autoNetworking;
        ServerConfig currentConfig;
        Connection conn = null;
        try {
            conn = DatabaseConnectionUtil.getLoginDbConnection();
            autoNetworking = isAutoNetworkingEnabled(conn);
            if (autoNetworking) {
                logger.info("[ServerConfigSync] AUTO_NETWORKING is enabled - skipping network field sync");
                logger.info("[ServerConfigSync] Wurm will auto-detect: externalIp, externalPort");
            }
            currentConfig = readCurrentValues(conn, serverId);
        } finally {
            DatabaseConnectionUtil.closeConnection(conn);
        }

        detectChanges(currentConfig, config, changes, autoNetworking);

        if (changes.isEmpty()) {
            logger.info("[ServerConfigSync] No changes detected, database is up to date");
            logVerification(config, serverId);
            return true;
        }

        logger.info("[ServerConfigSync] Applying " + changes.size() + " changes to database:");
        for (String change : changes) {
            logger.info("  - " + change);
        }

        conn = null;
        try {
            conn = DatabaseConnectionUtil.getLoginDbConnection();
            updateDatabase(conn, config, currentConfig, serverId, autoNetworking);
        } finally {
            DatabaseConnectionUtil.closeConnection(conn);
        }

        conn = null;
        try {
            conn = DatabaseConnectionUtil.getLoginDbConnection();
            syncServerProperties(conn, config);
        } finally {
            DatabaseConnectionUtil.closeConnection(conn);
        }

        logger.info("[ServerConfigSync] Database sync completed successfully");
        logVerification(config, serverId);
        return true;
    }

    /**
     * Proof-of-effect log: re-reads the SERVERS row we just wrote and
     * reflects into {@code Servers.localServer} for a subset of fields that
     * Wurm also caches in memory. Logs YAML / DB / Memory side-by-side with
     * ✓ or ✗ so the first-boot sync can be confirmed end-to-end.
     *
     * <p>Called from {@code ServerFullyReadyEvent} time — by then Wurm has
     * loaded its in-memory {@code ServerEntry} from the DB, so the memory
     * column reflects what the running server is actually using this boot.
     */
    private static void logVerification(ServerConfig yaml, int serverId) {
        Connection conn = null;
        try {
            // Use a fresh connection — the one used for the UPDATE may have
            // been returned to the pool by the dialect-rewriter proxy, and
            // reusing it here has been observed to throw "connection closed".
            conn = DatabaseConnectionUtil.getLoginDbConnection();
            ServerConfig db = readCurrentValues(conn, serverId);
            java.util.Map<String, Object> mem = readServerMemoryFields();

            logger.info("[ServerConfigSync] ===== SYNC VERIFICATION (YAML / DB / Memory) =====");
            row("skills.gainRate",    yaml.skills.gainRate,    db.skills.gainRate,    mem.get("skillGainRate"));
            row("combat.actionSpeed", yaml.combat.actionSpeed, db.combat.actionSpeed, mem.get("actionTimer"));
            row("combat.ratingMod",   yaml.combat.ratingModifier, db.combat.ratingModifier, mem.get("combatRatingModifier"));
            row("creatures.maxTotal", yaml.creatures.maxTotal, db.creatures.maxTotal, mem.get("maxCreatures"));
            // DB-only (not reflected into a known static field):
            row("server.name",        yaml.server.name,        db.server.name,        null);
            row("economy.maxDeedSize", yaml.economy.maxDeedSize, db.economy.maxDeedSize, null);
            row("world.treeGrowth",   yaml.world.treeGrowth,   db.world.treeGrowth,   null);
            logger.info("[ServerConfigSync] =================================================");
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.WARNING,
                "[ServerConfigSync] Verification logging failed (non-fatal)", t);
        } finally {
            DatabaseConnectionUtil.closeConnection(conn);
        }
    }

    private static void row(String name, Object yaml, Object db, Object mem) {
        boolean yamlDb  = java.util.Objects.equals(String.valueOf(yaml), String.valueOf(db));
        boolean yamlMem = mem == null || java.util.Objects.equals(String.valueOf(yaml), String.valueOf(mem));
        String memStr = (mem == null) ? "—" : String.valueOf(mem);
        String mark = (yamlDb && yamlMem) ? "✓" : "✗";
        logger.info(String.format("[ServerConfigSync]   %s %-24s  YAML=%s  DB=%s  MEM=%s",
            mark, name, yaml, db, memStr));
    }

    private static java.util.Map<String, Object> readServerMemoryFields() {
        java.util.Map<String, Object> out = new java.util.HashMap<>();
        try {
            Class<?> serversClass;
            try {
                serversClass = Class.forName("com.wurmonline.server.Servers");
            } catch (ClassNotFoundException cnf) {
                ClassLoader ctx = Thread.currentThread().getContextClassLoader();
                serversClass = Class.forName("com.wurmonline.server.Servers", true, ctx);
            }
            Object localServer = serversClass.getField("localServer").get(null);
            if (localServer == null) {
                logger.warning("[ServerConfigSync] Servers.localServer is null — memory verification unavailable");
                return out;
            }
            Class<?> entry = localServer.getClass();
            // Public fields first (maxCreatures), then getters for private fields.
            for (String f : new String[]{"maxCreatures"}) {
                try { out.put(f, entry.getField(f).get(localServer)); }
                catch (NoSuchFieldException nsfe) {
                    logger.fine("[ServerConfigSync] ServerEntry." + f + " not present on this Wurm version");
                }
            }
            for (String[] g : new String[][]{
                    {"skillGainRate",        "getSkillGainRate"},
                    {"actionTimer",          "getActionTimer"},
                    {"combatRatingModifier", "getCombatRatingModifier"}}) {
                try { out.put(g[0], entry.getMethod(g[1]).invoke(localServer)); }
                catch (NoSuchMethodException nsme) {
                    logger.fine("[ServerConfigSync] ServerEntry." + g[1] + "() not present on this Wurm version");
                }
            }
            if (out.isEmpty()) {
                logger.warning("[ServerConfigSync] ServerEntry has none of the expected fields; class="
                    + entry.getName());
            }
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.WARNING,
                "[ServerConfigSync] Could not read Servers.localServer", t);
        }
        return out;
    }

    /**
     * Sync config values to database using a DIRECT connection (bypassing connection pool).
     *
     * <p>This method creates a fresh SQLite connection directly to the database file,
     * bypassing DbConnector's connection pool which may have stale connections during early startup.</p>
     *
     * @param config The config to sync
     * @param serverId The server ID
     * @param dbPath Path to the wurmlogin.db file (e.g., from Constants.dbHost)
     * @throws SQLException if database update fails
     */
    public static void syncToDatabaseDirect(ServerConfig config, int serverId, String dbPath) throws SQLException {
        logger.info("[ServerConfigSync] Syncing config to database using direct connection (bypassing pool)");
        logger.info("[ServerConfigSync] Database path: " + dbPath);

        Connection conn = null;
        try {
            // Create a fresh SQLite connection directly (bypassing the pool)
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            List<String> changes = new ArrayList<>();

            // Check if auto-networking is enabled
            boolean autoNetworking = isAutoNetworkingEnabled(conn);
            if (autoNetworking) {
                logger.info("[ServerConfigSync] AUTO_NETWORKING is enabled - skipping network field sync");
            }

            // Read current database values
            ServerConfig currentConfig = readCurrentValues(conn, serverId);

            // Compare and track changes (skip network fields if auto-networking enabled)
            detectChanges(currentConfig, config, changes, autoNetworking);

            if (changes.isEmpty()) {
                logger.info("[ServerConfigSync] No changes detected, database is up to date");
                return;
            }

            // Apply changes
            logger.info("[ServerConfigSync] Applying " + changes.size() + " changes to database:");
            for (String change : changes) {
                logger.info("  - " + change);
            }

            updateDatabase(conn, config, currentConfig, serverId, autoNetworking);
            syncServerProperties(conn, config);

            logger.info("[ServerConfigSync] Database sync completed successfully (direct connection)");

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warning("[ServerConfigSync] Failed to close direct connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Read current values from database.
     */
    private static ServerConfig readCurrentValues(Connection conn, int serverId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(GET_SERVER);
            ps.setInt(1, serverId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Server ID " + serverId + " not found in SERVERS table");
            }

            // Use generateFromResultSet to avoid opening a second database connection
            return ServerConfigGenerator.generateFromResultSet(rs);

        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Detect changes between current and new config.
     */
    private static void detectChanges(ServerConfig current, ServerConfig newConfig, List<String> changes, boolean skipNetworkFields) {
        // Server identity
        if (!equals(current.server.name, newConfig.server.name)) {
            changes.add("server.name: \"" + current.server.name + "\" → \"" + newConfig.server.name + "\"");
        }
        if (!equals(current.server.motd, newConfig.server.motd)) {
            changes.add("server.motd: \"" + current.server.motd + "\" → \"" + newConfig.server.motd + "\"");
        }
        if (!equals(current.server.steamPassword, newConfig.server.steamPassword)) {
            changes.add("server.steamPassword: [hidden] → [hidden]");
        }
        if (current.server.kingdom != newConfig.server.kingdom) {
            changes.add("server.kingdom: " + current.server.kingdom + " → " + newConfig.server.kingdom);
        }
        if (current.server.pvp != newConfig.server.pvp) {
            changes.add("server.pvp: " + current.server.pvp + " → " + newConfig.server.pvp);
        }
        if (current.server.epic != newConfig.server.epic) {
            changes.add("server.epic: " + current.server.epic + " → " + newConfig.server.epic);
        }
        if (current.server.challenge != newConfig.server.challenge) {
            changes.add("server.challenge: " + current.server.challenge + " → " + newConfig.server.challenge);
        }
        if (current.server.homeServer != newConfig.server.homeServer) {
            changes.add("server.homeServer: " + current.server.homeServer + " → " + newConfig.server.homeServer);
        }
        if (current.server.randomSpawns != newConfig.server.randomSpawns) {
            changes.add("server.randomSpawns: " + current.server.randomSpawns + " → " + newConfig.server.randomSpawns);
        }
        if (current.server.entryServer != newConfig.server.entryServer) {
            changes.add("server.entryServer: " + current.server.entryServer + " → " + newConfig.server.entryServer);
        }
        if (current.server.loginServer != newConfig.server.loginServer) {
            changes.add("server.loginServer: " + current.server.loginServer + " → " + newConfig.server.loginServer);
        }
        if (current.server.isTest != newConfig.server.isTest) {
            changes.add("server.isTest: " + current.server.isTest + " → " + newConfig.server.isTest);
        }
        if (current.server.local != newConfig.server.local) {
            changes.add("server.local: " + current.server.local + " → " + newConfig.server.local);
        }
        if (!equals(current.server.mapName, newConfig.server.mapName)) {
            changes.add("server.mapName: \"" + current.server.mapName + "\" → \"" + newConfig.server.mapName + "\"");
        }
        if (current.server.caHelpGroup != newConfig.server.caHelpGroup) {
            changes.add("server.caHelpGroup: " + current.server.caHelpGroup + " → " + newConfig.server.caHelpGroup);
        }

        // Network settings (skip if auto-networking is enabled - Wurm manages these automatically)
        if (!skipNetworkFields) {
            if (!equals(current.server.externalIp, newConfig.server.externalIp)) {
                changes.add("server.externalIp: \"" + current.server.externalIp + "\" → \"" + newConfig.server.externalIp + "\"");
            }
            if (!equals(current.server.externalPort, newConfig.server.externalPort)) {
                changes.add("server.externalPort: \"" + current.server.externalPort + "\" → \"" + newConfig.server.externalPort + "\"");
            }
            if (!equals(current.server.internalIp, newConfig.server.internalIp)) {
                changes.add("server.internalIp: \"" + current.server.internalIp + "\" → \"" + newConfig.server.internalIp + "\"");
            }
            if (!equals(current.server.internalPort, newConfig.server.internalPort)) {
                changes.add("server.internalPort: \"" + current.server.internalPort + "\" → \"" + newConfig.server.internalPort + "\"");
            }
            if (!equals(current.server.rmiPort, newConfig.server.rmiPort)) {
                changes.add("server.rmiPort: \"" + current.server.rmiPort + "\" → \"" + newConfig.server.rmiPort + "\"");
            }
            if (!equals(current.server.rmiRegPort, newConfig.server.rmiRegPort)) {
                changes.add("server.rmiRegPort: \"" + current.server.rmiRegPort + "\" → \"" + newConfig.server.rmiRegPort + "\"");
            }
            if (!equals(current.server.intraServerPassword, newConfig.server.intraServerPassword)) {
                changes.add("server.intraServerPassword: [hidden] → [hidden]");
            }
            if (current.server.maxPlayers != newConfig.server.maxPlayers) {
                changes.add("server.maxPlayers: " + current.server.maxPlayers + " → " + newConfig.server.maxPlayers);
            }
        } // End skipNetworkFields check

        // Skills
        if (current.skills.gainRate != newConfig.skills.gainRate) {
            changes.add("skills.gainRate: " + current.skills.gainRate + " → " + newConfig.skills.gainRate);
        }
        if (current.skills.starting.basic != newConfig.skills.starting.basic) {
            changes.add("skills.starting.basic: " + current.skills.starting.basic + " → " + newConfig.skills.starting.basic);
        }
        if (current.skills.starting.mindLogic != newConfig.skills.starting.mindLogic) {
            changes.add("skills.starting.mindLogic: " + current.skills.starting.mindLogic + " → " + newConfig.skills.starting.mindLogic);
        }
        if (current.skills.starting.fighting != newConfig.skills.starting.fighting) {
            changes.add("skills.starting.fighting: " + current.skills.starting.fighting + " → " + newConfig.skills.starting.fighting);
        }
        if (current.skills.starting.bodyControl != newConfig.skills.starting.bodyControl) {
            changes.add("skills.starting.bodyControl: " + current.skills.starting.bodyControl + " → " + newConfig.skills.starting.bodyControl);
        }
        if (current.skills.starting.overall != newConfig.skills.starting.overall) {
            changes.add("skills.starting.overall: " + current.skills.starting.overall + " → " + newConfig.skills.starting.overall);
        }

        // Combat
        if (current.combat.actionSpeed != newConfig.combat.actionSpeed) {
            changes.add("combat.actionSpeed: " + current.combat.actionSpeed + " → " + newConfig.combat.actionSpeed);
        }
        if (current.combat.ratingModifier != newConfig.combat.ratingModifier) {
            changes.add("combat.ratingModifier: " + current.combat.ratingModifier + " → " + newConfig.combat.ratingModifier);
        }
        if (current.combat.hotaDelay != newConfig.combat.hotaDelay) {
            changes.add("combat.hotaDelay: " + current.combat.hotaDelay + " → " + newConfig.combat.hotaDelay);
        }

        // Creatures
        if (current.creatures.maxTotal != newConfig.creatures.maxTotal) {
            changes.add("creatures.maxTotal: " + current.creatures.maxTotal + " → " + newConfig.creatures.maxTotal);
        }
        if (current.creatures.percentAggressive != newConfig.creatures.percentAggressive) {
            changes.add("creatures.percentAggressive: " + current.creatures.percentAggressive + " → " + newConfig.creatures.percentAggressive);
        }
        if (current.creatures.breedingTimer != newConfig.creatures.breedingTimer) {
            changes.add("creatures.breedingTimer: " + current.creatures.breedingTimer + " → " + newConfig.creatures.breedingTimer);
        }

        // World
        if (current.world.treeGrowth != newConfig.world.treeGrowth) {
            changes.add("world.treeGrowth: " + current.world.treeGrowth + " → " + newConfig.world.treeGrowth);
        }
        if (current.world.fieldGrowthTime != newConfig.world.fieldGrowthTime) {
            changes.add("world.fieldGrowthTime: " + current.world.fieldGrowthTime + " → " + newConfig.world.fieldGrowthTime);
        }
        if (current.world.tunnelingHits != newConfig.world.tunnelingHits) {
            changes.add("world.tunnelingHits: " + current.world.tunnelingHits + " → " + newConfig.world.tunnelingHits);
        }

        // Economy
        if (current.economy.upkeepEnabled != newConfig.economy.upkeepEnabled) {
            changes.add("economy.upkeepEnabled: " + current.economy.upkeepEnabled + " → " + newConfig.economy.upkeepEnabled);
        }
        if (current.economy.maxDeedSize != newConfig.economy.maxDeedSize) {
            changes.add("economy.maxDeedSize: " + current.economy.maxDeedSize + " → " + newConfig.economy.maxDeedSize);
        }
        if (current.economy.freeDeeds != newConfig.economy.freeDeeds) {
            changes.add("economy.freeDeeds: " + current.economy.freeDeeds + " → " + newConfig.economy.freeDeeds);
        }
        if (current.economy.traders.maxMoney != newConfig.economy.traders.maxMoney) {
            changes.add("economy.traders.maxMoney: " + current.economy.traders.maxMoney + " → " + newConfig.economy.traders.maxMoney);
        }
        if (current.economy.traders.startingMoney != newConfig.economy.traders.startingMoney) {
            changes.add("economy.traders.startingMoney: " + current.economy.traders.startingMoney + " → " + newConfig.economy.traders.startingMoney);
        }
        if (current.economy.kingdomStartingMoney != newConfig.economy.kingdomStartingMoney) {
            changes.add("economy.kingdomStartingMoney: " + current.economy.kingdomStartingMoney + " → " + newConfig.economy.kingdomStartingMoney);
        }

        // Players
        if (current.players.maxPlayers != newConfig.players.maxPlayers) {
            changes.add("players.maxPlayers: " + current.players.maxPlayers + " → " + newConfig.players.maxPlayers);
        }

        // Spawn points
        if (current.spawns.jennKellonX != newConfig.spawns.jennKellonX) {
            changes.add("spawns.jennKellonX: " + current.spawns.jennKellonX + " → " + newConfig.spawns.jennKellonX);
        }
        if (current.spawns.jennKellonY != newConfig.spawns.jennKellonY) {
            changes.add("spawns.jennKellonY: " + current.spawns.jennKellonY + " → " + newConfig.spawns.jennKellonY);
        }
        if (current.spawns.molRehanX != newConfig.spawns.molRehanX) {
            changes.add("spawns.molRehanX: " + current.spawns.molRehanX + " → " + newConfig.spawns.molRehanX);
        }
        if (current.spawns.molRehanY != newConfig.spawns.molRehanY) {
            changes.add("spawns.molRehanY: " + current.spawns.molRehanY + " → " + newConfig.spawns.molRehanY);
        }
        if (current.spawns.hotsX != newConfig.spawns.hotsX) {
            changes.add("spawns.hotsX: " + current.spawns.hotsX + " → " + newConfig.spawns.hotsX);
        }
        if (current.spawns.hotsY != newConfig.spawns.hotsY) {
            changes.add("spawns.hotsY: " + current.spawns.hotsY + " → " + newConfig.spawns.hotsY);
        }

        // Server Properties (SERVERPROPERTIES table)
        if (current.properties.multiKingdom != newConfig.properties.multiKingdom) {
            changes.add("properties.multiKingdom: " + current.properties.multiKingdom + " → " + newConfig.properties.multiKingdom);
        }
        if (current.properties.epic != newConfig.properties.epic) {
            changes.add("properties.epic: " + current.properties.epic + " → " + newConfig.properties.epic);
        }
        if (current.properties.allowChaos != newConfig.properties.allowChaos) {
            changes.add("properties.allowChaos: " + current.properties.allowChaos + " → " + newConfig.properties.allowChaos);
        }
        if (current.properties.newbieFriendly != newConfig.properties.newbieFriendly) {
            changes.add("properties.newbieFriendly: " + current.properties.newbieFriendly + " → " + newConfig.properties.newbieFriendly);
        }
        if (current.properties.spyPrevention != newConfig.properties.spyPrevention) {
            changes.add("properties.spyPrevention: " + current.properties.spyPrevention + " → " + newConfig.properties.spyPrevention);
        }
        if (current.properties.npcs != newConfig.properties.npcs) {
            changes.add("properties.npcs: " + current.properties.npcs + " → " + newConfig.properties.npcs);
        }
        if (current.properties.endGameItems != newConfig.properties.endGameItems) {
            changes.add("properties.endGameItems: " + current.properties.endGameItems + " → " + newConfig.properties.endGameItems);
        }
        if (current.properties.autoNetworking != newConfig.properties.autoNetworking) {
            changes.add("properties.autoNetworking: " + current.properties.autoNetworking + " → " + newConfig.properties.autoNetworking);
        }
        if (current.properties.enablePnpPortForward != newConfig.properties.enablePnpPortForward) {
            changes.add("properties.enablePnpPortForward: " + current.properties.enablePnpPortForward + " → " + newConfig.properties.enablePnpPortForward);
        }
        if (current.properties.steamQueryPort != newConfig.properties.steamQueryPort) {
            changes.add("properties.steamQueryPort: " + current.properties.steamQueryPort + " → " + newConfig.properties.steamQueryPort);
        }
        if (!equals(current.properties.adminPassword, newConfig.properties.adminPassword)) {
            changes.add("properties.adminPassword: [hidden] → [hidden]");
        }
    }

    /**
     * Update database with new config values.
     *
     * @param conn Database connection
     * @param config New config values to apply
     * @param currentConfig Current DB values (already read by caller; used to preserve network fields when {@code skipNetworkFields} is true). Must be non-null if {@code skipNetworkFields} is true.
     * @param serverId Server ID
     * @param skipNetworkFields If true, preserves current database network values (when AUTO_NETWORKING is enabled)
     */
    private static void updateDatabase(Connection conn, ServerConfig config, ServerConfig currentConfig, int serverId, boolean skipNetworkFields) throws SQLException {
        // Reuse the currentConfig the caller already read — reading again on the
        // same conn fails under the Postgres dialect-rewriter, which returns
        // connections to the pool after a statement is closed.
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(UPDATE_SERVER);

            int idx = 1;

            // Server identity
            ps.setString(idx++, config.server.name);
            ps.setString(idx++, config.server.motd);
            ps.setString(idx++, config.server.steamPassword);
            ps.setByte(idx++, config.server.kingdom);

            ps.setBoolean(idx++, config.server.pvp);
            ps.setBoolean(idx++, config.server.epic);
            ps.setBoolean(idx++, config.server.challenge);
            ps.setBoolean(idx++, config.server.homeServer);
            ps.setBoolean(idx++, config.server.randomSpawns);

            ps.setBoolean(idx++, config.server.entryServer);
            ps.setBoolean(idx++, config.server.loginServer);
            ps.setBoolean(idx++, config.server.isTest);
            ps.setBoolean(idx++, config.server.local);
            ps.setString(idx++, config.server.mapName);
            ps.setByte(idx++, config.server.caHelpGroup);

            // Network settings (preserve current values if auto-networking is enabled)
            if (skipNetworkFields && currentConfig != null) {
                // Use current database values (don't overwrite auto-detected values)
                ps.setString(idx++, currentConfig.server.externalIp);
                ps.setString(idx++, currentConfig.server.externalPort);
                ps.setString(idx++, currentConfig.server.internalIp);
                ps.setString(idx++, currentConfig.server.internalPort);
                ps.setString(idx++, currentConfig.server.rmiPort);
                ps.setString(idx++, currentConfig.server.rmiRegPort);
                ps.setString(idx++, currentConfig.server.intraServerPassword);
                ps.setInt(idx++, currentConfig.server.maxPlayers);
            } else {
                // Use YAML config values
                ps.setString(idx++, config.server.externalIp);
                ps.setString(idx++, config.server.externalPort);
                ps.setString(idx++, config.server.internalIp);
                ps.setString(idx++, config.server.internalPort);
                ps.setString(idx++, config.server.rmiPort);
                ps.setString(idx++, config.server.rmiRegPort);
                ps.setString(idx++, config.server.intraServerPassword);
                ps.setInt(idx++, config.server.maxPlayers);
            }

            // Skills
            ps.setFloat(idx++, config.skills.gainRate);
            ps.setFloat(idx++, config.skills.starting.basic);
            ps.setFloat(idx++, config.skills.starting.mindLogic);
            ps.setFloat(idx++, config.skills.starting.fighting);
            ps.setFloat(idx++, config.skills.starting.bodyControl);
            ps.setFloat(idx++, config.skills.starting.overall);

            // Combat
            ps.setFloat(idx++, config.combat.actionSpeed);
            ps.setFloat(idx++, config.combat.ratingModifier);
            ps.setInt(idx++, config.combat.hotaDelay);

            // Creatures
            ps.setInt(idx++, config.creatures.maxTotal);
            ps.setFloat(idx++, config.creatures.percentAggressive);
            ps.setLong(idx++, config.creatures.breedingTimer);

            // World
            ps.setInt(idx++, config.world.treeGrowth);
            ps.setLong(idx++, config.world.fieldGrowthTime);
            ps.setInt(idx++, config.world.tunnelingHits);

            // Economy
            ps.setBoolean(idx++, config.economy.upkeepEnabled);
            ps.setInt(idx++, config.economy.maxDeedSize);
            ps.setBoolean(idx++, config.economy.freeDeeds);
            ps.setInt(idx++, config.economy.traders.maxMoney);
            ps.setInt(idx++, config.economy.traders.startingMoney);
            ps.setInt(idx++, config.economy.kingdomStartingMoney);

            // Spawn points
            ps.setInt(idx++, config.spawns.jennKellonX);
            ps.setInt(idx++, config.spawns.jennKellonY);
            ps.setInt(idx++, config.spawns.molRehanX);
            ps.setInt(idx++, config.spawns.molRehanY);
            ps.setInt(idx++, config.spawns.hotsX);
            ps.setInt(idx++, config.spawns.hotsY);

            // WHERE clause
            ps.setInt(idx++, serverId);

            ps.executeUpdate();

        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Sync server properties to SERVERPROPERTIES table.
     *
     * <p>Uses INSERT OR REPLACE to update key-value pairs in SERVERPROPERTIES table.</p>
     */
    private static void syncServerProperties(Connection conn, ServerConfig config) throws SQLException {
        PreparedStatement ps = null;

        try {
            // SQLite-native form; SqlDialectRewriter translates to
            // ON CONFLICT (PROPKEY) DO UPDATE ... on Postgres, REPLACE INTO
            // on MySQL. SQLite passes through.
            String upsert = "INSERT OR REPLACE INTO SERVERPROPERTIES (PROPKEY, PROPVAL) VALUES (?, ?)";
            ps = conn.prepareStatement(upsert);

            // Sync all properties
            setProperty(ps, "MULTI_KINGDOM", String.valueOf(config.properties.multiKingdom));
            setProperty(ps, "EPIC", String.valueOf(config.properties.epic));
            setProperty(ps, "ALLOWCHAOS", String.valueOf(config.properties.allowChaos));
            setProperty(ps, "NEWBIEFRIENDLY", String.valueOf(config.properties.newbieFriendly));
            setProperty(ps, "SPYPREVENTION", String.valueOf(config.properties.spyPrevention));
            setProperty(ps, "NPCS", String.valueOf(config.properties.npcs));
            setProperty(ps, "ENDGAMEITEMS", String.valueOf(config.properties.endGameItems));
            setProperty(ps, "AUTO_NETWORKING", String.valueOf(config.properties.autoNetworking));
            setProperty(ps, "ENABLE_PNP_PORT_FORWARD", String.valueOf(config.properties.enablePnpPortForward));
            setProperty(ps, "STEAMQUERYPORT", String.valueOf(config.properties.steamQueryPort));
            setProperty(ps, "ADMINPASSWORD", config.properties.adminPassword);
            setProperty(ps, "SERVERPASSWORD", config.server.serverPassword);
            setProperty(ps, "HOMESERVER_KINGDOM", String.valueOf(config.server.homeServerKingdom));

        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Helper to set a property in SERVERPROPERTIES table.
     */
    private static void setProperty(PreparedStatement ps, String key, String value) throws SQLException {
        ps.setString(1, key);
        ps.setString(2, value);
        ps.executeUpdate();
    }

    /**
     * Apply config directly to server's in-memory structures using reflection.
     * This bypasses the database entirely and modifies the ServerEntry that was already loaded.
     *
     * @param config The config to apply
     * @param serverId The server ID
     * @throws Exception if reflection fails
     */
    public static void applyConfigToServerMemory(ServerConfig config, int serverId) throws Exception {
        // Get the Servers class
        Class<?> serversClass = Class.forName("com.wurmonline.server.Servers");

        // Get the localServer static field (ServerEntry instance)
        java.lang.reflect.Field localServerField = serversClass.getField("localServer");
        Object localServer = localServerField.get(null);

        if (localServer == null) {
            throw new IllegalStateException("Servers.localServer is null - server not initialized yet");
        }

        Class<?> serverEntryClass = localServer.getClass();

        // Apply creature settings
        setFieldValue(serverEntryClass, localServer, "maxCreatures", config.creatures.maxTotal);
        setFieldValue(serverEntryClass, localServer, "MAXCREATURES", config.creatures.maxTotal);

        // Apply skill settings
        setFieldValue(serverEntryClass, localServer, "SKILLGAINRATE", config.skills.gainRate);

        // Apply combat settings
        setFieldValue(serverEntryClass, localServer, "ACTIONTIMER", config.combat.actionSpeed);
        setFieldValue(serverEntryClass, localServer, "CRMOD", config.combat.ratingModifier);

        logger.info("[ServerConfigSync] Applied config directly to Servers.localServer via reflection");
        logger.info("[ServerConfigSync]   - maxCreatures: " + config.creatures.maxTotal);
        logger.info("[ServerConfigSync]   - skillGainRate: " + config.skills.gainRate);
        logger.info("[ServerConfigSync]   - actionTimer: " + config.combat.actionSpeed);
    }

    /**
     * Set a field value using reflection, ignoring if field doesn't exist.
     */
    private static void setFieldValue(Class<?> clazz, Object instance, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = clazz.getField(fieldName);
            field.set(instance, value);
        } catch (NoSuchFieldException e) {
            // Field doesn't exist - that's okay, might be different Wurm version
            logger.fine("[ServerConfigSync] Field not found: " + fieldName + " (might not exist in this Wurm version)");
        } catch (Exception e) {
            logger.warning("[ServerConfigSync] Failed to set field " + fieldName + ": " + e.getMessage());
        }
    }

    /**
     * Null-safe string equality.
     */
    /**
     * Check if AUTO_NETWORKING is enabled in SERVERPROPERTIES.
     * When enabled, Wurm auto-detects external IP and we should not override it.
     */
    private static boolean isAutoNetworkingEnabled(Connection conn) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement("SELECT PROPVAL FROM SERVERPROPERTIES WHERE PROPKEY='AUTO_NETWORKING'");
            rs = ps.executeQuery();
            if (rs.next()) {
                String value = rs.getString("PROPVAL");
                return "true".equalsIgnoreCase(value) || "1".equals(value);
            }
            return false; // Default to false if not found
        } catch (SQLException e) {
            logger.warning("[ServerConfigSync] Failed to check AUTO_NETWORKING status: " + e.getMessage());
            return false; // Assume disabled on error
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        }
    }

    private static boolean equals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}

package com.garward.wurmmodloader.config;

import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;

import java.sql.Connection;
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
        "SKILLGAINRATE=?, SKILLBASICSTART=?, SKILLMINDLOGICSTART=?, SKILLFIGHTINGSTART=?, " +
        "SKILLBODYCONTROLSTART=?, SKILLOVERALLSTART=?, " +
        "ACTIONTIMER=?, CRMOD=?, HOTADELAY=?, " +
        "MAXCREATURES=?, PERCENT_AGG_CREATURES=?, BREEDING=?, " +
        "TREEGROWTH=?, FIELDGROWTH=?, TUNNELING=?, " +
        "UPKEEP=?, MAXDEED=?, FREEDEEDS=?, TRADERMAX=?, TRADERINIT=?, KINGSMONEY=?, " +
        "MAXPLAYERS=?, " +
        "SPAWNPOINTJENNX=?, SPAWNPOINTJENNY=?, SPAWNPOINTMOLX=?, SPAWNPOINTMOLY=?, SPAWNPOINTLIBX=?, SPAWNPOINTLIBY=? " +
        "WHERE SERVER=?";

    private static final String GET_SERVER = "SELECT * FROM SERVERS WHERE SERVER=?";

    /**
     * Sync config values to database.
     *
     * @param config The config to sync
     * @param serverId The server ID
     * @throws SQLException if database update fails
     */
    public static void syncToDatabase(ServerConfig config, int serverId) throws SQLException {
        logger.info("[ServerConfigSync] Syncing config to database for serverId=" + serverId);

        List<String> changes = new ArrayList<>();

        Connection conn = null;
        try {
            conn = DatabaseConnectionUtil.getLoginDbConnection();

            // Read current database values
            ServerConfig currentConfig = readCurrentValues(conn, serverId);

            // Compare and track changes
            detectChanges(currentConfig, config, changes);

            if (changes.isEmpty()) {
                logger.info("[ServerConfigSync] No changes detected, database is up to date");
                return;
            }

            // Apply changes
            logger.info("[ServerConfigSync] Applying " + changes.size() + " changes to database:");
            for (String change : changes) {
                logger.info("  - " + change);
            }

            updateDatabase(conn, config, serverId);

            logger.info("[ServerConfigSync] Database sync completed successfully");

        } finally {
            DatabaseConnectionUtil.closeConnection(conn);
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

            return ServerConfigGenerator.generateFromDatabase(serverId);

        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Detect changes between current and new config.
     */
    private static void detectChanges(ServerConfig current, ServerConfig newConfig, List<String> changes) {
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
    }

    /**
     * Update database with new config values.
     */
    private static void updateDatabase(Connection conn, ServerConfig config, int serverId) throws SQLException {
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

            // Players
            ps.setInt(idx++, config.players.maxPlayers);

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
     * Null-safe string equality.
     */
    private static boolean equals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}

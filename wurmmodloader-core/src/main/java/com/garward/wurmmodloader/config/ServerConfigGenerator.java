package com.garward.wurmmodloader.config;

import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Generates ServerConfig from current database values.
 *
 * <p>Used for first-time setup to export existing server settings into a config file.
 * This allows existing server owners to drop in the modloader and get their complete
 * settings automatically exported.</p>
 *
 * <p><strong>Classloader Isolation:</strong> Uses DatabaseConnectionUtil for world-agnostic
 * database access. Does NOT depend on Wurm static fields.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ServerConfigGenerator {

    private static final Logger logger = Logger.getLogger(ServerConfigGenerator.class.getName());

    private static final String GET_SERVER_SETTINGS =
        "SELECT * FROM SERVERS WHERE SERVER=?";

    /**
     * Generate ServerConfig from current database values.
     *
     * @param serverId Server ID
     * @return ServerConfig populated with database values
     * @throws SQLException if database query fails
     */
    public static ServerConfig generateFromDatabase(int serverId) throws SQLException {
        logger.info("[ServerConfigGenerator] Reading server settings from database for serverId=" + serverId);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnectionUtil.getLoginDbConnection();
            ps = conn.prepareStatement(GET_SERVER_SETTINGS);
            ps.setInt(1, serverId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Server ID " + serverId + " not found in SERVERS table");
            }

            return generateFromResultSet(rs);

        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Generate ServerConfig from an existing ResultSet.
     *
     * <p>This method does NOT close the ResultSet or its connection.
     * Useful when you already have a database connection open and want
     * to avoid opening a second connection.</p>
     *
     * @param rs ResultSet positioned at the server row
     * @return ServerConfig populated with ResultSet values
     * @throws SQLException if reading fails
     */
    public static ServerConfig generateFromResultSet(ResultSet rs) throws SQLException {
        ServerConfig config = new ServerConfig();

        // Read server identity
        readServerIdentity(rs, config.server);

        // Read skill settings
        readSkills(rs, config.skills);

        // Read combat settings
        readCombat(rs, config.combat);

        // Read creature settings
        readCreatures(rs, config.creatures);

        // Read world settings
        readWorld(rs, config.world);

        // Read economy settings
        readEconomy(rs, config.economy);

        // Read player settings
        readPlayers(rs, config.players);

        // Read spawn points
        readSpawnPoints(rs, config.spawns);

        // Read server properties (SERVERPROPERTIES table)
        readServerProperties(config);

        logger.info("[ServerConfigGenerator] Successfully read server settings from ResultSet");
        return config;
    }

    private static void readServerIdentity(ResultSet rs, ServerConfig.ServerIdentityConfig server) throws SQLException {
        server.name = getStringOrDefault(rs, "NAME", "Wurm Unlimited Server");
        server.motd = getStringOrDefault(rs, "MOTD", "");
        server.steamPassword = getStringOrDefault(rs, "STEAMPW", "");
        server.kingdom = getByteOrDefault(rs, "KINGDOM", (byte) 4);

        server.pvp = getBooleanOrDefault(rs, "PVP", false);
        server.epic = getBooleanOrDefault(rs, "EPIC", false);
        server.challenge = getBooleanOrDefault(rs, "CHALLENGE", false);
        server.homeServer = getBooleanOrDefault(rs, "HOMESERVER", true);
        server.randomSpawns = getBooleanOrDefault(rs, "RANDOMSPAWNS", false);

        // Additional server flags
        server.entryServer = getBooleanOrDefault(rs, "ENTRYSERVER", false);
        server.loginServer = getBooleanOrDefault(rs, "LOGINSERVER", false);
        server.isTest = getBooleanOrDefault(rs, "ISTEST", false);
        server.local = getBooleanOrDefault(rs, "LOCAL", false);
        server.mapName = getStringOrDefault(rs, "MAPNAME", "");
        server.caHelpGroup = getByteOrDefault(rs, "CAHELPGROUP", (byte) -1);

        // Network configuration
        server.externalIp = getStringOrDefault(rs, "EXTERNALIP", "127.0.1.1");
        server.externalPort = getStringOrDefault(rs, "EXTERNALPORT", "3724");
        server.internalIp = getStringOrDefault(rs, "INTRASERVERADDRESS", "127.0.0.1");
        server.internalPort = getStringOrDefault(rs, "INTRASERVERPORT", "48020");
        server.rmiPort = getStringOrDefault(rs, "RMIPORT", "7220");
        server.rmiRegPort = getStringOrDefault(rs, "REGISTRATIONPORT", "7221");
        server.intraServerPassword = getStringOrDefault(rs, "INTRASERVERPASSWORD", "");
        server.maxPlayers = getIntOrDefault(rs, "MAXPLAYERS", 200);
    }

    private static void readSkills(ResultSet rs, ServerConfig.SkillsConfig skills) throws SQLException {
        skills.gainRate = getFloatOrDefault(rs, "SKILLGAINRATE", 1.0f);

        skills.starting.basic = getFloatOrDefault(rs, "SKILLBASICSTART", 20.0f);
        skills.starting.mindLogic = getFloatOrDefault(rs, "SKILLMINDLOGICSTART", 20.0f);
        skills.starting.fighting = getFloatOrDefault(rs, "SKILLFIGHTINGSTART", 1.0f);
        skills.starting.bodyControl = getFloatOrDefault(rs, "SKILLBODYCONTROLSTART", 20.0f);
        skills.starting.overall = getFloatOrDefault(rs, "SKILLOVERALLSTART", 1.0f);
    }

    private static void readCombat(ResultSet rs, ServerConfig.CombatConfig combat) throws SQLException {
        combat.actionSpeed = getFloatOrDefault(rs, "ACTIONTIMER", 1.0f);
        combat.ratingModifier = getFloatOrDefault(rs, "CRMOD", 1.0f);
        combat.hotaDelay = getIntOrDefault(rs, "HOTADELAY", 2160);
    }

    private static void readCreatures(ResultSet rs, ServerConfig.CreaturesConfig creatures) throws SQLException {
        creatures.maxTotal = getIntOrDefault(rs, "MAXCREATURES", 1000);
        creatures.percentAggressive = getFloatOrDefault(rs, "PERCENT_AGG_CREATURES", 10.0f);
        creatures.breedingTimer = getLongOrDefault(rs, "BREEDING", 0L);
    }

    private static void readWorld(ResultSet rs, ServerConfig.WorldConfig world) throws SQLException {
        world.treeGrowth = getIntOrDefault(rs, "TREEGROWTH", 20);
        world.fieldGrowthTime = getLongOrDefault(rs, "FIELDGROWTH", 86400000L);
        world.tunnelingHits = getIntOrDefault(rs, "TUNNELING", 51);
    }

    private static void readEconomy(ResultSet rs, ServerConfig.EconomyConfig economy) throws SQLException {
        economy.upkeepEnabled = getBooleanOrDefault(rs, "UPKEEP", true);
        economy.maxDeedSize = getIntOrDefault(rs, "MAXDEED", 0);
        economy.freeDeeds = getBooleanOrDefault(rs, "FREEDEEDS", false);
        economy.traders.maxMoney = getIntOrDefault(rs, "TRADERMAX", 500000);
        economy.traders.startingMoney = getIntOrDefault(rs, "TRADERINIT", 10000);
        economy.kingdomStartingMoney = getIntOrDefault(rs, "KINGSMONEY", 0);
    }

    private static void readPlayers(ResultSet rs, ServerConfig.PlayersConfig players) throws SQLException {
        // maxPlayers moved to ServerIdentityConfig
        // limitOverridable not stored in database, keep default
    }

    private static void readSpawnPoints(ResultSet rs, ServerConfig.SpawnConfig spawn) throws SQLException {
        spawn.jennKellonX = getIntOrDefault(rs, "SPAWNPOINTJENNX", 0);
        spawn.jennKellonY = getIntOrDefault(rs, "SPAWNPOINTJENNY", 0);
        spawn.molRehanX = getIntOrDefault(rs, "SPAWNPOINTMOLX", 0);
        spawn.molRehanY = getIntOrDefault(rs, "SPAWNPOINTMOLY", 0);
        spawn.hotsX = getIntOrDefault(rs, "SPAWNPOINTLIBX", 0);
        spawn.hotsY = getIntOrDefault(rs, "SPAWNPOINTLIBY", 0);
    }

    private static void readServerProperties(ServerConfig config) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnectionUtil.getLoginDbConnection();
            ps = conn.prepareStatement("SELECT PROPKEY, PROPVAL FROM SERVERPROPERTIES");
            rs = ps.executeQuery();

            // Read all properties into a map first
            java.util.Map<String, String> propMap = new java.util.HashMap<>();
            while (rs.next()) {
                String key = rs.getString("PROPKEY");
                String value = rs.getString("PROPVAL");
                if (key != null && value != null) {
                    propMap.put(key, value);
                }
            }

            // Map properties to config fields
            config.properties.multiKingdom = getBooleanFromMap(propMap, "MULTI_KINGDOM", false);
            config.properties.epic = getBooleanFromMap(propMap, "EPIC", false);
            config.properties.allowChaos = getBooleanFromMap(propMap, "ALLOWCHAOS", false);
            config.properties.newbieFriendly = getBooleanFromMap(propMap, "NEWBIEFRIENDLY", true);
            config.properties.spyPrevention = getBooleanFromMap(propMap, "SPYPREVENTION", false);
            config.properties.npcs = getBooleanFromMap(propMap, "NPCS", true);
            config.properties.endGameItems = getBooleanFromMap(propMap, "ENDGAMEITEMS", true);
            config.properties.autoNetworking = getBooleanFromMap(propMap, "AUTO_NETWORKING", true);
            config.properties.enablePnpPortForward = getBooleanFromMap(propMap, "ENABLE_PNP_PORT_FORWARD", true);
            config.properties.steamQueryPort = getIntFromMap(propMap, "STEAMQUERYPORT", 27016);
            config.properties.adminPassword = propMap.getOrDefault("ADMINPASSWORD", "");

            // Read serverPassword and homeServerKingdom from SERVERPROPERTIES
            // Note: These are synced to both SERVERS table and SERVERPROPERTIES for persistence
            config.server.serverPassword = propMap.getOrDefault("SERVERPASSWORD", "");
            String kingdomStr = propMap.get("HOMESERVER_KINGDOM");
            if (kingdomStr != null) {
                try {
                    config.server.homeServerKingdom = Byte.parseByte(kingdomStr);
                } catch (NumberFormatException e) {
                    config.server.homeServerKingdom = 1; // Default to JK
                }
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    // Safe getter methods that return defaults if column doesn't exist

    private static String getStringOrDefault(ResultSet rs, String column, String defaultValue) {
        try {
            String value = rs.getString(column);
            return (value != null) ? value : defaultValue;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private static int getIntOrDefault(ResultSet rs, String column, int defaultValue) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? defaultValue : value;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private static long getLongOrDefault(ResultSet rs, String column, long defaultValue) {
        try {
            long value = rs.getLong(column);
            return rs.wasNull() ? defaultValue : value;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private static float getFloatOrDefault(ResultSet rs, String column, float defaultValue) {
        try {
            float value = rs.getFloat(column);
            return rs.wasNull() ? defaultValue : value;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private static boolean getBooleanOrDefault(ResultSet rs, String column, boolean defaultValue) {
        try {
            return rs.getBoolean(column);
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private static byte getByteOrDefault(ResultSet rs, String column, byte defaultValue) {
        try {
            byte value = rs.getByte(column);
            return rs.wasNull() ? defaultValue : value;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    /**
     * Generate YAML string with comments and formatting.
     *
     * @param config The config to convert to YAML
     * @return YAML string with inline comments
     */
    public static String generateYamlWithComments(ServerConfig config) {
        StringBuilder yaml = new StringBuilder();

        yaml.append("# Wurm Unlimited Server Configuration\n");
        yaml.append("# Generated from current database values\n");
        yaml.append("# Edit this file to customize your server settings\n");
        yaml.append("# Changes will sync to the database on next server startup\n");
        yaml.append("#\n");
        yaml.append("# IMPORTANT: After editing, restart the server to apply changes\n");
        yaml.append("\n");

        // Add comprehensive GM command reference
        yaml.append("# ===== WURM UNLIMITED GM COMMANDS REFERENCE =====\n");
        yaml.append("# All GM commands start with # and require appropriate power level (1-5, where 5 is highest)\n");
        yaml.append("# These are IN-GAME commands typed in chat, NOT console commands\n");
        yaml.append("# Console only has: shutdown\n");
        yaml.append("#\n");
        yaml.append("# --- PLAYER MANAGEMENT ---\n");
        yaml.append("# #kick <player> - Kick player from server\n");
        yaml.append("# #ban <player> <reason> - Ban player permanently\n");
        yaml.append("# #unban <player> - Remove player ban\n");
        yaml.append("# #mute <player> - Mute player in chat\n");
        yaml.append("# #unmute <player> - Unmute player\n");
        yaml.append("# #changeemail <player> <email> - Change player email\n");
        yaml.append("# #changepassword <player> <password> - Change player password\n");
        yaml.append("# #setrep <player> <amount> - Set player reputation\n");
        yaml.append("# #setpower <player> <level> - Set GM power level (0-5)\n");
        yaml.append("#\n");
        yaml.append("# --- TELEPORTATION ---\n");
        yaml.append("# #teleport <x> <y> - Teleport to coordinates\n");
        yaml.append("# #summon <player> - Summon player to you\n");
        yaml.append("# #goto <player> - Teleport to player\n");
        yaml.append("# #send <player> <x> <y> - Send player to coordinates\n");
        yaml.append("#\n");
        yaml.append("# --- ITEMS & INVENTORY ---\n");
        yaml.append("# #getinv <player> - View player inventory\n");
        yaml.append("# #giveskill <player> <skill> <amount> - Give skill points\n");
        yaml.append("# #createitem <templateid> [ql] [material] - Create item by template ID\n");
        yaml.append("# #createcoin <type> <amount> - Create coins (iron/copper/silver/gold)\n");
        yaml.append("# #gmtool - Give yourself the GM wand\n");
        yaml.append("# #destroy - Destroy targeted item\n");
        yaml.append("# #setql <quality> - Set item quality\n");
        yaml.append("# #setrarity <0-3> - Set item rarity (0=normal, 1=rare, 2=supreme, 3=fantastic)\n");
        yaml.append("# #rename <newname> - Rename targeted item\n");
        yaml.append("# #setdamage <damage> - Set item damage\n");
        yaml.append("#\n");
        yaml.append("# --- CREATURES & COMBAT ---\n");
        yaml.append("# #spawnCreature <name> - Spawn creature by name\n");
        yaml.append("# #age <creature> <age> - Set creature age\n");
        yaml.append("# #disease <creature> - Disease creature\n");
        yaml.append("# #kill - Kill targeted creature\n");
        yaml.append("# #hell <on/off> - Toggle hell horses spawning\n");
        yaml.append("# #setcr <value> - Set creature combat rating\n");
        yaml.append("# #tame - Tame targeted creature\n");
        yaml.append("# #untame - Untame creature\n");
        yaml.append("# #setloyalty <value> - Set pet loyalty\n");
        yaml.append("#\n");
        yaml.append("# --- TERRAIN & WORLD ---\n");
        yaml.append("# #setcave - Set tile to cave\n");
        yaml.append("# #flatten <radius> - Flatten terrain around you\n");
        yaml.append("# #dirt <amount> - Raise/lower terrain height\n");
        yaml.append("# #settile <tileid> - Change tile type by ID\n");
        yaml.append("# #level <radius> - Level terrain in radius\n");
        yaml.append("# #fix - Fix issues with current tile\n");
        yaml.append("# #addtile <tileid> - Add tile resource\n");
        yaml.append("#\n");
        yaml.append("# --- STRUCTURES & BUILDINGS ---\n");
        yaml.append("# #finishbuilding - Complete building instantly\n");
        yaml.append("# #buildwall <direction> - Build wall in direction\n");
        yaml.append("# #destroyfence - Destroy targeted fence\n");
        yaml.append("# #setdecay <value> - Set item decay rate\n");
        yaml.append("#\n");
        yaml.append("# --- DEEDS & SETTLEMENTS ---\n");
        yaml.append("# #setmayor <player> <settlement> - Set settlement mayor\n");
        yaml.append("# #expanddeed <tiles> - Expand deed by X tiles\n");
        yaml.append("# #destroysettlement - Destroy targeted settlement\n");
        yaml.append("#\n");
        yaml.append("# --- TIME & WEATHER ---\n");
        yaml.append("# #time <hours> - Advance time by hours\n");
        yaml.append("# #settime <wurmtime> - Set server time\n");
        yaml.append("# #weather <type> - Change weather (clear/rain/snow/fog)\n");
        yaml.append("# #season <spring/summer/autumn/winter> - Change season\n");
        yaml.append("#\n");
        yaml.append("# --- SERVER MANAGEMENT ---\n");
        yaml.append("# #startshutdown <minutes> <reason> - Schedule server shutdown\n");
        yaml.append("# #cancelshutdown - Cancel scheduled shutdown\n");
        yaml.append("# #setcreatures <amount> - Set max creatures on server\n");
        yaml.append("# #togglenpc - Toggle NPC spawning\n");
        yaml.append("# #gmlist - Show online GMs\n");
        yaml.append("# #serverinfo - Display server statistics\n");
        yaml.append("#\n");
        yaml.append("# --- DEBUGGING & INFO ---\n");
        yaml.append("# #examine - Examine targeted object (detailed info)\n");
        yaml.append("# #iteminfo - Show item template info\n");
        yaml.append("# #creatureinfo - Show creature stats\n");
        yaml.append("# #tileinfo - Show tile information\n");
        yaml.append("# #findplayer <name> - Locate player\n");
        yaml.append("# #finditem <itemid> - Locate item by ID\n");
        yaml.append("# #poll - Force update poll\n");
        yaml.append("# #togglefly - Toggle flying mode\n");
        yaml.append("# #toggleinvis - Toggle invisibility\n");
        yaml.append("# #toggleinvuln - Toggle invulnerability\n");
        yaml.append("#\n");
        yaml.append("# --- SKILLS & CHARACTERISTICS ---\n");
        yaml.append("# #allskills <amount> - Set all skills to amount\n");
        yaml.append("# #characteristics <amount> - Set all characteristics\n");
        yaml.append("# #setskill <skillname> <amount> - Set specific skill\n");
        yaml.append("# #setbodystat <stat> <value> - Set body stat (strength/stamina/etc)\n");
        yaml.append("# #setfavor <amount> - Set deity favor\n");
        yaml.append("# #setfaith <amount> - Set faith level\n");
        yaml.append("#\n");
        yaml.append("# --- SPECIAL ACTIONS ---\n");
        yaml.append("# #changekingdom <player> <kingdomid> - Change player kingdom\n");
        yaml.append("# #champion <player> - Make player a champion\n");
        yaml.append("# #unchampion <player> - Remove champion status\n");
        yaml.append("# #priest <player> <deity> - Make player priest of deity\n");
        yaml.append("# #unpriest <player> - Remove priest status\n");
        yaml.append("# #setaffi <amount> - Set village affinity\n");
        yaml.append("# #starve <player> - Starve player\n");
        yaml.append("# #heal <player> - Fully heal player\n");
        yaml.append("# #hurt <player> <damage> - Damage player\n");
        yaml.append("#\n");
        yaml.append("# NOTE: Some commands may require specific power levels or may not work\n");
        yaml.append("# on all server configurations. Always test on backup worlds first!\n");
        yaml.append("#\n");
        yaml.append("# ============================================================\n");
        yaml.append("\n");

        // Version
        yaml.append("# Config schema version (do not change)\n");
        yaml.append("version: ").append(config.version).append("\n");
        yaml.append("\n");

        // Server Identity
        yaml.append("# Server Identity\n");
        yaml.append("server:\n");
        yaml.append("  name: \"").append(escape(config.server.name)).append("\"\n");
        yaml.append("  motd: \"").append(escape(config.server.motd)).append("\"\n");
        yaml.append("  steamPassword: \"").append(escape(config.server.steamPassword)).append("\"\n");
        yaml.append("  kingdom: ").append(config.server.kingdom).append("  # 1=JK, 2=MR, 3=HOTS, 4=Freedom\n");
        yaml.append("\n");
        yaml.append("  # Server Type Flags\n");
        yaml.append("  pvp: ").append(config.server.pvp).append("\n");
        yaml.append("  epic: ").append(config.server.epic).append("\n");
        yaml.append("  challenge: ").append(config.server.challenge).append("\n");
        yaml.append("  homeServer: ").append(config.server.homeServer).append("\n");
        yaml.append("  randomSpawns: ").append(config.server.randomSpawns).append("\n");
        yaml.append("  entryServer: ").append(config.server.entryServer).append("  # New players can spawn here\n");
        yaml.append("  loginServer: ").append(config.server.loginServer).append("  # Handles authentication for server cluster\n");
        yaml.append("  isTest: ").append(config.server.isTest).append("  # Development/testing server\n");
        yaml.append("  local: ").append(config.server.local).append("  # Single-player or LAN only\n");
        yaml.append("  mapName: \"").append(escape(config.server.mapName)).append("\"  # Map name\n");
        yaml.append("  caHelpGroup: ").append(config.server.caHelpGroup).append("  # Community Assistant help group (-1 = none)\n");
        yaml.append("\n");

        // Network configuration
        yaml.append("  # Network Configuration\n");
        yaml.append("  externalIp: \"").append(escape(config.server.externalIp)).append("\"  # External IP address\n");
        yaml.append("  externalPort: \"").append(escape(config.server.externalPort)).append("\"  # External port\n");
        yaml.append("  internalIp: \"").append(escape(config.server.internalIp)).append("\"  # Internal/intra-server IP\n");
        yaml.append("  internalPort: \"").append(escape(config.server.internalPort)).append("\"  # Internal/intra-server port\n");
        yaml.append("  rmiPort: \"").append(escape(config.server.rmiPort)).append("\"  # RMI port\n");
        yaml.append("  rmiRegPort: \"").append(escape(config.server.rmiRegPort)).append("\"  # RMI registry port\n");
        yaml.append("  intraServerPassword: \"").append(escape(config.server.intraServerPassword)).append("\"  # Intra-server password\n");
        yaml.append("  maxPlayers: ").append(config.server.maxPlayers).append("  # Maximum concurrent players\n");
        yaml.append("\n");

        // Authentication
        yaml.append("  # Authentication\n");
        yaml.append("  serverPassword: \"").append(escape(config.server.serverPassword)).append("\"  # Player connection password (empty = no password)\n");
        yaml.append("  homeServerKingdom: ").append(config.server.homeServerKingdom).append("  # Home server kingdom (1=JK, 2=MR, 3=HOTS, 4=Freedom)\n");
        yaml.append("\n");

        // Skills
        yaml.append("# Skill & Progression Settings\n");
        yaml.append("skills:\n");
        yaml.append("  gainRate: ").append(config.skills.gainRate).append("  # Skill gain multiplier (1.0 = normal, 10.0 = 10x faster)\n");
        yaml.append("\n");
        yaml.append("  # Starting Skill Values\n");
        yaml.append("  starting:\n");
        yaml.append("    basic: ").append(config.skills.starting.basic).append("  # Digging, mining, smithing, etc.\n");
        yaml.append("    mindLogic: ").append(config.skills.starting.mindLogic).append("  # Mind speed, mind logic\n");
        yaml.append("    fighting: ").append(config.skills.starting.fighting).append("  # Fighting, various weapon skills\n");
        yaml.append("    bodyControl: ").append(config.skills.starting.bodyControl).append("  # Body control\n");
        yaml.append("    overall: ").append(config.skills.starting.overall).append("  # Overall skill level multiplier\n");
        yaml.append("\n");

        // Combat
        yaml.append("# Combat & Action Settings\n");
        yaml.append("combat:\n");
        yaml.append("  actionSpeed: ").append(config.combat.actionSpeed).append("  # Action timer multiplier (1.0 = normal, 0.5 = 2x faster)\n");
        yaml.append("  ratingModifier: ").append(config.combat.ratingModifier).append("  # Combat rating modifier\n");
        yaml.append("  hotaDelay: ").append(config.combat.hotaDelay).append("  # Hunt of the Ancients delay (in Wurm hours)\n");
        yaml.append("\n");

        // Creatures
        yaml.append("# Creature Settings\n");
        yaml.append("creatures:\n");
        yaml.append("  maxTotal: ").append(config.creatures.maxTotal).append("  # Maximum total creatures on server\n");
        yaml.append("  percentAggressive: ").append(config.creatures.percentAggressive).append("  # Percentage of aggressive creatures (0.0-100.0)\n");
        yaml.append("  breedingTimer: ").append(config.creatures.breedingTimer).append("  # Animal breeding timer (milliseconds, 0 = use default)\n");
        yaml.append("\n");

        // World
        yaml.append("# World Settings\n");
        yaml.append("world:\n");
        yaml.append("  treeGrowth: ").append(config.world.treeGrowth).append("  # Tree growth rate\n");
        yaml.append("  fieldGrowthTime: ").append(config.world.fieldGrowthTime).append("  # Crop growth time (milliseconds)\n");
        yaml.append("  tunnelingHits: ").append(config.world.tunnelingHits).append("  # Tunneling hits required to mine\n");
        yaml.append("\n");

        // Economy
        yaml.append("# Economy Settings\n");
        yaml.append("economy:\n");
        yaml.append("  upkeepEnabled: ").append(config.economy.upkeepEnabled).append("  # Village upkeep enabled\n");
        yaml.append("  maxDeedSize: ").append(config.economy.maxDeedSize).append("  # Max deed size (0 = unlimited, otherwise NxN tiles)\n");
        yaml.append("  freeDeeds: ").append(config.economy.freeDeeds).append("  # Free deeds (no cost to found)\n");
        yaml.append("\n");
        yaml.append("  # Trader Settings (in iron coins)\n");
        yaml.append("  traders:\n");
        yaml.append("    maxMoney: ").append(config.economy.traders.maxMoney).append("  # Trader max money\n");
        yaml.append("    startingMoney: ").append(config.economy.traders.startingMoney).append("  # Trader starting money\n");
        yaml.append("\n");
        yaml.append("  kingdomStartingMoney: ").append(config.economy.kingdomStartingMoney).append("  # Kingdom starting money (on restart/creation)\n");
        yaml.append("\n");

        // Players
        yaml.append("# Player Settings\n");
        yaml.append("players:\n");
        yaml.append("  # maxPlayers moved to server.maxPlayers (Network Configuration section)\n");
        yaml.append("  limitOverridable: ").append(config.players.limitOverridable).append("  # Player limit can be overridden by admins\n");
        yaml.append("\n");

        // Spawn Points
        yaml.append("# Kingdom Spawn Points\n");
        yaml.append("# Coordinates are tile-based (1 tile = 4x4 meters)\n");
        yaml.append("# 0,0 = map center\n");
        yaml.append("spawns:\n");
        yaml.append("  jennKellonX: ").append(config.spawns.jennKellonX).append("  # Jenn-Kellon spawn X\n");
        yaml.append("  jennKellonY: ").append(config.spawns.jennKellonY).append("  # Jenn-Kellon spawn Y\n");
        yaml.append("  molRehanX: ").append(config.spawns.molRehanX).append("  # Mol-Rehan spawn X\n");
        yaml.append("  molRehanY: ").append(config.spawns.molRehanY).append("  # Mol-Rehan spawn Y\n");
        yaml.append("  hotsX: ").append(config.spawns.hotsX).append("  # HOTS/Libila spawn X\n");
        yaml.append("  hotsY: ").append(config.spawns.hotsY).append("  # HOTS/Libila spawn Y\n");
        yaml.append("\n");

        // Server Properties (SERVERPROPERTIES table)
        yaml.append("# Advanced Server Properties (SERVERPROPERTIES table)\n");
        yaml.append("# These toggles override SERVERS-table defaults and control network / kingdom behavior.\n");
        yaml.append("properties:\n");
        yaml.append("  multiKingdom: ").append(config.properties.multiKingdom).append("  # false = Freedom (PvE), true = kingdoms enabled (PvP)\n");
        yaml.append("  epic: ").append(config.properties.epic).append("  # Epic cluster mission system\n");
        yaml.append("  allowChaos: ").append(config.properties.allowChaos).append("  # Allow Libila/HOTS kingdom (PvP only)\n");
        yaml.append("  newbieFriendly: ").append(config.properties.newbieFriendly).append("  # Newbie protection + starter gear + tutorial\n");
        yaml.append("  spyPrevention: ").append(config.properties.spyPrevention).append("  # Hide skills/stats from other players\n");
        yaml.append("  npcs: ").append(config.properties.npcs).append("  # Enable traders, guards, spirit templars, etc.\n");
        yaml.append("  endGameItems: ").append(config.properties.endGameItems).append("  # Enable artifacts, dragon armor, etc.\n");
        yaml.append("  autoNetworking: ").append(config.properties.autoNetworking).append("  # true = Wurm auto-detects externalIp/Port (OVERRIDES yaml values); false = yaml server.externalIp wins\n");
        yaml.append("  enablePnpPortForward: ").append(config.properties.enablePnpPortForward).append("  # Auto-configure router via UPnP\n");
        yaml.append("  steamQueryPort: ").append(config.properties.steamQueryPort).append("  # Steam server browser port\n");
        yaml.append("  adminPassword: \"").append(escape(config.properties.adminPassword)).append("\"  # GM console admin password\n");

        return yaml.toString();
    }

    // Helper methods for reading from SERVERPROPERTIES map

    private static boolean getBooleanFromMap(java.util.Map<String, String> map, String key, boolean defaultValue) {
        String value = map.get(key);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static int getIntFromMap(java.util.Map<String, String> map, String key, int defaultValue) {
        String value = map.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Escape special YAML characters in strings.
     */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"").replace("\n", "\\n");
    }
}

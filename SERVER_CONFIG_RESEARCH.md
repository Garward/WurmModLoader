# Wurm Unlimited Server Configuration Research

**Research Date:** 2025-11-14
**Purpose:** Research all configurable server settings and design a comprehensive config system that syncs to database on startup

---

## Executive Summary

**Current State:** Wurm has TWO separate configuration systems:
1. **wurm.ini** - Technical/database settings (mostly low-level)
2. **SERVERS table** (logindb) - Gameplay settings (skill rates, timers, etc.)

**Problem:** The wurm.ini file is VERY lacking - it only contains ~74 technical settings, while the database contains 40+ gameplay settings that are NOT exposed via config file.

**Solution:** Create a comprehensive config system that:
- Provides a single, well-documented config file
- Syncs all gameplay settings to the SERVERS table on startup
- Supports runtime reloading
- Preserves existing wurm.ini for technical settings

---

## 1. Current Configuration Files

### wurm.ini Location
```
/home/garward/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/Adventure/wurm.ini
```

### Current wurm.ini Settings (74 total)

**Database Connection (11):**
- DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_DRIVER
- LOGIN_DB_HOST, LOGIN_DB_PORT, LOGIN_DB_USER, LOGIN_DB_PASS, LOGIN_DB_DRIVER
- SITE_DB_HOST, SITE_DB_PORT, SITE_DB_USER, SITE_DB_PASS, SITE_DB_DRIVER

**Database Options (10):**
- USEDB, USE_POOLED_DB, USE_LOGIN_DB, USE_SITE_DB
- ANALYSE_ALL_DB_TABLES, CHECK_ALL_DB_TABLES, OPTIMISE_ALL_DB_TABLES
- USE_SPLIT_CREATURES_TABLE
- CREATE_TEMPORARY_DATABASE_INDICES_AT_STARTUP
- TRACK_OPEN_DATABASE_RESOURCES

**Performance (19):**
- USE_SCHEDULED_EXECUTOR (multiple variants for different systems)
- SCHEDULED_EXECUTOR_SERVICE_NUMBER_OF_THREADS
- NUMBER_OF_DB_PLAYER_POSITIONS_TO_UPDATE_EACH_TIME
- NUMBER_OF_DB_CREATURE_POSITIONS_TO_UPDATE_EACH_TIME
- NUMBER_OF_DB_ITEM_DAMAGES_TO_UPDATE_EACH_TIME
- NUMBER_OF_DIRTY_MESH_ROWS_TO_SAVE_EACH_CALL
- USE_DIRECT_BYTE_BUFFERS_FOR_MESHIO
- USE_QUEUE_TO_SEND_DATA_TO_PLAYERS
- USE_MULTI_THREADED_BANK_POLLING

**Logging (6):**
- USE_TILE_LOG, USE_ITEM_TRANSFER_LOG, CHECK_WURMLOGS
- USE_DATABASE_FOR_SERVER_STATISTICS_LOG
- PLAYERLOG, LAG_THRESHOLD

**Database Maintenance (5):**
- PRUNEDB, PREPSTATEMENTS, DBSTATS, DBPATH

**Server Mode (8):**
- IS_GAME_SERVER, DEVMODE, MAINTAINING, CRASHED
- STARTCHALLENGE, PROSPECT, CAVEIMG, CREATESEEDS

**Connection (2):**
- PLAYER_CONN_MILLIS, WEB_PATH

**Social Media (7):**
- TRELLO_BOARD_ID, TRELLO_MUTE_VOTE_BOARD_ID
- USE_SCHEDULED_EXECUTOR_FOR_TRELLO, USE_SCHEDULED_EXECUTOR_TO_UPDATE_TWITTER

**Misc (6):**
- NPCS, RUNBATCH

**⚠️ NOTABLE ABSENCE:**
- NO skill gain rates
- NO action timers
- NO creature limits
- NO deed settings
- NO starting skill values
- NO combat modifiers

---

## 2. SERVERS Table Configuration (Database)

### Database Location
```
logindb (typically SQLite or MySQL depending on config)
Table: SERVERS
```

### Gameplay Settings in SERVERS Table (40+ settings)

**Analyzed from:**
- `ServerEntry.java` (lines 94-161, 1059-1223)
- `Servers.java` loadAllServers() (lines 417-614)

#### Skill & Progression Settings (9)
```java
SKILLGAINRATE             FLOAT    DEFAULT 1.0    // Skill gain multiplier
ACTIONTIMER               FLOAT    DEFAULT 1.0    // Action speed multiplier
SKILLBASICSTART           FLOAT    DEFAULT 20.0   // Starting basic skills
SKILLMINDLOGICSTART       FLOAT    DEFAULT 20.0   // Starting mind/logic
SKILLFIGHTINGSTART        FLOAT    DEFAULT 1.0    // Starting fighting
SKILLOVERALLSTART         FLOAT    DEFAULT 1.0    // Starting overall
SKILLBODYCONTROLSTART     FLOAT    DEFAULT 20.0   // Starting body control
CRMOD                     FLOAT    DEFAULT 1.0    // Combat rating modifier
HOTADELAY                 INT      DEFAULT 2160   // Hunt of the Ancients delay
```

**Use Cases:**
- Power servers: SKILLGAINRATE=10.0, ACTIONTIMER=0.1 (10x skills, 10x action speed)
- Hardcore servers: SKILLGAINRATE=0.5, ACTIONTIMER=2.0 (slower progression)
- New player friendly: SKILLBASICSTART=30.0 (higher starting skills)

#### Creature Settings (4)
```java
MAXCREATURES              INT      DEFAULT 1000   // Max total creatures
PERCENT_AGG_CREATURES     FLOAT    DEFAULT 10.0   // % of aggressive creatures
TREEGROWTH                INT      DEFAULT 20     // Tree growth rate
BREEDING                  BIGINT   DEFAULT 0      // Breeding timer
```

**Use Cases:**
- High population: MAXCREATURES=5000, PERCENT_AGG_CREATURES=30.0 (more dangerous)
- Peaceful server: PERCENT_AGG_CREATURES=1.0 (mostly passive)
- Fast breeding: BREEDING=600000 (10 minutes in milliseconds)

#### Player & Server Limits (2)
```java
MAXPLAYERS                INT      DEFAULT 200    // Max concurrent players
MESHSIZE                  INT      DEFAULT 2048   // Map size (read-only)
```

#### Economy Settings (5)
```java
UPKEEP                    BOOLEAN  DEFAULT TRUE   // Village upkeep enabled
MAXDEED                   INT      DEFAULT 0      // Max deed size (0=unlimited)
FREEDEEDS                 BOOLEAN  DEFAULT FALSE  // Free deeds enabled
TRADERMAX                 INT      DEFAULT 500000 // Trader max money (irons)
TRADERINIT                INT      DEFAULT 10000  // Trader starting money (irons)
```

**Use Cases:**
- No-upkeep server: UPKEEP=FALSE
- Mega-deeds: MAXDEED=100 (100x100 max deed)
- Free play: FREEDEEDS=TRUE
- Economy boost: TRADERMAX=5000000, TRADERINIT=100000

#### World Settings (3)
```java
FIELDGROWTH               BIGINT   DEFAULT 86400000 // Crop growth time (ms)
KINGSMONEY                INT      DEFAULT 0         // Kingdom money at restart
TUNNELING                 INT      DEFAULT 51        // Tunneling hits required
```

**Use Cases:**
- Fast farming: FIELDGROWTH=3600000 (1 hour)
- Easy mining: TUNNELING=10 (fewer hits needed)

#### Server Identity (4)
```java
NAME                      VARCHAR  // Server name
MOTD                      VARCHAR  // Message of the day
STEAMPW                   VARCHAR  // Steam server password
KINGDOM                   TINYINT  // Default kingdom
```

#### Server Type Flags (6)
```java
PVP                       BOOLEAN  // PvP server
HOMESERVER                BOOLEAN  // Home server
EPIC                      BOOLEAN  // Epic server
CHALLENGE                 BOOLEAN  // Challenge server
ISTEST                    BOOLEAN  // Test server
RANDOMSPAWNS              BOOLEAN  // Random spawn points
```

#### Network Settings (7)
```java
EXTERNALIP                VARCHAR  // Public IP
EXTERNALPORT              VARCHAR  // Public port
INTRASERVERADDRESS        VARCHAR  // Internal IP
INTRASERVERPORT           VARCHAR  // Internal port
INTRASERVERPASSWORD       VARCHAR  // Internal password
RMIPORT                   VARCHAR  // RMI port
REGISTRATIONPORT          VARCHAR  // Registration port
```

---

## 3. Proposed Config System Architecture

### Design Goals

1. **Single Source of Truth** - Config file → Database (not vice versa)
2. **World-Folder-Agnostic** - Work with Adventure, Riverweave, Creative, etc.
3. **Validation** - Type checking, range validation, dependency validation
4. **Documentation** - Inline comments explaining each setting
5. **Backward Compatible** - Don't break existing wurm.ini
6. **Runtime Reload** - Support `/admin config reload` command

### File Structure

```
~/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/
├── wurm.ini              # Keep for technical settings (don't touch)
└── server_config.yaml    # NEW: Comprehensive gameplay settings
```

**Why YAML?**
- Human-readable and well-commented
- Type-safe (strings, numbers, booleans)
- Hierarchical (grouping related settings)
- Standard library support in Java

**Alternative:** Properties file (if YAML library is a dependency concern)

---

## 4. Proposed Config File Format

### server_config.yaml (Example)

```yaml
# Wurm Unlimited Server Configuration
# This file syncs to the SERVERS table on startup
# Server ID: Determined by wurm.ini setting or auto-detected

server:
  # Server Identity
  name: "My Epic Server"
  motd: "Welcome to the best Wurm server!"
  steam_password: ""  # Leave empty for no password
  kingdom: 4  # 1=Freedom, 2=Mol-Rehan, 3=JK, 4=HOTS

  # Server Type
  pvp: true
  epic: false
  challenge: false
  home_server: true
  random_spawns: false

# Skill & Progression Settings
skills:
  # Skill gain multiplier (1.0 = normal, 10.0 = 10x faster)
  gain_rate: 5.0

  # Starting skill values (default: 20.0 for basic, 1.0 for fighting)
  starting:
    basic: 30.0        # Digging, mining, smithing, etc.
    mind_logic: 30.0   # Mind speed, mind logic
    fighting: 10.0     # Fighting, various weapon skills
    body_control: 30.0 # Body control
    overall: 10.0      # Overall skill level multiplier

# Action & Combat Settings
combat:
  # Action timer multiplier (1.0 = normal, 0.5 = 2x faster actions)
  action_speed: 0.5

  # Combat rating modifier (affects CR calculations)
  rating_modifier: 1.5

  # Hunt of the Ancients delay (in Wurm hours, default 2160 = 90 days)
  hota_delay: 720  # 30 days

# Creature Settings
creatures:
  # Maximum total creatures on server
  max_total: 3000

  # Percentage of aggressive creatures (default: 10.0)
  percent_aggressive: 15.0

  # Animal breeding timer (milliseconds, 0 = use default)
  breeding_timer: 1800000  # 30 minutes

# World Settings
world:
  # Tree growth rate (default: 20)
  tree_growth: 50

  # Crop growth time (milliseconds, default: 86400000 = 24 hours)
  field_growth_time: 3600000  # 1 hour

  # Tunneling hits required to mine (default: 51)
  tunneling_hits: 30

# Economy Settings
economy:
  # Village upkeep enabled
  upkeep_enabled: false

  # Max deed size (0 = unlimited, otherwise NxN tiles)
  max_deed_size: 0

  # Free deeds (no cost to found)
  free_deeds: true

  # Trader settings (in iron coins)
  traders:
    max_money: 1000000     # 10 gold
    starting_money: 50000  # 50 silver

  # Kingdom starting money (on restart/creation)
  kingdom_starting_money: 100000

# Player Limits
players:
  # Maximum concurrent players
  max_players: 500

  # Player limit can be overridden by admins
  limit_overridable: true
```

---

## 5. Implementation Plan

### Phase 1: Config Loader

**File:** `/wurmmodloader-core/src/main/java/com/garward/wurmmodloader/config/ServerConfigLoader.java`

**Responsibilities:**
- Load `server_config.yaml` on startup
- Validate all settings (types, ranges, dependencies)
- Provide default values for missing settings
- Log warnings for invalid values

**Key Methods:**
```java
public class ServerConfigLoader {
    public static ServerConfig load(String worldName);
    public static void validate(ServerConfig config);
    public static ServerConfig getDefaults();
}
```

### Phase 2: Database Sync

**File:** `/wurmmodloader-core/src/main/java/com/garward/wurmmodloader/config/ServerConfigSync.java`

**Responsibilities:**
- Update SERVERS table with config values
- Use world-folder-agnostic database connections (DatabaseConnectionUtil pattern)
- Only update if values have changed (avoid unnecessary writes)
- Handle database errors gracefully

**Key Methods:**
```java
public class ServerConfigSync {
    public static void syncToDatabase(ServerConfig config, int serverId);
    public static boolean needsUpdate(ServerConfig config, int serverId);
}
```

### Phase 3: Integration Hook

**Location:** Modify `DelegatedLauncher.java` or create new hook in server startup

**Pseudocode:**
```java
// During server startup, AFTER wurm.ini is loaded, BEFORE server starts
public void onServerPreInit() {
    // 1. Load server_config.yaml
    ServerConfig config = ServerConfigLoader.load(getCurrentWorldName());

    // 2. Validate config
    ServerConfigLoader.validate(config);

    // 3. Sync to database
    int serverId = Servers.getLocalServerId();
    ServerConfigSync.syncToDatabase(config, serverId);

    // 4. Reload server settings from database (standard Wurm behavior)
    Servers.loadAllServers(true);

    logger.info("[ServerConfig] Configuration synced successfully");
}
```

### Phase 4: Runtime Reload (Optional)

**Admin Command:** `/admin config reload`

**Behavior:**
- Reload `server_config.yaml`
- Validate changes
- Sync to database
- Reload Servers.localServer settings
- Broadcast change message to online players

---

## 6. Implementation Details

### World-Folder-Agnostic Pattern

**DO NOT hardcode world folder names!**

```java
// ❌ BAD - Hardcoded world name
String configPath = "Adventure/server_config.yaml";

// ✅ GOOD - Dynamic world detection
String worldName = getCurrentWorldName();  // e.g., "Adventure", "Riverweave"
String configPath = worldName + "/server_config.yaml";
```

### Database Connection Pattern

**Use existing DatabaseConnectionUtil pattern:**

```java
Connection conn = null;
try {
    // Get connection to logindb (world-agnostic)
    conn = DatabaseConnectionUtil.getLoginDbConnection();

    // Update SERVERS table
    String sql = "UPDATE SERVERS SET SKILLGAINRATE=?, ACTIONTIMER=?, ... WHERE SERVER=?";
    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setFloat(1, config.skills.gainRate);
    ps.setFloat(2, config.combat.actionSpeed);
    // ... set other parameters
    ps.setInt(N, serverId);
    ps.executeUpdate();

} finally {
    DatabaseConnectionUtil.closeConnection(conn);
}
```

### Validation Examples

```java
public static void validate(ServerConfig config) {
    // Range validation
    if (config.skills.gainRate <= 0 || config.skills.gainRate > 1000) {
        throw new ConfigException("skills.gain_rate must be between 0 and 1000");
    }

    // Logical validation
    if (config.creatures.maxTotal < 100) {
        logger.warn("creatures.max_total is very low, server may feel empty");
    }

    // Dependency validation
    if (config.economy.freeDeeds && config.economy.upkeepEnabled) {
        logger.warn("free_deeds=true and upkeep_enabled=true may cause confusion");
    }
}
```

---

## 7. Benefits

### For Server Admins

✅ **Single config file** - All gameplay settings in one place
✅ **Well-documented** - Inline comments explain each setting
✅ **Version controllable** - Track changes with git
✅ **Easy to share** - Copy config file to new world folders
✅ **No SQL knowledge required** - No manual database updates

### For Mod Developers

✅ **Predictable settings** - Always loaded from config
✅ **Easy testing** - Change config, restart server
✅ **No database tools needed** - Edit YAML file instead
✅ **Validation** - Catch invalid configs before they cause issues

### For Players

✅ **Consistent experience** - Settings don't change unexpectedly
✅ **Transparent** - Admins can share config file
✅ **Easier setup** - New servers start with good defaults

---

## 8. Migration Path

### For Existing Servers

1. **Backup database** - Always backup before migration
2. **Generate config from database** - Tool to export current SERVERS table values to YAML
3. **Review and edit** - Admins review generated config
4. **Enable sync** - Set flag to start using config→database sync
5. **Test** - Verify settings match expectations

### For New Servers

1. **Copy template** - Use provided `server_config.yaml.template`
2. **Edit settings** - Customize for server style (PvP, PvE, fast progression, etc.)
3. **Start server** - Config automatically syncs to database

---

## 9. Technical Challenges

### Challenge 1: Server ID Detection

**Problem:** Config needs to know which SERVERS row to update

**Solution:**
- Read server ID from wurm.ini if present
- OR use `Servers.getLocalServerId()` during startup
- OR add `server_id` field to config file

**Recommended:** Use `Servers.getLocalServerId()` (already available at startup)

### Challenge 2: YAML Library Dependency

**Problem:** Java doesn't include YAML parser by default

**Options:**
1. **SnakeYAML** (most popular, ~300KB)
2. **Jackson YAML** (if already using Jackson for JSON)
3. **Properties file** (no dependency, less readable)

**Recommended:** SnakeYAML (widely used, well-tested)

### Challenge 3: Timing

**Problem:** When to sync config → database?

**Options:**
1. Before `Servers.loadAllServers()` - Config values immediately available
2. After `Servers.loadAllServers()` - Risk of being overwritten
3. Replace `Servers.loadAllServers()` - High risk, invasive

**Recommended:** Option 1 - Sync BEFORE loadAllServers()

### Challenge 4: Partial Updates

**Problem:** What if config file only has some settings?

**Solution:**
- Merge with database values (config file overrides database)
- OR require complete config file (safer, more predictable)

**Recommended:** Require complete config file (use defaults for missing values)

---

## 10. Testing Strategy

### Unit Tests

```java
@Test
public void testConfigLoading() {
    ServerConfig config = ServerConfigLoader.load("test_config.yaml");
    assertEquals(5.0f, config.skills.gainRate);
}

@Test
public void testValidation() {
    ServerConfig config = new ServerConfig();
    config.skills.gainRate = -1.0f;  // Invalid
    assertThrows(ConfigException.class, () -> {
        ServerConfigLoader.validate(config);
    });
}
```

### Integration Tests

1. **Config → Database Sync**
   - Load config with specific values
   - Sync to test database
   - Query SERVERS table
   - Verify values match

2. **World-Folder-Agnostic**
   - Test with "Adventure" world
   - Test with "Riverweave" world
   - Test with custom world name
   - All should work identically

3. **Reload Behavior**
   - Change config file
   - Trigger reload
   - Verify new values in database
   - Verify server picks up changes

---

## 11. Documentation Requirements

### For Users

**File:** `SERVER_CONFIG_GUIDE.md`

Contents:
- What is server_config.yaml
- How to edit it safely
- List of all settings with explanations
- Common configurations (PvP, PvE, fast progression, etc.)
- Troubleshooting

### For Developers

**File:** `SERVER_CONFIG_API.md`

Contents:
- How config loading works
- How to add new settings
- Validation rules
- Database sync mechanism
- Testing procedures

---

## 12. Future Enhancements

### Phase 2 Features (Later)

1. **Hot Reload** - Change settings without restart
2. **Per-World Configs** - Different settings for Adventure vs Riverweave
3. **Config Profiles** - Predefined configs (PvP, PvE, RP, etc.)
4. **Web UI** - Edit config via web interface
5. **Config Validation Tool** - Standalone tool to check config before starting server

### Mod Integration

Mods could add their own config sections:

```yaml
# Mod-specific settings
mods:
  powerscaling:
    enabled: true
    base_power: 100

  materialsystem:
    enabled: true
    custom_materials: true
```

---

## 13. Next Steps

1. **Decide on config format** - YAML vs Properties
2. **Add SnakeYAML dependency** (if using YAML)
3. **Create ServerConfig data class**
4. **Implement ServerConfigLoader**
5. **Implement ServerConfigSync**
6. **Add startup hook to DelegatedLauncher**
7. **Create default server_config.yaml template**
8. **Test with Adventure world**
9. **Document usage**

---

## 14. Example: Typical Server Configurations

### PvP Server (High Risk, Fast Pace)
```yaml
skills:
  gain_rate: 3.0
  starting:
    fighting: 20.0
    basic: 25.0
combat:
  action_speed: 0.7  # Faster combat
  rating_modifier: 1.2
creatures:
  percent_aggressive: 25.0  # More dangerous
economy:
  upkeep_enabled: true
  free_deeds: false
```

### PvE Server (Relaxed, Exploration)
```yaml
skills:
  gain_rate: 5.0
  starting:
    basic: 30.0
combat:
  action_speed: 0.5  # Fast actions
creatures:
  percent_aggressive: 5.0  # Mostly peaceful
economy:
  upkeep_enabled: false
  free_deeds: true
world:
  field_growth_time: 7200000  # 2 hours
```

### Hardcore Server (Slow, Challenging)
```yaml
skills:
  gain_rate: 0.5  # Half speed
  starting:
    basic: 10.0  # Lower starting skills
combat:
  action_speed: 1.5  # Slower actions
  rating_modifier: 0.8
creatures:
  percent_aggressive: 30.0
economy:
  upkeep_enabled: true
```

---

## 15. Code Reference Locations

**Server Settings:**
- `com.wurmonline.server.ServerEntry` - All gameplay settings (lines 94-161, 1059-1223)
- `com.wurmonline.server.Servers` - Loading from database (lines 417-614)
- `com.wurmonline.server.Constants` - Technical settings from wurm.ini (lines 14-120)

**Database:**
- SERVERS table - logindb (typically SQLite: `logindb/wurmlogin.db` or MySQL)
- UPDATE statement: ServerEntry.saveNewGui() (line 834)

**Load Method:**
- `Servers.loadAllServers()` - Called during server startup
- Reads SERVERS table and populates ServerEntry objects

---

**Research Status:** ✅ COMPLETE
**Next Action:** Design and implement ServerConfigLoader
**Priority:** 🔥 HIGH (major improvement to server administration)

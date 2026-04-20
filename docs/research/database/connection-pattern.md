# Database Connection Pattern - CRITICAL

## ⚠️ **NEVER Hardcode World Folder Paths!**

World folder names vary across servers:
- `Adventure` (default)
- `Riverweave` (user's server)
- `Creative`
- Any custom name the server admin chooses

**Hardcoding paths WILL break when the world folder changes!**

---

## ✅ **Correct Pattern: Use DatabaseConnectionUtil**

All database managers **MUST** use `DatabaseConnectionUtil` to get connections:

```java
import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;

// CORRECT - World-agnostic
Connection conn = DatabaseConnectionUtil.getCreatureDbConnection();
Connection conn = DatabaseConnectionUtil.getPlayerDbConnection();
Connection conn = DatabaseConnectionUtil.getZonesDbConnection();
Connection conn = DatabaseConnectionUtil.getDeityDbConnection();
Connection conn = DatabaseConnectionUtil.getItemDbConnection();

// ALWAYS close connections
try {
    conn = DatabaseConnectionUtil.getCreatureDbConnection();
    // ... do work ...
} finally {
    DatabaseConnectionUtil.closeConnection(conn);
}
```

---

## ❌ **WRONG: Hardcoded Paths**

```java
// WRONG - Breaks when world folder name changes!
String path = "Adventure/sqlite/wurmcreatures.db";
String path = "C:/Program Files (x86)/Steam/.../Adventure/sqlite/..."; // hardcoded — breaks on every other machine
Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);

// WRONG - Hardcoded in queries
Statement stmt = conn.createStatement();
stmt.execute("ATTACH DATABASE 'Adventure/sqlite/wurmitems.db' AS items");
```

---

## How It Works

`DatabaseConnectionUtil` uses Wurm's `DbConnector` class via reflection:

```java
// Internal implementation (you don't need to do this)
Class<?> dbConnectorClass = Class.forName("com.wurmonline.server.DbConnector");
Method method = dbConnectorClass.getMethod("getCreatureDbCon");
return (Connection) method.invoke(null);
```

**DbConnector automatically resolves:**
- Active world folder name from `wurm.ini`
- Full path to SQLite database files
- Connection pooling and configuration

---

## Available Database Connections

| Method | Database | Use Case |
|--------|----------|----------|
| `getCreatureDbConnection()` | wurmcreatures.db | Creatures, NPCs |
| `getPlayerDbConnection()` | wurmplayers.db | Players, skills |
| `getZonesDbConnection()` | wurmzones.db | Zones, tiles, mining |
| `getDeityDbConnection()` | wurmdeities.db | Deities, faith, rituals |
| `getItemDbConnection()` | wurmitems.db | Items, containers |
| `getEconomyDbConnection()` | wurmeconomy.db | Shops, traders |
| `getLogsDbConnection()` | wurmlogs.db | Audit logs |

---

## Example: CreatureDatabaseManager

```java
private void addColumnIfNeeded(String columnName, String columnType) {
    Connection conn = null;
    try {
        // CORRECT - Uses DatabaseConnectionUtil
        conn = DatabaseConnectionUtil.getCreatureDbConnection();

        if (columnExists(conn, columnName)) {
            return;
        }

        String sql = "ALTER TABLE CREATURES ADD COLUMN " + columnName + " " + columnType;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }

    } catch (SQLException e) {
        logger.log(Level.WARNING, "Failed to add column", e);
    } finally {
        // ALWAYS close
        DatabaseConnectionUtil.closeConnection(conn);
    }
}
```

---

## For New Database Managers

When creating a new `[Table]DatabaseManager`, always:

1. ✅ **Import DatabaseConnectionUtil**
   ```java
   import com.garward.wurmmodloader.core.database.DatabaseConnectionUtil;
   ```

2. ✅ **Use appropriate getXxxDbConnection() method**
   ```java
   Connection conn = DatabaseConnectionUtil.getZonesDbConnection();
   ```

3. ✅ **Always close connections in finally block**
   ```java
   try {
       conn = DatabaseConnectionUtil.getXxxDbConnection();
       // work...
   } finally {
       DatabaseConnectionUtil.closeConnection(conn);
   }
   ```

4. ❌ **Never hardcode paths or world folder names**

---

## Testing Across World Folders

To verify your database manager works with any world name:

```bash
# Test with default world
./WurmServerLauncher start=Adventure

# Test with custom world name
./WurmServerLauncher start=MyCustomWorld

# Test with user's world
./WurmServerLauncher start=Riverweave
```

Your mod should work identically with all world folder names.

---

## Why This Matters

**Scenario: User changes world name**
```bash
# Server starts with "Adventure" world
# Your mod adds columns to Adventure/sqlite/wurmcreatures.db

# User decides to rename world to "Riverweave"
# Server now uses Riverweave/sqlite/wurmcreatures.db
```

**With DatabaseConnectionUtil:**
- ✅ Your mod automatically uses the new world folder
- ✅ Columns are added to the correct database
- ✅ No configuration changes needed

**With hardcoded paths:**
- ❌ Your mod tries to access Adventure/sqlite/... (doesn't exist!)
- ❌ SQLException: database not found
- ❌ Mod fails to load

---

## Checklist for Database Managers

Before committing a new `[Table]DatabaseManager`:

- [ ] Uses `DatabaseConnectionUtil` for all connections
- [ ] Never hardcodes "Adventure" or any world name
- [ ] Never hardcodes full filesystem paths
- [ ] Always closes connections in `finally` blocks
- [ ] Tested with at least 2 different world folder names

---

## Summary

**Golden Rule:** Let Wurm's `DbConnector` handle all path resolution.

**Never hardcode:**
- ❌ World folder names (Adventure, Creative, etc.)
- ❌ Database file paths
- ❌ Full filesystem paths

**Always use:**
- ✅ `DatabaseConnectionUtil.getXxxDbConnection()`
- ✅ `DatabaseConnectionUtil.closeConnection(conn)`
- ✅ Reflection-based access via `DbConnector`

---

**This pattern is used by all official database managers:**
- `CreatureDatabaseManager` ✅
- `DeityDatabaseManager` (in progress)
- `ZoneDatabaseManager` (in progress)
- `PlayerDatabaseManager` (planned)
- `ItemDatabaseManager` (planned)

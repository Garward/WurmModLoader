# Zone and Deity Database Events - Implementation Plan

## Priority Justification

**Why zones/deities before items:**
- ✅ Item wurmIds ARE stable across restarts (stored in wurmitems.db)
- ✅ Items persist properly already
- 🔥 Zones need custom data for territory/pollution/resource systems
- 🔥 Deities need custom data for religion/favor mechanics
- Items can wait since they're already persistent

---

## 1. ZONES Database Events

### Database: wurmzones.db

**Key Tables:**
- `ZONES` - Main zone data (ZONEID primary key, BLOB storage for creatures/items/structures)
- `FOCUSZONES` - Named zones (ID primary key)
- `MINING` - Tile mining data

### Zone Persistence Pattern

**Unlike creatures**, zones don't have a simple DbZone.save() method. Instead:
- Zones are loaded on server start
- Zone data (creatures/items/structures) stored as BLOBs
- Updates happen via tile-level operations

### Proposed Hook Points

**Option 1: Tile-Level Events** (RECOMMENDED)
Fire events when tiles are modified/saved:

```java
public class TileDbSaveEvent {
    private final int tileX;
    private final int tileY;
    private final boolean surface;
    private final int zoneId;
    // Custom column management
}

public class TileDbLoadEvent {
    private final int tileX;
    private final int tileY;
    private final boolean surface;
    private final ResultSet resultSet;
}
```

**Option 2: Zone-Level Events**
Fire events when zones are loaded/unloaded:

```java
public class ZoneDbLoadEvent {
    private final Zone zone;
    private final int zoneId;
}

public class ZoneDbSaveEvent {
    private final Zone zone;
    private final int zoneId;
}
```

### Use Cases

**Territory Mods:**
```sql
ALTER TABLE ZONES ADD COLUMN CONTROL_FACTION_ID INTEGER DEFAULT 0;
ALTER TABLE ZONES ADD COLUMN CONTROL_STRENGTH FLOAT DEFAULT 0.0;
ALTER TABLE ZONES ADD COLUMN CONTROL_TIMESTAMP BIGINT DEFAULT 0;
```

**Environment Mods:**
```sql
ALTER TABLE ZONES ADD COLUMN POLLUTION_LEVEL FLOAT DEFAULT 0.0;
ALTER TABLE ZONES ADD COLUMN RADIATION_LEVEL FLOAT DEFAULT 0.0;
ALTER TABLE ZONES ADD COLUMN LAST_ENVIRONMENTAL_TICK BIGINT DEFAULT 0;
```

**Resource Mods:**
```sql
ALTER TABLE MINING ADD COLUMN ORE_VEIN_ID INTEGER DEFAULT 0;
ALTER TABLE MINING ADD COLUMN VEIN_QUALITY FLOAT DEFAULT 0.0;
ALTER TABLE MINING ADD COLUMN VEIN_RICHNESS INTEGER DEFAULT 0;
```

### Classes to Patch

```
com.wurmonline.server.zones.Zones - Zone management
com.wurmonline.server.zones.DbZone - Zone database operations
com.wurmonline.mesh.MeshIO - Tile data persistence (for tile-level events)
```

---

## 2. DEITIES Database Events

### Database: wurmdeities.db

**Key Tables:**
- `DEITIES` (ID primary key) - Main deity data
- `HELPERS` (WURMID primary key) - Player karma/deity association
- `ENTITIES` (ID primary key) - Demigods/ascended players
- `EPICMISSIONS` - Epic mission tracking
- `RITUALCASTS` / `RITUALCLAIMS` - Ritual system

### Deity Persistence Pattern

Deities ARE persisted with save() methods:

```java
public class DbDeity {
    public boolean create() { ... }  // INSERT
    public boolean update() { ... }  // UPDATE
    public static Deity load(int id) { ... } // SELECT
}
```

### Proposed Events

```java
public class DeityDbSaveEvent {
    private final int deityId;
    private final String deityName;
    // Custom column management like CreatureDbSaveEvent
}

public class DeityDbLoadEvent {
    private final int deityId;
    private final ResultSet resultSet;
}
```

### Use Cases

**Custom Deity Stats:**
```sql
ALTER TABLE DEITIES ADD COLUMN SPELL_POWER_MULTIPLIER FLOAT DEFAULT 1.0;
ALTER TABLE DEITIES ADD COLUMN FAVOR_REGEN_RATE FLOAT DEFAULT 1.0;
ALTER TABLE DEITIES ADD COLUMN MAX_FOLLOWERS INTEGER DEFAULT -1; -- -1 = unlimited
ALTER TABLE DEITIES ADD COLUMN SPECIAL_ABILITIES TEXT DEFAULT '';
```

**Enhanced Faith System:**
```sql
ALTER TABLE HELPERS ADD COLUMN DEVOTION_LEVEL INTEGER DEFAULT 0;
ALTER TABLE HELPERS ADD COLUMN PRAYER_STREAK INTEGER DEFAULT 0;
ALTER TABLE HELPERS ADD COLUMN LAST_PRAYER_TIME BIGINT DEFAULT 0;
ALTER TABLE HELPERS ADD COLUMN TOTAL_SACRIFICES INTEGER DEFAULT 0;
```

**Demigod Enhancements:**
```sql
ALTER TABLE ENTITIES ADD COLUMN ASCENSION_DATE BIGINT DEFAULT 0;
ALTER TABLE ENTITIES ADD COLUMN FOLLOWERS_COUNT INTEGER DEFAULT 0;
ALTER TABLE ENTITIES ADD COLUMN DIVINE_POWERS TEXT DEFAULT '';
```

### Classes to Patch

```
com.wurmonline.server.deities.DbDeity - Deity database operations
com.wurmonline.server.deities.Deity - Main deity class
```

---

## Implementation Order

### Phase 1: Deities (Simpler - Start Here) ✅

Deities follow the same pattern as creatures (have save/load methods):

1. Create `DeityDbSaveEvent` and `DeityDbLoadEvent`
2. Create `DbDeityPatch` (hook create/update/load methods)
3. Create `DeityDatabaseManager`
4. Add to ProxyServerHook and ServerHook
5. Register patch in DelegatedLauncher

**Estimated Time:** 30-45 minutes (reuse creature pattern)

### Phase 2: Zones (More Complex - After Deities) 🔄

Zones need investigation of save pattern:

1. Research how zones persist (BLOBs vs. explicit saves)
2. Decide on tile-level vs. zone-level events
3. Create appropriate events
4. Create patch for chosen hook point
5. Create ZoneDatabaseManager
6. Test with territory/environment mods

**Estimated Time:** 1-2 hours (need research)

---

## Alternative: Focus on MINING Table First

The **MINING table** in wurmzones.db is tile-based and simpler than zones:

```sql
CREATE TABLE MINING
(
    TILEX INT,
    TILEY INT,
    TILE INT,
    DATE BIGINT,
    RESURVEYED INT,
    LAYER INT
);
```

**Could add:**
```sql
ALTER TABLE MINING ADD COLUMN ORE_VEIN_ID INTEGER DEFAULT 0;
ALTER TABLE MINING ADD COLUMN VEIN_QUALITY FLOAT DEFAULT 50.0;
ALTER TABLE MINING ADD COLUMN LAST_MINED BIGINT DEFAULT 0;
```

This would enable resource/mining mods without solving the full zone problem.

---

## Recommended Approach

**Start with Deities (30-45 min):**
- Straightforward save/load pattern like creatures
- Enables religion/faith mods immediately
- Proves pattern works for static game objects

**Then investigate Zones:**
- Research exact save pattern
- Might need tile-level events instead
- Consider MINING table as proof-of-concept

**Items can wait because:**
- Item wurmIds are stable
- Items already persist correctly
- MaterialSystem/SoulboundGear can wait until zones/deities done

---

## Question for User

**For zones, which use case is more important?**

A) **Territory/faction control** (zone-level data)
B) **Mining/resources/pollution** (tile-level data)
C) **Both equally**

This determines whether we need:
- Zone-level events (for A)
- Tile-level events (for B)
- Both (for C)

**Should I start with DeityDbSaveEvent/LoadEvent now?** (Following exact creature pattern, should be quick)

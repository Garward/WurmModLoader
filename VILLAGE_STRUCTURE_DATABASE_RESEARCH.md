# Village & Structure Database Events - Research Findings

**Research Date:** 2025-11-14
**Researcher:** Claude Code
**Purpose:** Determine feasibility and implementation approach for village/structure database events

---

## Executive Summary

**Structures:** ✅ **HIGHLY RECOMMENDED** - Perfect candidate for database events
**Villages:** ⚠️ **COMPLEX** - Possible but requires different approach than structures

---

## 1. Structure Database Events (RECOMMENDED)

### Database Schema

**Table:** `STRUCTURES` (in wurmzones.db)

```sql
CREATE TABLE STRUCTURES (
    WURMID          BIGINT    primary key,
    CENTERX         INT,
    CENTERY         INT,
    ROOF            TINYINT,
    FINISHED        TINYINT(1)  NOT NULL DEFAULT 0,
    FINFINISHED     TINYINT(1)  NOT NULL DEFAULT 0,
    SURFACED        TINYINT(1),
    NAME            VARCHAR(255),
    WRITID          BIGINT,
    ALLOWSALLIES    TINYINT(1)  NOT NULL DEFAULT 0,
    ALLOWSVILLAGERS TINYINT(1)  NOT NULL DEFAULT 0,
    ALLOWSKINGDOM   TINYINT(1)  NOT NULL DEFAULT 0,
    STRUCTURETYPE   TINYINT     NOT NULL DEFAULT 0,
    PLANNER         VARCHAR(40) NOT NULL DEFAULT "",
    OWNERID         BIGINT      NOT NULL DEFAULT -10,
    SETTINGS        INT         NOT NULL DEFAULT 0,
    VILLAGE         INT         NOT NULL DEFAULT -1
);
```

### Save/Load Pattern

**Class:** `com.wurmonline.server.structures.DbStructure`

**Load Method:** `DbStructure.load()` (line 62)
```java
void load() throws IOException, NoSuchStructureException {
    Connection dbcon = DbConnector.getZonesDbCon();
    ps = dbcon.prepareStatement(GET_STRUCTURE); // SELECT * FROM STRUCTURES WHERE WURMID=?
    ps.setLong(1, this.getWurmId());
    rs = ps.executeQuery();
    if (rs.next()) {
        this.setStructureType(rs.getByte("STRUCTURETYPE"));
        this.setSurfaced(rs.getBoolean("SURFACED"));
        this.setRoof(rs.getByte("ROOF"));
        this.setName(rs.getString("NAME"), false);
        // ... load other fields
    }
}
```

**Save Method:** `DbStructure.save()` (line 127)
```java
public void save() throws IOException {
    Connection dbcon = DbConnector.getZonesDbCon();
    if (!this.exists(dbcon)) {
        this.create(dbcon); // INSERT INTO STRUCTURES
    }
    ps = dbcon.prepareStatement(SAVE_STRUCTURE);
    // UPDATE STRUCTURES SET CENTERX=?,CENTERY=?,ROOF=?,... WHERE WURMID=?
    ps.setInt(1, this.getCenterX());
    ps.setInt(2, this.getCenterY());
    // ... set other fields
    ps.executeUpdate();
}
```

### Hook Points

**✅ Perfect for bytecode patching:**

1. **Save Event:** Patch `DbStructure.save()`
   - Insert BEFORE vanilla save to fire `StructureDbSaveEvent`
   - Mods can add custom columns and data
   - Use same pattern as `DbDeityPatch.patchSaveMethod()`

2. **Load Event:** Patch `DbStructure.load()`
   - Insert AFTER result set is populated to fire `StructureDbLoadEvent`
   - Pass ResultSet to event so mods can read custom columns
   - Use same pattern as `DbDeityPatch.patchConstructor()`

### Implementation Priority

**Priority:** 🔥 **HIGH**

**Reasons:**
- Clean save/load methods (easy to hook)
- Widely used by mods (houses, bridges, fences, walls)
- Row-based storage (easy custom column additions)
- Follows exact same pattern as creatures/deities

**Use Cases:**
- Custom structure properties (durability multipliers, special effects)
- Structure upgrade systems (tiers, enhancements)
- Structure ownership metadata (permissions, access logs)
- Building material quality tracking

---

## 2. Village Database Events (COMPLEX)

### Database Schema

**Table:** `VILLAGES` (in wurmzones.db)

```sql
CREATE TABLE VILLAGES (
    ID                  INTEGER    NOT NULL PRIMARY KEY,
    NAME                VARCHAR(255) NOT NULL,
    DEVISE              VARCHAR(255) NOT NULL,
    FOUNDER             VARCHAR(255) NOT NULL,
    MAYOR               VARCHAR(255) NOT NULL,
    CREATIONDATE        BIGINT,
    STARTX              INT          NOT NULL,
    ENDX                INT          NOT NULL,
    STARTY              INT          NOT NULL,
    ENDY                INT          NOT NULL,
    DEEDID              BIGINT       NOT NULL,
    SURFACED            TINYINT(1)   NOT NULL DEFAULT 0,
    DEMOCRACY           TINYINT(1)   NOT NULL DEFAULT 0,
    HOMESTEAD           TINYINT(1)   NOT NULL DEFAULT 0,
    MAYPICKUP           TINYINT(1)   NOT NULL DEFAULT 0,
    TOKEN               BIGINT       NOT NULL DEFAULT -10,
    DISBAND             BIGINT       NOT NULL DEFAULT 0,
    DISBANDER           BIGINT       NOT NULL DEFAULT -10,
    LASTLOGIN           BIGINT       NOT NULL DEFAULT 0,
    KINGDOM             TINYINT      NOT NULL DEFAULT 0,
    UPKEEP              BIGINT       NOT NULL DEFAULT 0,
    ACCEPTSHOMESTEADS   TINYINT(1)   NOT NULL DEFAULT 0,
    MAXCITIZENS         INT          NOT NULL DEFAULT 0,
    PERIMETER           INT          NOT NULL DEFAULT 0,
    DISBANDED           TINYINT(1)   NOT NULL DEFAULT 0,
    PERMANENT           TINYINT(1)   NOT NULL DEFAULT 0,
    SPAWNKINGDOM        TINYINT(1)   NOT NULL DEFAULT 0,
    MERCHANTS           TINYINT(1)   NOT NULL DEFAULT 0,
    AGGROS              TINYINT(1)   NOT NULL DEFAULT 0,
    -- Twitter integration fields omitted
    FAITHWAR            FLOAT        NOT NULL DEFAULT 0,
    FAITHHEAL           FLOAT        NOT NULL DEFAULT 0,
    FAITHCREATE         FLOAT        NOT NULL DEFAULT 0,
    ALLIANCENUMBER      INT          NOT NULL DEFAULT 0,
    HOTAWINS            SMALLINT     NOT NULL DEFAULT 0,
    NAMECHANGED         BIGINT       NOT NULL DEFAULT 0,
    MOTD                VARCHAR(200) NOT NULL DEFAULT "",
    VILLAGEREP          INT          NOT NULL DEFAULT 0
);
```

### Save/Load Pattern (⚠️ UNCONVENTIONAL)

**Class:** `com.wurmonline.server.villages.DbVillage`

**❌ NO Central Save Method**

`DbVillage.save()` (line 1132) is **EMPTY:**
```java
public void save() {
    // Empty method - does nothing!
}
```

**Instead:** Individual UPDATE methods scattered throughout the class:

```java
void setMayor(String newMayor) throws IOException {
    // UPDATE VILLAGES SET MAYOR=? WHERE ID=?
}

void setDevise(String devise) throws IOException {
    // UPDATE VILLAGES SET DEVISE=? WHERE ID=?
}

void setPerimeter(int newPerimeter) throws IOException {
    // UPDATE VILLAGES SET PERIMETER=? WHERE ID=?
}

void setUpkeep(long aUpk) throws IOException {
    // UPDATE VILLAGES SET UPKEEP=? WHERE ID=?
}

// ... 20+ individual setter methods
```

**Load Method:** `Villages.loadVillages()` (static, line 606)
```java
public static final void loadVillages() throws IOException {
    Connection dbcon = DbConnector.getZonesDbCon();
    ps = dbcon.prepareStatement("SELECT * FROM VILLAGES WHERE DISBANDED=0");
    rs = ps.executeQuery();
    while (rs.next()) {
        int id = rs.getInt("ID");
        String name = rs.getString("NAME");
        String founderName = rs.getString("FOUNDER");
        // ... read all fields

        DbVillage village = new DbVillage(
            id, startx, endx, starty, endy, name, founderName, mayorName,
            deedid, surfaced, democracy, devise, creationDate, ...
        );
        // Add to villages map
    }
}
```

### Challenges

**⚠️ No single save() method to hook:**
- Would need to hook 20+ individual setter methods
- Very error-prone and maintenance-heavy
- Could miss updates if new setters are added

**⚠️ Static load method:**
- Not instance-based like structures/creatures/deities
- Would need to hook the static `Villages.loadVillages()` method
- Harder to fire per-village events

**⚠️ No create() method with auto-generated ID:**
- Village creation happens in `DbVillage.create()` (line 94)
- Returns village ID, but doesn't have clean hooks like structures

### Possible Approaches

**Option 1: Hook all individual setters (NOT RECOMMENDED)**
- Extremely fragile
- Hard to maintain
- Easy to miss updates

**Option 2: Hook loadVillages() for load events only**
- Fire `VillageDbLoadEvent` for each village loaded
- Skip save events entirely
- **Use Case:** Mods can read custom village data on startup
- **Limitation:** Can't easily save custom data (would need separate mechanism)

**Option 3: Add custom save() method via bytecode**
- Insert actual save logic into the empty `DbVillage.save()` method
- Call it from framework at appropriate times
- **Risky:** Might break if game calls save() expecting it to do nothing

**Option 4: Separate custom village data table**
- Don't use database events for villages
- Create `CUSTOM_VILLAGE_DATA` table with village ID as foreign key
- Mods manage their own village data separately
- **Cleanest approach** given village's unconventional pattern

### Implementation Priority

**Priority:** ⏸️ **LOW/DEFERRED**

**Reasons:**
- Unconventional save pattern (no central method)
- High complexity, low benefit
- Better alternatives exist (custom tables)
- Structures provide similar extensibility for buildings

**Recommendation:**
- Implement **StructureDbEvents** first (high value, low complexity)
- Defer village events until proven necessary
- If needed, use **Option 4** (separate table) instead of trying to hook scattered setters

---

## 3. Comparison Matrix

| Aspect | Structures | Villages | Creatures | Deities |
|--------|-----------|----------|-----------|---------|
| **Save Method** | ✅ Central `save()` | ❌ Empty `save()` | ✅ Central `save()` | ✅ Central `save()` |
| **Load Method** | ✅ Instance `load()` | ⚠️ Static `loadAll()` | ✅ Constructor | ✅ Constructor |
| **Hook Complexity** | 🟢 Low | 🔴 High | 🟢 Low | 🟢 Low |
| **Custom Columns** | ✅ Easy (ALTER TABLE) | ✅ Easy (ALTER TABLE) | ✅ Easy (ALTER TABLE) | ✅ Easy (ALTER TABLE) |
| **Mod Value** | 🔥 High | ⏸️ Medium | 🔥 High | 🟡 Medium |
| **Priority** | 🔥 HIGH | ⏸️ LOW | ✅ DONE | ✅ DONE |

---

## 4. Implementation Plan

### Phase 1: Structure Database Events (NEXT)

**Files to Create:**

1. **API Events:**
   - `/wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/structure/StructureDbSaveEvent.java`
   - `/wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/structure/StructureDbLoadEvent.java`

2. **Bytecode Patch:**
   - `/wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/patches/DbStructurePatch.java`

3. **Database Manager:**
   - `/wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/database/StructureDatabaseManager.java`

4. **Hook Integration:**
   - Update `ProxyServerHook.java` - Add `fireStructureDbSaveEvent()` and `fireStructureDbLoadEvent()`
   - Update `ServerHook.java` - Implement event firing
   - Update `DelegatedLauncher.java` - Register `DbStructurePatch`

**Pattern:** Follow exact same approach as `DbDeityPatch` and `DeityDatabaseManager`

**Estimated Complexity:** 🟢 Low (1-2 hours)

### Phase 2: Village Events (OPTIONAL/DEFERRED)

**Recommended Approach:** Create separate `CUSTOM_VILLAGE_DATA` table instead of database events

**Alternative:** If database events are required, use Option 2 (load-only events)

---

## 5. Use Case Examples

### Structure Events - Example Mod

```java
@SubscribeEvent
public void onStructureDbSave(StructureDbSaveEvent event) {
    // Add custom durability multiplier
    event.addCustomColumn("DURABILITY_MULT", "FLOAT DEFAULT 1.0");

    // Save custom data
    float durabilityMult = getStructureDurabilityMultiplier(event.getStructureId());
    event.setCustomData("DURABILITY_MULT", durabilityMult);
}

@SubscribeEvent
public void onStructureDbLoad(StructureDbLoadEvent event) {
    // Load custom data
    float durabilityMult = event.getFloat("DURABILITY_MULT", 1.0f);

    // Apply custom structure modifiers
    applyDurabilityMultiplier(event.getStructureId(), durabilityMult);
}
```

**Benefits:**
- Custom structure properties persist across server restarts
- No separate lookup tables needed
- Data stored alongside vanilla structure data
- Automatic cleanup when structure is deleted

---

## 6. Recommendations

### ✅ DO THIS:
1. **Implement Structure Database Events** - High value, low complexity
2. Use `DatabaseConnectionUtil` for world-folder-agnostic connections
3. Follow exact pattern from `DbDeityPatch` and `DeityDatabaseManager`
4. Test with simple mod (durability multiplier, tier system, etc.)

### ⏸️ DEFER THIS:
1. **Village Database Events** - Complex, better alternatives exist
2. If village data needed, use separate custom table with village ID foreign key
3. Consider revisiting after structures proven successful

### ❌ DON'T DO THIS:
1. Try to hook 20+ individual village setter methods
2. Modify village save() to add logic (game expects it empty)
3. Overcomplicate village persistence when custom tables work fine

---

## 7. Next Steps

**Immediate Action:** Implement Structure Database Events

**Commands:**
```bash
cd /home/garward/Scripts/Games/WurmUnlimited/WurmModLoader

# Create event classes (copy deity event pattern)
# Create bytecode patch (copy DbDeityPatch pattern)
# Create database manager (copy DeityDatabaseManager pattern)
# Register patch in DelegatedLauncher
# Build and test
./gradlew build
```

**Success Criteria:**
- Structure save fires `StructureDbSaveEvent`
- Structure load fires `StructureDbLoadEvent`
- Mods can add custom columns to STRUCTURES table
- Custom data persists across server restarts
- All tests pass

---

## 8. Technical Notes

### Database Connection
Always use `DatabaseConnectionUtil.getZonesDbConnection()` for structures and villages (both use wurmzones.db)

### Thread Safety
Structure save/load happens on server thread, but use thread-safe column tracking like other database managers (`ConcurrentHashMap.newKeySet()`)

### Performance
Structures save frequently (construction, damage, etc.) - keep custom data lightweight

### Compatibility
Test with existing mods that modify structures (BetterFarm, etc.)

---

**Research Status:** ✅ COMPLETE
**Next Action:** Implement Structure Database Events
**Estimated Time:** 1-2 hours
**Priority:** 🔥 HIGH

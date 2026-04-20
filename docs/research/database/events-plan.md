# Wurm Database Events - Comprehensive Plan

## Overview

Wurm Unlimited is fundamentally database-driven. To enable proper mod persistence, we need comprehensive database save/load events for ALL major tables.

## Why This Matters

**Current Problem:**
- Mods using the capability system store data in a separate database (wurmmodloader/capabilities.db)
- This creates:
  - **Orphaned data** when entities are deleted
  - **Extra lookups** (JOIN performance hit)
  - **Schema drift** between vanilla and mod data
  - **Inconsistent persistence** (capabilities vs. native DB)

**Solution:**
- Add custom columns directly to Wurm's native database tables
- Use the same batching/transaction system as vanilla
- Automatic cleanup when entities are deleted
- Single-table queries (no JOINs needed)

---

## Core Databases

### 1. wurmcreatures.db - CREATURES Table ✅ **IMPLEMENTED**

**Status:** Complete
**Events:** `CreatureDbSaveEvent`, `CreatureDbLoadEvent`
**Manager:** `CreatureDatabaseManager`
**Patch:** `DbCreatureStatusPatch`

**Use Cases:**
- PowerScaling: Store creature power levels (POWER_BASE, POWER_SPAWN_TIME, etc.)
- AI mods: Store AI state, aggression levels
- Taming mods: Store training progress
- Creature mods: Store custom stats

---

### 2. wurmitems.db - ITEMS Table 🔥 **CRITICAL**

**Priority:** HIGHEST (needed by MaterialSystem, SoulboundGear, UpgradeTree)

**Tables to Hook:**
- `ITEMS` - Main item data
- `ITEM_EFFECTS` - Enchantments/buffs

**Proposed Events:**
```java
public class ItemDbSaveEvent {
    private final Item item;
    private final long wurmId;
    // Custom column management like CreatureDbSaveEvent
}

public class ItemDbLoadEvent {
    private final Item item;
    private final ResultSet resultSet;
}
```

**Classes to Patch:**
- `com.wurmonline.server.items.DbItem` (save/load methods)
- Or hook `Items.createItem()` and item save batching

**Use Cases:**
- **MaterialSystem:** Store material_id, material_bonuses
- **SoulboundGear:** Store bound_player_id, bind_timestamp
- **UpgradeTree:** Store upgrade_tier, upgrade_data
- **Enchant mods:** Store custom enchantment values

---

### 3. wurmplayers.db - PLAYERS Table 🔴 **HIGH PRIORITY**

**Priority:** HIGH (player-specific data)

**Tables:**
- `PLAYERS` - Main player data
- `PLAYER_BUFFS` - Temporary buffs
- `PLAYER_EFFECTS` - Permanent effects

**Proposed Events:**
```java
public class PlayerDbSaveEvent {
    private final Player player;
    // Custom columns for player stats
}

public class PlayerDbLoadEvent {
    private final Player player;
    private final ResultSet resultSet;
}
```

**Classes to Patch:**
- `com.wurmonline.server.players.DbPlayerStatus` (or similar)

**Use Cases:**
- PowerScaling: Store player power levels (alternative to capabilities)
- Quest mods: Store quest progress
- Reputation mods: Store faction standings
- Skill mods: Store custom skill data

---

### 4. wurmzones.db - ZONES/TILES Table 🟡 **MEDIUM PRIORITY**

**Priority:** MEDIUM (for territory/zone mods)

**Tables:**
- `ZONES` - Zone data
- `TILES` - Individual tile data

**Proposed Events:**
```java
public class ZoneDbSaveEvent {
    private final int zoneId;
}

public class TileDbSaveEvent {
    private final int tileX;
    private final int tileY;
    private final boolean surface;
}
```

**Use Cases:**
- Territory mods: Store control points, ownership
- Environment mods: Store pollution, radiation
- Resource mods: Store ore veins, crop quality
- Weather mods: Store microclimates

---

### 5. wurmdeities.db - DEITIES Table ⚪ **LOW PRIORITY**

**Priority:** LOW (specialized mods only)

**Use Cases:**
- Religion mods: Custom deity stats
- Faith mods: Favor mechanics

---

## Implementation Pattern

For each database table, we need:

### 1. Events (in wurmmodloader-api)
```java
- [Table]DbSaveEvent extends Event
- [Table]DbLoadEvent extends Event
```

### 2. Bytecode Patch (in wurmmodloader-core)
```java
- Db[Table]Patch.apply()
- Hook save() and constructor/load methods
```

### 3. Database Manager (in wurmmodloader-core)
```java
- [Table]DatabaseManager.getInstance()
- handleSaveEvent()
- addColumnIfNeeded()
- saveCustomData()
```

### 4. ProxyServerHook Methods
```java
- public static void fire[Table]DbSaveEvent(...)
- public static void fire[Table]DbLoadEvent(...)
```

### 5. ServerHook Implementation
```java
- public void fire[Table]DbSave(...)
- public void fire[Table]DbLoad(...)
```

---

## Migration Strategy

### Phase 1: Core Tables (Week 1)
1. ✅ CREATURES table (complete)
2. 🔥 ITEMS table (critical for MaterialSystem, SoulboundGear)
3. 🔴 PLAYERS table (for player-specific mods)

### Phase 2: Extended Tables (Week 2)
4. ZONES/TILES table (for territory mods)
5. DEITIES table (for religion mods)

### Phase 3: Optimization
- Batch writes for custom columns
- Index creation for mod columns
- Performance profiling

---

## Benefits

### For Mod Developers:
```java
@SubscribeEvent
public void onItemDbSave(ItemDbSaveEvent event) {
    // Add column once
    event.addCustomColumn("MATERIAL_ID", "INTEGER DEFAULT 0");

    // Save data every time
    int materialId = getMaterialId(event.getItem());
    event.setCustomData("MATERIAL_ID", materialId);
}

@SubscribeEvent
public void onItemDbLoad(ItemDbLoadEvent event) {
    // Load data
    int materialId = event.getInt("MATERIAL_ID", 0);
    if (materialId > 0) {
        applyMaterialProperties(event.getItem(), materialId);
    }
}
```

### For Performance:
- **No JOINs:** All data in one table
- **Batched writes:** Uses existing Wurm batching (100ms for creatures)
- **Single query:** Load all data at once
- **Automatic cleanup:** Data deleted when entity deleted

### For Reliability:
- **No orphaned data:** Foreign key relationship implicit
- **Consistent persistence:** Same DB as vanilla
- **Transaction safety:** Uses Wurm's existing transaction logic
- **Backup compatible:** Standard SQLite backup includes mod data

---

## Example: MaterialSystem with ItemDbSaveEvent

**Before (using capabilities):**
```
wurmitems.db:
  ITEMS table (vanilla columns only)

wurmmodloader/capabilities.db:
  capability_data table:
    - entity_id: 12345
    - capability_id: "materialsystem:material"
    - data: "{material_id: 5, bonuses: {...}}"

Problems:
  ❌ If item 12345 deleted, orphaned capability data remains
  ❌ Loading item requires 2 database queries (ITEMS + capability_data)
  ❌ No foreign key constraint enforcement
```

**After (using ItemDbSaveEvent):**
```
wurmitems.db:
  ITEMS table:
    Vanilla columns:
      WURMID, NAME, TEMPLATEID, QUALITYLEVEL, etc.
    + Custom columns (added via ALTER TABLE):
      MATERIAL_ID INTEGER DEFAULT 0
      MATERIAL_DAMAGE_BONUS FLOAT DEFAULT 0.0
      MATERIAL_SPEED_BONUS FLOAT DEFAULT 0.0

Benefits:
  ✅ Item deletion automatically deletes material data
  ✅ Single query loads all item data including material
  ✅ Standard SQLite foreign key constraints work
  ✅ Existing Wurm batching handles mod columns
```

---

## Testing Strategy

### Unit Tests:
- Column addition works correctly
- Data persists across server restarts
- NULL handling for new columns
- Type conversions (int, long, float, string)

### Integration Tests:
- Multiple mods adding columns simultaneously
- High-frequency saves don't cause locks
- Server crashes don't corrupt data
- Backup/restore preserves mod data

### Performance Tests:
- Measure overhead of custom columns
- Batch write timing (should be <1ms)
- Load time comparison (capabilities vs. DB columns)

---

## Next Steps

1. **Implement ItemDbSaveEvent/LoadEvent** (CRITICAL for MaterialSystem)
2. **Implement PlayerDbSaveEvent/LoadEvent** (HIGH for PowerScaling alternative)
3. **Convert PowerScaling to use CREATURES columns** (test case)
4. **Convert MaterialSystem to use ITEMS columns** (when available)
5. **Document patterns for mod developers**

---

## API Documentation Template

For each table, create `docs/database_events/[TABLE]_EVENTS.md`:

```markdown
# [TABLE] Database Events

## Overview
[What this table stores]

## Events

### [Table]DbSaveEvent
**Fired:** When [table] is being saved to database
**Use Case:** Save custom mod data alongside vanilla data

### [Table]DbLoadEvent
**Fired:** When [table] is being loaded from database
**Use Case:** Load custom mod data when entity loads

## Examples
[Code examples for common use cases]

## Column Naming Convention
- Prefix with mod name: POWERSCALING_BASE_POWER
- Use UPPERCASE with underscores
- Include DEFAULT values in column definition

## Performance Notes
[Batching info, best practices]
```

---

## Success Metrics

✅ All major tables have save/load events
✅ Mods can add custom columns without ALTER TABLE knowledge
✅ Zero orphaned data after entity deletion
✅ Performance equal to or better than capabilities
✅ 100% test coverage for database events
✅ Complete documentation with examples

---

**Status:** CREATURES table complete, ITEMS table next priority

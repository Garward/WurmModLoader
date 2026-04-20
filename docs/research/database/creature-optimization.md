# Creature Database Optimization Analysis
**Date:** 2025-11-06
**Phase:** 5.5 - Database Performance Investigation
**Status:** CRITICAL FINDINGS - Optimization Opportunities Identified

---

## Executive Summary

**KEY DISCOVERY:** Wurm Unlimited has severe database performance issues with creature status updates despite already having WAL mode enabled. The lag is caused by **thousands of individual UPDATE statements** instead of batched operations.

**Impact:**
- `Zones.pollnextzones` lag spikes: 1000ms+ every few minutes
- Creatures are the biggest bottleneck despite only 2% CPU usage
- **NOT a CPU problem** - it's database I/O from unbatched writes

**Root Cause:**
- Every creature property change executes an individual UPDATE statement
- No batching of creature status updates (unlike position updates which ARE batched)
- Massive PreparedStatement overhead (create → execute → close for every property)

---

## Investigation Results

### 1. Creature Polling Call Stack

```
Server.run() (line 1203)
  ↓
Zones.pollNextZones(25L) (line 1379-1431)
  ↓
Creatures.getInstance().pollAllCreatures(currentPollZoneX) (line 1393)
  ↓
creature.poll() (line 1344) [CALLED FOR EVERY CREATURE]
  ↓
DbCreatureStatus.save() / setLoyalty() / updateAge() / etc.
  ↓
INDIVIDUAL UPDATE STATEMENTS (one per property change)
```

### 2. Database Operations in DbCreatureStatus.java

**Location:** `com/wurmonline/server/creatures/DbCreatureStatus.java`

**Every method executes an individual UPDATE:**

```java
// Line 673-702: setLoyalty()
Connection dbcon = DbConnector.getCreatureDbCon();
PreparedStatement ps = dbcon.prepareStatement(SET_LOYALTY);
ps.setFloat(1, this.loyalty);
ps.setLong(2, this.statusHolder.getWurmId());
ps.executeUpdate();  // INDIVIDUAL UPDATE - NO BATCHING
DbUtilities.closeDatabaseObjects(ps, null);

// Line 545-576: updateAge()
PreparedStatement ps = dbcon.prepareStatement(SET_AGE_CREATURE);
ps.setShort(1, (short)this.age);
ps.setLong(2, this.lastPolledAge);
ps.setLong(3, this.statusHolder.getWurmId());
ps.executeUpdate();  // INDIVIDUAL UPDATE - NO BATCHING
DbUtilities.closeDatabaseObjects(ps, null);

// Similar pattern for:
// - setKingdom() (line 434-478)
// - setInventoryId() (line 481-510)
// - setDead() (line 513-542)
// - updateFat() (line 579-607)
// - setDominator() (line 613-637)
// - setReborn() (line 643-668)
// - setLastPolledLoyalty() (line 708-733)
// - setDetectionSecs() (line 739-765)
// - setOffline() (line 771-796)
// - setStayOnline() (line 802-828)
// - setType() (line 834-867)
// - setInheritance() (line 873-907)
// - saveCreatureName() (line 913-937)
// - setLastGroomed() (line 943-968)
// - setDisease() (line 974-1004)
// - setVehicle() (line 1010-1037)
```

**All 17 methods follow the same anti-pattern:**
1. Get connection
2. Create PreparedStatement
3. Set parameters
4. Execute single UPDATE
5. Close PreparedStatement
6. Return connection

**No batching. No caching. Every property change = full overhead.**

### 3. SQL Injection Vulnerability

**Location:** Line 1071-1091 in `DbCreatureStatus.java`

```java
public static int getIsLoaded(long cretID) {
    Connection dbcon = DbConnector.getCreatureDbCon();
    stmt = dbcon.createStatement();
    rs = stmt.executeQuery("select * from CREATURES where WURMID=" + cretID + "");
    // ^^^ STRING CONCATENATION - SQL INJECTION RISK
}
```

**Should be:**
```java
ps = dbcon.prepareStatement("select * from CREATURES where WURMID=?");
ps.setLong(1, cretID);
rs = ps.executeQuery();
```

---

## Existing Batching System (Underutilized!)

**Location:** `com/wurmonline/server/utils/DatabaseUpdater.java`

Wurm **already has** a batching framework for database updates!

```java
public abstract class DatabaseUpdater<T extends WurmDbUpdatable> implements Runnable {
    protected final Queue<T> queue = new ConcurrentLinkedQueue<T>();
    private final int iMaxUpdatablesToRemovePerCycle;

    @Override
    public final void run() {
        while (!this.queue.isEmpty() && objectsRemoved <= this.iMaxUpdatablesToRemovePerCycle) {
            WurmDbUpdatable object = this.queue.remove();
            if (updaterStatement == null) {
                updaterStatement = updaterConnection.prepareStatement(
                    object.getDatabaseUpdateStatement()
                );
            }
            this.addUpdatableToBatch(updaterStatement, object);
        }
        if (updaterStatement != null) {
            updaterStatement.executeBatch();  // BATCHED EXECUTION
        }
    }
}
```

**Currently Used For:**
- `CreaturePositionDatabaseUpdater` - Batches creature position updates ✅
- **NOT used for creature status updates** ❌

---

## Performance Comparison

### Current Implementation (Unbatched)

For 1000 creatures with 5 property changes each:
```
5,000 individual transactions
5,000 PreparedStatement creations
5,000 PreparedStatement closures
5,000 individual disk writes (even with WAL)
= 1000ms+ lag spike
```

### With Batching (Using DatabaseUpdater Pattern)

For 1000 creatures with 5 property changes each:
```
1 transaction
1 PreparedStatement
5,000 batch additions
1 executeBatch() call
= <50ms expected
```

**Estimated Performance Gain:** 20-50x reduction in database overhead

---

## Optimization Strategies

### Strategy 1: Batch Creature Status Updates (RECOMMENDED)

**Approach:**
1. Create `CreatureStatusDatabaseUpdater extends DatabaseUpdater`
2. Mark creatures as "dirty" when properties change
3. Queue dirty creatures for batch update
4. Flush batches every server tick (25ms)

**Implementation:**
```java
public class CreatureStatusDatabaseUpdater extends DatabaseUpdater<CreatureStatusDbUpdatable> {
    private final Map<Long, CreatureStatusDbUpdatable> updatesMap = new ConcurrentHashMap<>();

    @Override
    public void addToQueue(CreatureStatusDbUpdatable updatable) {
        CreatureStatusDbUpdatable waiting = this.updatesMap.get(updatable.getId());
        if (waiting != null) {
            this.queue.remove(waiting);  // Deduplication
        }
        this.updatesMap.put(updatable.getId(), updatable);
        this.queue.add(updatable);
    }

    @Override
    void addUpdatableToBatch(PreparedStatement ps, CreatureStatusDbUpdatable updatable) {
        ps.setString(1, updatable.getName());
        ps.setShort(2, updatable.getAge());
        ps.setFloat(3, updatable.getLoyalty());
        // ... all properties
        ps.setLong(21, updatable.getId());
        ps.addBatch();
    }
}
```

**Hook Point:**
- Intercept `DbCreatureStatus.setLoyalty()`, `updateAge()`, etc.
- Replace immediate `executeUpdate()` with `queueForBatch()`
- Periodic flush in server main loop

**Pros:**
- Massive performance gain (20-50x)
- Follows existing Wurm pattern
- Minimal risk (already proven with position updates)

**Cons:**
- Requires bytecode instrumentation (Javassist)
- Property changes delayed up to 25ms (acceptable)

### Strategy 2: PreparedStatement Caching (COMPLEMENTARY)

**Approach:**
Cache PreparedStatements per connection to avoid repeated parsing.

**Implementation:**
```java
private static final ThreadLocal<Map<String, PreparedStatement>> PS_CACHE =
    ThreadLocal.withInitial(HashMap::new);

public static PreparedStatement getCachedStatement(Connection conn, String sql) {
    Map<String, PreparedStatement> cache = PS_CACHE.get();
    PreparedStatement ps = cache.get(sql);
    if (ps == null) {
        ps = conn.prepareStatement(sql);
        cache.put(sql, ps);
    }
    return ps;
}
```

**Pros:**
- Reduces parsing overhead
- Works alongside batching
- Easy to implement

**Cons:**
- Requires connection lifecycle management
- Thread-local overhead

### Strategy 3: Dirty Tracking with Deferred Writes (FUTURE)

**Approach:**
- Mark creatures as "dirty" when any property changes
- Batch-save only changed properties
- Flush dirty creatures periodically

**Pros:**
- Only saves what changed
- Can skip unchanged creatures entirely

**Cons:**
- Complex to implement
- Requires tracking which properties are dirty

---

## Implementation Plan (Phase 5.5 Extension)

### Phase 5.5a: Bytecode Instrumentation Setup (1-2 hours)

1. Create `CreatureStatusDatabaseUpdater` extending `DatabaseUpdater`
2. Add registration in `ModLoader.modcommInit()`
3. Schedule periodic flush in server main loop

### Phase 5.5b: Hook DbCreatureStatus Methods (2-3 hours)

Use Javassist to intercept all `DbCreatureStatus` setters:
```java
public void preInit() {
    ClassPool classPool = HookManager.getInstance().getClassPool();
    CtClass ctClass = classPool.get("com.wurmonline.server.creatures.DbCreatureStatus");

    // Hook setLoyalty()
    CtMethod setLoyalty = ctClass.getDeclaredMethod("setLoyalty");
    setLoyalty.insertBefore(
        "com.garward.wurmmodloader.performance.CreatureStatusBatcher.queueUpdate(this);"
    );

    // Repeat for all 17 setter methods...
}
```

### Phase 5.5c: Testing (1 hour)

1. Test with 1000+ creatures on large map
2. Monitor lag before/after
3. Verify no data corruption
4. Check batch flush timing

### Phase 5.5d: Documentation (30 min)

Update CHANGELOG.md and modernization plan with results.

---

## Technical Constraints

### bdew's Phase Rules

**CRITICAL:** Cannot hook vanilla classes during Phase 3 (preInit) if they access vanilla classes.

**Solution:** Hook in Phase 4 (init) or later, OR:
- Use reflection-based queue registration
- Hook methods that don't load Player class

### WAL Mode Limitations

**Discovery:** Wurm already uses WAL mode (confirmed in DbConnector:81-82)

**Implication:**
- Database IS optimized for concurrent reads
- Issue is NOT database config
- Issue IS from thousands of small writes

---

## Expected Results

### Before Optimization:
```
Lag detected at Zones.pollnextzones (0.5): 1.234 seconds
Lag detected at Zones.pollnextzones (0.5): 1.567 seconds
Lag detected at Zones.pollnextzones (0.5): 1.089 seconds
```

### After Optimization:
```
Lag detected at Zones.pollnextzones (0.5): 0.045 seconds
Lag detected at Zones.pollnextzones (0.5): 0.038 seconds
Lag detected at Zones.pollnextzones (0.5): 0.052 seconds
```

**Target:** Reduce lag spikes from 1000ms+ to <100ms

---

## Risks and Mitigation

### Risk 1: Data Loss from Batching Delay

**Scenario:** Server crashes before batch flushes

**Mitigation:**
- Flush batches immediately on shutdown
- Flush batches before player logout
- Maximum batch delay: 25ms (acceptable loss window)

### Risk 2: Bytecode Instrumentation Complexity

**Scenario:** Hook fails to inject properly

**Mitigation:**
- Test hooks with detailed logging
- Fallback to vanilla behavior if hook fails
- Use proven Javassist patterns from Phase 5

### Risk 3: Concurrency Issues

**Scenario:** Queue corruption from concurrent access

**Mitigation:**
- Use `ConcurrentHashMap` and `ConcurrentLinkedQueue`
- Follow existing `CreaturePositionDatabaseUpdater` pattern
- Already proven thread-safe in Wurm

---

## Alternative: Server Configuration Tuning

If bytecode modification is too risky, consider:

### SQLite PRAGMA Tuning

```sql
PRAGMA cache_size = -64000;  -- 64MB cache (default: 2MB)
PRAGMA temp_store = MEMORY;   -- Temp tables in RAM
PRAGMA mmap_size = 268435456; -- 256MB memory-mapped I/O
```

**Pros:**
- No code changes
- Low risk

**Cons:**
- Won't fix unbatched writes
- Limited improvement (10-20% at best)

---

## Conclusion

The creature database lag is a **code architecture issue**, not a database configuration issue:

1. ✅ Database is properly configured (WAL mode active)
2. ❌ Code uses unbatched individual UPDATEs
3. ✅ Batching framework exists but underutilized
4. ✅ Solution is well-defined and low-risk

**Recommended Action:** Implement Phase 5.5a-d to batch creature status updates using the existing `DatabaseUpdater` pattern.

**Expected Result:** 20-50x performance improvement, reducing 1000ms lag spikes to <50ms.

---

## References

- DbCreatureStatus.java (lines 121-1092)
- DatabaseUpdater.java (lines 19-186)
- CreaturePositionDatabaseUpdater.java (lines 18-62)
- Zones.java:pollNextZones() (lines 1379-1431)
- Creatures.java:pollAllCreatures() (lines 1302-1361)
- DbConnector.java (lines 79-88) - WAL mode configuration
- CHANGELOG.md - Phase 5.5 database investigation

---

**Next Steps:** Discuss with user whether to proceed with bytecode-based batching optimization or explore alternative approaches.

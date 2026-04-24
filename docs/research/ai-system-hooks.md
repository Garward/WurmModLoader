# Vanilla Wurm Unlimited AI Systems — Bytecode Hook Audit

System:Creatures
Hooks:AI,Combat,Movement,Targeting,Spawning
Class:com.wurmonline.server.creatures.ai.CreatureAI
Class:com.wurmonline.server.creatures.ai.PathFinder
Class:com.wurmonline.server.creatures.Creature
Class:com.wurmonline.server.zones.Zone

---

## 1. Pathing System

### Key Classes
- **CreatureAI** (`com/wurmonline/server/creatures/ai/CreatureAI.java`) — abstract base orchestrating AI tick cycle
- **PathFinder** (`com/wurmonline/server/creatures/ai/PathFinder.java`) — A-star + raycasting pathfinding; 2D only (ignores posZ in tile selection)
- **PathTile** (`com/wurmonline/server/creatures/ai/PathTile.java`) — immutable path waypoint; stores tile X/Y + optional precise float coords
- **GenericCreatureAI** (`com/wurmonline/server/creatures/ai/scripts/GenericCreatureAI.java`) — concrete impl; calls `pathedMovementTick()` and `simpleMovementTick()`

### Hook Candidates

#### Tier 1 (Obvious Must-Haves)

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `CreatureAI` | `pathedMovementTick` | `void pathedMovementTick(Creature c)` | Advances creature along computed Path; checks arrival at waypoints. Gets next PathTile from path.getFirst(), compares distance, removes tile when close enough. | **Path progression is the central movement loop.** Called every tick if path exists. Mutable: can inspect/modify path, skip waypoints, inject custom logic. | **POST** — after path step. Mutable: allow/veto step, modify next waypoint. |
| `PathFinder` | `canPass` | `boolean canPass(PathTile from, PathTile to)` | Checks if creature can move between two tiles during A-star. Tests: solid caves, lava, buildings, doors (with permissions), bridges, water depth restrictions (for submerged creatures). | **Gatekeeper for every pathfinding decision.** All movement restrictions flow through here. Stable signature. | **PRE** — veto a path segment. Returns `false` to block. |
| `CreatureAI` | `creatureMovementTick` | `void creatureMovementTick(Creature c, boolean rotateFromBlocker)` | **Core position update loop.** Calculates velocity from rotation + speed + modifiers, applies Z from `Zones.calculatePosZ()`, checks ground contact, updates `status.posX/Y/Z`, fires `moved()` callback. Called by both pathed & simple movement. | **Only place creature position actually changes each tick.** All movement goes through here. Can intercept/modify final position. | **PRE** — inspect/modify position delta before applying. **POST** — verify final position. |
| `PathFinder` | `findPath` | `Path findPath(Creature creature, int startX, int startY, int endX, int endY, boolean surface, int areaSize)` throws `NoPathException` | Entry point for pathfinding. Clamps end target to max distance (50 tiles). Tries raycasting first (surface only), falls back to A-star. Returns sorted `Path` or throws. | **Single point where all paths originate.** Pre-compute filtering (reject impossible targets), post-compute path modification (waypoint injection). | **PRE** — validate/reject path request. **POST** — modify returned path (inject detours, prune waypoints). |

#### Tier 2 (Nice-to-Have)

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `CreatureAI` | `simpleMovementTick` | `void simpleMovementTick(Creature c)` | Random-walk fallback when no path exists. 5% chance to pick random tile ±2 tiles away, call `moveTowardsTile()`. | **Opportunistic:** can suppress random walks, inject custom idle behavior. Low call rate (only when no path). | **PRE** — skip/inject alternative idle move. |
| `CreatureAI` | `getMovementTarget` | `PathTile getMovementTarget(Creature c, int tilePosX, int tilePosY)` | Wraps raw tile X/Y into a PathTile. Checks height/swim restrictions. Returns null if unreachable. | **Validation layer for manual tile targets.** Can relax restrictions (e.g. allow land creatures into water). | **POST** — transform/reject PathTile. |
| `PathFinder` | `step2` | `int step2()` [private] | Core A-star iteration. Picks lowest-cost open node, expands neighbors, detects finish. | **Deep pathing control:** can modify heuristic, alter expansion order. Risky: modifies shared `pathList`. | **PRE** — alter neighbor list or costs. |

#### Tier 3 (Speculative/Risky)

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `PathMesh` | `getAdjacent` | `PathTile[] getAdjacent(PathTile tile)` | Returns 4 orthogonal neighbors for A-star expansion. | **Can inject diagonal pathing or custom tile grids.** Requires understanding mesh layout. | **POST** — modify neighbor array. |
| `PathFinder` | `getCost` | `static float getCost(int tile)` | Returns movement cost for a tile type. Solid caves: `MAX_VALUE`; water (height < 1): `3.0f`; normal: `1.0f`. | **Biome-based cost modifier.** Can make swamps impassable, deserts slow. Static method, simple return. | **POST** — modify cost weight. |

### Known Gotchas

1. **Pathing is strictly 2D.** PathFinder works on tile X/Y only; posZ is computed *after* movement (`Zones.calculatePosZ`). No Z-aware pathfinding (can't navigate vertical structures via pathfinding; Z is applied via terrain height only).

2. **`canPass()` has two code paths** (creature vs. null creature). Null creature path is simpler (no structure/door checks). Pathing via `Creature.startPathingToTile()` always provides creature context, so null path rarely hit.

3. **Blocking check is *expensive*:** calls `Blocking.getBlockerBetween()` which evaluates structures, doors, fences on every A-star expansion. Hooking `canPass()` to bypass this is tempting but risks breaking door logic.

4. **Door opening logic only in `canPass()`:** if a creature encounters a door during pathfinding, it marks the tile as a door and returns `true` *only if* creature `canOpenDoors()`. No separate "request to open" event; door passages are silent successes.

5. **Path mutations during movement are unsafe.** If you `removeFirst()` from the path mid-tick, next call to `pathedMovementTick()` sees a shorter path. Better to hook `getFirst()` return or flag waypoints for skip.

6. **Max path distance is hard-coded to 50 tiles** in `findPath()`. Creatures can't path > 50 tiles without multiple path recalculations. Check GenericCreatureAI's `addPathToInteresting()` for re-pathing frequency.

---

## 2. Target Selection & Aggro

### Key Classes
- **Creature** (`com/wurmonline/server/creatures/Creature.java`) — central `setTarget()` method; `target` field; `getLatestAttackers()`
- **GenericCreatureAI** (`scripts/GenericCreatureAI.java`) — concrete `pollMovement()` logic that picks targets from attacker list
- **CreatureAI** (`CreatureAI.java`) — abstract `pollAttack()` (called by subclasses)

### Hook Candidates

#### Tier 1

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Creature` | `setTarget` | `void setTarget(long targ, boolean switchTarget)` | **Master target-setter.** Validates target (checks prey status, vehicle ownership, spirit guard zone limits). Updates creature `target` field, broadcasts to players, persists state. Called by AI and player actions. | **Single veto point for all target changes.** Can reject hostile targets, suppress animal hunting, enforce faction rules. | **PRE** — veto/modify target ID. Returns early if rejected. |
| `GenericCreatureAI` | `pollMovement` | `boolean pollMovement(Creature c, long delta)` | **AI target acquisition loop.** If no current path: checks if has target → path to it; else checks `getLatestAttackers()` → sorts by distance + player preference → picks first in range → sets target. Calls `addPathToInteresting()` if idle. | **Entire target selection happens here.** Can intercept attacker list, modify distance sorting, suppress target picks. | **PRE** — filter attacker list. **POST** — modify chosen target. |
| `Creature` | `getLatestAttackers` | `long[] getLatestAttackers()` | Returns array of creature IDs that have hit this creature recently. Stored in `attackers` HashMap with timestamps. | **Attacker list is the basis for counter-aggro.** Can filter/inject attackers. | **POST** — modify returned array. |

#### Tier 2

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Creature` | `getMaxHuntDistance` | `int getMaxHuntDistance()` [inferred from code] | Returns max distance creature will pursue a target (checked in `pollMovement` when iterating attackers). | **Range limiter for aggro.** Can make creatures more/less aggressive. Likely returns template property. | **POST** — modify distance. |
| `CreatureAI` | `pollAttack` | `boolean pollAttack(Creature c, long delta)` [abstract] | Abstract method subclasses implement. Called every tick if not fighting & no special pre-attack block. | **Attack decision point.** Subclasses (MobBehaviour, etc.) decide here whether to swing/cast. | **POST** — inspect combat decision. |
| `Creature` | `maybeAttackCreature` | `boolean maybeAttackCreature(Creature c, VirtualZone vz, Creature mover)` | Hook in CreatureAI; returns false by default. Fired when a creature moves into same tile. | **Opportunity attack trigger.** Can suppress/inject attacks on entry. | **PRE** — veto attack. |

#### Tier 3

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Creature` | `trimAttackers` | `void trimAttackers(boolean...)` | Removes stale entries from `attackers` HashMap (timeout-based). Called in `pollMisc()`. | **Cleanup for attacker tracking.** Can extend timeout, prevent forgetting. | **PRE** — modify cleanup logic. |
| `GenericCreatureAI` | `addPathToInteresting` | `protected abstract boolean addPathToInteresting(Creature c, long delta)` | Abstract; subclasses implement idle path selection (e.g., wander to food, lair). | **Subclass-specific.** Concrete impl varies (HuntingAI seeks prey; DenAI nests). | **POST** — modify path destination. |

### Known Gotchas

1. **`getLatestAttackers()` returns a *snapshot array*; mutating it doesn't affect the creature.** Changes to returned array are lost. Need to hook the sorting/filtering inside `pollMovement()` instead.

2. **Target switching is gated by combat rating check:** `if (getBaseCombatRating() > 10.0f || fleeCounter <= 0)`. Low-CR creatures (fresh spawns, animals) rarely switch targets mid-fight. Hooking `setTarget()` alone won't force aggressive switching.

3. **Spirit guards have zone restrictions:** `setTarget()` explicitly checks if target is outside village perimeter ±5 tiles, zeroes the target, and sends a lore message. Kingdom guards (non-spirit) have no such restriction. Overriding this requires patching the zone check, not just the target setter.

4. **Prey creatures are immune to targeting:** first line of `setTarget()` is `if (isPrey()) return;`. Hooking won't override this without also patching `isPrey()` check.

5. **Player preference modifier is in the `Comparator` inside `pollMovement()`:** if `doesPreferPlayers()`, player attackers get distance multiplied by `getPrefersPlayersModifier()` (usually < 1.0, making them appear closer). This is *local* to GenericCreatureAI; other AI subclasses don't implement player preference.

---

## 3. Restrictions (What AI Can/Can't Do)

### Key Classes
- **CreatureAI.creatureMovementTick()** — implicit restrictions via water depth, guarded zones, lava
- **Zone.spawnCreature()** (`com/wurmonline/server/zones/Zone.java`) — spawn restrictions (steepness, water, biome)
- **PathFinder.canPass()** — movement restrictions during pathfinding

### Hook Candidates

#### Tier 1

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `CreatureAI` | `creatureMovementTick` | [see Pathing] | Implicitly enforces: guarded zones repel non-guard creatures (`!t.isGuarded()`), lava blocks non-fliers, submerged creatures can't enter shallow water. | **Easiest place to override terrain restrictions.** Can suppress guard zone repel, allow lava entry, enforce custom biome rules. | **PRE** — skip guard/lava checks. |
| `PathFinder` | `canPass` | [see Pathing] | Returns `false` for solid caves, lava (for non-ghosts), deep water (for non-submerged). Door blocks unless `canOpenDoors()`. | **Gatekeeper for terrain passability.** Can relax water/cave restrictions, enforce custom terrain rules. | **POST** — override return value. |
| `Zone` | `spawnCreature` | `void spawnCreature(int tx, int ty, boolean _spawnKingdom)` [line 620] | **Spawn placement validation.** Checks: steepness >= 40 → reject; mine doors/holes → reject; kingdom spawn type (guard vs. wild creature); den-based spawning. | **Controls which biomes accept creature spawns.** Can suppress steepness check, relax biome rules, force spawn on mountains. | **PRE** — skip validation checks. |

#### Tier 2

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Creature` | `isSwimming` | `boolean isSwimming()` [inferred] | Property checked in `creatureMovementTick()` line 163, 236. If true, water-blocked tiles become passable. | **Swim permission toggle.** Can force swimming on land creatures, suppress water penalty. | **POST** — override return. |
| `Creature` | `isSubmerged` | `boolean isSubmerged()` [inferred] | Property checked in pathfinding (line 420: submerged creatures reject height > -20). | **Deep water permission.** Can suppress submerged check. | **POST** — override return. |
| `Zone` | `maySpawnCreatureTemplate` | `static boolean maySpawnCreatureTemplate(CreatureTemplate ctemplate, boolean isDen, boolean _spawnKingdom)` | Validates whether a template is allowed to spawn. Likely checks creature type (animals only in dens, guards only kingdom-assigned). | **Template-level spawn filter.** Can suppress animal/guard restrictions. | **POST** — override return. |

#### Tier 3

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Creature` | `canOpenDoors` | `boolean canOpenDoors()` | Checked in PathFinder.canPass() when encountering a door. Animals/pets typically can't. | **Door access control.** Can allow animals to open doors (risky for world integrity). | **POST** — override return. |
| `Creature` | `getTileSteepness` | `static short[] getTileSteepness(int tx, int ty, boolean onSurface)` | Returns steepness values. Checked in spawn (>= 40 → reject). | **Steepness threshold.** Can relax mountain spawn rules. | **POST** — modify steepness array. |
| `Blocking` | `getBlockerBetween` | [external, not in AI file] | Returns first structure/blocker in a line segment. Called from `canPass()`. | **Deep structure checks.** Can suppress specific structure blocks (risky). | **POST** — filter blockers. |

### Known Gotchas

1. **Water restrictions are fragmented:**
   - Pathfinding (PathFinder): `isSubmerged()` check rejects height > -20
   - Movement (creatureMovementTick): shallow water penalty if lPosZ < -0.7 and not submerged
   - Both checks are independent; patching one doesn't override the other

2. **Guarded zone check is in `creatureMovementTick()`, not pathfinding.** Creature *can* path to guarded tiles but *fails to move* into them mid-tick. Pathing doesn't know about guard zones.

3. **Steepness check in spawning is hardcoded >= 40.** No setter; value is in-place calculated from height deltas.

4. **Lava blocking is special-cased for animals:** `c.isAnimal() && t.hasFire()` in line 137. Creature type matters, not just template.

5. **Bridge logic in `canPass()` is convoluted:** if on a bridge, creature only moves perpendicular to bridge orientation. This is *not* obvious from reading the code path.

---

## 4. Spawn Positioning

### Key Classes
- **Zone.spawnCreature()** (`com/wurmonline/server/zones/Zone.java` lines 620–850) — main spawn placement logic
- **Creature.doNew()** (`Creature.java`) — factory; calls template + creates instance
- **Creatures** (`com/wurmonline/server/creatures/Creatures.java`) — singleton registry

### Hook Candidates

#### Tier 1

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Zone` | `spawnCreature` | `void spawnCreature(int tx, int ty, boolean _spawnKingdom)` | **Master spawn placement.** Given tile X/Y, validates biome, checks steepness, selects creature template (guard vs. wild vs. den), calls `Creature.doNew()`. | **Single point for all land creature spawning.** Can suppress validation, inject creature templates, modify spawn location. | **PRE** — veto spawn. **POST** — inject alternative template. |
| `Creature` | `doNew` | `static Creature doNew(int templateId, float posX, float posY, float rotation, int layer, String name, byte sex)` [inferred signature] | Factory method. Creates creature instance, initializes position, registers in world. | **Spawn instantiation.** Can wrap to log spawns, enforce faction-based spawning. | **POST** — inspect/modify created creature. |

#### Tier 2

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Zone` | `spawnSeaCreature` | `void spawnSeaCreature(boolean spawnSeaHunter)` [line 490] | Spawn sea creatures (huntsmiths, kraken). Picks random water tile, creates creature. | **Water spawn control.** Can suppress sea creature spawns, enforce ocean zones. | **PRE** — veto spawn. |
| `Zone` | [poll/tick] | `void poll(...)` [inferred] | Main tick that decides when to spawn. Calls `spawnCreature()` or `spawnSeaCreature()` when density allows. | **Spawn frequency gate.** Can suppress spawns entirely, throttle to custom rate. | **PRE** — skip spawn decision. |
| `Creatures` | [constructor/respawn] | [depends on Npc vs. Player logic] | Creature resurrection post-death. Likely in death handler or respawn timer. | **Respawn control.** Can suppress respawn, relocate spawn. | **PRE** — veto/modify respawn. |

#### Tier 3

| Class | Method | Signature | What It Does | Why Hook | Event Type |
|-------|--------|-----------|-------------|----------|-----------|
| `Offspring` | [breeding] | [separate breeding system] | Offspring creation post-breeding. Picks biome-based location. | **Breeding spawn.** Can force offspring into specific zones. | **POST** — modify spawn location. |
| `Den` | [spawning] | [Den-based respawn] | Den creatures respawn near den after death. | **Den control.** Can suppress den respawn. | **PRE** — veto respawn. |

### Known Gotchas

1. **Spawn tile selection is randomized; validation is *post-hoc*.** Zone picks random tile from current zone, then checks if valid. If steepness >= 40, tries again (no loop limit visible; infinite retry risk if zone is all mountains).

2. **Kingdom spawn has fallback logic:** if no kingdom found at exact tile, checks ±50 tiles in cardinal + diagonal directions (8 checks). If still none, sets `_spawnKingdom = false` and spawns wild creature instead.

3. **Creature elevation is inferred from surrounding tiles:** code scans 2×2 tile box to determine if spawning in lava, under trees, on rock. This is *approximate* and can fail on tile boundaries.

4. **Sea creatures are spawned in separate method, not through Zone.spawnCreature().** Patching one doesn't control the other. Check if `spawnSeaHunter` flag for sea hunter type.

5. **Template selection is hardcoded by creature type:** guards → template 37/39/40 (by kingdom); wild → den template or random spawn item template; sea → separate hardcoded list. No dynamic template factory; changes require patching select statements.

6. **Position offset is always `(tx << 2) + 2.0f, (ty << 2) + 2.0f`.** Creature spawns at center of tile (tile X*4 + 2). No randomization within the 4×4 tile grid; creatures always spawn at precise tile center.

---

## Cross-Cutting Observations

### Central Tick Entry Point
**All four systems flow through `CreatureAI.pollCreature()`** (line 248):
```
pollCreature(Creature c, long delta) {
  → pollSpecialPreAttack()
  → pollAttack()
  → [check actions]
  → pollSpecialPreBreeding()
  → pollBreeding()
  → pollSpecialPreMovement()
  → pollMovement()  ← pathing + target selection
    → pathedMovementTick() or simpleMovementTick()
    → creatureMovementTick()  ← position update
}
```

Hooking this method with early-return capabilities would give broad control; however, the method is ~150 lines and calls many overrideable hooks, so granular hooks are safer.

### 2D Pathing Is Fundamental
**All pathing is 2D (X/Y only); Z is always post-hoc.** This means:
- Creatures cannot path *around* tall buildings (pathfinding sees through them)
- Creatures cannot navigate vertical multi-floor structures via pathfinding (only via manual Z movement)
- Possible future hook: inject Z-aware pathfinding wrapper, or suppress guarded zone repel and let creatures brute-force through

### Attacker List Is the Behavior Driver
**`getLatestAttackers()` array drives all counter-aggro.** Modifying this array (or the sorting in `pollMovement()`) controls whether creatures chase that pesky player or turn on teammates. Mods like DUSKombat likely patch this.

### Spawn Validation is Local to Zone
**Spawning is per-zone; no global spawn gatekeeper.** Each zone independently decides spawn density, template, location. To enforce global spawn rules (e.g., "no animals in caves"), must patch `Zone.spawnCreature()` or `Zone.maySpawnCreatureTemplate()`.

### Blocking Check is Expensive
**PathFinder.canPass() calls `Blocking.getBlockerBetween()` on *every A-star expansion*.** For large paths, this is O(n * m) where n = path length, m = structure list size. Optimizing pathing requires either:
- Caching blocker results (complex, stale-data risk)
- Hooking `canPass()` to short-circuit checks for low-priority creatures
- Suppressing structure checks entirely (risky for integrity)

---

## Summary of Highest-Value Hooks Per System

### Pathing
- **Tier 1:** `CreatureAI.pathedMovementTick()` (path progression), `PathFinder.canPass()` (terrain access), `CreatureAI.creatureMovementTick()` (position update)
- **One-line hook:** Veto movement into specific tiles via `canPass()` return value

### Targeting
- **Tier 1:** `Creature.setTarget()` (target veto), `GenericCreatureAI.pollMovement()` (attacker selection & sorting)
- **One-line hook:** Reject all hostile targets via early return in `setTarget()` (pacifism mode)

### Restrictions
- **Tier 1:** `CreatureAI.creatureMovementTick()` (implicit restrictions), `PathFinder.canPass()` (terrain restrictions), `Zone.spawnCreature()` (spawn validation)
- **One-line hook:** Suppress guard zone repel by skipping the `t.isGuarded()` check

### Spawn
- **Tier 1:** `Zone.spawnCreature()` (placement + validation)
- **One-line hook:** Force spawn on mountains by suppressing steepness >= 40 check

---

## Architecture Surprises

1. **No creature AI "decision tree".** AI is just a tick loop calling abstract methods. Subclasses (GenericCreatureAI, FishAI, etc.) implement the actual logic. This is modular but scattered.

2. **Pathfinding is synchronous, not async.** `findPath()` runs A-star to completion on the calling thread (max 10,000 iterations). Large paths can stall the server. No visible async wrapper or priority queue.

3. **Target selection is *reactive, not proactive*.** Creatures only pick targets from existing `getLatestAttackers()` array or direct orders. No "scan nearby creatures for enemies" phase. Implies low CPU cost but passive aggression.

4. **Spawn tiles are validated *by biome, not by creature properties*.** Water creatures can spawn on land if the Zone biome allows it. Template swimming flag isn't checked during spawn validation, only during movement. Risk of water creatures spawning on mountains.

5. **Bridge logic is implicit and fragile.** Creatures track bridge ID; when moving between bridge tiles, special logic prevents moving perpendicular to bridge orientation. No explicit bridge-following pathfinding; bridges are treated as terrain with orientation constraints.

6. **Spirit guards have hard-coded zone restrictions.** Can't be generalized to other faction guards without duplicate logic. Vanilla hardcodes village perimeter ±5 checks only for spirit guards.

---

## Recommended Hook Implementation Sequence

1. Start with `PathFinder.canPass()` — lowest-impact, highest-value (control all movement restrictions)
2. Add `Creature.setTarget()` — gate hostile targets (pacifism, faction rules)
3. Add `CreatureAI.creatureMovementTick()` for position inspection (logging, mod action integration)
4. Add `Zone.spawnCreature()` for spawn validation (biome rules, population control)
5. Optional: `GenericCreatureAI.pollMovement()` for attacker list filtering (complex, AI-subclass-specific)

---

## Confidence Levels

- **Tier 1 hooks:** 95% confident — code is public, tested, stable signatures
- **Tier 2 hooks:** 80% confident — rely on decompiler accuracy, signature inference
- **Tier 3 hooks:** 60% confident — require diving into private methods, risky for breakage

All hooks assume Javassist-style `insertAfter()` / `insertBefore()` patching with bytecode generation. Test each hook in isolation on a non-production server.


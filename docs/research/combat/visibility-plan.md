# Mod Internals Visibility Plan
*Goal: Make it provably clear that mods are doing what they're supposed to do at runtime.*

> **Reframe (2026-04, post-crossref):** This document was written assuming the framework's job is to let mods *peek into* vanilla combat. The real goal is bigger — the framework's job is to make vanilla's horrible combat code **invisible**. If a mod needs a bytecode patch, that is a framework bug: the event doesn't exist yet and needs to be added. DUSKombat reimplements the whole formula only because the event surface isn't complete; Armoury papers over gaps with query events for the same reason.
>
> The tiers below are still valid as *tactical steps for diagnostics*, but they are no longer the strategic plan. See:
> - **[`mod-crossref.md`](mod-crossref.md)** — what Armoury and DUSKombat actually do, and why
> - **[`event-surface-gap.md`](event-surface-gap.md)** — the events the framework needs so no future mod has to reimplement combat

---

## Current State

### What exists already
- **DiagnosticServer** — HTTP server exposing patch registry, event registry, mod list as JSON endpoints
- **EventSimulator** — Can fire synthetic events to test handlers in isolation (some stubs marked "not yet implemented")
- **ConsoleGMCommandRouter + GMCommandDiscovery** — In-game GM command system with annotation-based discovery
- **ServerHook DEBUG mode** — Rate-limited event counters that log "fired N times in 30s" per event type
- **ProxyServerHook.trackDamage** — Tracks damage dealt per attacker for loot system

### The Gap: CombatDamageEvent is a black box

The bytecode hook fires **after** the full damage formula runs. We receive a single `double damage` value — but the formula that produced it involves:

```
rawWeaponDamage
  → strength modifier
  → weapon quality modifier
  → skill modifier (attacker)
  → parry check (defender shield skill)
  → attack rating vs defense rating
  → armourMod (material + quality)
  → wound type multiplier
  → position multiplier
  → AI causedWound/receivedWound hooks
  → minimum damage floor check
  = final damage
```

None of these intermediate values are visible to mods currently. We can intercept the final number and change it, but we can't tell *why* it is what it is.

---

## Plan: Three Tiers of Visibility

### Tier 1 — Damage Breakdown Logging (No formula changes needed)

**What:** Hook `addWound()` at entry *and* at the wound application point. Log the delta.

The `addWound` signature gives us:
```java
addWound(Creature performer, Creature defender, byte type, int pos,
         double damage, float armourMod, String attString, ...)
```

`damage` here is already post-formula. But `damage * armourMod` = effective damage before armour reduction.
We can log both and derive the armour reduction in real terms.

**Implementation:** Add a second hook point — `WoundApplicationEvent` — fired *inside* `addWound` before the wound object is created. Expose:
- `rawDamage` (the `damage` param — post formula, pre armour)
- `armourMod` (float — lower = more protection)
- `effectiveDamage` = rawDamage * armourMod
- wound type, body position, attacker string

This doesn't crack the formula but gives you pre-armour vs post-armour split which is the most actionable number for mod authors.

**Files to touch:**
- `CombatDamagePatch.java` — add second injection point at `addWound()` entry
- New `WoundApplicationEvent.java` in API events/combat/
- `ServerHook.java` — add `fireWoundApplicationEvent()`

---

### Tier 2 — GM Debug Commands (In-game real numbers)

**What:** Use the existing `ConsoleGMCommandRouter` to add a `#debug` command suite.

Already have the annotation discovery system. Just need to register commands.

**Commands to add:**

```
#debug combat on|off   — enable per-hit damage breakdown messages sent to the GM
#debug stats <target>  — dump creature's full stat block (str, agi, skills, equipped gear QL)
#debug event list      — list all registered event handlers (uses DiagnosticServer data)
#debug event fire <EventName>  — manually fire a synthetic event (wraps EventSimulator)
#debug patch list      — list all active bytecode patches with priority and conflict keys
#debug mod list        — list loaded mods with versions
```

When `#debug combat on` is active, every `CombatDamageEvent` sends the GM a private colored message:
```
[Combat] Goblin → You | raw: 12.4 | armour: 0.82 | effective: 10.2 | type: SLASH | pos: CHEST
```

**Files to touch:**
- New `DebugCommandMod.java` in core/console/commands/
- `GMCommandDiscovery.java` — register the new command class (or it auto-discovers via annotation)

---

### Tier 3 — Formula Transparency API (The hard one, addresses the blocker)

**What:** Expose the intermediate damage calculation values as a structured object mods can query.

The formula is buried in a 1100+ line monolith in `CombatEngine.java`. The approach that doesn't require rewriting it:

**Option A: Wrap with a thread-local accumulator**

Before the attack sequence starts, install a `DamageCalculationContext` into a thread-local. Each formula stage that we've patched writes its intermediate value in. After the formula runs, fire a `CombatDamageBreakdownEvent` with the full context.

```java
public class DamageCalculationContext {
    public double weaponBaseDamage;
    public double strengthMultiplier;
    public double skillModifier;
    public double attackRating;
    public double defenseRating;
    public boolean parryOccurred;
    public double preArmourDamage;
    public float armourMod;
    public double finalDamage;
}
```

Stages we can intercept from decompiled source:
- Line 670: `CombatEngine.getWeaponDamage(attWeapon, attStrengthSkill)` — weapon base
- Line 644: `getDamagePercent()` / crit check — critical flag
- The parry block (involves shield skill check) — parry result
- armourMod passed into `addWound` — armour reduction

**Option B: Empirical logging pass first (recommended)**

Before building the full API, add a `DEBUG_FORMULA=true` flag that logs every intermediate value we *can* see to a file during combat. After 20-30 hits you'll have enough data to:
- Verify the formula is consistent
- Identify which branches matter (archery vs melee vs spell vs creature AI overrides)
- Build the context object around real observations rather than assumptions

**Recommendation: Do Option B first.** Fast to implement, and will tell you whether the formula has too many branching paths (archery/melee/spell/creature AI) to make a single clean API worth building, or if it's consistent enough to wrap.

---

## Quick Win Right Now (5 minutes)

The `ServerHook.DEBUG` flag already exists. Add one line to `fireCombatDamage()` in `ServerHook.java`:

```java
if (DEBUG) {
    logger.info(String.format("[Combat] %s → %s | damage=%.2f | type=%d | pos=%d",
        attacker != null ? attacker.getName() : "?",
        defender != null ? defender.getName() : "?",
        damage, woundType, bodyPart));
}
```

This is zero architecture and immediately gives you something to look at in the logs.

---

## Priority Order

| # | Task | Effort | Value |
|---|------|--------|-------|
| 1 | Add per-hit debug log line to `fireCombatDamage` | 5 min | Immediate combat visibility |
| 2 | `WoundApplicationEvent` with raw vs effective damage | ~2h | Pre/post armour split |
| 3 | `#debug combat on` GM command | ~3h | In-game real-time readout |
| 4 | `#debug stats` GM command | ~1h | Verify stat reads |
| 5 | Formula logging pass (Option B) | ~4h | Foundation for Tier 3 |
| 6 | `DamageCalculationContext` API (Tier 3) | ~1-2 days | Full formula transparency |

Items 1-4 can all be done without touching the damage formula at all.

---

## Relevant Files

### ModLoader
- `wurmmodloader-core/.../modloader/server/ServerHook.java` — `fireCombatDamage()` at line 427
- `wurmmodloader-core/.../modloader/server/ProxyServerHook.java` — static entry points
- `wurmmodloader-core/.../bytecode/patches/CombatDamagePatch.java` — bytecode injection point
- `wurmmodloader-core/.../debug/DiagnosticServer.java` — existing HTTP debug endpoints
- `wurmmodloader-core/.../console/ConsoleGMCommandRouter.java` — GM command routing
- `wurmmodloader-core/.../console/GMCommandDiscovery.java` — annotation-based discovery
- `wurmmodloader-core/.../testing/eventsim/EventSimulator.java` — synthetic event firing

### Vanilla Source (pre-decompiled)
Path: `<decompiled-wurm-source>/` (a local checkout of the decompiled Wurm sources)
- `server_decompiled/com/wurmonline/server/combat/CombatEngine.java`
  - Lines ~600-920: full attack sequence (where damage is calculated)
  - Lines ~918-1120: `addWound()` (where armourMod is applied and wound is created)
- `server_decompiled/com/wurmonline/server/combat/CombatMove.java` — move type definitions

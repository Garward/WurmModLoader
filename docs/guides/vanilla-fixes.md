# Vanilla Bug Fixes

The framework bundles a small set of patches that fix known defects in vanilla Wurm
Unlimited. These run out of the box and require no mod to enable.

## Inclusion criteria

A patch qualifies as a "vanilla fix" only if it meets **all** of:

1. **Fixes a real defect** — crash, exception spam, or data corruption. A stack trace or
   reproducer is required; "this looks wrong" is not.
2. **Strictly non-opinionated** — behavior change is limited to "don't crash" or "don't
   produce nonsense output". No balance, pacing, or feel changes.
3. **No judgment calls** — if there's a design decision about what *should* happen, the
   patch belongs in a mod, not here.
4. **Backwards-compatible** — a correctly-behaving call site sees no functional change.

## Configuration

Config lives at `<server-root>/config/wurmmodloader-vanilla-fixes.properties` (alongside
vanilla's `logging.properties`). `deploy.sh` seeds a commented template on first run
— non-destructive, never overwrites your edits. Missing file = all defaults.

| Key | Default | Effect |
|---|---|---|
| `enabled` | `true` | Master toggle — disables all fixes when `false` |
| `<fix_key>` | `true` | Per-fix override (see table below) |
| `action_timer.min_spell` | `2` | Minimum spell casting seconds after action-timer scaling |
| `action_timer.min_pick` | `0` | Minimum pick-action seconds (0 = disabled) |
| `action_timer.min_breed` | `0` | Minimum breed-action seconds (0 = disabled) |

Every key can also be set as a system property (prefix `wurmmodloader.vanilla_fixes.`)
for runtime diagnostics — system properties win over the file.

Example (disable just the nextInt guards for one run):

```bash
./wurmmodloader.sh start=Riverweave -Dwurmmodloader.vanilla_fixes.nextint_guards=false
```

## Current fixes

### `nextint_guards` — unguarded `Random.nextInt(computed)` crashes

**Defect:** several vanilla call sites pass a computed arithmetic expression to
`Random.nextInt(int)` with no floor guard. When the expression evaluates to `0` or
negative (degenerate HOTA zone, village with inverted dimensions, zero-bonus weapon,
etc.), `nextInt` throws `IllegalArgumentException: bound must be positive`. The most
visible symptom is continuous log spam from the creature poll loop:

```
java.lang.IllegalArgumentException: bound must be positive
        at java.util.Random.nextInt(Random.java:388)
        at com.wurmonline.server.creatures.Npc.getMoveTarget(Npc.java:742)
        at com.wurmonline.server.creatures.Creature.startPathing(Creature.java:11239)
        ...
```

**Fix:** every `Random.nextInt(int)` call in the affected methods is wrapped with
`Math.max(1, arg)`. A correct call (`arg >= 1`) is unaffected — the max is a no-op. A
degenerate call becomes `nextInt(1)` which returns `0`, matching the graceful "no
random offset this tick" path that every call site already handles.

**Patched call sites:**

| Class | Method | Symptom |
|---|---|---|
| `Npc` | `getMoveTarget` | HOTA wander, NPC pathing spam |
| `LoginHandler` | `getStartTileForDeed` | Login crash on enemy-deed respawn with malformed village |
| `CombatEngine` | `performAttack` | Parry timing / enchanted-weapon spell damage |
| `CombatEngine` | `addWound` | Wound infection tick |
| `Archery` | `hit` | Poison-arrow damage computation |

### `action_timer` — actions that ignore the server timer multiplier

**Defect:** many vanilla actions hardcode their tick cadence and ignore
`ServerEntry.getActionTimer()`, so servers running `actionTimer > 1.0` get most actions
scaled correctly but a specific set (prayer, meditation, alchemy, improving, etc.) still
run at the vanilla 1.0× rate. This is a well-known community bug — bdew's GPL `timerfix`
mod has been the de facto patch for years.

**Fix:** the bdew TimerFix bytecode surgery is ported into the framework (minus its
opinionated spell blacklist; per-spell overrides belong in a mod). Every affected action
divides its hardcoded duration by `getActionTimer()` so fast-timer servers see uniform
scaling. Three minimum caps are exposed as config (`action_timer.min_spell`,
`action_timer.min_pick`, `action_timer.min_breed`) to prevent degenerate
"instant spell" / "instant breed" cases on high-multiplier servers.

**Patched actions:**

| Area | Class / method |
|---|---|
| Flatten | `Flattening.flatten` (tick cadence + send-action-control) |
| Spells | `Spell.getCastingTime` |
| Destroy | `MethodsStructure.destroyWall` / `destroyFence` / `destroyFloor`, `MethodsItems.destroyItem` |
| Prayer | `MethodsReligion.pray` (both overloads) |
| Sacrifice | `MethodsReligion.sacrifice` |
| Sow | `TileDirtBehaviour.action` |
| Meditate | `Cults.meditate` |
| Alchemy | `MethodsItems.smear` / `createOil` / `createSalve` |
| Forage | `TileBehaviour.forage` |
| Breed | `MethodsCreatures.breed` |
| Coloring / stringing | `MethodsStructure.colorWall` / `colorFence` / `removeColor`, `MethodsItems.colorItem` / `improveColor` / `removeColor` / `string` / `stringRod` / `unstringBow` |
| Picking (cap only) | `Actions.getPickActionTime` (when `min_pick > 0`) |

The improve-action path (`Actions.getImproveActionTime`) is **not** patched — the
`Actions` class is frozen early by action-registration patches, and the improve timer
is already routed through the framework's own `ActionTimeEvent`. Mods that want to
tweak improve timing should subscribe to that event.

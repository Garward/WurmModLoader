# Kingdom mechanics — what vanilla WU ships vs. what sits dormant

Research dump of the kingdom / NPC-hostility / siege / PvP surface in vanilla
Wurm Unlimited, focused on separating "live and running" from "fully coded
but silenced by a flag." Feeds the decision of what to surface via the
framework.

All paths rooted at
`~/Scripts/Games/WurmUnlimited/PowerFantasy/Wurmguide/decompiled/server_decompiled/`.

---

## 1. Kingdom model

- `com/wurmonline/server/kingdom/Kingdom.java` — state holder per kingdom.
- `Kingdoms.java` — registry + alliance map.
- `King.java` — per-kingdom king + era tracking.

**Kingdom IDs:** 0 neutral, 1 Jenn-Kellon, 2 Mol-Rehan, 3 HOTS, 4 Freedom, 5+
PMK. `isCustomKingdom()` = `id < 0 || id > 4`.

Each `Kingdom` carries: `template` (which vanilla it models), members list
(`PLAYERS WHERE KINGDOM=?`), alliance map (`Map<Byte,Byte>`: 0/1/2 =
none/alliance/pending), hash-derived RGB color, `acceptsTransfers` flag.

`setExistsHere(true)` flips `Servers.loginServer.shouldResendKingdoms`.
`disband()` cascades: destroys towers, converts members to kingdom 0,
cleans alliances.

## 2. Player-made kingdoms (PMK)

`questions/KingdomFoundationQuestion.java` drives creation. Prerequisites:

- Player is a settlement mayor, standing on it, not champion.
- ≥ 1 premium in deed+perimeter, none fighting/moving.
- Kingdom count < 255 (`Kingdoms.mayCreateKingdom`).
- "Declaration" item in inventory, untradedly owned.
- Name 2–20 chars, password ≥ 5 chars, template choice Jenn/Molr/HOTS.

On accept: mass-converts everyone in deed+perimeter (plus guards and loyal
creatures) to the new kingdom. Writes one row to `KINGDOMS`.

King election + succession is real and runs on a timer (`King.poll`):
10 challenges needed to hold/become king (3 on test), era rows written to
`KING_ERA`, appointments in `APPOINTMENTS` + `OFFICES` (9 titles + 6 orders
+ 11 named officials). Weekly reset if `lastChecked > 7d` and an inactive
PMK king auto-purges after 30d.

**Practically silent on HOMESERVER** — no kingdom boundaries to contest, no
challenges to issue.

## 3. NPC town aggression & guard towers

`kingdom/GuardTower.java` — tower is an item with `auxData=kingdom`, owner =
founder. Guards stored in `TOWERGUARDS`; max 4 per tower. `sendAttackWarning`
broadcasts to king's "Ambassador" official (ID 1502) every 3 min.

`villages/Village.java:415-450` `isEnemy(Creature)` cascade:

1. Invulnerable / unique → no.
2. Different kingdom AND not allied → **ENEMY**.
3. Dominator chain check — dominator's village enmity wins.
4. Reputation ≤ −30 in this village AND within min perimeter → **ENEMY**.
5. Aggressive creature from non-friendly kingdom → enemy unless village
   `allowsAggCreatures()`.
6. Valrei creature + aggressive → enemy.

Enmity triggers `addTarget`: alarm, gates lock, horn plays, up to 3 guards
assigned per target.

**On HOMESERVER this entire pipeline short-circuits.** `Zones.getKingdom`
(`zones/Zones.java:258-300`) hardcodes every tile to `Servers.localServer.KINGDOM`
when `HOMESERVER=true`, so no creature ever shows as belonging to a hostile
kingdom in the first place. All the guard/tower/aggression code is *wired
and tested* — it just has nothing to attack.

## 4. Siege mechanics

`behaviours/WarmachineBehaviour.java` — catapults, trebuchets, siege
shields, battering ram components (templates 931 / 937 / 1125 plus
variants).

Real mechanics:

- Requires 21+ Strength, flat ground, 5-tile clear radius.
- Winch actions 237/238/239 — aim/elevation. Fire = 235/236.
- Projectiles are `ServerProjectile` with full ballistic arcs.
- Damage model exists; projectiles land and apply impact.

**What's NOT there:**

- No door/gate destruction — doors are permission-gated, not damageable by
  rams. The battering ram *item* exists, but there's no hook making it
  actually damage a structure.
- No deed takeover / deed-stealing — village ownership only transfers by
  `Village.disband` or a sanctioned mayor-change. No "conquest" path.
- No village capture.

So siege gear exists, fires projectiles, kills players and creatures — but
can't meaningfully take anything. That's the WO→WU amputation.

## 5. War / warfare state machine

`villages/WarDeclaration.java` + `VillageWar.java` — table
`VILLAGEWARDECLARATIONS`. Lifecycle: declarer→receiver row, `accept()`
→ `Villages.createWar`, `dissolve()` emits broadcast + cleans.

`mayDeclareWarOn`:

- Same kingdom only.
- Target not permanent.
- Reputation ≥ 50 OR on Chaos OR PMK non-capital.
- Gated on `PVPSERVER` — otherwise silently disallowed.

`villages/AllianceWar.java` — PvP-alliance-level wars. Peace requires
4 days (`345_600_000 ms`) since `timeStarted`. Epic-only in practice.

`zones/Zones.java` `addWarDomains()` — registers kingdom influence around
items returned by `Items.getWarTargets()`. Only fires on
`!HOMESERVER && targets.length > 0`. Targets are physical items that
someone has to plant. Not a "declare invasion" entry point — there's no
code that generates invasion events on its own.

## 6. Server flags — the master switchboard

`ServerEntry.java:59-112`:

| Flag | Effect |
|---|---|
| `HOMESERVER` | Single-kingdom mode. `Zones.getKingdom` hardcodes every tile. War declarations blocked. Guards have nothing hostile to see. |
| `PVPSERVER` | Enables village wars, enemy checks, KOS enforcement, aggressive creature kingdom checks. |
| `EPIC` | Turns on HOTA polling, Valrei fights, epic missions, alternate loot. |
| `challengeServer` | Challenge scoring, HOTA reward tracking variant. |
| `testServer` | King challenge count drops from 10 → 3, HOTA delay changes. |

Flags live on `ServerEntry`, persisted in `SERVERS` row, seeded from
`server_config.yaml`.

## 7. Alignment / reputation

`villages/Reputation.java`: `Map<Long, Reputation>` per village. Byte
value −100..+100, optional `permanent`. −30 is the criminal threshold —
below that in a village, guards attack (only if the player is also inside
the min perimeter).

`REPUTATION` table: `(WURMID, VILLAGEID, REPUTATION, PERMANENT)`.

No alignment stat (good/evil) — WU stripped that. What remains is kingdom
enmity + reputation + domination chain. All of it works; most of it sits
idle on Freedom because step 2 of `isEnemy` short-circuits.

## 8. Titles / offices

`kingdom/Appointment.java` + `Appointments.java`.

- 9 generic titles (0–8), stored as bitmask.
- 6 orders (30–35).
- 11 named officials (1500–1510). Only 1502 "Ambassador" has a live
  behavior hook — it receives tower attack warnings.

Otherwise cosmetic. No title-gated permissions (e.g. "chancellor can
declare war") — that's all unimplemented.

Reset cadence: weekly check in `King.poll`. 30d inactivity on PMK → auto
re-election.

## 9. HOTA (Hunt of the Ancients)

`epic/Hota.java` — Epic-server PvP event, runs fully but gated on
`Servers.isThisAnEpicServer() && getNextHota() > 0`.

Mechanics: pillars planted as FocusZones, broadcast announcement ("Let The
Hunt Begin!"), players capture pillars by touching them, first alliance to
4 wins, village gets a HOTA prize item, alliance gets +5 score.

`hotaDelay` default 2160 min = 36h. Not scheduled on Creative/Adventure
— `nextHota` stays `Long.MAX_VALUE`. **Fully coded and portable to any
PvP server with a two-line flip.**

## 10. Missions

- `tutorial/Missions.java` — vanilla tutorial/skill missions. Per-player
  rows in `MISSIONS`. Live.
- `epic/EpicMission.java` + `EpicMissionEnum` — deity + Valrei missions.
  Only fire on Epic.
- **No kingdom-scoped mission system.** PMKs have no native quest board.
  Mods would build on the generic mission table.

## 11. Diplomacy

- Kingdom-to-kingdom alliances in `KALLIANCES`. Real and respected by
  `isEnemy` checks and guard logic. Rarely surfaced because HOMESERVER
  servers have one kingdom.
- `PvPAlliance` — village alliances with deity pairings, MOTD, HOTA
  tracking. Stored in `PVPALLIANCE`.
- **No non-aggression pact state.** Binary only: allied, or eligible for
  war.
- **No vassalage / tribute.** Not modeled.

## 12. The dormant-but-coded list (the money section)

| Feature | Gate | Status |
|---|---|---|
| Guard tower influence zones | `Feature.NEW_KINGDOM_INF` | Works, enabled by default — but invisible on HOMESERVER |
| War-target PvP zones | `Items.getWarTargets().length > 0 && !HOMESERVER` | Works, needs items planted |
| HOTA | `EPIC && getNextHota() > 0` | Fully functional, turn-key on Epic |
| Alliance wars | `PVPSERVER` | Works, Epic-only in practice |
| Village wars | `PVPSERVER && !permanent` | Works, blocked on Freedom |
| Kingdom influence calc | always | Works, hardcoded to one answer on HOMESERVER |
| Guard tower aggression loop | always | Works, silent when no hostile kingdoms present |
| Reputation hostility (−30 rule) | always | Works, rarely triggered because reputation rarely tanks that far |
| King/chancellor election + eras | always | Works on PMK, silent on vanilla kingdoms unless PMK exists |
| Deed permissions / role system | `Feature.HIGHWAYS` | Works when feature enabled |
| PMK appointments (titles/orders/officials) | always | Schema + assignment works, behavior hooks only for official 1502 |

### What's actually missing (not just disabled)

- **Deed capture / conquest** — no code path ever transfers a village via
  combat. Would have to be modded top-down.
- **Door / gate destruction** — rams and catapults exist, but no target
  hook on structures.
- **Kingdom-scoped quests / missions** — no native surface; mods would
  layer on top of the generic mission table.
- **Non-aggression / vassalage diplomacy** — simply not modeled.
- **Invasion / raid generators** — no autonomous PvP event spawner; only
  HOTA exists and that's player-driven once pillars are up.

## 13. Relevant DB / config surface

`SERVERS` row (loaded via `server_config.yaml`):
- `HOMESERVER`, `PVPSERVER`, `EPIC`, `CHALLENGESERVER`, `TESTSERVER`
- `KINGDOM` (default id when HOMESERVER)
- `SPAWNPOINT{JENN,MOL,LIB}{X,Y}` — per-kingdom fallback respawn
- `HOTADELAY` (minutes)

`KINGDOMS`, `KALLIANCES`, `KING_ERA`, `APPOINTMENTS`, `OFFICES` — kingdom
side.

`VILLAGES`, `VILLAGEWARDECLARATIONS`, `VILLAGE_WARS`, `PVPALLIANCE`,
`ALLIANCEWARS`, `TOWERGUARDS`, `REPUTATION` — village / war side.

## 14. Takeaways for the framework

The most interesting thing in this code isn't what's disabled — it's what's
**fully wired and silent.** Guard towers, enmity pipelines, reputation,
kingdom influence, PMK elections all work; they just don't matter when
`HOMESERVER=true`. Flipping `HOMESERVER=false` on a custom map today would
surface most of it, at the cost of making everything PvP. The opportunity
is a middle-path mode: enmity + hostile NPCs + kingdom territory **without**
player PvP. That's a mod (or a set of framework hooks), not a vanilla
setting.

Specific candidates worth building framework events for:

1. **Kingdom enmity check override** — hook into `Village.isEnemy(Creature)`
   so mods can let mobs/guards treat any kingdom combo as hostile without
   also unlocking player PvP.
2. **Tower aggression broadcast** — hook the "ambassador alerted" path so
   a mod can surface a proper in-world alarm (UI, chat, map ping).
3. **HOTA-style events for non-Epic** — either flip the EPIC gate for HOTA
   specifically, or port the pillar-capture logic into its own generic
   event runner.
4. **War-target planter** — a tiny mod that auto-plants war-target items
   mid-map to wake up `addWarDomains` without requiring Epic.
5. **Siege-to-structure damage** — the actually-missing piece: make
   catapult/trebuchet projectile hits damage walls/gates. Needs a new
   event on structure impact.
6. **Deed conquest** — brand-new subsystem; worth scoping as a mod, not a
   framework feature.

None of this is next-up; it's the catalog for when the "kingdoms on a
PvE server" question comes up again.

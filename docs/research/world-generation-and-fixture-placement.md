# Vanilla Wurm world generation & fixture placement

How vanilla Wurm Unlimited decides where starter towns, altars, and spawn points go
on a new world. Reference for framework work aimed at fixing the "custom map +
stock DB → all NPC towns cluster in the NW corner" trap that bites every server
owner shipping a custom map.

All line refs point into
`~/Scripts/Games/WurmUnlimited/PowerFantasy/Wurmguide/decompiled/server_decompiled/`.

## TL;DR

**Wurm does not procedurally place starter towns.** Everything ships as rows in
`wurmzones.db`/`wurmitems.db` plus fallback coordinates in `server_config.yaml`,
all authored by the map publisher for the stock Creative/Adventure maps. Custom
maps (Riverweave, etc.) ship **only terrain** — no village DB, no spawn seeds —
so the server falls back to Creative-map coordinates baked into the config and
clamps anything out-of-bounds to the nearest map edge. That's the NW corner
cluster.

## Startup order

`Server.java:550-588` — first-boot init:

1. `Zones.staticInitialize()` — build zone grid (`Zones.java:3240`, calls
   `createZones()` at `:3242`)
2. `Villages.loadVillages()` — load permanent towns **from DB**
   (`Villages.java:606`)
3. `CreaturePos.loadAllPositions()`, `Creatures.loadAllCreatures()`
4. `Villages.loadGuards()`, `Zones.loadTowers()`
5. `Zones.addWarDomains()` — mark war-target flag for on-demand PvP camps

Nothing in this sequence scans terrain for "good town sites." Everything is DB
reads.

## Starter towns

`Villages.java:606-679` — pure SQL load:

```sql
SELECT ID, NAME, STARTX, STARTY, ENDX, ENDY, ..., PERMANENT, KINGDOM, SPAWNKINGDOM
FROM VILLAGES WHERE permanent=true
```

Columns that matter for placement:
- `STARTX/STARTY/ENDX/ENDY` — **absolute world tile coordinates**
- `KINGDOM` — owner kingdom (1=Jenn-Kellon, 2=Mol-Rehan, 3=HOTS, 4=Freedom)
- `SPAWNKINGDOM` — which kingdom respawns here
- `PERMANENT` — only rows with =1 count as starter towns

Live example (Gartopolis / Adventure preset):
- Hearth (526,829)–(540,843) K=2 Mol-Rehan
- Winkshir (1732,889)–(1750,907) K=1 Jenn-Kellon
- Litocania (908,1618)–(928,1642) K=3 HOTS

These numbers were hand-authored for the 4096² Adventure map. Drop them into a
1024² custom map and they either exceed bounds or land in meaningless spots.

## Altars

Altars are plain `ITEMS` rows with altar template IDs in `wurmitems.db`, loaded
with the rest of zone items. No placement algorithm; they sit wherever the
publisher placed them. Zone polling registers them into deity domains.

## Spawn points (respawn on death / initial spawn)

`Player.calculateSpawnPoints()` at `Player.java:3810-3863`, priority order:

1. Active mission respawn point.
2. Entry-server tutorial (K=4 Freedom permanent villages).
3. `randomSpawns=true` → spawn-stone **items** from the `ITEMS` table (lines
   3845-3853).
4. Normal: permanent villages matching player's kingdom (lines 3854-3863).
5. **Fallback** (`Player.spawn()` `:3745-3754`): hardcoded per-kingdom coords
   read from the `SERVERS` DB row:
   - `SPAWNPOINTJENNX/Y`
   - `SPAWNPOINTMOLX/Y`
   - `SPAWNPOINTLIBX/Y`

These come from `server_config.yaml` (`Adventure/server_config.yaml:113-122`):

```yaml
spawns:
  jennKellonX: 1758
  jennKellonY:  892
  molRehanX:    539
  molRehanY:    827
  hotsX:        883
  hotsY:       1627
```

Values are hand-authored for Adventure's 4096² landmass. Loaded at
`Servers.java:463-468`, stored on the `ServerEntry` (`:168`).

## Bounds clamping (the NW-corner mechanism)

`Zones.isGoodTileForSpawn()` at `Zones.java:3125-3132`:

```java
if (tilex < 0 || tiley < 0
        || tilex > worldTileSizeX || tiley > worldTileSizeY) return false;
```

Out-of-bounds coords are rejected for spawn selection, and the search falls
back. But villages themselves are still loaded at their DB coords, and
`Zones.safeTileX/Y` clamps downstream callers. When a custom map is smaller
than the coords authored for Adventure, everything collapses toward (0,0) or
the nearest edge. Add in that all three fallback spawns are in a rough cluster
on Adventure's real layout, and a smaller custom map puts them all on top of
each other — typically the NW strip of playable land.

## What *is* procedural

Two fixtures are genuinely generated at runtime, both gated behind flags that
default off:

**War-target camps** — `Zones.java:755-796`, `createCampsOnLine`. Places 3 PvP
camps, each randomly within a ±`worldTileSizeY/3` band, rejecting sites that
are: inside an existing village (60-tile buffer), on water (`height ≤ 2`), or
too steep (corners >±5 from center). Up to 1000 retries each. Fires once when
`shouldCreateWarTargets` is set.

**Source springs** — `Zones.java:722-753`, `createSprings`. Generates
`worldTileSizeX / 50` springs, random in the middle third of the map,
`height > 5`. Fires once when `shouldSourceSprings` is set.

Neither touches towns, altars, or starter spawns.

## Relevant server tunables

From `server_config.yaml` (mirrored into `SERVERS` DB row):

| Key | Effect |
|-----|--------|
| `spawns.{jennKellon,molRehan,hots}{X,Y}` | Fallback spawn coords per kingdom |
| `server.homeServer` | Locks players to their kingdom's villages only |
| `server.kingdom` | Server's default kingdom (for home servers) |
| `server.randomSpawns` | Use spawn-stone items instead of villages |
| `server.pvp` / `server.epic` | Alter spawn filtering |

## Custom map files (what a publisher actually ships)

Verified against `Riverweave 8k.rar` (Downloads, 353 MiB):

- `top_layer.map` — surface heightmap
- `rock_layer.map` — underground rock
- `map_cave.map` — cave tiles
- `resources.map` — ore placement
- `flags.map` — tile flags
- `map.png`, `heightmap.png`, `biomes.png`, `cave.png`, `topography.png` — refs
- `map_actions.act` — WurmMapGen history

**No `wurmzones.db`. No `wurmitems.db` seed. No kingdom DB. No spawn coords.**

This is typical for custom maps. Terrain only.

## Fixes a server owner currently has

1. Hand-edit `Adventure/sqlite/wurmzones.db → VILLAGES` with coords that make
   sense for the new map; update token items in `wurmitems.db`; update
   `server_config.yaml` fallback spawns.
2. Recreate villages in-game as a GM and flip `PERMANENT=1`.
3. Set `randomSpawns: true` and place spawn-stone items.
4. Wipe `wurmzones.db`/`wurmitems.db` on a fresh custom map so nothing inherits
   the old seed.

All manual, undocumented, and the reason server owners give up.

## Framework opportunity

A small bootstrap that, before `Villages.loadVillages()` runs, checks whether
the current `VILLAGES`/fallback coords are valid for the loaded map size and
— if not — seeds sensible rows per kingdom (yaml-configured or terrain-scanned
from `top_layer.map`). See `project_world_seeding_plan` memory for the
sketched design.

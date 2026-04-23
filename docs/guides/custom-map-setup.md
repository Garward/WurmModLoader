# Custom maps — painless first boot

If you're dropping a custom map (Riverweave, any third-party `.map` bundle)
onto a fresh server, read this first. It saves you the single most common
custom-map papercut: **all NPC starter towns clustered in the NW corner with
nothing else on the map**.

## Why that happens (the short version)

Vanilla Wurm doesn't procedurally place starter towns. It reads them from
absolute tile coordinates in `wurmzones.db` and `wurmitems.db` that were
hand-authored for the stock Creative / Adventure maps. Custom map bundles
ship only terrain — no village DB, no kingdom seeds, no altar rows. Result:
either you inherit an Adventure-map seed whose coordinates don't fit your
landmass, or you have no seed at all and respawn breaks.

Full technical trace: [`../research/world-generation-and-fixture-placement.md`](../research/world-generation-and-fixture-placement.md).

## What WurmModLoader does about it

A framework-owned bootstrap runs at `ServerPreInitEvent` — before Wurm reads
the zones DB — and, if no in-bounds permanent villages exist, seeds one.

**Default policy (works on any map, zero config):**

- One shared starter town at the geometric map center
  (`worldTileSizeX/2, worldTileSizeY/2`).
- Altar of Three + Bone Altar placed adjacent.
- All three kingdoms spawn there.
- Water-safety: the center tile and every tile within 20 tiles of it must be
  dry land (height > 0). If the raw center fails the check, the seeder
  spiral-searches outward for the nearest dry spot satisfying the same
  buffer.

That's the zero-config path. Boot a fresh custom map, and you get a playable
start point without editing anything.

## When you want more than the default

The seeder reads an optional yaml file:

**Path:** `<server-root>/config/wurmmodloader-world-seed.yaml`

Same directory as the other framework configs (`wurmmodloader-http.properties`,
`wurmmodloader-vanilla-fixes.properties`). A missing file means "use
defaults"; no boot error.

Starter template lives at
[`../reference/wurmmodloader-world-seed.yaml.example`](../reference/wurmmodloader-world-seed.yaml.example)
— copy it to `config/` and edit.

The yaml lets you:

- Turn the seeder off (`enabled: false`) if you're managing fixtures yourself.
- Switch to manual mode and list your own towns with per-kingdom assignments,
  custom sizes, and altars per town.
- Tune the water-safety buffer for tight-inlet maps.
- Opt in to **terrain flattening** under the town footprint (`flattenFootprint:
  true`). The seeder levels the village tiles + a 3-tile border to the center
  height — but refuses to do so if the footprint spans steep terrain (controlled
  by `flattenMaxSlope`), so you won't accidentally carve a plateau out of a
  mountain.
- Override the per-kingdom fallback respawn coordinates in the `SERVERS`
  table (the absolute last-resort point if a kingdom has no village).

## Manual migration from the stock seed

If you've already booted the server with an inherited `wurmzones.db` and
towns are stuck in the NW corner, the seeder won't auto-repair on its own —
the DB isn't empty, it's just wrong. Two options:

1. **Delete the stale rows** and let the seeder run clean on next boot:
   ```
   UPDATE VILLAGES SET PERMANENT=0 WHERE PERMANENT=1;
   ```
   in `<server-root>/<WorldName>/sqlite/wurmzones.db`. Backup first.
2. **Switch to `strategy: manual`** in the yaml and list the towns you
   actually want.

Either way, restart the server after changes — the seeder runs at boot, not
live.

## Related
- [`../research/world-generation-and-fixture-placement.md`](../research/world-generation-and-fixture-placement.md) — full vanilla placement algorithm, file:line refs
- [`../reference/wurmmodloader-world-seed.yaml.example`](../reference/wurmmodloader-world-seed.yaml.example) — config template

# LiveMap — WurmMapGen-based redesign

**Status:** Design — not yet implemented
**Date:** 2026-04-24
**Replaces:** the current in-house `LiveMapRenderer` + minimal `index.html`

---

## Problem

The in-house renderer is unstable at zoomed-out levels and slow under naïve full-resolution rendering. The web UI is a one-page placeholder. WurmMapGen has a battle-tested Leaflet UI with first-class realtime player markers, plus a working tile-pyramid generation pipeline.

**However** — side-by-side comparison shows our renderer produces visibly nicer output than WurmMapGen's: depth-graded water, directional hillshade with normalized normals, and an elevation brightness ramp that we don't want to lose. Their output is flatter and less informative.

So the win is asymmetric: lift WurmMapGen's **plumbing** (pyramid build orchestration, web UI, leaflet integration) but keep our **renderer** (per-pixel color choices). The original goal — **live in-game tracking of players and villages** — does not require live-rendered tiles. Tiles can be static. Markers must be live.

## Goals

1. Lift WurmMapGen's tile-pyramid orchestration (multi-zoom output, threadpool, file layout) into the mod, but plug in our existing `LiveMapRenderer` as the per-tile pixel source.
2. Replace the in-house web UI with WurmMapGen's `template/` (lifted, repointed at our endpoints).
3. Keep the live `/livemap/api/data` endpoint as the marker source.
4. In-game UI consumes the same marker feed via ModComm push (no HTTP from the client).
5. `#livemap regen` admin command for on-demand tile rebuild.

## Non-goals

- On-demand tile rendering. Tiles are pre-generated, served from disk.
- Reading WurmMapGen's database SQLite files. We have a live `MapData` source already.
- Per-tile cache invalidation on terrain change. Whole pyramid regenerates as a unit.
- Migration from the old endpoints — it's a fresh mod, no upstream consumers exist.

---

## Architecture

```
                ┌──────────────────────────────────────────────────┐
                │ LiveMapMod                                       │
                │                                                  │
ServerStarted ──┼─► TilePyramidBuilder.generate() (background)     │
                │       │                                          │
                │       ▼                                          │
                │   <world>/livemap-cache/tiles/{z}/{x}_{y}.png    │
                │                                                  │
                │   HttpServerSubsystem                            │
                │     /livemap/            → template/index.html   │
                │     /livemap/static/...  → template/* (css/js)   │
                │     /livemap/tiles/...   → disk tile pyramid     │
                │     /livemap/api/data    → MapData (live JSON)   │
                │     /livemap/api/config  → static map metadata   │
                │                                                  │
                │   ModComm channel "livemap.markers"              │
                │     server pushes player+village snapshot 2s     │
                │     in-game client UI consumes, renders          │
                └──────────────────────────────────────────────────┘
```

### Components

| File | Purpose |
|---|---|
| `LiveMapMod.java` | Lifecycle, endpoint registration, ModComm publishing loop |
| `renderer/TilePyramidBuilder.java` | Orchestration lifted from WurmMapGen `TileMapGenerator` (multi-zoom output layout, threadpool, file naming). Per-pixel rendering delegates to our existing `LiveMapRenderer`. Strips WurmMapGen's `WurmMapGen.fileManager` / `properties` static deps. |
| `renderer/LiveMapRenderer.java` | **Kept as-is.** Depth-graded water + directional hillshade + elevation brightness ramp. Visibly nicer than WurmMapGen's flat slope-shade. |
| `data/MapData.java` | Existing — players + villages snapshot |
| `data/MarkerSnapshot.java` | New — JSON-serializable POJO for both `/api/data` and ModComm payload |
| `web/StaticAssetServer.java` | Serves classpath resources under `/livemap/static/` and `/livemap/` (index) |
| `command/RegenCommand.java` | `#livemap regen` server console handler |
| `src/dist/web/` | WurmMapGen `template/` lifted verbatim, with the JS data-fetch URLs repointed |

### Tile storage

`<server>/livemap-cache/<world>/tiles/{z}/{x}_{y}.png`

- One pyramid per world.
- Generated in a background thread on `ServerStartedEvent` if missing or `mod.config: regenOnBoot=true`.
- Regenerated on `#livemap regen` (blocking on the calling thread, with progress logged).
- No automatic invalidation. Terrain rarely changes; admins can regen explicitly.

### Live markers — two consumers, one source

Both `/livemap/api/data` (browser) and the `livemap.markers` ModComm channel (in-game UI) produce the same `MarkerSnapshot` JSON.

- **Browser**: long-poll or 2s setInterval (whatever WurmMapGen's `app/markers.js` already does — keep it).
- **In-game client**: server pushes via ModComm every `markerPushIntervalMs` (default 2000) to all players whose client mod advertises the `livemap.markers` channel.

ModComm push avoids: HTTP-port discovery, NAT issues, CORS, auth headaches. Browser keeps HTTP because that's what Leaflet wants.

### Village-gating

Existing `MapData.getPlayersForViewer(viewerName)` already gates player visibility by village membership. ModComm push uses the per-player gated view; browser `/api/data` returns the public (no-player-positions) view. Existing `/api/data/me/{name}` viewer-scoped endpoint stays for the rare browser-from-logged-in-user case.

---

## Data flow

### Boot

1. `LiveMapMod.init()` — register Configurable, etc.
2. `ServerStartedEvent` fires.
3. Endpoints registered with HttpServerSubsystem (`/`, `/static/...`, `/tiles/...`, `/api/data`, `/api/data/me/...`, `/api/config`).
4. If `livemap-cache/<world>/tiles/0/` is empty (or `regenOnBoot=true`), background thread runs `TilePyramidBuilder.generate()`.
5. Marker push timer starts — every 2s, gather snapshot, push to all subscribed clients via ModComm.

### Browser request

1. GET `/livemap/` → static `index.html` (server-templated for `serverName`, `enableRealtimeMarkers`).
2. JS fetches `/livemap/api/config` → map size, tile size, zoom range.
3. Leaflet requests `/livemap/tiles/{z}/{x}_{y}.png` → static file.
4. Markers JS polls `/livemap/api/data` every 2s → repaints overlay.

### In-game client

1. Client mod registers `livemap.markers` ModComm channel.
2. Server's marker push timer sends `MarkerSnapshot` JSON to channel.
3. Client UI (separate concern, future work) renders tiles + overlay.

---

## Configuration (`mod.config`)

```properties
# Tile generation
tileSize=256                    # Pixels per tile (matches WurmMapGen default)
regenOnBoot=false               # Force-rebuild pyramid every boot
maxRenderThreads=4              # Parallel workers for tile generation

# Marker feed
markerPushIntervalMs=2000       # ModComm push cadence to in-game clients
enablePlayerTracking=true       # Master switch — false hides players from both feeds
enableVillageDisplay=true

# Web UI cosmetics (passed through to template)
serverName=My Wurm Server
showCitizens=true
```

---

## Lifting WurmMapGen — what changes

### Java side

`TileMapGenerator.java` (their) → `renderer/TilePyramidBuilder.java` (ours):

- Strip static singletons: `WurmMapGen.fileManager`, `WurmMapGen.properties`, `Logger.title/ok/warn`.
- Replace with constructor-injected `TilePyramidConfig` (paths, threads, tileSize) and JUL logger.
- Remove `database/*` and `filegen/*` references (we don't generate JSON files; markers are live).
- Keep MeshIO open/close, the multi-zoom loop, threadpool, file output layout (`tiles/{z}/{x}_{y}.png`).
- **Replace** their `generateImageTile()` per-pixel pass with a delegated call into our `LiveMapRenderer.createMapDumpScaled(worldX, worldY, worldW, worldH, tileSize, tileSize)` so all tiles inherit our rendering aesthetic.
- Output dir: configurable, defaults to `<world>/livemap-cache/tiles/`.

### Web side

`src/resources/template/` → `src/dist/web/` in the mod jar:

- Keep: `index.html`, `css/`, `app/{main,map,gui,util,markers}.js`, `markers/*.svg`, `dist/Leaflet*`.
- Drop: any reference to local `data/villages.json` etc. — we serve markers as one combined `/api/data` payload.
- Patch `app/markers.js` realtime fetch URL: `data/players.json` → `api/data`.
- Patch `index.html` Mustache template variables — server-templated by `LiveMapMod` at request time using `serverName`, `enableRealtimeMarkers`, `showPlayers`, `showCitizens` from config.

### Templating

WurmMapGen uses Mustache for `index.html` at build time. We do the same at request time (cheap — the file is small and we cache the rendered string). Use the existing `org.jmustache` dep if it's already on the classpath, otherwise a tiny regex pass over `{{key}}` placeholders.

---

## Error handling

| Failure | Behavior |
|---|---|
| Tile pyramid generation fails | Log SEVERE, keep server running, web UI shows blank tiles + Leaflet's default 404 grid. `#livemap regen` to retry. |
| HttpServerSubsystem not loaded | Log WARNING in `registerEndpoints()`, mod stays loaded for ModComm-only path. |
| ModComm channel not subscribed by any client | Push loop silently skips that 2s tick. |
| MeshIO fails to open | Pyramid build fails (logged); no impact on marker feed. |
| Player viewer not found in village | `MapData.getPlayersForViewer` returns empty list — already handled. |

No fallbacks for disabled features. If `enablePlayerTracking=false`, both feeds omit the `players` array.

---

## Testing

- **Unit**: `TilePyramidBuilder` against a small synthetic `MeshIO` — verify output dir layout, file count matches expected zoom levels.
- **Integration**: boot a server, `curl /livemap/` returns templated HTML, `curl /livemap/api/data` returns valid JSON with expected schema, `curl /livemap/tiles/0/0_0.png` returns a PNG.
- **Manual**: open browser, verify tiles render, players move in real-time, village markers appear.
- **In-game**: deferred — depends on client UI work. For now, log received `livemap.markers` payload size on a test client to confirm push cadence.

---

## Migration

This mod has no production users yet. The existing renderer + cache are deleted wholesale. No version bump needed beyond the mod's own `1.0.0`.

Files removed:
- `data/CachedTile.java` if it exists (in-memory tile cache — replaced by disk)
- The minimal embedded `index.html` resource

Files kept:
- `renderer/LiveMapRenderer.java` — still the per-pixel renderer, now invoked by `TilePyramidBuilder` instead of by `LiveMapMod` directly.

---

## Risks

1. **WurmMapGen renderer expects to run as a CLI with a populated FileManager.** Lifting it might surface assumptions we don't see yet. Mitigation: do the lift incrementally, run the original tool against our `top_layer.map` first as a baseline.
2. **License compatibility.** WurmMapGen is MIT (per `LICENSE`); mod is also permissive. Add `NOTICE` entry crediting `tyoda-wurm/WurmMapGen` and the upstream `woubuc/WurmMapGen`.
3. **First boot is slow.** Generating the full pyramid for an 8k map is multi-minute. Mitigation: background thread, web UI shows missing tiles as blank until generated, log progress. Default `regenOnBoot=false` so subsequent boots are instant.
4. **Mustache dependency.** If we don't already pull it transitively, the regex fallback is fine — `index.html` only uses `{{key}}` and `{{#flag}}...{{/flag}}` blocks, both trivially regex-able.

---

## Implementation order

1. Lift `TileMapGenerator` → `TilePyramidBuilder`, with our `LiveMapRenderer` injected as the pixel source. Run standalone against a test map, verify output uses our color/shading aesthetic at all zoom levels.
2. Wire `TilePyramidBuilder` into `LiveMapMod.onServerStarted()` — background thread, no HTTP yet.
3. Rip out the in-memory tile cache, register new tile-serving endpoint pointing at the disk pyramid.
4. Lift `template/` into `src/dist/web/`. Wire static endpoint. Verify browser loads the page (markers will be empty initially).
5. Repoint markers JS at `/livemap/api/data`. Verify polling works.
6. ModComm `livemap.markers` channel + push timer. (No client UI yet — verify with logged payload.)
7. `#livemap regen` console command.
8. Remove old code paths, update `README.md` and `mod.config`.

Each step is independently testable. Stops are safe — partial work doesn't break the rest of the modloader.

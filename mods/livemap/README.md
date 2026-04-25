# LiveMap Mod

Live interactive web map for Wurm Unlimited servers. Web UI lifted from
[tyoda-wurm/WurmMapGen](https://gitlab.com/tyoda-wurm/WurmMapGen) (MIT) with
the per-pixel renderer kept in-house — depth-graded water, directional
hillshade, elevation brightness ramp.

The server also pushes per-player marker snapshots over a ModComm channel so
in-game client mods can render the same data without an HTTP roundtrip.

See [`DESIGN.md`](DESIGN.md) for the architectural rationale.

## Architecture

```
ServerStarted ──► TileGridBuilder.generate() (background, daemon)
                    └── <serverdir>/mods/livemap/cache/images/{x}-{y}.png

HttpServerSubsystem
  /livemap/             → templated index.html
  /livemap/app|css|...  → bundled WurmMapGen web UI
  /livemap/images/...   → static tile grid on disk
  /livemap/data/*.json  → live JSON (config, villages, guardtowers, players, ...)
  /livemap/admin/regen  → trigger tile rebuild

ModComm channel "livemap.markers"
  push every markerPushIntervalMs (default 2s) per subscribed player
  payload: per-viewer village-gated player list + village list
```

Tiles are generated **once** at native resolution as a single-zoom grid; the
client-side Leaflet handles all zoom from that one base layer (no pyramid).

## Installation

The mod ships in the standard ModLoader distribution under
`mods/livemap/livemap.jar` + `mods/livemap.properties`. Drop it into
the server's `mods/` folder and restart.

Open `http://<server-ip>:<httpserver-port>/livemap/` in a browser.

## Configuration

`mods/livemap.properties` — see `mod.config` for full list:

| Option | Default | Description |
|---|---|---|
| `tileSize` | 256 | Pixel size of each square map tile |
| `regenOnBoot` | false | Force-regenerate the tile grid every boot |
| `maxRenderThreads` | 4 | Parallel workers during tile generation |
| `enablePlayerTracking` | true | Show online players (browser + ModComm push) |
| `enableVillageDisplay` | true | Show village/deed boundaries |
| `markerPushIntervalMs` | 2000 | ModComm push cadence; 0 = disable in-game push |
| `serverName` | Wurm Unlimited Server | Shown in the web UI title/header |

## Endpoints

| Path | Returns |
|---|---|
| `GET /livemap/` | Templated `index.html` |
| `GET /livemap/images/{x}-{y}.png` | Static tile from disk |
| `GET /livemap/data/config.json` | Map dimensions + zoom range |
| `GET /livemap/data/villages.json` | All villages with borders + tokens |
| `GET /livemap/data/guardtowers.json` | All guard towers |
| `GET /livemap/data/players.json` | All online players (no village gating) |
| `GET /livemap/data/structures.json` | (placeholder, empty) |
| `GET /livemap/data/portals.json` | (placeholder, empty) |
| `GET /livemap/admin/regen` | Trigger background tile regeneration |

The browser path returns the **public** player list (everyone online). The
village-gated per-player view is only sent through the in-game ModComm push
channel — sharing it over HTTP would let any browser scrape positions.

## ModComm channel

Channel name: `livemap.markers`

Packet format:
```
byte   packet type (1 = snapshot)
short  JSON byte length
bytes  UTF-8 JSON payload
```

Payload schema:
```json
{
  "players": [
    { "name": "...", "x": 0, "y": 0, "surface": true, "kingdom": 0 }
  ],
  "villages": [
    { "name": "...", "x": 0, "y": 0, "borders": [sx, sy, ex, ey] }
  ]
}
```

Snapshots are sent on player connect and every `markerPushIntervalMs` after.
Players see only fellow villagers (plus themselves) in the `players` array.

## Regenerating the tile grid

Tiles are generated once on first boot (or whenever the cache directory is
empty). To rebuild — for example after editing terrain — either:

- `curl http://<server-ip>:<port>/livemap/admin/regen` (returns 202-style JSON)
- Set `regenOnBoot=true` and restart
- Delete `<serverdir>/mods/livemap/cache/images/` and restart

The endpoint is currently unauthenticated; if you expose the HTTP server to
the public internet, firewall it or front it with a reverse proxy.

## Credits

- Web UI + tile-grid concept: [tyoda-wurm/WurmMapGen](https://gitlab.com/tyoda-wurm/WurmMapGen) (MIT)
- Renderer: in-house; see `renderer/LiveMapRenderer.java`
- Map library: [Leaflet](https://leafletjs.com/)

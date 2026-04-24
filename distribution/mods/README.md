# mods/

Each mod lives in its own self-contained subfolder:

```
mods/
├── enabled.json              ← master toggle (optional)
├── <modname>/
│   ├── <modname>.jar         ← the mod jar (filename matches folder name)
│   ├── mod.properties        ← classname, optional dependencies
│   ├── mod.config            ← runtime configuration (if the mod uses one)
│   └── <any other assets>    ← server packs, web UI, sql, etc.
```

## enabled.json

Permissive master toggle. Missing entries default to **enabled** — only an
explicit `false` disables a mod:

```json
{
  "livemap": true,
  "postgresbackend": false,
  "experimentalmod": false
}
```

Mod names match the subfolder name under `mods/`.

## Legacy layouts (still supported)

* Flat `mods/<name>.properties` + `mods/<name>.jar` (Ago-era)
* Flat `mods/<name>.properties` + `mods/<name>/<name>.jar` (mixed)

The loader tries the canonical subfolder layout first, then falls back to these
for unmigrated mods. New mods should ship in the canonical layout.

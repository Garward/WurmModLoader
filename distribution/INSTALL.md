# WurmModLoader Installation Guide

Quick install for server hosts. For full documentation, see the project [README](../README.md).

## Requirements

- **Wurm Unlimited Dedicated Server** (Steam, build 4596061+)
- **Java 8** to run a server (the JRE bundled with Wurm Unlimited works; system OpenJDK 8 also works)
- Java 17+ is only required if you want to **build** the framework or a mod from source

## Installation

### 1. Extract WurmModLoader

Extract the runtime ZIP into your Wurm Unlimited Dedicated Server directory:

```bash
cd "/path/to/Wurm Unlimited Dedicated Server"
unzip WurmModloader-Runtime-0.10.1.zip
```

The ZIP unpacks alongside `server.jar` / `common.jar` and adds the framework JARs, the `wurmmodloader.sh` / `wurmmodloader.bat` launchers, and an empty `mods/` folder.

### 2. Add Mods

Drop each mod into its own subfolder under `mods/`. Each mod needs a `mod.properties` descriptor and an unversioned JAR matching the folder name:

```
mods/
├── enabled.json                      ← master toggle (optional)
├── oversizedclub/
│   ├── mod.properties                ← REQUIRED
│   ├── mod.config                    ← optional mod settings
│   ├── icons/                        ← optional PNG icons
│   └── oversizedclub.jar             ← REQUIRED (unversioned)
└── duskombat/
    ├── mod.properties
    └── duskombat.jar
```

`mod.config` and `icons/` are optional — most mods won't ship either. Only `mod.properties` + `<modname>.jar` are required.

⚠️ The JAR filename **must not contain a version suffix** — the loader matches by folder name. Rename `mymod-1.2.3.jar` to `mymod.jar`.

> **Legacy Ago-style layout still loads.** Older mods using top-level `mods/<modname>.properties` + `mods/<modname>/<modname>.jar` continue to work via the legacy bridge. New mods should use the self-contained subfolder layout above.

### 3. Enable / disable mods (`mods/enabled.json`)

`mods/enabled.json` is the canonical master toggle. Missing entries default to enabled, so you only need to list mods you want turned **off**:

```json
{
  "_comment": "Set a mod to false to disable it without removing the folder.",

  "experimentalmod": false
}
```

### 4. Launch the Server

Run the framework launcher directly — it patches the server JAR in-memory and starts the world. There is no separate patcher step.

**Headless (production / automated):**
```bash
./wurmmodloader.sh start=Adventure
```

**GUI (development / world picker):**
```bash
./wurmmodloader.sh
```

**Windows:** use `wurmmodloader.bat` with the same arguments.

If a bytecode patch fails and you want startup to continue (for diagnostics), add `--continue-on-patch-error`.

## Verification

You should see lines like:

```
INFO ModLoader initialization COMPLETE
INFO Loaded N mods
```

in the console. Per-mod load lines (`[ModLoader] Loading <classname> as <modname>`) show which mods came up.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Loaded 0 mods` | No `mod.properties` in any `mods/<modname>/` folder, or `mods/enabled.json` has them all set to `false` |
| `ClassNotFoundException` for a mod class | `classname=` in `mod.properties` doesn't match the actual class inside the JAR |
| `<modname>` JAR found but mod never loads | JAR filename has a version suffix (`mymod-1.2.3.jar`); rename to `mymod.jar` or set `classpath=mymod-1.2.3.jar` in the descriptor |
| `class is frozen` errors | Mod is using game classes during `preInit()` — move them into an event handler. See README → Mod Development Q&A |

For detailed log triage, see [`docs/guides/troubleshooting.md`](../docs/guides/troubleshooting.md).

## Rollback

The framework doesn't modify `server.jar` on disk — patches are applied at load time by the launcher. To stop using WurmModLoader, just go back to launching the vanilla `WurmServerLauncher` binary. No restore step needed.

## Support

- **GitHub Issues:** https://github.com/garward/WurmModLoader/issues
- **Documentation:** [`docs/`](../docs/) directory in the repo
- **Original project:** https://github.com/ago1024/WurmServerModLauncher

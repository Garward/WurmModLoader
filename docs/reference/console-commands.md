# Console GM Commands

WurmModLoader adds a `#`-prefixed command system that runs on the **server console stdin** — no login required, no GM character needed. Commands execute at power level 5 (highest) because physical console access is treated as trusted admin.

- Implementation: [`ConsoleGMCommandRouter.java`](../../wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/console/ConsoleGMCommandRouter.java)
- Auto-discovery of native WU GM commands: [`GMCommandDiscovery.java`](../../wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/console/GMCommandDiscovery.java) + `GMCommandAutoInvoker.java`
- Reflective access to `Server` / players / items / skills: `ServerReflectionUtil.java`

Type any command at the server's stdin. Run `#help` to print the curated list, `#listall` to dump every auto-discovered native GM command.

> **SIGTERM warning.** `kill`, `pkill`, and Ctrl-C on the server process do **not** flush the DB — you'll get rollbacks. Always use `#shutdown` for a safe save.

---

## Quick reference

| Category | Commands |
|---|---|
| **Players** | `#who`, `#kick`, `#ban`, `#setpower`, `#findplayer` |
| **Teleport** | `#summon`, `#send` |
| **Server** | `#shutdown`, `#serverinfo` |
| **Time / weather** | `#time`, `#weather` |
| **Items / skills** | `#createitem`, `#giveskill` *(fuzzy-name matched)* |
| **Creatures** | `#spawncreature` *(fuzzy)* |
| **Chat** | `#toggleglobal`, `#sendmessage` |
| **Meta** | `#help`, `#listall` |

Custom commands listed here take priority over auto-discovered native commands with the same name.

---

## Server management

### `#shutdown <minutes> <reason>`

Canonical safe-save shutdown. Calls `Server.startShutdown(minutes * 60, reason)` via reflection — flushes DB, notifies players, saves world.

```
#shutdown 10 "Server restart"
#shutdown 1 "Emergency"
```

- `minutes` must be ≥ 1 (vanilla WU does not allow 0).
- `reason` can be quoted; surrounding quotes are stripped.

### `#serverinfo`

Prints online player count and current Wurm game time.

---

## Player management

### `#who`
List online players. GMs are tagged `[GM:<power>]`.

### `#kick <player>`
```
#kick Bob
```

### `#ban <player> <reason>`
Permanent ban (duration = 0).
```
#ban Alice "Cheating"
```

### `#setpower <player> <0-5>`
Player must be **online**.
```
#setpower Bob 2
```

### `#findplayer <name>`
Prints name, `(x, y)`, and GM power of the target. Returns "not found" if the player is offline (vanilla lookup only resolves online players).

---

## Teleport

### `#summon <player>`
Teleports to `(0, 0)` — map center / typical spawn. Not spawn-config-aware.
```
#summon Bob
```

### `#send <player> <x> <y>`
```
#send Bob 500 500
```

---

## Time & weather

### `#time <hours>`
Advances Wurm game time by N **Wurm hours** (added as `hours * 1000 * 60 * 60` ms).
```
#time 24
```

### `#weather <type>`
One of `clear` | `light` | `medium` | `heavy`.
```
#weather clear
```

---

## Items & skills

Both accept either a numeric ID **or** a name — names are fuzzy-matched against the full template list. On a miss, the top 3 suggestions are printed.

### `#createitem <player> <id|name> [quality]`
Quality is 1–100, default 50.
```
#createitem Bob 1 50          # by ID
#createitem Bob sword 50      # by name (fuzzy match)
```

### `#giveskill <player> <id|name> <amount>`
Amount is 0–100 (skill level, not delta).
```
#giveskill Bob 102 50
#giveskill Bob stamina 50
```

---

## Creatures

### `#spawncreature <name> <x> <y> [layer]`
`layer` is `0` (surface, default) or `-1` (cave). Name is fuzzy-matched.
```
#spawncreature troll 500 500
#spawncreature trol 500 500       # typo → fuzzy matches "troll"
#spawncreature rat 1200 800 -1    # cave spawn
```

---

## Chat

### `#toggleglobal <on|off>`
Accepts `on`/`off`, `true`/`false`, or `1`/`0`.
```
#toggleglobal off
```

### `#sendmessage <player> <message>`
```
#sendmessage Bob "Welcome to the server!"
```

---

## Meta

### `#help`
Prints the curated command list above with examples.

### `#listall`
Dumps every **auto-discovered** native WU GM command. These run through a logged-in GM's communicator (requires power ≥ 2 online). See `GMCommandDiscovery` / `GMCommandAutoInvoker`.

---

## Architecture notes

- Console input is read on a background thread and dispatched through a single-thread executor (`ConsoleGM-Executor`) to avoid racing server state.
- Commands are rejected with `Server not ready yet` until `ServerReflectionUtil.isServerRunning()` returns true.
- Custom commands (listed in `#help`) override auto-discovered versions of the same name — intentional, so console can have improved output and console-only safe paths.
- `#shutdown` is the only sanctioned shutdown path. `SIGTERM`, `kill`, Ctrl-C, and closing the tty skip the DB flush and roll back recent state.

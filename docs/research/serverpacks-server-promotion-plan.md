# ServerPacks Scope B — Server-Side Framework Promotion Plan

**Status:** PLAN (not yet executed)
**Companion:** Scope A (client) already shipped — see memory `project_serverpacks_client_promotion`.
**Author note:** This file exists to survive context compaction. Source of truth.

---

## Why

Today the server side of pack distribution lives in a community mod at
`WurmModLoader-CommunityMods/mods/serverpacks/`. The framework's icon stack
already depends on it being present, and reaches it via cross-classloader
reflection from `IconPackServerPacksBridge`:

- `IconPackServerPacksBridge` (framework) listens for the `serverpacks`
  ModEntry, then calls `addServerPack(String, byte[], ServerPackOptions[])`
  reflectively.
- `IconPackGenerator` / `FrameworkIconsPack` / `IconPackServerHook` all
  funnel through that bridge.

This is the same shape as the client-side problem Scope A solved (mod jar
in a sibling classloader → framework can't see it). On the client, we
folded `mods/serverpacks` into `wurmmodloader-client-{api,core}` and
canonicalized the API at
`com.garward.wurmmodloader.client.api.serverpacks.PackAssetResolver`.

Server side, the same fix: own pack hosting + ModComm distribution +
HTTP serving inside the framework, expose a clean public API in
`wurmmodloader-api`, and stop reflecting through a sibling mod.

---

## Source files to absorb (CommunityMods → framework)

```
mods/serverpacks/src/main/java/com/garward/wurmmodloader/mods/serverpacks/
├── ServerPackMod.java       (434 lines — the implementation, channels, HTTP, manifests)
├── PackInfo.java            ( 65 lines — name/data/path/sha1/sha256/size record)
├── CommandHandler.java      ( 21 lines — ModComm IChannelListener; both channels share it)
└── api/
    └── ServerPacks.java     ( 44 lines — public interface + ServerPackOptions enum)
```

**Total:** 564 lines, 4 files. Comparable to Scope A (client) in size.

---

## Target structure (mirror of Scope A)

```
wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/serverpacks/
├── ServerPacks.java          (public interface — kept identical signature)
├── ServerPackOptions.java    (extracted enum — was nested in ServerPacks)
└── package-info.java         (Javadoc: stability, since 0.x.0)

wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/serverpacks/
├── ServerPackHost.java       (replaces ServerPackMod — but as a framework service,
│                              not a WurmServerMod; lifecycle driven by ServerHook)
├── PackInfo.java             (internal; package-private if possible)
├── ServerPackChannelListener.java   (renamed CommandHandler)
├── ManifestBuilder.java      (extract sha1/sha256/size computation)
└── HttpEndpointBinder.java   (extract httpserver:register_endpoint wiring)
```

Public API stable identifier: `com.garward.wurmmodloader.api.serverpacks.ServerPacks`.

---

## Lifecycle / wiring

`ServerPackMod` is currently a `WurmServerMod` doing 4 things:

1. `init()` registers two ModComm channels (`com.garward.serverpacks` canonical,
   `ago.serverpacks` legacy alias — skipped if Ago's serverpacks owns it).
2. `onServerStarted` fires `httpserver:register_endpoint` to bind
   `^/(?<path>[^/]*)$` to `servePack(packid)`.
3. `onModAction(ModActionEvent serverpacks:add_pack)` ingests pack
   registrations from external mods.
4. `notifyPlayer` (called from a `ServerStartedEvent` handler / on first
   sync) writes manifests to whichever channel the player has active.

In the framework, this becomes a singleton service:

- **Construction:** `ServerPackHost.initialize()` called from `ServerHook`
  bootstrap (early, before mods load — channels must exist before any
  mod tries `addServerPack`).
- **Channels:** registered identically. Ago detection logic preserved
  verbatim (skip legacy registration if some other mod claimed the
  channel name first).
- **HTTP binding:** subscribe to `ServerStartedEvent`, fire
  `httpserver:register_endpoint` ModActionEvent. Same shape as today.
- **`addServerPack`:** static façade `ServerPackHost.add(...)` *and* the
  `ServerPacks` interface implementation both delegate into the same
  registry map.
- **`serverpacks:add_pack` ModActionEvent:** **kept verbatim**. Third-
  party mods (Ago era + community) ship code that fires this event;
  breaking it would break the ecosystem. Framework subscribes to it
  with the same handler logic.

---

## Migration steps (in order)

1. **Stub the API jar.** Copy `ServerPacks.java` into
   `wurmmodloader-api/.../api/serverpacks/`, extract `ServerPackOptions`
   to its own file. Do *not* delete the old API yet — re-export from
   the old package via a deprecated subclass that extends/implements
   the new one.

2. **Move the implementation.** Port `ServerPackMod` body into
   `ServerPackHost`. Drop the `WurmServerMod` interface — it becomes a
   framework-owned singleton with `initialize() / shutdown()` driven
   by `ServerHook`. `Configurable` config plumbing folds into
   framework config (`framework.properties`?) — preserve every
   existing key.

3. **Move helpers.** `PackInfo` → core (package-private). `CommandHandler`
   → `ServerPackChannelListener`. Extract `computeSha1/computeSha256/buildManifest`
   into `ManifestBuilder` (separable, makes Phase 4 sha1 sunset easier).
   Extract HTTP endpoint wiring into `HttpEndpointBinder`.

4. **Rewire `IconPackServerPacksBridge`.** Today it reflects through
   `entry.getModClassLoader().loadClass("...mods.serverpacks.api.ServerPacks$ServerPackOptions")`
   and calls `addServerPack` reflectively. After promotion: import
   `com.garward.wurmmodloader.api.serverpacks.ServerPacks` directly,
   delete the reflection. Bridge collapses to ~30 lines or disappears
   entirely (callers go straight to `ServerPackHost.add(...)` or
   `ServerPacks.get().addServerPack(...)`).

5. **Update internal callers.** `IconPackGenerator`, `FrameworkIconsPack`,
   `IconPackServerHook` — repoint to new API.

6. **Reduce the community mod to a compat shim** *or* delete it.
   - Compat shim: empty `WurmServerMod` that logs "serverpacks now
     framework-owned, this jar can be removed" and does nothing. Keeps
     `serverpacks.properties` / folder presence non-fatal for users
     who upgrade framework but not configs.
   - Delete: simpler, but server admins with the folder will see a
     mod-load failure. Document in CHANGELOG.

7. **Preserve external mod compat.**
   - `serverpacks:add_pack` ModActionEvent: kept.
   - Both channel names (`com.garward.serverpacks` + `ago.serverpacks`):
     kept. SHA-1 wire format preserved on legacy channel until Phase 4.
   - `com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks`
     symbol: re-exported as deprecated alias of the new
     `com.garward.wurmmodloader.api.serverpacks.ServerPacks`. Mods
     compiled against the old jar keep linking.
   - Ago detection: untouched — if Ago's serverpacks mod is also
     present, framework yields the legacy channel to it.

8. **Build, deploy, verify.**
   - oversizedclub icon end-to-end (canonical smoke test from Scope A).
   - iconzz (or any Ago-era pack mod) still serves on the legacy
     channel.
   - Vanilla client connects (HTTP endpoint serving via vanilla
     resource sync).
   - `wurmlog --since-last-restart --grep ServerPack` — no warnings,
     manifest counts match.

9. **Regenerate code index** (`codeindex regen`) so new packages are
   discoverable.

---

## Risk areas

- **HTTP endpoint timing.** httpserver mod must be present + initialized
  before `ServerStartedEvent` fires. Current implementation depends on
  this; promotion preserves the same timing — but if the framework now
  owns serverpacks, the *httpserver* dependency becomes a framework-
  level concern. Either: (a) framework hard-depends on httpserver and
  we promote that too (large), or (b) we keep httpserver as a soft
  optional dep and serverpacks degrades gracefully without it
  (current behavior — log a warning, skip HTTP, ModComm-only). **Pick (b).**

- **Cross-classloader symbol identity.** Mods compiled against the old
  `com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks` see
  *that* class object at runtime. The deprecated re-export must
  `extends` the new framework class so ServiceLoader / instanceof
  checks still work. *Or* the framework registers two separate
  service registrations (old type + new type) pointing at the same
  instance. Need to validate which approach the existing callers
  tolerate — `IconPackServerPacksBridge` does `entry.getWurmMod()`
  which returns whatever the mod registered, so if framework also
  registers itself as a `WurmServerMod`-shaped facade for the
  shim's benefit, both paths resolve. **Open question — verify
  before committing.**

- **Config keys.** `serverpacks.properties` keys (httpServerPort
  override, etc.) need a migration path. Either keep reading the file
  or relocate keys to `framework.properties`. **Decision: keep reading
  `serverpacks.properties` if present** so existing deployments don't
  lose config; new installs get docs pointing at `framework.properties`.

- **Phase 4 sha1 sunset.** Already planned; promotion doesn't change
  the timeline. Once promoted, dropping the legacy channel is a
  one-line deletion in `ServerPackHost`.

---

## Out of scope (explicitly)

- httpserver mod promotion — separate plan, larger blast radius.
- Ago's serverpacks (the upstream/legacy mod from Ago1024) — that
  remains a third-party drop-in; we coexist with it via the existing
  detection logic.
- Client-side changes — Scope A is done; this plan is server-only.
- Pack format changes — wire format frozen on both channels.

---

## Reference: Scope A (already shipped)

Stored as memory `project_serverpacks_client_promotion`:

> ServerPacks client promoted into framework — `mods/serverpacks` (client)
> folded into `wurmmodloader-client-{api,core}` to fix cross-classloader
> `PackAssetResolver` lookups; canonical API at
> `com.garward.wurmmodloader.client.api.serverpacks.PackAssetResolver`.

Use that diff as a structural template when executing this plan.

---

**Last updated:** 2026-04-30

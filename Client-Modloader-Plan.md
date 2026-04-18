## 1. Boot order & wrapper design

You already have the server side stack:

> `PATCH → ProxyServerHook.fireXyzEvent(...) → ServerHook.fireXyz(...) → EventBus.post(...)`

Plus:

* `BytecodePatch` interface in the API
* `PatchRegistry` / `PatchManager` to collect and apply patches

And your `ServerHook` is already acting as the bridge from Ago’s server listeners into your EventBus, with `ProxyServerHook.getInstance()` as the singleton.

You want a **mirror** of this on the client:

### 1.1 New module

Create a *client core* module mirroring server core:

* `wurmmodloader-core-client/`

  * `com.garward.wurmmodloader.modloader.client.ProxyClientHook`
  * `com.garward.wurmmodloader.modloader.client.ClientHook`
  * `com.garward.wurmmodloader.core.client.bytecode.*`
  * `com.garward.wurmmodloader.core.client.registry.*` (if needed later)

And extend your existing PatchRegistry usage by either:

* Reusing the same `PatchRegistry` (it’s generic `BytecodePatch` based), or
* Making a thin `ClientPatchRegistry` wrapper that just delegates to `PatchRegistry` but sits in a client-specific package.

### 1.2 Wrapper over Ago’s client modloader

High-level strategy:

* **Ship your own client bootstrap jar** (like Ago’s) that:

  * Injects your bytecode patches into the client JAR.
  * Exposes a `ProxyClientHook` singleton, just like `ProxyServerHook`.
  * **Internally** creates and holds a `ClientHook` instance which:

    * Wraps Ago’s client modloader interfaces via reflection (like `ServerHook` wraps Ago’s `ServerStartedListener` etc.).

Boot order options (practical):

1. **Replace Ago’s client patcher entirely**

   * Your launcher calls *your* patcher first.
   * Inside `ClientHook` you:

     * `new org.gotti.wurmunlimited.modloader.client.ClientModLoader(...)` (or equivalent) by reflection.
     * Register its listeners into your EventBus (like server side).
   * Result: all old Ago client mods still work, but *go through your hook*.

Given how you already mirrored Ago server-side with `ServerHook`, do the same pattern:

* `ClientHook` = your modern façade
* Internally: keep `Listeners` / legacy handler objects for Ago clients, similar to server `Listeners`.

---

## 2. Client hook layer: ClientHook / ProxyClientHook

Mirror the rulebook’s ServerHook pattern:

### 2.1 New classes

* `com.garward.wurmmodloader.modloader.client.ProxyClientHook`
* `com.garward.wurmmodloader.modloader.client.ClientHook`

**ProxyClientHook**

* Static singleton `getInstance()` exactly like `ProxyServerHook.getInstance()`.
* All methods are **static**, end in `Event` (per rulebook).
* Each method just forwards to the instance:

  ```java
  public static void fireClientTickEvent(...) {
      getInstance().fireClientTick(...);
  }
  ```

**ClientHook**

* Instance methods only, no statics (mirror ServerHook rules).

* For each Proxy method:

  ```java
  public void fireClientTick(...) {
      eventBus.post(new ClientTickEvent(...));
  }
  ```

* Also the place where you:

  * Initialize EventBus on the client.
  * Bootstrap legacy Ago client listeners.
  * Glue your client lifecycle to server lifecycle (e.g. send handshake packet on connect).

---

## 3. Bytecode patches & events for prediction

Using your existing patch architecture (`BytecodePatch`, `PatchRegistry`, `PatchManager`), we add **client-targeted patches** that only call `ProxyClientHook.*Event`.

We care about:

* **Client lifecycle** (when to init)
* **Game loop** (per-frame/per-tick events)
* **Input** (movement commands)
* **Entity state updates** (NPC & player position updates from server)

From the client index we know:

* `com.wurmonline.client.WurmClientBase` – core client class
* `com.wurmonline.client.LwjglClient` – LWJGL bootstrap, game loop driver
* `com.wurmonline.client.WurmEventHandler` – input/event handler

So:

### 3.1 Lifecycle patches

**Patch A: WurmClientBase init → ClientPreInit**

* Target: `com.wurmonline.client.WurmClientBase` constructor or `startClient`/`init` method (pick from the index once you inspect the full file).
* Insert at end of init:

  ```java
  ProxyClientHook.fireClientInitEvent(this);
  ```

  → `ClientInitEvent` in API.

**Patch B: Post-world load → ClientWorldLoaded**

* Target: method that finishes loading world/terrain and populates entities (e.g. in `WorldRender` or `WurmClientBase` depending on where it is).
* Insert after world is fully ready:

  ```java
  ProxyClientHook.fireClientWorldLoadedEvent(...);
  ```

### 3.2 Game loop patch (per-frame / per-tick)

**Patch C: LwjglClient main loop → ClientTick + ClientFrame**

* Target: main loop in `com.wurmonline.client.LwjglClient` (likely `run()`/`startGameLoop()`).
* Insert:

  * At the *very top* of each frame: `fireClientPreFrameEvent(...)`
  * After world update but before render: `fireClientTickEvent(...)`
  * After render: `fireClientPostFrameEvent(...)`

You can decide if you want one or two events; but having at least **ClientTickEvent** every frame is key for prediction.

### 3.3 Input patch (movement intent)

**Patch D: WurmEventHandler input → ClientInput**

* Target: keyboard handler in `WurmEventHandler` or the class that translates WASD/mouse to move commands.
* Insert just after the client decides on a movement command:

  ```java
  ProxyClientHook.fireClientInputEvent(player, inputState);
  ```

This gives you a clean “input stream” for prediction, without touching vanilla logic (still all logic in mods).

### 3.4 Entity update patches (for prediction hooks)

You need hooks around **where the client receives authoritative positions from the server**:

* The client class that decodes movement packets and updates `PlayerPosition` / `Creature` states.
* Those are visible in your client index as methods taking `ByteBuffer` and touching player/creature coordinates – once you locate them, add:

  * **Patch E: Authoritative Player Update**

    ```java
    ProxyClientHook.fireAuthoritativePlayerPositionEvent(player, newX, newY, newZ, seqId);
    ```

  * **Patch F: NPC Position Update**

    ```java
    ProxyClientHook.fireNpcPositionUpdateEvent(creature, newX, newY, newZ, vel, seqId);
    ```

These become:

* `AuthoritativePlayerPositionEvent`
* `NpcPositionUpdateEvent`

in the API, letting prediction code reconcile smoothly.

### 3.5 Events to add in API

In `wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/client/`:

* `ClientInitEvent`
* `ClientWorldLoadedEvent`
* `ClientTickEvent`
* `ClientPreFrameEvent`, `ClientPostFrameEvent` (optional but nice)
* `ClientInputEvent`
* `AuthoritativePlayerPositionEvent`
* `NpcPositionUpdateEvent`

All extend base `Event` and are immutable, per rulebook.

---

## 4. Sync with the server modloader

Prediction fails if the server has no idea what the client is doing, so we need a **sync channel** between your **client EventBus** and **server EventBus**.

### 4.1 Design a mod-comm style channel

On the server you already interact with `ModComm` etc for legacy networking.

Plan:

1. **Define a custom channel** (`WML_SYNC`) using ModComm (server side).
2. **Client side**, use Ago’s existing ModComm client or roll a tiny client shim in your wrapper to send/receive.

### 4.2 Events that cross the wire

Minimal useful messages:

* **MovementIntentMessage**

  * `playerId`
  * `seqId`
  * `inputState` (WASD, sprint, strafe, etc.)
* **ClientPredictionStateMessage**

  * Optional: client’s predicted pos for debugging / future smart reconciliation.
* **ServerCorrectionMessage**

  * `seqId`
  * `correctPos`
  * `reason` (e.g., collision, stamina, etc. – optional metadata)

Flow:

* Client:

  * `ClientInputEvent` handler → build `MovementIntentMessage` and send via ModComm.
  * Prediction module runs on `ClientTickEvent` using those inputs immediately.

* Server:

  * Your existing server movement patches already know when movement is processed; add a new event like `MovementAuthEvent` that includes the `seqId` and finalized position.
  * A handler compares with last client intent(s) and, when necessary, sends back `ServerCorrectionMessage`.

* Client:

  * On receiving `ServerCorrectionMessage`, client posts `ServerCorrectionEvent` into the client EventBus so prediction module reconciles (snap/lerp).

This keeps the *heavy* logic entirely in **mods / modules**, not in patches or hooks.

---

## 5. What belongs in “the actual mod” vs core

You specifically asked “how much I should put in the actual mod for this massive change”.

Given your rulebook and architecture:

### Core (modloader + client core)

**Only:**

* Bytecode patches:

  * Lifecycle, loop, input, entity update hooks (A–F above).
* `ProxyClientHook` / `ClientHook`.
* Client EventBus bootstrap.
* Minimal networking glue:

  * Registering the `WML_SYNC` channel.
  * Tiny send/receive helpers that convert ModComm payloads into **events**, nothing more.

No simulation, no prediction math.

### “Prediction mod” (or set of mods)

Put all **real logic** into one or more dedicated mods:

* `wml-prediction-client`:

  * Handles:

    * `ClientInputEvent` → store queued inputs with seqIds.
    * `ClientTickEvent` → simulate player movement using shared math.
    * `AuthoritativePlayerPositionEvent` / `ServerCorrectionEvent` → reconciliation.
    * `NpcPositionUpdateEvent` → NPC interpolation/extrapolation.
  * Talks to the sync channel by emitting events that a small “network bridge” mod converts into ModComm messages.

* Optional `wml-prediction-shared`:

  * Shared **movement math** between server and client so both sides use identical formulas.
  * Java code reused in both runtime classpaths.

On the **server**, another mod:

* `wml-prediction-server`:

  * Listens to `MovementIntentReceivedEvent` (fired when your ModComm handler receives client intent).
  * Uses the same math to validate vs actual `Creature.move()` / `Player.tick` results.
  * Decides whether to send corrections.

Combat prediction (later) would just be more events/patches:

* Hook attack action start, animation triggers, combat roll resolution.
* Fire events like `ClientCombatPredictionEvent` & `ServerCombatResultEvent`.
* Keep all the smarts in **mods** as well.

---

## TL;DR actionable steps

If you want a concrete to-do list out of this:

1. **Module & hook layer**

   * Create `ProxyClientHook` + `ClientHook` mirroring existing `ProxyServerHook`/`ServerHook`.
   * Wire them to a client EventBus.

2. **Client bootstrap wrapper**

   * Build your own client patcher jar.
   * Inside `ClientHook`, instantiate Ago’s client loader and bridge its callbacks into your EventBus (like server).

3. **Bytecode patches (core)**

   * Patch `WurmClientBase` → `ClientInitEvent`, `ClientWorldLoadedEvent`.
   * Patch `LwjglClient` → `ClientTickEvent` (and optional Pre/PostFrame).
   * Patch `WurmEventHandler` (or equivalent) → `ClientInputEvent`.
   * Patch entity update methods → `AuthoritativePlayerPositionEvent` and `NpcPositionUpdateEvent`.

4. **API events (wml-api)**

   * Add the client events listed in 3.5.

5. **Sync channel**

   * Add a WML sync ModComm channel.
   * Define MovementIntent / ServerCorrection messages.
   * Bridge them to events on both sides.

6. **Prediction mods**

   * Implement `wml-prediction-client` + `wml-prediction-server` to do all actual simulation, reconciliation, and smoothing.


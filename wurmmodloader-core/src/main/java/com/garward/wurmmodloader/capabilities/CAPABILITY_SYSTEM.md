# Server Capability Detection System

## Overview

The WML_CAPABILITIES ModComm channel enables client mods to detect which server-side mods are active before enabling client features. This prevents client mods from enabling features when the server doesn't support them.

## Use Cases

- **Sprint System:** Client only enables sprint UI/controls if server has sprint mod
- **Power Scaling:** Client shows custom UI elements only if server has power scaling active
- **Custom Combat:** Client animations sync with server combat mechanics
- **Prediction:** Client-side prediction only works with server-side validation

## Architecture

```
Server Startup
  ↓
WMLCapabilitiesChannel.initialize()
  ↓
Mods register capabilities:
  WMLCapabilitiesChannel.registerServerMod("sprint_system", "1.0.0")

Player Login
  ↓
Server sends capability packet to client
  ↓
Client fires ServerCapabilitiesReceivedEvent
  ↓
Client mods check capabilities and enable/disable features
```

---

## Server-Side Usage

### Step 1: Register Your Mod's Capability

In your server mod's initialization (e.g., in `configure()` or on `ServerStartedEvent`):

```java
import com.garward.wurmmodloader.capabilities.WMLCapabilitiesChannel;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;

public class SprintServerMod implements WurmServerMod {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Register this mod's capability so clients can detect it
        WMLCapabilitiesChannel.registerServerMod("sprint_system", "1.0.0");

        logger.info("Sprint system registered with capability system");
    }
}
```

### Step 2: (Optional) Add Description

```java
WMLCapabilitiesChannel.registerServerMod(
    "sprint_system",
    "1.0.0",
    "Adds sprinting mechanics with stamina consumption"
);
```

### What Happens Automatically

1. Server collects all registered mod capabilities
2. When player logs in, server sends capability list via ModComm
3. Client receives list and stores it in `ServerCapabilities`
4. Client fires `ServerCapabilitiesReceivedEvent`

---

## Client-Side Usage

### Step 1: Subscribe to ServerCapabilitiesReceivedEvent

```java
import com.garward.wurmmodloader.client.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.client.api.events.lifecycle.ServerCapabilitiesReceivedEvent;
import com.garward.wurmmodloader.client.api.capabilities.ServerCapabilities;

public class SprintClientMod {

    private boolean sprintEnabled = false;

    @SubscribeEvent
    public void onServerCapabilities(ServerCapabilitiesReceivedEvent event) {
        // Check if server has sprint system
        if (ServerCapabilities.hasServerMod("sprint_system")) {
            enableSprintFeatures();

            String version = ServerCapabilities.getModVersion("sprint_system");
            logger.info("Sprint enabled - server version: " + version);
        } else {
            logger.info("Server does not have sprint_system, client features disabled");
        }
    }

    private void enableSprintFeatures() {
        sprintEnabled = true;
        registerSprintKeybind();
        initSprintUI();
        logger.info("Sprint client features enabled");
    }
}
```

### Step 2: Query Capabilities Anytime

You can also query capabilities outside of the event:

```java
import com.garward.wurmmodloader.client.api.capabilities.ServerCapabilities;

public class SprintUI {

    public void render() {
        // Only render sprint UI if server supports it
        if (!ServerCapabilities.hasServerMod("sprint_system")) {
            return; // Server doesn't support sprinting
        }

        // Render sprint bar, stamina, etc.
        drawSprintBar();
    }
}
```

### Step 3: Check Version Compatibility

```java
@SubscribeEvent
public void onServerCapabilities(ServerCapabilitiesReceivedEvent event) {
    // Check if server has at least version 2.0
    if (ServerCapabilities.hasServerModWithVersion("power_scaling", "2.0")) {
        enablePowerScaling();
    } else {
        logger.warning("Server has old version of power_scaling - client features disabled");
    }
}
```

### Step 4: List All Server Mods

```java
@SubscribeEvent
public void onServerCapabilities(ServerCapabilitiesReceivedEvent event) {
    logger.info("Server mods detected:");
    for (ServerCapabilities.ModInfo mod : ServerCapabilities.getServerMods()) {
        logger.info("  " + mod.getModId() + " v" + mod.getVersion());
    }
}
```

---

## Complete Example: Sprint System

### Server Mod (sprint-server/src/main/java/...)

```java
package com.example.sprint.server;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.capabilities.WMLCapabilitiesChannel;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

public class SprintServerMod implements WurmServerMod {

    @Override
    public void configure() {
        // Empty - using events instead
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Register capability
        WMLCapabilitiesChannel.registerServerMod(
            "sprint_system",
            "1.0.0",
            "Server-side sprint mechanics"
        );
    }

    @SubscribeEvent
    public void onMovement(PlayerMovementEvent event) {
        // Handle sprint mechanics server-side
        // ... sprint logic
    }
}
```

### Client Mod (sprint-client/src/main/java/...)

```java
package com.example.sprint.client;

import com.garward.wurmmodloader.client.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.client.api.events.lifecycle.ServerCapabilitiesReceivedEvent;
import com.garward.wurmmodloader.client.api.events.lifecycle.ClientTickEvent;
import com.garward.wurmmodloader.client.api.capabilities.ServerCapabilities;

public class SprintClientMod {

    private boolean sprintEnabled = false;
    private boolean isSprinting = false;

    @SubscribeEvent
    public void onServerCapabilities(ServerCapabilitiesReceivedEvent event) {
        if (ServerCapabilities.hasServerMod("sprint_system")) {
            sprintEnabled = true;
            logger.info("Sprint enabled (server v" +
                       ServerCapabilities.getModVersion("sprint_system") + ")");
        } else {
            sprintEnabled = false;
            logger.info("Sprint disabled - server doesn't have sprint_system");
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (!sprintEnabled) {
            return; // Server doesn't support sprint
        }

        // Handle sprint input
        if (isSprintKeyPressed()) {
            if (!isSprinting) {
                startSprinting();
            }
        } else {
            if (isSprinting) {
                stopSprinting();
            }
        }

        // Update sprint UI
        updateSprintUI();
    }

    private void startSprinting() {
        isSprinting = true;
        // Send sprint intent to server via ModComm
        // SprintChannel.sendSprintStart();
    }
}
```

---

## API Reference

### Server-Side API

```java
// Register a mod capability
WMLCapabilitiesChannel.registerServerMod(String modId, String version);
WMLCapabilitiesChannel.registerServerMod(String modId, String version, String description);

// Get registered capabilities
List<ServerModCapability> capabilities = WMLCapabilitiesChannel.getServerMods();

// Send capabilities to a player (automatic on login)
WMLCapabilitiesChannel.sendCapabilitiesToPlayer(Player player);
```

### Client-Side API

```java
// Check if server has a mod
boolean hasIt = ServerCapabilities.hasServerMod("sprint_system");

// Check mod version
boolean compatible = ServerCapabilities.hasServerModWithVersion("sprint_system", "1.0");

// Get exact version
String version = ServerCapabilities.getModVersion("sprint_system"); // or null

// Get detailed info
ServerCapabilities.ModInfo info = ServerCapabilities.getModInfo("sprint_system");

// List all mods
List<ServerCapabilities.ModInfo> mods = ServerCapabilities.getServerMods();

// Check if capabilities received
boolean received = ServerCapabilities.hasReceivedCapabilities();
```

### Events

```java
// Fired when client receives server capabilities
@SubscribeEvent
public void onCapabilities(ServerCapabilitiesReceivedEvent event) {
    // Check capabilities
    event.hasServerMod("my_mod");
    event.getModVersion("my_mod");
    List<ServerModInfo> mods = event.getServerMods();
}
```

---

## Best Practices

### ✅ DO

- Register capabilities early (in `configure()` or `ServerStartedEvent`)
- Use semantic versioning (e.g., "1.0.0", "2.1.3")
- Check capabilities before enabling client features
- Gracefully handle missing server mods
- Log capability status for debugging

### ❌ DON'T

- Don't assume server has your mod - always check
- Don't register duplicate mod IDs
- Don't send custom packets before checking capabilities
- Don't hardcode version checks without compatibility range
- Don't enable client features without server support

---

## Troubleshooting

### Client features not enabling?

```java
@SubscribeEvent
public void onCapabilities(ServerCapabilitiesReceivedEvent event) {
    logger.info("Capabilities received: " + event.getServerMods().size() + " mods");
    for (ServerModInfo mod : event.getServerMods()) {
        logger.info("  - " + mod);
    }

    if (!ServerCapabilities.hasServerMod("my_mod")) {
        logger.warning("Server does not have my_mod!");
    }
}
```

### Server capabilities not being sent?

Check server logs for:
```
[WMLCapabilities] Registered server mod: my_mod:1.0.0
[WMLCapabilities] Sent N mod capabilities to PlayerName
```

### Capabilities cleared on disconnect?

Capabilities are automatically cleared when disconnecting. Subscribe to `ServerCapabilitiesReceivedEvent` to re-enable features when connecting to a new server.

---

## Version History

- **0.2.0** - Initial implementation
- Added WML_CAPABILITIES ModComm channel
- Added ServerCapabilities API
- Added ServerCapabilitiesReceivedEvent

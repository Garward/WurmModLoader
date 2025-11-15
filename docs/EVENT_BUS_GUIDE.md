# Event Bus Developer Guide

**WurmModLoader Phase 6: Modern Event System**

This guide shows you how to create mods using the new annotation-driven event system.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Available Events](#available-events)
3. [Event Priorities](#event-priorities)
4. [Event Cancellation](#event-cancellation)
5. [Complete Examples](#complete-examples)
6. [Best Practices](#best-practices)
7. [API Reference](#api-reference)

---

## Quick Start

### Creating Your First Event-Driven Mod

```java
package com.example.mymod;

import com.garward.wurmmodloader.api.event.*;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.util.logging.Logger;

public class MyMod implements WurmServerMod {
    private static final Logger logger = Logger.getLogger(MyMod.class.getName());

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("My mod is running!");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

**That's it!** No interfaces to implement, no manual registration needed.

### What You Need

**Dependencies in your `build.gradle.kts`:**
```kotlin
dependencies {
    // Modern event system
    implementation(project(":wurmmodloader-api"))

    // For ItemTemplateBuilder, CreatureTemplateBuilder, etc.
    implementation(project(":wurmmodloader-modsupport"))

    // Base interface (compileOnly - provided at runtime)
    compileOnly("org.gotti.wurmunlimited:modloader-shared:0.18")
}
```

**Imports you'll use:**
```java
import com.garward.wurmmodloader.api.event.*;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
```

---

## Available Events

### Server Lifecycle Events

#### ServerStartedEvent
Fired when the server has fully started and is ready to accept players.

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    logger.info("Server is ready!");
    // Initialize your systems, log status, etc.
}
```

**Use cases:**
- Post-initialization logging
- Registering with external systems
- Starting background tasks

---

#### ServerStoppingEvent
Fired when the server is shutting down.

```java
@SubscribeEvent
public void onServerStopping(ServerStoppingEvent event) {
    logger.info("Server shutting down - cleaning up...");
    // Save data, close connections, etc.
}
```

**Use cases:**
- Saving persistent data
- Closing database connections
- Cleanup operations

---

#### ServerPollEvent
Fired every server tick (very high frequency - ~1Hz).

```java
@SubscribeEvent
public void onServerPoll(ServerPollEvent event) {
    // This runs VERY frequently - keep it fast!
    checkSomethingPeriodically();
}
```

**⚠️ Warning:** This event fires every server tick. Keep your handler **extremely fast** or you'll cause lag.

**Use cases:**
- Periodic timers
- Background maintenance tasks
- Short polling operations

---

### Item & Template Events

#### ItemTemplatesCreatedEvent
Fired when item templates are initialized. This is where you create custom items.

```java
@SubscribeEvent
public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
    try {
        new ItemTemplateBuilder("mymod.customsword")
            .name("custom sword", "custom swords", "A magical blade")
            .combatDamage(10)
            .weightGrams(2000)
            .primarySkill(SkillList.SWORDS)
            .build();

        logger.info("Custom items created!");
    } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create custom items", e);
    }
}
```

**Use cases:**
- Creating custom weapons
- Adding new tools
- Defining custom armor pieces

**⚠️ Important:** Always wrap in try-catch and handle `IOException`.

---

### Player Events (Cancellable)

#### PlayerLoginEvent
Fired when a player attempts to log in. **Cancellable.**

```java
@SubscribeEvent
public void onPlayerLogin(PlayerLoginEvent event) {
    Player player = event.getPlayer();

    // Check if player is banned
    if (isPlayerBanned(player.getName())) {
        event.setCancelled(true);
        player.getCommunicator().sendAlertServerMessage("You are banned!");
        logger.info("Blocked login from banned player: " + player.getName());
    }
}
```

**Cancellation:** Setting `event.setCancelled(true)` prevents the player from logging in.

**Use cases:**
- Custom authentication
- Whitelist/blacklist systems
- Login restrictions

---

#### PlayerLogoutEvent
Fired when a player logs out.

```java
@SubscribeEvent
public void onPlayerLogout(PlayerLogoutEvent event) {
    Player player = event.getPlayer();
    logger.info(player.getName() + " logged out");

    // Save player data
    savePlayerData(player);
}
```

**Use cases:**
- Saving player data
- Cleanup operations
- Logging/statistics

---

#### PlayerMessageEvent
Fired when a player sends a message. **Cancellable.**

```java
@SubscribeEvent
public void onPlayerMessage(PlayerMessageEvent event) {
    Communicator comm = event.getCommunicator();
    String message = event.getMessage();
    String title = event.getTitle();

    // Custom command system
    if (message.startsWith("!help")) {
        comm.sendNormalServerMessage("Custom commands: !help, !info");
        event.setCancelled(true); // Don't show in chat
    }
}
```

**Cancellation:** Prevents the message from being processed further.

**Use cases:**
- Custom command systems
- Chat filters
- Message logging

---

#### ChannelMessageEvent
Fired when a channel message (kingdom/village/alliance) is sent. **Cancellable.**

```java
@SubscribeEvent
public void onChannelMessage(ChannelMessageEvent event) {
    Message message = event.getMessage();

    // Filter spam in kingdom chat
    if (isSpam(message.getMessage())) {
        event.setCancelled(true);
    }
}
```

**Use cases:**
- Channel-specific filters
- Logging alliance/kingdom communications
- Custom channel features

---

## Event Priorities

Control the order your event handlers run using `EventPriority`.

### Priority Levels

```java
public enum EventPriority {
    HIGHEST,  // Runs first
    HIGH,
    NORMAL,   // Default
    LOW,
    LOWEST    // Runs last
}
```

### Using Priorities

```java
@SubscribeEvent(priority = EventPriority.HIGHEST)
public void criticalHandler(ServerStartedEvent event) {
    // This runs before other handlers
    initializeCriticalSystems();
}

@SubscribeEvent(priority = EventPriority.LOW)
public void lateHandler(ServerStartedEvent event) {
    // This runs after most handlers
    doCleanupAfterOtherMods();
}

@SubscribeEvent  // Default: EventPriority.NORMAL
public void normalHandler(ServerStartedEvent event) {
    // Runs in the middle
}
```

### When to Use Each Priority

| Priority | Use When |
|----------|----------|
| **HIGHEST** | Critical initialization that other mods depend on |
| **HIGH** | Important operations that should run early |
| **NORMAL** | Most handlers (default) |
| **LOW** | Operations that depend on other mods finishing |
| **LOWEST** | Final cleanup, logging, or verification |

**Example Use Case:**

```java
// Mod A: Creates item templates
@SubscribeEvent(priority = EventPriority.HIGHEST)
public void createBaseItems(ItemTemplatesCreatedEvent event) {
    createMyItemTemplates();
}

// Mod B: Modifies item templates created by Mod A
@SubscribeEvent(priority = EventPriority.LOW)
public void enhanceItems(ItemTemplatesCreatedEvent event) {
    modifyExistingItemTemplates();
}
```

---

## Event Cancellation

Some events can be **cancelled** to prevent default behavior.

### Cancellable Events

- `PlayerLoginEvent`
- `PlayerMessageEvent`
- `ChannelMessageEvent`

### How to Cancel

```java
@SubscribeEvent
public void onPlayerLogin(PlayerLoginEvent event) {
    if (shouldBlockLogin(event.getPlayer())) {
        event.setCancelled(true);
        // Player won't log in
    }
}
```

### Receiving Cancelled Events

By default, handlers **don't run** on cancelled events. You can opt-in to receive them:

```java
@SubscribeEvent(receiveCancelled = true)
public void logAllLoginAttempts(PlayerLoginEvent event) {
    // This runs even if the event was cancelled by another handler
    if (event.isCancelled()) {
        logger.info("Login attempt was blocked for: " + event.getPlayer().getName());
    } else {
        logger.info("Login successful for: " + event.getPlayer().getName());
    }
}
```

### Cancellation Priority Flow

```java
// Handler 1 (HIGHEST) - Runs first
@SubscribeEvent(priority = EventPriority.HIGHEST)
public void firstCheck(PlayerLoginEvent event) {
    if (bannedPlayer) {
        event.setCancelled(true);
    }
}

// Handler 2 (NORMAL) - Doesn't run if cancelled
@SubscribeEvent
public void secondCheck(PlayerLoginEvent event) {
    // Won't run if firstCheck cancelled the event
}

// Handler 3 (LOWEST, receiveCancelled=true) - Always runs
@SubscribeEvent(priority = EventPriority.LOWEST, receiveCancelled = true)
public void logAttempt(PlayerLoginEvent event) {
    // Always runs, even if event was cancelled
    logger.info("Login attempt processed");
}
```

---

## Complete Examples

### Example 1: Custom Weapon Mod

```java
package com.example.weaponmod;

import com.garward.wurmmodloader.api.event.*;
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MagicSwordMod implements WurmServerMod {
    private static final Logger logger = Logger.getLogger(MagicSwordMod.class.getName());

    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        logger.info("Creating magic sword...");

        try {
            new ItemTemplateBuilder("magicsword.excalibur")
                .name("Excalibur", "Excaliburs",
                      "A legendary sword of immense power")
                .combatDamage(15)
                .weightGrams(3000)
                .primarySkill(10008) // Swords
                .dimensions(10, 100, 3)
                .difficulty(80.0f)
                .material((byte) 8) // Steel
                .itemTypes(new short[] { 22, 47, 37, 41 })
                // WEAPON, NAMED, REPAIRABLE, COMBAT
                .build();

            logger.info("Successfully created Excalibur!");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create magic sword", e);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("Magic Sword Mod v1.0.0 loaded!");
        logger.info("Excalibur is now available in the game");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

---

### Example 2: Player Management Mod

```java
package com.example.playermanager;

import com.garward.wurmmodloader.api.event.*;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.wurmonline.server.players.Player;
import java.util.*;
import java.util.logging.Logger;

public class PlayerManagerMod implements WurmServerMod {
    private static final Logger logger = Logger.getLogger(PlayerManagerMod.class.getName());

    private final Set<String> bannedPlayers = new HashSet<>();
    private final Map<String, Date> loginTimes = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void checkBannedPlayers(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (bannedPlayers.contains(player.getName().toLowerCase())) {
            event.setCancelled(true);
            player.getCommunicator().sendAlertServerMessage(
                "You are banned from this server");
            logger.warning("Blocked login from banned player: " + player.getName());
        }
    }

    @SubscribeEvent
    public void trackLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        loginTimes.put(player.getName(), new Date());
        logger.info(player.getName() + " logged in");
    }

    @SubscribeEvent
    public void trackLogout(PlayerLogoutEvent event) {
        Player player = event.getPlayer();
        Date loginTime = loginTimes.remove(player.getName());

        if (loginTime != null) {
            long sessionTime = new Date().getTime() - loginTime.getTime();
            logger.info(String.format("%s logged out after %d minutes",
                player.getName(), sessionTime / 60000));
        }
    }

    @SubscribeEvent
    public void handleCommands(PlayerMessageEvent event) {
        String message = event.getMessage();

        if (message.startsWith("!ban ")) {
            String playerName = message.substring(5).toLowerCase();
            bannedPlayers.add(playerName);
            event.getCommunicator().sendNormalServerMessage(
                "Banned player: " + playerName);
            event.setCancelled(true);
        }
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

---

### Example 3: Multi-Event Mod with Priorities

```java
package com.example.servermonitor;

import com.garward.wurmmodloader.api.event.*;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ServerMonitorMod implements WurmServerMod {
    private static final Logger logger = Logger.getLogger(ServerMonitorMod.class.getName());

    private final List<String> eventLog = new ArrayList<>();
    private long startTime;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerStartedFirst(ServerStartedEvent event) {
        // Critical initialization - runs first
        startTime = System.currentTimeMillis();
        logger.info("Server monitor starting...");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerStartedLast(ServerStartedEvent event) {
        // Final logging - runs last
        logger.info("All mods initialized successfully!");
        long initTime = System.currentTimeMillis() - startTime;
        logger.info("Total initialization time: " + initTime + "ms");
    }

    @SubscribeEvent(receiveCancelled = true)
    public void logAllLoginAttempts(PlayerLoginEvent event) {
        String status = event.isCancelled() ? "BLOCKED" : "ALLOWED";
        String logEntry = String.format("[%s] Login %s: %s",
            new Date(), status, event.getPlayer().getName());

        eventLog.add(logEntry);
        logger.info(logEntry);
    }

    @SubscribeEvent
    public void logPlayerMessages(PlayerMessageEvent event) {
        if (!event.isCancelled()) {
            String logEntry = String.format("[%s] %s: %s",
                new Date(),
                event.getCommunicator().getPlayer().getName(),
                event.getMessage());

            eventLog.add(logEntry);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Save all logs before shutdown
        try {
            saveLogsToFile();
            logger.info("Server monitor logs saved");
        } catch (IOException e) {
            logger.severe("Failed to save logs: " + e.getMessage());
        }
    }

    private void saveLogsToFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("server_monitor.log"))) {
            for (String entry : eventLog) {
                writer.write(entry);
                writer.newLine();
            }
        }
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

---

## Best Practices

### 1. Always Handle Exceptions

```java
@SubscribeEvent
public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
    try {
        // Create items
        createCustomItems();
    } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create items", e);
        // Don't crash the server!
    }
}
```

### 2. Keep ServerPollEvent Handlers Fast

```java
@SubscribeEvent
public void onServerPoll(ServerPollEvent event) {
    // BAD: Heavy operation every tick
    // processAllPlayersAndItems();

    // GOOD: Throttle to once per minute
    if (tickCounter++ % 3600 == 0) {
        doPeriodicMaintenance();
    }
}
```

### 3. Use Appropriate Priorities

```java
// Initialize database connection early
@SubscribeEvent(priority = EventPriority.HIGHEST)
public void initDatabase(ServerStartedEvent event) { }

// Use database after it's initialized
@SubscribeEvent(priority = EventPriority.NORMAL)
public void loadData(ServerStartedEvent event) { }
```

### 4. Clean Up Resources

```java
@SubscribeEvent
public void onServerStopping(ServerStoppingEvent event) {
    // Close connections
    if (databaseConnection != null) {
        databaseConnection.close();
    }

    // Save data
    saveAllData();

    // Clear caches
    cache.clear();
}
```

### 5. Log Important Events

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    logger.info("MyMod v" + getVersion() + " loaded successfully");
}

@SubscribeEvent(receiveCancelled = true)
public void logCancelledLogins(PlayerLoginEvent event) {
    if (event.isCancelled()) {
        logger.warning("Login blocked for: " + event.getPlayer().getName());
    }
}
```

### 6. One Event Handler Per Method

```java
// GOOD: Clear, focused handlers
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    initializeSystems();
}

@SubscribeEvent
public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
    createCustomItems();
}

// BAD: Don't try to handle multiple events in one method
// (Not even possible with @SubscribeEvent!)
```

---

## API Reference

### @SubscribeEvent Annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    /**
     * Event priority - controls execution order
     * Default: EventPriority.NORMAL
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Should this handler receive cancelled events?
     * Default: false
     */
    boolean receiveCancelled() default false;
}
```

**Usage:**
```java
@SubscribeEvent
public void defaultHandler(SomeEvent event) { }

@SubscribeEvent(priority = EventPriority.HIGH)
public void highPriorityHandler(SomeEvent event) { }

@SubscribeEvent(receiveCancelled = true)
public void alwaysRunHandler(SomeEvent event) { }

@SubscribeEvent(priority = EventPriority.LOWEST, receiveCancelled = true)
public void lastHandlerEvenIfCancelled(SomeEvent event) { }
```

---

### Event Base Class

```java
public abstract class Event {
    /**
     * Can this event be cancelled?
     */
    public boolean isCancellable();

    /**
     * Is this event currently cancelled?
     */
    public boolean isCancelled();

    /**
     * Cancel this event (only works if isCancellable() returns true)
     * @throws UnsupportedOperationException if event is not cancellable
     */
    public void setCancelled(boolean cancel);
}
```

---

### EventPriority Enum

```java
public enum EventPriority {
    HIGHEST(0),  // Executes first
    HIGH(1),
    NORMAL(2),   // Default
    LOW(3),
    LOWEST(4);   // Executes last

    public int getValue();
}
```

**Execution order:** HIGHEST → HIGH → NORMAL → LOW → LOWEST

---

## Troubleshooting

### My event handler isn't being called

**Check:**
1. ✅ Method is `public`
2. ✅ Method has `@SubscribeEvent` annotation
3. ✅ Method takes exactly one parameter (the event type)
4. ✅ Your mod class is loaded (check server logs)
5. ✅ Event type matches exactly (e.g., `ServerStartedEvent` not `ServerStartEvent`)

**Example:**
```java
// WRONG: Private method
@SubscribeEvent
private void onServerStarted(ServerStartedEvent event) { }

// WRONG: No parameter
@SubscribeEvent
public void onServerStarted() { }

// WRONG: Wrong event type
@SubscribeEvent
public void onServerStarted(ItemTemplatesCreatedEvent event) { }

// CORRECT!
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) { }
```

---

### Event handler throws exception

**Solution:** Wrap in try-catch
```java
@SubscribeEvent
public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
    try {
        createItems();
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to create items", e);
        // Server continues running
    }
}
```

---

### Cancelled event still runs my handler

**Check:** Are you using `receiveCancelled = true`?

```java
// Runs even if cancelled
@SubscribeEvent(receiveCancelled = true)
public void alwaysRun(PlayerLoginEvent event) { }

// Doesn't run if cancelled (default behavior)
@SubscribeEvent
public void onlyIfNotCancelled(PlayerLoginEvent event) { }
```

---

## Migration from Legacy Interfaces

See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for detailed instructions on converting existing mods to use the event system.

---

## Additional Resources

- **Example Mod:** See `examples/oversizedclub/` for a complete working example
- **API Source:** See `wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/event/`
- **Test Results:** See `PHASE6_TEST_RESULTS.md` for verification with DUSKombat

---

**Questions or issues?** Check the documentation or examine the OversizedClub example mod for reference implementations.

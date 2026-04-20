# Migration Guide: Legacy Interfaces → Event Bus

**Converting existing mods to use WurmModLoader's modern event system**

---

## Table of Contents

1. [Should I Migrate?](#should-i-migrate)
2. [Quick Migration Steps](#quick-migration-steps)
3. [Interface-by-Interface Guide](#interface-by-interface-guide)
4. [Complete Example Migrations](#complete-example-migrations)
5. [Testing Your Migration](#testing-your-migration)
6. [Troubleshooting](#troubleshooting)

---

## Should I Migrate?

### Your Existing Mods Still Work!

**Important:** You **DO NOT** need to migrate existing mods. The legacy interface system is fully supported and will continue to work alongside the new event system.

### Benefits of Migrating

| Old Way (Interfaces) | New Way (Events) | Benefit |
|---------------------|------------------|---------|
| `implements ServerStartedListener` | `@SubscribeEvent` | Cleaner code, fewer imports |
| Multiple interface implementations | Single `WurmServerMod` interface | Less interface pollution |
| No type safety on parameters | Type-safe event parameters | IDE auto-completion, compile-time checking |
| No priority control | `EventPriority` support | Control execution order |
| No cancellation | Event cancellation support | Block default behavior |

### When to Migrate

**Good times to migrate:**
- ✅ Starting a new feature
- ✅ Major refactoring
- ✅ Learning the new system
- ✅ Adding priority-based handlers
- ✅ Need event cancellation

**Skip migration if:**
- ❌ Mod is stable and working
- ❌ No active development
- ❌ Time-constrained
- ❌ Not comfortable with new patterns yet

---

## Quick Migration Steps

### Step 1: Keep WurmServerMod

```java
// KEEP THIS!
public class MyMod implements WurmServerMod {
    // ...
}
```

The `WurmServerMod` interface is required for both old and new patterns.

### Step 2: Remove Listener Interfaces

```java
// BEFORE
public class MyMod
    implements WurmServerMod, ServerStartedListener, ItemTemplatesCreatedListener {
    // ...
}

// AFTER
public class MyMod implements WurmServerMod {
    // ...
}
```

### Step 3: Replace @Override with @SubscribeEvent

```java
// BEFORE
@Override
public void onServerStarted() {
    logger.info("Server started!");
}

// AFTER
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    logger.info("Server started!");
}
```

### Step 4: Add Event Parameter

```java
// BEFORE: No parameters
public void onItemTemplatesCreated() { }

// AFTER: Add event parameter
public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) { }
```

### Step 5: Update Imports

```java
// REMOVE old imports
// import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;

// ADD new imports
import com.garward.wurmmodloader.api.event.*;
```

---

## Interface-by-Interface Guide

### ServerStartedListener

**Before:**
```java
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;

public class MyMod implements WurmServerMod, ServerStartedListener {

    @Override
    public void onServerStarted() {
        logger.info("Server started!");
        initializeSystems();
    }
}
```

**After:**
```java
import com.garward.wurmmodloader.api.event.ServerStartedEvent;
import com.garward.wurmmodloader.api.event.SubscribeEvent;

public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("Server started!");
        initializeSystems();
    }
}
```

**Changes:**
- ❌ Remove `ServerStartedListener` from implements clause
- ❌ Remove `@Override`
- ✅ Add `@SubscribeEvent`
- ✅ Add `ServerStartedEvent event` parameter
- ✅ Update imports

---

### ServerShutdownListener

**Before:**
```java
import org.gotti.wurmunlimited.modloader.interfaces.ServerShutdownListener;

public class MyMod implements WurmServerMod, ServerShutdownListener {

    @Override
    public void onServerShutdown() {
        logger.info("Server shutting down!");
        saveData();
    }
}
```

**After:**
```java
import com.garward.wurmmodloader.api.event.ServerStoppingEvent;
import com.garward.wurmmodloader.api.event.SubscribeEvent;

public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        logger.info("Server shutting down!");
        saveData();
    }
}
```

**Changes:**
- Event name changed: `ServerShutdown` → `ServerStopping`
- Method name can stay `onServerShutdown` or change to `onServerStopping` (your choice)

---

### ItemTemplatesCreatedListener

**Before:**
```java
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;

public class MyMod implements WurmServerMod, ItemTemplatesCreatedListener {

    @Override
    public void onItemTemplatesCreated() {
        try {
            createCustomItems();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create items", e);
        }
    }
}
```

**After:**
```java
import com.garward.wurmmodloader.api.event.ItemTemplatesCreatedEvent;
import com.garward.wurmmodloader.api.event.SubscribeEvent;

public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        try {
            createCustomItems();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create items", e);
        }
    }
}
```

**No functional changes** - just add the event parameter.

---

### ServerPollListener

**Before:**
```java
import org.gotti.wurmunlimited.modloader.interfaces.ServerPollListener;

public class MyMod implements WurmServerMod, ServerPollListener {

    @Override
    public void onServerPoll() {
        // Runs every tick
        doPeriodicTask();
    }
}
```

**After:**
```java
import com.garward.wurmmodloader.api.event.ServerPollEvent;
import com.garward.wurmmodloader.api.event.SubscribeEvent;

public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onServerPoll(ServerPollEvent event) {
        // Runs every tick
        doPeriodicTask();
    }
}
```

**⚠️ Performance:** Still runs very frequently - keep it fast!

---

### PlayerLoginListener

**Before:**
```java
import org.gotti.wurmunlimited.modloader.interfaces.PlayerLoginListener;

public class MyMod implements WurmServerMod, PlayerLoginListener {

    @Override
    public void onPlayerLogin(Player player) {
        logger.info(player.getName() + " logged in");
    }

    @Override
    public void onPlayerLogout(Player player) {
        logger.info(player.getName() + " logged out");
    }
}
```

**After:**
```java
import com.garward.wurmmodloader.api.event.*;

public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        logger.info(player.getName() + " logged in");
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLogoutEvent event) {
        Player player = event.getPlayer();
        logger.info(player.getName() + " logged out");
    }
}
```

**Changes:**
- Two separate events: `PlayerLoginEvent` and `PlayerLogoutEvent`
- Get player from event: `event.getPlayer()`
- Can now cancel login: `event.setCancelled(true)`

---

### PlayerMessageListener

**Before:**
```java
import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;

public class MyMod implements WurmServerMod, PlayerMessageListener {

    @Override
    public MessagePolicy onPlayerMessage(Communicator comm, String message, String title) {
        if (message.startsWith("!help")) {
            comm.sendNormalServerMessage("Help: ...");
            return MessagePolicy.DISCARD;
        }
        return MessagePolicy.PASS;
    }

    @Override
    public boolean onPlayerMessage(Communicator comm, String message) {
        // Legacy method - not used
        return false;
    }
}
```

**After:**
```java
import com.garward.wurmmodloader.api.event.*;

public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onPlayerMessage(PlayerMessageEvent event) {
        Communicator comm = event.getCommunicator();
        String message = event.getMessage();
        String title = event.getTitle();

        if (message.startsWith("!help")) {
            comm.sendNormalServerMessage("Help: ...");
            event.setCancelled(true); // Replaces MessagePolicy.DISCARD
        }
        // No need to return anything for MessagePolicy.PASS
    }
}
```

**Changes:**
- `MessagePolicy.DISCARD` → `event.setCancelled(true)`
- `MessagePolicy.PASS` → do nothing (default)
- Only one method needed (legacy variant removed)

---

### ChannelMessageListener

**Before:**
```java
import org.gotti.wurmunlimited.modloader.interfaces.ChannelMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;

public class MyMod implements WurmServerMod, ChannelMessageListener {

    @Override
    public MessagePolicy onKingdomMessage(Message message) {
        if (isSpam(message.getMessage())) {
            return MessagePolicy.DISCARD;
        }
        return MessagePolicy.PASS;
    }

    @Override
    public MessagePolicy onVillageMessage(Village village, Message message) {
        // Handle village messages
        return MessagePolicy.PASS;
    }

    @Override
    public MessagePolicy onAllianceMessage(PvPAlliance alliance, Message message) {
        // Handle alliance messages
        return MessagePolicy.PASS;
    }
}
```

**After:**
```java
import com.garward.wurmmodloader.api.event.*;

public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onChannelMessage(ChannelMessageEvent event) {
        Message message = event.getMessage();

        // Check message type if needed
        // (All channel types go through this one event)

        if (isSpam(message.getMessage())) {
            event.setCancelled(true);
        }
    }
}
```

**Changes:**
- Three methods collapsed into one event handler
- Check event properties if you need to distinguish channel types
- `MessagePolicy.DISCARD` → `event.setCancelled(true)`

---

## Complete Example Migrations

### Example 1: Simple Mod

**BEFORE:**
```java
package com.example.simplemod;

import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleMod
    implements WurmServerMod, ServerStartedListener, ItemTemplatesCreatedListener {

    private static final Logger logger = Logger.getLogger(SimpleMod.class.getName());

    @Override
    public void onItemTemplatesCreated() {
        try {
            createMyItems();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create items", e);
        }
    }

    @Override
    public void onServerStarted() {
        logger.info("SimpleMod v1.0.0 loaded!");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    private void createMyItems() throws IOException {
        // Create items...
    }
}
```

**AFTER:**
```java
package com.example.simplemod;

import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.api.event.*;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleMod implements WurmServerMod {

    private static final Logger logger = Logger.getLogger(SimpleMod.class.getName());

    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        try {
            createMyItems();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create items", e);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("SimpleMod v1.0.0 loaded!");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    private void createMyItems() throws IOException {
        // Create items...
    }
}
```

**Changes:**
- Removed 2 interface implementations
- Changed 2 `@Override` to `@SubscribeEvent`
- Added event parameters
- Updated imports

---

### Example 2: Player Management Mod

**BEFORE:**
```java
package com.example.playermgmt;

import org.gotti.wurmunlimited.modloader.interfaces.*;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.creatures.Communicator;

import java.util.*;
import java.util.logging.Logger;

public class PlayerMgmtMod
    implements WurmServerMod, PlayerLoginListener, PlayerMessageListener {

    private static final Logger logger = Logger.getLogger(PlayerMgmtMod.class.getName());
    private final Set<String> bannedPlayers = new HashSet<>();

    @Override
    public void onPlayerLogin(Player player) {
        if (bannedPlayers.contains(player.getName().toLowerCase())) {
            // Can't actually block login here in old system
            logger.warning("Banned player logged in: " + player.getName());
        } else {
            logger.info(player.getName() + " logged in");
        }
    }

    @Override
    public void onPlayerLogout(Player player) {
        logger.info(player.getName() + " logged out");
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator comm, String message, String title) {
        if (message.startsWith("!ban ")) {
            String playerName = message.substring(5).toLowerCase();
            bannedPlayers.add(playerName);
            comm.sendNormalServerMessage("Banned: " + playerName);
            return MessagePolicy.DISCARD;
        }
        return MessagePolicy.PASS;
    }

    @Override
    public boolean onPlayerMessage(Communicator comm, String message) {
        return false; // Unused
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

**AFTER:**
```java
package com.example.playermgmt;

import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.api.event.*;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.creatures.Communicator;

import java.util.*;
import java.util.logging.Logger;

public class PlayerMgmtMod implements WurmServerMod {

    private static final Logger logger = Logger.getLogger(PlayerMgmtMod.class.getName());
    private final Set<String> bannedPlayers = new HashSet<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void checkBannedPlayers(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (bannedPlayers.contains(player.getName().toLowerCase())) {
            // NOW we can actually block the login!
            event.setCancelled(true);
            player.getCommunicator().sendAlertServerMessage("You are banned");
            logger.warning("Blocked banned player: " + player.getName());
        } else {
            logger.info(player.getName() + " logged in");
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLogoutEvent event) {
        Player player = event.getPlayer();
        logger.info(player.getName() + " logged out");
    }

    @SubscribeEvent
    public void handleBanCommand(PlayerMessageEvent event) {
        String message = event.getMessage();

        if (message.startsWith("!ban ")) {
            String playerName = message.substring(5).toLowerCase();
            bannedPlayers.add(playerName);

            Communicator comm = event.getCommunicator();
            comm.sendNormalServerMessage("Banned: " + playerName);

            event.setCancelled(true);
        }
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

**Improvements:**
- ✅ Can now **actually block logins** with `event.setCancelled(true)`
- ✅ Uses `EventPriority.HIGHEST` to block before other mods process
- ✅ Removed unused `onPlayerMessage(Communicator, String)` method
- ✅ Cleaner code with fewer interfaces

---

### Example 3: Complex Multi-Interface Mod

**BEFORE:**
```java
public class ComplexMod
    implements WurmServerMod,
               Configurable,
               PreInitable,
               ServerStartedListener,
               ServerShutdownListener,
               ServerPollListener,
               ItemTemplatesCreatedListener,
               PlayerLoginListener {

    @Override
    public void configure(Properties properties) {
        // Configuration
    }

    @Override
    public void preInit() {
        // Bytecode hooks
    }

    @Override
    public void onItemTemplatesCreated() {
        // Create items
    }

    @Override
    public void onServerStarted() {
        // Initialization
    }

    @Override
    public void onServerShutdown() {
        // Cleanup
    }

    @Override
    public void onServerPoll() {
        // Periodic tasks
    }

    @Override
    public void onPlayerLogin(Player player) {
        // Handle login
    }

    @Override
    public void onPlayerLogout(Player player) {
        // Handle logout
    }
}
```

**AFTER:**
```java
public class ComplexMod
    implements WurmServerMod,
               Configurable,      // Keep this - still needed for config
               PreInitable {      // Keep this - needed for bytecode hooks

    @Override
    public void configure(Properties properties) {
        // Configuration - unchanged
    }

    @Override
    public void preInit() {
        // Bytecode hooks - unchanged
    }

    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        // Create items
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Initialization
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Cleanup
    }

    @SubscribeEvent
    public void onServerPoll(ServerPollEvent event) {
        // Periodic tasks
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        // Handle login
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLogoutEvent event) {
        Player player = event.getPlayer();
        // Handle logout
    }
}
```

**Key Points:**
- ✅ **Keep** `Configurable` and `PreInitable` - they're not listener interfaces
- ✅ Remove only the **listener** interfaces
- ✅ `configure()` and `preInit()` stay exactly the same

---

## Testing Your Migration

### 1. Compilation Check

After migrating, verify your mod compiles:

```bash
./gradlew :your-mod:build
```

**Common errors:**
- Missing `@SubscribeEvent` annotation
- Wrong event parameter type
- Method not public
- Import statements incorrect

---

### 2. Load Test

Deploy and check server logs for your event registrations:

```
INFO com.garward.wurmmodloader.core.event.EventBus register
    Registered 3 event handler(s) from com.example.MyMod
```

You should see a count of your `@SubscribeEvent` methods.

---

### 3. Functional Test

Test each migrated event handler:

- ✅ **ServerStartedEvent:** Check startup logs
- ✅ **ItemTemplatesCreatedEvent:** Verify custom items exist
- ✅ **PlayerLoginEvent:** Test with player login
- ✅ **PlayerMessageEvent:** Send test messages
- ✅ **ServerPollEvent:** Monitor periodic execution

---

### 4. Regression Test

Compare behavior before and after migration:

```java
// Add temporary logging to verify behavior matches
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    logger.info("NEW EVENT SYSTEM: onServerStarted called");
    // Original code here
}
```

---

## Troubleshooting

### Event handler not called

**Checklist:**
```java
// ✅ Method is public
public void myHandler(...) { }

// ✅ Has @SubscribeEvent annotation
@SubscribeEvent
public void myHandler(...) { }

// ✅ Takes exactly ONE parameter
@SubscribeEvent
public void myHandler(ServerStartedEvent event) { }

// ✅ Parameter is correct event type
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) { } // Correct
// NOT: public void onServerStarted(ItemTemplatesCreatedEvent event) { }
```

---

### Imports don't resolve

**Add to build.gradle.kts:**
```kotlin
dependencies {
    implementation(project(":wurmmodloader-api"))
    implementation(project(":wurmmodloader-modsupport"))
}
```

**Import statements:**
```java
import com.garward.wurmmodloader.api.event.*;
```

---

### Event parameter is null

**Issue:** Forgot to remove `@Override`

```java
// WRONG: @Override with no interface
@Override
public void onServerStarted(ServerStartedEvent event) {
    // event will be null!
}

// CORRECT: @SubscribeEvent
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    // event is properly provided
}
```

---

### Duplicate event firing

**Issue:** Left old interface in place

```java
// WRONG: Both old and new
public class MyMod
    implements WurmServerMod, ServerStartedListener { // Remove this!

    @Override  // This runs
    public void onServerStarted() { }

    @SubscribeEvent  // This also runs - duplicated!
    public void onServerStarted(ServerStartedEvent event) { }
}

// CORRECT: Only new
public class MyMod implements WurmServerMod {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) { }
}
```

---

### Can't cancel events

**Issue:** Event is not cancellable

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    event.setCancelled(true); // UnsupportedOperationException!
    // ServerStartedEvent is NOT cancellable
}
```

**Solution:** Check which events support cancellation:
- ✅ `PlayerLoginEvent`
- ✅ `PlayerMessageEvent`
- ✅ `ChannelMessageEvent`
- ❌ `ServerStartedEvent`
- ❌ `ServerStoppingEvent`
- ❌ `ItemTemplatesCreatedEvent`
- ❌ `ServerPollEvent`

---

## Gradual Migration Strategy

You **don't** have to migrate everything at once!

### Step 1: Start with one method

```java
public class MyMod
    implements WurmServerMod,
               ServerStartedListener,  // Keep for now
               ItemTemplatesCreatedListener {  // Remove this one

    @Override
    public void onServerStarted() {
        // Still using old way
    }

    @SubscribeEvent  // Migrated this one!
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        // Using new way
    }
}
```

### Step 2: Test thoroughly

Make sure the migrated method works correctly.

### Step 3: Migrate next method

```java
public class MyMod implements WurmServerMod {  // All interfaces removed

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Migrated!
    }

    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        // Already migrated!
    }
}
```

---

## Additional Resources

- **Event Bus Guide:** See [EVENT_BUS_GUIDE.md](EVENT_BUS_GUIDE.md) for complete API reference
- **Example Mod:** See `examples/oversizedclub/` for a full migration example
- **Test Results:** See `PHASE6_TEST_RESULTS.md` for real-world migration verification

---

## Summary

### Migration Checklist

- [ ] Remove listener interfaces from `implements` clause
- [ ] Keep `WurmServerMod`, `Configurable`, `PreInitable` (if used)
- [ ] Change `@Override` to `@SubscribeEvent`
- [ ] Add event parameter to method signatures
- [ ] Update import statements
- [ ] Get data from event objects (e.g., `event.getPlayer()`)
- [ ] Replace `MessagePolicy.DISCARD` with `event.setCancelled(true)`
- [ ] Test compilation
- [ ] Test functionality
- [ ] Verify event registration in logs

### Key Points

1. **Migration is optional** - old code still works
2. **Migrate incrementally** - one method at a time is fine
3. **Keep non-listener interfaces** - `Configurable`, `PreInitable`, etc.
4. **Test thoroughly** - verify behavior matches original
5. **Use priorities** - leverage new features like `EventPriority.HIGHEST`
6. **Enable cancellation** - block unwanted behavior with `event.setCancelled(true)`

---

**Ready to migrate?** Start with one event handler and work your way through. The new system is more powerful and easier to maintain!

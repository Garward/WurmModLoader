# Console GM Commands Research

## Goal
Enable GM commands to be executed directly from the server console instead of requiring login to the game.

**Groundbreaking Feature:** Nobody has ever done this for Wurm Unlimited!

---

## Current Architecture

### 1. Console Command Reader (`com.wurmonline.server.console.CommandReader`)

**Location:** `CommandReader.java` line 27-47

**Current Behavior:**
```java
public void run() {
    BufferedReader consoleReader = new BufferedReader(new InputStreamReader(this.inputStream));
    do {
        try {
            nextLine = consoleReader.readLine();
            if (nextLine == null) break;
            if (nextLine.equals("shutdown")) {
                this.server.shutDown();
                break;
            }
            logger.warning("Unknown command: " + nextLine);  // Line 39
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Can't read from console", e);
        }
    } while (nextLine != null);
}
```

**Key Points:**
- Only recognizes `shutdown` command
- All other input triggers "Unknown command" warning
- Runs in separate thread, reads stdin continuously
- Has reference to `Server` instance

---

### 2. GM Command Handler (`com.wurmonline.server.creatures.Communicator`)

**Location:** `Communicator.java` line 3547+

**How GM Commands Work:**
```java
private void reallyHandle_CMD_MESSAGE(ByteBuffer byteBuffer) throws Exception {
    String message = // ... read from network packet
    int power = this.player.getPower();  // Get player's GM power level (0-5)

    if (message.charAt(0) == '#' && this.player.getPower() >= 1) {
        if (message.startsWith("#kick")) {
            this.handleHashMessageKick(message, power);
        } else if (message.startsWith("#ban")) {
            this.handleHashMessageBan(message, power);
        } else if (message.startsWith("#summon")) {
            this.handleHashMessageSummon(message, power);
        }
        // ... 100+ more commands
    }
}
```

**Key Requirements:**
1. **Player Object:** Commands require `this.player` reference
2. **Power Level:** `player.getPower()` determines which commands are available
3. **Handler Methods:** Each command has a `handleHashMessage*()` method
4. **Output:** Results sent via `this.sendSafeServerMessage()` to player client

---

## Implementation Approaches

### **Approach 1: Virtual GM Player** (Cleanest)

Create a "console player" entity that exists only in memory.

**How It Works:**
1. Patch `CommandReader.java` to detect commands starting with `#`
2. Create a `ConsolePlayer` wrapper that:
   - Extends or wraps Player class
   - Returns power level 5 (highest GM)
   - Redirects output to console instead of network
3. Create `ConsoleCommandHandler` that routes commands through existing handlers

**Pros:**
- Uses existing command infrastructure
- All commands work automatically
- Easy to maintain when Wurm updates

**Cons:**
- Requires creating mock Player object
- May need to mock Communicator as well
- Complex initialization

**Feasibility:** 8/10

---

### **Approach 2: Direct Command Router** (Most Practical)

Bypass Player/Communicator and call underlying server methods directly.

**How It Works:**
1. Patch `CommandReader.java` to detect `#` commands
2. Create `ConsoleGMCommands` class with static methods:
   ```java
   public static void executeCommand(String command) {
       if (command.startsWith("#kick")) {
           handleKick(command);
       } else if (command.startsWith("#summon")) {
           handleSummon(command);
       }
       // ... etc
   }

   private static void handleKick(String command) {
       String playerName = parsePlayerName(command);
       Player target = Players.getInstance().getPlayer(playerName);
       if (target != null) {
           target.getCommunicator().disconnect();  // Kick
           System.out.println("[Console GM] Kicked player: " + playerName);
       }
   }
   ```
3. Reimplement only the most useful commands (don't need all 100+)

**Pros:**
- No Player mock needed
- Full control over implementation
- Can output directly to console
- Simple and maintainable

**Cons:**
- Need to reimplement each command
- May miss edge cases from original handlers
- More code to write initially

**Feasibility:** 9/10

---

### **Approach 3: Command Injection** (Hacky but Simple)

Find an online GM player and inject commands as if they typed them.

**How It Works:**
1. Patch `CommandReader.java` to detect `#` commands
2. Find any online player with power >= 2:
   ```java
   Player gm = null;
   for (Player p : Players.getInstance().getPlayers()) {
       if (p.getPower() >= 2) {
           gm = p;
           break;
       }
   }
   ```
3. Call `gm.getCommunicator().reallyHandle_CMD_MESSAGE()` with synthetic message
4. Capture output and echo to console

**Pros:**
- Minimal code changes
- Uses existing handlers completely
- Quick to implement

**Cons:**
- Requires a GM to be logged in
- Output goes to player's client, not console
- Can't use when no GMs are online
- Hacky and unreliable

**Feasibility:** 6/10 (not recommended)

---

## Recommended Implementation: **Approach 2** (Direct Router)

### Phase 1: Core Commands (MVP)

Implement the most essential commands first:

**Player Management:**
- `#kick <player>` - Kick player
- `#ban <player> <reason>` - Ban player
- `#setpower <player> <level>` - Set GM power level

**Teleportation:**
- `#summon <player>` - Teleport player to spawn
- `#send <player> <x> <y>` - Send player to coordinates

**Server:**
- `#shutdown <minutes> <reason>` - Schedule shutdown
- `#who` - List online players

**Items:**
- `#createitem <templateid> <ql>` - Give item to a player
- `#giveskill <player> <skill> <amount>` - Give skill points

**Debugging:**
- `#serverinfo` - Display server stats

### Phase 2: Extended Commands

Add more advanced commands as needed:
- Creature spawning
- Terrain modification
- Time/weather control
- etc.

---

## Implementation Details

### 1. Patch CommandReader

**File:** `wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/patches/CommandReaderPatch.java`

```java
public class CommandReaderPatch {
    public static void apply() throws Exception {
        CtClass ctClass = HookManager.getInstance().getClassPool()
            .get("com.wurmonline.server.console.CommandReader");

        CtMethod runMethod = ctClass.getDeclaredMethod("run");

        // Replace the "Unknown command" section with our handler
        String injectedCode =
            "if (nextLine.startsWith(\"#\")) {" +
            "    com.garward.wurmmodloader.core.console.ConsoleGMCommands.executeCommand(nextLine);" +
            "} else if (nextLine.equals(\"shutdown\")) {" +
            "    this.server.shutDown();" +
            "} else {" +
            "    logger.warning(\"Unknown command: \" + nextLine);" +
            "}";

        // Use ExprEditor or replace the entire if-else block
    }
}
```

### 2. Create ConsoleGMCommands

**File:** `wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/console/ConsoleGMCommands.java`

```java
public class ConsoleGMCommands {
    private static final Logger LOGGER = Logger.getLogger(ConsoleGMCommands.class.getName());

    public static void executeCommand(String command) {
        try {
            String[] parts = command.substring(1).split(" ");  // Remove # and split
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "kick":
                    handleKick(parts);
                    break;
                case "ban":
                    handleBan(parts);
                    break;
                case "who":
                    handleWho();
                    break;
                case "help":
                    handleHelp();
                    break;
                default:
                    System.out.println("[Console GM] Unknown command: " + command);
                    System.out.println("[Console GM] Type #help for available commands");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to execute console GM command: " + command, e);
        }
    }

    private static void handleKick(String[] parts) throws Exception {
        if (parts.length < 2) {
            System.out.println("[Console GM] Usage: #kick <playername>");
            return;
        }

        String playerName = parts[1];

        // Use reflection to access Players.getInstance()
        Class<?> playersClass = Class.forName("com.wurmonline.server.Players");
        Method getInstance = playersClass.getMethod("getInstance");
        Object playersInstance = getInstance.invoke(null);

        // Get player by name
        Method getPlayer = playersClass.getMethod("getPlayer", String.class);
        Object player = getPlayer.invoke(playersInstance, playerName);

        if (player == null) {
            System.out.println("[Console GM] Player not found: " + playerName);
            return;
        }

        // Get communicator and disconnect
        Class<?> playerClass = Class.forName("com.wurmonline.server.players.Player");
        Method getCommunicator = playerClass.getMethod("getCommunicator");
        Object communicator = getCommunicator.invoke(player);

        Class<?> commClass = Class.forName("com.wurmonline.server.creatures.Communicator");
        Method disconnect = commClass.getMethod("disconnect");
        disconnect.invoke(communicator);

        System.out.println("[Console GM] Kicked player: " + playerName);
    }

    private static void handleWho() throws Exception {
        Class<?> playersClass = Class.forName("com.wurmonline.server.Players");
        Method getInstance = playersClass.getMethod("getInstance");
        Object playersInstance = getInstance.invoke(null);

        Method getPlayers = playersClass.getMethod("getPlayers");
        Object[] players = (Object[]) getPlayers.invoke(playersInstance);

        System.out.println("[Console GM] Online players (" + players.length + "):");

        Class<?> playerClass = Class.forName("com.wurmonline.server.players.Player");
        Method getName = playerClass.getMethod("getName");
        Method getPower = playerClass.getMethod("getPower");

        for (Object player : players) {
            String name = (String) getName.invoke(player);
            int power = (int) getPower.invoke(player);
            String powerStr = power > 0 ? " [GM:" + power + "]" : "";
            System.out.println("  - " + name + powerStr);
        }
    }

    private static void handleHelp() {
        System.out.println("[Console GM] Available Commands:");
        System.out.println("  #kick <player> - Kick player from server");
        System.out.println("  #ban <player> <reason> - Ban player");
        System.out.println("  #who - List online players");
        System.out.println("  #help - Show this help");
        // ... more commands
    }
}
```

---

## Alternative: Reflection-Safe Wrapper

Instead of raw reflection, create clean wrappers:

**File:** `wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/console/ServerReflectionUtil.java`

```java
public class ServerReflectionUtil {

    public static Player[] getOnlinePlayers() throws Exception {
        Class<?> playersClass = Class.forName("com.wurmonline.server.Players");
        Method getInstance = playersClass.getMethod("getInstance");
        Object playersInstance = getInstance.invoke(null);
        Method getPlayers = playersClass.getMethod("getPlayers");
        return (Player[]) getPlayers.invoke(playersInstance);
    }

    public static Player getPlayerByName(String name) throws Exception {
        Class<?> playersClass = Class.forName("com.wurmonline.server.Players");
        Method getInstance = playersClass.getMethod("getInstance");
        Object playersInstance = getInstance.invoke(null);
        Method getPlayer = playersClass.getMethod("getPlayer", String.class);
        return (Player) getPlayer.invoke(playersInstance, name);
    }

    public static void kickPlayer(Player player) throws Exception {
        Method getCommunicator = player.getClass().getMethod("getCommunicator");
        Object communicator = getCommunicator.invoke(player);
        Method disconnect = communicator.getClass().getMethod("disconnect");
        disconnect.invoke(communicator);
    }
}
```

---

## Output Handling

Console commands should output to:
1. **System.out** (visible in console/logs)
2. **Logger** (for structured logging)

Example:
```java
System.out.println("[Console GM] Kicked player: Bob");
LOGGER.info("Console GM command executed: kick Bob");
```

---

## Testing Plan

1. **Test command parsing:** Verify # commands are detected
2. **Test reflection:** Ensure Player/Server classes are accessible
3. **Test #who:** List online players from console
4. **Test #kick:** Kick a test player
5. **Test error handling:** Invalid commands, missing players, etc.

---

## Security Considerations

⚠️ **CRITICAL:** Console has FULL server access!

**Mitigation:**
1. Only allow on local server console (not remote)
2. Log ALL console GM commands
3. No authentication bypass - physical console access = trusted
4. Consider optional config to disable feature

---

## Next Steps

1. ✅ Research complete
2. ⏳ Implement CommandReaderPatch
3. ⏳ Implement ConsoleGMCommands with core commands
4. ⏳ Test with #who and #kick
5. ⏳ Expand to more commands
6. ⏳ Add to documentation

---

## Estimated Complexity

- **CommandReaderPatch:** 1-2 hours
- **ConsoleGMCommands (MVP):** 3-4 hours
- **Extended commands:** 1 hour per 5 commands
- **Testing & polish:** 2 hours

**Total:** ~8-10 hours for full implementation

**MVP (just #who, #kick, #help):** ~3-4 hours

---

## Conclusion

**FEASIBLE:** ✅ Yes, definitely possible!

**RECOMMENDED APPROACH:** Direct Command Router (Approach 2)

**GROUNDBREAKING:** ✅ Nobody has done this for Wurm before!

This would be an incredibly useful feature for server administrators who need to perform quick GM actions without logging into the game.

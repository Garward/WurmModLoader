package com.garward.wurmmodloader.core.console;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Routes console GM commands to appropriate handlers.
 *
 * <p>This is a groundbreaking feature for Wurm Unlimited - the first ever implementation
 * of console-based GM commands without requiring login to the game.</p>
 *
 * <p><strong>Thread Safety:</strong> Commands entered from console (background thread)
 * are queued and executed on a dedicated executor thread to prevent crashes.</p>
 *
 * <p><strong>MVP Commands:</strong></p>
 * <ul>
 *   <li>#who - List all online players</li>
 *   <li>#kick &lt;player&gt; - Kick player from server</li>
 *   <li>#help - Show available commands</li>
 * </ul>
 *
 * <p><strong>Permission Level:</strong> All console commands execute at GM power level 5
 * (highest possible). Physical console access = trusted administrator.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ConsoleGMCommandRouter {

    private static final ConcurrentLinkedQueue<String> commandQueue = new ConcurrentLinkedQueue<>();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ConsoleGM-Executor");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean initialized = false;

    /**
     * Initialize the command processor.
     *
     * <p>Starts a background thread that processes queued commands safely.</p>
     */
    public static synchronized void initialize() {
        if (initialized) return;

        // Auto-discover GM commands from Communicator
        GMCommandDiscovery.discoverCommands();

        // Process command queue every 100ms
        executor.scheduleAtFixedRate(() -> {
            try {
                processQueuedCommands();
            } catch (Exception e) {
                System.out.println("[Console GM] Error processing command queue: " + e.getMessage());
            }
        }, 100, 100, TimeUnit.MILLISECONDS);

        initialized = true;
        System.out.println("[Console GM] Command router initialized");
        System.out.println("[Console GM] Type #help for available commands");
    }

    /**
     * Queue a command for execution.
     *
     * <p>Called from CommandReader background thread. Commands are queued and
     * executed on a dedicated thread to prevent blocking the console reader.</p>
     *
     * @param command The command to execute (including # prefix)
     */
    public static void queueCommand(String command) {
        if (!initialized) {
            initialize();
        }

        if (command == null || command.trim().isEmpty()) {
            return;
        }

        commandQueue.offer(command.trim());
    }

    /**
     * Process all queued commands.
     *
     * <p>Runs on dedicated executor thread, safe from blocking main operations.</p>
     */
    private static void processQueuedCommands() {
        String command;
        while ((command = commandQueue.poll()) != null) {
            executeCommand(command);
        }
    }

    /**
     * Execute a single console GM command.
     *
     * @param command The command to execute (including # prefix)
     */
    private static void executeCommand(String command) {
        try {
            // Verify server is running
            if (!ServerReflectionUtil.isServerRunning()) {
                System.out.println("[Console GM] Server not ready yet, please wait...");
                return;
            }

            // Remove # prefix and parse
            if (!command.startsWith("#")) {
                System.out.println("[Console GM] Commands must start with #");
                return;
            }

            String cmd = command.substring(1).trim();
            if (cmd.isEmpty()) {
                System.out.println("[Console GM] Empty command");
                return;
            }

            // Parse command and arguments
            String[] parts = cmd.split("\\s+", 2);
            String commandName = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1] : "";

            // Route to custom handler first (priority)
            if (executeCustomCommand(commandName, args)) {
                return; // Handled by custom command
            }

            // Fall back to auto-discovered commands
            if (GMCommandDiscovery.isDiscoveredCommand(commandName)) {
                GMCommandAutoInvoker.autoInvoke(commandName, cmd);
                return;
            }

            // Unknown command
            System.out.println("[Console GM] Unknown command: " + commandName);
            System.out.println("[Console GM] Type #help for available commands");
            System.out.println("[Console GM] Type #listall to see all " + GMCommandDiscovery.getCommandCount() + " discovered commands");

        } catch (Exception e) {
            System.out.println("[Console GM] Command failed: " + e.getMessage());
            System.out.println("[Console GM] Error details: " + e.getClass().getSimpleName());
        }
    }

    // ========== Command Routing ==========

    /**
     * Execute custom (manually implemented) commands.
     *
     * <p>Custom commands have priority over auto-discovered commands.
     * These are commands that need special handling like client sync,
     * better console output, or fixed implementations.</p>
     *
     * @param commandName Command name
     * @param args Command arguments
     * @return true if handled, false if not a custom command
     */
    private static boolean executeCustomCommand(String commandName, String args) {
        switch (commandName) {
            case "help":
                handleHelp();
                return true;

            case "listall":
                handleListAll();
                return true;

            case "who":
                handleWho();
                return true;

            case "kick":
                handleKick(args);
                return true;

            case "ban":
                handleBan(args);
                return true;

            case "setpower":
                handleSetPower(args);
                return true;

            case "summon":
                handleSummon(args);
                return true;

            case "send":
                handleSend(args);
                return true;

            case "shutdown":
                handleShutdown(args);
                return true;

            case "serverinfo":
                handleServerInfo();
                return true;

            case "time":
                handleTime(args);
                return true;

            case "findplayer":
                handleFindPlayer(args);
                return true;

            case "createitem":
                handleCreateItem(args);
                return true;

            case "giveskill":
                handleGiveSkill(args);
                return true;

            case "spawncreature":
                handleSpawnCreature(args);
                return true;

            case "weather":
                handleWeather(args);
                return true;

            case "toggleglobal":
                handleToggleGlobal(args);
                return true;

            case "sendmessage":
                handleSendMessage(args);
                return true;

            default:
                return false; // Not a custom command
        }
    }

    // ========== Custom Command Handlers ==========

    /**
     * Handle #help command - show available commands.
     */
    private static void handleHelp() {
        System.out.println("========================================");
        System.out.println("  CONSOLE GM COMMANDS (Power Level 5)");
        System.out.println("========================================");
        System.out.println();
        System.out.println("PLAYER MANAGEMENT:");
        System.out.println("  #who");
        System.out.println("    List all online players");
        System.out.println("  #kick <player>");
        System.out.println("    Kick player from server");
        System.out.println("  #ban <player> <reason>");
        System.out.println("    Ban player permanently");
        System.out.println("  #setpower <player> <0-5>");
        System.out.println("    Set player GM power level");
        System.out.println("  #findplayer <name>");
        System.out.println("    Find player location (offline/online)");
        System.out.println();
        System.out.println("TELEPORTATION:");
        System.out.println("  #summon <player>");
        System.out.println("    Teleport player to spawn (map center)");
        System.out.println("  #send <player> <x> <y>");
        System.out.println("    Send player to coordinates");
        System.out.println();
        System.out.println("SERVER MANAGEMENT:");
        System.out.println("  #shutdown <minutes> <reason>");
        System.out.println("    Schedule server shutdown");
        System.out.println("  #serverinfo");
        System.out.println("    Display server statistics");
        System.out.println();
        System.out.println("TIME/WEATHER:");
        System.out.println("  #time <hours>");
        System.out.println("    Advance time by X Wurm hours");
        System.out.println("  #weather <type>");
        System.out.println("    Set weather (clear/light/medium/heavy)");
        System.out.println();
        System.out.println("ITEMS/SKILLS:");
        System.out.println("  #createitem <player> <id|name> [quality]");
        System.out.println("    Create item for player (supports fuzzy name matching)");
        System.out.println("  #giveskill <player> <id|name> <amount>");
        System.out.println("    Set player skill level (supports fuzzy name matching)");
        System.out.println();
        System.out.println("CREATURES:");
        System.out.println("  #spawncreature <name> <x> <y> [layer]");
        System.out.println("    Spawn creature (supports fuzzy name matching)");
        System.out.println();
        System.out.println("CHAT:");
        System.out.println("  #toggleglobal <on|off>");
        System.out.println("    Enable/disable global chat");
        System.out.println("  #sendmessage <player> <message>");
        System.out.println("    Send message to player");
        System.out.println();
        System.out.println("OTHER:");
        System.out.println("  #help");
        System.out.println("    Show this help message");
        System.out.println("  #listall");
        System.out.println("    List all " + GMCommandDiscovery.getCommandCount() + " auto-discovered commands");
        System.out.println();
        System.out.println("========================================");
        System.out.println("Examples:");
        System.out.println("  #kick Bob");
        System.out.println("  #ban Alice \"Cheating\"");
        System.out.println("  #setpower Charlie 2");
        System.out.println("  #send Bob 500 500");
        System.out.println("  #time 24");
        System.out.println("  #weather clear");
        System.out.println("  #createitem Bob 1 50      (by ID)");
        System.out.println("  #createitem Bob sword 50  (by name - fuzzy match)");
        System.out.println("  #giveskill Bob 102 50     (by ID)");
        System.out.println("  #giveskill Bob stamina 50 (by name - fuzzy match)");
        System.out.println("  #spawncreature trol 500 500  (fuzzy matches 'troll')");
        System.out.println("  #sendmessage Bob \"Hello!\"");
        System.out.println("  #shutdown 10 \"Server restart\"");
        System.out.println("========================================");
        System.out.println("HYBRID SYSTEM:");
        System.out.println("  " + GMCommandDiscovery.getCommandCount() + " commands auto-discovered from Wurm");
        System.out.println("  Custom commands shown above run on console");
        System.out.println("  Auto-discovered commands run via GM context");
        System.out.println("  (Requires GM power ≥2 to be online)");
        System.out.println("========================================");
    }

    /**
     * Handle #listall command - show all auto-discovered commands.
     */
    private static void handleListAll() {
        System.out.println("========================================");
        System.out.println("  ALL AUTO-DISCOVERED GM COMMANDS");
        System.out.println("========================================");
        System.out.println();

        if (!GMCommandAutoInvoker.isAvailable()) {
            System.out.println("WARNING: No GM online (power ≥2)");
            System.out.println("Auto-discovered commands require a GM to be online");
            System.out.println("Login as GM to use these commands");
            System.out.println();
        } else {
            String gmName = GMCommandAutoInvoker.getAvailableGM();
            System.out.println("Auto-invoke GM context: " + gmName);
            System.out.println("(Commands execute via this player's communicator)");
            System.out.println();
        }

        System.out.println("Total commands: " + GMCommandDiscovery.getCommandCount());
        System.out.println();

        // Group commands by category
        java.util.List<String> commands = GMCommandDiscovery.getAllCommandNames();

        int column = 0;
        for (String cmd : commands) {
            System.out.print(String.format("  %-20s", cmd));
            column++;
            if (column >= 3) {
                System.out.println();
                column = 0;
            }
        }
        if (column != 0) {
            System.out.println();
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("NOTE: Custom commands (shown in #help)");
        System.out.println("      have special console implementations");
        System.out.println("      and override auto-discovered versions");
        System.out.println("========================================");
    }

    /**
     * Handle #who command - list online players.
     */
    private static void handleWho() {
        try {
            Object[] players = ServerReflectionUtil.getOnlinePlayers();

            if (players == null || players.length == 0) {
                System.out.println("[Console GM] No players online");
                return;
            }

            System.out.println("========================================");
            System.out.println("  ONLINE PLAYERS (" + players.length + ")");
            System.out.println("========================================");

            for (Object player : players) {
                String name = ServerReflectionUtil.getPlayerName(player);
                int power = ServerReflectionUtil.getPlayerPower(player);

                String powerStr = "";
                if (power > 0) {
                    powerStr = " [GM:" + power + "]";
                }

                System.out.println("  • " + name + powerStr);
            }

            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("[Console GM] Failed to list players: " + e.getMessage());
        }
    }

    /**
     * Handle #kick command - kick player from server.
     *
     * @param args Player name
     */
    private static void handleKick(String args) {
        // Validate arguments
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #kick <playername>");
            System.out.println("[Console GM] Example: #kick Bob");
            return;
        }

        String playerName = args.trim();

        try {
            // Find player
            Object player = ServerReflectionUtil.getPlayerByName(playerName);

            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                System.out.println("[Console GM] Use #who to see online players");
                return;
            }

            // Get actual player name (for case correction)
            String actualName = ServerReflectionUtil.getPlayerName(player);

            // Kick the player
            ServerReflectionUtil.kickPlayer(player);

            System.out.println("[Console GM] ✓ Kicked player: " + actualName);

        } catch (IllegalStateException e) {
            System.out.println("[Console GM] Cannot kick player: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to kick player: " + e.getMessage());
        }
    }

    // ========== Extended Command Handlers ==========

    /**
     * Handle #ban command - ban player permanently.
     *
     * @param args Player name and reason
     */
    private static void handleBan(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #ban <player> <reason>");
            System.out.println("[Console GM] Example: #ban Bob \"Cheating\"");
            return;
        }

        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("[Console GM] Usage: #ban <player> <reason>");
            return;
        }

        String playerName = parts[0];
        String reason = parts[1].replaceAll("^\"|\"$", ""); // Remove quotes if present

        try {
            ServerReflectionUtil.banPlayer(playerName, reason, 0); // 0 = permanent
            System.out.println("[Console GM] ✓ Banned player: " + playerName);
            System.out.println("[Console GM] Reason: " + reason);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to ban player: " + e.getMessage());
        }
    }

    /**
     * Handle #setpower command - set player GM power level.
     *
     * @param args Player name and power level
     */
    private static void handleSetPower(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #setpower <player> <0-5>");
            System.out.println("[Console GM] Example: #setpower Bob 2");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("[Console GM] Usage: #setpower <player> <0-5>");
            return;
        }

        String playerName = parts[0];
        int powerLevel;
        try {
            powerLevel = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.out.println("[Console GM] Invalid power level: " + parts[1]);
            System.out.println("[Console GM] Must be 0-5");
            return;
        }

        if (powerLevel < 0 || powerLevel > 5) {
            System.out.println("[Console GM] Power level must be 0-5");
            return;
        }

        try {
            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                System.out.println("[Console GM] Player must be online to set power");
                return;
            }

            String actualName = ServerReflectionUtil.getPlayerName(player);
            ServerReflectionUtil.setPlayerPower(player, powerLevel);

            System.out.println("[Console GM] ✓ Set power level for " + actualName + " to " + powerLevel);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to set power: " + e.getMessage());
        }
    }

    /**
     * Handle #summon command - teleport player to spawn.
     *
     * @param args Player name
     */
    private static void handleSummon(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #summon <player>");
            System.out.println("[Console GM] Example: #summon Bob");
            return;
        }

        String playerName = args.trim();

        try {
            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                return;
            }

            String actualName = ServerReflectionUtil.getPlayerName(player);

            // Teleport to spawn (map center = 0, 0 in most cases)
            // For more accurate spawn, we'd need to read from server config
            ServerReflectionUtil.setPlayerPosition(player, 0f, 0f);

            System.out.println("[Console GM] ✓ Summoned " + actualName + " to spawn");
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to summon player: " + e.getMessage());
        }
    }

    /**
     * Handle #send command - send player to coordinates.
     *
     * @param args Player name, X, Y
     */
    private static void handleSend(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #send <player> <x> <y>");
            System.out.println("[Console GM] Example: #send Bob 500 500");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 3) {
            System.out.println("[Console GM] Usage: #send <player> <x> <y>");
            return;
        }

        String playerName = parts[0];
        float x, y;

        try {
            x = Float.parseFloat(parts[1]);
            y = Float.parseFloat(parts[2]);
        } catch (NumberFormatException e) {
            System.out.println("[Console GM] Invalid coordinates");
            return;
        }

        try {
            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                return;
            }

            String actualName = ServerReflectionUtil.getPlayerName(player);
            ServerReflectionUtil.setPlayerPosition(player, x, y);

            System.out.println("[Console GM] ✓ Sent " + actualName + " to (" + x + ", " + y + ")");
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to send player: " + e.getMessage());
        }
    }

    /**
     * Handle #shutdown command - schedule server shutdown.
     *
     * @param args Minutes and reason
     */
    private static void handleShutdown(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #shutdown <minutes> <reason>");
            System.out.println("[Console GM] Example: #shutdown 10 \"Server restart\"");
            return;
        }

        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("[Console GM] Usage: #shutdown <minutes> <reason>");
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            System.out.println("[Console GM] Invalid minutes: " + parts[0]);
            return;
        }

        String reason = parts[1].replaceAll("^\"|\"$", "");

        try {
            ServerReflectionUtil.scheduleShutdown(minutes, reason);
            System.out.println("[Console GM] ✓ Shutdown scheduled in " + minutes + " minutes");
            System.out.println("[Console GM] Reason: " + reason);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to schedule shutdown: " + e.getMessage());
        }
    }

    /**
     * Handle #serverinfo command - display server statistics.
     */
    private static void handleServerInfo() {
        try {
            int playerCount = ServerReflectionUtil.getOnlinePlayerCount();
            long wurmTime = ServerReflectionUtil.getWurmTime();

            System.out.println("========================================");
            System.out.println("  SERVER INFORMATION");
            System.out.println("========================================");
            System.out.println("  Online Players: " + playerCount);
            System.out.println("  Wurm Time: " + wurmTime);
            System.out.println("========================================");
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to get server info: " + e.getMessage());
        }
    }

    /**
     * Handle #time command - advance Wurm time.
     *
     * @param args Hours to advance
     */
    private static void handleTime(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #time <hours>");
            System.out.println("[Console GM] Example: #time 24");
            return;
        }

        int hours;
        try {
            hours = Integer.parseInt(args.trim());
        } catch (NumberFormatException e) {
            System.out.println("[Console GM] Invalid hours: " + args);
            return;
        }

        try {
            long currentTime = ServerReflectionUtil.getWurmTime();
            long hoursInWurmTime = hours * 1000L * 60L * 60L; // Wurm time is in milliseconds
            long newTime = currentTime + hoursInWurmTime;

            ServerReflectionUtil.setWurmTime(newTime);

            System.out.println("[Console GM] ✓ Advanced time by " + hours + " hours");
            System.out.println("[Console GM] New Wurm time: " + newTime);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to advance time: " + e.getMessage());
        }
    }

    /**
     * Handle #findplayer command - locate player.
     *
     * @param args Player name
     */
    private static void handleFindPlayer(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #findplayer <name>");
            System.out.println("[Console GM] Example: #findplayer Bob");
            return;
        }

        String playerName = args.trim();

        try {
            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                System.out.println("[Console GM] Player may be offline");
                return;
            }

            String actualName = ServerReflectionUtil.getPlayerName(player);
            float x = ServerReflectionUtil.getPlayerPosX(player);
            float y = ServerReflectionUtil.getPlayerPosY(player);
            int power = ServerReflectionUtil.getPlayerPower(player);

            System.out.println("========================================");
            System.out.println("  PLAYER LOCATION");
            System.out.println("========================================");
            System.out.println("  Name: " + actualName);
            System.out.println("  Position: (" + x + ", " + y + ")");
            System.out.println("  GM Power: " + power);
            System.out.println("========================================");
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to find player: " + e.getMessage());
        }
    }

    /**
     * Handle #createitem command - create item for player.
     *
     * @param args Player name, template ID/name, quality
     */
    private static void handleCreateItem(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #createitem <player> <templateid|name> [quality]");
            System.out.println("[Console GM] Example: #createitem Bob 1 50");
            System.out.println("[Console GM] Example: #createitem Bob sword 50");
            System.out.println("[Console GM] Quality: 1-100 (default: 50)");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("[Console GM] Usage: #createitem <player> <templateid|name> [quality]");
            return;
        }

        String playerName = parts[0];
        int templateId = -1;
        float quality = 50f; // Default quality

        // Try parsing as numeric ID first
        try {
            templateId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            // Not a number - try fuzzy name matching
            String itemName = parts[1];
            templateId = tryItemFuzzyMatch(itemName);
            if (templateId == -1) {
                return; // Fuzzy match failed or showed suggestions
            }
        }

        if (parts.length >= 3) {
            try {
                quality = Float.parseFloat(parts[2]);
                if (quality < 1 || quality > 100) {
                    System.out.println("[Console GM] Quality must be 1-100");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("[Console GM] Invalid quality: " + parts[2]);
                return;
            }
        }

        try {
            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                return;
            }

            String actualName = ServerReflectionUtil.getPlayerName(player);
            Object item = ServerReflectionUtil.createItemForPlayer(player, templateId, quality);

            if (item != null) {
                System.out.println("[Console GM] ✓ Created item (template " + templateId + ", QL " + quality + ") for " + actualName);
            } else {
                System.out.println("[Console GM] Failed to create item (invalid template ID?)");
            }
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to create item: " + e.getMessage());
        }
    }

    /**
     * Try to find fuzzy matches for item name.
     *
     * @param itemName User input item name
     * @return Template ID or -1 if not found
     */
    private static int tryItemFuzzyMatch(String itemName) {
        try {
            java.util.Map<Integer, String> allItems = ServerReflectionUtil.getAllItemTemplates();

            // Build list of item names for fuzzy matching
            java.util.List<String> itemNames = new java.util.ArrayList<>(allItems.values());
            String bestMatch = FuzzyMatcher.findBestMatch(itemName, itemNames);

            if (bestMatch != null) {
                // Find the template ID for this name
                for (java.util.Map.Entry<Integer, String> entry : allItems.entrySet()) {
                    if (entry.getValue().equals(bestMatch)) {
                        int templateId = entry.getKey();
                        System.out.println("[Console GM] Matched item name to: " + bestMatch + " (ID: " + templateId + ")");
                        return templateId;
                    }
                }
            }

            // No match found - show suggestions
            java.util.List<String> suggestions = FuzzyMatcher.findSuggestions(itemName, itemNames, 3);
            if (!suggestions.isEmpty()) {
                System.out.println("[Console GM] Unknown item: " + itemName);
                System.out.println("[Console GM] Did you mean:");
                for (String suggestion : suggestions) {
                    System.out.println("[Console GM]   - " + suggestion);
                }
            } else {
                System.out.println("[Console GM] Unknown item: " + itemName);
                System.out.println("[Console GM] No similar items found");
            }
            return -1;
        } catch (Exception ex) {
            System.out.println("[Console GM] Failed to lookup item: " + ex.getMessage());
            return -1;
        }
    }

    /**
     * Handle #giveskill command - give skill to player.
     *
     * @param args Player name, skill ID/name, amount
     */
    private static void handleGiveSkill(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #giveskill <player> <skillid|name> <amount>");
            System.out.println("[Console GM] Example: #giveskill Bob 102 50");
            System.out.println("[Console GM] Example: #giveskill Bob \"body stamina\" 50");
            System.out.println("[Console GM] Amount: 0-100 (skill level)");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 3) {
            System.out.println("[Console GM] Usage: #giveskill <player> <skillid|name> <amount>");
            return;
        }

        String playerName = parts[0];
        int skillId = -1;
        double amount;

        // Try parsing as numeric ID first
        try {
            skillId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            // Not a number - try fuzzy name matching
            String skillName = parts[1];
            skillId = trySkillFuzzyMatch(skillName);
            if (skillId == -1) {
                return; // Fuzzy match failed or showed suggestions
            }
        }

        try {
            amount = Double.parseDouble(parts[2]);
            if (amount < 0 || amount > 100) {
                System.out.println("[Console GM] Skill amount must be 0-100");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[Console GM] Invalid amount: " + parts[2]);
            return;
        }

        try {
            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                return;
            }

            String actualName = ServerReflectionUtil.getPlayerName(player);
            ServerReflectionUtil.giveSkill(player, skillId, amount);

            System.out.println("[Console GM] ✓ Set skill " + skillId + " to " + amount + " for " + actualName);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to give skill: " + e.getMessage());
        }
    }

    /**
     * Try to find fuzzy matches for skill name.
     *
     * @param skillName User input skill name
     * @return Skill ID or -1 if not found
     */
    private static int trySkillFuzzyMatch(String skillName) {
        try {
            java.util.Map<Integer, String> allSkills = ServerReflectionUtil.getAllSkills();

            // Build list of skill names for fuzzy matching
            java.util.List<String> skillNames = new java.util.ArrayList<>(allSkills.values());
            String bestMatch = FuzzyMatcher.findBestMatch(skillName, skillNames);

            if (bestMatch != null) {
                // Find the skill ID for this name
                for (java.util.Map.Entry<Integer, String> entry : allSkills.entrySet()) {
                    if (entry.getValue().equals(bestMatch)) {
                        int skillId = entry.getKey();
                        System.out.println("[Console GM] Matched skill name to: " + bestMatch + " (ID: " + skillId + ")");
                        return skillId;
                    }
                }
            }

            // No match found - show suggestions
            java.util.List<String> suggestions = FuzzyMatcher.findSuggestions(skillName, skillNames, 3);
            if (!suggestions.isEmpty()) {
                System.out.println("[Console GM] Unknown skill: " + skillName);
                System.out.println("[Console GM] Did you mean:");
                for (String suggestion : suggestions) {
                    System.out.println("[Console GM]   - " + suggestion);
                }
            } else {
                System.out.println("[Console GM] Unknown skill: " + skillName);
                System.out.println("[Console GM] No similar skills found");
            }
            return -1;
        } catch (Exception ex) {
            System.out.println("[Console GM] Failed to lookup skill: " + ex.getMessage());
            return -1;
        }
    }

    /**
     * Handle #spawncreature command - spawn creature at location.
     *
     * @param args Creature name, X, Y, [layer]
     */
    private static void handleSpawnCreature(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #spawncreature <name> <x> <y> [layer]");
            System.out.println("[Console GM] Example: #spawncreature troll 500 500");
            System.out.println("[Console GM] Layer: 0=surface (default), -1=cave");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 3) {
            System.out.println("[Console GM] Usage: #spawncreature <name> <x> <y> [layer]");
            return;
        }

        String creatureName = parts[0].toLowerCase();
        float x, y;
        int layer = 0; // Default to surface

        try {
            x = Float.parseFloat(parts[1]);
            y = Float.parseFloat(parts[2]);
        } catch (NumberFormatException e) {
            System.out.println("[Console GM] Invalid coordinates");
            return;
        }

        if (parts.length >= 4) {
            try {
                layer = Integer.parseInt(parts[3]);
                if (layer != 0 && layer != -1) {
                    System.out.println("[Console GM] Layer must be 0 (surface) or -1 (cave)");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("[Console GM] Invalid layer: " + parts[3]);
                return;
            }
        }

        try {
            // Try to spawn the creature
            Object creature = ServerReflectionUtil.spawnCreature(creatureName, x, y, layer);

            if (creature != null) {
                String layerStr = (layer == 0) ? "surface" : "cave";
                System.out.println("[Console GM] ✓ Spawned " + creatureName + " at (" + x + ", " + y + ") on " + layerStr);
            } else {
                // Creature template not found - try fuzzy matching
                tryCreatureFuzzyMatch(creatureName);
            }
        } catch (Exception e) {
            // Exception thrown (likely template not found) - try fuzzy matching
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("template") || errorMsg.contains("not found") || errorMsg.contains("null"))) {
                tryCreatureFuzzyMatch(creatureName);
            } else {
                System.out.println("[Console GM] Failed to spawn creature: " + e.getMessage());
            }
        }
    }

    /**
     * Try to find fuzzy matches for creature name.
     *
     * @param creatureName User input creature name
     */
    private static void tryCreatureFuzzyMatch(String creatureName) {
        try {
            java.util.List<String> allCreatures = ServerReflectionUtil.getAllCreatureTemplateNames();
            java.util.List<String> suggestions = FuzzyMatcher.findSuggestions(creatureName, allCreatures, 3);

            if (!suggestions.isEmpty()) {
                System.out.println("[Console GM] Unknown creature: " + creatureName);
                System.out.println("[Console GM] Did you mean:");
                for (String suggestion : suggestions) {
                    System.out.println("[Console GM]   - " + suggestion);
                }
            } else {
                System.out.println("[Console GM] Unknown creature: " + creatureName);
                System.out.println("[Console GM] No similar creature names found");
            }
        } catch (Exception ex) {
            System.out.println("[Console GM] Unknown creature: " + creatureName);
            System.out.println("[Console GM] Failed to load creature list: " + ex.getMessage());
        }
    }

    /**
     * Handle #weather command - set weather.
     *
     * @param args Weather type (clear/light/medium/heavy)
     */
    private static void handleWeather(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #weather <type>");
            System.out.println("[Console GM] Types: clear, light, medium, heavy");
            System.out.println("[Console GM] Example: #weather clear");
            return;
        }

        String weatherType = args.trim().toLowerCase();
        int weatherCode;

        switch (weatherType) {
            case "clear":
                weatherCode = 0;
                break;
            case "light":
                weatherCode = 1;
                break;
            case "medium":
                weatherCode = 2;
                break;
            case "heavy":
                weatherCode = 3;
                break;
            default:
                System.out.println("[Console GM] Invalid weather type: " + args);
                System.out.println("[Console GM] Valid types: clear, light, medium, heavy");
                return;
        }

        try {
            ServerReflectionUtil.setWeather(weatherCode);
            System.out.println("[Console GM] ✓ Set weather to: " + weatherType);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to set weather: " + e.getMessage());
        }
    }

    /**
     * Handle #toggleglobal command - toggle global chat.
     *
     * @param args on/off
     */
    private static void handleToggleGlobal(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #toggleglobal <on|off>");
            System.out.println("[Console GM] Example: #toggleglobal on");
            return;
        }

        String toggle = args.trim().toLowerCase();
        boolean enabled;

        if (toggle.equals("on") || toggle.equals("true") || toggle.equals("1")) {
            enabled = true;
        } else if (toggle.equals("off") || toggle.equals("false") || toggle.equals("0")) {
            enabled = false;
        } else {
            System.out.println("[Console GM] Invalid option: " + args);
            System.out.println("[Console GM] Use: on or off");
            return;
        }

        try {
            ServerReflectionUtil.toggleGlobalChat(enabled);
            String status = enabled ? "enabled" : "disabled";
            System.out.println("[Console GM] ✓ Global chat " + status);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to toggle global chat: " + e.getMessage());
        }
    }

    /**
     * Handle #sendmessage command - send message to player.
     *
     * @param args Player name and message
     */
    private static void handleSendMessage(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #sendmessage <player> <message>");
            System.out.println("[Console GM] Example: #sendmessage Bob \"Welcome to the server!\"");
            return;
        }

        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("[Console GM] Usage: #sendmessage <player> <message>");
            return;
        }

        String playerName = parts[0];
        String message = parts[1].replaceAll("^\"|\"$", ""); // Remove quotes if present

        try {
            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not found: " + playerName);
                return;
            }

            String actualName = ServerReflectionUtil.getPlayerName(player);
            ServerReflectionUtil.sendMessage(player, message);

            System.out.println("[Console GM] ✓ Sent message to " + actualName + ": " + message);
        } catch (Exception e) {
            System.out.println("[Console GM] Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Shutdown the command processor.
     *
     * <p>Called during server shutdown to clean up resources.</p>
     */
    public static synchronized void shutdown() {
        if (!initialized) return;

        System.out.println("[Console GM] Shutting down command router...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        initialized = false;
    }
}

package com.garward.wurmmodloader.core.console;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.core.event.EventBus;

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

    private static final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    private static volatile Thread consumerThread;

    private static volatile boolean initialized = false;
    private static volatile boolean eventSubscribed = false;
    private static final ConsoleGMCommandRouter EVENT_SINK = new ConsoleGMCommandRouter();

    /**
     * Register a ServerStartedEvent subscriber that eagerly initializes the router
     * so the command banner prints right after server boot (instead of lazily on
     * first keystroke). Idempotent.
     */
    public static synchronized void installEagerInit() {
        if (eventSubscribed) return;
        EventBus.getInstance().register(EVENT_SINK);
        eventSubscribed = true;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Eagerly start the consumer thread so commands queued before the first
        // keystroke are ready to run. Banner printing is deferred to
        // CommandReaderPatch — it prints when the console reader loop actually
        // begins, which is the true "ready to accept input" moment.
        initialize();
    }

    private static final java.util.concurrent.atomic.AtomicBoolean bannerPrinted =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Print the command-ready banner. Idempotent — subsequent calls are no-ops.
     * Called from {@code CommandReaderPatch} at the top of {@code CommandReader.run},
     * guaranteeing the banner prints when the console is actually accepting input
     * (not just when the server core has finished booting).
     */
    public static void printBanner() {
        if (!bannerPrinted.compareAndSet(false, true)) return;
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║   CONSOLE GM COMMANDS READY                                  ║");
        System.out.println("║                                                              ║");
        System.out.println("║   >>  Type  #help  for the full command list             <<  ║");
        System.out.println("║   >>  Use   #shutdown <minutes> <reason>  to safe-save   <<  ║");
        System.out.println("║                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Initialize the command processor.
     *
     * <p>Starts a dedicated daemon thread that blocks on the command queue
     * and executes commands as they arrive. Using a blocking consumer instead
     * of a ScheduledExecutorService avoids the documented {@code scheduleAtFixedRate}
     * footgun where any uncaught {@code Throwable} silently suppresses all
     * future executions — that previously caused the CLI to stop responding
     * to {@code #} commands after idle time, requiring a force-close.</p>
     */
    public static synchronized void initialize() {
        if (initialized) return;

        // Auto-discover GM commands from Communicator
        GMCommandDiscovery.discoverCommands();

        consumerThread = new Thread(ConsoleGMCommandRouter::consumeLoop, "ConsoleGM-Consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        initialized = true;
        System.out.println("[Console GM] Command router initialized");
    }

    /**
     * Blocking consumer loop. Survives any {@link Throwable} thrown by a
     * command handler — only {@link InterruptedException} exits the loop.
     */
    private static void consumeLoop() {
        while (true) {
            String command;
            try {
                command = commandQueue.take();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                executeCommand(command);
            } catch (Throwable t) {
                System.out.println("[Console GM] Handler crashed on: " + command
                        + " — " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
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

            case "villageperm":
                handleVillagePerm(args);
                return true;

            case "setmayor":
                handleSetMayor(args);
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
        System.out.println("VILLAGES:");
        System.out.println("  #villageperm <deedname> <role> <permname> <on|off>");
        System.out.println("    Edit a village role permission directly (bypasses readonly GM Tool)");
        System.out.println("    role: everybody | mayor | citizen | ally");
        System.out.println("    permname: any setCanXxx suffix (Build, PassGates, MineIron, ManageRoles, ...)");
        System.out.println("  #setmayor <deedname> <playername>");
        System.out.println("    Make <player> mayor of <deed> (player must be online)");
        System.out.println("    Unlocks the in-game GM Tool role editor for that deed");
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
        System.out.println("  #villageperm \"Freedom Landing\" everybody Build off");
        System.out.println("  #villageperm \"Freedom Landing\" everybody MineIron on");
        System.out.println("  #setmayor \"Freedom Landing\" Rorann");
        System.out.println("========================================");
        System.out.println("IN-GAME GM TOOL (village roles, etc.):");
        System.out.println("  1. #setpower <you> 5   (power 4+ uses ebony wand 176; 2-3 uses ivory 315)");
        System.out.println("  2. #createitem <you> 176 99   (ebony wand; or 315 for ivory)");
        System.out.println("  3. In-game: activate wand (select + press A)");
        System.out.println("  4. Right-click any ITEM you own (not your body/avatar)");
        System.out.println("       - wand menu only surfaces on ItemBehaviour targets");
        System.out.println("  5. Creatures submenu -> GM Management");
        System.out.println("  6. Tick 'Start GM Tool?' checkbox -> submit");
        System.out.println("  7. GM Tool window -> Village -> pick deed -> Show Roles");
        System.out.println("       - edits non-citizen role + every citizen role, bypasses");
        System.out.println("         mayManageRoles (normal village UI does NOT bypass it).");
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
     * Parse an argument string that may contain a quoted first token.
     * Returns the first token (quoted or whitespace-delimited) and the remainder.
     */
    private static String[] splitQuotedFirst(String args) {
        if (args == null) return new String[]{"", ""};
        args = args.trim();
        if (args.isEmpty()) return new String[]{"", ""};
        if (args.startsWith("\"")) {
            int close = args.indexOf('"', 1);
            if (close < 0) return new String[]{args.substring(1), ""};
            String first = args.substring(1, close);
            String rest = close + 1 < args.length() ? args.substring(close + 1).trim() : "";
            return new String[]{first, rest};
        }
        String[] parts = args.split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
    }

    /**
     * Resolve a role-status argument ("everybody"/"mayor"/"citizen"/"ally"/numeric)
     * to the VillageRole status byte used by Village.getRoleForStatus().
     */
    private static byte resolveRoleStatus(String token) {
        if (token == null) return -1;
        switch (token.trim().toLowerCase()) {
            case "everybody":
            case "noncitizens":
            case "non-citizens":
            case "everyone":
                return 1;
            case "mayor":
                return 2;
            case "citizen":
            case "citizens":
                return 3;
            case "ally":
            case "allies":
                return 5;
            default:
                try { return Byte.parseByte(token); } catch (NumberFormatException e) { return -1; }
        }
    }

    /**
     * Handle #villageperm — reflectively set a permission on a village role and persist.
     * Bypasses the GM Tool's "readonly unless citizen" gate by editing the VillageRole
     * directly via the setCanXxx setters + role.save().
     *
     * Usage: #villageperm <deedname> <role> <permname> <on|off>
     *   role      = everybody | mayor | citizen | ally | <numeric status>
     *   permname  = Build, ManageRoles, DestroyItems, PassGates, MineIron, ... (suffix of setCanXxx)
     */
    private static void handleVillagePerm(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #villageperm <deedname> <role> <permname> <on|off>");
            System.out.println("[Console GM]   role: everybody | mayor | citizen | ally");
            System.out.println("[Console GM]   permname: any setCanXxx suffix (Build, PassGates, MineIron, ManageRoles, ...)");
            System.out.println("[Console GM] Example: #villageperm \"Freedom Landing\" everybody Build off");
            return;
        }
        String[] firstSplit = splitQuotedFirst(args);
        String deedName = firstSplit[0];
        String[] rest = firstSplit[1].split("\\s+", 3);
        if (deedName.isEmpty() || rest.length < 3) {
            System.out.println("[Console GM] Usage: #villageperm <deedname> <role> <permname> <on|off>");
            return;
        }
        byte status = resolveRoleStatus(rest[0]);
        if (status < 0) {
            System.out.println("[Console GM] Unknown role: " + rest[0] + " (expected everybody|mayor|citizen|ally|<byte>)");
            return;
        }
        String permName = rest[1];
        String onOff = rest[2].trim().toLowerCase();
        boolean value;
        if (onOff.equals("on") || onOff.equals("true") || onOff.equals("1") || onOff.equals("yes")) {
            value = true;
        } else if (onOff.equals("off") || onOff.equals("false") || onOff.equals("0") || onOff.equals("no")) {
            value = false;
        } else {
            System.out.println("[Console GM] Expected on|off, got: " + onOff);
            return;
        }

        try {
            Class<?> villagesClass = Class.forName("com.wurmonline.server.villages.Villages");
            Object village = villagesClass.getMethod("getVillage", String.class).invoke(null, deedName);
            if (village == null) {
                System.out.println("[Console GM] Village not found: " + deedName);
                return;
            }
            Object role = village.getClass().getMethod("getRoleForStatus", byte.class).invoke(village, status);
            if (role == null) {
                System.out.println("[Console GM] Role not found on village (status=" + status + ")");
                return;
            }

            // Try setCan<PermName>(boolean) — normalize common aliases.
            String methodName = "setCan" + capitalize(permName);
            java.lang.reflect.Method setter = findBoolSetter(role.getClass(), methodName);
            if (setter == null) {
                // Fallback: try setMay<PermName>
                setter = findBoolSetter(role.getClass(), "setMay" + capitalize(permName));
            }
            if (setter == null) {
                System.out.println("[Console GM] No setCan/setMay setter matching '" + permName + "' on VillageRole.");
                System.out.println("[Console GM] Hint: try Build, PassGates, MineIron, ManageRoles, DestroyItems, etc.");
                return;
            }
            setter.setAccessible(true);
            setter.invoke(role, value);
            role.getClass().getMethod("save").invoke(role);

            String villName = (String) village.getClass().getMethod("getName").invoke(village);
            System.out.println("[Console GM] ✓ " + villName + " [" + rest[0] + "] " + setter.getName()
                + "(" + value + ") saved.");
        } catch (Throwable t) {
            System.out.println("[Console GM] Failed to edit village perm: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static java.lang.reflect.Method findBoolSetter(Class<?> cls, String name) {
        for (java.lang.reflect.Method m : cls.getMethods()) {
            if (!m.getName().equalsIgnoreCase(name)) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 1 && (p[0] == boolean.class || p[0] == Boolean.class)) return m;
        }
        return null;
    }

    /**
     * Handle #setmayor — set a player as mayor of a deed and add them as a citizen
     * with the mayor role. Player must be online (citizen add needs a live Creature).
     *
     * Usage: #setmayor <deedname> <playername>
     */
    private static void handleSetMayor(String args) {
        if (args == null || args.trim().isEmpty()) {
            System.out.println("[Console GM] Usage: #setmayor <deedname> <playername>");
            System.out.println("[Console GM] Player must be online. Updates VILLAGES.MAYOR + adds citizen row with status=2.");
            return;
        }
        String[] firstSplit = splitQuotedFirst(args);
        String deedName = firstSplit[0];
        String playerName = firstSplit[1].trim();
        if (deedName.isEmpty() || playerName.isEmpty()) {
            System.out.println("[Console GM] Usage: #setmayor <deedname> <playername>");
            return;
        }

        try {
            Class<?> villagesClass = Class.forName("com.wurmonline.server.villages.Villages");
            Object village = villagesClass.getMethod("getVillage", String.class).invoke(null, deedName);
            if (village == null) {
                System.out.println("[Console GM] Village not found: " + deedName);
                return;
            }

            Object player = ServerReflectionUtil.getPlayerByName(playerName);
            if (player == null) {
                System.out.println("[Console GM] Player not online: " + playerName + " (must be online for addCitizen).");
                return;
            }

            Object mayorRole = village.getClass().getMethod("getRoleForStatus", byte.class).invoke(village, (byte) 2);
            if (mayorRole == null) {
                System.out.println("[Console GM] Village has no mayor role (status=2).");
                return;
            }

            Class<?> creatureClass = Class.forName("com.wurmonline.server.creatures.Creature");
            Class<?> roleClass = Class.forName("com.wurmonline.server.villages.VillageRole");
            java.lang.reflect.Method addCitizen = village.getClass().getMethod("addCitizen", creatureClass, roleClass);
            Boolean added = (Boolean) addCitizen.invoke(village, player, mayorRole);

            village.getClass().getMethod("setMayor", String.class).invoke(village, playerName);

            String villName = (String) village.getClass().getMethod("getName").invoke(village);
            System.out.println("[Console GM] ✓ " + playerName + " set as mayor of " + villName
                + " (addCitizen=" + added + ", MAYOR column updated).");
            System.out.println("[Console GM]   You can now edit roles in the in-game GM Tool (no longer readonly).");
        } catch (Throwable t) {
            System.out.println("[Console GM] Failed to set mayor: " + t.getMessage());
            t.printStackTrace();
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
        Thread t = consumerThread;
        if (t != null) {
            t.interrupt();
            try {
                t.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        consumerThread = null;
        initialized = false;
    }
}

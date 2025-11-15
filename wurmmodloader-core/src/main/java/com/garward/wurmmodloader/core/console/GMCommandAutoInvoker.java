package com.garward.wurmmodloader.core.console;

import java.lang.reflect.Method;

/**
 * Auto-invokes discovered GM commands via reflection.
 *
 * <p>Uses the first online GM player's communicator as context to execute
 * commands. This allows access to all 100+ GM commands without manual
 * implementation.</p>
 *
 * <p><strong>How It Works:</strong></p>
 * <ol>
 *   <li>Find first online player with GM power ≥ 2</li>
 *   <li>Get their Communicator object</li>
 *   <li>Invoke handleHashMessage* method on their communicator</li>
 *   <li>Command executes as if that GM typed it in-game</li>
 *   <li>Output goes to that GM's client chat</li>
 * </ol>
 *
 * <p><strong>Limitations:</strong></p>
 * <ul>
 *   <li>Requires at least 1 GM to be online (power ≥ 2)</li>
 *   <li>Command output goes to GM's game client, not console</li>
 *   <li>Some commands may have side effects on the GM player</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class GMCommandAutoInvoker {

    /**
     * Auto-invoke a discovered GM command.
     *
     * @param commandName Command name (e.g., "createitem")
     * @param fullCommand Full command string with arguments (e.g., "createitem 1 50")
     * @return true if invoked successfully, false if failed
     */
    public static boolean autoInvoke(String commandName, String fullCommand) {
        try {
            // Find an online GM player to use as context
            Object gmPlayer = findOnlineGMPlayer();

            if (gmPlayer == null) {
                System.out.println("[Console GM] Auto-invoke requires at least 1 GM (power ≥2) to be online");
                System.out.println("[Console GM] Please login as GM or use custom commands instead");
                return false;
            }

            String gmName = ServerReflectionUtil.getPlayerName(gmPlayer);
            int gmPower = ServerReflectionUtil.getPlayerPower(gmPlayer);

            // Get the GM's communicator
            Method getCommunicator = gmPlayer.getClass().getMethod("getCommunicator");
            Object communicator = getCommunicator.invoke(gmPlayer);

            // Get the handler method
            Method handler = GMCommandDiscovery.getCommandHandler(commandName);

            if (handler == null) {
                System.out.println("[Console GM] Command not found: " + commandName);
                return false;
            }

            // Make method accessible
            handler.setAccessible(true);

            // Determine parameters based on method signature
            Class<?>[] paramTypes = handler.getParameterTypes();

            if (paramTypes.length == 2) {
                // handleHashMessage(String message, int power)
                handler.invoke(communicator, "#" + fullCommand, gmPower);
            } else if (paramTypes.length == 1) {
                // handleHashMessage(String message)
                handler.invoke(communicator, "#" + fullCommand);
            } else if (paramTypes.length == 0) {
                // handleHashMessage()
                handler.invoke(communicator);
            } else {
                System.out.println("[Console GM] Unsupported method signature for: " + commandName);
                return false;
            }

            System.out.println("[Console GM] ✓ Command executed via GM context: " + gmName + " (power " + gmPower + ")");
            System.out.println("[Console GM] Output sent to player's game client");
            return true;

        } catch (Exception e) {
            System.out.println("[Console GM] Auto-invoke failed: " + e.getMessage());
            System.out.println("[Console GM] Error: " + e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Find first online player with GM power ≥ 2.
     *
     * @return GM player object or null if none found
     */
    private static Object findOnlineGMPlayer() {
        try {
            Object[] players = ServerReflectionUtil.getOnlinePlayers();

            if (players == null || players.length == 0) {
                return null;
            }

            // Find first GM with power ≥ 2
            for (Object player : players) {
                int power = ServerReflectionUtil.getPlayerPower(player);
                if (power >= 2) {
                    return player;
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if auto-invoke is available.
     *
     * @return true if at least 1 GM is online, false otherwise
     */
    public static boolean isAvailable() {
        return findOnlineGMPlayer() != null;
    }

    /**
     * Get the GM player that would be used for auto-invoke.
     *
     * @return GM player name or null if none available
     */
    public static String getAvailableGM() {
        try {
            Object gmPlayer = findOnlineGMPlayer();
            if (gmPlayer == null) {
                return null;
            }
            return ServerReflectionUtil.getPlayerName(gmPlayer);
        } catch (Exception e) {
            return null;
        }
    }
}

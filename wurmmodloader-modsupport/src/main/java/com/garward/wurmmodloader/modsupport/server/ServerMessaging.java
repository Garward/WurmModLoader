package com.garward.wurmmodloader.modsupport.server;

import com.wurmonline.server.MessageServer;

/**
 * Framework wrapper for server messaging operations.
 *
 * <p>Provides safe access to server broadcast functionality without requiring
 * mods to import Wurm classes directly.</p>
 *
 * @since 1.0.0
 */
public final class ServerMessaging {

    private ServerMessaging() {
        throw new AssertionError("ServerMessaging cannot be instantiated");
    }

    /**
     * Broadcasts a message to all players on the server.
     *
     * <p>This is a safe wrapper around {@code MessageServer.broadCastSafe()}.</p>
     *
     * @param message The message to broadcast
     * @param messageType The message type (0=normal, 1=event, 2=combat, etc.)
     */
    public static void broadcast(String message, byte messageType) {
        MessageServer.broadCastSafe(message, messageType);
    }

    /**
     * Broadcasts a message to all players as a normal message (type 0).
     *
     * @param message The message to broadcast
     */
    public static void broadcast(String message) {
        broadcast(message, (byte) 0);
    }

    /**
     * Broadcasts an event message to all players (type 1).
     *
     * @param message The event message to broadcast
     */
    public static void broadcastEvent(String message) {
        broadcast(message, (byte) 1);
    }
}

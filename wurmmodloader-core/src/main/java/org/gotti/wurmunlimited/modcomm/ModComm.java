package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.server.players.Player;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Legacy compat shim. The canonical ModComm implementation now lives at
 * {@link com.garward.wurmmodloader.modcomm.ModComm}. This class exists only so
 * that legacy Ago-style mods (and internal helpers in the intra-server bridge)
 * that still import {@code org.gotti.wurmunlimited.modcomm.*} keep compiling
 * and running. All registration, bytecode instrumentation, and packet routing
 * is delegated to the garward implementation.
 */
public class ModComm {
    // Retained only so legacy callers that reflect into these maps don't NPE.
    // Real state lives in com.garward.wurmmodloader.modcomm.ModComm.
    static final HashMap<String, Channel> channels = new HashMap<>();
    static final HashMap<Integer, Channel> idMap = new HashMap<>();

    private static final Logger logger = Logger.getLogger(ModComm.class.getName());

    /**
     * Register a legacy channel. The listener is wrapped in a garward adapter
     * and the channel is actually registered against the garward registry;
     * the returned gotti {@link Channel} is a thin wrapper over the garward
     * channel so legacy mod call sites keep their expected API.
     */
    public static Channel registerChannel(String name, IChannelListener listener) {
        com.garward.wurmmodloader.modcomm.IChannelListener adapter = new GotiListenerAdapter(listener);
        com.garward.wurmmodloader.modcomm.Channel garwardCh =
            com.garward.wurmmodloader.modcomm.ModComm.registerChannel(name, adapter);
        Channel legacyCh = new Channel(garwardCh);
        channels.put(name, legacyCh);
        idMap.put(garwardCh.getId(), legacyCh);
        logger.info("[Legacy ModComm] Delegated channel '" + name + "' to garward registry (id=" + garwardCh.getId() + ")");
        return legacyCh;
    }

    // === internal (no-ops — garward owns the real lifecycle) ===

    /** No-op. garward ModComm.init() performs the single Communicator instrumentation. */
    public static void init() {
        logger.fine("[Legacy ModComm] init() is a no-op; garward ModComm is canonical");
    }

    /** No-op. garward ModComm.serverStarted() resolves the Player.modConnection field. */
    public static void serverStarted() {
        logger.fine("[Legacy ModComm] serverStarted() is a no-op; garward ModComm is canonical");
    }

    /** No-op. garward ModComm.playerConnected() sends the banner. */
    public static void playerConnected(Player player) {
        logger.fine("[Legacy ModComm] playerConnected() is a no-op; garward ModComm is canonical");
    }

    /**
     * Expose the garward player-connection via a legacy-typed view so gotti
     * {@link Channel#isActiveForPlayer(Player)} still works for legacy callers.
     */
    static PlayerModConnection getPlayerConnection(Player player) {
        com.garward.wurmmodloader.modcomm.PlayerModConnection gw =
            com.garward.wurmmodloader.modcomm.ModComm.getPlayerConnectionPublic(player);
        return new PlayerModConnection(gw);
    }

    // === Logging passthroughs kept for backward source compat ===

    static void logException(String msg, Throwable e) {
        if (logger != null) logger.log(Level.SEVERE, msg, e);
    }

    static void logWarning(String msg) {
        if (logger != null) logger.log(Level.WARNING, msg);
    }

    static void logInfo(String msg) {
        if (logger != null) logger.log(Level.INFO, msg);
    }
}

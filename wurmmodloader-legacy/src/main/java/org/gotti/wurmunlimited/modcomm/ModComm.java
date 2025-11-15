package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.server.players.Player;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModComm {
    static final HashMap<String, Channel> channels = new HashMap<>();
    static final HashMap<Integer, Channel> idMap = new HashMap<>();

    private static int nextChannelId = 1;

    private static Field fPlayerConnection;

    private static final Logger logger = Logger.getLogger(ModComm.class.getName());

    /**
     * Register mod channel
     *
     * @param name     Unique identifier of the channel
     * @param listener Listener that will handle communication
     * @return new channel object
     */
    public static Channel registerChannel(String name, IChannelListener listener) {
        logger.info("[Legacy ModComm] DEBUG: registerChannel called for '" + name + "' with listener " + listener.getClass().getName());

        if (channels.containsKey(name))
            throw new RuntimeException(String.format("Channel %s already registered", name));

        Channel ch = new Channel(nextChannelId++, name, listener);
        idMap.put(ch.id, ch);
        channels.put(name, ch);

        logger.info("[Legacy ModComm] DEBUG: Registered channel '" + name + "' with ID " + ch.id + " (total channels: " + channels.size() + ")");
        return ch;
    }

    // === internal stuff ===

    /**
     * Get player connection state
     */
    static PlayerModConnection getPlayerConnection(Player player) {
        logger.info("[Legacy ModComm] DEBUG: getPlayerConnection() called for player " +
            (player != null ? player.getName() : "null") + ", fPlayerConnection is " +
            (fPlayerConnection != null ? "initialized" : "NULL"));

        if (fPlayerConnection == null) {
            logger.severe("[Legacy ModComm] ERROR: fPlayerConnection is null! ModComm.serverStarted() was not called or failed.");
            throw new IllegalStateException("ModComm not initialized - fPlayerConnection is null. Call ModComm.serverStarted() first.");
        }

        try {
            return (PlayerModConnection) fPlayerConnection.get(player);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal initialization, called from {@link org.gotti.wurmunlimited.modloader.ModLoader#loadModsFromModDir}
     */
    public static void init() {
        System.out.println("[Legacy ModComm] INIT: Starting ModComm initialization");
        logger.info("[Legacy ModComm] INIT: Starting ModComm initialization");
        final ClassPool classPool = HookManager.getInstance().getClassPool();
        try {
            CtClass ctPlayer = classPool.getCtClass("com.wurmonline.server.players.Player");
            System.out.println("[Legacy ModComm] INIT: Got Player CtClass, frozen=" + ctPlayer.isFrozen() + ", interface=" + ctPlayer.isInterface());
            logger.info("[Legacy ModComm] INIT: Got Player CtClass, frozen=" + ctPlayer.isFrozen() + ", interface=" + ctPlayer.isInterface());

            CtField fConnection = new CtField(classPool.get("org.gotti.wurmunlimited.modcomm.PlayerModConnection"), "modConnection", ctPlayer);
            fConnection.setModifiers(Modifier.PUBLIC);
            ctPlayer.addField(fConnection, "new org.gotti.wurmunlimited.modcomm.PlayerModConnection()");
            System.out.println("[Legacy ModComm] INIT: Added modConnection field to Player CtClass");
            logger.info("[Legacy ModComm] INIT: Added modConnection field to Player CtClass");

            // List all fields to confirm
            CtField[] fields = ctPlayer.getDeclaredFields();
            System.out.println("[Legacy ModComm] INIT: Player CtClass now has " + fields.length + " fields");
            logger.info("[Legacy ModComm] INIT: Player CtClass now has " + fields.length + " fields");
            for (CtField f : fields) {
                if (f.getName().equals("modConnection")) {
                    System.out.println("[Legacy ModComm] INIT: Confirmed modConnection field exists in CtClass");
                    logger.info("[Legacy ModComm] INIT: Confirmed modConnection field exists in CtClass");
                }
            }

            CtClass ctCommunicator = classPool.getCtClass("com.wurmonline.server.creatures.Communicator");
            ctCommunicator.getMethod("reallyHandle", "(ILjava/nio/ByteBuffer;)V").instrument(new ExprEditor() {
                private boolean first = true;

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("get") && first) {
                        m.replace("$_ = $proceed($$);" +
                                "if ($_ == " + ModCommConstants.CMD_MODCOMM + ") {" +
                                "   org.gotti.wurmunlimited.modcomm.ModCommHandler.handlePacket(player, byteBuffer);" +
                                "   return;" +
                                "}");
                        first = false;
                    }
                }
            });
        } catch (NotFoundException | CannotCompileException e) {
            throw new RuntimeException("Error initializing ModComm", e);
        }
    }

    /**
     * Internal late initialization, called from {@link org.gotti.wurmunlimited.modloader.server.ServerHook#fireOnServerStarted}
     */
    public static void serverStarted() {
        logger.info("[Legacy ModComm] DEBUG: serverStarted() called");

        // Debug: List all fields on Player class
        java.lang.reflect.Field[] allFields = Player.class.getDeclaredFields();
        logger.info("[Legacy ModComm] DEBUG: Player class has " + allFields.length + " fields total");
        boolean found = false;
        for (java.lang.reflect.Field f : allFields) {
            if (f.getName().equals("modConnection")) {
                logger.info("[Legacy ModComm] DEBUG: Found modConnection field in Player class!");
                found = true;
            }
        }
        if (!found) {
            logger.severe("[Legacy ModComm] ERROR: modConnection field NOT found in Player class fields!");
            logger.severe("[Legacy ModComm] ERROR: This means the Javassist modification didn't take effect");
        }

        try {
            fPlayerConnection = Player.class.getDeclaredField("modConnection");
            logger.info("[Legacy ModComm] DEBUG: Successfully got fPlayerConnection field: " + fPlayerConnection);
        } catch (NoSuchFieldException e) {
            logger.severe("[Legacy ModComm] ERROR: modConnection field not found on Player class");
            logger.severe("[Legacy ModComm] ERROR: Player class was likely loaded before Javassist modifications");
            throw new RuntimeException("Error initializing ModComm", e);
        }
    }

    /**
     * Player connected handler, called from {@link org.gotti.wurmunlimited.modloader.server.ServerHook#fireOnPlayerLogin}
     */
    public static void playerConnected(Player player) {
        logger.info("[Legacy ModComm] DEBUG: playerConnected called for " + player.getName());
        logger.info("[Legacy ModComm] DEBUG: channels.size() = " + channels.size());

        if (!channels.isEmpty()) {
            logger.info("[Legacy ModComm] DEBUG: Sending ModComm banner to player");
            player.getCommunicator().sendNormalServerMessage(ModCommConstants.BANNER);

            // List all registered channels for debugging
            for (String channelName : channels.keySet()) {
                logger.info("[Legacy ModComm] DEBUG: Registered channel: " + channelName);
            }
        } else {
            logger.info("[Legacy ModComm] DEBUG: No channels registered, skipping banner");
        }
    }

    // === Logging ===

    static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    static void logWarning(String msg) {
        if (logger != null)
            logger.log(Level.WARNING, msg);
    }

    static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

}

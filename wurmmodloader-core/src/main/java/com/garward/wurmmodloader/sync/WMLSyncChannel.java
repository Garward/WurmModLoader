package com.garward.wurmmodloader.sync;

import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.IChannelListener;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;
import com.wurmonline.server.players.Player;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WurmModLoader sync channel for client-server prediction coordination.
 *
 * <p>This ModComm channel enables:
 * <ul>
 *   <li>Client → Server: Movement intent and predicted position</li>
 *   <li>Server → Client: Position corrections when prediction deviates</li>
 * </ul>
 *
 * <p><b>Usage (Server-side Mod):</b>
 * <pre>{@code
 * @SubscribeEvent
 * public void onMovementIntent(MovementIntentReceivedEvent event) {
 *     Player player = event.getPlayer();
 *     long seqId = event.getSeqId();
 *     // Validate prediction and send correction if needed
 *     WMLSyncChannel.sendCorrection(player, seqId, correctX, correctY, correctHeight, reason);
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
public class WMLSyncChannel implements IChannelListener {
    private static final Logger logger = Logger.getLogger(WMLSyncChannel.class.getName());
    private static final String CHANNEL_NAME = "WML_SYNC";

    private static Channel channel;
    private static WMLSyncChannel instance;

    /**
     * Initialize the WML_SYNC channel. Called during server startup.
     */
    public static void initialize() {
        if (instance == null) {
            instance = new WMLSyncChannel();
            channel = ModComm.registerChannel(CHANNEL_NAME, instance);
            logger.info("[WMLSync] Registered WML_SYNC ModComm channel for client-server prediction");
        }
    }

    /**
     * Send a position correction to the client.
     *
     * @param player Player to send correction to
     * @param seqId Sequence ID of the prediction being corrected
     * @param x Correct X position
     * @param y Correct Y position
     * @param height Correct height
     * @param reason Reason for correction
     */
    public static void sendCorrection(Player player, long seqId, float x, float y, float height,
                                     ServerCorrectionMessage.CorrectionReason reason) {
        if (channel == null) {
            logger.warning("[WMLSync] Cannot send correction - channel not initialized");
            return;
        }

        if (!channel.isActiveForPlayer(player)) {
            logger.fine("[WMLSync] Cannot send correction to " + player.getName() + " - channel not active");
            return;
        }

        try {
            ServerCorrectionMessage message = new ServerCorrectionMessage(seqId, x, y, height, reason);

            // Create ByteBuffer for message
            ByteBuffer buffer = ByteBuffer.allocate(30); // 1 (type) + 8 (seqId) + 12 (3 floats) + 1 (reason)
            message.writeTo(buffer);
            buffer.flip();

            channel.sendMessage(player, buffer);

            logger.fine(String.format("[WMLSync] Sent correction to %s: seq=%d, pos=(%.2f, %.2f, %.2f), reason=%s",
                    player.getName(), seqId, x, y, height, reason));
        } catch (Exception e) {
            logger.log(Level.WARNING, "[WMLSync] Failed to send correction to " + player.getName(), e);
        }
    }

    // ========== IChannelListener Implementation ==========

    @Override
    public void handleMessage(Player player, ByteBuffer message) {
        try {
            if (!message.hasRemaining()) {
                logger.warning("[WMLSync] Received empty message from " + player.getName());
                return;
            }

            // Read message type
            byte typeId = message.get();
            WMLSyncMessageType type = WMLSyncMessageType.fromId(typeId);

            switch (type) {
                case MOVEMENT_INTENT:
                    handleMovementIntent(player, message);
                    break;

                case PREDICTION_STATE:
                    handlePredictionState(player, message);
                    break;

                case SERVER_CORRECTION:
                    // Server shouldn't receive corrections from client
                    logger.warning("[WMLSync] Received unexpected SERVER_CORRECTION from " + player.getName());
                    break;

                default:
                    logger.warning("[WMLSync] Unknown message type " + typeId + " from " + player.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "[WMLSync] Error handling message from " + player.getName(), e);
        }
    }

    @Override
    public void onPlayerConnected(Player player) {
        logger.info("[WMLSync] Player " + player.getName() + " connected to WML_SYNC channel");
    }

    // ========== Message Handlers ==========

    private void handleMovementIntent(Player player, ByteBuffer message) {
        MovementIntentMessage msg = MovementIntentMessage.readFrom(message);

        logger.fine("[WMLSync] Received from " + player.getName() + ": " + msg);

        // Fire event into EventBus via ProxyServerHook
        ProxyServerHook.fireMovementIntentReceivedEvent(player, msg.getSeqId(), msg.getInputState());
    }

    private void handlePredictionState(Player player, ByteBuffer message) {
        PredictionStateMessage msg = PredictionStateMessage.readFrom(message);

        logger.fine("[WMLSync] Received from " + player.getName() + ": " + msg);

        // Fire event into EventBus via ProxyServerHook
        ProxyServerHook.firePredictionStateReceivedEvent(player, msg.getSeqId(),
                msg.getPredictedX(), msg.getPredictedY(), msg.getPredictedHeight());
    }
}

package com.garward.wurmmodloader.api.events.sync;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.players.Player;

/**
 * Fired on the server when a client sends predicted position for debugging.
 *
 * <p>This optional event allows server-side prediction validation to compare
 * client predictions with authoritative results for tuning and analysis.
 *
 * <p>Example usage:
 * <pre>{@code
 * @SubscribeEvent
 * public void onPredictionState(PredictionStateReceivedEvent event) {
 *     float predictedX = event.getPredictedX();
 *     float actualX = event.getPlayer().getPosX();
 *     float error = Math.abs(predictedX - actualX);
 *     if (error > 1.0f) {
 *         // Prediction error exceeds threshold, send correction
 *     }
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
public class PredictionStateReceivedEvent extends Event {
    private final Player player;
    private final long seqId;
    private final float predictedX;
    private final float predictedY;
    private final float predictedHeight;

    public PredictionStateReceivedEvent(Player player, long seqId, float predictedX, float predictedY, float predictedHeight) {
        super(false); // Not cancellable
        this.player = player;
        this.seqId = seqId;
        this.predictedX = predictedX;
        this.predictedY = predictedY;
        this.predictedHeight = predictedHeight;
    }

    public Player getPlayer() {
        return player;
    }

    public long getSeqId() {
        return seqId;
    }

    public float getPredictedX() {
        return predictedX;
    }

    public float getPredictedY() {
        return predictedY;
    }

    public float getPredictedHeight() {
        return predictedHeight;
    }

    @Override
    public String toString() {
        return String.format("PredictionStateReceived[player=%s, seq=%d, predicted=(%.2f, %.2f, %.2f)]",
                player.getName(), seqId, predictedX, predictedY, predictedHeight);
    }
}

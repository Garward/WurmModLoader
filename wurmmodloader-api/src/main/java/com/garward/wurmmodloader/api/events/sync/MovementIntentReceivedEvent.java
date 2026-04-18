package com.garward.wurmmodloader.api.events.sync;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.players.Player;

/**
 * Fired on the server when a client sends movement intent via the WML_SYNC channel.
 *
 * <p>This event allows server-side prediction mods to track client inputs and
 * validate predictions against actual movement results.
 *
 * <p>Example usage:
 * <pre>{@code
 * @SubscribeEvent
 * public void onMovementIntent(MovementIntentReceivedEvent event) {
 *     Player player = event.getPlayer();
 *     long seqId = event.getSeqId();
 *     boolean moving = event.isMoveForward() || event.isMoveBackward();
 *     // Store intent for later validation
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
public class MovementIntentReceivedEvent extends Event {
    private final Player player;
    private final long seqId;
    private final byte inputState;

    // Input state bit flags (mirror of MovementIntentMessage)
    public static final byte FLAG_MOVE_FORWARD = 1 << 0;
    public static final byte FLAG_MOVE_BACKWARD = 1 << 1;
    public static final byte FLAG_MOVE_LEFT = 1 << 2;
    public static final byte FLAG_MOVE_RIGHT = 1 << 3;
    public static final byte FLAG_SPRINT = 1 << 4;
    public static final byte FLAG_SNEAK = 1 << 5;

    public MovementIntentReceivedEvent(Player player, long seqId, byte inputState) {
        super(false); // Not cancellable
        this.player = player;
        this.seqId = seqId;
        this.inputState = inputState;
    }

    public Player getPlayer() {
        return player;
    }

    public long getSeqId() {
        return seqId;
    }

    public byte getInputState() {
        return inputState;
    }

    public boolean isMoveForward() {
        return (inputState & FLAG_MOVE_FORWARD) != 0;
    }

    public boolean isMoveBackward() {
        return (inputState & FLAG_MOVE_BACKWARD) != 0;
    }

    public boolean isMoveLeft() {
        return (inputState & FLAG_MOVE_LEFT) != 0;
    }

    public boolean isMoveRight() {
        return (inputState & FLAG_MOVE_RIGHT) != 0;
    }

    public boolean isSprint() {
        return (inputState & FLAG_SPRINT) != 0;
    }

    public boolean isSneak() {
        return (inputState & FLAG_SNEAK) != 0;
    }

    @Override
    public String toString() {
        return String.format("MovementIntentReceived[player=%s, seq=%d, forward=%b, back=%b, left=%b, right=%b]",
                player.getName(), seqId, isMoveForward(), isMoveBackward(), isMoveLeft(), isMoveRight());
    }
}

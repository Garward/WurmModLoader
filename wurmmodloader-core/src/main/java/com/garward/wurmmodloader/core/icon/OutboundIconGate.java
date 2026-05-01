package com.garward.wurmmodloader.core.icon;

import com.garward.wurmmodloader.api.icon.Icon;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;

/**
 * Gating helper invoked by {@link com.garward.wurmmodloader.core.bytecode.patches.OutboundIconGatePatch
 * OutboundIconGatePatch} at every {@code bb.putShort(item.getImageNumber())}-style
 * write site in {@code Communicator}.
 *
 * <p>Vanilla clients (no client modloader) only know icon ids 0–1679 — the
 * sheets shipped with the base game. Sending them a dynamic icon id
 * (>= {@link Icon#FIRST_CUSTOM_ICON}) lands them in an empty atlas slot, which
 * renders as the broken-icon question mark. The dynamic-icon stack on the
 * client side rewrites these into real assets, but only on patched clients;
 * the server has to detect a vanilla client and substitute a fallback.
 *
 * <p>The detection key is the {@code com.garward.icons} ModComm channel: a
 * patched client registers it during handshake, vanilla doesn't. If the
 * channel isn't active for the recipient, this gate returns {@code 0} (the
 * vanilla "unknown" slot) so the player at least sees something deterministic
 * rather than a corrupt mid-sheet entry.
 *
 * <p>Vanilla-range ids (&lt; 1680) bypass the gate entirely — every Wurm
 * client knows those, modloader or not.
 *
 * @since 0.4.1
 */
public final class OutboundIconGate {

    private OutboundIconGate() {
        throw new AssertionError("OutboundIconGate is a static helper");
    }

    /**
     * Decide what icon id to actually emit on the wire for {@code raw} when
     * sent to the player behind {@code communicator}.
     *
     * <p>Returns {@code raw} unchanged if either the id is in the vanilla
     * range or the recipient client has the icon-sync channel active. Returns
     * {@code 0} otherwise.
     *
     * <p>Resilient by design — any failure resolving the player or testing
     * channel activity falls back to {@code raw}. The gate is on a hot path
     * (every inventory packet, every creature update) and must never throw.
     */
    public static short gate(short raw, Communicator communicator) {
        if (raw < Icon.FIRST_CUSTOM_ICON) {
            return raw;
        }
        if (communicator == null) {
            return raw;
        }
        try {
            Player player = communicator.getPlayer();
            if (player == null) {
                return raw;
            }
            if (IconRegistrySyncChannel.isActiveFor(player)) {
                return raw;
            }
            return 0;
        } catch (Throwable t) {
            return raw;
        }
    }
}

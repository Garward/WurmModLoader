package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.server.players.Player;

import java.nio.ByteBuffer;

/**
 * Legacy compat wrapper over {@link com.garward.wurmmodloader.modcomm.Channel}.
 * All behavior delegates to the garward channel; this class exists only so
 * legacy mods that import {@code org.gotti.wurmunlimited.modcomm.Channel} keep
 * compiling and working.
 */
public class Channel {
    final com.garward.wurmmodloader.modcomm.Channel delegate;

    Channel(com.garward.wurmmodloader.modcomm.Channel delegate) {
        this.delegate = delegate;
    }

    public void sendMessage(Player player, ByteBuffer message) {
        delegate.sendMessage(player, message);
    }

    public boolean isActiveForPlayer(Player player) {
        return delegate.isActiveForPlayer(player);
    }
}

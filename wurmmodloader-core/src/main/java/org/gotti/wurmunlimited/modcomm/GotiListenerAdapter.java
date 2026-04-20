package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.server.players.Player;

import java.nio.ByteBuffer;

/**
 * Adapts a legacy {@link IChannelListener} to the garward
 * {@link com.garward.wurmmodloader.modcomm.IChannelListener} interface so that
 * legacy channels can live on the canonical garward registry.
 */
final class GotiListenerAdapter implements com.garward.wurmmodloader.modcomm.IChannelListener {

    private final IChannelListener delegate;

    GotiListenerAdapter(IChannelListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handleMessage(Player player, ByteBuffer message) {
        delegate.handleMessage(player, message);
    }

    @Override
    public void onPlayerConnected(Player player) {
        delegate.onPlayerConnected(player);
    }
}

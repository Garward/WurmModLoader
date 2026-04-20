package org.gotti.wurmunlimited.modcomm;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Legacy compat wrapper over
 * {@link com.garward.wurmmodloader.modcomm.PlayerModConnection}. Exists only so
 * legacy Ago-style mods that reference {@code org.gotti...PlayerModConnection}
 * keep compiling. State is held by the garward connection.
 */
public class PlayerModConnection {
    private final com.garward.wurmmodloader.modcomm.PlayerModConnection delegate;

    PlayerModConnection(com.garward.wurmmodloader.modcomm.PlayerModConnection delegate) {
        this.delegate = delegate;
    }

    /** Default-construct: only used if a legacy caller instantiates directly. */
    public PlayerModConnection() {
        this.delegate = new com.garward.wurmmodloader.modcomm.PlayerModConnection();
    }

    public boolean isActive() {
        return delegate.isActive();
    }

    public byte getVersion() {
        return delegate.getVersion();
    }

    public Set<Channel> getChannels() {
        Set<com.garward.wurmmodloader.modcomm.Channel> gw = delegate.getChannels();
        if (gw == null) return Collections.emptySet();
        Set<Channel> out = new LinkedHashSet<>(gw.size());
        for (com.garward.wurmmodloader.modcomm.Channel ch : gw) {
            Channel legacy = ModComm.idMap.get(ch.getId());
            if (legacy != null) out.add(legacy);
        }
        return out;
    }
}

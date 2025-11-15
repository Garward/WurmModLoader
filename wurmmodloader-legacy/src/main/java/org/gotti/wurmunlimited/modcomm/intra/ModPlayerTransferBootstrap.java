package org.gotti.wurmunlimited.modcomm.intra;

import org.gotti.wurmunlimited.modcomm.intra.playertransfer.ModPlayerTransfer;

/**
 * Bootstrap wrapper that keeps ModPlayerTransfer initialization inside the
 * HookManager classloader so callbacks and bytecode proxies share the same
 * definition.
 */
public final class ModPlayerTransferBootstrap {

    private ModPlayerTransferBootstrap() {}

    public static void init() {
        ModPlayerTransfer.init();
    }

    public static void serverStarted() {
        ModPlayerTransfer.serverStarted();
    }
}

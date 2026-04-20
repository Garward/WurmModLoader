package com.garward.wurmmodloader.examples.hellomod;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import java.util.logging.Logger;

/**
 * Smallest possible WurmModLoader mod.
 *
 * <p>One class, one event handler, no Wurm imports. When the server finishes
 * starting, this prints a banner to the log. That's it.
 *
 * <p>Use this as a starting skeleton — copy the folder, rename, add events.
 * For a fully-featured tutorial mod (item registration, capabilities, combat
 * hooks), see {@code examples/oversizedclub/}.
 */
public class HelloMod implements WurmServerMod {

    private static final Logger logger = Logger.getLogger(HelloMod.class.getName());

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("===========================================");
        logger.info(" HelloMod loaded — WurmModLoader is alive. ");
        logger.info("===========================================");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}

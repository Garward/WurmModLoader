package com.garward.wurmmodloader.core.legacy;

import com.garward.wurmmodloader.api.events.base.EventPriority;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.item.ItemTemplatesCreatedEvent;
import com.garward.wurmmodloader.api.events.server.ServerPollEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.server.ServerStoppingEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.modloader.interfaces.ServerStartedListener;
import com.garward.wurmmodloader.modloader.interfaces.ServerShutdownListener;
import com.garward.wurmmodloader.modloader.interfaces.ServerPollListener;
import com.garward.wurmmodloader.modloader.interfaces.ItemTemplatesCreatedListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridge between legacy listener interfaces and the modern event system.
 *
 * <p>This class automatically detects when a mod implements legacy listener
 * interfaces (e.g., {@code ServerStartedListener}) and registers synthetic
 * event handlers that call the legacy methods when the corresponding events
 * are posted.
 *
 * <p>This ensures that old mods continue to work without modification while
 * new mods can use the modern event system.
 *
 * <h2>Supported Legacy Interfaces</h2>
 * <ul>
 *   <li>{@link ServerStartedListener} → {@link ServerStartedEvent}</li>
 *   <li>{@link ServerShutdownListener} → {@link ServerStoppingEvent}</li>
 *   <li>{@link ServerPollListener} → {@link ServerPollEvent}</li>
 *   <li>{@link ItemTemplatesCreatedListener} → {@link ItemTemplatesCreatedEvent}</li>
 * </ul>
 *
 * <p><b>Note:</b> Player and message-related listeners (PlayerLoginListener,
 * PlayerMessageListener, ChannelMessageListener) require runtime access to
 * Wurm server classes and are bridged at a later stage in DelegatedLauncher.
 *
 * @since 1.0.0
 */
public class LegacyListenerBridge {

    private static final Logger logger = Logger.getLogger(LegacyListenerBridge.class.getName());

    private final EventBus eventBus;

    /**
     * Creates a new LegacyListenerBridge.
     *
     * @param eventBus the event bus to register handlers with
     */
    public LegacyListenerBridge(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Registers legacy listeners for a mod.
     *
     * <p>Scans the mod for legacy listener interface implementations and
     * registers synthetic event handlers for each one.
     *
     * @param mod the mod to register legacy listeners for
     */
    public void registerLegacyListeners(Object mod) {
        if (mod == null) {
            return;
        }

        int registeredCount = 0;

        // Check for ServerStartedListener
        if (mod instanceof ServerStartedListener) {
            ServerStartedListener listener = (ServerStartedListener) mod;
            eventBus.register(new Object() {
                @SubscribeEvent
                public void onServerStarted(ServerStartedEvent event) {
                    listener.onServerStarted();
                }
            });
            registeredCount++;
            logger.log(Level.FINE, "Registered legacy ServerStartedListener for " +
                mod.getClass().getName());
        }

        // Check for ServerShutdownListener
        if (mod instanceof ServerShutdownListener) {
            ServerShutdownListener listener = (ServerShutdownListener) mod;
            eventBus.register(new Object() {
                @SubscribeEvent
                public void onServerStopping(ServerStoppingEvent event) {
                    listener.onServerShutdown();
                }
            });
            registeredCount++;
            logger.log(Level.FINE, "Registered legacy ServerShutdownListener for " +
                mod.getClass().getName());
        }

        // Check for ServerPollListener
        if (mod instanceof ServerPollListener) {
            ServerPollListener listener = (ServerPollListener) mod;
            eventBus.register(new Object() {
                @SubscribeEvent(priority = EventPriority.LOW)
                public void onServerPoll(ServerPollEvent event) {
                    listener.onServerPoll();
                }
            });
            registeredCount++;
            logger.log(Level.FINE, "Registered legacy ServerPollListener for " +
                mod.getClass().getName());
        }

        // Check for ItemTemplatesCreatedListener
        if (mod instanceof ItemTemplatesCreatedListener) {
            ItemTemplatesCreatedListener listener = (ItemTemplatesCreatedListener) mod;
            eventBus.register(new Object() {
                @SubscribeEvent
                public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
                    try {
                        listener.onItemTemplatesCreated();
                    } catch (RuntimeException e) {
                        // Some legacy mods try to use Javassist during ItemTemplatesCreated
                        // which causes "class is frozen" errors. Log at FINE level to avoid spam.
                        if (e.getMessage() != null && e.getMessage().contains("class is frozen")) {
                            logger.log(Level.FINE, "Legacy mod " + mod.getClass().getName() +
                                " attempted bytecode modification during ItemTemplatesCreated (should be in preInit)");
                        } else {
                            throw e; // Re-throw if it's a different error
                        }
                    }
                }
            });
            registeredCount++;
            logger.log(Level.FINE, "Registered legacy ItemTemplatesCreatedListener for " +
                mod.getClass().getName());
        }

        // Note: Player, Message, and Communicator-based listeners are not bridged here
        // because they require Wurm server classes that are not available at compile time.
        // These will need to be handled by the ServerHook integration point at runtime
        // where the actual Wurm classes are available.

        // TODO: Implement player/message event bridging in DelegatedLauncher where Wurm
        // classes are available, using reflection to avoid compile-time dependencies

        if (registeredCount > 0) {
            logger.log(Level.INFO, "Registered " + registeredCount +
                " legacy listener(s) for " + mod.getClass().getName());
        }
    }

    /**
     * Unregisters all legacy listeners for a mod.
     *
     * <p>Note: Currently not implemented as the synthetic listener objects
     * are anonymous and cannot be easily unregistered. This is typically not
     * a problem as mods are registered once at startup and never unregistered.
     *
     * @param mod the mod to unregister legacy listeners for
     */
    public void unregisterLegacyListeners(Object mod) {
        // TODO: Implement if needed - requires tracking synthetic listener objects
        logger.log(Level.WARNING, "Legacy listener unregistration not yet implemented");
    }
}

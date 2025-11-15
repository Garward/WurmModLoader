package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;


import com.garward.wurmmodloader.api.capability.Capability;

/**
 * Event fired during server initialization for registering capabilities.
 *
 * <p>Mods should subscribe to this event and register their capabilities
 * for Players, Creatures, Items, and Tiles.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void registerCapabilities(CapabilityRegistrationEvent event) {
 *     event.registerPlayerCapability(RPGStatsCapability.INSTANCE);
 *     event.registerItemCapability(SoulboundDataCapability.INSTANCE);
 *     event.registerCreatureCapability(CustomAICapability.INSTANCE);
 * }
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0 (Phase 5.5)
 */
public class CapabilityRegistrationEvent extends Event {

    public CapabilityRegistrationEvent() {
        super(false); // Not cancellable
    }

    /**
     * Register a capability that can be attached to Players.
     *
     * <p>Once registered, any Player object will be able to access this
     * capability via {@code player.getCapability(capability)}.</p>
     *
     * @param capability The capability to register
     * @param <T> The data type
     */
    public <T> void registerPlayerCapability(Capability<T> capability) {
        try {
            Object manager = getCapabilityManager();
            java.lang.reflect.Method method = manager.getClass().getMethod("registerPlayerCapability", Capability.class);
            method.invoke(manager, capability);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register player capability", e);
        }
    }

    /**
     * Register a capability that can be attached to Creatures.
     *
     * <p>Once registered, any Creature object will be able to access this
     * capability via {@code creature.getCapability(capability)}.</p>
     *
     * @param capability The capability to register
     * @param <T> The data type
     */
    public <T> void registerCreatureCapability(Capability<T> capability) {
        try {
            Object manager = getCapabilityManager();
            java.lang.reflect.Method method = manager.getClass().getMethod("registerCreatureCapability", Capability.class);
            method.invoke(manager, capability);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register creature capability", e);
        }
    }

    /**
     * Register a capability that can be attached to Items.
     *
     * <p>Once registered, any Item object will be able to access this
     * capability via {@code item.getCapability(capability)}.</p>
     *
     * @param capability The capability to register
     * @param <T> The data type
     */
    public <T> void registerItemCapability(Capability<T> capability) {
        try {
            Object manager = getCapabilityManager();
            java.lang.reflect.Method method = manager.getClass().getMethod("registerItemCapability", Capability.class);
            method.invoke(manager, capability);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register item capability", e);
        }
    }

    /**
     * Register a capability that can be attached to Tiles.
     *
     * <p>Once registered, any Tile object will be able to access this
     * capability via {@code tile.getCapability(capability)}.</p>
     *
     * @param capability The capability to register
     * @param <T> The data type
     */
    public <T> void registerTileCapability(Capability<T> capability) {
        try {
            Object manager = getCapabilityManager();
            java.lang.reflect.Method method = manager.getClass().getMethod("registerTileCapability", Capability.class);
            method.invoke(manager, capability);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register tile capability", e);
        }
    }

    /**
     * Get the capability manager instance.
     * This method will be implemented by reflection from CapabilityManager.
     *
     * @return The capability manager
     */
    private Object getCapabilityManager() {
        try {
            Class<?> managerClass = Class.forName("com.garward.wurmmodloader.core.capability.CapabilityManager");
            java.lang.reflect.Method getInstance = managerClass.getMethod("getInstance");
            return getInstance.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get CapabilityManager", e);
        }
    }
}

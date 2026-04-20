package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.capability.Capability;
import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when a capability instance is deserialized from disk for a given
 * entity (player, creature, item, or tile).
 *
 * <p>Subscribe to this when your mod needs to take action as soon as its
 * persisted state comes back into memory — e.g. re-applying derived stats,
 * reconciling tick state, or validating a creature's scaling data on load.</p>
 *
 * <p>The event does <strong>not</strong> fire for freshly-created default
 * instances; only when a value was restored from the capability database.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onLoaded(CapabilityLoadedEvent e) {
 *     if (e.getCapability() == CreaturePowerCapability.INSTANCE) {
 *         CreaturePowerData data = (CreaturePowerData) e.getInstance();
 *         // re-derive scaling from persisted base power...
 *     }
 * }
 * }</pre>
 */
public class CapabilityLoadedEvent extends Event {

    /** Entity kinds a capability can be attached to. */
    public enum EntityType { PLAYER, CREATURE, ITEM, TILE }

    private final EntityType entityType;
    private final long entityId;
    private final Capability<?> capability;
    private final Object instance;

    public CapabilityLoadedEvent(EntityType entityType, long entityId,
                                 Capability<?> capability, Object instance) {
        super(false);
        this.entityType = entityType;
        this.entityId = entityId;
        this.capability = capability;
        this.instance = instance;
    }

    public EntityType getEntityType() { return entityType; }
    public long getEntityId() { return entityId; }
    public Capability<?> getCapability() { return capability; }
    public Object getInstance() { return instance; }
}

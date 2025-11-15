package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;


import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when checking if a creature's equipment is compatible for mounting.
 *
 * <p>This event is fired for each equipped item when checking if a creature can be mounted.
 * Vanilla checks include:
 * <ul>
 *   <li>Saddle size matches creature size</li>
 *   <li>Horseshoes only on horses/unicorns</li>
 *   <li>Barding only on horses</li>
 * </ul>
 *
 * <p>Mods can use this event to:
 * <ul>
 *   <li>Add custom equipment validation rules</li>
 *   <li>Allow or deny specific equipment combinations</li>
 *   <li>Create custom mount-specific gear requirements</li>
 * </ul>
 *
 * <p>Canceling this event marks the equipment as incompatible, preventing mounting.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class MountEquipmentCheckEvent extends CancellableEvent {

    private final Creature mount;
    private final Item equipment;
    private String incompatibilityReason;

    /**
     * Constructor.
     *
     * @param mount The creature being checked for mounting
     * @param equipment The equipped item being validated
     */
    public MountEquipmentCheckEvent(Creature mount, Item equipment) {
        this.mount = mount;
        this.equipment = equipment;
        this.incompatibilityReason = null;
    }

    /**
     * Get the mount creature being checked.
     *
     * @return The mount
     */
    public Creature getMount() {
        return mount;
    }

    /**
     * Get the equipment item being validated.
     *
     * @return The equipped item
     */
    public Item getEquipment() {
        return equipment;
    }

    /**
     * Mark equipment as incompatible with a custom message.
     *
     * <p>This prevents mounting and displays the reason to the player.</p>
     *
     * @param reason The incompatibility reason
     */
    public void markIncompatible(String reason) {
        cancel();
        this.incompatibilityReason = reason;
    }

    /**
     * Get the incompatibility reason if equipment was marked incompatible.
     *
     * @return The reason, or null if compatible or no reason provided
     */
    public String getIncompatibilityReason() {
        return incompatibilityReason;
    }

    /**
     * Check if the equipment is compatible (not cancelled).
     *
     * @return true if equipment is compatible with mounting
     */
    public boolean isCompatible() {
        return !isCancelled();
    }
}

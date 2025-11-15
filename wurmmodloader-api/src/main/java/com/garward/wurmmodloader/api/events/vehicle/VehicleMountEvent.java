package com.garward.wurmmodloader.api.events.vehicle;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;


import com.wurmonline.server.behaviours.Vehicle;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when a creature attempts to mount or board a vehicle.
 *
 * <p>This event is fired before the mount action is created, allowing mods to:
 * <ul>
 *   <li>Cancel mounting (e.g., level requirements, quest restrictions)</li>
 *   <li>Modify seat assignment</li>
 *   <li>Track mount/dismount actions</li>
 *   <li>Apply custom mount effects</li>
 * </ul>
 *
 * <p>Vehicle can be either a Creature (horse, unicorn, etc.) or an Item (cart, boat, etc.).</p>
 *
 * <p>Canceling this event prevents the mount action from occurring.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class VehicleMountEvent extends CancellableEvent {

    private final Creature rider;
    private final Creature mountCreature; // null if mounting item
    private final Item mountItem;         // null if mounting creature
    private final Vehicle vehicle;
    private int seatNumber;
    private final boolean asDriver;
    private String cancelReason;

    /**
     * Constructor for mounting a creature (horse, unicorn, etc.).
     *
     * @param rider The creature mounting
     * @param mount The creature being mounted
     * @param vehicle The vehicle data
     * @param seatNumber The seat being occupied
     * @param asDriver Whether mounting as driver/pilot (vs passenger)
     */
    public VehicleMountEvent(Creature rider, Creature mount, Vehicle vehicle, int seatNumber, boolean asDriver) {
        this.rider = rider;
        this.mountCreature = mount;
        this.mountItem = null;
        this.vehicle = vehicle;
        this.seatNumber = seatNumber;
        this.asDriver = asDriver;
        this.cancelReason = null;
    }

    /**
     * Constructor for mounting an item (cart, boat, etc.).
     *
     * @param rider The creature mounting
     * @param mount The item being mounted
     * @param vehicle The vehicle data
     * @param seatNumber The seat being occupied
     * @param asDriver Whether mounting as driver/pilot (vs passenger)
     */
    public VehicleMountEvent(Creature rider, Item mount, Vehicle vehicle, int seatNumber, boolean asDriver) {
        this.rider = rider;
        this.mountCreature = null;
        this.mountItem = mount;
        this.vehicle = vehicle;
        this.seatNumber = seatNumber;
        this.asDriver = asDriver;
        this.cancelReason = null;
    }

    /**
     * Get the creature attempting to mount.
     *
     * @return The rider
     */
    public Creature getRider() {
        return rider;
    }

    /**
     * Get the creature being mounted (if mounting a creature).
     *
     * @return The mount creature, or null if mounting an item
     */
    public Creature getMountCreature() {
        return mountCreature;
    }

    /**
     * Get the item being mounted (if mounting an item).
     *
     * @return The mount item, or null if mounting a creature
     */
    public Item getMountItem() {
        return mountItem;
    }

    /**
     * Check if mounting a creature (vs an item).
     *
     * @return true if mounting a creature, false if mounting an item
     */
    public boolean isMountingCreature() {
        return mountCreature != null;
    }

    /**
     * Check if mounting an item (vs a creature).
     *
     * @return true if mounting an item, false if mounting a creature
     */
    public boolean isMountingItem() {
        return mountItem != null;
    }

    /**
     * Get the vehicle data.
     *
     * @return The vehicle
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * Get the seat number being occupied.
     *
     * @return The seat number
     */
    public int getSeatNumber() {
        return seatNumber;
    }

    /**
     * Set the seat number to occupy.
     *
     * <p>Mods can use this to redirect riders to different seats.</p>
     *
     * @param seatNumber The new seat number
     */
    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    /**
     * Check if mounting as driver/pilot.
     *
     * @return true if mounting as driver, false if mounting as passenger
     */
    public boolean isAsDriver() {
        return asDriver;
    }

    /**
     * Check if this is a player mounting.
     *
     * @return true if rider is a player
     */
    public boolean isPlayerMounting() {
        return rider != null && rider.isPlayer();
    }

    /**
     * Cancel mounting with a custom message to display to the player.
     *
     * <p>The message will be sent to the player's communicator.</p>
     *
     * @param reason The reason message to display
     */
    public void cancel(String reason) {
        cancel();
        this.cancelReason = reason;
    }

    /**
     * Get the cancel reason if one was provided.
     *
     * @return The cancel reason, or null if not cancelled or no reason provided
     */
    public String getCancelReason() {
        return cancelReason;
    }
}

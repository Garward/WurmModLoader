package com.garward.wurmmodloader.api.events.vehicle;

import com.garward.wurmmodloader.api.events.base.Event;


import com.wurmonline.server.behaviours.Vehicle;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when calculating the speed of a vehicle or mount.
 *
 * <p>This event allows mods to modify vehicle speed based on:
 * <ul>
 *   <li>Custom equipment (horseshoes, saddles, vehicle upgrades)</li>
 *   <li>Buffs and enchantments</li>
 *   <li>Environmental conditions</li>
 *   <li>Creature stats or item quality</li>
 * </ul>
 *
 * <p>The speed multiplier is cumulative - multiple mods can adjust it.
 * The final speed is: baseSpeed * speedMultiplier</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class VehicleSpeedCalculationEvent extends Event {

    private final Vehicle vehicle;
    private final Creature mountCreature; // null for item vehicles
    private final Item mountItem;         // null for creature mounts
    private final Creature rider;
    private final float baseSpeed;
    private float speedMultiplier;
    private final boolean mounting; // true if calculating during mount, false if recalculating

    /**
     * Constructor for creature mount speed calculation.
     *
     * @param vehicle The vehicle data
     * @param mount The creature being ridden
     * @param rider The rider (pilot)
     * @param baseSpeed The base speed before modifications
     * @param mounting Whether this is calculated during mounting (vs recalculation)
     */
    public VehicleSpeedCalculationEvent(Vehicle vehicle, Creature mount,  Creature rider,
                                        float baseSpeed, boolean mounting) {
        this.vehicle = vehicle;
        this.mountCreature = mount;
        this.mountItem = null;
        this.rider = rider;
        this.baseSpeed = baseSpeed;
        this.speedMultiplier = 1.0f;
        this.mounting = mounting;
    }

    /**
     * Constructor for item vehicle speed calculation.
     *
     * @param vehicle The vehicle data
     * @param mount The item being used as vehicle
     * @param rider The rider (pilot)
     * @param baseSpeed The base speed before modifications
     * @param mounting Whether this is calculated during mounting (vs recalculation)
     */
    public VehicleSpeedCalculationEvent(Vehicle vehicle, Item mount,  Creature rider,
                                        float baseSpeed, boolean mounting) {
        this.vehicle = vehicle;
        this.mountCreature = null;
        this.mountItem = mount;
        this.rider = rider;
        this.baseSpeed = baseSpeed;
        this.speedMultiplier = 1.0f;
        this.mounting = mounting;
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
     * Get the creature being mounted (if this is a creature mount).
     *
     * @return The mount creature, or null if this is an item vehicle
     */
    
    public Creature getMountCreature() {
        return mountCreature;
    }

    /**
     * Get the item being used as vehicle (if this is an item vehicle).
     *
     * @return The mount item, or null if this is a creature mount
     */
    
    public Item getMountItem() {
        return mountItem;
    }

    /**
     * Check if this is a creature mount (vs item vehicle).
     *
     * @return true if creature mount, false if item vehicle
     */
    public boolean isCreatureMount() {
        return mountCreature != null;
    }

    /**
     * Check if this is an item vehicle (vs creature mount).
     *
     * @return true if item vehicle, false if creature mount
     */
    public boolean isItemVehicle() {
        return mountItem != null;
    }

    /**
     * Get the rider/pilot.
     *
     * @return The rider, or null if no rider
     */
    
    public Creature getRider() {
        return rider;
    }

    /**
     * Get the base speed before modifications.
     *
     * @return The base speed
     */
    public float getBaseSpeed() {
        return baseSpeed;
    }

    /**
     * Get the current speed multiplier.
     *
     * <p>Default is 1.0 (no modification). Mods can multiply this value.</p>
     *
     * @return The speed multiplier
     */
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Set the speed multiplier.
     *
     * <p>To add a +10% bonus: setSpeedMultiplier(getSpeedMultiplier() * 1.1f)</p>
     * <p>To add a -20% penalty: setSpeedMultiplier(getSpeedMultiplier() * 0.8f)</p>
     *
     * @param speedMultiplier The new speed multiplier
     */
    public void setSpeedMultiplier(float speedMultiplier) {
        this.speedMultiplier = Math.max(0.0f, speedMultiplier); // Prevent negative speed
    }

    /**
     * Add a flat bonus to the speed multiplier.
     *
     * <p>For example, addSpeedBonus(0.1f) adds +10% speed.</p>
     *
     * @param bonus The bonus to add (can be negative for penalties)
     */
    public void addSpeedBonus(float bonus) {
        this.speedMultiplier += bonus;
        this.speedMultiplier = Math.max(0.0f, this.speedMultiplier);
    }

    /**
     * Multiply the current speed multiplier by a factor.
     *
     * <p>For example, multiplySpeed(1.2f) increases speed by 20%.</p>
     *
     * @param factor The factor to multiply by
     */
    public void multiplySpeed(float factor) {
        this.speedMultiplier *= factor;
        this.speedMultiplier = Math.max(0.0f, this.speedMultiplier);
    }

    /**
     * Check if this calculation is happening during mounting.
     *
     * <p>When true, this is the initial mount. When false, speed is being recalculated.</p>
     *
     * @return true if mounting, false if recalculating
     */
    public boolean isMounting() {
        return mounting;
    }

    /**
     * Get the calculated final speed.
     *
     * @return base speed * speed multiplier
     */
    public float getFinalSpeed() {
        return baseSpeed * speedMultiplier;
    }

    /**
     * Check if a player is riding.
     *
     * @return true if rider is a player
     */
    public boolean hasPlayerRider() {
        return rider != null && rider.isPlayer();
    }
}

package com.garward.wurmmodloader.modsupport.vehicles;

import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.Seat;
import com.wurmonline.server.items.Item;

/**
 * A facade interface for interacting with vehicle entities in the Wurm Online server environment.
 *
 * <p>This interface provides methods to configure and manipulate vehicle properties such as
 * passenger seats, movement capabilities, and interaction behaviors. It serves as an abstraction
 * layer between mod code and the internal Wurm server vehicle implementation.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * public class CustomVehicleMod implements WurmServerMod {
 *     public void onVehicleCreated(VehicleFacade vehicle) {
 *         vehicle.setName("Custom Boat");
 *         vehicle.createPassengerSeats(4);
 *         vehicle.setSeatFightMod(0, 1.0f, 0.5f);
 *         vehicle.setMaxSpeed(20.0f);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong> VehicleFacade instances are typically created during vehicle
 * initialization and remain valid for the duration of the vehicle's existence in the game world.</p>
 *
 * <p><strong>Thread Safety:</strong> Implementations of this interface are not guaranteed to be
 * thread-safe. All method calls should be made from the main server thread unless otherwise
 * specified by the implementation.</p>
 *
 * @since 1.0.0
 * @see com.wurmonline.server.behaviours.Vehicle
 * @see com.wurmonline.server.items.Item
 */
public interface VehicleFacade {

    /**
     * Sets whether this vehicle can be unmounted by players.
     *
     * <p>When set to {@code true}, players can dismount from the vehicle using normal
     * dismount interactions. When {@code false}, special conditions may be required
     * to dismount.</p>
     *
     * @param b {@code true} to allow unmounting, {@code false} to prevent it
     * @since 1.0.0
     */
    void setUnmountable(boolean b);

    /**
     * Creates passenger seats for this vehicle with a restriction that only these seats
     * can be used for passengers.
     *
     * <p>This method creates exactly {@code i} passenger seats and marks the vehicle
     * as having exclusive passenger seating, meaning no additional seats can be added
     * beyond these initial {@code i} seats.</p>
     *
     * @param i the number of passenger seats to create
     * @since 1.0.0
     * @see #createPassengerSeats(int)
     */
    void createOnlyPassengerSeats(int i);

    /**
     * Sets the combat modifiers for a specific seat on this vehicle.
     *
     * <p>These modifiers affect combat abilities while seated, such as accuracy and
     * damage modifiers when fighting from this seat position.</p>
     *
     * @param i the seat index to modify
     * @param f the offensive combat modifier (e.g., accuracy)
     * @param g the defensive combat modifier (e.g., dodge chance)
     * @since 1.0.0
     * @see com.wurmonline.server.behaviours.Seat
     */
    void setSeatFightMod(int i, float f, float g);

    /**
     * Sets whether this vehicle represents a creature entity.
     *
     * <p>When {@code true}, this vehicle is treated as a living creature for
     * game mechanics purposes, potentially affecting how it interacts with
     * spells, enchantments, and other creature-specific systems.</p>
     *
     * @param b {@code true} if this vehicle represents a creature, {@code false} otherwise
     * @since 1.0.0
     */
    void setCreature(boolean b);

    /**
     * Sets the string displayed when a player attempts to embark (board) this vehicle.
     *
     * <p>This string is shown in the action menu when right-clicking the vehicle
     * to board it. For example, "Board ship" or "Mount horse".</p>
     *
     * @param string the embark action string to display
     * @since 1.0.0
     */
    void setEmbarkString(String string);

    /**
     * Sets the display name of this vehicle.
     *
     * <p>This name is used in player interfaces, chat messages, and other UI elements
     * when referring to this vehicle. It does not affect the underlying item name.</p>
     *
     * @param name the display name for this vehicle
     * @since 1.0.0
     */
    void setName(String name);

    /**
     * Sets the maximum depth this vehicle can traverse underwater.
     *
     * <p>Vehicles with higher max depth values can submerge deeper before
     * experiencing negative effects or being destroyed.</p>
     *
     * @param f the maximum depth in meters
     * @since 1.0.0
     */
    void setMaxDepth(float f);

    /**
     * Sets the maximum height difference this vehicle can traverse.
     *
     * <p>This affects how steep inclines the vehicle can climb or descend
     * without becoming stuck or damaged.</p>
     *
     * @param f the maximum height difference allowed
     * @since 1.0.0
     */
    void setMaxHeightDiff(float f);

    /**
     * Sets the command type for this vehicle.
     *
     * <p>The command type determines how this vehicle responds to movement
     * and control commands from players. Different values may represent
     * different control schemes or vehicle behaviors.</p>
     *
     * @param i the command type identifier
     * @since 1.0.0
     */
    void setCommandType(byte i);

    /**
     * Adds hitch seats to this vehicle for attaching other vehicles or creatures.
     *
     * <p>Hitch seats allow this vehicle to pull or be pulled by other entities
     * in the game world, such as caravans or plows.</p>
     *
     * @param hitches array of {@link Seat} objects representing hitch points
     * @since 1.0.0
     * @see com.wurmonline.server.behaviours.Seat
     */
    void addHitchSeats(Seat[] hitches);

    /**
     * Creates additional passenger seats for this vehicle.
     *
     * <p>Unlike {@link #createOnlyPassengerSeats(int)}, this method allows
     * for adding passenger seats to existing vehicles without restriction.</p>
     *
     * @param i the number of passenger seats to create
     * @since 1.0.0
     * @see #createOnlyPassengerSeats(int)
     */
    void createPassengerSeats(int i);

    /**
     * Sets the offset position for a specific seat on this vehicle.
     *
     * <p>This defines where passengers appear when seated, affecting both
     * visual positioning and interaction hitboxes.</p>
     *
     * @param i the seat index to modify
     * @param f the X coordinate offset
     * @param g the Y coordinate offset
     * @param h the Z coordinate offset
     * @since 1.0.0
     */
    void setSeatOffset(int i, float f, float g, float h);

    /**
     * Sets the skill requirement needed to operate this vehicle effectively.
     *
     * <p>Players with skill levels below this value may experience penalties
     * or be unable to use certain vehicle functions.</p>
     *
     * @param f the minimum skill level required
     * @since 1.0.0
     */
    void setSkillNeeded(float f);

    /**
     * Sets the maximum speed this vehicle can achieve.
     *
     * <p>Speed values are typically measured in meters per second and affect
     * how quickly the vehicle can traverse the game world.</p>
     *
     * @param f the maximum speed value
     * @since 1.0.0
     */
    void setMaxSpeed(float f);

    /**
     * Sets whether this vehicle can have equipment attached to it.
     *
     * <p>Equipment might include things like saddles, reins, or other
     * vehicle-specific gear that can be equipped by players.</p>
     *
     * @param b {@code true} to allow equipment, {@code false} to prevent it
     * @since 1.0.0
     */
    void setCanHaveEquipment(boolean b);

    /**
     * Gets the underlying {@link Item} representing this vehicle in the game world.
     *
     * <p>This method provides access to the physical item object that represents
     * the vehicle, allowing for direct manipulation of item properties.</p>
     *
     * @return the {@link Item} representing this vehicle
     * @throws NoSuchItemException if the underlying item no longer exists
     * @since 1.0.0
     * @see com.wurmonline.server.items.Item
     */
    Item getItem() throws NoSuchItemException;

    /**
     * Gets the Wurm ID of this vehicle.
     *
     * <p>The Wurm ID is the unique identifier for this vehicle entity within
     * the game world, corresponding to the ID of the underlying item.</p>
     *
     * @return the unique Wurm ID for this vehicle
     * @since 1.0.0
     */
    long getWurmid();
}
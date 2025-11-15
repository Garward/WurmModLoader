package com.garward.wurmmodloader.modsupport.vehicles;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.garward.wurmmodloader.modloader.internal.ReflectionUtil;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.Seat;
import com.wurmonline.server.behaviours.Vehicle;
import com.wurmonline.server.items.Item;

/**
 * Implementation of {@link VehicleFacade} that provides a simplified interface for modifying
 * {@link Vehicle} objects through reflection. This facade allows mod authors to customize
 * vehicle properties without directly accessing private fields and methods.
 * 
 * <p>This class is designed to be used during the vehicle initialization phase, typically
 * within mod loader hooks or vehicle creation events. It provides safe access to vehicle
 * internals while handling reflection complexity internally.
 * 
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * // In a mod loader hook or vehicle initialization method
 * Vehicle vehicle = ... // obtain vehicle instance
 * VehicleFacade facade = new VehicleFacadeImpl(vehicle);
 * 
 * // Customize vehicle properties
 * facade.setName("Custom Boat");
 * facade.setEmbarkString("boards the vessel");
 * facade.setMaxSpeed(2.5f);
 * facade.createPassengerSeats(4);
 * }</pre>
 * 
 * <p><strong>Lifecycle notes:</strong>
 * This facade should be instantiated when a vehicle is being created or initialized.
 * Modifications made through this facade take effect immediately but should be done
 * before the vehicle is fully operational to avoid inconsistent states.
 * 
 * <p><strong>Thread-safety:</strong>
 * This implementation is not thread-safe. All method calls should be made from the
 * main game thread to ensure consistency with the game state.
 * 
 * @since 1.0.0
 * @see VehicleFacade
 * @see Vehicle
 */
public class VehicleFacadeImpl implements VehicleFacade {

	private static final Method createOnlyPassengerSeats;
	private static final Method createPassengerSeats;
	private static final Method getWurmid;
	private static final Field embarkString;
	private static final Field maxSpeed;
	private static final Field canHaveEquipment;

	static {
		try {
			createOnlyPassengerSeats = ReflectionUtil.getMethod(Vehicle.class, "createOnlyPassengerSeats");
			createPassengerSeats = ReflectionUtil.getMethod(Vehicle.class, "createPassengerSeats");
			getWurmid = ReflectionUtil.getMethod(Vehicle.class, "getWurmid", new Class[] {});
			embarkString = ReflectionUtil.getField(Vehicle.class, "embarkString");
			maxSpeed = ReflectionUtil.getField(Vehicle.class, "maxSpeed");
			canHaveEquipment = ReflectionUtil.getField(Vehicle.class, "canHaveEquipment");
		} catch (NoSuchMethodException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	private Vehicle v;

	/**
	 * Constructs a new VehicleFacadeImpl instance for the specified vehicle.
	 * 
	 * @param v the vehicle to be wrapped by this facade
	 * @throws IllegalArgumentException if v is null
	 * @since 1.0.0
	 */
	public VehicleFacadeImpl(Vehicle v) {
		this.v = v;
	}

	/**
	 * Calls a private method on the wrapped vehicle instance with the provided arguments.
	 * 
	 * <p>This is an internal utility method used by other methods in this class to
	 * invoke private vehicle methods through reflection.
	 * 
	 * @param <T> the return type of the method
	 * @param method the method to invoke
	 * @param args the arguments to pass to the method
	 * @return the result of the method invocation
	 * @throws RuntimeException if the method invocation fails due to reflection issues
	 * @since 1.0.0
	 */
	public <T> T callPrivateMethod(Method method, Object... args) {
		try {
			return ReflectionUtil.callPrivateMethod(v, method, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets a private field on the wrapped vehicle instance to the specified value.
	 * 
	 * <p>This is an internal utility method used by other methods in this class to
	 * modify private vehicle fields through reflection.
	 * 
	 * @param <T> the type of the value to set
	 * @param field the field to modify
	 * @param value the value to set the field to
	 * @throws RuntimeException if setting the field fails due to reflection issues
	 * @since 1.0.0
	 */
	public <T> void setPrivateField(Field field, T value) {
		try {
			ReflectionUtil.setPrivateField(v, field, value);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets whether the vehicle can be unmounted. When set to true, players
	 * will not be able to dismount from this vehicle normally.
	 * 
	 * @param b true to make the vehicle unmountable, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public void setUnmountable(boolean b) {
		v.setUnmountable(b);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Creates the specified number of passenger seats that can only be used
	 * for passengers (not for driving/control). This is typically used for
	 * vehicles that have separate driver and passenger positions.
	 * 
	 * @param i the number of passenger-only seats to create
	 * @since 1.0.0
	 */
	@Override
	public void createOnlyPassengerSeats(int i) {
		callPrivateMethod(createOnlyPassengerSeats, Integer.valueOf(i));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the fight modifier values for the specified seat. These modifiers
	 * affect combat abilities when a creature or player is seated in this position.
	 * 
	 * @param i the seat index to modify
	 * @param f the fight skill modifier
	 * @param a the attack skill modifier
	 * @since 1.0.0
	 */
	@Override
	public void setSeatFightMod(int i, float f, float a) {
		v.setSeatFightMod(i, f, a);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets whether this vehicle represents a creature. This affects various
	 * game mechanics related to how the vehicle is treated in the world.
	 * 
	 * @param b true if this vehicle represents a creature, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public void setCreature(boolean b) {
		v.creature = b;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the string displayed when a player embarks (boards) this vehicle.
	 * This string appears in game messages to other players.
	 * 
	 * @param string the embark message string
	 * @since 1.0.0
	 */
	@Override
	public void setEmbarkString(String string) {
		setPrivateField(embarkString, string);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the display name of this vehicle. This name is shown in various
	 * UI elements and game messages related to the vehicle.
	 * 
	 * @param name the new name for the vehicle
	 * @since 1.0.0
	 */
	@Override
	public void setName(String name) {
		v.name = name;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the maximum depth this vehicle can operate at. This is particularly
	 * relevant for water-based vehicles that have depth limitations.
	 * 
	 * @param f the maximum operational depth
	 * @since 1.0.0
	 */
	@Override
	public void setMaxDepth(float f) {
		v.maxDepth = f;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the maximum height difference this vehicle can traverse. This affects
	 * the vehicle's ability to move over uneven terrain.
	 * 
	 * @param f the maximum height difference
	 * @since 1.0.0
	 */
	@Override
	public void setMaxHeightDiff(float f) {
		v.maxHeightDiff = f;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the command type for this vehicle. The command type determines
	 * what kinds of commands can be issued to this vehicle.
	 * 
	 * @param i the command type identifier
	 * @since 1.0.0
	 */
	@Override
	public void setCommandType(byte i) {
		v.commandType = i;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Adds hitch seats to this vehicle. Hitch seats allow other creatures
	 * or vehicles to be attached to this vehicle.
	 * 
	 * @param hitches array of hitch seats to add
	 * @since 1.0.0
	 */
	@Override
	public void addHitchSeats(Seat[] hitches) {
		v.addHitchSeats(hitches);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Creates the specified number of passenger seats for this vehicle.
	 * These seats can be used by passengers and may include driver positions.
	 * 
	 * @param passengerSeats the number of passenger seats to create
	 * @since 1.0.0
	 */
	@Override
	public void createPassengerSeats(int passengerSeats) {
		callPrivateMethod(createPassengerSeats, passengerSeats);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the offset position for the specified seat number. This determines
	 * where passengers appear when seated in this position.
	 * 
	 * @param aNumber the seat number to modify
	 * @param aOffx the X-axis offset
	 * @param aOffy the Y-axis offset
	 * @param aOffz the Z-axis offset
	 * @since 1.0.0
	 */
	@Override
	public void setSeatOffset(final int aNumber, final float aOffx, final float aOffy, final float aOffz) {
		v.setSeatOffset(aNumber, aOffx, aOffy, aOffz);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the skill needed to operate this vehicle. This affects the minimum
	 * skill level required for players to effectively use the vehicle.
	 * 
	 * @param skillNeeded the minimum skill level required
	 * @since 1.0.0
	 */
	@Override
	public void setSkillNeeded(float skillNeeded) {
		v.skillNeeded = skillNeeded;

	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets the maximum speed for this vehicle. This determines how fast
	 * the vehicle can move when operated.
	 * 
	 * @param f the maximum speed value
	 * @since 1.0.0
	 */
	@Override
	public void setMaxSpeed(float f) {
		setPrivateField(maxSpeed, f);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Sets whether this vehicle can have equipment attached to it. This affects
	 * whether items like ropes, decorations, or functional equipment can be added.
	 * 
	 * @param b true if the vehicle can have equipment, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public void setCanHaveEquipment(boolean b) {
		setPrivateField(canHaveEquipment, b);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Retrieves the Item representation of this vehicle. This allows the
	 * vehicle to be treated as an item for inventory operations.
	 * 
	 * @return the Item representing this vehicle
	 * @throws NoSuchItemException if no item exists with the vehicle's Wurm ID
	 * @since 1.0.0
	 */
	@Override
	public Item getItem() throws NoSuchItemException {
		return Items.getItem(this.getWurmid());
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Retrieves the Wurm ID of this vehicle. The Wurm ID is the unique
	 * identifier used internally by the game to reference this vehicle.
	 * 
	 * @return the Wurm ID of this vehicle
	 * @since 1.0.0
	 */
	@Override
	public long getWurmid() {
		return callPrivateMethod(getWurmid);
	}

}
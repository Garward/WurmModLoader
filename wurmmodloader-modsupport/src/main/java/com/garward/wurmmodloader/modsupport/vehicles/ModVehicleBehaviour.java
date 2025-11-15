package com.garward.wurmmodloader.modsupport.vehicles;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.wurmonline.server.behaviours.Seat;
import com.wurmonline.server.behaviours.Vehicle;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Abstract base class for custom vehicle behaviour implementations in Wurm Unlimited modding.
 * 
 * <p>This class provides a foundation for modders to define custom behaviour for vehicles
 * created through mods. It offers methods to configure vehicle settings based on either
 * a Creature (for creature-drawn vehicles) or an Item (for item-based vehicles).</p>
 * 
 * <p>Implementations of this class should override the abstract methods to define how
 * vehicles should be configured when they are created or loaded. The class also provides
 * utility methods for creating seats and wrapping Vehicle objects with a facade for
 * easier manipulation.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class CustomVehicleBehaviour extends ModVehicleBehaviour {
 *     
 *     @Override
 *     public void setSettingsForVehicle(Creature creature, Vehicle vehicle) {
 *         VehicleFacade facade = wrap(vehicle);
 *         facade.setMaxSpeed(50.0f);
 *         facade.setAcceleration(10.0f);
 *     }
 *     
 *     @Override
 *     public void setSettingsForVehicle(Item item, Vehicle vehicle) {
 *         VehicleFacade facade = wrap(vehicle);
 *         facade.setMaxSpeed(30.0f);
 *         facade.setAcceleration(5.0f);
 *     }
 * }
 * }</pre>
 * 
 * <p>Thread-safety: Implementations of this class should be thread-safe as vehicle
 * configuration may occur on multiple threads during server operation.</p>
 * 
 * @since 1.0.0
 * @see Vehicle
 * @see VehicleFacade
 * @see VehicleFacadeImpl
 */
public abstract class ModVehicleBehaviour {
	
	/**
	 * Configures vehicle settings when the vehicle is associated with a Creature.
	 * 
	 * <p>This method is called when a vehicle is created or loaded that is controlled
	 * or drawn by a Creature. Implementations should configure the vehicle's properties
	 * such as speed, acceleration, and other behavioural characteristics.</p>
	 * 
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * @Override
	 * public void setSettingsForVehicle(Creature creature, Vehicle vehicle) {
	 *     VehicleFacade facade = wrap(vehicle);
	 *     facade.setMaxSpeed(40.0f);
	 *     facade.setAcceleration(8.0f);
	 * }
	 * }</pre>
	 * 
	 * @param creature the Creature associated with this vehicle (may be the driver or drafter)
	 * @param vehicle the Vehicle to configure
	 * @since 1.0.0
	 * @see #setSettingsForVehicle(Item, Vehicle)
	 * @see #wrap(Vehicle)
	 */
	public abstract void setSettingsForVehicle(final Creature creature, final Vehicle vehicle);

	/**
	 * Configures vehicle settings when the vehicle is associated with an Item.
	 * 
	 * <p>This method is called when a vehicle is created or loaded that is based on
	 * an Item. This is common for vehicles like carts or boats that are created from
	 * items placed in the world. Implementations should configure the vehicle's properties
	 * such as speed, acceleration, and other behavioural characteristics.</p>
	 * 
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * @Override
	 * public void setSettingsForVehicle(Item item, Vehicle vehicle) {
	 *     VehicleFacade facade = wrap(vehicle);
	 *     facade.setMaxSpeed(25.0f);
	 *     facade.setAcceleration(4.0f);
	 * }
	 * }</pre>
	 * 
	 * @param item the Item associated with this vehicle
	 * @param vehicle the Vehicle to configure
	 * @since 1.0.0
	 * @see #setSettingsForVehicle(Creature, Vehicle)
	 * @see #wrap(Vehicle)
	 */
	public abstract void setSettingsForVehicle(final Item item, final Vehicle vehicle);
	

	/**
	 * Wraps a Vehicle object with a VehicleFacade for easier manipulation.
	 * 
	 * <p>This utility method provides a facade pattern implementation that simplifies
	 * the process of configuring vehicle properties. The facade provides convenient
	 * methods for setting common vehicle attributes without needing to work directly
	 * with the Vehicle class's API.</p>
	 * 
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * VehicleFacade facade = wrap(vehicle);
	 * facade.setMaxSpeed(35.0f);
	 * facade.setAcceleration(6.0f);
	 * facade.setCanPassenger(true);
	 * }</pre>
	 * 
	 * <p>Thread-safety: This method is thread-safe and can be called from any thread.</p>
	 * 
	 * @param v the Vehicle to wrap
	 * @return a VehicleFacade wrapping the provided Vehicle
	 * @since 1.0.0
	 * @see VehicleFacade
	 * @see VehicleFacadeImpl
	 */
	protected VehicleFacade wrap(final Vehicle v) {
		return new VehicleFacadeImpl(v);
	}
	

	/**
	 * Creates a new Seat instance with the specified hitch type.
	 * 
	 * <p>This utility method uses reflection to instantiate a Seat object, bypassing
	 * the normal constructor visibility restrictions. This is necessary because the
	 * Seat constructor is not publicly accessible in the base game.</p>
	 * 
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * Seat driverSeat = createSeat(Seat.TYPE_DRAGGED);
	 * Seat passengerSeat = createSeat(Seat.TYPE_NORMAL);
	 * }</pre>
	 * 
	 * <p>Lifecycle: This method can be called during vehicle initialization to create
	 * seats for passengers or drivers.</p>
	 * 
	 * <p>Thread-safety: This method is thread-safe and can be called from any thread.</p>
	 * 
	 * @param typeHitched the hitch type for the seat (see {@link Seat} constants)
	 * @return a new Seat instance with the specified hitch type
	 * @throws RuntimeException if the Seat cannot be instantiated due to reflection errors
	 * @since 1.0.0
	 * @see Seat
	 */
	protected Seat createSeat(byte typeHitched) {
		try {
			Constructor<Seat> c = Seat.class.getDeclaredConstructor(byte.class);
			boolean wasAccessible = c.isAccessible();
			c.setAccessible(true);
			try {
				return c.newInstance(typeHitched);
			} finally {
				c.setAccessible(wasAccessible);
			}
		} catch (InvocationTargetException | NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}
		
	}

}
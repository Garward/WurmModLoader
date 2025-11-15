package com.garward.wurmmodloader.modsupport.vehicles;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.wurmonline.server.behaviours.Vehicle;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

/**
 * Manages custom vehicle behaviors for creatures and items in Wurm Unlimited.
 * 
 * <p>This class provides a framework for mods to register custom vehicle behaviors that are
 * applied when creatures or items are used as vehicles. It hooks into the game's vehicle system
 * to allow mods to modify vehicle settings dynamically.</p>
 * 
 * <p><strong>Purpose:</strong> This class serves as a central registry for vehicle behavior
 * modifications. It allows multiple mods to register their custom behaviors without conflicts,
 * and ensures these behaviors are applied when vehicles are initialized.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. All methods should be called
 * from the main game thread during mod initialization or game events.</p>
 * 
 * <p><strong>Lifecycle:</strong> This class must be initialized during mod loading before any
 * vehicle behaviors can be registered. The {@link #init()} method should be called once during
 * server startup.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // During mod initialization
 * ModVehicleBehaviours.init();
 * 
 * // Register a custom behavior for a creature vehicle (e.g., template ID 123)
 * ModVehicleBehaviours.addCreatureVehicle(123, new CustomCreatureVehicleBehaviour());
 * 
 * // Register a custom behavior for an item vehicle (e.g., template ID 456)
 * ModVehicleBehaviours.addItemVehicle(456, new CustomItemVehicleBehaviour());
 * }</pre>
 * 
 * @since 1.0.0
 * @see ModVehicleBehaviour
 */
public class ModVehicleBehaviours {

	private static Map<Integer, List<ModVehicleBehaviour>> itemVehicles = null;
	private static Map<Integer, List<ModVehicleBehaviour>> creatureVehicles = null;
	private static boolean inited = false;

	/**
	 * Initializes the vehicle behavior system.
	 * 
	 * <p>This method sets up the necessary hooks into the game's vehicle system and initializes
	 * internal data structures. It must be called once during server startup before any vehicle
	 * behaviors can be registered.</p>
	 * 
	 * <p><strong>Lifecycle:</strong> This method should be called exactly once during mod loading.
	 * Subsequent calls will have no effect.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is not thread-safe and should only be called
	 * from the main game thread during initialization.</p>
	 * 
	 * @since 1.0.0
	 * @throws RuntimeException if there is an error setting up the class hooks
	 */
	public static void init() {
		if (inited)
			return;
		
		itemVehicles = new HashMap<>();
		creatureVehicles = new HashMap<>();

		try {

			ClassPool classpool = HookManager.getInstance().getClassPool();
			String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] { classpool.get("com.wurmonline.server.creatures.Creature"), classpool.get("com.wurmonline.server.behaviours.Vehicle") });
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.Vehicles", "setSettingsForVehicle", descriptor, new InvocationHandlerFactory() {

				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Object result = method.invoke(proxy, args);

							Creature creature = (Creature) args[0];
							List<ModVehicleBehaviour> vehicles = creatureVehicles.get(creature.getTemplate().getTemplateId());
							if (vehicles != null) {
								for (ModVehicleBehaviour vehicle : vehicles) {
									vehicle.setSettingsForVehicle(creature, (Vehicle) args[1]);
								}
							}

							return result;
						}
					};
				}
			});

			descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] { classpool.get("com.wurmonline.server.items.Item"), classpool.get("com.wurmonline.server.behaviours.Vehicle") });
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.Vehicles", "setSettingsForVehicle", descriptor, new InvocationHandlerFactory() {

				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Object result = method.invoke(proxy, args);

							Item item = (Item) args[0];
							List<ModVehicleBehaviour> vehicles = itemVehicles.get(item.getTemplate().getTemplateId());
							if (vehicles != null) {
								for (ModVehicleBehaviour vehicle : vehicles) {
									vehicle.setSettingsForVehicle(item, (Vehicle) args[1]);
								}
							}

							return result;
						}
					};
				}
			});

		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		
		inited = true;
	}

	/**
	 * Registers a custom vehicle behavior for a creature template.
	 * 
	 * <p>This method associates a {@link ModVehicleBehaviour} with a specific creature template ID.
	 * When a creature of that template is used as a vehicle, the registered behavior's
	 * {@link ModVehicleBehaviour#setSettingsForVehicle(Creature, Vehicle)} method will be called.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is not thread-safe and should only be called
	 * from the main game thread.</p>
	 * 
	 * <p><strong>Lifecycle:</strong> This method can only be called after {@link #init()} has been
	 * called. It should typically be called during mod initialization.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * // Register a custom behavior for horses (template ID 123)
	 * ModVehicleBehaviours.addCreatureVehicle(123, new CustomHorseBehavior());
	 * }</pre>
	 * 
	 * @param creatureTemplateId the template ID of the creature to associate with the behavior
	 * @param vehicle the custom vehicle behavior to register
	 * @throws RuntimeException if the vehicle system has not been initialized
	 * @since 1.0.0
	 * @see #init()
	 * @see ModVehicleBehaviour
	 */
	public static void addCreatureVehicle(int creatureTemplateId, ModVehicleBehaviour vehicle) {
		if (!inited) {
			throw new RuntimeException("ModVehicles was not initialized");
		}
		creatureVehicles.computeIfAbsent(creatureTemplateId, key -> new ArrayList<>()).add(vehicle);
	}

	/**
	 * Registers a custom vehicle behavior for an item template.
	 * 
	 * <p>This method associates a {@link ModVehicleBehaviour} with a specific item template ID.
	 * When an item of that template is used as a vehicle, the registered behavior's
	 * {@link ModVehicleBehaviour#setSettingsForVehicle(Item, Vehicle)} method will be called.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is not thread-safe and should only be called
	 * from the main game thread.</p>
	 * 
	 * <p><strong>Lifecycle:</strong> This method can only be called after {@link #init()} has been
	 * called. It should typically be called during mod initialization.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * // Register a custom behavior for carts (template ID 456)
	 * ModVehicleBehaviours.addItemVehicle(456, new CustomCartBehavior());
	 * }</pre>
	 * 
	 * @param itemTemplateId the template ID of the item to associate with the behavior
	 * @param vehicle the custom vehicle behavior to register
	 * @throws RuntimeException if the vehicle system has not been initialized
	 * @since 1.0.0
	 * @see #init()
	 * @see ModVehicleBehaviour
	 */
	public static void addItemVehicle(int itemTemplateId, ModVehicleBehaviour vehicle) {
		if (!inited) {
			throw new RuntimeException("ModVehicles was not initialized");
		}
		itemVehicles.computeIfAbsent(itemTemplateId, key -> new ArrayList<>()).add(vehicle);
	}
}
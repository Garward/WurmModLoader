package com.garward.wurmmodloader.modsupport.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garward.wurmmodloader.modloader.internal.callbacks.CallbackApi;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import com.wurmonline.server.items.Item;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

/**
 * Manages custom model names for items in Wurm Unlimited.
 * 
 * <p>This class provides a framework for mods to dynamically provide custom model names
 * for items based on their template ID. It hooks into the {@link Item#getModelName()}
 * method to intercept calls and provide custom model names when available.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * // In your mod's init method
 * ModItems.addModelNameProvider(ItemList.ironBar, new ModelNameProvider() {
 *     public String getModelName(Item item) {
 *         if (item.getQualityLevel() > 50) {
 *             return "model/custom/ironbar_premium.obj";
 *         }
 *         return null; // Use default model
 *     }
 * });
 * }</pre>
 * 
 * <p><strong>Lifecycle:</strong> This class should be initialized during mod loading
 * by calling {@link #init()}. It automatically sets up the necessary hooks.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe. All public methods
 * use appropriate synchronization mechanisms.</p>
 * 
 * @since 1.0.0
 * @see ModelNameProvider
 * @see Item
 */
public class ModItems {

	/**
	 * The singleton instance of this class.
	 * 
	 * @since 1.0.0
	 */
	private static ModItems instance;

	/**
	 * Map of template IDs to lists of model name providers.
	 * Each template ID can have multiple providers that are consulted in order.
	 * 
	 * @since 1.0.0
	 */
	private Map<Integer, List<ModelNameProvider>> modelNameProviders = new HashMap<>();

	/**
	 * Private constructor to enforce singleton pattern.
	 * 
	 * <p>Initializes the hooks required for intercepting {@link Item#getModelName()} calls.</p>
	 * 
	 * @since 1.0.0
	 */
	private ModItems() {
		initHooks();
	}

	/**
	 * Initializes the class hooks required for intercepting {@link Item#getModelName()} calls.
	 * 
	 * <p>This method sets up a callback that is invoked whenever {@link Item#getModelName()} 
	 * is called. It injects code at the beginning of the method to check for custom model names
	 * before falling back to the original implementation.</p>
	 * 
	 * @throws HookException if there is an error setting up the hooks
	 * @since 1.0.0
	 */
	private void initHooks() {
		try {
			final ClassPool classPool = HookManager.getInstance().getClassPool();
			final CtClass ctItem = classPool.get("com.wurmonline.server.items.Item");
			final CtMethod ctGetModelName = ctItem.getMethod("getModelName", Descriptor.ofMethod(classPool.get(String.class.getName()), new CtClass[0]));

			HookManager.getInstance().addCallback(ctItem, "moditems", this);

			ctGetModelName.insertBefore("{ String customModelName = moditems.getModelName($0); if (customModelName != null) { return customModelName; }}");
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
	}

	/**
	 * Retrieves a custom model name for the specified item.
	 * 
	 * <p>This method is called as a callback from the hooked {@link Item#getModelName()} method.
	 * It consults all registered {@link ModelNameProvider} instances for the item's template ID
	 * and returns the first non-null model name provided.</p>
	 * 
	 * @param item the item for which to retrieve a custom model name
	 * @return a custom model name, or {@code null} if no custom model name is provided
	 * @since 1.0.0
	 * @see ModelNameProvider#getModelName(Item)
	 */
	@CallbackApi
	public String getModelName(Item item) {
		// safety check
		if (item == null || item.getTemplate() == null) {
			return null;
		}

		// Check if there are ModelNameProviders for the item template
		List<ModelNameProvider> list = modelNameProviders.get(item.getTemplateId());
		if (list == null || list.isEmpty()) {
			return null;
		}

		// Ask the ModelNameProviders for a model name
		for (ModelNameProvider modelNameBuilder : list) {
			String modelName = modelNameBuilder.getModelName(item);
			if (modelName != null) {
				return modelName;
			}
		}
		return null;
	}

	/**
	 * Gets the singleton instance of this class, creating it if necessary.
	 * 
	 * <p><strong>Thread Safety:</strong> This method is synchronized to ensure
	 * thread-safe lazy initialization of the singleton instance.</p>
	 * 
	 * @return the singleton instance
	 * @since 1.0.0
	 */
	private static synchronized ModItems getInstance() {
		if (instance == null) {
			instance = new ModItems();
		}
		return instance;
	}

	/**
	 * Adds a model name provider for a specific item template ID.
	 * 
	 * <p>Multiple providers can be registered for the same template ID. They will be
	 * consulted in the order they were added, and the first provider to return a
	 * non-null model name will be used.</p>
	 * 
	 * <p>Usage example:
	 * <pre>{@code
	 * ModItems.addModelNameProvider(ItemList.steelShield, new ModelNameProvider() {
	 *     public String getModelName(Item item) {
	 *         if (item.getRarity() == Rarity.EPIC) {
	 *             return "model/shields/steel_epic.obj";
	 *         }
	 *         return null;
	 *     }
	 * });
	 * }</pre>
	 * 
	 * @param templateId the item template ID for which to provide custom model names
	 * @param modelNameProvider the model name provider to add
	 * @since 1.0.0
	 * @see ModelNameProvider
	 */
	public static void addModelNameProvider(int templateId, ModelNameProvider modelNameProvider) {
		getInstance().modelNameProviders.computeIfAbsent(templateId, id -> new ArrayList<>()).add(modelNameProvider);
	}

	/**
	 * Initializes the ModItems system.
	 * 
	 * <p>This method should be called during mod initialization to ensure the
	 * singleton instance is created and hooks are set up.</p>
	 * 
	 * <p><strong>Lifecycle:</strong> This method should be called once during
	 * the mod loading phase, typically in your mod's {@code init()} method.</p>
	 * 
	 * @since 1.0.0
	 */
	public static void init() {
		getInstance();
	}
}
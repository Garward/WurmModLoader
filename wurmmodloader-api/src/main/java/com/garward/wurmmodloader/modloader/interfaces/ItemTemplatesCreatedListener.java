package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Listener interface for item template creation events.
 * <p>
 * Mods implementing this interface will be notified when Wurm's item templates
 * have been loaded from the database and are ready for use or modification.
 * This is the ideal time to create custom items using {@code ItemTemplateBuilder}.
 * </p>
 * <p>
 * Use this listener when you need to:
 * <ul>
 *   <li>Create custom item templates</li>
 *   <li>Modify existing item templates</li>
 *   <li>Access or validate item template data</li>
 *   <li>Register items that depend on existing templates</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyMod implements WurmServerMod, ItemTemplatesCreatedListener {
 *     @Override
 *     public void onItemTemplatesCreated() {
 *         ItemTemplateBuilder builder = new ItemTemplateBuilder("mymod.custom.item");
 *         builder.name("Custom Item", "custom items", "A custom item");
 *         builder.weightGrams(1000);
 *         builder.build();
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Lifecycle order:</b>
 * <ol>
 *   <li>{@link WurmServerMod#preInit()}</li>
 *   <li>{@link WurmServerMod#init()}</li>
 *   <li><b>{@link #onItemTemplatesCreated()} ← You are here</b></li>
 *   <li>{@link ServerStartedListener#onServerStarted()}</li>
 * </ol>
 * </p>
 *
 * @see WurmServerMod
 * @since 1.0.0
 */
public interface ItemTemplatesCreatedListener {

	/**
	 * Called when item templates have been loaded and are ready for use.
	 * <p>
	 * At this point:
	 * <ul>
	 *   <li>All vanilla Wurm item templates are loaded</li>
	 *   <li>Item template database is accessible</li>
	 *   <li>Custom items can be safely created</li>
	 *   <li>Existing templates can be modified</li>
	 * </ul>
	 * </p>
	 * <p>
	 * This is the recommended place to create custom items using
	 * {@code ItemTemplateBuilder}.
	 * </p>
	 */
	void onItemTemplatesCreated();
}

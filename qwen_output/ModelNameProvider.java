package com.garward.wurmmodloader.modsupport.items;

import com.wurmonline.server.items.Item;

/**
 * Interface for providing custom model names for items in the Wurm Unlimited server.
 * 
 * <p>This interface allows mod developers to specify custom model names for items,
 * enabling the use of custom 3D models in the game. Implementations of this interface
 * are typically registered with the mod loader system and are called when items are
 * created or updated to determine their visual representation.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * public class CustomModelProvider implements ModelNameProvider {
 *     @Override
 *     public String getModelName(Item item) {
 *         if (item.getTemplateId() == CustomItems.customSwordId) {
 *             return "model.custom.sword";
 *         }
 *         return null;
 *     }
 * }
 * }</pre>
 * </p>
 * 
 * <p>Implementations of this interface should be registered during the server
 * initialization phase, typically in a mod's {@code onServerStarted()} method.</p>
 * 
 * @since 1.0.0
 * @see com.wurmonline.server.items.Item
 */
public interface ModelNameProvider {

	/**
	 * Retrieves the custom model name for the specified item.
	 * 
	 * <p>This method is called by the server when determining the 3D model to use
	 * for an item. If a custom model name is returned, it will be used instead of
	 * the default model specified in the item template.</p>
	 * 
	 * <p>Thread-safety: This method may be called from multiple threads concurrently,
	 * particularly during world loading and item creation. Implementations should be
	 * thread-safe or use appropriate synchronization mechanisms.</p>
	 * 
	 * <p>Lifecycle: This method is called during item creation, loading from database,
	 * and whenever an item's appearance might change. It may be called frequently
	 * during normal server operation.</p>
	 * 
	 * @param item the item for which to retrieve the model name; never null
	 * @return the custom model name to use for this item, or null if no custom
	 *         model should be used and the default should be applied
	 * @throws RuntimeException if there is an unexpected error while determining
	 *         the model name; throwing exceptions may cause items to appear incorrectly
	 * @since 1.0.0
	 * @see Item
	 * @see com.wurmonline.server.items.ItemTemplate
	 */
	String getModelName(Item item);

}
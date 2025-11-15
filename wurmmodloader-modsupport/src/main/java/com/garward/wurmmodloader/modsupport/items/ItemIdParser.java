package com.garward.wurmmodloader.modsupport.items;

import com.garward.wurmmodloader.modsupport.IdType;
import com.garward.wurmmodloader.modsupport.NonFreezingNamedIdParser;

/**
 * Parser for converting item names to their corresponding template IDs in Wurm Unlimited.
 * 
 * <p>This class provides functionality to parse item names and resolve them to their
 * numeric item template IDs. It extends {@link NonFreezingNamedIdParser} to provide
 * item-specific parsing capabilities while maintaining thread-safety through immutable
 * configuration.</p>
 * 
 * <p><strong>Purpose:</strong></p>
 * <ul>
 *   <li>Convert item names (e.g., "iron sword") to template IDs (e.g., 3)</li>
 *   <li>Validate item names against the game's item registry</li>
 *   <li>Provide consistent error handling for invalid item names</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre><code>
 * // Create a parser instance
 * ItemIdParser parser = new ItemIdParser();
 * 
 * // Parse a single item name
 * int ironSwordId = parser.parse("iron sword");
 * 
 * // Parse multiple item names
 * List&lt;String&gt; itemNames = Arrays.asList("iron sword", "wooden shield", "bread");
 * Map&lt;String, Integer&gt; itemIds = parser.parseAll(itemNames);
 * 
 * // Handle invalid item names
 * try {
 *     int invalidId = parser.parse("nonexistent item");
 * } catch (IllegalArgumentException e) {
 *     // Handle invalid item name
 *     System.err.println("Invalid item: " + e.getMessage());
 * }
 * </code></pre>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe as it contains no
 * mutable state and relies on thread-safe parent class implementation.</p>
 * 
 * <p><strong>Lifecycle:</strong> Instances can be created at any time and reused
 * throughout the application lifecycle. No special initialization or cleanup is required.</p>
 * 
 * @since 1.0.0
 * @see NonFreezingNamedIdParser
 * @see IdType#ITEMTEMPLATE
 */
public class ItemIdParser extends NonFreezingNamedIdParser {
	
	/**
	 * Returns the fully qualified class name containing item name constants.
	 * 
	 * <p>This method provides the class name that contains all valid item names
	 * as static string constants. The class is part of the Wurm Unlimited server
	 * implementation.</p>
	 * 
	 * @return the fully qualified class name "com.wurmonline.server.items.ItemList"
	 * @since 1.0.0
	 */
	@Override
	protected String getNamesClassName() {
		return "com.wurmonline.server.items.ItemList";
	}
	
	/**
	 * Returns the ID factory type for item templates.
	 * 
	 * <p>This method specifies that this parser works with item template IDs,
	 * which are identified by the {@link IdType#ITEMTEMPLATE} enumeration value.</p>
	 * 
	 * @return {@link IdType#ITEMTEMPLATE} indicating item template ID processing
	 * @since 1.0.0
	 * @see IdType#ITEMTEMPLATE
	 */
	@Override
	protected IdType getIdFactoryType() {
		return IdType.ITEMTEMPLATE;
	}
		
	/**
	 * Handles unparsable item names by throwing an appropriate exception.
	 * 
	 * <p>When an item name cannot be resolved to a valid template ID, this method
	 * is called to provide consistent error handling. It throws an {@link IllegalArgumentException}
	 * with a descriptive message indicating the invalid item name.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it only
	 * operates on the provided parameter and throws a new exception instance.</p>
	 * 
	 * @param name the item name that could not be parsed
	 * @return never returns normally - always throws an exception
	 * @throws IllegalArgumentException always thrown with a message indicating the invalid item name
	 * @since 1.0.0
	 */
	@Override
	protected int unparsable(String name) {
		throw new IllegalArgumentException(name + " is not a valid item name");
	}
}
package com.garward.wurmmodloader.modsupport.creatures;

import com.garward.wurmmodloader.modsupport.NonFreezingNamedIdParser;

/**
 * Parser for creature type constants defined in the Wurm Unlimited server API.
 * 
 * <p>This parser extracts creature type identifiers and their corresponding integer values
 * from the {@code com.wurmonline.shared.constants.CreatureTypes} class. It provides
 * a convenient way to map between creature type names (such as "HERBIVORE" or "CARNIVORE")
 * and their numeric IDs used internally by the game engine.</p>
 * 
 * <p><strong>Purpose:</strong><br>
 * The parser facilitates mod development by allowing mods to reference creature types
 * using human-readable names rather than hardcoded integer values. This improves
 * code maintainability and reduces errors when working with creature type constants.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre><code>
 * // Create a parser instance
 * CreatureTypesParser parser = new CreatureTypesParser();
 * 
 * // Get the ID for a specific creature type
 * int herbivoreId = parser.parse("HERBIVORE");
 * 
 * // Check if a creature type exists
 * boolean isValid = parser.isValid("CARNIVORE");
 * 
 * // Get all available creature types
 * Set&amp;lt;String&amp;gt; allTypes = parser.getValidNames();
 * </code></pre>
 * 
 * <p><strong>Thread Safety:</strong><br>
 * This class is thread-safe once initialized. The underlying data structures are
 * populated during construction and remain immutable thereafter.</p>
 * 
 * @since 1.0.0
 * @see com.garward.wurmmodloader.modsupport.NonFreezingNamedIdParser
 * @see com.wurmonline.shared.constants.CreatureTypes
 */
public class CreatureTypesParser extends NonFreezingNamedIdParser {
	
	/**
	 * Returns the fully qualified class name containing the creature type constants.
	 * 
	 * <p>This method returns the class name of the Wurm Unlimited API class that
	 * contains all creature type constant definitions.</p>
	 * 
	 * @return the fully qualified class name "com.wurmonline.shared.constants.CreatureTypes"
	 * @since 1.0.0
	 */
	@Override
	protected String getNamesClassName() {
		return "com.wurmonline.shared.constants.CreatureTypes";
	}
	
	/**
	 * Determines if a field name represents a valid creature type constant.
	 * 
	 * <p>This implementation checks if the field name starts with the prefix "C_TYPE",
	 * which is the naming convention used for creature type constants in the Wurm API.</p>
	 * 
	 * @param fieldName the name of the field to check
	 * @return {@code true} if the field name starts with "C_TYPE", {@code false} otherwise
	 * @throws NullPointerException if fieldName is null
	 * @since 1.0.0
	 */
	@Override
	protected boolean isValidName(String fieldName) {
		return fieldName.startsWith("C_TYPE");
	}
	
	/**
	 * Cleans up a field name to extract the meaningful creature type identifier.
	 * 
	 * <p>This method removes the "C_TYPE_" prefix from field names to produce clean,
	 * readable creature type names. For example, "C_TYPE_HERBIVORE" becomes "HERBIVORE".</p>
	 * 
	 * @param fieldName the raw field name to clean up
	 * @return the cleaned field name with the "C_TYPE_" prefix removed
	 * @throws NullPointerException if fieldName is null
	 * @since 1.0.0
	 */
	@Override
	protected String cleanupFieldName(String fieldName) {
		return fieldName.replaceAll("^C_TYPE_", "");
	}
	
	/**
	 * Handles unparsable creature type names by throwing an appropriate exception.
	 * 
	 * <p>When a requested creature type name cannot be resolved to a valid ID,
	 * this method throws an {@link IllegalArgumentException} with a descriptive message.</p>
	 * 
	 * @param name the creature type name that could not be parsed
	 * @return never returns normally - always throws an exception
	 * @throws IllegalArgumentException always thrown with a message indicating the invalid name
	 * @since 1.0.0
	 */
	@Override
	protected int unparsable(String name) {
		throw new IllegalArgumentException(name + " is not a valid creature type");
	}
}
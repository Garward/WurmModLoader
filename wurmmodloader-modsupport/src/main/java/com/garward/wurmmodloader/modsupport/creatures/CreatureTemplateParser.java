package com.garward.wurmmodloader.modsupport.creatures;

import com.garward.wurmmodloader.modsupport.IdType;
import com.garward.wurmmodloader.modsupport.NonFreezingNamedIdParser;

/**
 * Parser for creature template identifiers used in Wurm Unlimited server modifications.
 *
 * <p>This parser is designed to handle the conversion between creature template names and their
 * corresponding numeric IDs. It specifically works with the standard Wurm creature template 
 * naming convention where field names end with "_CID" suffix.</p>
 *
 * <p><strong>Purpose:</strong><br>
 * The parser facilitates mod development by providing a standardized way to reference creature
 * templates by name rather than hardcoded numeric IDs, making mods more maintainable and 
 * readable.</p>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre><code>
 * // Create a parser instance
 * CreatureTemplateParser parser = new CreatureTemplateParser();
 * 
 * // Parse a creature template ID by name
 * int horseId = parser.parse("HORSE_CID");  // Returns the numeric ID for horses
 * 
 * // Clean name lookup
 * int dragonId = parser.parse("DRAGON");    // Automatically handles _CID suffix
 * 
 * // Get all available creature templates
 * Map&lt;String, Integer&gt; templates = parser.getNamedIds();
 * </code></pre>
 *
 * <p><strong>Thread Safety:</strong><br>
 * This class is thread-safe once initialized. The underlying named ID map is populated
 * during construction and remains immutable thereafter.</p>
 *
 * @since 1.0.0
 * @see com.garward.wurmmodloader.modsupport.NonFreezingNamedIdParser
 * @see com.garward.wurmmodloader.modsupport.IdType
 */
public class CreatureTemplateParser extends NonFreezingNamedIdParser {
	
	/**
	 * Gets the fully qualified class name containing creature template ID constants.
	 * 
	 * <p>This method returns the class path to {@code com.wurmonline.server.creatures.CreatureTemplateIds}
	 * which contains all the standard creature template ID constants used in Wurm Unlimited.</p>
	 *
	 * @return the fully qualified class name "com.wurmonline.server.creatures.CreatureTemplateIds"
	 * @since 1.0.0
	 */
	@Override
	protected String getNamesClassName() {
		return "com.wurmonline.server.creatures.CreatureTemplateIds";
	}
	
	/**
	 * Determines if a field name represents a valid creature template identifier.
	 * 
	 * <p>Valid creature template field names must end with the "_CID" suffix as per
	 * Wurm Unlimited's naming convention for creature templates.</p>
	 *
	 * @param fieldName the field name to validate, never null
	 * @return true if the field name ends with "_CID", false otherwise
	 * @since 1.0.0
	 * @see #cleanupFieldName(String)
	 */
	@Override
	protected boolean isValidName(String fieldName) {
		return fieldName.endsWith("_CID");
	}
	
	/**
	 * Cleans up a field name by removing the "_CID" suffix to create a user-friendly name.
	 * 
	 * <p>This transformation allows users to reference creature templates using either
	 * the full constant name (e.g., "HORSE_CID") or the clean name (e.g., "HORSE").</p>
	 *
	 * @param fieldName the raw field name that may contain "_CID" suffix, never null
	 * @return the field name with "_CID" suffix removed, or the original name if suffix not present
	 * @since 1.0.0
	 * @see #isValidName(String)
	 */
	@Override
	protected String cleanupFieldName(String fieldName) {
		return fieldName.replaceAll("_CID$", "");
	}
	
	/**
	 * Gets the ID type identifier for creature templates.
	 * 
	 * <p>Returns {@link IdType#CREATURETEMPLATE} to identify this parser's domain.</p>
	 *
	 * @return IdType.CREATURETEMPLATE indicating this parser handles creature template IDs
	 * @since 1.0.0
	 * @see IdType#CREATURETEMPLATE
	 * @see com.garward.wurmmodloader.modsupport.IdType
	 */
	@Override
	protected IdType getIdFactoryType() {
		return IdType.CREATURETEMPLATE;
	}
	
	/**
	 * Handles unparsable creature template names by throwing an appropriate exception.
	 * 
	 * <p>When a requested creature template name cannot be resolved to a valid ID,
	 * this method throws an {@link IllegalArgumentException} with a descriptive message.</p>
	 *
	 * @param name the invalid creature template name that could not be parsed, never null
	 * @return never returns normally - always throws an exception
	 * @throws IllegalArgumentException always thrown with message indicating the invalid name
	 * @since 1.0.0
	 */
	@Override
	protected int unparsable(String name) {
		throw new IllegalArgumentException(name + " is not a valid creature template id");
	}
}
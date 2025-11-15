package com.garward.wurmmodloader.modsupport;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Abstract base class for parsing named identifiers and their corresponding integer IDs.
 *
 * <p>This class provides functionality to map between human-readable names and numeric IDs
 * by reflecting over a names class containing public static final integer fields. It supports
 * parsing individual names/IDs as well as comma-separated lists of such values.</p>
 *
 * <p>Subclasses must implement {@link #getNamesClass()} to specify which class contains
 * the name-to-ID mappings. The class automatically scans all public static final integer fields
 * in that class during initialization.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * public class CreatureTypeParser extends NamedIdParser {
 *     protected Class<?> getNamesClass() {
 *         return CreatureTypes.class;
 *     }
 *
 *     protected boolean isValidName(String fieldName) {
 *         return fieldName.startsWith("CREATURE_");
 *     }
 * }
 *
 * // Usage
 * CreatureTypeParser parser = new CreatureTypeParser();
 * int typeId = parser.parse("dragon");  // parses name
 * int typeId2 = parser.parse("123");    // parses ID directly
 * int[] typeIds = parser.parseList("dragon, phoenix, 456"); // parses list
 * String name = parser.toString(123);   // converts ID to name
 * }</pre>
 * </p>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe after construction.
 * All internal maps are populated during object construction and are not modified afterwards.</p>
 *
 * @since 1.0.0
 */
public abstract class NamedIdParser {

	private static final Logger logger = Logger.getLogger(NamedIdParser.class.getName());

	private Map<String, Integer> nameToId = new HashMap<>();
	private Map<Integer, String> idToName = new HashMap<>();

	/**
	 * Constructs a new NamedIdParser and initializes the name-to-ID mappings.
	 *
	 * <p>During construction, this method reflects over all public static final integer fields
	 * in the class returned by {@link #getNamesClass()} and builds internal mappings between
	 * normalized field names and their corresponding integer values.</p>
	 *
	 * <p>Field names are normalized using {@link #normalizeName(String)} before being stored
	 * as lookup keys. Only fields for which {@link #isValidName(String)} returns {@code true}
	 * are included in the mappings.</p>
	 *
	 * <p>Field names are optionally cleaned up using {@link #cleanupFieldName(String)} before
	 * normalization.</p>
	 *
	 * @since 1.0.0
	 */
	public NamedIdParser() {
		final Class<?> namesClass = getNamesClass();
		for (Field field : namesClass.getFields()) {
			final String fieldName = field.getName();
			if (isValidName(fieldName)) {
				final String name = cleanupFieldName(fieldName);
				final String normalized = normalizeName(name);
				try {
					int id = field.getInt(namesClass);
					nameToId.put(normalized, id);
					idToName.put(id, name);
				} catch (IllegalAccessException e) {
					logger.log(Level.WARNING, null, e);
				}
			}
		}
	}

	/**
	 * Returns the class containing the name-to-ID mappings.
	 *
	 * <p>This method must be implemented by subclasses to specify which class contains
	 * the public static final integer fields that define the name-to-ID mappings.</p>
	 *
	 * <p>The returned class will be scanned during construction for all public static
	 * final integer fields. Each such field will be added to the internal mappings.</p>
	 *
	 * @return the class containing the name-to-ID mappings
	 * @since 1.0.0
	 */
	protected abstract Class<?> getNamesClass();

	/**
	 * Returns the IdFactory type for mod-specific ID lookups.
	 *
	 * <p>If this method returns a non-null value, then names prefixed with "mod:" will
	 * be looked up using {@link IdFactory#getExistingIdFor(String, IdType)} with the
	 * returned type. This allows integration with the mod's ID factory system.</p>
	 *
	 * <p>By default, this method returns {@code null}, which disables mod-specific ID
	 * lookups. Subclasses that need this functionality should override this method to
	 * return the appropriate {@link IdType}.</p>
	 *
	 * @return the IdFactory type, or {@code null} to disable mod-specific lookups
	 * @since 1.0.0
	 */
	protected IdType getIdFactoryType() {
		return null;
	}

	/**
	 * Tests if a field name represents a valid entity name.
	 *
	 * <p>This method is called for each public static final integer field found in the
	 * names class. Only fields for which this method returns {@code true} will be
	 * included in the name-to-ID mappings.</p>
	 *
	 * <p>The default implementation always returns {@code true}, meaning all fields are
	 * considered valid. Subclasses can override this method to filter fields based on
	 * naming conventions or other criteria.</p>
	 *
	 * @param fieldName the name of the field to test
	 * @return {@code true} if the field represents a valid entity name, {@code false} otherwise
	 * @since 1.0.0
	 */
	protected boolean isValidName(String fieldName) {
		return true;
	}

	/**
	 * Cleans up a field name before normalization.
	 *
	 * <p>This method is called for each field name that passes the {@link #isValidName(String)}
	 * test. It allows subclasses to modify field names before they are normalized and
	 * added to the mappings.</p>
	 *
	 * <p>The default implementation returns the field name unchanged. Subclasses can override
	 * this method to remove prefixes, suffixes, or perform other transformations.</p>
	 *
	 * @param fieldName the field name to clean up
	 * @return the cleaned up field name
	 * @since 1.0.0
	 */
	protected String cleanupFieldName(String fieldName) {
		return fieldName;
	}

	/**
	 * Normalizes an entity name into a lookup key for the internal hash maps.
	 *
	 * <p>This method converts names to lowercase and removes all underscores and spaces,
	 * making lookups case-insensitive and format-flexible. For example, "Dragon_Red" and
	 * "dragon red" would both normalize to the same key "dragonred".</p>
	 *
	 * <p>This method is used both during initialization (to build the internal mappings)
	 * and during parsing (to look up names).</p>
	 *
	 * @param name the entity name to normalize
	 * @return the normalized lookup key
	 * @since 1.0.0
	 */
	public static String normalizeName(String name) {
		return name.toLowerCase().replaceAll("_| ", "");
	}

	/**
	 * Handles unparsable names.
	 *
	 * <p>This method is called when {@link #parse(String)} cannot parse a name. This happens
	 * when the name is not a valid integer and does not match any known entity name.</p>
	 *
	 * <p>The default implementation throws an {@link IllegalArgumentException}. Subclasses
	 * can override this method to provide fallback behavior, such as returning a default
	 * ID or attempting alternative parsing strategies.</p>
	 *
	 * @param name the unparsable name
	 * @return the ID to use for the unparsable name (if not throwing)
	 * @throws IllegalArgumentException if the name cannot be parsed (default behavior)
	 * @since 1.0.0
	 */
	protected int unparsable(String name) {
		throw new IllegalArgumentException(name);
	}

	/**
	 * Parses an entity name or ID into an integer ID.
	 *
	 * <p>This method attempts to parse the input in the following order:
	 * <ol>
	 * <li>If the input starts with "mod:" and {@link #getIdFactoryType()} returns non-null,
	 * look up the name using {@link IdFactory#getExistingIdFor(String, IdType)}</li>
	 * <li>If the input is a valid integer, return it directly</li>
	 * <li>Look up the normalized input in the internal name-to-ID map</li>
	 * <li>Call {@link #unparsable(String)} for the input</li>
	 * </ol></p>
	 *
	 * @param name the entity name or ID to parse
	 * @return the corresponding integer ID
	 * @throws IllegalArgumentException if the name cannot be parsed and {@link #unparsable(String)}
	 *                                  throws an exception
	 * @since 1.0.0
	 */
	public int parse(String name) {
		if (getIdFactoryType() != null && name.startsWith("mod:")) {
			int id = IdFactory.getExistingIdFor(name.substring(4), getIdFactoryType());
			if (id != -10)
				return id;
			return unparsable(name);
		}
		try {
			return Integer.parseInt(name);
		} catch (NumberFormatException e) {
		}
		Integer id = nameToId.get(normalizeName(name));
		if (id != null) {
			return id;
		}
		return unparsable(name);
	}

	/**
	 * Parses a comma-separated list of entity names or IDs.
	 *
	 * <p>This method splits the input string on commas, trims whitespace from each element,
	 * and parses each element using {@link #parse(String)}. The results are returned as
	 * an array of integers.</p>
	 *
	 * <p>Example:
	 * <pre>{@code
	 * int[] ids = parser.parseList("dragon, phoenix, 123, mod:special_creature");
	 * }</pre>
	 * </p>
	 *
	 * @param str the comma-separated list of entity names or IDs
	 * @return an array of corresponding integer IDs
	 * @throws IllegalArgumentException if any element cannot be parsed and {@link #unparsable(String)}
	 *                                  throws an exception
	 * @since 1.0.0
	 */
	public int[] parseList(String str) {
		return Arrays.stream(str.split(","))
				.map(String::trim)
				.mapToInt(this::parse)
				.toArray();
	}

	/**
	 * Converts an integer ID to its corresponding entity name.
	 *
	 * <p>If the ID has a corresponding name in the internal mappings, that name is returned.
	 * Otherwise, the ID is converted to a string representation.</p>
	 *
	 * @param id the integer ID to convert
	 * @return the corresponding entity name, or the string representation of the ID
	 * @since 1.0.0
	 */
	public String toString(int id) {
		String name = idToName.get(id);
		if (name != null) {
			return name;
		} else {
			return Integer.toString(id);
		}
	}

	/**
	 * Converts an array of integer IDs to a comma-separated list of entity names.
	 *
	 * <p>Each ID in the array is converted using {@link #toString(int)}, and the results
	 * are joined with ", " as separator.</p>
	 *
	 * <p>Example:
	 * <pre>{@code
	 * int[] ids = {123, 456, 789};
	 * String names = parser.toString(ids);  // "dragon, phoenix, 789"
	 * }</pre>
	 * </p>
	 *
	 * @param ids the array of integer IDs to convert
	 * @return a comma-separated list of corresponding entity names
	 * @since 1.0.0
	 */
	public String toString(int[] ids) {
		return Arrays.stream(ids)
				.mapToObj(this::toString)
				.collect(Collectors.joining(", "));
	}
}
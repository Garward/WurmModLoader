package com.garward.wurmmodloader.modsupport;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.registry.IdAllocator;
import com.garward.wurmmodloader.core.migration.IdFactoryMigration;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating and managing unique IDs for custom templates and mod objects.
 * <p>
 * <b>⚠️ COMPATIBILITY ADAPTER:</b> This class now delegates to {@link IdAllocator} for ID
 * allocation, providing backward compatibility while using the new JSON-based storage system.
 * </p>
 * <p>
 * <b>Migration from SQL to JSON:</b> On first use, this class automatically migrates any
 * existing SQL-based IDs to the new JSON storage via {@link IdFactoryMigration}. This ensures
 * existing mods retain their ID allocations without conflicts.
 * </p>
 * <p>
 * <b>Benefits of the new system:</b>
 * <ul>
 *   <li>No SQL database dependency (eliminates SQL errors)</li>
 *   <li>Human-readable JSON storage for easier debugging</li>
 *   <li>Integrated with new Registry system</li>
 *   <li>Faster performance (no database queries)</li>
 * </ul>
 * </p>
 *
 * <h2>Usage (unchanged from original API)</h2>
 * <pre>{@code
 * // Get or create an ID for a custom item
 * int itemId = IdFactory.getIdFor("mymod.customitem", IdType.ITEMTEMPLATE);
 *
 * // Check if an ID already exists
 * int existingId = IdFactory.getExistingIdFor("mymod.customitem", IdType.ITEMTEMPLATE);
 * if (existingId != -10) {
 *     // ID exists, use it
 * }
 *
 * // Get all IDs for a type
 * List<Entry<String, Integer>> itemIds = IdFactory.getIdsFor(IdType.ITEMTEMPLATE);
 * }</pre>
 *
 * @see IdAllocator
 * @see IdFactoryMigration
 * @see IdType
 * @see com.garward.wurmmodloader.modsupport.ItemTemplateBuilder
 * @see com.garward.wurmmodloader.modsupport.CreatureTemplateBuilder
 * @since 1.0.0
 * @deprecated Since 2.0.0, use {@link IdAllocator} directly for new code. This class remains
 *             for backward compatibility with existing mods.
 */
@Deprecated
public class IdFactory {

	private static final Logger logger = Logger.getLogger(IdFactory.class.getName());
	private static IdFactory instance;
	private static boolean migrationAttempted = false;

	/**
	 * Constructs a new IdFactory instance and performs one-time migration.
	 */
	private IdFactory() {
		init();
	}

	/**
	 * Initializes the IdFactory and performs one-time SQL→JSON migration.
	 * <p>
	 * This method attempts to migrate existing SQL-based IDs to the new JSON storage.
	 * If SQL database is unavailable or migration fails, it logs a warning but continues
	 * (new JSON storage will be used going forward).
	 * </p>
	 *
	 * @since 2.0.0
	 */
	private void init() {
		synchronized (IdFactory.class) {
			if (!migrationAttempted) {
				migrationAttempted = true;

				try {
					logger.info("IdFactory initializing - checking for SQL data to migrate");

					if (IdFactoryMigration.isMigrationNeeded()) {
						logger.info("SQL-based IDs detected - starting automatic migration to JSON");
						int migrated = IdFactoryMigration.migrateAllTypes();
						logger.info(String.format("Migration complete: %d IDs migrated from SQL to JSON", migrated));
					} else {
						logger.info("No SQL migration needed - using JSON storage");
					}

				} catch (Throwable e) {
					// Catch Throwable (not just Exception) to handle NoClassDefFoundError
					// when Wurm server classes aren't available (e.g., in standalone tests)
					logger.log(Level.WARNING,
						"Could not migrate SQL data (this is normal if no SQL database exists or " +
						"Wurm classes aren't loaded). Using JSON storage for new IDs.", e);
				}
			}
		}
	}

	/**
	 * Gets the singleton instance of IdFactory, creating it if necessary.
	 *
	 * @return the singleton IdFactory instance
	 * @since 1.0.0
	 */
	private static synchronized IdFactory getInstance() {
		if (instance == null) {
			instance = new IdFactory();
		}
		return instance;
	}

	/**
	 * Get or create a persistent ID for the given identifier and type.
	 * <p>
	 * <b>Implementation Note:</b> This method now delegates to {@link IdAllocator}.
	 * The identifier is converted to a {@link ResourceLocation} for namespace-safe allocation.
	 * </p>
	 *
	 * @param identifier unique string identifier for this object
	 * @param idType the type of ID to allocate
	 * @return the persistent ID for this identifier
	 * @since 1.0.0
	 */
	public int getId(String identifier, IIdType idType) {
		// Convert identifier to ResourceLocation
		ResourceLocation location = convertToResourceLocation(identifier, idType);

		// Delegate to IdAllocator
		int id = IdAllocator.getInstance().allocate(location);

		// Update IdType's last used ID (for backward compatibility)
		idType.updateLastUsedId(id);

		return id;
	}

	/**
	 * Retrieve an existing ID for the given identifier, or -10 if not found.
	 *
	 * @param identifier unique string identifier to look up
	 * @param idType the type of ID to search for
	 * @return the existing ID, or -10 if no ID exists for this identifier
	 * @since 1.0.0
	 */
	public int getExistingId(String identifier, IIdType idType) {
		ResourceLocation location = convertToResourceLocation(identifier, idType);

		// Check if ID exists in IdAllocator
		Map<ResourceLocation, Integer> allocations = IdAllocator.getInstance().getAllocations();

		if (allocations.containsKey(location)) {
			int id = allocations.get(location);
			idType.updateLastUsedId(id);
			return id;
		}

		return -10;
	}

	/**
	 * Get all known IDs for a specific type.
	 *
	 * @param idType the ID type to query
	 * @return list of entries mapping identifiers to IDs, ordered by ID
	 * @since 1.0.0
	 */
	public List<Map.Entry<String, Integer>> getIds(IIdType idType) {
		List<Map.Entry<String, Integer>> results = new ArrayList<>();
		Map<ResourceLocation, Integer> allocations = IdAllocator.getInstance().getAllocations();

		// Filter by type prefix and collect results
		String typePrefix = idType.typeName() + ":";

		for (Map.Entry<ResourceLocation, Integer> entry : allocations.entrySet()) {
			// Check if this ResourceLocation matches this ID type
			// We stored them with type prefix during conversion
			String fullPath = entry.getKey().toString();
			if (fullPath.contains(typePrefix) ||
			    // Also check namespace for backward compatibility
			    entry.getKey().getNamespace().equalsIgnoreCase(idType.typeName())) {

				String identifier = extractIdentifier(entry.getKey(), idType);
				results.add(new SimpleEntry<>(identifier, entry.getValue()));
			}
		}

		// Sort by ID (ascending)
		results.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));

		return results;
	}

	/**
	 * Get or create a persistent ID for the given identifier and type.
	 * <p>
	 * This is the primary method for obtaining IDs in mods.
	 * </p>
	 *
	 * @param identifier unique string identifier for this object
	 * @param idType the type of ID to allocate
	 * @return the persistent ID for this identifier
	 * @since 1.0.0
	 */
	public static int getIdFor(String identifier, IdType idType) {
		int id = getInstance().getId(identifier, idType);
		logger.log(Level.INFO, String.format("Using id %d for %s %s", id, idType.name().toLowerCase(Locale.ROOT), identifier));
		return id;
	}

	/**
	 * Retrieve an existing ID for the given identifier, or -10 if not found.
	 *
	 * @param identifier unique string identifier to look up
	 * @param idType the type of ID to search for
	 * @return the existing ID, or -10 if no ID exists for this identifier
	 * @since 1.0.0
	 */
	public static int getExistingIdFor(String identifier, IdType idType) {
		int id = getInstance().getExistingId(identifier, idType);
		if (id != -10)
			logger.log(Level.INFO, String.format("Using id %d for %s %s", id, idType.name().toLowerCase(Locale.ROOT), identifier));
		return id;
	}

	/**
	 * Get all known IDs for a specific type.
	 *
	 * @param idType the ID type to query
	 * @return list of entries mapping identifiers to IDs, ordered by ID
	 * @since 1.0.0
	 */
	public static List<Entry<String, Integer>> getIdsFor(IdType idType) {
		return getInstance().getIds(idType);
	}

	// ========== PRIVATE HELPER METHODS ==========

	/**
	 * Converts old-style identifier to ResourceLocation.
	 * <p>
	 * Strategy: Use ID type as namespace to ensure separation between types.
	 * This prevents item IDs from conflicting with creature IDs, etc.
	 * </p>
	 *
	 * @param identifier old identifier
	 * @param idType ID type
	 * @return ResourceLocation
	 */
	private static ResourceLocation convertToResourceLocation(String identifier, IIdType idType) {
		// Use type name as namespace for clear separation
		// e.g., "mymod.item" with ITEMTEMPLATE → ResourceLocation("itemtemplate", "mymod.item")
		String namespace = idType.typeName().toLowerCase(Locale.ROOT);

		// Clean up identifier (remove any existing type prefixes)
		String path = identifier;
		if (path.contains(":")) {
			String[] parts = path.split(":", 2);
			// If first part is type name, strip it
			if (parts[0].equalsIgnoreCase(idType.typeName())) {
				path = parts[1];
			}
		}

		return new ResourceLocation(namespace, path);
	}

	/**
	 * Extracts the original identifier from a ResourceLocation.
	 *
	 * @param location ResourceLocation
	 * @param idType ID type
	 * @return original identifier string
	 */
	private static String extractIdentifier(ResourceLocation location, IIdType idType) {
		// If namespace matches type, return just the path
		if (location.getNamespace().equalsIgnoreCase(idType.typeName())) {
			return location.getPath();
		}

		// Otherwise return full toString
		return location.toString();
	}
}

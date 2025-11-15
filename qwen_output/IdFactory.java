package com.garward.wurmmodloader.modsupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * IdFactory provides persistent ID management for custom items, creatures, and other
 * game objects that require unique identifiers. IDs are stored in the modsupport
 * database and are automatically assigned based on the ID type's allocation strategy.
 * </p>
 * <p>
 * Each {@link IdType} has its own allocation strategy:
 * <ul>
 *   <li><b>Counting Up</b> - IDs increment from a starting value (e.g., item templates)</li>
 *   <li><b>Counting Down</b> - IDs decrement from a starting value (e.g., actions)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Basic usage:</b>
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
 * </p>
 * <p>
 * <b>ID Persistence:</b><br>
 * IDs are stored in the modsupport database and persist across server restarts.
 * Once an ID is assigned to an identifier, it will always return the same ID
 * on subsequent calls. This ensures template IDs remain consistent.
 * </p>
 * <p>
 * <b>Thread Safety:</b><br>
 * This class is thread-safe. Multiple mods can request IDs concurrently without
 * risk of duplicate ID assignment.
 * </p>
 *
 * @see IdType
 * @see IIdType
 * @see com.garward.wurmmodloader.modsupport.ItemTemplateBuilder
 * @see com.garward.wurmmodloader.modsupport.CreatureTemplateBuilder
 * @since 1.0.0
 */
public class IdFactory {
	
	private static final Logger logger = Logger.getLogger(IdFactory.class.getName());
	private static IdFactory instance;
	
	/**
	 * Constructs a new IdFactory instance and initializes the database table.
	 * <p>
	 * This constructor is private as IdFactory follows the singleton pattern.
	 * The factory is initialized lazily when first accessed through
	 * {@link #getInstance()}.
	 * </p>
	 * 
	 * @see #init()
	 * @since 1.0.0
	 */
	private IdFactory() {
		init();
	}
	
	/**
	 * Initializes the database table for storing ID mappings.
	 * <p>
	 * Creates the IDS table if it doesn't exist. The table stores the mapping
	 * between string identifiers and their assigned integer IDs, along with
	 * the ID type.
	 * </p>
	 * <p>
	 * Table structure:
	 * <ul>
	 *   <li>ID (INT) - The assigned numeric ID</li>
	 *   <li>TYPE (VARCHAR(40)) - The ID type (e.g., "ITEMTEMPLATE")</li>
	 *   <li>NAME (VARCHAR(256)) - The string identifier</li>
	 * </ul>
	 * </p>
	 *
	 * @throws RuntimeException if database initialization fails
	 * @since 1.0.0
	 */
	private void init() {
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			if (!ModSupportDb.hasTable(dbcon, "IDS")) {
				try (Statement statement = dbcon.createStatement()) {
					statement.execute("CREATE TABLE IDS (ID INT, TYPE VARCHAR(40), NAME VARCHAR(256))");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Gets the singleton instance of IdFactory, creating it if necessary.
	 * <p>
	 * This method is synchronized to ensure thread-safe lazy initialization
	 * of the singleton instance.
	 * </p>
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
	 * If an ID already exists for the identifier, it returns the existing ID.
	 * Otherwise, a new ID is created based on the ID type's allocation strategy
	 * and stored in the database.
	 * </p>
	 * <p>
	 * The allocation strategy depends on the {@link IIdType#isCountingDown()}
	 * method:
	 * <ul>
	 *   <li>If counting down: assigns the next lower ID (min(ID) - 1)</li>
	 *   <li>If counting up: assigns the next higher ID (max(ID) + 1)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Uses SQL INSERT ... SELECT ... WHERE NOT EXISTS pattern to ensure atomic
	 * creation of new IDs without duplicates.
	 * </p>
	 *
	 * @param identifier unique string identifier for this object (e.g., "mymod.custom.item")
	 * @param idType the type of ID to allocate (determines allocation strategy)
	 * @return the persistent ID for this identifier (existing or newly created)
	 * @throws RuntimeException if database operations fail
	 * @since 1.0.0
	 */
	public int getId(String identifier, IIdType idType) {
		
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps1 = dbcon.prepareStatement("INSERT into IDS (ID, TYPE, NAME) SELECT (SELECT IFNULL(CASE ? WHEN 0 THEN MAX(ID) + 1 ELSE MIN(ID) - 1 END, ?)  FROM ids WHERE TYPE=?), ?, ? WHERE NOT EXISTS (SELECT * FROM IDS WHERE TYPE=? AND NAME=?)");
					PreparedStatement ps2 = dbcon.prepareStatement("SELECT ID FROM IDS WHERE TYPE=? AND NAME=?")) {
				ps1.setInt(1, idType.isCountingDown() ? 1 : 0);
				ps1.setInt(2, idType.startValue());
				ps1.setString(3, idType.typeName());
				ps1.setString(4, idType.typeName());
				ps1.setString(5, identifier);
				ps1.setString(6, idType.typeName());
				ps1.setString(7, identifier);
				
				ps2.setString(1, idType.typeName());
				ps2.setString(2, identifier);
				ps1.execute();
				try (ResultSet rs = ps2.executeQuery()) {
					if (rs.next()) {
						int id = rs.getInt(1);
						idType.updateLastUsedId(id);
						return id;
					}
					logger.log(Level.WARNING, "Resultset is empty");
					throw new RuntimeException("Failed to retrieve new id for " + identifier);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieve an existing ID for the given identifier, or -10 if not found.
	 * <p>
	 * Unlike {@link #getId(String, IIdType)}, this method will NOT create a
	 * new ID if one doesn't exist. Use this when you want to check if an ID
	 * has been previously allocated without creating a new one.
	 * </p>
	 *
	 * @param identifier unique string identifier to look up
	 * @param idType the type of ID to search for
	 * @return the existing ID, or -10 if no ID exists for this identifier
	 * @throws RuntimeException if database operations fail
	 * @since 1.0.0
	 */
	public int getExistingId(String identifier, IIdType idType) {
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps2 = dbcon.prepareStatement("SELECT ID FROM IDS WHERE TYPE=? AND NAME=?")) {
				ps2.setString(1, idType.typeName());
				ps2.setString(2, identifier);
				try (ResultSet rs = ps2.executeQuery()) {
					if (rs.next()) {
						int id = rs.getInt(1);
						idType.updateLastUsedId(id);
						return id;
					}
					return -10;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get all known IDs for a specific type.
	 * <p>
	 * Returns a list of entries mapping string identifiers to their assigned
	 * integer IDs, ordered by ID value. This is useful for debugging or
	 * enumerating all registered objects of a particular type.
	 * </p>
	 *
	 * @param idType the ID type to query
	 * @return list of entries mapping identifiers to IDs, ordered by ID
	 * @throws RuntimeException if database operations fail
	 * @since 1.0.0
	 */
	public List<Map.Entry<String, Integer>> getIds(IIdType idType) {
		List<Map.Entry<String, Integer>> results = new ArrayList<>();
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps2 = dbcon.prepareStatement("SELECT ID, NAME FROM IDS WHERE TYPE=? ORDER BY ID")) {
				ps2.setString(1, idType.typeName());
				try (ResultSet rs = ps2.executeQuery()) {
					while (rs.next()) {
						int id = rs.getInt(1);
						String name = rs.getString(2);
						results.add(new SimpleEntry<>(name, id));
					}
					return results;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get or create a persistent ID for the given identifier and type.
	 * <p>
	 * This is the primary method for obtaining IDs in mods. If an ID already exists
	 * for the identifier, it returns the existing ID. Otherwise, a new ID is created
	 * based on the ID type's allocation strategy and stored in the database.
	 * </p>
	 * <p>
	 * Example usage:
	 * <pre>{@code
	 * int swordId = IdFactory.getIdFor("mymod.iron.sword", IdType.ITEMTEMPLATE);
	 * int goblinId = IdFactory.getIdFor("mymod.goblin", IdType.CREATURETEMPLATE);
	 * }</pre>
	 * </p>
	 *
	 * @param identifier unique string identifier for this object (e.g., "mymod.custom.item")
	 * @param idType the type of ID to allocate (determines allocation strategy)
	 * @return the persistent ID for this identifier (existing or newly created)
	 * @throws RuntimeException if database operations fail
	 * @since 1.0.0
	 */
	public static int getIdFor(String identifier, IdType idType) {
		int id = getInstance().getId(identifier, idType);
		logger.log(Level.INFO, String.format("Using id %d for %s %s", id, idType.name().toLowerCase(Locale.ROOT), identifier));
		return id;
	}
	
	/**
	 * Retrieve an existing ID for the given identifier, or -10 if not found.
	 * <p>
	 * Unlike {@link #getIdFor(String, IdType)}, this method will NOT create a
	 * new ID if one doesn't exist. Use this when you want to check if an ID
	 * has been previously allocated without creating a new one.
	 * </p>
	 * <p>
	 * Example usage:
	 * <pre>{@code
	 * int existingId = IdFactory.getExistingIdFor("mymod.item", IdType.ITEMTEMPLATE);
	 * if (existingId != -10) {
	 *     // ID exists, we can use it
	 *     System.out.println("Item already has ID: " + existingId);
	 * } else {
	 *     // ID doesn't exist yet
	 *     System.out.println("Item has not been registered");
	 * }
	 * }</pre>
	 * </p>
	 *
	 * @param identifier unique string identifier to look up
	 * @param idType the type of ID to search for
	 * @return the existing ID, or -10 if no ID exists for this identifier
	 * @throws RuntimeException if database operations fail
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
	 * <p>
	 * Returns a list of entries mapping string identifiers to their assigned
	 * integer IDs, ordered by ID value. This is useful for debugging or
	 * enumerating all registered objects of a particular type.
	 * </p>
	 * <p>
	 * Example usage:
	 * <pre>{@code
	 * List<Entry<String, Integer>> itemTemplates = IdFactory.getIdsFor(IdType.ITEMTEMPLATE);
	 * for (Entry<String, Integer> entry : itemTemplates) {
	 *     System.out.println(entry.getKey() + " -> " + entry.getValue());
	 * }
	 * }</pre>
	 * </p>
	 *
	 * @param idType the ID type to query
	 * @return list of entries mapping identifiers to IDs, ordered by ID
	 * @throws RuntimeException if database operations fail
	 * @since 1.0.0
	 */
	public static List<Entry<String, Integer>> getIdsFor(IdType idType) {
		return getInstance().getIds(idType);
	}
}
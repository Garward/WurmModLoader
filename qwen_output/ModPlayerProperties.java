package com.garward.wurmmodloader.modsupport.properties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.garward.wurmmodloader.modsupport.IdFactory;
import com.garward.wurmmodloader.modsupport.IdType;
import com.garward.wurmmodloader.modsupport.ModSupportDb;

/**
 * Manages player-specific properties stored in the mod support database.
 * 
 * <p>This class provides functionality to store, retrieve, and delete custom properties
 * associated with individual players. Properties can be stored as integers, strings,
 * or floating-point numbers, and can optionally have expiration times.</p>
 * 
 * <p>Properties are stored in the PLAYERPROPS table with the following structure:
 * <ul>
 *   <li>ID: Unique identifier for each property entry</li>
 *   <li>PLAYERID: The player's unique identifier</li>
 *   <li>PROPID: Internal identifier for the property type</li>
 *   <li>PROPVAL: Integer value (optional)</li>
 *   <li>PROPSTR: String value (optional)</li>
 *   <li>PROPNUM: Floating-point value (optional)</li>
 *   <li>CREATED: Timestamp when the property was created</li>
 *   <li>EXPIRES: Optional expiration timestamp</li>
 * </ul>
 * </p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * // Store a string property for a player
 * ModPlayerProperties.getInstance().setPlayerProperty("favoriteColor", playerId, "blue");
 * 
 * // Retrieve properties for a player
 * List<Property> properties = ModPlayerProperties.getInstance().getPlayerProperties("favoriteColor", playerId);
 * 
 * // Delete all properties of a specific type for a player
 * ModPlayerProperties.getInstance().deletePlayerProperties("favoriteColor", playerId);
 * }</pre>
 * </p>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe. All public methods are
 * safe to call from multiple threads concurrently. The singleton instance is created
 * in a thread-safe manner.</p>
 * 
 * <p><strong>Lifecycle:</strong> This class should be instantiated during server startup
 * and remains active throughout the server's lifetime. The database table is automatically
 * created if it doesn't exist when the instance is first created.</p>
 * 
 * @since 1.0.0
 * @see Property
 * @see ModSupportDb
 * @see IdFactory
 */
public class ModPlayerProperties {
	
	private static ModPlayerProperties instance;
	private Map<String, Integer> propertyIds = new ConcurrentHashMap<>();
	
	/**
	 * Private constructor to enforce singleton pattern.
	 * 
	 * <p>Initializes the database table if it doesn't exist and prepares the
	 * internal property ID mapping.</p>
	 * 
	 * @since 1.0.0
	 */
	private ModPlayerProperties() {
		init();
	}
	
	/**
	 * Initializes the PLAYERPROPS database table if it doesn't exist.
	 * 
	 * <p>This method creates the PLAYERPROPS table with the following columns:
	 * ID, PLAYERID, PROPID, PROPVAL, PROPSTR, PROPNUM, CREATED, and EXPIRES.</p>
	 * 
	 * @since 1.0.0
	 * @throws RuntimeException if there's an error creating the database table
	 */
	private void init() {
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			if (!ModSupportDb.hasTable(dbcon, "PLAYERPROPS")) {
				try (Statement statement = dbcon.createStatement()) {
					statement.execute("CREATE TABLE PLAYERPROPS ("
							+ "ID INTEGER PRIMARY KEY AUTOINCREMENT,"
							+ "PLAYERID INT8 NOT NULL,"
							+ "PROPID INT NOT NULL,"
							+ "PROPVAL INT8,"
							+ "PROPSTR TEXT,"
							+ "PROPNUM REAL,"
							+ "CREATED INT8 NOT NULL,"
							+ "EXPIRES INT8)");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns the singleton instance of ModPlayerProperties.
	 * 
	 * <p>This method ensures thread-safe instantiation of the singleton instance.
	 * The instance is created lazily on first access.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is synchronized to ensure
	 * thread-safe lazy initialization of the singleton instance.</p>
	 * 
	 * @return the singleton instance of ModPlayerProperties
	 * @since 1.0.0
	 */
	public static synchronized ModPlayerProperties getInstance() {
		if (instance == null) {
			instance = new ModPlayerProperties();
		}
		return instance;
	}

	/**
	 * Gets or creates a unique ID for a property name.
	 * 
	 * <p>This method maps property names to unique integer IDs using the {@link IdFactory}.
	 * The mapping is cached in a concurrent map for performance.</p>
	 * 
	 * @param property the property name to get an ID for
	 * @return the unique integer ID for the property name
	 * @since 1.0.0
	 * @see IdFactory#getIdFor(String, IdType)
	 */
	private int getPropertyId(String property) {
		return propertyIds.computeIfAbsent(property, key -> IdFactory.getIdFor(key, IdType.PLAYERPROPERTY));
	}
	
	/**
	 * Retrieves all properties of a specific type for a player.
	 * 
	 * <p>This method is equivalent to calling {@link #getPlayerProperties(String, long, boolean)}
	 * with {@code includeExpired} set to {@code false}.</p>
	 * 
	 * @param property the property name to retrieve
	 * @param playerId the player's unique identifier
	 * @return a list of Property objects matching the criteria, never null
	 * @since 1.0.0
	 * @see #getPlayerProperties(String, long, boolean)
	 */
	public List<Property> getPlayerProperties(String property, long playerId) {
		return getPlayerProperties(property, playerId, false);
	}
	
	/**
	 * Retrieves all properties of a specific type for a player.
	 * 
	 * <p>This method queries the PLAYERPROPS database table for entries matching
	 * the specified property and player ID. Expired properties are filtered out
	 * unless includeExpired is set to true.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe and can be
	 * called concurrently from multiple threads.</p>
	 * 
	 * @param property the property name to retrieve
	 * @param playerId the player's unique identifier
	 * @param includeExpired whether to include expired properties in the results
	 * @return a list of Property objects matching the criteria, never null
	 * @since 1.0.0
	 * @throws RuntimeException if there's an error accessing the database
	 */
	public List<Property> getPlayerProperties(String property, long playerId, boolean includeExpired) {
		final long currentTimeMillis = System.currentTimeMillis();
		List<Property> properties = new ArrayList<>();
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps2 = dbcon.prepareStatement("SELECT ID, PROPVAL, PROPSTR, PROPNUM, CREATED, EXPIRES FROM PLAYERPROPS WHERE PROPID=? AND PLAYERID=?")) {
				
				ps2.setInt(1, getPropertyId(property));
				ps2.setLong(2, playerId);
				try (ResultSet rs = ps2.executeQuery()) {
					while (rs.next()) {
						Property p = new Property();
						p.setId(rs.getLong(1));
						if (rs.getObject(2) != null) {
							p.setIntValue(rs.getLong(2));
						}
						if (rs.getObject(3) != null) {
							p.setStrValue(rs.getString(3));
						}
						if (rs.getObject(4) != null) {
							p.setNumValue(rs.getFloat(4));
						}
						p.setCreated(rs.getInt(5));
						if (rs.getObject(6) != null) {
							p.setExpires(rs.getLong(6));
						}
						if (currentTimeMillis < p.getExpires() || includeExpired) {
							properties.add(p);
						}
					}
					return properties;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Sets a string property for a player.
	 * 
	 * <p>This method inserts a new property entry into the PLAYERPROPS table
	 * with the specified string value. The property is associated with the
	 * current timestamp as its creation time.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe and can be
	 * called concurrently from multiple threads.</p>
	 * 
	 * @param property the property name to set
	 * @param playerId the player's unique identifier
	 * @param value the string value to store
	 * @since 1.0.0
	 * @throws RuntimeException if there's an error accessing the database
	 */
	public void setPlayerProperty(String property, long playerId, String value) {
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps2 = dbcon.prepareStatement("INSERT INTO PLAYERPROPS (PROPID, PLAYERID, PROPSTR, CREATED) VALUES (?,?,?,?)")) {
				ps2.setInt(1, getPropertyId(property));
				ps2.setLong(2, playerId);
				ps2.setString(3, value);
				ps2.setLong(4, System.currentTimeMillis());
				ps2.execute();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Sets a long integer property for a player.
	 * 
	 * <p>This method inserts a new property entry into the PLAYERPROPS table
	 * with the specified long integer value. The property is associated with the
	 * current timestamp as its creation time.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe and can be
	 * called concurrently from multiple threads.</p>
	 * 
	 * @param property the property name to set
	 * @param playerId the player's unique identifier
	 * @param value the long integer value to store
	 * @since 1.0.0
	 * @throws RuntimeException if there's an error accessing the database
	 */
	public void setPlayerProperty(String property, long playerId, long value) {
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps2 = dbcon.prepareStatement("INSERT INTO PLAYERPROPS (PROPID, PLAYERID, PROPVAL, CREATED) VALUES (?,?,?,?)")) {
				ps2.setInt(1, getPropertyId(property));
				ps2.setLong(2, playerId);
				ps2.setLong(3, value);
				ps2.setLong(4, System.currentTimeMillis());
				ps2.execute();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Sets a floating-point property for a player.
	 * 
	 * <p>This method inserts a new property entry into the PLAYERPROPS table
	 * with the specified floating-point value. The property is associated with the
	 * current timestamp as its creation time.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe and can be
	 * called concurrently from multiple threads.</p>
	 * 
	 * @param property the property name to set
	 * @param playerId the player's unique identifier
	 * @param value the floating-point value to store
	 * @since 1.0.0
	 * @throws RuntimeException if there's an error accessing the database
	 */
	public void setPlayerProperty(String property, long playerId, float value) {
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps2 = dbcon.prepareStatement("INSERT INTO PLAYERPROPS (PROPID, PLAYERID, PROPNUM, CREATED) VALUES (?,?,?,?)")) {
				ps2.setInt(1, getPropertyId(property));
				ps2.setLong(2, playerId);
				ps2.setFloat(3, value);
				ps2.setLong(4, System.currentTimeMillis());
				ps2.execute();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Deletes all properties of a specific type for a player.
	 * 
	 * <p>This method removes all entries from the PLAYERPROPS table that match
	 * the specified property name and player ID.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe and can be
	 * called concurrently from multiple threads.</p>
	 * 
	 * @param property the property name to delete
	 * @param playerId the player's unique identifier
	 * @since 1.0.0
	 * @throws RuntimeException if there's an error accessing the database
	 */
	public void deletePlayerProperties(String property, long playerId) {
		try (Connection dbcon = ModSupportDb.getModSupportDb()) {
			try (PreparedStatement ps2 = dbcon.prepareStatement("DELETE FROM PLAYERPROPS WHERE PROPID=? AND PLAYERID=?")) {
				ps2.setInt(1, getPropertyId(property));
				ps2.setLong(2, playerId);
				ps2.execute();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
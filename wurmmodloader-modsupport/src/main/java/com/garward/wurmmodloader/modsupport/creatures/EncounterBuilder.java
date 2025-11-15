package com.garward.wurmmodloader.modsupport.creatures;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.wurmonline.server.zones.Encounter;
import com.wurmonline.server.zones.EncounterType;
import com.wurmonline.server.zones.SpawnTable;

/**
 * A builder class for creating and configuring {@link Encounter} instances that define
 * what creatures can spawn in specific tile types and elevations within the Wurm Online world.
 *
 * <p>This class provides a fluent API for adding creature templates to encounters and
 * registering them with the game's spawn system. It handles the complexity of working
 * with the internal {@link SpawnTable} and {@link EncounterType} classes through
 * reflection to maintain compatibility with different Wurm server versions.
 *
 * <p>Usage example:
 * <pre>{@code
 * // Create an encounter for forest tiles (tiletype 12) at surface level
 * new EncounterBuilder((byte) 12)
 *     .addCreatures(123, 5)  // Add 5 creatures of template ID 123
 *     .addCreatures(456, 3)  // Add 3 creatures of template ID 456
 *     .build(100);           // Register with 100% chance
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong> Instances of this class are designed to be used once
 * and discarded. After calling {@link #build(int)}, the internal encounter data is
 * registered with the spawn system and the builder should not be reused.
 *
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. All methods should
 * be called from the main server thread or properly synchronized if used in a
 * multi-threaded context.
 *
 * @since 1.0.0
 * @see Encounter
 * @see EncounterType
 * @see SpawnTable
 */
public class EncounterBuilder {

	private byte tiletype;
	private byte elevation;
	private Encounter encounter;
	
	private static Method spawnTableAddTileType;
	
	static {
		try {
			spawnTableAddTileType = SpawnTable.class.getDeclaredMethod("addTileType", EncounterType.class);
			spawnTableAddTileType.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Constructs a new EncounterBuilder for the specified tile type at surface elevation (0).
	 *
	 * @param tiletype the tile type identifier for which this encounter will be registered
	 * @since 1.0.0
	 * @see #EncounterBuilder(byte, byte)
	 */
	public EncounterBuilder(byte tiletype) {
		this(tiletype, (byte) 0);
	}

	/**
	 * Constructs a new EncounterBuilder for the specified tile type and elevation.
	 *
	 * @param tiletype the tile type identifier for which this encounter will be registered
	 * @param elevation the elevation level for which this encounter will be registered
	 * @since 1.0.0
	 * @see #EncounterBuilder(byte)
	 */
	public EncounterBuilder(byte tiletype, byte elevation) {
		this.tiletype = tiletype;
		this.elevation = elevation;
		
		this.encounter = new Encounter();
	}
	
	/**
	 * Adds creatures of the specified template ID to this encounter.
	 *
	 * <p>This method can be called multiple times to add different creature types
	 * to the same encounter. The count represents the relative weight or number
	 * of creatures of this type that will be considered when spawning.
	 *
	 * <p>Usage example:
	 * <pre>{@code
	 * builder.addCreatures(123, 5)  // Add 5 creatures of template ID 123
	 *        .addCreatures(456, 3); // Add 3 creatures of template ID 456
	 * }</pre>
	 *
	 * @param templateId the creature template ID to add to the encounter
	 * @param count the number or weight of creatures of this type
	 * @return this EncounterBuilder instance for method chaining
	 * @since 1.0.0
	 * @see Encounter#addType(int, int)
	 */
	public EncounterBuilder addCreatures(int templateId, int count) {
		encounter.addType(templateId, count);
		return this;
	}

	/**
	 * Builds and registers this encounter with the game's spawn system.
	 *
	 * <p>This method registers the configured encounter with the {@link SpawnTable}
	 * for the specified tile type and elevation. If no {@link EncounterType} exists
	 * for this tile type/elevation combination, one will be created automatically.
	 *
	 * <p>The chance parameter determines how frequently this encounter will be selected
	 * when creatures spawn in the specified tile type. A chance of 100 means it will
	 * always be considered, while lower values make it less likely to be selected.
	 *
	 * <p>Usage example:
	 * <pre>{@code
	 * // Register encounter with 50% chance of being selected
	 * builder.build(50);
	 * }</pre>
	 *
	 * <p><strong>Lifecycle:</strong> After calling this method, the builder's internal
	 * encounter data is registered and the builder should not be reused.
	 *
	 * @param chance the chance (0-100) that this encounter will be selected when spawning
	 * @throws RuntimeException if there are reflection errors accessing the spawn table
	 * @since 1.0.0
	 * @see SpawnTable#getType(byte, byte)
	 * @see EncounterType#addEncounter(Encounter, int)
	 */
	public void build(int chance) {
		if (encounter == null || chance == 0) {
			return;
		}

		try {
			EncounterType encounterType = SpawnTable.getType(tiletype, elevation);
			if (encounterType == null) {
				encounterType = new EncounterType(tiletype, elevation);
				spawnTableAddTileType.invoke(SpawnTable.class, encounterType);
			}
			encounterType.addEncounter(encounter, chance);
		} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

}
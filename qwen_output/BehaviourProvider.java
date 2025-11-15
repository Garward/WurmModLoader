package com.garward.wurmmodloader.modsupport.actions;

import java.util.List;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.structures.Wall;

/**
 * Interface for providing custom behaviours to creatures in the Wurm Unlimited server.
 * 
 * <p>This interface allows mod authors to inject custom {@link ActionEntry} instances into the game's
 * behaviour system, enabling new actions to be performed by creatures on various targets such as
 * items, tiles, creatures, structures, and more.</p>
 * 
 * <p>Implementations of this interface should be registered with the mod loader to have their
 * behaviours recognized by the game engine. Each method corresponds to a specific type of interaction
 * target, allowing fine-grained control over when custom actions should appear in context menus.</p>
 * 
 * <p>Example usage:
 * <pre>{@code
 * public class MyBehaviourProvider implements BehaviourProvider {
 *     @Override
 *     public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
 *         List<ActionEntry> entries = new ArrayList<>();
 *         if (target.getTemplateId() == 1234) { // Custom item ID
 *             entries.add(new ActionEntry((short) 1000, "Custom Action", "performing"));
 *         }
 *         return entries;
 *     }
 * }
 * }</pre>
 * </p>
 * 
 * <p><strong>Lifecycle:</strong> Implementations are typically instantiated during server startup
 * and remain active for the duration of the server session. Methods are called dynamically as
 * creatures interact with game objects.</p>
 * 
 * <p><strong>Thread Safety:</strong> Implementations should be thread-safe as these methods may be
 * called from multiple game threads concurrently. Avoid maintaining mutable state without proper
 * synchronization.</p>
 * 
 * @since 1.0.0
 * @see ActionEntry
 * @see Creature
 * @see Item
 * @see Wound
 * @see Skill
 * @see Wall
 * @see Fence
 * @see Floor
 * @see BridgePart
 */
public interface BehaviourProvider {

	/**
	 * Gets custom behaviours for a generic target identified by its ID.
	 * 
	 * <p>This method is called when a creature targets an object by its Wurm ID, which can represent
	 * various types of game objects including items, creatures, and structures.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param target the Wurm ID of the target object
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, long target) {
		return null;
	}

	/**
	 * Gets custom behaviours for tile interactions with an item context.
	 * 
	 * <p>This method is called when a creature uses an item on a specific tile location.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param object the item being used
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param tile the tile type identifier
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, int tile) {
		return null;
	}

	/**
	 * Gets custom behaviours for tile interactions without an item context.
	 * 
	 * <p>This method is called when a creature interacts with a tile location directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param tile the tile type identifier
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile) {
		return null;
	}

	/**
	 * Gets custom behaviours for directional tile interactions with an item context.
	 * 
	 * <p>This method is called when a creature uses an item on a tile with a specific direction.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param object the item being used
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param tile the tile type identifier
	 * @param dir the direction of the interaction
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, int tile, int dir) {
		return null;
	}

	/**
	 * Gets custom behaviours for directional tile interactions without an item context.
	 * 
	 * <p>This method is called when a creature interacts with a tile in a specific direction.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param tile the tile type identifier
	 * @param dir the direction of the interaction
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir) {
		return null;
	}

	/**
	 * Gets custom behaviours for tile border interactions with an item context.
	 * 
	 * <p>This method is called when a creature uses an item on a tile border with specific directional properties.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param object the item being used
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param dir the direction of the tile border
	 * @param border whether this is a border interaction
	 * @param heightOffset the height offset for the interaction
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, Tiles.TileBorderDirection dir, boolean border, int heightOffset) {
		return null;
	}

	/**
	 * Gets custom behaviours for tile border interactions without an item context.
	 * 
	 * <p>This method is called when a creature interacts with a tile border directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param dir the direction of the tile border
	 * @param border whether this is a border interaction
	 * @param heightOffset the height offset for the interaction
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, Tiles.TileBorderDirection dir, boolean border, int heightOffset) {
		return null;
	}

	/**
	 * Gets custom behaviours for tile corner interactions with an item context (deprecated).
	 * 
	 * <p><strong>Deprecated:</strong> This method is deprecated and may be removed in future versions.
	 * Use {@link #getBehavioursFor(Creature, Item, int, int, boolean, boolean, int, int)} instead.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param object the item being used
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param corner whether this is a corner interaction
	 * @param tile the tile type identifier
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @deprecated Use {@link #getBehavioursFor(Creature, Item, int, int, boolean, boolean, int, int)} instead
	 * @since 1.0.0
	 */
	@Deprecated
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, boolean corner, int tile) {
		return null;
	}

	/**
	 * Gets custom behaviours for tile corner interactions with an item context.
	 * 
	 * <p>This method is called when a creature uses an item on a tile corner with height offset.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param object the item being used
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param corner whether this is a corner interaction
	 * @param tile the tile type identifier
	 * @param heightOffset the height offset for the interaction
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset) {
		return getBehavioursFor(performer, object, tilex, tiley, onSurface, corner, tile);
	}

	/**
	 * Gets custom behaviours for tile corner interactions without an item context (deprecated).
	 * 
	 * <p><strong>Deprecated:</strong> This method is deprecated and may be removed in future versions.
	 * Use {@link #getBehavioursFor(Creature, int, int, boolean, boolean, int, int)} instead.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param corner whether this is a corner interaction
	 * @param tile the tile type identifier
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @deprecated Use {@link #getBehavioursFor(Creature, int, int, boolean, boolean, int, int)} instead
	 * @since 1.0.0
	 */
	@Deprecated
	public default List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile) {
		return null;
	}

	/**
	 * Gets custom behaviours for tile corner interactions without an item context.
	 * 
	 * <p>This method is called when a creature interacts with a tile corner with height offset.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param tilex the x-coordinate of the tile
	 * @param tiley the y-coordinate of the tile
	 * @param onSurface whether the action is taking place on the surface level
	 * @param corner whether this is a corner interaction
	 * @param tile the tile type identifier
	 * @param heightOffset the height offset for the interaction
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset) {
		return getBehavioursFor(performer, tilex, tiley, onSurface, corner, tile);
	}

	/**
	 * Gets custom behaviours for skill interactions with an item context.
	 * 
	 * <p>This method is called when a creature uses an item in the context of a specific skill.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param subject the item being used
	 * @param skill the skill being used
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Skill skill) {
		return null;
	}

	/**
	 * Gets custom behaviours for skill interactions without an item context.
	 * 
	 * <p>This method is called when a creature uses a skill directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param skill the skill being used
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Skill skill) {
		return null;
	}

	/**
	 * Gets custom behaviours for item interactions without a subject context.
	 * 
	 * <p>This method is called when a creature interacts with an item directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param target the item being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
		return null;
	}

	/**
	 * Gets custom behaviours for item-to-item interactions.
	 * 
	 * <p>This method is called when a creature uses one item on another item.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param subject the item being used
	 * @param target the item being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
		return null;
	}

	/**
	 * Gets custom behaviours for wound interactions without a subject context.
	 * 
	 * <p>This method is called when a creature interacts with a wound directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param target the wound being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Wound target) {
		return null;
	}

	/**
	 * Gets custom behaviours for item-to-wound interactions.
	 * 
	 * <p>This method is called when a creature uses an item on a wound.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param subject the item being used
	 * @param target the wound being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Wound target) {
		return null;
	}

	/**
	 * Gets custom behaviours for creature interactions without a subject context.
	 * 
	 * <p>This method is called when a creature interacts with another creature directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param target the creature being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
		return null;
	}

	/**
	 * Gets custom behaviours for item-to-creature interactions.
	 * 
	 * <p>This method is called when a creature uses an item on another creature.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param subject the item being used
	 * @param target the creature being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
		return null;
	}

	/**
	 * Gets custom behaviours for item-to-wall interactions.
	 * 
	 * <p>This method is called when a creature uses an item on a wall.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param subject the item being used
	 * @param target the wall being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Wall target) {
		return null;
	}

	/**
	 * Gets custom behaviours for wall interactions without a subject context.
	 * 
	 * <p>This method is called when a creature interacts with a wall directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param target the wall being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Wall target) {
		return null;
	}

	/**
	 * Gets custom behaviours for item-to-fence interactions.
	 * 
	 * <p>This method is called when a creature uses an item on a fence.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param subject the item being used
	 * @param target the fence being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Fence target) {
		return null;
	}

	/**
	 * Gets custom behaviours for fence interactions without a subject context.
	 * 
	 * <p>This method is called when a creature interacts with a fence directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param target the fence being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Fence target) {
		return null;
	}

	/**
	 * Gets custom behaviours for planetary item interactions.
	 * 
	 * <p>This method is called when a creature uses an item in the context of a specific planet.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param object the item being used
	 * @param planetId the ID of the planet
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, Item object, int planetId) {
		return null;
	}

	/**
	 * Gets custom behaviours for planetary interactions without an item context.
	 * 
	 * <p>This method is called when a creature interacts with a planet directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param planetId the ID of the planet
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, int planetId) {
		return null;
	}

	/**
	 * Gets custom behaviours for floor interactions without an item context.
	 * 
	 * <p>This method is called when a creature interacts with a floor structure directly.</p>
	 * 
	 * @param performer the creature performing the action
	 * @param onSurface whether the action is taking place on the surface level
	 * @param floor the floor being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature performer, boolean onSurface, Floor floor) {
		return null;
	}

	/**
	 * Gets custom behaviours for item-to-floor interactions.
	 * 
	 * <p>This method is called when a creature uses an item on a floor structure.</p>
	 * 
	 * @param creature the creature performing the action
	 * @param item the item being used
	 * @param onSurface whether the action is taking place on the surface level
	 * @param floor the floor being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature creature, Item item, boolean onSurface, Floor floor) {
		return null;
	}

	/**
	 * Gets custom behaviours for bridge part interactions without an item context.
	 * 
	 * <p>This method is called when a creature interacts with a bridge part directly.</p>
	 * 
	 * @param aPerformer the creature performing the action
	 * @param aOnSurface whether the action is taking place on the surface level
	 * @param aBridgePart the bridge part being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature aPerformer, boolean aOnSurface, BridgePart aBridgePart) {
		return null;
	}

	/**
	 * Gets custom behaviours for item-to-bridge part interactions.
	 * 
	 * <p>This method is called when a creature uses an item on a bridge part.</p>
	 * 
	 * @param aPerformer the creature performing the action
	 * @param item the item being used
	 * @param aOnSurface whether the action is taking place on the surface level
	 * @param aBridgePart the bridge part being targeted
	 * @return a list of custom {@link ActionEntry} instances, or {@code null} if no custom behaviours
	 * @since 1.0.0
	 */
	public default List<ActionEntry> getBehavioursFor(Creature aPerformer, Item item, boolean aOnSurface, BridgePart aBridgePart) {
		return null;
	}
}
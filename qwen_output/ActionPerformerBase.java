package com.garward.wurmmodloader.modsupport.actions;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.shared.constants.CounterTypes;

/**
 * Base interface for action performers in the Wurm Unlimited server modding framework.
 * 
 * <p>This interface defines the contract for handling various game actions that can be performed
 * by creatures in the game world. Implementations of this interface are responsible for
 * processing actions related to tiles, items, creatures, structures, and other game objects.
 * 
 * <p>Each method corresponds to a specific type of action target, identified by its counter type
 * from {@link CounterTypes}. The methods are grouped by target type for easier navigation.
 * 
 * <p>Usage example:
 * <pre>{@code
 * public class MyActionPerformer implements ActionPerformerBase {
 *     private final short actionId;
 *     
 *     public MyActionPerformer(short actionId) {
 *         this.actionId = actionId;
 *     }
 *     
 *     @Override
 *     public short getActionId() {
 *         return actionId;
 *     }
 *     
 *     @Override
 *     public boolean action(Action action, Creature performer, Item target, short num, float counter) {
 *         // Handle item action logic here
 *         return true;
 *     }
 *     
 *     // Implement other required methods...
 * }
 * }</pre>
 * 
 * <p><strong>Lifecycle:</strong> Instances of implementing classes should be registered
 * with the action system during mod initialization to receive action callbacks.
 * 
 * <p><strong>Thread Safety:</strong> Implementations should be thread-safe as action methods
 * may be called from multiple game threads. Synchronization should be used when accessing
 * shared state.
 * 
 * @since 1.0.0
 * @see com.wurmonline.server.behaviours.Action
 * @see com.wurmonline.server.creatures.Creature
 * @see com.wurmonline.server.items.Item
 * @see com.wurmonline.shared.constants.CounterTypes
 */
public interface ActionPerformerBase {

    /**
     * Gets the unique action ID that this performer handles.
     * 
     * <p>This ID is used by the action system to route actions to the appropriate performer.
     * Each performer should have a unique ID within the mod.
     * 
     * @return the action ID for this performer
     * @since 1.0.0
     */
    short getActionId();

    //
    // Tile corners
    //

    /**
     * Handles actions performed on tile corners.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILECORNER} (27)
     * 
     * <p><strong>Deprecated:</strong> Replaced with 
     * {@link #action(Action, Creature, Item, int, int, boolean, boolean, int, int, short, float)}
     * which includes height offset parameter.
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used (may be null)
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param corner always true for this method
     * @param tile the tile type identifier
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     * @deprecated use {@link #action(Action, Creature, Item, int, int, boolean, boolean, int, int, short, float)} instead
     */
    @Deprecated
    boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter);

    /**
     * Handles actions performed on tile corners with height offset support.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILECORNER} (27)
     * 
     * <p>The corner parameter is always true for this method.
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used (may be null)
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param corner always true for this method
     * @param tile the tile type identifier
     * @param heightOffset the height offset for the action
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.4
     */
    boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter);

    /**
     * Handles actions performed on tile corners without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILECORNER} (27)
     * 
     * <p><strong>Deprecated:</strong> Replaced with 
     * {@link #action(Action, Creature, int, int, boolean, boolean, int, int, short, float)}
     * which includes height offset parameter.
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param corner always true for this method
     * @param tile the tile type identifier
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     * @deprecated use {@link #action(Action, Creature, int, int, boolean, boolean, int, int, short, float)} instead
     */
    @Deprecated
    boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter);

    /**
     * Handles actions performed on tile corners without item source, with height offset support.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILECORNER} (27)
     * 
     * <p>The corner parameter is always true for this method.
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param corner always true for this method
     * @param tile the tile type identifier
     * @param heightOffset the height offset for the action
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.4
     */
    boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter);

    //
    // Tiles (surface)
    //

    /**
     * Handles actions performed on surface tiles without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILES} (3)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param tile the tile type identifier
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short num, float counter);

    /**
     * Handles actions performed on surface tiles with item source and height offset.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILES} (3)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param heightOffset the height offset for the action
     * @param tile the tile type identifier
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short num, float counter);

    //
    // Planets, missions, tickets
    //

    /**
     * Handles actions performed on planets, missions, or tickets without item source.
     * 
     * <p>Counter types:
     * <ul>
     *   <li>{@link CounterTypes#COUNTER_TYPE_PLANETS} (14)</li>
     *   <li>{@link CounterTypes#COUNTER_TYPE_MISSIONPERFORMED} (22)</li>
     *   <li>{@link CounterTypes#COUNTER_TYPE_TICKETS} (25)</li>
     * </ul>
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param id the identifier for the planet, mission, or ticket
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, int id, short num, float counter);

    /**
     * Handles actions performed on planets or missions with item source.
     * 
     * <p>Counter types:
     * <ul>
     *   <li>{@link CounterTypes#COUNTER_TYPE_PLANETS} (14)</li>
     *   <li>{@link CounterTypes#COUNTER_TYPE_MISSIONPERFORMED} (22)</li>
     * </ul>
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param id the identifier for the planet or mission
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, int id, short num, float counter);

    //
    // Wounds
    //

    /**
     * Handles actions performed on wounds without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_WOUNDS} (8)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param target the wound being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Wound target, short num, float counter);

    /**
     * Handles actions performed on wounds with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_WOUNDS} (8)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param target the wound being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, Wound target, short num, float counter);

    //
    // Items
    //

    /**
     * Handles actions performed on items with item source (item-to-item actions).
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_ITEMS} (2)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param target the item being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, Item target, short num, float counter);

    /**
     * Handles actions performed on items without item source (creature-to-item actions).
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_ITEMS} (2)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param target the item being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item target, short num, float counter);

    /**
     * Handles multi-item actions performed on multiple items at once.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_ITEMS} (2)
     * 
     * <p>Multi-action variant.
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param targets the items being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item[] targets, short num, float counter);

    //
    // Creatures
    //

    /**
     * Handles actions performed on creatures with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_CREATURES} (1)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param target the creature being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter);

    /**
     * Handles actions performed on creatures without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_CREATURES} (1)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param target the creature being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Creature target, short num, float counter);

    //
    // Walls
    //

    /**
     * Handles actions performed on walls with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_WALLS} (5)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param target the wall being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, Wall target, short num, float counter);

    /**
     * Handles actions performed on walls without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_WALLS} (5)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param target the wall being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Wall target, short num, float counter);

    //
    // Fences
    //

    /**
     * Handles actions performed on fences with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_FENCES} (7)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param onSurface whether the action is on the surface level
     * @param target the fence being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, boolean onSurface, Fence target, short num, float counter);

    /**
     * Handles actions performed on fences without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_FENCES} (7)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param onSurface whether the action is on the surface level
     * @param target the fence being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, boolean onSurface, Fence target, short num, float counter);

    //
    // Skills
    //

    /**
     * Handles actions performed on skills with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_SKILLIDS} (18)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param skill the skill being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, Skill skill, short num, float counter);

    /**
     * Handles actions performed on skills without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_SKILLIDS} (18)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param skill the skill being acted upon
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Skill skill, short num, float counter);

    //
    // Floors
    //

    /**
     * Handles actions performed on floors with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_FLOORS} (23)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param onSurface whether the action is on the surface level
     * @param target the floor being acted upon
     * @param encodedTile the encoded tile information
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter);

    /**
     * Handles actions performed on floors without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_FLOORS} (23)
     * 
     * <p><strong>Deprecated:</strong> This method is unused.
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param onSurface whether the action is on the surface level
     * @param floor the floor being acted upon
     * @param encodedTile the encoded tile information
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     * @deprecated unused method
     */
    @Deprecated
    boolean action(Action action, Creature performer, boolean onSurface, Floor floor, int encodedTile, short num, float counter);

    //
    // Tile border
    //

    /**
     * Handles actions performed on tile borders with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILEBORDER} (12)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param heightOffset the height offset for the action
     * @param dir the direction of the tile border
     * @param borderId the unique identifier of the border
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, Tiles.TileBorderDirection dir, long borderId, short num, float counter);

    /**
     * Handles actions performed on tile borders without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_TILEBORDER} (12)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level
     * @param dir the direction of the tile border
     * @param borderId the unique identifier of the border
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, Tiles.TileBorderDirection dir, long borderId, short num, float counter);

    //
    // Bridges
    //

    /**
     * Handles actions performed on bridges without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_BRIDGE_PARTS} (28)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param onSurface whether the action is on the surface level
     * @param bridgePart the bridge part being acted upon
     * @param encodedTile the encoded tile information
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter);

    /**
     * Handles actions performed on bridges with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_BRIDGE_PARTS} (28)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param item the item being used
     * @param onSurface whether the action is on the surface level
     * @param bridgePart the bridge part being acted upon
     * @param encodedTile the encoded tile information
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.0.0
     */
    boolean action(Action action, Creature performer, Item item, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter);

    //
    // Cave tiles
    //

    /**
     * Handles actions performed on cave tiles without item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_CAVETILES} (17)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level (false for cave tiles)
     * @param tile the tile type identifier
     * @param dir the direction associated with the cave tile
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.5
     */
    boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir, short num, float counter);

    /**
     * Handles actions performed on cave tiles with item source.
     * 
     * <p>Counter type: {@link CounterTypes#COUNTER_TYPE_CAVETILES} (17)
     * 
     * @param action the action being performed
     * @param performer the creature performing the action
     * @param source the item being used
     * @param tilex the x-coordinate of the tile
     * @param tiley the y-coordinate of the tile
     * @param onSurface whether the action is on the surface level (false for cave tiles)
     * @param heightOffset the height offset for the action
     * @param tile the tile type identifier
     * @param dir the direction associated with the cave tile
     * @param num action-specific parameter
     * @param counter the action progress counter
     * @return true if the action was handled, false otherwise
     * @since 1.5
     */
    boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, int dir, short num, float counter);
}
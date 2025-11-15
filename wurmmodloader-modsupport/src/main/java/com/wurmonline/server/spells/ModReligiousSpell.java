package com.wurmonline.server.spells;

import com.wurmonline.mesh.Tiles.TileBorderDirection;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;

/**
 * A modifiable religious spell implementation that extends the base ReligiousSpell functionality.
 *
 * <p>This class provides a framework for creating custom religious spells with modified behavior
 * while maintaining compatibility with the core spell system. It delegates all spell effects
 * to the parent class by default, allowing subclasses to override specific behaviors.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * public class CustomBlessingSpell extends ModReligiousSpell {
 *     public CustomBlessingSpell() {
 *         super("Custom Blessing", 1001, 30, 50, 40, 30, 300000L);
 *     }
 *
 *     @Override
 *     public void doEffect(Skill castSkill, double power, Creature performer, Creature target) {
 *         // Custom blessing logic here
 *         target.getStatus().setStamina(target.getStatus().getStamina() + 1000);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong> Instances are typically created at server startup and remain active
 * throughout the server's lifetime. Spell instances should be thread-safe as they may be called
 * concurrently by multiple creatures casting spells.</p>
 *
 * @since 1.0.0
 */
public class ModReligiousSpell extends ReligiousSpell {

	/**
	 * Constructs a new modifiable religious spell with the specified parameters.
	 *
	 * @param aName the name of the spell as it appears in-game
	 * @param aNum the unique identifier number for this spell
	 * @param aCastingTime the time in seconds required to cast this spell
	 * @param aCost the mana cost to cast this spell
	 * @param aDifficulty the skill difficulty required to learn/cast this spell
	 * @param aLevel the minimum skill level required to cast this spell
	 * @param cooldown the cooldown period in milliseconds between castings
	 * @since 1.0.0
	 */
	public ModReligiousSpell(String aName, int aNum, int aCastingTime, int aCost, int aDifficulty, int aLevel, long cooldown) {
		super(aName, aNum, aCastingTime, aCost, aDifficulty, aLevel, cooldown);
	}

	/**
	 * Executes the primary effect of this spell when targeting a creature.
	 *
	 * <p>This method is called when the spell successfully completes casting on a creature target.
	 * By default, it delegates to the parent class implementation but can be overridden to provide
	 * custom spell effects.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method may be called concurrently from multiple threads,
	 * so implementations should avoid modifying shared state without proper synchronization.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param power the power level of the spell (0.0 to 100.0)
	 * @param performer the creature casting the spell
	 * @param target the creature target of the spell
	 * @since 1.0.0
	 */
	@Override
	public void doEffect(Skill castSkill, double power, Creature performer, Creature target) {
		super.doEffect(castSkill, power, performer, target);
	}

	/**
	 * Executes the negative effect of this spell when targeting a creature.
	 *
	 * <p>This method is called when the spell fails or has a negative outcome on a creature target.
	 * By default, it delegates to the parent class implementation but can be overridden to provide
	 * custom negative spell effects.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param power the power level of the spell (0.0 to 100.0)
	 * @param performer the creature casting the spell
	 * @param target the creature target of the spell
	 * @since 1.0.0
	 */
	@Override
	public void doNegativeEffect(Skill castSkill, double power, Creature performer, Creature target) {
		super.doNegativeEffect(castSkill, power, performer, target);
	}

	/**
	 * Executes the primary effect of this spell when targeting an item.
	 *
	 * <p>This method is called when the spell successfully completes casting on an item target.
	 * By default, it delegates to the parent class implementation but can be overridden to provide
	 * custom spell effects.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param power the power level of the spell (0.0 to 100.0)
	 * @param performer the creature casting the spell
	 * @param target the item target of the spell
	 * @since 1.0.0
	 */
	@Override
	public void doEffect(Skill castSkill, double power, Creature performer, Item target) {
		super.doEffect(castSkill, power, performer, target);
	}

	/**
	 * Executes the negative effect of this spell when targeting an item.
	 *
	 * <p>This method is called when the spell fails or has a negative outcome on an item target.
	 * By default, it delegates to the parent class implementation but can be overridden to provide
	 * custom negative spell effects.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param power the power level of the spell (0.0 to 100.0)
	 * @param performer the creature casting the spell
	 * @param target the item target of the spell
	 * @since 1.0.0
	 */
	@Override
	public void doNegativeEffect(Skill castSkill, double power, Creature performer, Item target) {
		super.doNegativeEffect(castSkill, power, performer, target);
	}

	/**
	 * Executes the primary effect of this spell when targeting a wound.
	 *
	 * <p>This method is called when the spell successfully completes casting on a wound target.
	 * By default, it delegates to the parent class implementation but can be overridden to provide
	 * custom spell effects.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param power the power level of the spell (0.0 to 100.0)
	 * @param performer the creature casting the spell
	 * @param target the wound target of the spell
	 * @since 1.0.0
	 */
	@Override
	public void doEffect(Skill castSkill, double power, Creature performer, Wound target) {
		super.doEffect(castSkill, power, performer, target);
	}

	/**
	 * Executes the primary effect of this spell when targeting a tile border.
	 *
	 * <p>This method is called when the spell successfully completes casting on a tile border target.
	 * By default, it delegates to the parent class implementation but can be overridden to provide
	 * custom spell effects.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param power the power level of the spell (0.0 to 100.0)
	 * @param performer the creature casting the spell
	 * @param tilex the x-coordinate of the targeted tile
	 * @param tiley the y-coordinate of the targeted tile
	 * @param layer the layer of the targeted tile
	 * @param heightOffset the height offset within the tile
	 * @param dir the direction of the tile border
	 * @since 1.0.0
	 */
	@Override
	public void doEffect(Skill castSkill, double power, Creature performer, int tilex, int tiley, int layer, int heightOffset, TileBorderDirection dir) {
		super.doEffect(castSkill, power, performer, tilex, tiley, layer, heightOffset, dir);
	}

	/**
	 * Executes the primary effect of this spell when targeting a tile.
	 *
	 * <p>This method is called when the spell successfully completes casting on a tile target.
	 * By default, it delegates to the parent class implementation but can be overridden to provide
	 * custom spell effects.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param power the power level of the spell (0.0 to 100.0)
	 * @param performer the creature casting the spell
	 * @param tilex the x-coordinate of the targeted tile
	 * @param tiley the y-coordinate of the targeted tile
	 * @param layer the layer of the targeted tile
	 * @param heightOffset the height offset within the tile
	 * @since 1.0.0
	 */
	@Override
	public void doEffect(Skill castSkill, double power, Creature performer, int tilex, int tiley, int layer, int heightOffset) {
		super.doEffect(castSkill, power, performer, tilex, tiley, layer, heightOffset);
	}

	/**
	 * Checks if the preconditions for casting this spell on a creature target are met.
	 *
	 * <p>This method is called before spell casting begins to verify that the spell can be cast
	 * on the specified creature target. By default, it delegates to the parent class implementation
	 * but can be overridden to add custom preconditions.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param performer the creature casting the spell
	 * @param target the creature target of the spell
	 * @return true if the preconditions are met, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean precondition(Skill castSkill, Creature performer, Creature target) {
		return super.precondition(castSkill, performer, target);
	}

	/**
	 * Checks if the preconditions for casting this spell on an item target are met.
	 *
	 * <p>This method is called before spell casting begins to verify that the spell can be cast
	 * on the specified item target. By default, it delegates to the parent class implementation
	 * but can be overridden to add custom preconditions.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param performer the creature casting the spell
	 * @param target the item target of the spell
	 * @return true if the preconditions are met, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean precondition(Skill castSkill, Creature performer, Item target) {
		return super.precondition(castSkill, performer, target);
	}

	/**
	 * Checks if the postconditions for casting this spell on an item target are met.
	 *
	 * <p>This method is called after spell casting completes to verify that the spell effect
	 * should be applied to the specified item target. By default, it delegates to the parent
	 * class implementation but can be overridden to add custom postconditions.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param performer the creature casting the spell
	 * @param target the item target of the spell
	 * @param effect the calculated effect power of the spell
	 * @return true if the postconditions are met, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean postcondition(Skill castSkill, Creature performer, Item target, double effect) {
		return super.postcondition(castSkill, performer, target, effect);
	}

	/**
	 * Checks if the preconditions for casting this spell on a wound target are met.
	 *
	 * <p>This method is called before spell casting begins to verify that the spell can be cast
	 * on the specified wound target. By default, it delegates to the parent class implementation
	 * but can be overridden to add custom preconditions.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param performer the creature casting the spell
	 * @param target the wound target of the spell
	 * @return true if the preconditions are met, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean precondition(Skill castSkill, Creature performer, Wound target) {
		return super.precondition(castSkill, performer, target);
	}

	/**
	 * Checks if the preconditions for casting this spell on a tile target are met.
	 *
	 * <p>This method is called before spell casting begins to verify that the spell can be cast
	 * on the specified tile target. By default, it delegates to the parent class implementation
	 * but can be overridden to add custom preconditions.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param performer the creature casting the spell
	 * @param tilex the x-coordinate of the targeted tile
	 * @param tiley the y-coordinate of the targeted tile
	 * @param layer the layer of the targeted tile
	 * @return true if the preconditions are met, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean precondition(Skill castSkill, Creature performer, int tilex, int tiley, int layer) {
		return super.precondition(castSkill, performer, tilex, tiley, layer);
	}

	/**
	 * Checks if the preconditions for casting this spell on a tile border target are met.
	 *
	 * <p>This method is called before spell casting begins to verify that the spell can be cast
	 * on the specified tile border target. By default, it delegates to the parent class implementation
	 * but can be overridden to add custom preconditions.</p>
	 *
	 * @param castSkill the skill used to cast this spell
	 * @param performer the creature casting the spell
	 * @param tilex the x-coordinate of the targeted tile
	 * @param tiley the y-coordinate of the targeted tile
	 * @param layer the layer of the targeted tile
	 * @param heightOffset the height offset within the tile
	 * @param dir the direction of the tile border
	 * @return true if the preconditions are met, false otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean precondition(Skill castSkill, Creature performer, int tilex, int tiley, int layer, int heightOffset, TileBorderDirection dir) {
		return super.precondition(castSkill, performer, tilex, tiley, layer, heightOffset, dir);
	}

}

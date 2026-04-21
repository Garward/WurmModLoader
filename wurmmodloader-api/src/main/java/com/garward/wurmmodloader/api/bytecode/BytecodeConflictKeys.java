package com.garward.wurmmodloader.api.bytecode;

/**
 * Shared conflict-key constants that help different patches coordinate without stringly-typed copy/paste.
 *
 * <p>Third-party mods are encouraged to re-use these keys (or extend them using their own namespaces)
 * so the runtime can make intelligent decisions when multiple patches try to mutate the same call site.</p>
 */
public final class BytecodeConflictKeys {

    private BytecodeConflictKeys() {}

    public static final String SERVER_LIFECYCLE_START = "server.lifecycle.start";
    public static final String SERVER_LIFECYCLE_SHUTDOWN = "server.lifecycle.shutdown";
    public static final String SERVER_LIFECYCLE_ITEM_TEMPLATES = "server.lifecycle.itemTemplates";
    public static final String SERVER_POLL = "server.poll";
    public static final String PLAYER_LOGIN = "player.login";
    public static final String PLAYER_LOGOUT = "player.logout";
    public static final String PRIEST_RESTRICTION = "player.priestRestriction";
    public static final String PRAYER_FAITH = "player.prayerFaith";
    public static final String BODY_MENU = "player.bodyMenu";
    public static final String BODY_MENU_ACTIONS = "player.bodyMenu.actions";
    public static final String COMMUNICATOR_MESSAGE = "communications.message";
    public static final String COMMUNICATOR_CHANNEL = "communications.channel";
    public static final String CREATURE_DEATH = "creature.death";
    public static final String CREATURE_EXAMINE = "creature.examine";
    public static final String CREATURE_BREED = "creature.breed";
    public static final String CROP_HARVEST = "farming.harvest";
    public static final String CROP_GROWTH = "farming.growth";
    public static final String COMBAT_DAMAGE = "combat.damage";
    public static final String COMBAT_CRITICAL = "combat.critical";
    public static final String CREATURE_SPAWN = "creature.spawn";
    public static final String CREATURE_POSITION = "creature.position";
    public static final String COMBAT_ATTACK = "combat.attack";
    public static final String ITEM_EXAMINE = "item.examine";
    public static final String ITEM_ENCHANTMENT_STRINGS = "item.enchantmentStrings";
    public static final String ITEM_DROP = "item.drop";
    public static final String ITEM_TRADE = "item.trade";
    public static final String ITEM_CONTAINER = "item.container";
    public static final String VEHICLE_MOUNT_CREATURE = "vehicle.mount.creature";
    public static final String VEHICLE_MOUNT_ITEM = "vehicle.mount.item";
    public static final String VEHICLE_SPEED = "vehicle.speed";
    public static final String VEHICLE_EQUIPMENT_CHECK = "vehicle.equipment";
    public static final String ACTION_TIME = "actions.time";
    public static final String ACTION_STAMINA = "actions.stamina";
    public static final String ACTION_FATIGUE = "actions.fatigue";
    public static final String SKILL_ADVANCE = "skill.advance";
    public static final String COMBAT_SWING_SPEED = "combat.swingSpeed";
    public static final String COMBAT_WEAPON_USE = "combat.weaponUse";
    public static final String COMBAT_OPPORTUNITY = "combat.opportunity";
    public static final String COMBAT_SHIELD = "combat.shield";
    public static final String COMBAT_DUAL_WIELD = "combat.dualWield";
    public static final String COMBAT_SPECIAL_MOVE_SEND = "combat.specialMove.send";
    public static final String COMBAT_SPECIAL_MOVE_HANDLE = "combat.specialMove.handle";
    public static final String PLAYER_SKILL_LOSS = "player.skillLoss";
    public static final String MATERIAL_DAMAGE_MODIFIER = "material.damageModifier";
    public static final String MATERIAL_DECAY_MODIFIER = "material.decayModifier";
    public static final String MATERIAL_IMP_BONUS = "material.impBonus";
    public static final String MATERIAL_REPAIR_TIME = "material.repairTime";
    public static final String MATERIAL_CREATION_BONUS = "material.creationBonus";
    public static final String MATERIAL_LOCKPICK_BONUS = "material.lockpickBonus";
    public static final String MATERIAL_ANCHOR_BONUS = "material.anchorBonus";
    public static final String MATERIAL_SPELL_POWER_BONUS = "material.spellPowerBonus";
    public static final String MATERIAL_ARMOUR_BONUS = "material.armourBonus";
    public static final String MATERIAL_MOVEMENT_BONUS = "material.movementBonus";
    public static final String MATERIAL_PENDULUM_BONUS = "material.pendulumBonus";
    public static final String MATERIAL_SHATTER_BONUS = "material.shatterBonus";
    public static final String MOVEMENT_BROADCAST = "movement.broadcast";
    public static final String PLAYER_MOVEMENT_BROADCAST = "movement.player.broadcast";
    public static final String WEAPON_MATERIAL_DAMAGE = "weapon.materialDamage";
    public static final String WEAPON_MATERIAL_ARMOUR_DAMAGE = "weapon.materialArmourDamage";
    public static final String WEAPON_MATERIAL_PARRY = "weapon.materialParry";
    public static final String WEAPON_MATERIAL_BASH = "weapon.materialBash";
    public static final String WEAPON_BASE_SPEED = "weapon.baseSpeed";
    public static final String TAME_CHARM = "taming.charm";
    public static final String TAME_DOMINATE = "taming.dominate";
    public static final String PET_RELEASED = "taming.petReleased";
    public static final String DB_CONNECTOR_INIT = "database.connector.init";
    public static final String DB_CONNECTION_OPENED = "database.connection.opened";
    public static final String DB_MIGRATION = "database.migration";
    public static final String DB_INDEX_MAINTENANCE = "database.index.maintenance";
    public static final String VANILLAFIX_NEXTINT_GUARDS = "vanillafix.nextIntGuards";
    public static final String VANILLAFIX_ACTION_TIMER = "vanillafix.actionTimer";
    public static final String VANILLAFIX_NPC_MOVE_TARGET_GUARD = "vanillafix.npcMoveTargetGuard";
    public static final String SPELL_CAST_TIME = "spell.castTime";
    public static final String SPELL_COOLDOWN = "spell.cooldown";
    public static final String SPELL_POWER = "spell.power";
    public static final String SPELL_CAST_ATTEMPT = "spell.castAttempt";
    public static final String SPELL_EFFECT = "spell.effect";
    public static final String SPELL_DIFFICULTY = "spell.difficulty";
    public static final String DEITY_SPELL_REGISTRATION = "deity.spellRegistration";
    public static final String SPELL_PRECONDITION = "spell.precondition";
    public static final String SPELL_RESIST = "spell.resist";
    public static final String SPELL_VISIBILITY = "spell.visibility";
    public static final String SACRIFICE_ACCEPTANCE = "priest.sacrifice.acceptance";
    public static final String SACRIFICE_FAVOR_VALUE = "priest.sacrifice.favorValue";
    public static final String SACRIFICE_FAVOR_MODIFIER = "priest.sacrifice.favorModifier";
}

package com.garward.wurmmodloader.modloader.server;

import com.garward.wurmmodloader.api.events.combat.OpportunityAttackEvent;
import com.wurmonline.server.Message;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;

/**
 * Hook into com.wurmonline.server.Server.startRunning()
 * 
 * The InvocationHandler calls startRunning() first, then fires onServerStarted
 * event
 */
public class ProxyServerHook extends ServerHook {

	private static ProxyServerHook instance;

	private boolean playerHooksRegistered = false;

	private static final ThreadLocal<OpportunityContext> OPPORTUNITY_CONTEXT =
		ThreadLocal.withInitial(OpportunityContext::new);

	// Captured attackers map (stored before die() clears them, retrieved by LootManager)
	private static final java.util.concurrent.ConcurrentHashMap<Long, java.util.Map<Long, Long>> CAPTURED_ATTACKERS =
		new java.util.concurrent.ConcurrentHashMap<>();

	// Damage tracking per creature (victimId -> attackerId -> total damage dealt)
	private static final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.ConcurrentHashMap<Long, Double>> DAMAGE_TRACKING =
		new java.util.concurrent.ConcurrentHashMap<>();

	// Note: Capability hooks are installed in DelegatedLauncher.main()
	// BEFORE this class is ever loaded. See DelegatedLauncher.java:22

	private ProxyServerHook() {
		// Creature and combat hooks (Phase 3 event system) are installed via BytecodePatch implementations.
		// Item hooks (Phase 3 - for soulbound gear and RPG mods) are installed via BytecodePatch implementations.

		// Phase 4+ hooks are installed via BytecodePatch implementations; retained fire helpers below.
	}

	@Override
	public void registerPlayerHooks() {
		if (!playerHooksRegistered) {
			playerHooksRegistered = true;
		}
	}

	public static boolean communicatorMessageHook(Communicator communicator, String message, String title) {
		return getInstance().fireOnMessage(communicator, message, title);
	}

	public static boolean communicatorChannelHook(Message message) {
		return getInstance().fireOnKingdomMessage(message);
	}

	public static boolean communicatorChannelHook(Village village, Message message) {
		if ("Alliance".equals(message.getWindow())) {
			return false;
		}
		return getInstance().fireOnVillageMessage(village, message);
	}

	public static boolean communicatorChannelHook(PvPAlliance alliance, Message message) {
		return getInstance().fireOnAllianceMessage(alliance, message);
	}

	// ========================================================================
	// Static Fire Methods (called from bytecode hooks)
	// ========================================================================

	/**
	 * Capture attackers map from a creature using EventLogic (CLEAN - called from patch).
	 *
	 * <p>This method delegates to {@link com.garward.wurmmodloader.core.eventlogic.CreatureDeathEventLogic}
	 * to handle the reflection-based attacker map extraction.</p>
	 *
	 * @param victim The creature about to die
	 */
	public static void captureAttackersViaLogic(com.wurmonline.server.creatures.Creature victim) {
		// Use EventLogic to capture attackers (NO REFLECTION IN HOOK!)
		java.util.Map<Long, Long> attackersMap =
			com.garward.wurmmodloader.core.eventlogic.CreatureDeathEventLogic.captureAttackersMap(victim);

		// Store for LootManager if capture succeeded
		if (attackersMap != null) {
			CAPTURED_ATTACKERS.put(victim.getWurmId(), attackersMap);
		}
	}

	/**
	 * Capture attackers map before die() clears it (legacy - for direct calls).
	 */
	public static void captureAttackers(long creatureId, java.util.Map<Long, Long> attackers) {
		CAPTURED_ATTACKERS.put(creatureId, attackers);
	}

	/**
	 * Retrieve and remove captured attackers (called by LootManager).
	 */
	public static java.util.Map<Long, Long> getAndRemoveCapturedAttackers(long creatureId) {
		return CAPTURED_ATTACKERS.remove(creatureId);
	}

	/**
	 * Track damage dealt by an attacker to a victim (called from CombatDamageEvent).
	 *
	 * @param victimId The creature being damaged
	 * @param attackerId The creature dealing damage
	 * @param damage The amount of damage dealt
	 */
	public static void trackDamage(long victimId, long attackerId, double damage) {
		DAMAGE_TRACKING.computeIfAbsent(victimId, k -> new java.util.concurrent.ConcurrentHashMap<>())
			.merge(attackerId, damage, Double::sum);
	}

	/**
	 * Retrieve and remove damage tracking for a creature (called by LootManager).
	 * Returns map of attackerId -> total damage dealt.
	 *
	 * @param victimId The creature that died
	 * @return Map of attacker IDs to total damage dealt, or empty map if none tracked
	 */
	public static java.util.Map<Long, Double> getAndRemoveDamageTracking(long victimId) {
		java.util.concurrent.ConcurrentHashMap<Long, Double> damage = DAMAGE_TRACKING.remove(victimId);
		return damage != null ? new java.util.HashMap<>(damage) : new java.util.HashMap<>();
	}

	/**
	 * Fire CreatureDeathEvent with killer determination via EventLogic.
	 * This is the CLEAN method that bytecode patches should call.
	 *
	 * <p>Uses {@link com.garward.wurmmodloader.core.eventlogic.CreatureDeathEventLogic}
	 * to determine the killer from damage tracking and attacker lists, then fires
	 * the appropriate event (CreatureDeathEvent or PlayerDeathEvent).</p>
	 *
	 * @param victim The creature that died
	 * @param damageMap Map of attackerId -> total damage dealt (may be null)
	 */
	public static void fireCreatureDeathEventWithLogic(
			com.wurmonline.server.creatures.Creature victim,
			java.util.Map<Long, Double> damageMap) {

		// Use EventLogic to determine killer (NO LOGIC IN HOOK!)
		com.wurmonline.server.creatures.Creature killer =
			com.garward.wurmmodloader.core.eventlogic.CreatureDeathEventLogic.determineKiller(victim, damageMap);

		// Route to appropriate event based on victim type
		if (victim.isPlayer()) {
			firePlayerDeathEvent((com.wurmonline.server.players.Player) victim, killer);
		} else {
			fireCreatureDeathEvent(victim, killer);
		}

		// If the victim was someone's pet, also fire PetReleasedEvent(DIED).
		try {
			if (victim.isDominated() && victim.getDominator() != null) {
				long ownerId = victim.getDominator().getWurmId();
				getInstance().firePetReleased(victim, ownerId,
					com.garward.wurmmodloader.api.events.creature.PetReleasedEvent.Reason.DIED);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	// ========================================================================
	// Taming events (CharmAnimal, Dominate)
	// ========================================================================

	/** Fire TameAttemptEvent from CharmAnimal.precondition. Returns true if cancelled. */
	public static boolean fireTameAttemptEventCharm(com.wurmonline.server.creatures.Creature performer,
	                                                 com.wurmonline.server.creatures.Creature target) {
		return getInstance().fireTameAttempt(performer, target,
			com.garward.wurmmodloader.api.events.creature.TameAttemptEvent.Source.CHARM);
	}

	/** Fire TameAttemptEvent from Dominate.mayDominate. Returns true if cancelled. */
	public static boolean fireTameAttemptEventDominate(com.wurmonline.server.creatures.Creature performer,
	                                                    com.wurmonline.server.creatures.Creature target) {
		return getInstance().fireTameAttempt(performer, target,
			com.garward.wurmmodloader.api.events.creature.TameAttemptEvent.Source.DOMINATE);
	}

	/** Fire TameCompleteEvent after CharmAnimal.doEffect. */
	public static void fireTameCompleteEventCharm(com.wurmonline.server.creatures.Creature performer,
	                                              com.wurmonline.server.creatures.Creature target,
	                                              double power) {
		getInstance().fireTameComplete(performer, target,
			com.garward.wurmmodloader.api.events.creature.TameAttemptEvent.Source.CHARM, power);
	}

	/** Fire TameCompleteEvent after Dominate.dominate. */
	public static void fireTameCompleteEventDominate(com.wurmonline.server.creatures.Creature performer,
	                                                 com.wurmonline.server.creatures.Creature target,
	                                                 double power) {
		getInstance().fireTameComplete(performer, target,
			com.garward.wurmmodloader.api.events.creature.TameAttemptEvent.Source.DOMINATE, power);
	}

	/** Fire PetReleasedEvent (any reason). */
	public static void firePetReleasedEvent(com.wurmonline.server.creatures.Creature pet,
	                                        long formerOwnerId,
	                                        com.garward.wurmmodloader.api.events.creature.PetReleasedEvent.Reason reason) {
		getInstance().firePetReleased(pet, formerOwnerId, reason);
	}

	/**
	 * Fire CreatureDeathEvent (called from bytecode hook or internal routing).
	 */
	public static void fireCreatureDeathEvent(com.wurmonline.server.creatures.Creature victim,
			com.wurmonline.server.creatures.Creature killer) {
		getInstance().fireCreatureDeath(victim, killer);
	}

	/**
	 * Fire CreatureExamineEvent (called from bytecode hook).
	 * Returns modified examine text.
	 */
	public static String fireCreatureExamineEvent(com.wurmonline.server.creatures.Creature creature,
	                                              String examineText) {
		return getInstance().fireCreatureExamine(creature, examineText);
	}

	/**
	 * Fire CombatDamageEvent (called from bytecode hook).
	 * Returns modified damage value.
	 */
	public static double fireCombatDamageEvent(com.wurmonline.server.creatures.Creature attacker,
			com.wurmonline.server.creatures.Creature defender,
			double damage,
			byte woundType,
			int bodyPart) {
		return getInstance().fireCombatDamage(attacker, defender, damage, woundType, bodyPart);
	}

	/**
	 * Fire CreatureSpawnEvent (called from bytecode hook).
	 */
	public static void fireCreatureSpawnEvent(com.wurmonline.server.creatures.Creature creature) {
		getInstance().fireCreatureSpawn(creature);
	}

	/**
	 * Fire CreaturePositionUpdatedEvent (called from bytecode hook).
	 */
	public static void fireCreaturePositionUpdatedEvent(com.wurmonline.server.creatures.Creature creature,
	                                                     float x, float y, float z, float rot, long bridgeId) {
		getInstance().fireCreaturePositionUpdated(creature, x, y, z, rot, bridgeId);
	}

	/**
	 * Fire CreatureBreedEvent (called from bytecode hook).
	 * Returns true if the event was cancelled and breeding should be prevented.
	 */
	public static boolean fireCreatureBreedEvent(com.wurmonline.server.creatures.Creature performer,
	                                              com.wurmonline.server.creatures.Creature target,
	                                              short breedType,
	                                              com.wurmonline.server.behaviours.Action action,
	                                              float counter) {
		return getInstance().fireCreatureBreed(performer, target, breedType, action, counter);
	}

	/**
	 * Fire CreatureDbSaveEvent (called from bytecode hook in DbCreatureStatus.save()).
	 */
	public static void fireCreatureDbSaveEvent(com.wurmonline.server.creatures.Creature creature) {
		getInstance().fireCreatureDbSave(creature);
	}

	/**
	 * Fire CreatureDbLoadEvent (called from bytecode hook in DbCreatureStatus constructor).
	 */
	public static void fireCreatureDbLoadEvent(com.wurmonline.server.creatures.Creature creature,
	                                            java.sql.ResultSet resultSet) {
		getInstance().fireCreatureDbLoad(creature, resultSet);
	}

	/**
	 * Fire DeityDbSaveEvent (called from bytecode hook in DbDeity.save()).
	 */
	public static void fireDeityDbSaveEvent(int deityId, String deityName) {
		getInstance().fireDeityDbSave(deityId, deityName);
	}

	/**
	 * Fire DeityDbLoadEvent (called from bytecode hook in DbDeity constructor).
	 */
	public static void fireDeityDbLoadEvent(int deityId, String deityName,
	                                         java.sql.ResultSet resultSet) {
		getInstance().fireDeityDbLoad(deityId, deityName, resultSet);
	}

	/**
	 * Fire StructureDbSaveEvent (called from bytecode hook in DbStructure.save()).
	 */
	public static void fireStructureDbSaveEvent(long structureId, String structureName) {
		getInstance().fireStructureDbSave(structureId, structureName);
	}

	/**
	 * Fire StructureDbLoadEvent (called from bytecode hook in DbStructure.load()).
	 */
	public static void fireStructureDbLoadEvent(long structureId, String structureName,
	                                            java.sql.ResultSet resultSet) {
		getInstance().fireStructureDbLoad(structureId, structureName, resultSet);
	}

	/**
	 * Fire ActionFatigueEvent (called from bytecode hook in Action constructor).
	 * Returns the fatigue value after mods have had a chance to modify it.
	 */
	public static boolean fireActionFatigueEvent(com.wurmonline.server.creatures.Creature performer,
	                                              long subject,
	                                              long target,
	                                              short action,
	                                              boolean defaultFatigue) {
		return getInstance().fireActionFatigue(performer, subject, target, action, defaultFatigue);
	}

	/**
	 * Fire CropHarvestEvent (called from bytecode hook in Terraforming.harvest).
	 * Returns the harvest quantity after mods have had a chance to modify it.
	 */
	public static int fireCropHarvestEvent(com.wurmonline.server.creatures.Creature performer,
	                                        int tilex,
	                                        int tiley,
	                                        boolean onSurface,
	                                        int tile,
	                                        float counter,
	                                        com.wurmonline.server.items.Item tool,
	                                        int quantity) {
		return getInstance().fireCropHarvest(performer, tilex, tiley, onSurface, tile, counter, tool, quantity);
	}

	/**
	 * Fire CropGrowthEvent (called from bytecode hook in CropTilePoller/TilePoller).
	 * Returns true if the event was cancelled and growth check should be skipped.
	 */
	public static boolean fireCropGrowthEvent(int tilex, int tiley, int tile, byte data, byte farmData) {
		return getInstance().fireCropGrowth(tilex, tiley, tile, data, farmData);
	}
	/**
     * Fire MovementBroadcastEvent (called from bytecode hook).
     */
    public static void fireMovementBroadcastEvent(
            Communicator communicator,
            long creatureId,
            float x,
            float y,
            int rotation,
            boolean moving
    ) {
        getInstance().fireMovementBroadcast(communicator, creatureId, x, y, rotation, moving);
    }


    /**
     * Fire PlayerMovementBroadcastEvent (called from bytecode hook).
     */
    public static void firePlayerMovementBroadcastEvent(
            Communicator communicator,
            float x,
            float y,
            float z,
            float rotation,
            boolean moving
    ) {
        getInstance().firePlayerMovementBroadcast(communicator, x, y, z, rotation, moving);
    }


	/**
	 * Fire PriestRestrictionCheckEvent (called from bytecode hook in crafting methods).
	 * Returns the modified priest status after mods have had a chance to override it.
	 */
	public static boolean firePriestRestrictionCheckEvent(com.wurmonline.server.creatures.Creature creature,
	                                                       String context,
	                                                       boolean defaultIsPriest) {
		return getInstance().firePriestRestrictionCheck(creature, context, defaultIsPriest);
	}

	/**
	 * Fire PrayerFaithEvent (called from bytecode hook in prayer/faith methods).
	 * Returns array [numFaith, lastFaith] after mods have had a chance to modify them.
	 */
	public static Object[] firePrayerFaithEvent(long playerId, byte numFaith, long lastFaith) {
		return getInstance().firePrayerFaith(playerId, numFaith, lastFaith);
	}

	/**
	 * Fire CombatAttackEvent (called from bytecode hook).
	 * Returns result indicating if event was cancelled and the attack result.
	 */
	public static CombatAttackResult fireCombatAttackEvent(com.wurmonline.server.creatures.Creature attacker,
	                                                        com.wurmonline.server.creatures.Creature defender,
	                                                        int combatCounter,
	                                                        boolean opportunity,
	                                                        float actionCounter,
	                                                        com.wurmonline.server.behaviours.Action action) {
		return getInstance().fireCombatAttack(attacker, defender, combatCounter, opportunity, actionCounter, action);
	}

	/**
	 * Fire SpecialMoveSendEvent (called from bytecode hook).
	 * Returns true if event was cancelled (suppresses vanilla UI).
	 */
	public static boolean fireSpecialMoveSendEvent(com.wurmonline.server.creatures.Creature creature) {
		return getInstance().fireSpecialMoveSend(creature);
	}

	/**
	 * Fire SpecialMoveHandleEvent (called from bytecode hook).
	 * Returns result indicating if event was cancelled and the handler result.
	 */
	public static SpecialMoveResult fireSpecialMoveHandleEvent(com.wurmonline.server.creatures.Creature performer,
	                                                             com.wurmonline.server.creatures.Creature target,
	                                                             short action,
	                                                             float counter) {
		return getInstance().fireSpecialMoveHandle(performer, target, action, counter);
	}

	/**
	 * Fire PlayerSkillLossEvent (called from bytecode hook).
	 * Returns true if event was cancelled (prevents skill loss).
	 */
	public static boolean firePlayerSkillLossEvent(com.wurmonline.server.creatures.Creature creature) {
		return getInstance().firePlayerSkillLoss(creature);
	}

	/**
	 * Fire ShieldCheckEvent (called from bytecode hook).
	 */
	public static com.garward.wurmmodloader.api.events.combat.shield.ShieldCheckEvent fireShieldCheckEvent(
			com.wurmonline.server.creatures.Creature attacker,
			com.wurmonline.server.creatures.Creature defender,
			com.wurmonline.server.items.Item weapon,
			com.wurmonline.server.items.Item shield) {
		return getInstance().fireShieldCheck(attacker, defender, weapon, shield);
	}

	/**
	 * Fire ShieldDamageEvent (called from bytecode hook).
	 */
	public static double fireShieldDamageEvent(com.wurmonline.server.creatures.Creature defender,
			com.wurmonline.server.creatures.Creature attacker,
			com.wurmonline.server.items.Item shield,
			double damageDelta) {
		return getInstance().fireShieldDamage(defender, attacker, shield, damageDelta);
	}

	public static float fireMaterialDamageModifierEvent(com.wurmonline.server.items.Item item, byte material,
			float base) {
		return getInstance().fireMaterialDamageModifier(item, material, base);
	}

	public static float fireMaterialDecayModifierEvent(com.wurmonline.server.items.Item item, byte material,
			float base) {
		return getInstance().fireMaterialDecayModifier(item, material, base);
	}

	public static float fireMaterialImpBonusEvent(com.wurmonline.server.items.Item item, byte material, float base) {
		return getInstance().fireMaterialImpBonus(item, material, base);
	}

	public static float fireMaterialRepairTimeEvent(com.wurmonline.server.items.Item item, byte material, float base) {
		return getInstance().fireMaterialRepairTime(item, material, base);
	}

	public static double fireMaterialBonusEvent(
			com.garward.wurmmodloader.api.events.item.material.MaterialBonusEvent.BonusType type,
			Object context,
			byte material,
			double base) {
		return getInstance().fireMaterialBonus(type, context, material, base);
	}

	public static double fireWeaponStatEvent(com.wurmonline.server.combat.Weapon weapon,
			com.wurmonline.server.items.Item item,
			byte material,
			com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent.StatType type,
			double baseValue) {
		return getInstance().fireWeaponStat(weapon, item, material, type, baseValue);
	}

	/**
	 * Fire WeaponStatQueryEvent for speed with null-safe material extraction.
	 * This is the CLEAN method for patches - handles null checking logic.
	 *
	 * @param weapon The weapon (may be null)
	 * @param item The item (may be null)
	 * @param type The stat type (always SPEED for this method)
	 * @param baseValue The base value to modify
	 * @return Modified value from event handlers
	 */
	public static double fireWeaponStatEventForSpeed(com.wurmonline.server.combat.Weapon weapon,
			com.wurmonline.server.items.Item item,
			com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent.StatType type,
			double baseValue) {
		// ✅ NULL CHECKING LOGIC BELONGS HERE, NOT IN PATCH
		byte material = (item != null) ? item.getMaterial() : (byte)0;
		return getInstance().fireWeaponStat(weapon, item, material, type, baseValue);
	}

	public static OpportunityAttackEvent fireOpportunityAttackEvent(com.wurmonline.server.creatures.Creature defender,
			com.wurmonline.server.creatures.Creature trespasser,
			double skillResult,
			double difficulty,
			byte opportunityCounter,
			int usedOpportunityAttacks,
			int combatCounter,
			float actionCounter) {
		return getInstance().fireOpportunityAttack(defender, trespasser, skillResult, difficulty,
				opportunityCounter, usedOpportunityAttacks, combatCounter, actionCounter);
	}

	public static void recordOpportunitySkill(double skillResult, double difficulty) {
		OpportunityContext ctx = OPPORTUNITY_CONTEXT.get();
		ctx.skillResult = skillResult;
		ctx.difficulty = difficulty;
	}

	public static OpportunityContext getOpportunityContext() {
		return OPPORTUNITY_CONTEXT.get();
	}

	public static float fireCombatCriticalHitChanceEvent(com.wurmonline.server.creatures.Creature attacker,
			com.wurmonline.server.creatures.Creature defender,
			com.wurmonline.server.items.Item weapon,
			com.wurmonline.server.creatures.AttackAction attackAction,
			float baseChance,
			boolean usingNewCombatSystem) {
		return getInstance().fireCombatCriticalHitChance(attacker, defender, weapon, attackAction, baseChance,
				usingNewCombatSystem);
	}

	public static final class OpportunityContext {
		private double skillResult;
		private double difficulty;

		public double getSkillResult() {
			return skillResult;
		}

		public double getDifficulty() {
			return difficulty;
		}
	}

	public static float fireActionTimeEvent(com.wurmonline.server.creatures.Creature performer,
			com.wurmonline.server.items.Item source,
			com.wurmonline.server.items.Item target,
			float baseTime) {
		return getInstance().fireActionTime(performer, source, target, baseTime);
	}

	public static com.garward.wurmmodloader.api.events.skill.SkillAdvanceEvent fireSkillAdvanceEvent(
			com.wurmonline.server.skills.Skill skill,
			com.wurmonline.server.items.Item item,
			double difficulty,
			double bonus) {
		return getInstance().fireSkillAdvance(skill, item, difficulty, bonus);
	}

	public static float fireActionSpeedEvent(com.wurmonline.server.creatures.Creature performer,
			int staminaNeeded,
			float baseModifier) {
		return getInstance().fireActionSpeed(performer, staminaNeeded, baseModifier);
	}

	public static float fireCombatSwingSpeedEvent(com.wurmonline.server.creatures.Creature attacker,
			com.wurmonline.server.items.Item weapon,
			float baseSpeed) {
		return getInstance().fireCombatSwingSpeed(attacker, weapon, baseSpeed);
	}

	public static com.garward.wurmmodloader.api.events.combat.CombatDualWieldEvent fireCombatDualWieldEvent(
			com.wurmonline.server.creatures.Creature attacker,
			com.wurmonline.server.creatures.Creature defender,
			com.wurmonline.server.items.Item offhand,
			float delta) {
		return getInstance().fireCombatDualWield(attacker, defender, offhand, delta);
	}

	public static com.garward.wurmmodloader.api.events.combat.WeaponUseEvent fireWeaponUseEvent(
			com.wurmonline.server.creatures.Creature creature,
			com.wurmonline.server.items.Item weapon,
			float previousValue,
			float newValue) {
		return getInstance().fireWeaponUse(creature, weapon, previousValue, newValue);
	}

	/**
	 * Fire PlayerDeathEvent (called from bytecode hook).
	 */
	public static void firePlayerDeathEvent(com.wurmonline.server.players.Player player,
			com.wurmonline.server.creatures.Creature killer) {
		getInstance().firePlayerDeath(player, killer);
	}

	/**
	 * Fire ItemExamineEvent (called from bytecode hook).
	 * Returns the potentially modified examine text.
	 */
	public static String fireItemExamineEvent(com.wurmonline.server.items.Item item,
			com.wurmonline.server.creatures.Creature examiner,
			com.wurmonline.server.creatures.Creature owner,
			String originalText) {
		return getInstance().fireItemExamine(item, examiner, owner, originalText);
	}

	/**
	 * Fire ItemEnchantmentStringsEvent (called from bytecode hook).
	 * Allows mods to send additional colored messages about item stats.
	 */
	public static void fireItemEnchantmentStringsEvent(com.wurmonline.server.items.Item item,
			com.wurmonline.server.creatures.Creature examiner) {
		getInstance().fireItemEnchantmentStrings(item, examiner);
	}

	/**
	 * Fire ItemDropEvent (called from bytecode hook).
	 * Returns true if the drop should be cancelled.
	 *
	 * @param itemId   The item being dropped
	 * @param ownerId  The creature dropping the item
	 * @param onGround Whether the item is being dropped on ground (true) or to
	 *                 container/corpse (false)
	 */
	public static boolean fireItemDropEvent(long itemId, long ownerId, boolean onGround) {
		try {
			// Look up the item being dropped
			com.wurmonline.server.items.Item item = com.wurmonline.server.Items.getItem(itemId);

			// Look up the creature dropping the item
			com.wurmonline.server.creatures.Creature dropper = com.wurmonline.server.creatures.Creatures.getInstance()
					.getCreatureOrNull(ownerId);

			// Fire the event
			return getInstance().fireItemDrop(item, dropper, onGround);

		} catch (com.wurmonline.server.NoSuchItemException e) {
			// Item not found - allow drop
			return false;
		} catch (Exception e) {
			// Log error but allow drop to proceed
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
					.log(java.util.logging.Level.WARNING, "Error firing ItemDropEvent", e);
			return false;
		}
	}

	/**
	 * Fire ItemTradeEvent (called from bytecode hook).
	 * Returns true if the trade should be cancelled.
	 */
	public static boolean fireItemTradeEvent(com.wurmonline.server.items.Item item,
			com.wurmonline.server.items.TradingWindow window) {
		return getInstance().fireItemTrade(item, window);
	}

	// ========================================================================
	// Vehicle/Mount Hook Static Fire Methods
	// ========================================================================

	/**
	 * Fire VehicleMountEvent for creature mounting (called from bytecode hook).
	 */
	public static boolean fireVehicleMountEventCreature(
			com.wurmonline.server.creatures.Creature rider,
			com.wurmonline.server.creatures.Creature mount,
			com.wurmonline.server.behaviours.Vehicle vehicle,
			int seatNum,
			boolean asDriver) {
		return getInstance().fireVehicleMount(rider, mount, vehicle, seatNum, asDriver);
	}

	/**
	 * Fire VehicleMountEvent for item vehicle mounting (called from bytecode hook).
	 */
	public static boolean fireVehicleMountEventItem(
			com.wurmonline.server.creatures.Creature rider,
			com.wurmonline.server.items.Item mount,
			com.wurmonline.server.behaviours.Vehicle vehicle,
			int seatNum,
			boolean asDriver) {
		return getInstance().fireVehicleMount(rider, mount, vehicle, seatNum, asDriver);
	}

	/**
	 * Fire VehicleSpeedCalculationEvent for creature mounts (called from bytecode
	 * hook).
	 * Returns modified speed based on event handlers.
	 */
	public static float fireVehicleSpeedCalculationCreature(
			com.wurmonline.server.behaviours.Vehicle vehicle,
			com.wurmonline.server.creatures.Creature mount,
			long pilotId,
			boolean mounting,
			float vanillaSpeed) {
		try {
			com.wurmonline.server.creatures.Creature rider = null;
			if (pilotId != -10L) {
				rider = com.wurmonline.server.Server.getInstance().getCreature(pilotId);
			}

			// Fire event with vanilla calculated speed as base
			float finalSpeed = getInstance().fireVehicleSpeedCalculation(
					vehicle, mount, rider, vanillaSpeed, mounting);

			return finalSpeed;

		} catch (Exception e) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
					.log(java.util.logging.Level.WARNING, "Error in vehicle speed calculation", e);
			return vanillaSpeed; // Use vanilla calculation on error
		}
	}

	/**
	 * Fire MountEquipmentCheckEvent (called from bytecode hook).
	 * Returns true if equipment is incompatible (cancel mounting).
	 */
	public static boolean fireMountEquipmentCheckEvent(
			com.wurmonline.server.creatures.Creature mount,
			com.wurmonline.server.items.Item equipment) {
		return getInstance().fireMountEquipmentCheck(mount, equipment);
	}

	/**
	 * Fire BodyMenuPopulateEvent (called from bytecode hook).
	 * Allows mods to add custom menu entries to the body context menu.
	 */
	public static void fireBodyMenuPopulateEvent(com.wurmonline.server.creatures.Creature performer,
	                                              com.wurmonline.server.items.Item bodyPart,
	                                              java.util.List<com.wurmonline.server.behaviours.ActionEntry> menuEntries) {
		getInstance().fireBodyMenuPopulate(performer, bodyPart, menuEntries);
	}

	// ========== SPELL SYSTEM EVENTS ==========

	/**
	 * Fire ItemDamageEvent (called from bytecode hook).
	 * Returns modified damage amount.
	 */
	public static float fireItemDamageEvent(long itemId, String itemName, float damage, float currentDamage) {
		return getInstance().fireItemDamage(itemId, itemName, damage, currentDamage);
	}

	/**
	 * Fire ContainerVolumeEvent (called from bytecode hook).
	 * Returns modified container volume/size.
	 */
	public static int fireContainerVolumeEvent(long itemId, String itemName, int value, int volumeType) {
		return getInstance().fireContainerVolume(itemId, itemName, value, volumeType);
	}

	/**
	 * Fire SkillDifficultyEvent (called from bytecode hook).
	 * Returns modified difficulty.
	 */
	public static double fireSkillDifficultyEvent(long performerId, String performerName,
	                                              int skillId, String skillName,
	                                              long toolId, String toolName, double difficulty) {
		return getInstance().fireSkillDifficulty(performerId, performerName, skillId, skillName,
		                                         toolId, toolName, difficulty);
	}

	/**
	 * Fire StaminaCostEvent (called from bytecode hook).
	 * Returns modified stamina cost.
	 */
	public static int fireStaminaCostEvent(long creatureId, String creatureName,
	                                       int cost, int currentStamina, String actionType) {
		return getInstance().fireStaminaCost(creatureId, creatureName, cost, currentStamina, actionType);
	}

	/**
	 * Fire SpellFavorCostEvent (called from bytecode hook).
	 * Returns modified favor cost.
	 */
	public static int fireSpellFavorCostEvent(long casterId, String casterName,
	                                          int spellId, String spellName,
	                                          int cost, float currentFavor) {
		return getInstance().fireSpellFavorCost(casterId, casterName, spellId, spellName, cost, currentFavor);
	}

	/**
	 * Fire SpellCastingTimeEvent (called from bytecode hook).
	 * Returns modified casting time in seconds.
	 */
	public static int fireSpellCastingTimeEvent(int spellId, String spellName,
	                                            long casterId, String casterName,
	                                            int originalTime) {
		return getInstance().fireSpellCastingTime(spellId, spellName, casterId, casterName, originalTime);
	}

	/**
	 * Fire SpellCooldownEvent (called from bytecode hook).
	 * Returns modified cooldown duration in ms; 0 or negative = skip cooldown.
	 */
	public static long fireSpellCooldownEvent(int spellId, String spellName,
	                                          long casterId, String casterName,
	                                          long originalCooldownMs) {
		return getInstance().fireSpellCooldown(spellId, spellName, casterId, casterName, originalCooldownMs);
	}

	/**
	 * Fire SpellPowerEvent (called from bytecode hook).
	 * Returns modified power (post-trimPower).
	 */
	public static double fireSpellPowerEvent(int spellId, String spellName,
	                                         long casterId, String casterName,
	                                         double originalPower) {
		return getInstance().fireSpellPower(spellId, spellName, casterId, casterName, originalPower);
	}

	/**
	 * Fire SpellCastAttemptEvent (cancellable, called from bytecode hook).
	 * Returns true if the cast was cancelled; the caller should short-circuit.
	 */
	public static boolean fireSpellCastAttemptEvent(int spellId, String spellName,
	                                                long casterId, String casterName) {
		return getInstance().fireSpellCastAttempt(spellId, spellName, casterId, casterName);
	}

	/**
	 * Fire SpellEffectEvent (cancellable, called from bytecode hook).
	 * Returns true if the effect should be skipped.
	 */
	public static boolean fireSpellEffectEvent(int spellId, String spellName,
	                                           long casterId, String casterName,
	                                           double power, boolean negative) {
		return getInstance().fireSpellEffect(spellId, spellName, casterId, casterName, power, negative);
	}

	/**
	 * Fire SpellDifficultyEvent (called from bytecode hook).
	 * Returns modified difficulty.
	 */
	public static int fireSpellDifficultyEvent(int spellId, String spellName,
	                                           int originalDifficulty, boolean forItem) {
		return getInstance().fireSpellDifficulty(spellId, spellName, originalDifficulty, forItem);
	}

	/**
	 * Fire DeitySpellRegistrationEvent (notification, called from bytecode hook).
	 */
	public static void fireDeitySpellRegistrationEvent(int deityNumber, String deityName,
	                                                   int spellId, String spellName, boolean added) {
		getInstance().fireDeitySpellRegistration(deityNumber, deityName, spellId, spellName, added);
	}

	/**
	 * Fire SpellPreconditionEvent (called from bytecode hook).
	 * Returns the (possibly modified) allow flag; mods may override vanilla's decision.
	 */
	public static boolean fireSpellPreconditionEvent(int spellId, String spellName,
	                                                 long casterId, String casterName,
	                                                 long targetId, String targetType,
	                                                 boolean originalAllowed) {
		return getInstance().fireSpellPrecondition(spellId, spellName, casterId, casterName,
				targetId, targetType, originalAllowed);
	}

	/**
	 * Fire SpellResistEvent (called from bytecode hook).
	 * Returns the (possibly modified) resist roll.
	 */
	public static double fireSpellResistEvent(int spellId, String spellName,
	                                          long casterId, long targetId,
	                                          int difficulty, double originalResist) {
		return getInstance().fireSpellResist(spellId, spellName, casterId, targetId,
				difficulty, originalResist);
	}

	/**
	 * Fire SacrificeAcceptanceEvent (called from bytecode hook on MethodsReligion.canBeSacrificed).
	 */
	public static boolean fireSacrificeAcceptanceEvent(long itemId, int templateId, boolean originalAccepted) {
		return getInstance().fireSacrificeAcceptance(itemId, templateId, originalAccepted);
	}

	/**
	 * Fire SacrificeFavorValueEvent (called from bytecode hook on MethodsReligion.getFavorValue).
	 */
	public static float fireSacrificeFavorValueEvent(int deityNumber, long itemId, int templateId, float originalValue) {
		return getInstance().fireSacrificeFavorValue(deityNumber, itemId, templateId, originalValue);
	}

	/**
	 * Fire SacrificeFavorModifierEvent (called from bytecode hook on MethodsReligion.getFavorModifier).
	 */
	public static float fireSacrificeFavorModifierEvent(int deityNumber, long itemId, int templateId, float originalModifier) {
		return getInstance().fireSacrificeFavorModifier(deityNumber, itemId, templateId, originalModifier);
	}

	/**
	 * Filter a vanilla spell list through SpellVisibilityEvent — called from bytecode
	 * hooks in the Behaviour classes. Fires one event per spell and returns a new
	 * array omitting cancelled entries. Null-safe: returns the input unchanged if null.
	 */
	public static com.wurmonline.server.spells.Spell[] filterSpellVisibility(
			com.wurmonline.server.spells.Spell[] spells,
			com.wurmonline.server.creatures.Creature performer,
			long targetId, String targetType) {
		if (spells == null || spells.length == 0) return spells;
		long casterId = performer == null ? -1L : performer.getWurmId();
		java.util.ArrayList<com.wurmonline.server.spells.Spell> kept =
				new java.util.ArrayList<com.wurmonline.server.spells.Spell>(spells.length);
		for (int i = 0; i < spells.length; i++) {
			com.wurmonline.server.spells.Spell s = spells[i];
			if (s == null) continue;
			boolean cancelled = getInstance().fireSpellVisibility(
					s.number, s.name, casterId, targetId, targetType);
			if (!cancelled) kept.add(s);
		}
		if (kept.size() == spells.length) return spells;
		return kept.toArray(new com.wurmonline.server.spells.Spell[0]);
	}

	/**
	 * Fire CombatRatingEvent (called from bytecode hook).
	 * Returns modified combat rating.
	 */
	public static float fireCombatRatingEvent(long creatureId, String creatureName, float rating) {
		return getInstance().fireCombatRating(creatureId, creatureName, rating);
	}

	// ========== WML_SYNC MODCOMM CHANNEL EVENTS ==========

	/**
	 * Fire MovementIntentReceivedEvent (called from WMLSyncChannel).
	 */
	public static void fireMovementIntentReceivedEvent(com.wurmonline.server.players.Player player,
	                                                   long seqId, byte inputState) {
		getInstance().fireMovementIntentReceived(player, seqId, inputState);
	}

	/**
	 * Fire PredictionStateReceivedEvent (called from WMLSyncChannel).
	 */
	public static void firePredictionStateReceivedEvent(com.wurmonline.server.players.Player player,
	                                                    long seqId, float x, float y, float height) {
		getInstance().firePredictionStateReceived(player, seqId, x, y, height);
	}

	// ========================================================================
	// Database backend SPI static fire methods (called from bytecode hooks)
	// ========================================================================

	private static final java.util.concurrent.atomic.AtomicBoolean DB_BACKEND_SELECTION_FIRED =
		new java.util.concurrent.atomic.AtomicBoolean(false);

	/**
	 * Fire DatabaseBackendSelectionEvent (called from bytecode hook in DbConnector.initialize()).
	 *
	 * <p>Vanilla calls {@code DbConnector.initialize()} from multiple entry points
	 * (WurmServerGuiController, Server.startRunning(), refreshConnectionForSchema),
	 * and the patch's {@code insertBefore} runs ahead of vanilla's {@code isInitialized}
	 * short-circuit. A single-shot latch here preserves the SPI's "once per process"
	 * contract — mods get exactly one registration window.</p>
	 */
	public static void fireDatabaseBackendSelectionEvent() {
		if (!DB_BACKEND_SELECTION_FIRED.compareAndSet(false, true)) {
			return;
		}
		getInstance().fireDatabaseBackendSelection();
	}

	/**
	 * Fire DatabaseBackendBootstrapEvent (called from DatabaseBackendEventLogic after a backend
	 * wins registration, before per-schema factories are instantiated).
	 *
	 * @param backend the registered {@code DatabaseBackend}; must not be null
	 */
	public static void fireDatabaseBackendBootstrapEvent(Object backend) {
		try {
			getInstance().fireDatabaseBackendBootstrap(
				(com.garward.wurmmodloader.api.database.DatabaseBackend) backend);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DatabaseBackendBootstrapEvent", t);
		}
	}

	/**
	 * Fire DatabaseConnectionOpenedEvent (called from bytecode hooks in SqliteConnectionFactory /
	 * MysqlConnectionFactory createConnection()).
	 *
	 * <p>Parameters are typed {@link Object} so patches can pass values without caring about
	 * classloader visibility; this method casts to the concrete WU types.</p>
	 */
	public static void fireDatabaseConnectionOpenedEvent(Object schema, Object connection) {
		try {
			getInstance().fireDatabaseConnectionOpened(
				(com.wurmonline.server.database.WurmDatabaseSchema) schema,
				(java.sql.Connection) connection);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DatabaseConnectionOpenedEvent", t);
		}
	}

	/**
	 * Fire DatabaseMigrationStartingEvent (called from bytecode hook before migrate()).
	 */
	public static void fireDatabaseMigrationStartingEvent(Object schema) {
		try {
			getInstance().fireDatabaseMigrationStarting(
				(com.wurmonline.server.database.WurmDatabaseSchema) schema);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DatabaseMigrationStartingEvent", t);
		}
	}

	/**
	 * Fire DatabaseMigrationCompletedEvent (called from bytecode hook after migrate() succeeds).
	 */
	public static void fireDatabaseMigrationCompletedEvent(Object schema) {
		try {
			getInstance().fireDatabaseMigrationCompleted(
				(com.wurmonline.server.database.WurmDatabaseSchema) schema);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DatabaseMigrationCompletedEvent", t);
		}
	}

	// ========================================================================
	// Trade / Village / ItemMove Hook Static Fire Methods
	// ========================================================================

	/**
	 * Fires TradeInitiateEvent followed by NpcTradePermissionCheckEvent from
	 * {@code Creature.startTrading()}. Returns true if either event cancels.
	 */
	public static boolean fireTradeSessionStartEvent(Object npc) {
		try {
			return getInstance().fireTradeSessionStart(
				(com.wurmonline.server.creatures.Creature) npc);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TradeSessionStart events", t);
			return false;
		}
	}

	/**
	 * Fires TradeBalanceEvent from {@code TradeHandler.balance()}.
	 * Returns true if the vanilla balance pass should be skipped.
	 */
	public static boolean fireTradeBalanceEvent(Object tradeHandler) {
		try {
			return getInstance().fireTradeBalance(
				(com.wurmonline.server.creatures.TradeHandler) tradeHandler);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TradeBalanceEvent", t);
			return false;
		}
	}

	/**
	 * Fires ItemMoveCheckEvent from {@code Item.moveToItem()}.
	 * Returns true if the move should be rejected.
	 */
	public static boolean fireItemMoveCheckEvent(Object item, Object mover, long targetId, boolean lastMove) {
		try {
			return getInstance().fireItemMoveCheck(
				(com.wurmonline.server.items.Item) item,
				(com.wurmonline.server.creatures.Creature) mover,
				targetId, lastMove);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire ItemMoveCheckEvent", t);
			return false;
		}
	}

	/**
	 * Fires VillageExpansionCheckEvent from
	 * {@code VillageFoundationQuestion.parseVillageFoundationQuestion5()}.
	 * Returns true if the foundation/expansion should be aborted.
	 */
	public static boolean fireVillageExpansionCheckEvent(Object question) {
		try {
			return getInstance().fireVillageExpansionCheck(
				(com.wurmonline.server.questions.VillageFoundationQuestion) question);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire VillageExpansionCheckEvent", t);
			return false;
		}
	}

	/**
	 * Fires TerrainModificationEvent from {@code Terraforming.dig(...)}.
	 * Returns true to abort the dig.
	 */
	public static boolean fireTerrainModificationEvent(Object performer, Object tool,
	                                                   int tileX, int tileY, int tile,
	                                                   float counter, boolean corner) {
		try {
			return getInstance().fireTerrainModification(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				tileX, tileY, tile, counter, corner);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TerrainModificationEvent", t);
			return false;
		}
	}

	/**
	 * Fires GuardPlanPollEvent from {@code GuardPlan.pollUpkeep()}.
	 * Returns true to skip the upkeep drain entirely.
	 */
	public static boolean fireGuardPlanPollEvent(Object plan) {
		try {
			return getInstance().fireGuardPlanPoll(
				(com.wurmonline.server.villages.GuardPlan) plan);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire GuardPlanPollEvent", t);
			return false;
		}
	}

	/**
	 * Fires StructurePlanningCheckEvent from {@code MethodsStructure.canPlanStructureAt(...)}.
	 * Returns true if the placement should be denied.
	 */
	public static boolean fireStructurePlanningCheckEvent(Object performer, Object tool,
	                                                     int tileX, int tileY, int tile) {
		try {
			return getInstance().fireStructurePlanningCheck(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				tileX, tileY, tile);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire StructurePlanningCheckEvent", t);
			return false;
		}
	}

	/**
	 * Fires {@code StructureGateCheckEvent} from mid-method gate patches in
	 * {@code MethodsStructure} and {@code FloorBehaviour}. Listeners that flip
	 * {@code bypass=true} let the performer past the vanilla check.
	 * Returns true when at least one listener bypassed the gate.
	 */
	public static boolean fireStructureGateCheckEvent(Object performer,
	                                                  String subjectName,
	                                                  String phaseName,
	                                                  int heightOffset) {
		try {
			com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent.Subject subject =
				com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent.Subject.valueOf(subjectName);
			com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent.Phase phase =
				com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent.Phase.valueOf(phaseName);
			return getInstance().fireStructureGateCheck(
				(com.wurmonline.server.creatures.Creature) performer,
				subject, phase, heightOffset);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire StructureGateCheckEvent", t);
			return false;
		}
	}

	/**
	 * Dispatch shim used by {@code QuestionAnswerPatch} — replaces
	 * {@code question.answer(answers)} call sites inside {@code Communicator}.
	 * Fires {@link com.garward.wurmmodloader.api.events.player.QuestionAnswerEvent};
	 * if not cancelled, invokes the original {@code Question.answer(Properties)}.
	 */
	public static void dispatchQuestionAnswer(Object question, Object answers) {
		try {
			com.wurmonline.server.questions.Question q =
				(com.wurmonline.server.questions.Question) question;
			java.util.Properties p = (java.util.Properties) answers;
			boolean cancelled = getInstance().fireQuestionAnswer(q, p);
			if (!cancelled) {
				q.answer(p);
			}
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to dispatch QuestionAnswerEvent", t);
			// Fall back to invoking answer so vanilla flow is preserved.
			try {
				((com.wurmonline.server.questions.Question) question)
					.answer((java.util.Properties) answers);
			} catch (Throwable t2) {
				java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
					.log(java.util.logging.Level.WARNING, "Fallback Question.answer also failed", t2);
			}
		}
	}

	/**
	 * Fires SacrificePostEvent after {@code MethodsReligion.sacrifice} resolves.
	 * Observer-only; return value is ignored.
	 */
	public static void fireSacrificePostEvent(Object action, Object performer, Object altar, boolean done) {
		try {
			getInstance().fireSacrificePost(
				(com.wurmonline.server.behaviours.Action) action,
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) altar,
				done);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire SacrificePostEvent", t);
		}
	}

	/**
	 * Fires ContainerInsertionCheckEvent from the private
	 * {@code Item.testInsertHollowItem}. Returns true to reject the insertion.
	 */
	public static boolean fireContainerInsertionCheckEvent(Object container, Object incoming, boolean testItemCount) {
		try {
			return getInstance().fireContainerInsertionCheck(
				(com.wurmonline.server.items.Item) container,
				(com.wurmonline.server.items.Item) incoming,
				testItemCount);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire ContainerInsertionCheckEvent", t);
			return false;
		}
	}

	/**
	 * Fires TrellisPruningEvent from {@code TrellisBehaviour.prune}.
	 * Returns true to abort the prune.
	 */
	public static boolean fireTrellisPruningEvent(Object action, Object performer, Object sickle,
	                                              Object trellis, float counter) {
		try {
			return getInstance().fireTrellisPruning(
				(com.wurmonline.server.behaviours.Action) action,
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) sickle,
				(com.wurmonline.server.items.Item) trellis,
				counter);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TrellisPruningEvent", t);
			return false;
		}
	}

	/**
	 * Fires FaithGainResetEvent from {@code Players.resetFaithGain}.
	 * Returns true to skip the vanilla reset.
	 */
	public static boolean fireFaithGainResetEvent() {
		try {
			return getInstance().fireFaithGainReset();
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire FaithGainResetEvent", t);
			return false;
		}
	}

	/**
	 * Fires CreatureMovementSpeedEvent from {@code MovementScheme.getSpeedModifier}.
	 * Returns the (possibly modified) speed; on failure, returns the original.
	 */
	public static float fireCreatureMovementSpeedEvent(Object creature, float speed) {
		try {
			return getInstance().fireCreatureMovementSpeed(
				(com.wurmonline.server.creatures.Creature) creature, speed);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire CreatureMovementSpeedEvent", t);
			return speed;
		}
	}

	/**
	 * Fires CreatureTemplateColorEvent from {@code CreatureTemplate.getColorRed/Green/Blue}.
	 * Returns the (possibly modified) color value; on failure, returns the original.
	 */
	public static int fireCreatureTemplateColorEvent(Object template, String channel, int value) {
		try {
			com.garward.wurmmodloader.api.events.creature.CreatureTemplateColorEvent.Channel ch =
				com.garward.wurmmodloader.api.events.creature.CreatureTemplateColorEvent.Channel.valueOf(channel);
			return getInstance().fireCreatureTemplateColor(
				(com.wurmonline.server.creatures.CreatureTemplate) template, ch, value);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire CreatureTemplateColorEvent", t);
			return value;
		}
	}

	/**
	 * Fires TerrainFlattenEvent from {@code Flattening.flatten}. Returns true to cancel.
	 */
	public static boolean fireTerrainFlattenEvent(Object performer, Object tool, int tile,
			int tileX, int tileY, float counter, Object action) {
		try {
			return getInstance().fireTerrainFlatten(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				tile, tileX, tileY, counter,
				(com.wurmonline.server.behaviours.Action) action);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TerrainFlattenEvent", t);
			return false;
		}
	}

	/**
	 * Fires TerrainPackEvent from {@code Terraforming.pack}. Returns true to cancel.
	 */
	public static boolean fireTerrainPackEvent(Object performer, Object tool, int tileX, int tileY,
			boolean onSurface, int tile, float counter, Object action) {
		try {
			return getInstance().fireTerrainPack(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				tileX, tileY, onSurface, tile, counter,
				(com.wurmonline.server.behaviours.Action) action);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TerrainPackEvent", t);
			return false;
		}
	}

	/**
	 * Fires TerrainCultivateEvent from {@code Terraforming.cultivate}. Returns true to cancel.
	 */
	public static boolean fireTerrainCultivateEvent(Object performer, Object tool, int tileX, int tileY,
			boolean onSurface, int tile, float counter) {
		try {
			return getInstance().fireTerrainCultivate(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				tileX, tileY, onSurface, tile, counter);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TerrainCultivateEvent", t);
			return false;
		}
	}

	/**
	 * Fires CaveMineEvent from {@code CaveTileBehaviour.mine}. Returns true to cancel
	 * (cancel returns true from mine() — matches the action-loop done/abort semantics).
	 */
	public static boolean fireCaveMineEvent(Object action, Object performer, Object source,
			int tileX, int tileY, short mineAction, float counter, int dir, Object digTilePos) {
		try {
			return getInstance().fireCaveMine(
				(com.wurmonline.server.behaviours.Action) action,
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) source,
				tileX, tileY, mineAction, counter, dir,
				(com.wurmonline.math.TilePos) digTilePos);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire CaveMineEvent", t);
			return false;
		}
	}

	/**
	 * Fires ActionAllowedOnVehicleEvent from {@code Actions.isActionAllowedOnVehicle}.
	 * Returns the (possibly overridden) allow/deny verdict.
	 */
	public static boolean fireActionAllowedOnVehicleEvent(short action, boolean vanillaAllowed) {
		try {
			return getInstance().fireActionAllowedOnVehicle(action, vanillaAllowed);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire ActionAllowedOnVehicleEvent", t);
			return vanillaAllowed;
		}
	}

	/**
	 * Fires CaveTileActionEvent from {@code CaveTileBehaviour.action}. Returns true to cancel
	 * (cancel returns true from action() — action-loop done/abort semantics).
	 */
	public static boolean fireCaveTileActionEvent(Object action, Object performer, Object source,
			int tileX, int tileY, boolean onSurface, int heightOffset,
			int tile, int dir, short actionShort, float counter) {
		try {
			return getInstance().fireCaveTileAction(
				(com.wurmonline.server.behaviours.Action) action,
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) source,
				tileX, tileY, onSurface, heightOffset, tile, dir, actionShort, counter);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire CaveTileActionEvent", t);
			return false;
		}
	}

	/**
	 * Fires CaveTileGetBehavioursEvent after {@code CaveTileBehaviour.getBehavioursFor}
	 * returns. Listeners mutate the live entries list.
	 */
	public static void fireCaveTileGetBehavioursEvent(Object performer, Object source,
			int tileX, int tileY, boolean onSurface, int tile, int dir, Object entries) {
		try {
			getInstance().fireCaveTileGetBehaviours(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) source,
				tileX, tileY, onSurface, tile, dir,
				(java.util.List) entries);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire CaveTileGetBehavioursEvent", t);
		}
	}

	/**
	 * Fires SurfaceRockActionEvent from {@code TileRockBehaviour.action}. Returns true to cancel.
	 */
	public static boolean fireSurfaceRockActionEvent(Object action, Object performer, Object source,
			int tileX, int tileY, boolean onSurface, int heightOffset,
			int tile, short actionShort, float counter) {
		try {
			return getInstance().fireSurfaceRockAction(
				(com.wurmonline.server.behaviours.Action) action,
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) source,
				tileX, tileY, onSurface, heightOffset, tile, actionShort, counter);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire SurfaceRockActionEvent", t);
			return false;
		}
	}

	/**
	 * Fires SurfaceRockGetBehavioursEvent after {@code TileRockBehaviour.getBehavioursFor}
	 * returns.
	 */
	public static void fireSurfaceRockGetBehavioursEvent(Object performer, Object source,
			int tileX, int tileY, boolean onSurface, int tile, Object entries) {
		try {
			getInstance().fireSurfaceRockGetBehaviours(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) source,
				tileX, tileY, onSurface, tile,
				(java.util.List) entries);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire SurfaceRockGetBehavioursEvent", t);
		}
	}

	/**
	 * Fires DirtDestinationResolveEvent and performs the insertItem call on
	 * the resolved target. Used by {@code TerraformingDigInnerPatch} and
	 * {@code FlatteningInnerPatch} to replace the hardcoded
	 * {@code target.insertItem(dirt, true)} call sites.
	 *
	 * @return the boolean result of the insertItem call (matches vanilla
	 *         return contract). Falls back to {@code vanillaTarget.insertItem}
	 *         on any failure so the vanilla path is preserved.
	 */
	public static boolean fireDirtDestinationResolve(Object dirt, Object performer, Object tool,
			Object vanillaTarget, boolean dredging, boolean toPile, String contextName) {
		com.wurmonline.server.items.Item dirtItem = (com.wurmonline.server.items.Item) dirt;
		com.wurmonline.server.items.Item vanilla = (com.wurmonline.server.items.Item) vanillaTarget;
		try {
			com.wurmonline.server.items.Item resolved = getInstance().fireDirtDestinationResolve(
				dirtItem,
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				vanilla, dredging, toPile, contextName);
			com.wurmonline.server.items.Item target = resolved != null ? resolved : vanilla;
			if (target == null) return false;
			return target.insertItem(dirtItem, true);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DirtDestinationResolveEvent", t);
			try {
				return vanilla != null && vanilla.insertItem(dirtItem, true);
			} catch (Throwable t2) {
				return false;
			}
		}
	}

	/**
	 * Fires DirtSourceResolveEvent and returns the resolved carried item.
	 * Falls back to {@code vanillaFound} on failure.
	 */
	public static com.wurmonline.server.items.Item fireDirtSourceResolve(Object performer, int templateId,
			Object vanillaFound, String contextName) {
		com.wurmonline.server.items.Item vanilla = (com.wurmonline.server.items.Item) vanillaFound;
		try {
			return getInstance().fireDirtSourceResolve(
				(com.wurmonline.server.creatures.Creature) performer,
				templateId, vanilla, contextName);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DirtSourceResolveEvent", t);
			return vanilla;
		}
	}

	/**
	 * Fires DigCapacityOverrideEvent for the pile-count gate and returns the
	 * (possibly overridden) int value. Falls back to vanillaValue on failure.
	 */
	public static int fireDigCapacityNumItems(Object performer, Object tool, Object target,
			int vanillaValue, boolean toPile, boolean dredging) {
		try {
			long v = getInstance().fireDigCapacityOverride(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				(com.wurmonline.server.items.Item) target,
				"NUM_ITEMS_NOT_COINS", (long) vanillaValue, toPile, dredging);
			return (int) v;
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DigCapacityOverrideEvent (NUM_ITEMS)", t);
			return vanillaValue;
		}
	}

	/**
	 * Fires DigCapacityOverrideEvent for the canCarry gate. vanillaValue
	 * marshals as 1/0.
	 */
	public static boolean fireDigCapacityCanCarry(Object performer, Object tool, Object target,
			boolean vanillaValue, boolean toPile, boolean dredging) {
		try {
			long v = getInstance().fireDigCapacityOverride(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				(com.wurmonline.server.items.Item) target,
				"CAN_CARRY", vanillaValue ? 1L : 0L, toPile, dredging);
			return v != 0L;
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DigCapacityOverrideEvent (CAN_CARRY)", t);
			return vanillaValue;
		}
	}

	/**
	 * Fires DigCapacityOverrideEvent for the free-volume gate.
	 */
	public static int fireDigCapacityFreeVolume(Object performer, Object tool, Object target,
			int vanillaValue, boolean toPile, boolean dredging) {
		try {
			long v = getInstance().fireDigCapacityOverride(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) tool,
				(com.wurmonline.server.items.Item) target,
				"FREE_VOLUME", (long) vanillaValue, toPile, dredging);
			return (int) v;
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire DigCapacityOverrideEvent (FREE_VOLUME)", t);
			return vanillaValue;
		}
	}

	/**
	 * Fires ActionPerformRequestEvent. Returns {@code true} if the action
	 * should be cancelled (dispatcher will {@code return} early). Any
	 * exception is swallowed and treated as not-cancelled so vanilla
	 * dispatch always runs.
	 */
	public static boolean fireActionPerformRequest(Object performer, long subject, long target, short actionShort) {
		try {
			return getInstance().fireActionPerformRequest(
				(com.wurmonline.server.creatures.Creature) performer,
				subject, target, actionShort);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire ActionPerformRequestEvent", t);
			return false;
		}
	}

	/**
	 * Fires ActionMenuBuildEvent with the live {@code availableActions} list
	 * so listeners can mutate in place before the menu is sent.
	 */
	public static void fireActionMenuBuild(Object communicator, Object availableActions,
			String helpString, boolean sendToSelectBar) {
		try {
			getInstance().fireActionMenuBuild(
				(com.wurmonline.server.creatures.Communicator) communicator,
				(java.util.List) availableActions,
				helpString, sendToSelectBar);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire ActionMenuBuildEvent", t);
		}
	}

	/**
	 * Fires TileMenuBuildEvent. Target-aware tile menu injection path.
	 */
	public static void fireTileMenuBuild(Object performer, long target, boolean onSurface,
			Object source, Object availableActions, String helpString) {
		try {
			getInstance().fireTileMenuBuild(
				(com.wurmonline.server.creatures.Creature) performer,
				target, onSurface,
				(com.wurmonline.server.items.Item) source,
				(java.util.List) availableActions, helpString);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TileMenuBuildEvent", t);
		}
	}

	/**
	 * Fires ItemMenuBuildEvent. Target-aware item menu injection path.
	 */
	public static void fireItemMenuBuild(Object performer, long targetId, Object source,
			Object availableActions, String helpString) {
		try {
			getInstance().fireItemMenuBuild(
				(com.wurmonline.server.creatures.Creature) performer,
				targetId,
				(com.wurmonline.server.items.Item) source,
				(java.util.List) availableActions, helpString);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire ItemMenuBuildEvent", t);
		}
	}

	/**
	 * Fires TileDirtConsumeEvent. Returns {@code true} if a listener claimed
	 * the consumption (patch then skips vanilla destroyItem and deducts one
	 * template weight from the source).
	 */
	public static boolean fireTileDirtConsume(Object action, Object performer, Object source) {
		com.wurmonline.server.items.Item src = (com.wurmonline.server.items.Item) source;
		try {
			boolean consumed = getInstance().fireTileDirtConsume(
				(com.wurmonline.server.behaviours.Action) action,
				(com.wurmonline.server.creatures.Creature) performer,
				src);
			if (consumed && src != null) {
				src.setWeight(src.getWeightGrams() - src.getTemplate().getWeightGrams(), true);
			}
			return consumed;
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire TileDirtConsumeEvent", t);
			return false;
		}
	}

	/**
	 * Fires PlanterItemAcceptEvent. vanillaValue is the pre-event result of
	 * {@code Item.isRaw()} / {@code Item.isSpice()}.
	 */
	public static boolean firePlanterItemAccept(Object performer, Object herb, Object planter,
			String kindName, boolean vanillaValue) {
		try {
			return getInstance().firePlanterItemAccept(
				(com.wurmonline.server.creatures.Creature) performer,
				(com.wurmonline.server.items.Item) herb,
				(com.wurmonline.server.items.Item) planter,
				kindName, vanillaValue);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire PlanterItemAcceptEvent", t);
			return vanillaValue;
		}
	}

	/**
	 * Fires BulkStackNameEvent. Listeners can canonicalize the item name used
	 * for bulk-stack matching.
	 */
	public static String fireBulkStackName(Object item, String vanillaName) {
		try {
			return getInstance().fireBulkStackName(
				(com.wurmonline.server.items.Item) item, vanillaName);
		} catch (Throwable t) {
			java.util.logging.Logger.getLogger(ProxyServerHook.class.getName())
				.log(java.util.logging.Level.WARNING, "Failed to fire BulkStackNameEvent", t);
			return vanillaName;
		}
	}

	/**
	 * Called from {@link com.garward.wurmmodloader.core.bytecode.patches.ServerPreInitPatch}
	 * at the top of {@code Villages.loadVillages()}. Posts {@link com.garward.wurmmodloader.api.events.server.ServerPreInitEvent}
	 * so subsystems can seed/repair zones DB rows before they're loaded into memory.
	 */
	public static void fireServerPreInitEvent() {
		getInstance().fireOnServerPreInit();
	}

	public static synchronized ProxyServerHook getInstance() {
		if (instance == null) {
			instance = new ProxyServerHook();
		}
		return instance;
	}
}

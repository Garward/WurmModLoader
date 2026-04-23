package com.garward.wurmmodloader.core.bytecode;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ActionArrayBoundsCheckPatch;
// ActionConstructorSafetyPatch removed - addCatch() fails on complex constructors
import com.garward.wurmmodloader.core.bytecode.patches.RemoveActionsFinalModifierPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ActionFatiguePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ActionSpeedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ActionTimingPatch;
import com.garward.wurmmodloader.core.bytecode.patches.BodyMenuPopulatePatch;
import com.garward.wurmmodloader.core.bytecode.patches.BodyPartModActionsPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CombatAttackPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CombatCriticalHitPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CombatDamagePatch;
import com.garward.wurmmodloader.core.bytecode.patches.CombatDualWieldPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CombatSwingSpeedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CommunicatorChannelPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CommunicatorMessagePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ContainerVolumePatch;
import com.garward.wurmmodloader.core.bytecode.patches.CharmAnimalPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CreatureBreedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.DominatePatch;
import com.garward.wurmmodloader.core.bytecode.patches.CreatureDeathPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CreaturePositionPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CreatureSpawnPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CropGrowthPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CropHarvestPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ConnectionFactoryCtorWidenPatch;
import com.garward.wurmmodloader.core.bytecode.patches.DbConnectionOpenedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.DbConnectorInitPatch;
import com.garward.wurmmodloader.core.bytecode.patches.DbIndexManagerMaintenancePatch;
import com.garward.wurmmodloader.core.bytecode.patches.DbMigrationPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemDropPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemContainerPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemDamagePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemEnchantmentConstructorPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemEnchantmentStringsPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemExaminePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemTemplatesCreatedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemMoveCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemTradePatch;
import com.garward.wurmmodloader.core.bytecode.patches.TradeBalancePatch;
import com.garward.wurmmodloader.core.bytecode.patches.TradeSessionStartPatch;
import com.garward.wurmmodloader.core.bytecode.patches.VillageExpansionCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.TerrainModificationPatch;
import com.garward.wurmmodloader.core.bytecode.patches.TerrainFlattenPatch;
import com.garward.wurmmodloader.core.bytecode.patches.TerrainPackPatch;
import com.garward.wurmmodloader.core.bytecode.patches.TerrainCultivatePatch;
import com.garward.wurmmodloader.core.bytecode.patches.CaveMinePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ActionAllowedOnVehiclePatch;
import com.garward.wurmmodloader.core.bytecode.patches.CaveTileActionPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CaveTileGetBehavioursPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SurfaceMiningSlopeLowerPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SurfaceRockActionPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SurfaceRockGetBehavioursPatch;
import com.garward.wurmmodloader.core.bytecode.patches.TerraformingDigInnerPatch;
import com.garward.wurmmodloader.core.bytecode.patches.FlatteningInnerPatch;
import com.garward.wurmmodloader.core.bytecode.patches.BehaviourDispatcherPatch;
import com.garward.wurmmodloader.core.bytecode.patches.BehaviourDispatcherMenuPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CommunicatorMenuPatch;
import com.garward.wurmmodloader.core.bytecode.patches.TileDirtConsumePatch;
import com.garward.wurmmodloader.core.bytecode.patches.PlanterItemAcceptPatch;
import com.garward.wurmmodloader.core.bytecode.patches.BulkStackNamePatch;
import com.garward.wurmmodloader.core.bytecode.patches.GuardPlanPollPatch;
import com.garward.wurmmodloader.core.bytecode.patches.StructurePlanningCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.WallPlanningGatePatch;
import com.garward.wurmmodloader.core.bytecode.patches.FloorPlanningGatePatch;
import com.garward.wurmmodloader.core.bytecode.patches.FloorLevelGatePatch;
import com.garward.wurmmodloader.core.bytecode.patches.QuestionAnswerPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SacrificePostPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ContainerInsertionCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.TrellisPruningPatch;
import com.garward.wurmmodloader.core.bytecode.patches.FaithGainResetPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CreatureMovementSpeedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.CreatureTemplateColorPatch;
import com.garward.wurmmodloader.core.bytecode.patches.MaterialBonusExtendedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.MountEquipmentCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.MovementBroadcastPatch;
import com.garward.wurmmodloader.core.bytecode.patches.OpportunityAttackPatch;
import com.garward.wurmmodloader.core.bytecode.patches.GmHelpAddendumPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PlayerLoginPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PlayerLogoutPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PlayerSkillLossPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PrayerFaithPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PriestRestrictionPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ShieldCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SkillAdvancePatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellCastingTimePatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellCooldownPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellCastAttemptPatch;
import com.garward.wurmmodloader.core.bytecode.patches.DeitySpellRegistrationPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellDifficultyPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellEffectPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellPreconditionPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellResistPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SacrificePatches;
import com.garward.wurmmodloader.core.bytecode.patches.SpellVisibilityPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpellPowerPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpecialMoveHandlePatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpecialMoveSendPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerConfigLoadPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerPollPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerPreInitPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerShutdownPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerStartPatch;
import com.garward.wurmmodloader.core.bytecode.patches.WeaponStatQueryPatch;
import com.garward.wurmmodloader.core.bytecode.patches.WeaponUsePatch;
import com.garward.wurmmodloader.core.bytecode.patches.vanillafixes.ActionTimerFix;
import com.garward.wurmmodloader.core.bytecode.patches.vanillafixes.NpcMoveTargetGuardFix;
import com.garward.wurmmodloader.core.bytecode.patches.vanillafixes.VanillaNextIntGuardsFix;
import com.garward.wurmmodloader.core.bytecode.patches.VehicleMountCreaturePatch;
import com.garward.wurmmodloader.core.bytecode.patches.VehicleMountItemPatch;
import com.garward.wurmmodloader.core.bytecode.patches.VehicleSpeedPatch;
import com.garward.wurmmodloader.core.registry.RuntimeRegistries;

import java.util.Arrays;
import java.util.List;

/**
 * Registers the core bytecode patches that replace legacy ProxyServerHook wiring.
 */
public final class CoreBytecodePatches {

    private static final List<BytecodePatch> CORE_PATCHES = Arrays.asList(
        new RemoveActionsFinalModifierPatch(),  // CRITICAL: Must run FIRST before mods register actions
        new ItemEnchantmentConstructorPatch(),  // CRITICAL: Must run early so mods can extend ItemEnchantment
        new ServerConfigLoadPatch(),  // CRITICAL: Syncs YAML config to DB before server reads config
        new ServerPreInitPatch(),     // Fires before Villages.loadVillages so subsystems can seed wurmzones.db
        new ServerStartPatch(),
        new ServerShutdownPatch(),
        new ItemTemplatesCreatedPatch(),
        // ActionConstructorSafetyPatch removed - addCatch() fails on complex constructors
        new ActionArrayBoundsCheckPatch(),  // CRITICAL: Prevents crashes from unregistered ModActions
        new BodyMenuPopulatePatch(),
        new BodyPartModActionsPatch(),
        new CreatureDeathPatch(),
        new CreatureBreedPatch(),
        new CharmAnimalPatch(),
        new DominatePatch(),
        new CropHarvestPatch(),  // Rewritten to use ExprEditor instead of fragile insertAt()
        new CropGrowthPatch(),
        new CombatAttackPatch(),
        new CombatDamagePatch(),
        new CombatCriticalHitPatch(),
        new CombatSwingSpeedPatch(),
        new CreatureSpawnPatch(),
        new CreaturePositionPatch(),
        new MovementBroadcastPatch(),
        new ItemExaminePatch(),
        new ItemEnchantmentStringsPatch(),
        new ItemDropPatch(),
        new ItemTradePatch(),
        new ItemContainerPatch(),
        new ItemDamagePatch(),
        new ItemMoveCheckPatch(),
        new TradeSessionStartPatch(),
        new TradeBalancePatch(),
        new VillageExpansionCheckPatch(),
        new TerrainModificationPatch(),
        new TerrainFlattenPatch(),
        new TerrainPackPatch(),
        new TerrainCultivatePatch(),
        new CaveMinePatch(),
        new CaveTileActionPatch(),
        new CaveTileGetBehavioursPatch(),
        new SurfaceRockActionPatch(),
        new SurfaceMiningSlopeLowerPatch(),
        new SurfaceRockGetBehavioursPatch(),
        new TerraformingDigInnerPatch(),
        new FlatteningInnerPatch(),
        new BehaviourDispatcherPatch(),
        new BehaviourDispatcherMenuPatch(),
        new CommunicatorMenuPatch(),
        new TileDirtConsumePatch(),
        new PlanterItemAcceptPatch(),
        new BulkStackNamePatch(),
        new ActionAllowedOnVehiclePatch(),
        new GuardPlanPollPatch(),
        new StructurePlanningCheckPatch(),
        new WallPlanningGatePatch(),
        new FloorPlanningGatePatch(),
        new FloorLevelGatePatch(),
        new QuestionAnswerPatch(),
        new SacrificePostPatch(),
        new ContainerInsertionCheckPatch(),
        new TrellisPruningPatch(),
        new FaithGainResetPatch(),
        new CreatureMovementSpeedPatch(),
        new CreatureTemplateColorPatch(),
        new ContainerVolumePatch(),
        new WeaponUsePatch(),
        new WeaponStatQueryPatch(),
        new MaterialBonusExtendedPatch(),
        new CombatDualWieldPatch(),
        new OpportunityAttackPatch(),
        new ShieldCheckPatch(),
        new SpecialMoveSendPatch(),
        new SpecialMoveHandlePatch(),
        new ActionFatiguePatch(),
        new ActionTimingPatch(),
        new ActionSpeedPatch(),
        new SkillAdvancePatch(),
        new PlayerLoginPatch(),
        new GmHelpAddendumPatch(),
        new PlayerLogoutPatch(),
        new PlayerSkillLossPatch(),
        new PriestRestrictionPatch(),
        new PrayerFaithPatch(),
        new SpellCastingTimePatch(),
        new SpellCooldownPatch(),
        new SpellPowerPatch(),
        new SpellCastAttemptPatch(),
        new SpellEffectPatch(),
        new SpellDifficultyPatch(),
        new SpellPreconditionPatch(),
        new SpellResistPatch(),
        new SpellVisibilityPatch(),
        new DeitySpellRegistrationPatch(),
        new SacrificePatches(),
        new ServerPollPatch(),
        new CommunicatorMessagePatch(),
        new CommunicatorChannelPatch(),
        new VehicleMountCreaturePatch(),
        new VehicleMountItemPatch(),
        new VehicleSpeedPatch(),
        new MountEquipmentCheckPatch(),
        // Database backend SPI
        new ConnectionFactoryCtorWidenPatch(),
        new DbConnectorInitPatch(),
        new DbConnectionOpenedPatch(),
        new DbMigrationPatch(),
        new DbIndexManagerMaintenancePatch(),
        // Vanilla bug fixes — non-opinionated, crash-class only. See docs/guides/vanilla-fixes.md
        new VanillaNextIntGuardsFix(),
        new NpcMoveTargetGuardFix(),
        new ActionTimerFix()
    );

    private CoreBytecodePatches() {}

    public static void registerAll() {
        CORE_PATCHES.forEach(RuntimeRegistries.BYTECODE_PATCHES::register);
    }
}

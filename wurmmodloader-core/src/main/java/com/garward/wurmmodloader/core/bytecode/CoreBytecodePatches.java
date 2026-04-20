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
import com.garward.wurmmodloader.core.bytecode.patches.ItemDropPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemContainerPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemDamagePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemEnchantmentConstructorPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemEnchantmentStringsPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemExaminePatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemTemplatesCreatedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ItemTradePatch;
import com.garward.wurmmodloader.core.bytecode.patches.MaterialBonusExtendedPatch;
import com.garward.wurmmodloader.core.bytecode.patches.MountEquipmentCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.MovementBroadcastPatch;
import com.garward.wurmmodloader.core.bytecode.patches.OpportunityAttackPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PlayerLoginPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PlayerLogoutPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PlayerSkillLossPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PrayerFaithPatch;
import com.garward.wurmmodloader.core.bytecode.patches.PriestRestrictionPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ShieldCheckPatch;
import com.garward.wurmmodloader.core.bytecode.patches.SkillAdvancePatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpecialMoveHandlePatch;
import com.garward.wurmmodloader.core.bytecode.patches.SpecialMoveSendPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerConfigLoadPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerPollPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerShutdownPatch;
import com.garward.wurmmodloader.core.bytecode.patches.ServerStartPatch;
import com.garward.wurmmodloader.core.bytecode.patches.WeaponStatQueryPatch;
import com.garward.wurmmodloader.core.bytecode.patches.WeaponUsePatch;
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
        new PlayerLogoutPatch(),
        new PlayerSkillLossPatch(),
        new PriestRestrictionPatch(),
        new PrayerFaithPatch(),
        new ServerPollPatch(),
        new CommunicatorMessagePatch(),
        new CommunicatorChannelPatch(),
        new VehicleMountCreaturePatch(),
        new VehicleMountItemPatch(),
        new VehicleSpeedPatch(),
        new MountEquipmentCheckPatch()
    );

    private CoreBytecodePatches() {}

    public static void registerAll() {
        CORE_PATCHES.forEach(RuntimeRegistries.BYTECODE_PATCHES::register);
    }
}

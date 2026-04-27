# EventLogic Registry Examples

The framework ships with four JSON loadable registries inside the `eventlogic` packages. Each registry is paired with an event handler that subscribes to the event bus and applies the registered profiles when matching events fire. Until something fills the registry, the handlers do nothing and vanilla behavior is unchanged.

These `.json.example` files document the schema each registry expects. **They are not loaded automatically.** A mod has to opt in by reading the file and handing it to the registry during `init()`.

## What ships in the framework

| Registry | Profile Class | Event Handler | What it controls |
|---|---|---|---|
| `MaterialProfileRegistry` | `MaterialProfile` | `MaterialEventHandler` | Per material multipliers for damage, decay, improve, repair time, action speed, skill advance, weapon stats, spell power, armour, etc. |
| `SwingSpeedRegistry` | `SwingSpeedProfile` | `SwingSpeedAdjuster` | Minimum swing time per weapon class plus rarity based reduction. |
| `WeaponTimerRegistry` | `WeaponTimerPolicy` | `WeaponTimerReset` | Per weapon timer reset value applied on `WeaponUseEvent`. |
| `DualWieldRegistry` | `DualWieldProfile` | `DualWieldScheduler` | Eligibility rules for dual wielding (creature type, weapon size, blacklist). |

All four handlers are registered to `RuntimeRegistries.EVENT_LOGIC` during `SystemBootstrap` (lines 36 to 39), so they are already subscribed to the bus before any mod loads. You only need to populate their registries.

## How to load these from a mod

Drop the JSON file alongside your mod and load it during `init()`. Strip the `.example` suffix when shipping a real config.

```java
import com.garward.wurmmodloader.core.eventlogic.materials.MaterialProfileRegistry;
import com.garward.wurmmodloader.core.eventlogic.combat.timing.SwingSpeedRegistry;
import com.garward.wurmmodloader.core.eventlogic.combat.timing.WeaponTimerRegistry;
import com.garward.wurmmodloader.core.eventlogic.combat.timing.DualWieldRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class MyRebalanceMod implements WurmModLoader {

    @Override
    public void init() {
        Path configDir = Paths.get("mods", "myrebalance");

        MaterialProfileRegistry.getInstance().loadFromJson(configDir.resolve("materials.json"));
        SwingSpeedRegistry.getInstance().load(configDir.resolve("swingspeed.json"));
        WeaponTimerRegistry.getInstance().load(configDir.resolve("weapontimer.json"));
        DualWieldRegistry.getInstance().load(configDir.resolve("dualwield.json"));
    }
}
```

That is the entire wiring. The framework handles event subscription, lookup, and application. Your mod is responsible only for populating the registry from disk (or programmatically via `register()` if you prefer).

## Schema notes

### Top level shape

Every file accepts either a bare JSON array of profiles or a wrapper object with a `"profiles"` key. The wrapper form (used in these examples) leaves room for a `_comment` field and any future top level metadata.

```json
{ "profiles": [ { ... }, { ... } ] }
```

### Numeric multipliers

In `materials.json`, every numeric field is a **multiplier** applied to the vanilla base value. `1.0` means no change. `1.10` is a 10 percent buff. `0.85` is a 15 percent reduction. Omitting a field is identical to setting it to 1.0; the handler simply skips that adjustment.

### Material identity

Materials can be referenced by either `"materialId"` (the raw byte) or `"material"` (the lowercase string name). The string form goes through `com.wurmonline.server.items.Materials.convertMaterialStringIntoByte`, so any name accepted by that method works (`"iron"`, `"steel"`, `"oakenwood"`, `"willow"`, `"magic"`, etc.). Use `materialId` for materials that lack a string mapping.

**Watch out for typos.** `convertMaterialStringIntoByte` returns `0` (MATERIAL_UNDEFINED) for any unknown string. The profile registers under id 0 and silently never matches a real item. Multiple typos all collide on key 0 and overwrite each other in the registry. There is no warning logged. Verify your material names against the switch in `Materials.convertMaterialStringIntoByte` if a profile seems to have no effect.

### Whitelists and blacklists

`templates`, `materials`, and `templateBlacklist` accept arrays of integer template ids. An empty or omitted whitelist means **match anything**. Template ids can be looked up in `wurmquery search ItemList` or by reading the `ItemList` decompiled source.

### Bonus and stat enums

`bonusMultipliers` keys are `MaterialBonusEvent.BonusType` names: `CREATION`, `MOVEMENT`, `ANCHOR`, `PENDULUM`, `LOCKPICK`, `SPELL_POWER`, `ARMOUR`, `SHATTER`, `OTHER`.

`weaponStats` keys are `WeaponStatQueryEvent.StatType` names: `DAMAGE`, `SPEED`, `PARRY`, `ARMOUR_DAMAGE`, `BASH`, `OTHER`.

`spellPowerMultipliers` keys are stringified enchantment byte ids. Look up enchantment ids in `com.wurmonline.server.spells.Spells`.

## When not to use these

If your mod only needs to adjust one or two materials, skip the JSON entirely and call `MaterialProfileRegistry.getInstance().register(profile)` directly with a `MaterialProfile.builder(material).damageModifier(1.10).build()`. The JSON path exists for bulk content, not for one off tweaks.

If your mod needs behavior that none of these registries describe (per item overrides, per player modifiers, runtime conditions), subscribe to the underlying event yourself and ignore the registries entirely. The registries are a convenience layer, not a requirement.

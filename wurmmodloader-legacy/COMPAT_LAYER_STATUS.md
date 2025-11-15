# Legacy Compatibility Layer Status

## Overview

The legacy module provides backward compatibility for mods using the old `org.gotti.wurmunlimited` package structure. This is achieved through a **hybrid approach** combining thin proxies for public APIs with full implementations for complex internal classes.

## Compatibility Strategy

### Tier 1: Thin Proxy Interfaces (✅ Complete)

These interfaces simply extend the new implementations, providing complete type compatibility:

**Mod Interfaces:**
- `WurmServerMod` → extends `com.garward.wurmmodloader.modloader.interfaces.WurmServerMod`
- `WurmMod`, `Configurable`, `PreInitable` → extend new interfaces
- `ServerStartedListener`, `ServerShutdownListener`, `ServerPollListener` → extend new interfaces
- `PlayerLoginListener`, `PlayerMessageListener`, `ChannelMessageListener` → extend new interfaces
- `ItemTemplatesCreatedListener`, `MessagePolicy` → extend new interfaces

**Support Interfaces:**
- `ModQuestion` → extends `com.garward.wurmmodloader.modsupport.questions.ModQuestion`
- `IntraRequest` → extends `com.garward.wurmmodloader.modcomm.intra.IntraRequest`
- `IChannelListener`, `ActionPerformer`, `BehaviourProvider` → extend new interfaces
- `ModAction`, `IIdType`, `Property`, `TraitsSetter` → extend new interfaces
- `ModCreature`, `ModelNameProvider`, `VehicleFacade` → extend new interfaces

###  Tier 2: Delegating Wrapper Classes (✅ Complete)

These classes delegate all method calls to the new implementations:

**Core Classes:**
- `ModLoader` → extends `com.garward.wurmmodloader.modloader.ModLoader`
- `ModActions` → delegates to `com.garward.wurmmodloader.modsupport.actions.ModActions`
- `ModQuestions` → delegates to `com.garward.wurmmodloader.modsupport.questions.ModQuestions`

**Server Classes:**
- `ServerHook` → extends new implementation
- `SimpleMod` → extends new implementation

### Tier 3: Simple Extension Classes (✅ Complete)

These classes can simply extend the new implementation:

**Builder Classes:**
- `ItemTemplateBuilder`, `CreatureTemplateBuilder`, `EncounterBuilder`
- `ActionEntryBuilder`, `BmlBuilder`

**Parser Classes:**
- `CreatureTemplateParser`, `CreatureTypesParser`, `ItemIdParser`

**Support Classes:**
- `ServerLauncher`, `DelegatedLauncher`, `PatchedLauncher`
- `WrappedBehaviourProvider`, `ChainedBehaviourProvider`

### Tier 4: Full Implementation Classes (⚠️ Hybrid)

These classes retain full original implementations due to:
- Complex constructors requiring parameters
- Enum types that cannot be extended
- Private constructors or package-private access
- Internal cross-references between legacy classes

**Classes with Full Implementation:**
- `IdType` (enum)
- `ActionPropagation` (enum)
- `TextStyle` (enum)
- `ModVehicleBehaviour` (enum)
- `Property` (complex)
- `ActionPerformerChain`, `WrappedBehaviour` (package-private)
- `ActionPerformerBase`, `ActionPerformerBehaviour` (complex constructors)
- `BmlNodeBuilder`, `VehicleFacadeImpl` (complex constructors)
- `ProxyServerHook` (private constructor)
- `Listeners` (generic with complex constructor)
- `NamedIdParser`, `NonFreezingNamedIdParser` (abstract methods)

**Communication Classes (Original Implementations):**
- `ModComm`, `ModCommHandler`, `ModCommConstants`
- `Channel`, `PacketReader`, `PacketWriter`, `PlayerModConnection`
- `ModIntraServer`, `ModIntraServerHandler`, `ModIntraServerConstants`
- `BBHelper`, `IntraRequestHandler`
- `GetRemoteTemplatesMessage`, `ModPlayerTransfer`, `TemplateIdMapper`

**Support Classes (Original Implementations):**
- `IdFactory`, `ModSupportDb`
- `ModCreatures`, `ModItems`, `ModTraits`
- `ModPlayerProperties`, `ModVehicleBehaviours`

## Backward Compatibility Guarantees

### ✅ Fully Compatible

Mods using these APIs will work without any code changes:

1. **Mod Interfaces**: All mod lifecycle interfaces (`WurmServerMod`, listeners, etc.)
2. **Builder APIs**: `ItemTemplateBuilder`, `CreatureTemplateBuilder`, `ActionEntryBuilder`
3. **Static Utilities**: `ModActions`, `ModQuestions`, `ModItems`, `ModCreatures`
4. **Core Loader**: `ModLoader` class

### ⚠️ Partially Compatible

Some advanced internal APIs may require attention:

1. **Enums**: Direct enum comparisons work, but type casting between old/new enums may fail
2. **Complex Constructors**: Classes with specific constructor requirements may need updates
3. **Package-Private Classes**: Classes not part of public API may have limitations

### 📝 Migration Recommended

For best long-term support, mods should migrate to the new package structure:

```java
// OLD (deprecated but functional)
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

// NEW (recommended)
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
```

## Testing Status

- ✅ Compilation: Core compatibility layer compiles
- ⏳ Runtime Testing: Requires testing with actual mods
- ⏳ Integration Testing: Needs validation with mod ecosystem

## Known Limitations

1. **Enum Type Incompatibility**: `IdType` enum values cannot be directly cast between old/new packages
2. **Internal API Changes**: Some internal implementation classes may behave differently
3. **Reflection**: Code using reflection on package names will need updates

## Migration Guide

For mod authors who want to update to the new package structure:

1. **Update imports** from `org.gotti.wurmunlimited.*` to `com.garward.wurmmodloader.*`
2. **Recompile** against the new API
3. **Test** thoroughly with your mod
4. **Update** build dependencies to use new artifact coordinates

## Future Work

- Runtime testing with real mods
- Create example migration for common mod patterns
- Document any discovered edge cases
- Consider creating an automated migration tool

## Conclusion

This hybrid approach provides a pragmatic balance between backward compatibility and forward progress. Most mods should work without changes, while providing a clear migration path for mod authors who want to adopt the new package structure.

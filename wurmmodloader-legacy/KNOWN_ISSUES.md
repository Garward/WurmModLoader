# WurmModLoader Legacy Module - Known Issues

## Status: Functional with Hybrid Compatibility Layer (Phase 2.5)

The legacy module provides backward compatibility through a **hybrid approach**: thin proxies for public APIs combined with full implementations for complex internal classes. This allows most mods to work without modifications while providing a clear migration path.

## Solution Implemented

### Original Problem

Phase 2's package rename (org.gotti.wurmunlimited → com.garward.wurmmodloader) created type incompatibility issues for mods using the old package names.

### Hybrid Compatibility Strategy

**Tier 1 - Interface Extension** (✅ Implemented)
- Public API interfaces extend new implementations
- Examples: `WurmServerMod`, `ModQuestion`, `IntraRequest`, all listener interfaces
- Result: Full type compatibility through interface extension

**Tier 2 - Delegating Wrappers** (✅ Implemented)
- Utility classes delegate to new implementations
- Examples: `ModActions`, `ModQuestions`, `ModLoader`
- Result: Zero-overhead delegation to new code

**Tier 3 - Simple Extensions** (✅ Implemented)
- Builder and parser classes extend new implementations
- Examples: `ItemTemplateBuilder`, `CreatureTemplateBuilder`, `ActionEntryBuilder`
- Result: Full inheritance-based compatibility

**Tier 4 - Full Implementations** (✅ Retained)
- Complex internal classes keep original code
- Includes: Enums, classes with complex constructors, package-private classes
- Result: Complete backward compatibility for internal APIs

## Remaining Issues

### Minor: Enum Type Casting

**Issue**: `IdType` enum values may have casting issues in edge cases

**Example:**
```java
// This works:
IdType type = IdType.ITEMTEMPLATE;

// This may fail in mixed old/new code:
com.garward.wurmmodloader.modsupport.IdType newType =
    (com.garward.wurmmodloader.modsupport.IdType) oldType; // ClassCastException
```

**Workaround**: Use enum by name rather than direct casting
**Impact**: Low - rare in typical mod code

### Minor: Reflection on Package Names

**Issue**: Code using reflection to find classes by package name will need updates

**Example:**
```java
// This won't find legacy classes:
Class.forName("org.gotti.wurmunlimited.modloader.ModLoader")
```

**Workaround**: Update reflection code to use new package names
**Impact**: Low - uncommon pattern

## Testing Status

- ✅ Compilation: Legacy module compiles successfully with hybrid approach
- ⏳ Runtime Testing: Needs validation with real mods
- ⏳ Integration Testing: Requires mod ecosystem testing

## Migration Recommendations

### For Mod Authors

**Option 1: Continue Using Legacy API (Easiest)**
- No code changes required
- Update dependency to include legacy module
- Accept deprecated warnings

**Option 2: Migrate to New API (Recommended)**
- Update imports: `org.gotti.wurmunlimited.*` → `com.garward.wurmmodloader.*`
- Recompile mod
- Benefits: No deprecated warnings, better long-term support

### Migration Example

```java
// BEFORE
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

public class MyMod implements WurmServerMod {
    @Override
    public void preInit() {
        ModActions.init();
    }
}

// AFTER
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;

public class MyMod implements WurmServerMod {
    @Override
    public void preInit() {
        ModActions.init();
    }
}
```

## Benefits of Hybrid Approach

1. **Backward Compatibility**: Most mods work without changes
2. **Forward Progress**: New development uses clean package structure
3. **Clear Migration Path**: Mod authors can update incrementally
4. **Minimal Overhead**: Thin proxies have negligible performance impact
5. **Maintainability**: Full implementations only where truly necessary

## Documentation

See `COMPAT_LAYER_STATUS.md` for complete details on:
- Which classes use which compatibility strategy
- Full API compatibility guarantees
- Detailed migration guide
- Testing checklist

## Conclusion

The hybrid compatibility layer successfully resolves the package rename issues while maintaining backward compatibility for the vast majority of mod code. The approach balances pragmatism with long-term maintainability.

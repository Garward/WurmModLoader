# Legacy Module — Known Issues

The compat layer has been in production since 0.5.x and carries the full Ago-era mod corpus. The issues below are edge cases, not blockers.

## Enum casting between old and new packages

You can't directly cast an `org.gotti.wurmunlimited.modsupport.IdType` to `com.garward.wurmmodloader.modsupport.IdType` — Java enums can't be extended, so the legacy module keeps its own copy.

```java
// Works:
if (type == IdType.ITEMTEMPLATE) { ... }

// Fails with ClassCastException:
var newType = (com.garward.wurmmodloader.modsupport.IdType) oldType;
```

Compare by `name()` if you need to cross the boundary, or stay in one package.

Applies to: `IdType`, `ActionPropagation`, `TextStyle`, `ModVehicleBehaviour`.

## Reflection against hardcoded package names

Code that does `Class.forName("org.gotti...")` finds the legacy shim. Code that does `Class.forName("com.garward...")` finds the real implementation. Both load; they're just different classes from Java's POV.

If a mod reflectively walks one package expecting to find everything, it won't — move to the canonical `com.garward.wurmmodloader.*` packages.

## No other blockers

Everything else — mod lifecycle, `ModActions`/`ModItems`/`ModCreatures`, `ModQuestions`, `ModComm`, `ModIntraServer`, `ItemTemplateBuilder` / `CreatureTemplateBuilder` / `ActionEntryBuilder`, server listeners — runs unchanged.

## Migration recommendation

You don't have to migrate. But for new development, import from `com.garward.wurmmodloader.*` directly — it's a straight rename, no API changes:

```java
// Before
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

// After
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
```

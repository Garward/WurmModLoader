# WurmModLoader Legacy Compatibility Module

Keeps mods compiled against the original `org.gotti.wurmunlimited.*` WurmServerModLauncher running on modern WurmModLoader without recompiling.

> Swap the old modloader for the new one, drop old mods in, they still work.

## Status

In production since 0.5.x. Proven against the full Ago-era mod corpus. New `com.garward.wurmmodloader.*` namespace is canonical; `org.gotti.*` is compat-only and not recommended for new development.

## How it works

Hybrid layer — see [`COMPAT_LAYER_STATUS.md`](COMPAT_LAYER_STATUS.md) for the per-class breakdown:

- **Interface extension** — old interfaces extend their new counterparts. Old `WurmServerMod` *is a* new `WurmServerMod`.
- **Delegating wrappers** — old static utilities (`ModActions`, `ModQuestions`, `ModLoader`) call into the new implementations.
- **Simple extensions** — builders/parsers subclass the new classes.
- **Full original implementations** — enums, package-private helpers, classes with private/complex constructors, and the ModComm/ModIntraServer packet layer keep their original code for exact bytecode match.

## Usage

### Shipping mods

Old compiled mods just work — drop the jar in `mods/<name>/` and add a `.properties` file. No recompile, no import changes.

```java
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class MyMod implements WurmServerMod {
    @Override public void preInit() { ModActions.init(); }
}
```

### New development

Use the canonical namespace:

```java
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
```

The legacy module is available as a compile-time dep only if you explicitly need to port against old types.

## Distribution layout

The Runtime zip ships a single uber jar (`wurmmodloader-<version>.jar`) which bundles api/core/modsupport/legacy/cli. The SDK zip ships the module jars separately for modders who compile against a specific module.

In both cases the `org.gotti.*` classes live inside `wurmmodloader-legacy-*.jar` (SDK) or the uber jar (Runtime) — no separate legacy jar to ship.

## Known rough edges

See [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md).

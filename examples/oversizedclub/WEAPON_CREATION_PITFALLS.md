# Weapon Creation Pitfalls

Short list of the mistakes that bite most people building a custom weapon with
WurmModLoader. Every one of these has silently cost someone a day of debugging.

## 1. Missing `ITEM_TYPE_WEAPON`

If you set a damage type (`ITEM_TYPE_WEAPON_CRUSH` / `_SLASH` / `_PIERCE`) but
forget the base `ITEM_TYPE_WEAPON`, Wurm treats the item as an inventory object,
not a weapon. Symptoms: no attack animation, no skill gain, combat uses fists.

Always include **both**:

```java
.itemTypes(new short[] {
    // ...
    ItemTypes.ITEM_TYPE_WEAPON,       // base — required
    ItemTypes.ITEM_TYPE_WEAPON_CRUSH  // one damage type
})
```

## 2. Material item-type and `.material()` disagree

The `ITEM_TYPE_WOOD` / `_METAL` / `_STONE` flag in `itemTypes` must match whatever
you pass to `.material(...)`. Mismatches cause subtle breakage — crafting recipes
that should apply don't, or Wurm uses wrong fallback rules.

Wood weapon → `ITEM_TYPE_WOOD` + `Materials.MATERIAL_WOOD_*`.
Metal weapon → `ITEM_TYPE_METAL` + a metal `Materials.MATERIAL_*`.
Stone weapon → `ITEM_TYPE_STONE` + `Materials.MATERIAL_STONE`.

Change them together.

## 3. No `new Weapon(...)` registration on Armoury / DUSKombat servers

`ItemTemplateBuilder.combatDamage(...)` is only a **fallback**. Servers running
Armoury or DUSKombat-style combat mods look up a `Weapon` record by template id,
and if they don't find one the weapon deals zero damage and you'll see log lines
like `Weapon map does not contain entry for <template id>`.

Always register:

```java
new Weapon(templateId, damage, speed, critParam, reach, weightGroup, parryPct, skillPenalty);
```

Safe to include even on vanilla servers — they just ignore it.

## 4. Crit parameter is divided by 5.0

The `critParam` argument to `new Weapon(...)` is **not** the actual crit chance.
Wurm divides it by 5.0 internally:

| `critParam` | actual crit chance |
| --- | --- |
| `0.002f` | 0.04% (vanilla clubs) |
| `0.012f` | 0.24% |
| `0.02f`  | 0.4% (titan weapons) |
| `0.05f`  | 1.0% |

Setting `critParam = 0.5f` thinking you'll get 50% gets you 10%. Setting it to
`1.0f` saturates at 20%.

## 5. Source item must weigh more than the result

`CreationEntryCreator.createSimpleEntry(...)` errors out at craft time with
"too little material" if the source item weighs less than the item being made.

Result 12 kg → source must be ≥ 12 kg. A log (20+ kg) works; a shaft (1 kg) does
not. If you need a light source, split the craft into multiple steps.

## 6. JAR filename must not include a version

Deployment layout:

```
mods/<modname>/<modname>.jar
mods/<modname>.properties
```

The loader matches by folder / file name, so `myweapon-1.0.0.jar` will **not**
load; it has to be `myweapon.jar`. Gradle build scripts usually set
`archiveBaseName` to the mod name without the version for this reason.

## 7. Never `import com.wurmonline.*` from bytecode-patch code

In an event handler (the normal case), type-importing `com.wurmonline.*` classes
is fine — that's how you receive `Item`, `Creature`, etc.

But inside a `preInit()` bytecode patch you're running before those classes are
finished loading. Touching them by direct import triggers class-resolution order
issues and patches silently fail to apply. Use Javassist string-based references
(`classPool.get("com.wurmonline...")`) from patches.

## 8. Namespace strings must be unique

```java
new ItemTemplateBuilder("garward.oversizedclub")
```

The namespace is used to allocate a stable template id across restarts. If two
mods ship the same namespace string, one of them silently clobbers the other's
id. Prefix with your mod / author name (`garward.oversizedclub`,
`joedobo27.hotfoot`, etc.) and treat it as permanent — changing the namespace
after a server has been running with the old one orphans every existing
instance of the item in the database.

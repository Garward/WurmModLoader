# WurmModLoader - Project State Audit
Last updated: 2026-06-10 | v0.9.1

> Living document: what is done, what is rough, what is blocking release.
> Update as you audit/clean. External memory for low working memory + complex architecture.

---

## What This Project Actually Is

A complete, multi-module modloader for Wurm Unlimited (client + server), written from scratch
on top of a fork of Ago's WurmServerModLauncher. Not a patch. A full architecture replacement
with a clean API, event system, 57 bytecode patches, capability system, and legacy compat bridge.

What you are sitting on:
- Working modloader with 57 registered bytecode patches
- 98 API events across combat, creatures, items, actions, deity, UI, and more
- 4 actual showcase mods (powerscaling, soulboundgear, upgradetree, materialsystem)
- Legacy bridge so Ago-era mods still run without rewriting
- Client modloader plan and partial implementation
- The Ago modbase already half ported

---

## Module Map

| Module | Status | Purpose |
|--------|--------|---------|
| wurmmodloader-api | SOLID | Public API: events, interfaces, capability, bytecode contracts |
| wurmmodloader-core | MOSTLY SOLID | Boot, patching pipeline, event bus, legacy bridge, mod loading |
| wurmmodloader-modsupport | WORKS | Higher-level helpers: creature builders, BML, loot, vehicles |
| wurmmodloader-patcher | WORKS | Jar patcher / distribution installer |
| wurmmodloader-legacy | WORKS | Ago-era mod compatibility shim |
| mods/* | WIP | Bundled showcase mods |
| client modules | PARTIAL | Plan exists, partial impl, not release-blocking for server drop |

---

## What is Done and Working

### Boot and Mod Loading
- Full boot sequence: config -> class loading -> bytecode patching -> mod init
- ModLoader handles discovery, ordering, lifecycle
- DelegatedLauncher wraps server startup cleanly
- ProxyServerHook is the event dispatch backbone

### Bytecode Patching (57 patches)
Coverage across:
- Combat: Attack, CriticalHit, Damage, DualWield, SwingSpeed, ShieldCheck, OpportunityAttack, SpecialMove, WeaponUse, WeaponStatQuery
- Creatures: Spawn, Death, Examine, Position, Breed, VehicleMount
- Items: Drop, Damage, Trade, Container, Examine, Templates, Enchantment, MaterialBonus
- Player: Login, Logout, SkillLoss, Movement, SkillAdvance
- Server: Start, Shutdown, Poll, Config load
- Actions: Speed, Timing, Fatigue, ArrayBoundsCheck, RemoveActionsFinal
- World: CropGrowth, Harvest, DB (Creature/Deity/Structure)
- Misc: Communicator, ContainerVolume, MountEquip, PriestRestriction, Prayer, CommandReader

### Event System (98 events)
Clean hierarchy: CancellableEvent -> domain event classes
Categories: action, combat (shield/, weapon/ subcategories), creature, deity, item (material/ subcategory), player, server, skill, vehicle, world

### Capability System
Capability<T> + ICapabilityProvider - attach arbitrary data to any game object without collision

### Legacy Bridge
LegacyListenerBridge - Ago-era mods load and fire through the new system

### ModSupport Helpers
Creature template builder, BML helpers, loot tables, vehicle support, IdFactory, NamedIdParser

### Build System
Gradle multi-module, build-and-deploy.sh, distribution artifacts in build/

### Bundled Mods
| Mod | Java files | State |
|-----|-----------|-------|
| powerscaling | 23 | Most complete showcase mod |
| soulboundgear | 11 | Working |
| upgradetree | 8 | Working |
| materialsystem | 5 | Working |
| eventlister | 1 | Debug/dev tool |

---

## Known Rough Spots (Audit Targets)

### BLOCKING / MAJOR

1. DAMAGE FORMULA API - incomplete hook transparency
   - CombatDamagePatch hooks addWound() before calculation fires
   - You can intercept the final damage float but NOT the intermediate values
     (attacker skill contributions, defender armor calc, wound type modifiers, etc.)
   - Vanilla CombatEngine is a monolith with ~100-case switches, no clean method to wrap
   - Real fix: empirical logging pass to reverse-engineer the modifier chain,
     then expose as separate events or event fields
   - THIS IS THE MAIN BLOCKER for hooks feeling complete

2. Event coverage gaps (verify during audit)
   - Crafting outcomes, skill gain modifiers, action queue manipulation
   - Check against vanilla behaviors you want hookable

### CLEANUP / POLISH

3. Duplicate event structure
   - api/events/ contains both flat event files AND events/eventlogic/ sub-path
   - Decision needed: flatten it, or formalize the split (what is eventlogic for?)

4. TODO markers (6 files - check during audit)
   - ModLoader.java:224 - SteamVersion stub throws RuntimeException(Unimplemented)
   - DelegatedLauncher.java:214 - stub detection warning
   - 4 others in core - are these real gaps or just defensive comments?

5. .backup files in modsupport source tree
   - CreatureTemplateBuilder.java.backup, IdFactory.java.backup, etc.
   - Delete or move to git history

6. Architecture.MD ends with AI prompt text
   - Last line: 'Would you like me to make a short version too'
   - Remove before repo goes public

### NICE TO HAVE FOR RELEASE

7. Hook coverage reference doc
   - Architecture.MD is good on boot flow but weak on hooks reference
   - Mod authors need a table of what they can actually listen for/cancel

8. Ago migration guide (1 page)
   - Legacy bridge handles compat but people want to understand the delta

9. Client modloader - plan is solid, NOT required for server release
   - Decouple the launches for the initial drop

---

## Release Checklist

- [ ] Audit and resolve the 6 TODO files
- [ ] Clean .backup files from source tree
- [ ] Fix trailing AI text in Architecture.MD
- [ ] Decision on event directory structure (flatten or formalize)
- [ ] First pass at damage formula transparency problem
- [ ] Hook coverage reference doc (can be partial)
- [ ] Ago migration guide (1 page)
- [ ] README: what this is, why better than Ago, quickstart install
- [ ] Tag v1.0.0-rc1

---

## Why This Is Better Than Ago

Key points for the release announcement:
1. Client + server in one system - Ago is server-only
2. Event-based API - clean event.cancel(), typed event fields, no string hacking
3. Bytecode conflict keys - patches declare what they touch, loader detects conflicts early
4. Capability system - attach arbitrary data to any game object without collision
5. 57 patches already written - Ago users currently work around most of these by hand
6. Legacy compat - existing Ago mods load without rewriting
7. Actual module architecture - not a single monolith jar

---

## File Reference

WurmModLoader/
  Architecture.MD               boot flow and architecture overview (needs cleanup)
  Client-Modloader-Plan.md      detailed client-side plan
  CHANGELOG.md                  phases 0-5 history
  CODEBASE_CHEATSHEET.md        auto-generated class/method index (noisy but complete)
  PROJECT_STATE.md              THIS FILE
  build.gradle.kts              root gradle config
  settings.gradle               module declarations
  build-and-deploy.sh           dist build script
  wurmmodloader-api/            public API (events, interfaces, capability)
  wurmmodloader-core/           engine (boot, patches, event bus, mod loading)
  wurmmodloader-modsupport/     helper library for mod authors
  wurmmodloader-patcher/        jar patcher / installer
  wurmmodloader-legacy/         Ago compat shim
  mods/                         bundled showcase mods
    powerscaling/
    soulboundgear/
    upgradetree/
    materialsystem/
    eventlister/                debug tool

---

## Audit Log

Record what you find and fix during cleanup passes.

### 2026-06-10 - Initial state doc created
- Confirmed: 57 patches, 98 events, 5 bundled mods, 6 TODO files, .backup files in source
- Main blocker identified: damage formula API transparency
- ClawForge project created (id 5) for tracking

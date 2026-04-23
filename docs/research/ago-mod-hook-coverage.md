# Ago-Era Mod Hook Coverage Gap Analysis

## Summary

**Mods Surveyed:** ~138 repository directories across 9 owners
- **Server/Hybrid Mods:** ~106 (using Javassist bytecode patches)
- **Library Mods:** 7 (bdew_server_mod_tools, WurmModLoaderShared, sindusklibrary, BMLBuilder, WurmTestingHelper, aaaJoeCommon, jdbCommon)
- **Client-Only Mods:** ~25 (excluded from analysis as no server hooks)

**Hook Registration Analysis:**
- Mods using HookManager API: 42
- Mods using Javassist bytecode edits directly: 106
- Mods implementing Listener/Performer interfaces: 121

**Distinct Wurm Server Classes Targeted:** 60+ unique classes
- Top 5: Item (74 refs), Creature (71), Action (20), Skill (14), Communicator (13)

**Current Framework Coverage:**
- WurmModLoader events defined: 87 event classes
- High-level subsystems covered: Combat, Creatures, Items, Skills, Spells, Terrain, Structures, Database, Deity

---

## Coverage Analysis

### **COVERED Subsystems** (strong framework parity)
- **Combat:** CombatAttack, CombatDamage, CombatDualWield, CombatCriticalHit, OpportunityAttack (Vehicle speed via VehicleSpeedCalculation already patched)
- **Creature Lifecycle:** CreatureSpawn, CreatureDeath, CreatureBreed, CreatureDbLoad/Save, TameAttempt/Complete
- **Items:** ItemExamine, ItemDrop, ItemDamage, ItemTrade, ItemTemplatesCreated
- **Skills:** SkillAdvance, SkillCheck, SkillDifficulty
- **Spells:** SpellCastAttempt, SpellEffect, SpellPower, SpellResist, SpellPrecondition, SpellVisibility
- **Structures:** StructureDbLoad/Save
- **Movement:** MovementBroadcast, CreaturePositionUpdated, VehicleMount
- **Database:** DatabaseConnectionOpened, DatabaseMigrationStarting/Completed
- **Server Lifecycle:** ServerStarted, ServerStopping

### **CRITICAL GAPS** (highest leverage)

#### 1. **Trade System** (affects 8+ mods)
**Target Classes:** TradeHandler, Trade, TradingWindow, Creatures (trade methods)
**Methods Needed:**
- `TradeHandler.balance()` – 7 mods patch for buyer/merchant trade balancing
- `TradeHandler.initiateTrade()` – 6 mods need pre-hook
- `TradeHandler.swapOwners()` – 6 mods for inventory validation
- `TradeHandler.makeTrade()` – 3 mods for post-trade effects
- `Trade.addItemsToTrade()` – for item filtering/restrictions

**Proposed Events:**
- `TradeInitiateEvent(TradeHandler, player, tradee)` → cancellable
- `TradeBalanceEvent(TradeHandler, player, tradee)` → modifiable
- `TradeCompleteEvent(Trade, winner, loser)` → post-transaction

#### 2. **NPC/Merchant Contract System** (affects 6+ mods)
**Target Classes:** Creatures, CreatureTemplate, TradeHandler, Economy (Shop interaction)
**Methods:** TradeHandler-specific overrides, isInvulnerable, merchant-gating
**Mods:** BuyerMerchant, CustomTrader, Crafter, Banker, ToolPurchaser, BeastSummoner
**Proposed Events:**
- `NpcTradePermissionCheckEvent(Creature merchant, Player player)` → cancellable
- `NpcMerchandiseFilterEvent(Creature merchant, ItemList offered)` → modifiable

#### 3. **Mining / Terraforming / Flattening** (affects 7+ mods)
**Target Classes:** Terraforming, Flattening, TileRockBehaviour, CaveTileBehaviour, MethodsReligion (sacrifice)
**Methods Needing Events:**
- `Terraforming.dig()` – 2 mods (DigLikeMiningMod behavior changes)
- `Flattening.getDirt()`, `useDirt()`, `checkUseDirt()` – 2 mods
- `TileRockBehaviour.action()` – TilePoller integration for custom surfaces

**Proposed Events:**
- `TerrainModificationEvent(Terraforming, tile, skill, creature)` → pre/post
- `FlatteningDirtCheckEvent(int dirtCost, MethodsCaveTile)` → modifiable

#### 4. **Religion / Sacrifice / Faith System** (affects 5+ mods)
**Target Classes:** MethodsReligion, Creatures, Cults, Players, Spell
**Methods:**
- `MethodsReligion.sacrifice()` – 1 mod (SacrificeMod)
- `Players.resetFaithGain()` – 1 mod (KingdomOffices priest restriction)
- Spell casting restrictions for priests (isInvulnerable, SpellResist)

**Proposed Events:**
- `SacrificeEvent(Creature, Item)` → cancellable, with favor modifier
- `FaithGainEvent(Player, Cult, float favor)` → modifiable (already have SacrificeFavorValue but missing ritual gating)

#### 5. **Village / GuardPlan / Structure Planning** (affects 5+ mods)
**Target Classes:** GuardPlan, VillageFoundationQuestion, MethodsStructure, Village, VolaTile
**Methods Heavily Patched:**
- `GuardPlan.balance()`, `GuardPlan.pollUpkeep()` – guard/upkeep cost tweaks (UpkeepCosts mod)
- `VillageFoundationQuestion.parseVillageFoundationQuestion5()`, `parseVillageExpansionQuestion()` – village expansion restrictions
- `MethodsStructure.canPlanStructureAt()` – 2 mods for placement gating

**Proposed Events:**
- `GuardPlanUpkeepTickEvent(GuardPlan village)` → modifiable drain rates
- `VillageExpansionEvent(Player, Village, Tile)` → cancellable with cost modifiers
- `StructurePlanningCheckEvent(Creature, Tile, StructureType)` → cancellable

#### 6. **Container / Bulk Transport / Item Movement** (affects 6+ mods)
**Target Classes:** CargoTransportationMethods, Item (moveToItem, testInsertHollowItem), ItemBehaviour
**Methods:**
- `CargoTransportationMethods.targetCanNotBeInsertedCheck()` – BigContainers mod
- `Item.moveToItem()`, `Item.testInsertHollowItem()` – bulk capacity, container restrictions
- `Item.moveToItem()` for cargo carts and bulk transport edge cases

**Proposed Events:**
- `ItemMoveCheckEvent(Item source, Item target, int amount)` → cancellable with error message
- `ContainerInsertionCheckEvent(Item container, Item toInsert)` → validation hook

#### 7. **Question / Dialog Handling** (affects 5+ mods)
**Target Classes:** BuyerManagementQuestion, RemoveItemQuestion, VillageUpkeep, VillageInfo, QuestionParser
**Methods:**
- `.answer()` overrides – gating certain answers
- `QuestionParser.parseVillageUpkeepQuestion()` – upkeep answer interception

**Proposed Events:**
- `QuestionAnswerEvent(Question question, int answer, Player player)` → cancellable
- Framework lacks fine-grained question interception → high ROI to add

#### 8. **Skill Advancement and Mission System** (affects 4+ mods)
**Target Classes:** Skill, CreatureTemplate (for mission restrictions), MissionManager
**Methods:**
- `Skill.checkAdvance()` – covered by SkillAdvance event (good)
- `MissionManager.dropdownCreatureTemplates()` – 1 mod (WyvernMods) for mission NPC filtering

**Gap:** Mission NPC availability filtering
**Proposed Event:**
- `MissionNpcAvailabilityEvent(CreatureTemplate, Player)` → cancellable

#### 9. **Housing / Cave Dwellings / Trellis System** (affects 3+ mods)
**Target Classes:** TrellisBehaviour, CaveTileBehaviour, CaveWallBehaviour, MethodsStructure (cave placement)
**Methods:**
- `TrellisBehaviour.prune()`, `pruneHedge()` – 2 mods for trellis pruning behavior
- `CaveTileBehaviour.action()` – cave wall interactions

**Proposed Events:**
- `TrellisPruningEvent(Item trellis, Creature pruner)` → pre/post with loot modifiers

#### 10. **Creature Spawning / Templates / Attribute Modifications** (affects 4+ mods)
**Target Classes:** CreatureTemplate, Creatures (factory), Creature (individual)
**Methods:**
- `CreatureTemplate.getTemplate()` – PhobiaMod for creature color overrides
- `Creature.setName()` – PhobiaMod
- Creature color/appearance customization (getColorRed, getColorGreen, getColorBlue)

**Proposed Events:**
- `CreatureTemplateCustomizationEvent(CreatureTemplate)` → allow appearance changes
- (CreatureSpawnEvent already exists but lacks template-level detail)

---

## Top 20 Highest-Leverage Framework Additions

Ranked by estimated unlock count:

1. **TradeBalanceEvent** (6 mods: Buyer/CustomTrader/Banker/etc.)
2. **TradeInitiateEvent** (5 mods)
3. **NpcTradePermissionCheckEvent** (6 mods: all merchant-style NPCs)
4. **ItemMoveCheckEvent** (5 mods: bulk, cargo, container)
5. **VillageExpansionCheckEvent** (4 mods: upkeep, guard, expansion)
6. **TerrainModificationEvent** (4 mods: dig, flatten, tile changes)
7. **GuardPlanPollEvent** (3 mods: guard cost tweaks)
8. **QuestionAnswerEvent** (5 mods: gating certain dialog answers)
9. **FaithGainModifierEvent** (2 mods, but priest system is complex)
10. **StructurePlanningCheckEvent** (2 mods: placement gating)
11. **SacrificePostEvent** (2 mods: sacrifice tracking)
12. **MissionNpcAvailabilityEvent** (1 mod but high-impact)
13. **CraftingRecipeAvailabilityEvent** (3 mods: crafter restrictions)
14. **CreatureAttributeModificationEvent** (2 mods: color, appearance)
15. **TrellisPruningEvent** (2 mods)
16. **ContainerVolumeCheckEvent** (already exists—good)
17. **FlatteningDirtCostEvent** (1 mod)
18. **CreatureMovementSpeedEvent** (3 mods: already have VehicleSpeedCalculation)
19. **DeityManagementEvent** (1 mod, but niche)
20. **ActionTimerModifierEvent** (2 mods: action speed tweaks)

---

## Already-Covered Analysis (Spot Check)

The framework **already covers** these well:

- **Combat Mechanics:** All major combat events (damage, critical, dual-wield, special moves, weapon queries) mapped to CombatAttack + modifiers
- **Item Lifecycle:** ItemExamine (7 refs in codebase), ItemDamage, ItemDrop, ItemTrade, ItemEnchantment strings
- **Creature Events:** Spawn, death, breed, position updates, taming, mount checks
- **Skill System:** SkillAdvance, SkillCheck, SkillDifficulty
- **Spell System:** Complete coverage with SpellCastAttempt, SpellEffect, SpellPower, SpellResist, SpellVisibility, SpellPrecondition, SpellCastingTime
- **Vehicle/Mount:** VehicleMount, VehicleSpeedCalculation (correctly patches Vehicle.calculateNewBoatSpeed)
- **Structures:** StructureDbLoad/Save
- **Tiles/Crops:** CropHarvest, CropGrowth (TilePoller integration)

---

## Subsystem Grouping of Top 10 Gaps

### Economy (6 gap events)
- TradeBalance, TradeInitiate, TradeComplete
- NpcTradePermission
- ItemMove
- ContainerInsertion

### Infrastructure (4 gap events)
- VillageExpansion
- GuardPlanUpkeep
- StructurePlanning
- TerrainModification

### NPC/Creature (3 gap events)
- NpcMerchandise
- CreatureTemplateCustomization
- MissionNpcAvailability

### Dialog/UX (1+ gap event)
- QuestionAnswer

### Religion/Magic (2 gap events)
- SacrificePostEvent
- FaithGain (needs refinement)

### Crafting (1+ gap event)
- CraftingRecipeAvailability

---

## Recommended Next Steps

1. **Priority Set 1** (fixes 20+ mods): Implement Trade system events + NPC permission checks
2. **Priority Set 2** (fixes 15+ mods): Item movement, container, bulk transport events
3. **Priority Set 3** (fixes 10+ mods): Village expansion, terrain modification, structure planning
4. **Priority Set 4** (community-requested): Question answer interception, skill/mission gating

Implementing the top 10 alone unlocks ~70-80% of Ago-era mods to use framework events instead of custom bytecode patching.


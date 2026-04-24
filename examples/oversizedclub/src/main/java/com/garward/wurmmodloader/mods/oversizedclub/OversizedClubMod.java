package com.garward.wurmmodloader.mods.oversizedclub;

import com.garward.wurmmodloader.api.capability.ICapabilityProvider;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.combat.CombatCriticalHitEvent;
import com.garward.wurmmodloader.api.events.combat.OpportunityAttackEvent;
import com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent;
import com.garward.wurmmodloader.api.events.item.ItemExamineEvent;
import com.garward.wurmmodloader.api.events.item.ItemTemplatesCreatedEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.mods.oversizedclub.capability.ItemLevel;
import com.garward.wurmmodloader.mods.oversizedclub.capability.ItemLevelCapability;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.combat.Weapon;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.CreationCategories;
import com.wurmonline.server.items.CreationEntryCreator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.server.items.Materials;
import com.wurmonline.server.skills.SkillList;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reference tutorial mod: adds a custom two-handed weapon ("Oversized Club") and
 * demonstrates every moving part a typical gameplay mod touches:
 *
 * <ul>
 *   <li>Registering a custom {@link ItemTemplate} via {@link ItemTemplateBuilder}</li>
 *   <li>Registering a {@link Weapon} so Armoury / DUSKombat-style combat mods pick it up</li>
 *   <li>Adding a crafting recipe via {@link CreationEntryCreator}</li>
 *   <li>Registering a {@link com.garward.wurmmodloader.api.capability.Capability}
 *       for persistent per-item data (level / XP)</li>
 *   <li>Subscribing to framework events with {@code @SubscribeEvent}:
 *       item templates created, server started, capability registration,
 *       item examine, weapon stat query, critical hit, opportunity attack</li>
 * </ul>
 *
 * <p>Weapon numbers are deliberately tuned on the strong side so the event hooks
 * have something visible to modify. See {@code WEAPON_CREATION_PITFALLS.md} in this
 * folder for the short list of gotchas that trip up most people building custom
 * weapons.</p>
 *
 * @see com.garward.wurmmodloader.modsupport.ItemTemplateBuilder
 * @see com.wurmonline.server.items.CreationEntryCreator
 * @see com.wurmonline.server.combat.Weapon
 */
public class OversizedClubMod implements WurmServerMod {

    private static final Logger logger = Logger.getLogger(OversizedClubMod.class.getName());

    private static final double BASE_CLUB_DAMAGE = 19.0d;
    private static final double DAMAGE_PER_LEVEL = 0.5d;
    private static final double MAX_LEVEL_DAMAGE_BONUS = 19.0d; // caps the level scaling at +100% of base

    private static int oversizedClubTemplateId = -1;

    // ─────────────────────────────────────────────────────────────────────────
    // Item template + weapon registration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fires after vanilla item templates have been created. Safe point to:
     * copy icons from vanilla templates, reference {@link ItemList} constants,
     * and register recipes that use vanilla items as ingredients.
     */
    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        logger.info("Creating Oversized Club item template...");

        try {
            // STEP 1: Copy the icon from an existing vanilla template rather than
            // hardcoding an icon number. If Wurm ever renumbers icons internally,
            // this keeps working.
            //
            // Other common weapon icons to copy from:
            //   Clubs:  ItemList.clubHuge, ItemList.club
            //   Swords: ItemList.swordLong, ItemList.swordShort, ItemList.swordGreat
            //   Axes:   ItemList.hatchet, ItemList.axe, ItemList.axeHuge
            //   Spears: ItemList.spearLong, ItemList.spearSteel
            ItemTemplate hugeClubTemplate = ItemTemplateFactory.getInstance().getTemplate(ItemList.clubHuge);
            short hugeClubIcon = hugeClubTemplate.getImageNumber();

            // STEP 2: Build the template. The namespace string ("garward.oversizedclub")
            // must be globally unique — prefix it with your mod / author name.
            ItemTemplate template = new ItemTemplateBuilder("garward.oversizedclub")
                    .name("oversized club", "oversized clubs",
                          "A massive, crudely made club of enormous proportions. " +
                          "It deals devastating damage but is extremely slow to swing. " +
                          "Only the strongest warriors can wield this effectively.")

                    // Visuals: reuse an existing Wurm model so no client-side files are needed.
                    //   Clubs:  model.weapon.club., model.weapon.club.huge.
                    //   Swords: model.weapon.sword.long., model.weapon.sword.short.
                    //   Axes:   model.weapon.axe., model.weapon.axe.huge.
                    //   Spears: model.weapon.spear.
                    .modelName("model.weapon.club.huge.")
                    .imageNumber(hugeClubIcon)
                    .behaviourType((short) 35) // standard weapon

                    // Item types control how Wurm classifies the item. The last entry picks
                    // the damage type — every weapon needs ITEM_TYPE_WEAPON plus ONE of:
                    //
                    //   ITEM_TYPE_WEAPON_CRUSH  — clubs, mauls, hammers   (best vs armour)
                    //   ITEM_TYPE_WEAPON_SLASH  — swords, axes            (best vs cloth)
                    //   ITEM_TYPE_WEAPON_PIERCE — spears, knives          (best vs leather)
                    //
                    // The material item-type (ITEM_TYPE_WOOD / _METAL / _STONE) must match
                    // whatever .material() is set to further down.
                    .itemTypes(new short[] {
                        ItemTypes.ITEM_TYPE_NAMED,
                        ItemTypes.ITEM_TYPE_REPAIRABLE,
                        ItemTypes.ITEM_TYPE_WOOD,
                        ItemTypes.ITEM_TYPE_WEAPON,
                        ItemTypes.ITEM_TYPE_WEAPON_CRUSH
                    })

                    .combatDamage(40) // fallback used only when no Weapon() registration exists
                    .primarySkill(SkillList.CLUB_HUGE)
                    .bodySpaces(MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY) // let Wurm decide

                    // Weight affects stamina drain, crafting source-vs-result weight checks,
                    // and inventory grid sizing.
                    .weightGrams(12000)
                    .dimensions(60, 300, 60)
                    .difficulty(40.0f)

                    // Material + ITEM_TYPE above must agree. To switch to steel:
                    //   .material(Materials.MATERIAL_STEEL)  +  ITEM_TYPE_METAL in itemTypes
                    //   + update the crafting recipe below to a metal source (e.g. steelLump)
                    .material(Materials.MATERIAL_WOOD_BIRCH)
                    .decayTime(Long.MAX_VALUE)

                    .value(15000)

                    .build();

            oversizedClubTemplateId = template.getTemplateId();

            // STEP 3: Register a Weapon record. The template's combatDamage() alone
            // is only a fallback — servers running Armoury / DUSKombat look up the
            // Weapon entry and ignore the fallback.
            //
            // Constructor: (templateId, damage, speed, critParam, reach, weightGroup, parryPct, skillPenalty)
            //
            //   damage       base damage multiplier  (vanilla club 8.3, huge axe 14.2)
            //   speed        seconds between swings  (vanilla club 4.5, huge axe 5.9)
            //   critParam    this value is divided by 5.0 internally
            //                — 0.002  → 0.04% crit (clubs)
            //                — 0.02   → 0.4%  crit (titan weapons)
            //                — 0.05   → 1.0%  crit
            //   reach        3 = standard melee, 5 = polearm
            //   weightGroup  1=very light, 3=medium, 4=heavy, 5=very heavy
            //   parryPct     0.0 – 1.0
            //   skillPenalty 0.0 = none, 0.5 = moderate (clubs, knuckles)
            //
            // Reference values from WyvernMods:
            //   Regular club : (8.3f , 4.5f , 0.002f, 3, 3, 0.4f, 0.5d)
            //   Huge axe     : (14.2f, 5.9f , 0.012f, 4, 4, 0.6f, 0.0d)
            //   Titan weapons: (11f  , 5f   , 0.02f , 4, 4, 1.0f, 0.0d)
            new Weapon(
                    oversizedClubTemplateId,
                    (float) BASE_CLUB_DAMAGE, // 19.0 — intentionally high so level hooks are visible
                    8.0f,                     // very slow — 19.0 / 8.0 ≈ 2.38 DPS, close to huge axe's 2.41
                    0.002f,                   // 0.04% actual crit; the critical-hit event hook boosts this at level
                    2,                        // short reach — thematic, you have to close the gap
                    5,                        // weight group 5 — heavy
                    0.3f,
                    0.0d
            );

            // Material modifiers on Armoury / DUSKombat servers:
            //
            //   final_damage = base_damage × material_modifier × quality × combat_multipliers
            //
            // Typical material modifiers from Armoury.properties:
            //   Steel        1.03x damage
            //   Adamantine   1.05x armour damage
            //   Glimmersteel 0.95x parry (more parries)
            //   Bronze       0.98x speed (faster swings)
            //   Wood / iron  1.00x (no modifier)
            //
            // Worked example for this weapon on a DUSKombat + Armoury server:
            //   base 19.0 × wood 1.0 × Q100 1.0 × PvE 1.0 ≈ ~19000 end-damage.
            //   Swap to steel and it becomes 19.0 × 1.03 ≈ 19.57 base, scaling from there.
            //
            // Server admins can change these multipliers via Armoury.properties, so
            // numbers in your own tutorial comments should say "typical" not "exact".

            // STEP 4: Crafting recipe — LOG + carving knife → oversized club.
            //
            // Pitfall: the source item must weigh MORE than the result or Wurm
            // throws "too little material". The result here is 12kg; a log is 20kg+.
            //
            // To restrict to a single material, the clean approach is to skip
            // a generic recipe and register one specific recipe per allowed source.
            // For example, a steel-only version would use ItemList.steelLump as source
            // and SkillList.SMITHING_WEAPON_HEADS as the skill, with ItemList.hammerMetal
            // as the tool. Add more createSimpleEntry() calls to allow additional
            // source materials.
            CreationEntryCreator.createSimpleEntry(
                    SkillList.CARPENTRY,
                    ItemList.knifeCarving,     // tool (must be activated)
                    ItemList.log,              // source material
                    oversizedClubTemplateId,   // result
                    false,                     // useTemplateWeight
                    true,                      // destroyTarget
                    0.0f,                      // skillGainMultiplier (0 = default)
                    false,                     // allowOnWater
                    false,                     // allowOnSurface
                    CreationCategories.WEAPONS
            );

            logger.log(Level.INFO,
                    "Oversized Club registered (template id {0}, base damage {1}, swing {2}s).",
                    new Object[] { oversizedClubTemplateId, BASE_CLUB_DAMAGE, 8.0f });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create Oversized Club item template", e);
        }
    }

    /**
     * Logged once the server is fully up. Good place to surface admin-visible info
     * (recipes, dependencies, config state) but keep it short — tutorial mods should
     * model good log citizenship.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("Oversized Club ready: craft with log + carving knife (40 carpentry). " +
                    "Uses the huge-club model and huge-club skill.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Capabilities — persistent per-item data
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Capabilities attach custom data to items / players / creatures / tiles without
     * the mod having to manage its own database. Register them during this event;
     * the framework handles persistence and lazy loading on first access.
     */
    @SubscribeEvent
    public void onCapabilityRegistration(CapabilityRegistrationEvent event) {
        event.registerItemCapability(ItemLevelCapability.INSTANCE);
        logger.info("ItemLevel capability registered.");
    }

    /**
     * Helper: pull the ItemLevel capability off an item. Returns null if the item
     * isn't capability-aware or the lookup fails. Centralised because the combat
     * hooks below all need the same lookup.
     */
    private ItemLevel getItemLevelData(Item item) {
        if (!(item instanceof ICapabilityProvider)) {
            return null;
        }
        try {
            ICapabilityProvider provider = (ICapabilityProvider) item;
            return (ItemLevel) provider.getCapability(ItemLevelCapability.INSTANCE);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to read ItemLevel capability", e);
            return null;
        }
    }

    private double calculateDamageBonus(int level) {
        return Math.min(level * DAMAGE_PER_LEVEL, MAX_LEVEL_DAMAGE_BONUS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gameplay hooks
    // ─────────────────────────────────────────────────────────────────────────

    /** Adds level / damage-bonus lines to the examine text for oversized clubs. */
    @SubscribeEvent
    public void onItemExamine(ItemExamineEvent event) {
        Item item = event.getItem();
        if (item.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(item);
        if (itemLevel == null) {
            return;
        }

        event.addDescription("\n[" + itemLevel + "]");

        double bonusDamage = calculateDamageBonus(itemLevel.getLevel());
        if (bonusDamage > 0) {
            double percent = (bonusDamage / BASE_CLUB_DAMAGE) * 100.0d;
            event.addDescription(String.format(Locale.US,
                    "\n[+%.0f%% DMG from level (%.1f bonus damage)]", percent, bonusDamage));
        }
    }

    /**
     * Fires whenever Wurm asks for a weapon stat (damage, swing speed, parry, etc.).
     * Because the event carries the actual {@link Item} instance, stats can scale
     * with capability data, material, quality — anything the item knows about itself.
     */
    @SubscribeEvent
    public void onWeaponStatQuery(WeaponStatQueryEvent event) {
        Item item = event.getItemTemplate();
        if (item == null || item.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(item);
        if (itemLevel == null) {
            return;
        }

        int level = itemLevel.getLevel();
        switch (event.getStatType()) {
            case DAMAGE:
                event.setValue(event.getValue() + calculateDamageBonus(level));
                break;
            case SPEED:
                // Veterans handle the weight better — shave up to a few seconds off the swing.
                event.setValue(Math.max(2.5d, event.getValue() - (level * 0.1d)));
                break;
            case PARRY:
                event.setValue(event.getValue() + Math.min(0.25d, level * 0.01d));
                break;
            default:
                break;
        }
    }

    /** Boosts the crit chance of a levelled oversized club, up to +25%. */
    @SubscribeEvent
    public void onCombatCriticalHit(CombatCriticalHitEvent event) {
        Item weapon = event.getWeapon();
        if (weapon == null || weapon.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(weapon);
        if (itemLevel == null) {
            return;
        }

        float bonus = Math.min(0.25f, itemLevel.getLevel() * 0.01f);
        event.setCritChance(Math.min(1.0f, event.getCritChance() + bonus));
    }

    /**
     * Two different behaviours at low vs high level:
     * <ul>
     *   <li>Level &lt; 3 — cancel repeated opportunity swings (novices whiff them)</li>
     *   <li>Higher levels — shorten the recovery window so the next swing lands sooner</li>
     * </ul>
     */
    @SubscribeEvent
    public void onOpportunityAttack(OpportunityAttackEvent event) {
        Creature defender = event.getDefender();
        if (defender == null) {
            return;
        }

        Item weapon = defender.getPrimWeapon();
        if (weapon == null || weapon.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(weapon);
        if (itemLevel == null) {
            return;
        }

        int level = itemLevel.getLevel();

        if (level < 3 && event.getOpportunityCounter() > 0) {
            event.setCancelled(true);
            return;
        }

        float hasteBonus = Math.min(1.0f, level * 0.05f);
        event.setActionCounter(Math.max(1.5f, event.getActionCounter() - hasteBonus));
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}

package com.garward.wurmmodloader.mods.oversizedclub;

import com.garward.wurmmodloader.api.capability.ICapabilityProvider;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.ModQueryEvent;
import com.garward.wurmmodloader.api.events.combat.CombatAttackEvent;
import com.garward.wurmmodloader.api.events.combat.CombatCriticalHitEvent;
import com.garward.wurmmodloader.api.events.combat.OpportunityAttackEvent;
import com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent;
import com.garward.wurmmodloader.api.events.item.ItemExamineEvent;
import com.garward.wurmmodloader.api.events.item.ItemTemplatesCreatedEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.mods.oversizedclub.capability.ItemLevel;
import com.garward.wurmmodloader.mods.oversizedclub.capability.ItemLevelCapability;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.Reloadable;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import com.garward.wurmmodloader.modsupport.combat.WeaponRegistry;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.combat.Weapon;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.CreationCategories;
import com.wurmonline.server.items.CreationEntryCreator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.Items;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.server.items.Materials;
import com.wurmonline.server.skills.SkillList;

import java.util.Locale;
import java.util.Properties;
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
 *   <li>Optional cross-mod integration (DUSKombat) via the generic
 *       {@code ModQueryEvent} bus — no build-time dependency, runtime-detected,
 *       and gracefully degrades when the other mod isn't installed. See the
 *       tutorial block around {@code isDuskombatPresent()} and
 *       {@code onDUSKombatTooltip}.</li>
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
public class OversizedClubMod implements WurmServerMod, Configurable, Reloadable {

    private static final Logger logger = Logger.getLogger(OversizedClubMod.class.getName());

    // Default balance values. configure() overwrites these from mod.config,
    // and #reloadmods re-fires configure() + onReload() so live GM edits take
    // effect without a restart. onReload() calls WeaponRegistry.reregister()
    // to overwrite the existing Weapon() entry for this template id, so both
    // examine-display math AND dealt combat damage refresh immediately.
    private double baseClubDamage = 19.0d;
    private double damagePerLevel = 0.5d;
    private double maxLevelDamageBonus = 19.0d;
    private double weaponSpeed = 8.0d;
    private double critChance = 0.002d;

    private static int oversizedClubTemplateId = -1;

    // ─────────────────────────────────────────────────────────────────────────
    // TUTORIAL: Optional integration with another mod, without compiling against it
    // ─────────────────────────────────────────────────────────────────────────
    //
    // Goal: light up a "nicer" feature when DUSKombat is installed, fall back
    // to a plain version when it isn't — without adding a build dependency on
    // DUSKombat, and without forcing users to install it.
    //
    // The trick is two framework primitives:
    //
    //   1. `Class.forName("...")` at runtime to detect the optional mod.
    //      Cached in a Boolean so the lookup runs once. No compile-time reference
    //      to any DUSKombat type ever appears in this file.
    //
    //   2. Generic `ModQueryEvent` / `ModActionEvent` buses to talk across mods.
    //      DUSKombat fires a `ModQueryEvent("duskombat:item_tooltip")` when it
    //      builds weapon tooltips. Anyone subscribed sees it — payload is just
    //      a string-keyed map, so neither side needs the other's types.
    //
    // See {@link #isDuskombatPresent()} and {@link #onDUSKombatTooltip} below
    // for the working pair. {@link #onItemExamine} demonstrates the other half
    // of the pattern: step aside when the richer integration is available, so
    // the player doesn't see the same bonus printed twice.
    //
    // This is how you build *optional enhancements* to other mods — the framework
    // lets mods see each other and adapt, without turning their release artifacts
    // into hard-linked dependencies.
    private Boolean duskombatPresent;

    private boolean isDuskombatPresent() {
        if (duskombatPresent == null) {
            try {
                Class.forName("mod.piddagoras.duskombat.DamageMethods");
                duskombatPresent = Boolean.TRUE;
            } catch (ClassNotFoundException e) {
                duskombatPresent = Boolean.FALSE;
            }
        }
        return duskombatPresent.booleanValue();
    }

    @Override
    public void configure(Properties properties) {
        this.baseClubDamage = readDouble(properties, "damageBase", this.baseClubDamage);
        this.damagePerLevel = readDouble(properties, "damagePerLevel", this.damagePerLevel);
        this.maxLevelDamageBonus = readDouble(properties, "maxLevelDamageBonus", this.maxLevelDamageBonus);
        this.weaponSpeed = readDouble(properties, "weaponSpeed", this.weaponSpeed);
        this.critChance = readDouble(properties, "critChance", this.critChance);
        logger.log(Level.INFO,
                "OversizedClub configured: damageBase={0}, damagePerLevel={1}, maxLevelDamageBonus={2}, speed={3}, crit={4}",
                new Object[] { baseClubDamage, damagePerLevel, maxLevelDamageBonus, weaponSpeed, critChance });
    }

    /**
     * Called after configure() on #reloadmods. Re-registers the Weapon() so
     * dealt combat damage (not just examine text) picks up the new config
     * values immediately. Skipped on first boot — the template doesn't
     * exist yet until onItemTemplatesCreated() runs.
     */
    @Override
    public void onReload() {
        if (oversizedClubTemplateId < 0) {
            return;
        }
        WeaponRegistry.reregister(
                oversizedClubTemplateId,
                (float) baseClubDamage,
                (float) weaponSpeed,
                (float) critChance,
                2, 5, 0.3f, 0.0d);
        logger.info("OversizedClub: Weapon() re-registered — new stats apply to the next swing.");
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try { return Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) {
            logger.warning("OversizedClub: invalid " + key + "=" + raw + ", using " + fallback);
            return fallback;
        }
    }

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
            // STEP 1: Pick the inventory icon. Two ways:
            //
            //   (a) Copy a vanilla icon by id — robust against icon renumbering:
            //         short hugeClubIcon = ItemTemplateFactory.getInstance()
            //             .getTemplate(ItemList.clubHuge).getImageNumber();
            //         ...
            //         .imageNumber(hugeClubIcon)
            //
            //   (b) Ship your own 32×32 PNG and reference it by string id —
            //         drop the PNG at mods/oversizedclub/icons/club.png and the
            //         framework's boot-time scanner allocates a wire-protocol
            //         short, syncs the registry to clients, and serves the file
            //         through the framework-icons server-pack. Modded clients
            //         render the custom art; vanilla clients quietly fall back
            //         to icon 0.
            //
            // We use (b) here so the example demonstrates the dynamic-icon
            // pipeline end-to-end. The string id format is "<modname>:<relpath
            // without .png>" — directory layout determines the id.
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
                    .icon("oversizedclub:club")
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
            // Using WeaponRegistry (thin wrapper around `new Weapon(...)`) so the
            // same call site can be re-used on #reloadmods via onReload().
            WeaponRegistry.register(
                    oversizedClubTemplateId,
                    (float) baseClubDamage,  // config-backed; live-reloadable
                    (float) weaponSpeed,     // config-backed; live-reloadable
                    (float) critChance,      // config-backed; live-reloadable
                    2,                       // short reach — thematic
                    5,                       // weight group 5 — heavy
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
                    new Object[] { oversizedClubTemplateId, baseClubDamage, weaponSpeed });

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
        return Math.min(level * damagePerLevel, maxLevelDamageBonus);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gameplay hooks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds level / damage-bonus lines to the examine text for oversized clubs.
     *
     * <p>When DUSKombat is installed, {@link #onDUSKombatTooltip} pushes the same
     * info through DUSKombat's native tooltip channel instead — so this handler
     * steps aside to prevent a duplicate display. The early-return is the other
     * half of the cross-mod-adaptation pattern documented above.</p>
     */
    @SubscribeEvent
    public void onItemExamine(ItemExamineEvent event) {
        Item item = event.getItem();
        if (item.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        // DUSKombat path takes over via onDUSKombatTooltip — avoid a duplicate line.
        if (isDuskombatPresent()) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(item);
        if (itemLevel == null) {
            return;
        }

        event.addDescription("\n[" + itemLevel + "]");

        double bonusDamage = calculateDamageBonus(itemLevel.getLevel());
        if (bonusDamage > 0) {
            double percent = (bonusDamage / baseClubDamage) * 100.0d;
            event.addDescription(String.format(Locale.US,
                    "\n[+%.0f%% DMG from level (%.1f bonus damage)]", percent, bonusDamage));
        }
    }

    /**
     * TUTORIAL: optional cross-mod integration via a generic event bus.
     *
     * <p>DUSKombat fires {@code ModQueryEvent("duskombat:item_tooltip")} when it
     * builds weapon tooltips. We subscribe without knowing anything about
     * DUSKombat's types — payload is a string-keyed map, so the event type
     * itself is all the handshake we need.</p>
     *
     * <p>If DUSKombat isn't installed, this handler simply never fires (the
     * event type never appears on the bus), and {@link #onItemExamine}
     * handles the fallback display.</p>
     */
    @SubscribeEvent
    public void onDUSKombatTooltip(ModQueryEvent event) {
        if (!"duskombat:item_tooltip".equals(event.getEventType())) {
            return;
        }

        // DUSKombat's tooltip payload (see DUSKombat/ItemInfo.java:149+):
        //   itemId     (long)    — target item's wurmId
        //   playerId   (long)    — examining player's wurmId
        //   isWeapon   (boolean) — true if target is a weapon
        //   isArmour   (boolean) — true if target is armour
        //   baseDamage (int)     — weapons only
        //   damageType (String)  — weapons only, SLASH / PIERCE / CRUSH / UNARMED
        if (!event.getBoolean("isWeapon")) {
            return;
        }

        Item item;
        try {
            item = Items.getItem(event.getLong("itemId"));
        } catch (Exception e) {
            return;
        }
        if (item.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(item);
        if (itemLevel == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        java.util.List<String> lines = (java.util.List<String>) event.get("tooltipLines");
        if (lines == null) {
            lines = new java.util.ArrayList<>();
        }
        lines.add("Oversized Club: " + itemLevel);
        double bonusDamage = calculateDamageBonus(itemLevel.getLevel());
        if (bonusDamage > 0) {
            double percent = (bonusDamage / baseClubDamage) * 100.0d;
            lines.add(String.format(Locale.US,
                    "  Level Bonus: +%.0f%% DMG (%.1f bonus damage)", percent, bonusDamage));
        }
        event.set("tooltipLines", lines);
        event.setHandled(true);
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


    /** Boosts the crit chance of a levelled oversized club, up to +25%. Crits also grant bonus XP. */
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

        itemLevel.addExperience(5);
    }

    /**
     * Award XP for every attack the player makes with an oversized club.
     * This is what actually drives the capability forward — without it the item
     * sits at Level 1 / 0 XP forever.
     */
    @SubscribeEvent
    public void onCombatAttack(CombatAttackEvent event) {
        Creature attacker = event.getAttacker();
        if (attacker == null || !attacker.isPlayer()) {
            return;
        }

        Item weapon = attacker.getPrimWeapon();
        if (weapon == null || weapon.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(weapon);
        if (itemLevel == null) {
            return;
        }

        int before = itemLevel.getLevel();
        itemLevel.addExperience(1);
        if (itemLevel.getLevel() > before) {
            attacker.getCommunicator().sendNormalServerMessage(
                    "Your " + weapon.getName() + " has reached level " + itemLevel.getLevel() + "!");
        }
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

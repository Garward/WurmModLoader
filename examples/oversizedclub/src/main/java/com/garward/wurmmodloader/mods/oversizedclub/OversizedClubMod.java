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
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import com.garward.wurmmodloader.modloader.interfaces.ItemTemplatesCreatedListener;
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
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * COMPREHENSIVE WEAPON CREATION TUTORIAL
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This mod demonstrates how to create a fully functional custom weapon with
 * WurmModLoader's modern event system. The "Oversized Club" serves as a
 * reference implementation showing ALL critical patterns for weapon creation.
 *
 * <p><b>What This Mod Creates:</b></p>
 * <ul>
 *   <li>A massive two-handed club weapon (15.0 base damage vs club's 8.3)</li>
 *   <li>Very slow swing speed (6.0s vs club's 4.5s)</li>
 *   <li>Uses huge club skill for leveling</li>
 *   <li>Crafted from log + carving knife (40 carpentry)</li>
 *   <li>Registered with Armoury/DUSKombat for proper damage calculation</li>
 * </ul>
 *
 * <p><b>🎓 TUTORIAL GUIDE - What You'll Learn:</b></p>
 * <ul>
 *   <li>✅ How to change weapon damage types (crush/slash/pierce)</li>
 *   <li>✅ How to change materials (wood/metal/stone)</li>
 *   <li>✅ How to modify crafting recipes</li>
 *   <li>✅ How to restrict materials (e.g., "steel only")</li>
 *   <li>✅ How to register with Armoury/DUSKombat combat systems</li>
 *   <li>✅ How to avoid common pitfalls (see WEAPON_CREATION_PITFALLS.md)</li>
 * </ul>
 *
 * <p><b>📋 Prerequisites:</b></p>
 * <ul>
 *   <li>WurmModLoader 1.0.0+ (with Phase 6 event system)</li>
 *   <li>Wurm Unlimited Dedicated Server</li>
 *   <li>Optional: Armoury/DUSKombat mods for enhanced combat</li>
 * </ul>
 *
 * <p><b>🔧 Modern Event System Demo:</b></p>
 * <p>Notice how this mod uses {@code @SubscribeEvent} annotations instead of
 * implementing listener interfaces. This is cleaner, more flexible, and easier
 * to maintain than the old approach.</p>
 *
 * <h2>Old Way (Still Works):</h2>
 * <pre>{@code
 * public class OversizedClubMod implements WurmServerMod, ItemTemplatesCreatedListener {
 *     @Override
 *     public void onItemTemplatesCreated() {
 *         // Create item...
 *     }
 * }
 * }</pre>
 *
 * <h2>New Way (This Mod):</h2>
 * <pre>{@code
 * public class OversizedClubMod implements WurmServerMod {
 *     @SubscribeEvent
 *     public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
 *         // Create item...
 *     }
 * }
 * }</pre>
 *
 * <p><b>📚 See Also:</b></p>
 * <ul>
 *   <li>{@code WEAPON_CREATION_PITFALLS.md} - Common mistakes and fixes</li>
 *   <li>{@code ItemTemplateBuilder} - API for creating items</li>
 *   <li>{@code CreationEntryCreator} - API for crafting recipes</li>
 * </ul>
 *
 * @author Garward
 * @version 1.0.0
 * @see com.garward.wurmmodloader.modsupport.ItemTemplateBuilder
 * @see com.wurmonline.server.items.CreationEntryCreator
 * @see com.wurmonline.server.combat.Weapon
 */
public class OversizedClubMod implements WurmServerMod, ItemTemplatesCreatedListener {

    private static final Logger logger = Logger.getLogger(OversizedClubMod.class.getName());
    private static final double BASE_CLUB_DAMAGE = 19.0d;
    private static final double DAMAGE_PER_LEVEL = 0.5d;
    private static final double MAX_LEVEL_DAMAGE_BONUS = 15.0d;
    private static int oversizedClubTemplateId = -1;
    private volatile boolean templatesRegistered = false;

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * ITEM TEMPLATE CREATION - Called during server initialization
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * <p>This is where we register our custom weapon using the modern event system.
     * Notice the {@code @SubscribeEvent} annotation - no interface implementation needed!
     *
     * <p><b>⚠️ CRITICAL TIMING:</b> This event fires AFTER vanilla item templates are
     * created, which is perfect for:</p>
     * <ul>
     *   <li>Copying icons from existing items</li>
     *   <li>Referencing vanilla ItemList constants</li>
     *   <li>Creating recipes using vanilla items as ingredients</li>
     * </ul>
     *
     * @param event the item templates created event (not used here, but required)
     */
    @SubscribeEvent
    public void onItemTemplatesCreatedEvent(ItemTemplatesCreatedEvent event) {
        registerItemTemplates();
    }

    @Override
    public void onItemTemplatesCreated() {
        registerItemTemplates();
    }

    private synchronized void registerItemTemplates() {
        if (templatesRegistered) {
            return;
        }
        templatesRegistered = true;
        logger.info("Creating Oversized Club item template...");

        try {
            // ═══════════════════════════════════════════════════════════════════════
            // STEP 1: COPY ICON FROM EXISTING ITEM (Recommended Approach)
            // ═══════════════════════════════════════════════════════════════════════
            //
            // 🎨 Icon Strategy: Dynamically copy from similar items instead of hardcoding
            // icon numbers. This prevents breakage if Wurm updates internal icon IDs.
            //
            // 📝 Examples of where to get icons:
            //   - For clubs: ItemList.clubHuge, ItemList.club
            //   - For swords: ItemList.swordLong, ItemList.swordShort, ItemList.swordGreat
            //   - For axes: ItemList.hatchet, ItemList.axe, ItemList.axeHuge
            //   - For spears: ItemList.spearLong, ItemList.spearSteel
            //   - For knives: ItemList.knifeButterblade, ItemList.knifeCarving
            //
            ItemTemplate hugeClubTemplate = ItemTemplateFactory.getInstance().getTemplate(ItemList.clubHuge);
            short hugeClubIcon = hugeClubTemplate.getImageNumber();
            logger.info("Copying icon from huge club: " + hugeClubIcon);

            // ═══════════════════════════════════════════════════════════════════════
            // STEP 2: BUILD THE ITEM TEMPLATE
            // ═══════════════════════════════════════════════════════════════════════
            //
            // 🏗️ ItemTemplateBuilder: Fluent API for creating item templates
            //
            // 📝 The namespace string ("garward.oversizedclub") must be UNIQUE across
            // all mods. Use your mod name as a prefix to avoid conflicts!
            //
            ItemTemplate template = new ItemTemplateBuilder("garward.oversizedclub")
                    // ─────────────────────────────────────────────────────────────────
                    // Basic Properties
                    // ─────────────────────────────────────────────────────────────────
                    .name("oversized club", "oversized clubs",
                          "A massive, crudely made club of enormous proportions. " +
                          "It deals devastating damage but is extremely slow to swing. " +
                          "Only the strongest warriors can wield this effectively.")

                    // ─────────────────────────────────────────────────────────────────
                    // Visual Properties
                    // ─────────────────────────────────────────────────────────────────
                    //
                    // 🎨 Model Strategy: Reuse existing Wurm models for server-side mods.
                    // Client doesn't need any files - server tells client which model to use.
                    //
                    // 📝 Common weapon models:
                    //   - Clubs: "model.weapon.club.", "model.weapon.club.huge."
                    //   - Swords: "model.weapon.sword.long.", "model.weapon.sword.short."
                    //   - Axes: "model.weapon.axe.", "model.weapon.axe.huge."
                    //   - Spears: "model.weapon.spear."
                    //
                    .modelName("model.weapon.club.huge.") // Use huge club model (visually bigger)
                    .imageNumber(hugeClubIcon) // Copy icon from actual huge club template
                    .behaviourType((short) 35) // Standard weapon behavior

                    // ═════════════════════════════════════════════════════════════════
                    // 🔴 CRITICAL: ITEM TYPES - Controls how Wurm treats this item
                    // ═════════════════════════════════════════════════════════════════
                    //
                    // ⚠️ PITFALL WARNING: Missing ITEM_TYPE_WEAPON causes item to behave
                    // as a container/inventory object instead of a weapon!
                    //
                    // 🎓 TUTORIAL: Changing Weapon Damage Types
                    // ─────────────────────────────────────────────────────────────────
                    //
                    // The last ItemType constant determines the weapon's damage type.
                    // You MUST include ITEM_TYPE_WEAPON as the base, then add ONE of:
                    //
                    // 🔨 CRUSH (Clubs, Mauls, Hammers):
                    //    ItemTypes.ITEM_TYPE_WEAPON_CRUSH
                    //    ↳ Best against: Skeletons, armored enemies
                    //    ↳ Skills: CLUB, CLUB_HUGE, MAUL, HAMMER
                    //
                    // ⚔️ SLASH (Swords, Axes):
                    //    ItemTypes.ITEM_TYPE_WEAPON_SLASH
                    //    ↳ Best against: Unarmored enemies, cloth
                    //    ↳ Skills: SWORDS_LONG, SWORDS_SHORT, AXE, AXE_HUGE
                    //
                    // 🗡️ PIERCE (Spears, Knives):
                    //    ItemTypes.ITEM_TYPE_WEAPON_PIERCE
                    //    ↳ Best against: Leather armor, fast attacks
                    //    ↳ Skills: SPEAR, KNIFE
                    //
                    // 📝 Example: To make this a SLASHING weapon instead of CRUSHING:
                    //    Change: ItemTypes.ITEM_TYPE_WEAPON_CRUSH
                    //    To:     ItemTypes.ITEM_TYPE_WEAPON_SLASH
                    //
                    .itemTypes(new short[] {
                        ItemTypes.ITEM_TYPE_NAMED,          // Has a custom name
                        ItemTypes.ITEM_TYPE_REPAIRABLE,     // Can be repaired
                        ItemTypes.ITEM_TYPE_WOOD,           // Made of wood (see MATERIAL TUTORIAL below)
                        ItemTypes.ITEM_TYPE_WEAPON,         // ⚠️ BASE WEAPON TYPE (CRITICAL - DON'T REMOVE!)
                        ItemTypes.ITEM_TYPE_WEAPON_CRUSH    // 🔨 CRUSHING damage (clubs/mauls)

                        // 💡 TO CHANGE TO SLASH: Replace line above with:
                        // ItemTypes.ITEM_TYPE_WEAPON_SLASH

                        // 💡 TO CHANGE TO PIERCE: Replace line above with:
                        // ItemTypes.ITEM_TYPE_WEAPON_PIERCE
                    })

                    // ─────────────────────────────────────────────────────────────────
                    // Combat Properties
                    // ─────────────────────────────────────────────────────────────────
                    .combatDamage(40) // Fallback damage (used if no Armoury/DUSKombat)
                    .primarySkill(SkillList.CLUB_HUGE) // Skill that levels up when using this weapon
                    .bodySpaces(MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY) // Let Wurm decide which body parts

                    // ─────────────────────────────────────────────────────────────────
                    // Weight and Dimensions
                    // ─────────────────────────────────────────────────────────────────
                    //
                    // 📝 Weight affects:
                    //   - Stamina drain when wielding
                    //   - Crafting material requirements (source must be heavier than result)
                    //   - Inventory management (heavier = fewer items carried)
                    //
                    .weightGrams(12000) // 12kg - very heavy!
                    .dimensions(60, 300, 60) // Width, Height, Length in mm (affects inventory grid)
                    .difficulty(40.0f) // Minimum skill for 100% success rate

                    // ═════════════════════════════════════════════════════════════════
                    // 🎓 TUTORIAL: Changing Material Types
                    // ═════════════════════════════════════════════════════════════════
                    //
                    // The .material() method sets the BASE material, and .itemTypes()
                    // includes the ITEM_TYPE_* constant matching that material.
                    //
                    // ⚠️ BOTH must be changed together for consistency!
                    //
                    // 🌳 WOOD MATERIALS (current setup):
                    //    .material(Materials.MATERIAL_WOOD_BIRCH)  // or WOOD_OAK, WOOD_PINE, etc.
                    //    .itemTypes({ ..., ItemTypes.ITEM_TYPE_WOOD, ... })
                    //
                    // ⚒️ METAL MATERIALS:
                    //    .material(Materials.MATERIAL_STEEL)
                    //    .itemTypes({ ..., ItemTypes.ITEM_TYPE_METAL, ... })
                    //
                    //    Available metals:
                    //      - Materials.MATERIAL_IRON
                    //      - Materials.MATERIAL_STEEL
                    //      - Materials.MATERIAL_SILVER
                    //      - Materials.MATERIAL_GOLD
                    //      - Materials.MATERIAL_COPPER
                    //      - Materials.MATERIAL_BRASS
                    //      - Materials.MATERIAL_BRONZE
                    //
                    // 🪨 STONE MATERIALS:
                    //    .material(Materials.MATERIAL_STONE)
                    //    .itemTypes({ ..., ItemTypes.ITEM_TYPE_STONE, ... })
                    //
                    // 💡 EXAMPLE: To change this from WOOD to STEEL:
                    //    1. Change line 213 from ITEM_TYPE_WOOD to ITEM_TYPE_METAL
                    //    2. Change line below from MATERIAL_WOOD_BIRCH to MATERIAL_STEEL
                    //    3. Change crafting recipe (line 278+) to use steel lump instead of log
                    //
                    .material(Materials.MATERIAL_WOOD_BIRCH) // 🌳 Birch wood (change for different materials)
                    .decayTime(Long.MAX_VALUE) // Never decays (set to 9072000L for ~3 months)

                    // ─────────────────────────────────────────────────────────────────
                    // Value and Rarity
                    // ─────────────────────────────────────────────────────────────────
                    .value(15000) // Base copper value (affects NPC merchant prices)

                    // Build the template (creates the ItemTemplate object)
                    .build();

            // ═════════════════════════════════════════════════════════════════
            // STEP 3: STORE TEMPLATE ID (needed for Weapon registration and recipes)
            // ═════════════════════════════════════════════════════════════════
            oversizedClubTemplateId = template.getTemplateId();

            // ═════════════════════════════════════════════════════════════════
            // 🔴 CRITICAL: WEAPON REGISTRATION - Armoury/DUSKombat Combat System
            // ═════════════════════════════════════════════════════════════════
            //
            // ⚠️ PITFALL WARNING: Without this, your weapon will deal 0 damage on
            // servers running Armoury or DUSKombat mods (common PvP servers)!
            //
            // 📝 The .combatDamage(40) in ItemTemplateBuilder is just a FALLBACK.
            // Real damage calculation happens here via the Weapon constructor.
            //
            // 🎮 How to check if you need this:
            //    - Look for server log warnings: "Weapon map does not contain entry for..."
            //    - If present: YOU NEED THIS REGISTRATION!
            //    - If absent: Vanilla Wurm (registration still safe to include)
            //
            // ═════════════════════════════════════════════════════════════════
            // Constructor Parameters Explained:
            // ═════════════════════════════════════════════════════════════════
            //
            // new Weapon(templateId, damage, speed, critChance, reach, weightGroup, parryPercent, skillPenalty)
            //                 ↓         ↓      ↓        ↓        ↓         ↓            ↓              ↓
            //
            // 1. templateId (int): The item template ID from .getTemplateId()
            //    - Links this Weapon registration to your ItemTemplate
            //
            // 2. damage (float): Base damage multiplier
            //    - Regular club: 8.3
            //    - Huge axe: 14.2 (modified on this server)
            //    - Our oversized: 19.0 (MEME TIER - 34% more than huge axe!)
            //    - DUSKombat applies heavy multipliers: 19.0 → ~19000 actual damage @ Q100
            //    - Quality affects final damage (Q9 ≈ 10500, Q100 ≈ 19000)
            //
            // 3. speed (float): Time in SECONDS between swings
            //    - Regular club: 4.5s
            //    - Huge axe: 5.9s (modified on this server)
            //    - Our oversized: 8.0s (EXTREMELY slow - meme tier commitment!)
            //    - Lower = faster attacks
            //    - DPS: 19.0 / 8.0 = 2.375 (similar to huge axe's 2.41)
            //    - High burst, low sustained DPS = balanced!
            //
            // 4. critChance (float): Critical hit chance parameter
            //    ⚠️ IMPORTANT: This value is DIVIDED BY 5.0 internally!
            //    - 0.002 parameter → 0.002/5.0 = 0.0004 = 0.04% actual crit (clubs)
            //    - 0.012 parameter → 0.012/5.0 = 0.0024 = 0.24% actual crit
            //    - 0.02 parameter → 0.02/5.0 = 0.004 = 0.4% actual crit (titan weapons)
            //    - To get 1% actual crit, use parameter value 0.05
            //
            // 5. reach (int): Attack range
            //    - 3 = standard melee (clubs, swords)
            //    - 5 = longer reach (spears, polearms)
            //
            // 6. weightGroup (int): Stamina drain category
            //    - 1 = very light (knives)
            //    - 3 = medium (regular club)
            //    - 4 = heavy (our oversized club, huge weapons)
            //    - 5 = very heavy (mauls, great axes)
            //
            // 7. parryPercent (float): Parry effectiveness (0.0 to 1.0)
            //    - 0.4 = 40% parry modifier (standard for clubs)
            //    - Higher = better at blocking attacks
            //
            // 8. skillPenalty (double): Combat skill penalty/effectiveness
            //    ⚠️ NOTE: This is NOT armour damage (that's handled by material type)
            //    - 0d = no skill penalty (most titan/endgame weapons)
            //    - 0.5d = moderate penalty (clubs, knuckles)
            //    - Higher values = harder to use effectively
            //    - Affects skill gain rate and combat effectiveness
            //
            // ─────────────────────────────────────────────────────────────────
            // Reference Values (from WyvernMods - actual working examples):
            // ─────────────────────────────────────────────────────────────────
            // Format: (damage, speed, crit_param, reach, weight, parry, skill_penalty)
            //
            //   Regular Club:  (8.3f,  4.5f, 0.002f, 3, 3, 0.4f, 0.5d)
            //   Knuckles:      (3.8f,  2.2f, 0.002f, 1, 1, 0.2f, 0.5d) <- Fast, low damage
            //   Battle Yoyo:   (6.85f, 3.75f, 0.012f, 2, 2, 0.0f, 0d) <- High crit
            //   Warhammer:     (9.5f,  5.6f, 0.015f, 4, 3, 1.0f, 0d) <- High parry
            //   Titan weapons: (11f,   5f,   0.02f,  4, 4, 1.0f, 0d) <- Endgame
            //   Eviscerator:   (100f,  3f,   0.02f,  5, 5, 0.4f, 0.5d) <- Genocide tier
            //
            new Weapon(
                oversizedClubTemplateId,  // 1. Template ID from above
                19.0f,                    // 2. Damage: 19.0 base (MEME TIER - 34% > huge axe!)
                8.0f,                     // 3. Speed: 8.0 seconds (EXTREMELY slow)
                0.002f,                   // 4. Crit param: 0.04% actual (0.002/5.0)
                2,                        // 5. Reach: 2 (shorter - must get dangerously close)
                5,                        // 6. Weight group: 5 (very heavy like huge weapons)
                0.3f,                     // 7. Parry: 30% (worse defense - glass cannon)
                0.0d                      // 8. Skill penalty: 0.0 (titan tier - no penalty)
            );

            // ═════════════════════════════════════════════════════════════════
            // 📚 ADVANCED: Armoury/DUSKombat Material System
            // ═════════════════════════════════════════════════════════════════
            //
            // ⚠️ IMPORTANT: This section only applies if your server runs
            // Armoury or DUSKombat mods (common on PvP/custom servers)!
            //
            // 🎮 Vanilla Servers: Skip this section - the Weapon() registration
            // above is all you need!
            //
            // ─────────────────────────────────────────────────────────────────
            // 🔧 Material-Based Damage Modifiers (Armoury Mod)
            // ─────────────────────────────────────────────────────────────────
            //
            // On servers with Armoury installed, weapon damage is FURTHER
            // modified by the material the weapon is made from:
            //
            // Formula: final_damage = base_damage × material_modifier
            //
            // Common material modifiers (from Armoury.properties):
            //   - Steel: 1.03x (3% bonus damage)
            //   - Bronze: No damage modifier, but 2% faster swing (0.98x speed)
            //   - Glimmersteel: No damage bonus, but 5% more parries (0.95x parry)
            //   - Adamantine: 5% more armor damage (1.05x armor damage)
            //   - Iron: No specific bonuses in default config
            //   - Wood: No modifiers (1.0x)
            //
            // 📝 Example Damage Calculation (Our Oversized Club):
            //   Base damage parameter: 15.0
            //   Material: Birch wood (MATERIAL_WOOD_BIRCH)
            //   Material modifier: 1.0 (no bonus for wood)
            //   Armoury damage: 15.0 × 1.0 = 15.0
            //
            // 📝 Example if we changed to Steel (see Material Tutorial above):
            //   Base damage parameter: 15.0
            //   Material: Steel (MATERIAL_STEEL)
            //   Material modifier: 1.03 (from Armoury config)
            //   Armoury damage: 15.0 × 1.03 = 15.45
            //
            // ⚠️ NOTE: These multipliers are configured in Armoury.properties
            // Server admins can customize them! Check your server config.
            //
            // ─────────────────────────────────────────────────────────────────
            // ⚔️ DUSKombat Damage Multipliers & Combat Flow
            // ─────────────────────────────────────────────────────────────────
            //
            // DUSKombat completely redesigns combat with a new damage flow:
            //
            // 📊 Combat Flow: Swing → Accuracy → Dodge → Critical →
            //                 Shield Block → Parry → Glance → Damage
            //
            // Key DUSKombat multipliers (from DUSKombat.properties):
            //   - playerToEnvironmentDamageMultiplier: 1.0 (vs NPCs)
            //   - environmentToPlayerDamageMultiplier: 1.0 (NPCs vs players)
            //   - playerToPlayerDamageMultiplier: 0.7 (PvP - 30% reduction!)
            //
            // 💥 Critical Strikes (NEW in DUSKombat):
            //   - Available in PvE and PvP (both directions)
            //   - Crit multiplier: 1.5x damage
            //   - Bypasses parry and shield block (only dodge can avoid)
            //   - Our club: 0.04% base crit chance (from 0.002 param)
            //
            // 📝 Complete Damage Example (DUSKombat + Armoury server):
            //
            //   1. Base damage: 19.0 (from Weapon registration - MEME TIER)
            //   2. Material mod: ×1.0 (wood, no Armoury bonus)
            //   3. Quality mod: Q9 = ~0.5556 multiplier
            //   4. DUSKombat PvE: ×1.0 (vs creatures)
            //   5. Skill/Focus/Stance: Various multipliers
            //   6. Result: ~10500 actual damage @ Q9 (ONE-SHOT territory!)
            //
            //   If Q100 steel (instead of Q9 wood):
            //   1. Base: 19.0
            //   2. Material: ×1.03 (steel bonus) = 19.57
            //   3. Quality: ×1.0 (Q100) = 19.57
            //   4. DUSKombat PvE: ×1.0
            //   5. With typical multipliers: ~34000+ damage (DEVASTATING!)
            //
            //   Balance: DPS = 19.0 / 8.0 = 2.375
            //   Compare: Huge Axe DPS = 14.2 / 5.9 = 2.41
            //   Result: Similar DPS, but EXTREME burst vs sustained damage tradeoff
            //
            // ─────────────────────────────────────────────────────────────────
            // 🛡️ Armor Damage (How It Actually Works)
            // ─────────────────────────────────────────────────────────────────
            //
            // ❌ CORRECTION: Parameter 8 (skillPenalty) does NOT affect
            // armor damage! Here's how armor damage REALLY works:
            //
            // 🎯 Armor Damage Sources:
            //
            // 1️⃣ Vanilla Wurm:
            //    - Uses getMaterialArmourDamageBonus(material)
            //    - Different per material (crush weapons get bonus)
            //    - Example: Mauls get higher armor damage than swords
            //
            // 2️⃣ Armoury Mod:
            //    - Adds materialWeaponArmourDamage multiplier
            //    - Configured in Armoury.properties
            //    - Example: Adamantine = 1.05x (5% more armor damage)
            //    - Formula: armor_dmg = base_armor_dmg × material_multiplier
            //
            // 3️⃣ Damage Type Effectiveness (Armoury):
            //    - Crush (clubs/mauls): BEST vs armor
            //    - Slash (swords/axes): Medium vs armor
            //    - Pierce (spears/knives): Worst vs armor
            //    - This is why mauls are "tank busters"!
            //
            // 4️⃣ Armor Takes Double Damage (DUSKombat):
            //    - "Armour now takes roughly double the amount of damage
            //       in combat from before."
            //    - Makes armor maintenance more important
            //
            // 📝 Armor Damage Examples:
            //
            //   Our Oversized Club (crush weapon):
            //     - Crush damage type (ITEM_TYPE_WEAPON_CRUSH)
            //     - Gets vanilla crush bonus vs armor
            //     - Wood material = no Armoury bonus
            //     - On DUSKombat: 2x armor damage taken
            //
            //   If we changed to Steel Maul (hypothetical):
            //     - Crush damage type (even higher base armor damage)
            //     - Steel material = no specific Armoury armor bonus
            //     - Adamantine = 1.05x armor damage
            //     - On DUSKombat: Still 2x armor damage taken
            //
            // ═════════════════════════════════════════════════════════════════

            // ═════════════════════════════════════════════════════════════════
            // STEP 4: CREATE CRAFTING RECIPE
            // ═════════════════════════════════════════════════════════════════
            //
            // This makes the item craftable in-game via the creation menu.
            //
            // 🎓 TUTORIAL: Recipe Format
            // ─────────────────────────────────────────────────────────────────
            // Recipe: [SOURCE ITEM] + [TOOL] = [RESULT]
            // Player: Right-click SOURCE with TOOL activated → creation menu appears
            //
            // Current recipe: LOG + CARVING KNIFE = OVERSIZED CLUB
            //
            // ⚠️ PITFALL WARNING: Source material must weigh MORE than result!
            //   - Our club: 12kg (12000g)
            //   - Log: 20kg+ ✅ GOOD
            //   - Shaft: 1kg ❌ BAD - "too little material" error
            //
            // ═════════════════════════════════════════════════════════════════
            // 🎓 TUTORIAL: Changing Crafting Recipes
            // ═════════════════════════════════════════════════════════════════
            //
            // 📝 EXAMPLE 1: Change to ANVIL + HAMMER (smithing recipe)
            //    Change parameter 2 (tool): ItemList.knifeCarving → ItemList.hammerMetal
            //    Change parameter 3 (source): ItemList.log → ItemList.steelBar
            //    Change parameter 1 (skill): SkillList.CARPENTRY → SkillList.SMITHING_WEAPON_HEADS
            //
            // 📝 EXAMPLE 2: Change to STEEL LUMP + HAMMER (more basic smithing)
            //    Keep parameter 2: ItemList.hammerMetal
            //    Change parameter 3: ItemList.log → ItemList.steelLump
            //    Change parameter 1: SkillList.CARPENTRY → SkillList.SMITHING_WEAPON_HEADS
            //
            // 📝 EXAMPLE 3: Multiple recipes (call createSimpleEntry multiple times)
            //    You can create several recipes for the same item:
            //      - Iron lump + hammer = oversized club (50 smithing)
            //      - Steel lump + hammer = oversized club (40 smithing)
            //      - Log + carving knife = oversized club (60 carpentry)
            //
            // Common Tools:
            //   - ItemList.knifeCarving (carpentry)
            //   - ItemList.hammerMetal (smithing)
            //   - ItemList.saw (woodworking)
            //   - ItemList.chisel (stone carving)
            //   - ItemList.needle (leatherworking)
            //
            // Common Source Materials:
            //   - ItemList.log (20-60kg, carpentry)
            //   - ItemList.plank (4kg, carpentry)
            //   - ItemList.ironLump (smithing)
            //   - ItemList.steelLump (smithing)
            //   - ItemList.rock (stone)
            //
            CreationEntryCreator.createSimpleEntry(
                    SkillList.CARPENTRY,           // 1. Skill required (affects difficulty)
                    ItemList.knifeCarving,        // 2. Tool needed (right-click with this activated)
                    ItemList.log,                 // 3. Source material (must weigh enough!)
                    oversizedClubTemplateId,      // 4. Result item template ID
                    false,                        // 5. useTemplateWeight (false = fixed weight)
                    true,                         // 6. destroyTarget (true = consume source)
                    0.0f,                         // 7. skillGainMultiplier (0 = normal gain)
                    false,                        // 8. allowOnWater (false = can't craft in water)
                    false,                        // 9. allowOnSurface (false = must be in inventory)
                    CreationCategories.WEAPONS    // 10. Category in creation menu
            );

            // ═════════════════════════════════════════════════════════════════
            // 🎓 TUTORIAL: Material Restrictions ("Steel Only" Example)
            // ═════════════════════════════════════════════════════════════════
            //
            // ❓ Problem: The recipe above accepts ANY log (birch, oak, pine, etc.)
            // but we might want to restrict to STEEL only.
            //
            // ❌ WRONG APPROACH: Just changing source to steelLump
            //    This allows steel, but player could also use improved recipes
            //    that Wurm creates automatically (e.g., using steel bars).
            //
            // ✅ CORRECT APPROACH: Validate material in onCreationEntry hook
            //
            // 📝 EXAMPLE: Add material validation (advanced - requires bytecode hook)
            //
            // In your preInit() method (not shown here), add:
            //
            // ```java
            // @Override
            // public void preInit() {
            //     try {
            //         ClassPool classPool = HookManager.getInstance().getClassPool();
            //         CtClass ctCreationEntry = classPool.get("com.wurmonline.server.items.CreationEntry");
            //
            //         // Hook the creation validation
            //         CtMethod method = ctCreationEntry.getDeclaredMethod("create");
            //         method.insertBefore(
            //             "if (target.getTemplateId() == " + oversizedClubTemplateId + ") {" +
            //             "    byte targetMaterial = source.getMaterial();" +
            //             "    if (targetMaterial != " + Materials.MATERIAL_STEEL + ") {" +
            //             "        performer.getCommunicator().sendNormalServerMessage(" +
            //             "            \"The oversized club can only be crafted from steel.\");" +
            //             "        return true; // Block creation" +
            //             "    }" +
            //             "}"
            //         );
            //     } catch (Exception e) {
            //         logger.log(Level.SEVERE, "Failed to add material restriction hook", e);
            //     }
            // }
            // ```
            //
            // ═════════════════════════════════════════════════════════════════
            // 🎓 TUTORIAL: SIMPLER Material Restriction (Recommended)
            // ═════════════════════════════════════════════════════════════════
            //
            // Instead of bytecode hooks, create SPECIFIC recipes for each allowed material:
            //
            // ```java
            // // Only allow steel lump
            // CreationEntryCreator.createSimpleEntry(
            //     SkillList.SMITHING_WEAPON_HEADS,
            //     ItemList.hammerMetal,
            //     ItemList.steelLump,              // ONLY steel lump accepted
            //     oversizedClubTemplateId,
            //     false, true, 0.0f, false, false,
            //     CreationCategories.WEAPONS
            // );
            //
            // // Optionally allow steel bar too
            // CreationEntryCreator.createSimpleEntry(
            //     SkillList.SMITHING_WEAPON_HEADS,
            //     ItemList.hammerMetal,
            //     ItemList.steelBar,               // Also accept steel bar
            //     oversizedClubTemplateId,
            //     false, true, 0.0f, false, false,
            //     CreationCategories.WEAPONS
            // );
            // ```
            //
            // This way, ONLY the materials you explicitly list will work.
            // Simpler and safer than bytecode hooks!
            //
            // ═════════════════════════════════════════════════════════════════

            // ═════════════════════════════════════════════════════════════════
            // SUCCESS! Log confirmation
            // ═════════════════════════════════════════════════════════════════
            logger.log(Level.INFO, "✅ Successfully created Oversized Club item template!");
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, "Item Details:");
            logger.log(Level.INFO, "  • Template ID: " + oversizedClubTemplateId);
            logger.log(Level.INFO, "  • Namespace: garward.oversizedclub");
            logger.log(Level.INFO, "  • Material: Birch wood (ITEM_TYPE_WOOD)");
            logger.log(Level.INFO, "  • Damage Type: Crushing (ITEM_TYPE_WEAPON_CRUSH)");
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, "Combat Stats (MEME TIER):");
            logger.log(Level.INFO, "  • Base Damage: 19.0 (~19000 @ Q100 - 34% > huge axe!)");
            logger.log(Level.INFO, "  • Swing Speed: 8.0 seconds (EXTREMELY slow)");
            logger.log(Level.INFO, "  • Weight: 12kg (heavy - stamina intensive)");
            logger.log(Level.INFO, "  • Reach: 2 (short - must get close!)");
            logger.log(Level.INFO, "  • Skill: Huge Club");
            logger.log(Level.INFO, "  • DPS: ~2.38 (balanced despite massive burst)");
            logger.log(Level.INFO, "  • ✅ Registered with Armoury/DUSKombat combat system");
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, "Crafting:");
            logger.log(Level.INFO, "  • Recipe: log + carving knife");
            logger.log(Level.INFO, "  • Skill: 40 carpentry");
            logger.log(Level.INFO, "  • Category: Weapons");
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, "📚 This mod serves as a comprehensive tutorial for weapon creation.");
            logger.log(Level.INFO, "📝 See inline comments in OversizedClubMod.java for customization examples.");
            logger.log(Level.INFO, "⚠️ See WEAPON_CREATION_PITFALLS.md for common mistakes to avoid.");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Failed to create Oversized Club item template", e);
            logger.log(Level.SEVERE, "");
            logger.log(Level.SEVERE, "If you see this error, check:");
            logger.log(Level.SEVERE, "  1. Is the server running Wurm Unlimited 1.9+?");
            logger.log(Level.SEVERE, "  2. Is WurmModLoader installed correctly?");
            logger.log(Level.SEVERE, "  3. Are all dependencies present (ItemList, Materials, etc.)?");
            logger.log(Level.SEVERE, "  4. See stack trace above for specific error details");
        }
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * SERVER STARTED EVENT - Final initialization step
     * ═════════════════════════════════════════════════════════════════════════
     *
     * <p>Called when the server has finished starting and is ready for players.
     * This is the perfect place to announce your mod's features to server admins.</p>
     *
     * <p>Another example of the modern event system in action! This method is
     * called automatically thanks to the {@code @SubscribeEvent} annotation.</p>
     *
     * <p><b>📝 Use Cases:</b></p>
     * <ul>
     *   <li>Log confirmation that your mod loaded successfully</li>
     *   <li>Display crafting recipes for admin reference</li>
     *   <li>Show combat stats and gameplay info</li>
     *   <li>Warn about dependencies or configuration requirements</li>
     * </ul>
     *
     * @param event the server started event (not used here, but required)
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        String separator = "═══════════════════════════════════════════════════════════════════";
        logger.info(separator);
        logger.info("🎮 OVERSIZED CLUB MOD - Server Ready");
        logger.info(separator);
        logger.info("");
        logger.info("✅ The Oversized Club has been added to the game!");
        logger.info("");
        logger.info("📖 HOW TO CRAFT:");
        logger.info("  1. Activate a carving knife");
        logger.info("  2. Right-click a LOG in your inventory");
        logger.info("  3. Select 'Create > Weapons > oversized club'");
        logger.info("  4. Requires 40 carpentry skill for 100%");
        logger.info("  5. ⚠️ Very heavy (12kg) - plan your inventory space!");
        logger.info("");
        logger.info("👁️ VISUAL:");
        logger.info("  • Uses huge club 3D model");
        logger.info("  • Server-side only - no client mods needed");
        logger.info("  • Prepare for comedic oversized weapon fun");
        logger.info("");
        logger.info("⚔️ COMBAT STATS (MEME TIER):");
        logger.info("  • Base damage: 19.0 (~19000 @ Q100 - 34% MORE than huge axe!)");
        logger.info("  • Swing speed: 8.0 seconds (EXTREMELY slow - commit to the swing!)");
        logger.info("  • Reach: 2 (short - get dangerously close)");
        logger.info("  • DPS: ~2.38 (balanced by extreme speed penalty)");
        logger.info("  • Uses huge club fighting skill for leveling");
        logger.info("  • Best for: Drive-by ONE-SHOTS and comedic devastation");
        logger.info("  • ⚠️ Glass cannon: Massive burst, terrible defense");
        logger.info("  • ✅ Registered with Armoury/DUSKombat combat systems");
        logger.info("");
        logger.info("🎓 TUTORIAL MOD:");
        logger.info("  • This mod demonstrates comprehensive weapon creation");
        logger.info("  • See OversizedClubMod.java for inline documentation");
        logger.info("  • Learn how to change damage types, materials, recipes");
        logger.info("  • See WEAPON_CREATION_PITFALLS.md for common mistakes");
        logger.info("");
        logger.info("📚 CUSTOMIZATION GUIDE:");
        logger.info("  • Change damage type: Modify ItemTypes.ITEM_TYPE_WEAPON_* constant");
        logger.info("  • Change material: Update .material() and ITEM_TYPE_* together");
        logger.info("  • Change recipe: Modify CreationEntryCreator parameters");
        logger.info("  • Restrict materials: Create specific recipes per material");
        logger.info("");
        logger.info("💡 See inline comments in the source code for examples!");
        logger.info(separator);
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * CAPABILITY REGISTRATION - Register custom data attachments
     * ═════════════════════════════════════════════════════════════════════════
     *
     * <p>This demonstrates the Phase 5.5 Capabilities System. Capabilities allow
     * mods to attach custom data to items, players, creatures, and tiles without
     * managing their own database code.</p>
     *
     * <p><b>How It Works:</b></p>
     * <ul>
     *   <li>Register capability during server startup (this method)</li>
     *   <li>Framework injects ICapabilityProvider into Item class via Javassist</li>
     *   <li>Access data anywhere: {@code item.getCapability(ItemLevelCapability.INSTANCE)}</li>
     *   <li>Framework automatically persists to database</li>
     * </ul>
     *
     * @param event the capability registration event
     */
    @SubscribeEvent
    public void onCapabilityRegistration(CapabilityRegistrationEvent event) {
        logger.info("[OversizedClub] Registering ItemLevel capability...");

        // Register our item level capability
        // This allows ALL items in the game to have level/experience data
        event.registerItemCapability(ItemLevelCapability.INSTANCE);

        logger.info("[OversizedClub] ItemLevel capability registered successfully");
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * ITEM EXAMINE HANDLER - Display capability data to players
     * ═════════════════════════════════════════════════════════════════════════
     *
     * <p>When a player examines an item, we retrieve its level data from the
     * capability and add it to the examine text.</p>
     *
     * <p><b>Capabilities Demo:</b></p>
     * <ol>
     *   <li>Player examines item</li>
     *   <li>Framework calls item.getCapability() → lazy loads from database</li>
     *   <li>We get ItemLevel data and format it</li>
     *   <li>Player sees: "This is a club. [Level 5 (XP: 123/500)]"</li>
     * </ol>
     *
     * @param event the item examine event
     */
    @SubscribeEvent
    public void onItemExamine(ItemExamineEvent event) {
        Item item = event.getItem();

        // Only show level for our oversized club (optional - could show for all items)
        if (item.getTemplateId() != oversizedClubTemplateId) {
            return;
        }

        ItemLevel itemLevel = getItemLevelData(item);
        if (itemLevel == null) {
            return;
        }

        // Add level info to examine text
        String levelInfo = "\n[" + itemLevel + "]";
        event.addDescription(levelInfo);

        double bonusDamage = calculateDamageBonus(itemLevel.getLevel());
        if (bonusDamage > 0 && BASE_CLUB_DAMAGE > 0) {
            double percent = (bonusDamage / BASE_CLUB_DAMAGE) * 100.0d;
            String dmgInfo = String.format(Locale.US, "\n[+%.0f%% DMG from level (%.1f bonus damage)]", percent, bonusDamage);
            event.addDescription(dmgInfo);
        }

        logger.fine("[OversizedClub] Displayed level for item " + item.getWurmId() + ": " + itemLevel);
    }

    /**
     * Helper to safely pull the ItemLevel capability off an item. The cast and database
     * lookup are repetitive, so we centralize it here and reuse it from the combat hooks.
     */
    private ItemLevel getItemLevelData(Item item) {
        if (item == null || !(item instanceof ICapabilityProvider)) {
            return null;
        }
        try {
            ICapabilityProvider provider = (ICapabilityProvider) item;
            return (ItemLevel) provider.getCapability(ItemLevelCapability.INSTANCE);
        } catch (Exception e) {
            logger.log(Level.WARNING, "[OversizedClub] Failed to get item level capability", e);
            return null;
        }
    }

    private double calculateDamageBonus(int level) {
        return Math.min(level * DAMAGE_PER_LEVEL, MAX_LEVEL_DAMAGE_BONUS);
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * WEAPON STAT QUERY EVENT - Per-item stat tweaks (Phase 6+)
     * ═════════════════════════════════════════════════════════════════════════
     *
     * <p>This event fires every time Wurm queries a weapon stat such as base damage,
     * swing speed, parry %, or armour damage modifiers. Because we have access to the
     * actual {@link Item} instance we can scale the numbers with capability data or
     * material-specific bonuses.</p>
     */
    @SubscribeEvent
    public void onWeaponStatQuery(WeaponStatQueryEvent event) {
        Item item = event.getItemTemplate();
        if (item == null || item.getTemplateId() != oversizedClubTemplateId) {
            return; // Only care about actual oversized clubs
        }

        ItemLevel itemLevel = getItemLevelData(item);
        if (itemLevel == null) {
            return;
        }

        int level = itemLevel.getLevel();
        switch (event.getStatType()) {
            case DAMAGE:
                // +0.5 base damage per level (capped to avoid memes)
                event.setValue(event.getValue() + calculateDamageBonus(level));
                break;
            case SPEED:
                // Higher level wielders learn to handle the weight better: -0.1s per level
                double fasterSwing = Math.max(2.5d, event.getValue() - (level * 0.1d));
                event.setValue(fasterSwing);
                break;
            case PARRY:
                // Slight parry bonus to offset the slow wind-up
                double parryBonus = event.getValue() + Math.min(0.25d, level * 0.01d);
                event.setValue(parryBonus);
                break;
            default:
                // Leave other stats alone for tutorial clarity
                break;
        }
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * COMBAT CRITICAL HIT EVENT - Adjust crit chance on the fly
     * ═════════════════════════════════════════════════════════════════════════
     *
     * <p>Oversized clubs are clumsy, so we compensate by giving veterans a better
     * chance to land a devastating crit. The event fires just before Wurm rolls for
     * a crit, so tweaking {@link CombatCriticalHitEvent#setCritChance(float)} is
     * perfectly safe.</p>
     */
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

        float bonus = Math.min(0.25f, itemLevel.getLevel() * 0.01f); // up to +25% crit
        event.setCritChance(Math.min(1.0f, event.getCritChance() + bonus));
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * OPPORTUNITY ATTACK EVENT - Cancel or accelerate free swings
     * ═════════════════════════════════════════════════════════════════════════
     *
     * <p>This hook demonstrates how to either cancel an opportunity attack
     * (novices panic and drop their swing) or to shorten the recovery window for
     * experienced clubbers.</p>
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

        // Beginner safety rails: prevent back-to-back opportunity swings that often whiff
        if (level < 3 && event.getOpportunityCounter() > 0) {
            event.setCancelled(true);
            return;
        }

        // Veterans wind up faster after capitalizing on mistakes
        float hasteBonus = Math.min(1.0f, level * 0.05f);
        event.setActionCounter(Math.max(1.5f, event.getActionCounter() - hasteBonus));
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}

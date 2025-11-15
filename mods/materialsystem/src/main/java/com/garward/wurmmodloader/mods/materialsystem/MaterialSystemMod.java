package com.garward.wurmmodloader.mods.materialsystem;

import com.garward.wurmmodloader.api.events.item.ItemTemplatesCreatedEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.icon.IconRegistry;
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import com.garward.wurmmodloader.modsupport.constants.MaterialConstants;
import com.garward.wurmmodloader.modsupport.constants.ItemTypeConstants;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.wurmonline.server.items.ItemTemplate;  // Type import for return values (acceptable per architecture rules)

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Material System Mod
 *
 * Registers custom material items dropped by creatures.
 * Materials can be infused into soulbound gear for permanent bonuses.
 *
 * Material Tiers:
 * - Common: Wolf Fang, Bear Claw, Troll Hide, Spider Venom, Scorpion Chitin
 * - Rare: Champion Heart, Dragon Scale, Unicorn Horn, Forest Giant Essence
 * - Legendary: Ifrit Core, Frozen Heart, Verdant Seed, Void Shard
 *
 * All materials registered via Phase 4 Registry with ResourceLocation namespace.
 * Creature loot tables configured via JSON files.
 *
 * @author Garward
 * @version 1.0.0
 */
public class MaterialSystemMod implements WurmServerMod {

    private static final Logger logger = Logger.getLogger(MaterialSystemMod.class.getName());
    private static final String NAMESPACE = "powerfantasy";

    /**
     * Convert ResourceLocation to template ID string.
     * ResourceLocation("powerfantasy", "ifrit_core") -> "powerfantasy.ifrit_core"
     */
    private static String toTemplateId(ResourceLocation id) {
        return id.getNamespace() + "." + id.getPath();
    }

    /**
     * Register all material item templates using Phase 4 Registry.
     * Materials are registered with ResourceLocation for proper namespace isolation.
     */
    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("MaterialSystemMod: Registering material items...");
        logger.info("═══════════════════════════════════════════════════════════════");

        try {
            // Register Common Tier materials
            registerWolfFang();
            registerBearClaw();
            registerSpiderVenomSac();
            registerTrollHide();
            registerScorpionChitin();

            // Register Rare Tier materials
            registerChampionHeart();
            registerDragonScale();
            registerUnicornHorn();
            registerForestGiantEssence();

            // Register Legendary Tier materials
            registerIfritCore();
            registerFrozenHeart();
            registerVerdantSeed();
            registerVoidShard();

            int totalMaterials = MaterialRegistry.size();
            logger.info("✅ Successfully registered " + totalMaterials + " materials!");
            logger.info("   - Common: " + MaterialRegistry.getMaterialsByTier(MaterialTier.COMMON).size());
            logger.info("   - Rare: " + MaterialRegistry.getMaterialsByTier(MaterialTier.RARE).size());
            logger.info("   - Legendary: " + MaterialRegistry.getMaterialsByTier(MaterialTier.LEGENDARY).size());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Failed to register materials", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMMON TIER MATERIALS
    // ═══════════════════════════════════════════════════════════════════════════

    private void registerWolfFang() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "wolf_fang");
        Icon icon = IconRegistry.registerCustom(id, "wolf_fang.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Wolf Fang", "Wolf Fangs",
                  "A sharp fang from a wolf. Infusing this into a weapon grants a small damage bonus.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(100)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_BONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(1000)  // 10 copper
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(2.0f)  // +2 damage per stack
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.COMMON, 50);
        logger.info("Registered: Wolf Fang (template ID: " + template.getTemplateId() + ")");
    }

    private void registerBearClaw() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "bear_claw");
        Icon icon = IconRegistry.registerCustom(id, "bear_claw.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Bear Claw", "Bear Claws",
                  "A massive claw from a bear. Infusing this into a weapon grants moderate damage.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(150)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_BONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(1500)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(3.0f)  // +3 damage per stack
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.COMMON, 50);
        logger.info("Registered: Bear Claw (template ID: " + template.getTemplateId() + ")");
    }

    private void registerSpiderVenomSac() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "spider_venom_sac");
        Icon icon = IconRegistry.registerCustom(id, "spider_venom_sac.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Spider Venom Sac", "Spider Venom Sacs",
                  "A pulsing sac filled with deadly venom. Grants poison chance when infused.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(80)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_MEAT)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(2000)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(1.0f)
            .specialEffect(SpecialEffect.POISON)
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.COMMON, 50);
        logger.info("Registered: Spider Venom Sac (template ID: " + template.getTemplateId() + ")");
    }

    private void registerTrollHide() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "troll_hide");
        Icon icon = IconRegistry.registerCustom(id, "troll_hide.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Troll Hide", "Troll Hides",
                  "Thick hide from a troll. Infusing this grants durability bonuses.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(200)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_LEATHER)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(1200)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .durabilityPercent(0.05f)  // +5% durability per stack
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.COMMON, 50);
        logger.info("Registered: Troll Hide (template ID: " + template.getTemplateId() + ")");
    }

    private void registerScorpionChitin() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "scorpion_chitin");
        Icon icon = IconRegistry.registerCustom(id, "scorpion_chitin.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Scorpion Chitin", "Scorpion Chitins",
                  "Hardened chitin from a scorpion. Grants parry bonuses when infused.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(120)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_STONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(1800)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .parryPercent(0.02f)  // +2% parry per stack
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.COMMON, 50);
        logger.info("Registered: Scorpion Chitin (template ID: " + template.getTemplateId() + ")");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RARE TIER MATERIALS
    // ═══════════════════════════════════════════════════════════════════════════

    private void registerChampionHeart() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "champion_heart");
        Icon icon = IconRegistry.registerCustom(id, "champion_heart.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Champion Heart", "Champion Hearts",
                  "The still-beating heart of a champion creature. Grants significant power.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(300)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_MEAT)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(10000)  // 1 silver
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(20.0f)
            .damagePercent(0.05f)  // +5% damage
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.RARE, 5);
        logger.info("Registered: Champion Heart (template ID: " + template.getTemplateId() + ")");
    }

    private void registerDragonScale() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "dragon_scale");
        Icon icon = IconRegistry.registerCustom(id, "dragon_scale.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Dragon Scale", "Dragon Scales",
                  "A shimmering scale from a dragon hatchling. Grants damage and defense.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(250)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_STONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(15000)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(15.0f)
            .parryPercent(0.10f)  // +10% parry
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.RARE, 5);
        logger.info("Registered: Dragon Scale (template ID: " + template.getTemplateId() + ")");
    }

    private void registerUnicornHorn() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "unicorn_horn");
        Icon icon = IconRegistry.registerCustom(id, "unicorn_horn.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Unicorn Horn", "Unicorn Horns",
                  "A pristine horn from a unicorn. Grants healing effectiveness.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(200)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_BONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(20000)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(10.0f)
            .specialEffect(SpecialEffect.LIFESTEAL)
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.RARE, 5);
        logger.info("Registered: Unicorn Horn (template ID: " + template.getTemplateId() + ")");
    }

    private void registerForestGiantEssence() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "forest_giant_essence");
        Icon icon = IconRegistry.registerCustom(id, "forest_giant_essence.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Forest Giant Essence", "Forest Giant Essences",
                  "Concentrated essence from a forest giant. Grants stamina regeneration.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(150)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_WOOD_BIRCH)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(12000)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(12.0f)
            .attackSpeedPercent(0.05f)  // +5% attack speed
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.RARE, 5);
        logger.info("Registered: Forest Giant Essence (template ID: " + template.getTemplateId() + ")");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LEGENDARY TIER MATERIALS
    // ═══════════════════════════════════════════════════════════════════════════

    private void registerIfritCore() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "ifrit_core");
        Icon icon = IconRegistry.registerCustom(id, "ifrit_core.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Ifrit Core", "Ifrit Cores",
                  "A blazing core pulsing with the essence of a Fire Titan. " +
                  "Infusing this into a weapon grants devastating fire damage and attack speed.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(500)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_STONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(100000)  // 1 gold
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(50.0f)
            .attackSpeedPercent(0.10f)  // +10% attack speed
            .elementalDamage("fire", 50.0f)
            .specialEffect(SpecialEffect.BURN)
            .visualEffect("flames")
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.LEGENDARY, 1);
        logger.info("Registered: Ifrit Core (template ID: " + template.getTemplateId() + ")");
    }

    private void registerFrozenHeart() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "frozen_heart");
        Icon icon = IconRegistry.registerCustom(id, "frozen_heart.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Frozen Heart", "Frozen Hearts",
                  "An icy heart from an Ice Titan, forever frozen. " +
                  "Grants cold damage and critical strike bonuses.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(500)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_STONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(100000)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(50.0f)
            .critChancePercent(0.10f)  // +10% crit chance
            .elementalDamage("cold", 50.0f)
            .specialEffect(SpecialEffect.FREEZE)
            .visualEffect("ice")
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.LEGENDARY, 1);
        logger.info("Registered: Frozen Heart (template ID: " + template.getTemplateId() + ")");
    }

    private void registerVerdantSeed() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "verdant_seed");
        Icon icon = IconRegistry.registerCustom(id, "verdant_seed.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Verdant Seed", "Verdant Seeds",
                  "A living seed from a Nature Titan. " +
                  "Grants life-stealing and health regeneration.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(500)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_WOOD_BIRCH)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(100000)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(40.0f)
            .specialEffect(SpecialEffect.LIFESTEAL)
            .visualEffect("nature")
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.LEGENDARY, 1);
        logger.info("Registered: Verdant Seed (template ID: " + template.getTemplateId() + ")");
    }

    private void registerVoidShard() throws IOException {
        ResourceLocation id = new ResourceLocation(NAMESPACE, "void_shard");
        Icon icon = IconRegistry.registerCustom(id, "void_shard.png");

        ItemTemplate template = new ItemTemplateBuilder(toTemplateId(id))
            .name("Void Shard", "Void Shards",
                  "A fragment of pure darkness from a Shadow Titan. " +
                  "Grants shadow damage and fear effects.")
            .imageNumber((short) icon.getIconId())
            .weightGrams(500)
            .dimensions(5, 5, 5)
            .material(MaterialConstants.MATERIAL_STONE)
            .itemTypes(new short[] {
                ItemTypeConstants.ITEM_TYPE_DECORATION,
                ItemTypeConstants.ITEM_TYPE_NO_IMPROVE,
                ItemTypeConstants.ITEM_TYPE_TURNABLE
            })
            .value(100000)
            .decayTime(Long.MAX_VALUE)
            .behaviourType((short) 1)
            .difficulty(0.0f)
            .build();

        MaterialBonus bonus = new MaterialBonus.Builder()
            .baseDamage(45.0f)
            .damagePercent(0.05f)  // +5% damage
            .elementalDamage("shadow", 40.0f)
            .specialEffect(SpecialEffect.FEAR)
            .visualEffect("shadow")
            .build();

        MaterialRegistry.register(id, bonus, MaterialTier.LEGENDARY, 1);
        logger.info("Registered: Void Shard (template ID: " + template.getTemplateId() + ")");
    }

    /**
     * Log startup confirmation.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("Material System Mod - Ready");
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("");
        logger.info("📦 Registered Materials:");
        logger.info("   ✅ Common Tier: " + MaterialRegistry.getMaterialsByTier(MaterialTier.COMMON).size() + " materials");
        logger.info("   ✅ Rare Tier: " + MaterialRegistry.getMaterialsByTier(MaterialTier.RARE).size() + " materials");
        logger.info("   ✅ Legendary Tier: " + MaterialRegistry.getMaterialsByTier(MaterialTier.LEGENDARY).size() + " materials");
        logger.info("   ✅ Total: " + MaterialRegistry.size() + " materials");
        logger.info("");
        logger.info("Materials will drop from creatures according to JSON loot tables.");
        logger.info("Materials can be infused into soulbound gear for bonuses.");
        logger.info("");
        logger.info("Public API: MaterialRegistry.getBonus(ResourceLocation id)");
        logger.info("");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}

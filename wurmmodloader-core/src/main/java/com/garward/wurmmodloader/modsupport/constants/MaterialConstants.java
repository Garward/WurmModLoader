package com.garward.wurmmodloader.modsupport.constants;

import com.wurmonline.shared.constants.ItemMaterials;

/**
 * Material constants for item creation and manipulation.
 *
 * <p>This class provides a safe way for mods to access material constants without
 * importing Wurm classes directly. All constants are re-exported from the game's
 * ItemMaterials interface.</p>
 *
 * <p><strong>Architecture Note:</strong> Mods should NEVER import
 * {@code com.wurmonline.server.items.Materials} or {@code com.wurmonline.shared.constants.ItemMaterials}.
 * Always use this framework class instead.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * ItemTemplate sword = new ItemTemplateBuilder(itemId)
 *     .name("Steel Sword", "Steel Swords", "A sharp blade")
 *     .material(MaterialConstants.MATERIAL_STEEL)
 *     .build();
 * }</pre>
 *
 * @author WurmModLoader Framework
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MaterialConstants {

    // Prevent instantiation
    private MaterialConstants() {
        throw new AssertionError("MaterialConstants cannot be instantiated");
    }

    // Basic materials
    public static final byte MATERIAL_UNDEFINED = ItemMaterials.MATERIAL_UNDEFINED;
    public static final byte MATERIAL_FLESH = ItemMaterials.MATERIAL_FLESH;
    public static final byte MATERIAL_MEAT = ItemMaterials.MATERIAL_MEAT;
    public static final byte MATERIAL_RYE = ItemMaterials.MATERIAL_RYE;
    public static final byte MATERIAL_OAT = ItemMaterials.MATERIAL_OAT;
    public static final byte MATERIAL_BARLEY = ItemMaterials.MATERIAL_BARLEY;
    public static final byte MATERIAL_WHEAT = ItemMaterials.MATERIAL_WHEAT;

    // Precious metals
    public static final byte MATERIAL_GOLD = ItemMaterials.MATERIAL_GOLD;
    public static final byte MATERIAL_SILVER = ItemMaterials.MATERIAL_SILVER;
    public static final byte MATERIAL_ELECTRUM = ItemMaterials.MATERIAL_ELECTRUM;

    // Common metals
    public static final byte MATERIAL_STEEL = ItemMaterials.MATERIAL_STEEL;
    public static final byte MATERIAL_COPPER = ItemMaterials.MATERIAL_COPPER;
    public static final byte MATERIAL_IRON = ItemMaterials.MATERIAL_IRON;
    public static final byte MATERIAL_LEAD = ItemMaterials.MATERIAL_LEAD;
    public static final byte MATERIAL_ZINC = ItemMaterials.MATERIAL_ZINC;
    public static final byte MATERIAL_BRASS = ItemMaterials.MATERIAL_BRASS;
    public static final byte MATERIAL_BRONZE = ItemMaterials.MATERIAL_BRONZE;
    public static final byte MATERIAL_TIN = ItemMaterials.MATERIAL_TIN;

    // Fantasy metals
    public static final byte MATERIAL_ADAMANTINE = ItemMaterials.MATERIAL_ADAMANTINE;
    public static final byte MATERIAL_GLIMMERSTEEL = ItemMaterials.MATERIAL_GLIMMERSTEEL;
    public static final byte MATERIAL_SERYLL = ItemMaterials.MATERIAL_SERYLL;

    // Metal fragments
    public static final byte MATERIAL_METALFRAG_BASE = ItemMaterials.MATERIAL_METALFRAG_BASE;
    public static final byte MATERIAL_METALFRAG_ALLOY = ItemMaterials.MATERIAL_METALFRAG_ALLOY;
    public static final byte MATERIAL_METALFRAG_MOON = ItemMaterials.MATERIAL_METALFRAG_MOON;

    // Woods - Common
    public static final byte MATERIAL_WOOD_BIRCH = ItemMaterials.MATERIAL_WOOD_BIRCH;
    public static final byte MATERIAL_WOOD_PINE = ItemMaterials.MATERIAL_WOOD_PINE;
    public static final byte MATERIAL_WOOD_OAK = ItemMaterials.MATERIAL_WOOD_OAK;
    public static final byte MATERIAL_WOOD_CEDAR = ItemMaterials.MATERIAL_WOOD_CEDAR;
    public static final byte MATERIAL_WOOD_WILLOW = ItemMaterials.MATERIAL_WOOD_WILLOW;
    public static final byte MATERIAL_WOOD_MAPLE = ItemMaterials.MATERIAL_WOOD_MAPLE;

    // Woods - Fruit trees
    public static final byte MATERIAL_WOOD_APPLE = ItemMaterials.MATERIAL_WOOD_APPLE;
    public static final byte MATERIAL_WOOD_LEMON = ItemMaterials.MATERIAL_WOOD_LEMON;
    public static final byte MATERIAL_WOOD_OLIVE = ItemMaterials.MATERIAL_WOOD_OLIVE;
    public static final byte MATERIAL_WOOD_CHERRY = ItemMaterials.MATERIAL_WOOD_CHERRY;
    public static final byte MATERIAL_WOOD_GRAPE = ItemMaterials.MATERIAL_WOOD_GRAPE;
    public static final byte MATERIAL_WOOD_ORANGE = ItemMaterials.MATERIAL_WOOD_ORANGE;

    // Woods - Bushes
    public static final byte MATERIAL_WOOD_LAVENDER = ItemMaterials.MATERIAL_WOOD_LAVENDER;
    public static final byte MATERIAL_WOOD_ROSE = ItemMaterials.MATERIAL_WOOD_ROSE;
    public static final byte MATERIAL_WOOD_THORN = ItemMaterials.MATERIAL_WOOD_THORN;
    public static final byte MATERIAL_WOOD_CAMELLIA = ItemMaterials.MATERIAL_WOOD_CAMELLIA;
    public static final byte MATERIAL_WOOD_OLEANDER = ItemMaterials.MATERIAL_WOOD_OLEANDER;
    public static final byte MATERIAL_WOOD_IVY = ItemMaterials.MATERIAL_WOOD_IVY;
    public static final byte MATERIAL_WOOD_RASPBERRY = ItemMaterials.MATERIAL_WOOD_RASPBERRY;
    public static final byte MATERIAL_WOOD_BLUEBERRY = ItemMaterials.MATERIAL_WOOD_BLUEBERRY;
    public static final byte MATERIAL_WOOD_LINGONBERRY = ItemMaterials.MATERIAL_WOOD_LINGONBERRY;

    // Woods - Other
    public static final byte MATERIAL_WOOD_CHESTNUT = ItemMaterials.MATERIAL_WOOD_CHESTNUT;
    public static final byte MATERIAL_WOOD_WALNUT = ItemMaterials.MATERIAL_WOOD_WALNUT;
    public static final byte MATERIAL_WOOD_FIR = ItemMaterials.MATERIAL_WOOD_FIR;
    public static final byte MATERIAL_WOOD_LINDEN = ItemMaterials.MATERIAL_WOOD_LINDEN;
    public static final byte MATERIAL_WOOD_HAZELNUT = ItemMaterials.MATERIAL_WOOD_HAZELNUT;

    // Stone and earth
    public static final byte MATERIAL_STONE = ItemMaterials.MATERIAL_STONE;
    public static final byte MATERIAL_SLATE = ItemMaterials.MATERIAL_SLATE;
    public static final byte MATERIAL_MARBLE = ItemMaterials.MATERIAL_MARBLE;
    public static final byte MATERIAL_SANDSTONE = ItemMaterials.MATERIAL_SANDSTONE;
    public static final byte MATERIAL_CLAY = ItemMaterials.MATERIAL_CLAY;
    public static final byte MATERIAL_POTTERY = ItemMaterials.MATERIAL_POTTERY;

    // Textiles
    public static final byte MATERIAL_LEATHER = ItemMaterials.MATERIAL_LEATHER;
    public static final byte MATERIAL_COTTON = ItemMaterials.MATERIAL_COTTON;
    public static final byte MATERIAL_WEMP = ItemMaterials.MATERIAL_WEMP;
    public static final byte MATERIAL_WOOL = ItemMaterials.MATERIAL_WOOL;
    public static final byte MATERIAL_STRAW = ItemMaterials.MATERIAL_STRAW;

    // Other materials
    public static final byte MATERIAL_GLASS = ItemMaterials.MATERIAL_GLASS;
    public static final byte MATERIAL_MAGIC = ItemMaterials.MATERIAL_MAGIC;
    public static final byte MATERIAL_VEGETARIAN = ItemMaterials.MATERIAL_VEGETARIAN;
    public static final byte MATERIAL_FIRE = ItemMaterials.MATERIAL_FIRE;
    public static final byte MATERIAL_OIL = ItemMaterials.MATERIAL_OIL;
    public static final byte MATERIAL_WATER = ItemMaterials.MATERIAL_WATER;
    public static final byte MATERIAL_COAL = ItemMaterials.MATERIAL_COAL;
    public static final byte MATERIAL_DAIRY = ItemMaterials.MATERIAL_DAIRY;
    public static final byte MATERIAL_HONEY = ItemMaterials.MATERIAL_HONEY;
    public static final byte MATERIAL_FAT = ItemMaterials.MATERIAL_FAT;
    public static final byte MATERIAL_PAPER = ItemMaterials.MATERIAL_PAPER;
    public static final byte MATERIAL_BONE = ItemMaterials.MATERIAL_BONE;
    public static final byte MATERIAL_SALT = ItemMaterials.MATERIAL_SALT;
    public static final byte MATERIAL_CRYSTAL = ItemMaterials.MATERIAL_CRYSTAL;
    public static final byte MATERIAL_DIAMOND = ItemMaterials.MATERIAL_DIAMOND;
    public static final byte MATERIAL_ANIMAL = ItemMaterials.MATERIAL_ANIMAL;
    public static final byte MATERIAL_TAR = ItemMaterials.MATERIAL_TAR;
    public static final byte MATERIAL_PEAT = ItemMaterials.MATERIAL_PEAT;
    public static final byte MATERIAL_REED = ItemMaterials.MATERIAL_REED;

    // Meat types
    public static final byte MATERIAL_MEAT_BEAR = ItemMaterials.MATERIAL_MEAT_BEAR;
    public static final byte MATERIAL_MEAT_BEEF = ItemMaterials.MATERIAL_MEAT_BEEF;
    public static final byte MATERIAL_MEAT_CANINE = ItemMaterials.MATERIAL_MEAT_CANINE;
    public static final byte MATERIAL_MEAT_CAT = ItemMaterials.MATERIAL_MEAT_CAT;
    public static final byte MATERIAL_MEAT_DRAGON = ItemMaterials.MATERIAL_MEAT_DRAGON;
    public static final byte MATERIAL_MEAT_FOWL = ItemMaterials.MATERIAL_MEAT_FOWL;
    public static final byte MATERIAL_MEAT_GAME = ItemMaterials.MATERIAL_MEAT_GAME;
    public static final byte MATERIAL_MEAT_HORSE = ItemMaterials.MATERIAL_MEAT_HORSE;
    public static final byte MATERIAL_MEAT_HUMAN = ItemMaterials.MATERIAL_MEAT_HUMAN;
    public static final byte MATERIAL_MEAT_HUMANOID = ItemMaterials.MATERIAL_MEAT_HUMANOID;
    public static final byte MATERIAL_MEAT_INSECT = ItemMaterials.MATERIAL_MEAT_INSECT;
    public static final byte MATERIAL_MEAT_LAMB = ItemMaterials.MATERIAL_MEAT_LAMB;
    public static final byte MATERIAL_MEAT_PORK = ItemMaterials.MATERIAL_MEAT_PORK;
    public static final byte MATERIAL_MEAT_SEAFOOD = ItemMaterials.MATERIAL_MEAT_SEAFOOD;
    public static final byte MATERIAL_MEAT_SNAKE = ItemMaterials.MATERIAL_MEAT_SNAKE;
    public static final byte MATERIAL_MEAT_TOUGH = ItemMaterials.MATERIAL_MEAT_TOUGH;

    // Bounds
    public static final byte MATERIAL_MAX = ItemMaterials.MATERIAL_MAX;
}

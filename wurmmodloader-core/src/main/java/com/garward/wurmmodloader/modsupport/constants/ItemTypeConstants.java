package com.garward.wurmmodloader.modsupport.constants;

import com.wurmonline.server.items.ItemTypes;

/**
 * Item type constants for item creation and property flags.
 *
 * <p>This class provides a safe way for mods to access item type constants without
 * importing Wurm classes directly. All constants are re-exported from the game's
 * ItemTypes class.</p>
 *
 * <p>Item types are bitflags that define item properties and behaviors. Items can have
 * multiple types set simultaneously (e.g., HOLLOW + LOCKABLE + CONTAINER_LIQUID).</p>
 *
 * <p><strong>Architecture Note:</strong> Mods should NEVER import
 * {@code com.wurmonline.server.items.ItemTypes}. Always use this framework class instead.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * ItemTemplate chest = new ItemTemplateBuilder(itemId)
 *     .name("Magic Chest", "Magic Chests", "A mystical storage container")
 *     .itemTypes(new short[] {
 *         ItemTypeConstants.ITEM_TYPE_HOLLOW,
 *         ItemTypeConstants.ITEM_TYPE_LOCKABLE,
 *         ItemTypeConstants.ITEM_TYPE_DECORATION
 *     })
 *     .build();
 * }</pre>
 *
 * @author WurmModLoader Framework
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ItemTypeConstants {

    // Prevent instantiation
    private ItemTypeConstants() {
        throw new AssertionError("ItemTypeConstants cannot be instantiated");
    }

    // Basic container and item properties
    public static final short ITEM_TYPE_HOLLOW = ItemTypes.ITEM_TYPE_HOLLOW;
    public static final short ITEM_TYPE_CONTAINER_LIQUID = ItemTypes.ITEM_TYPE_CONTAINER_LIQUID;
    public static final short ITEM_TYPE_NOTAKE = ItemTypes.ITEM_TYPE_NOTAKE;
    public static final short ITEM_TYPE_NODROP = ItemTypes.ITEM_TYPE_NODROP;
    public static final short ITEM_TYPE_LOCKABLE = ItemTypes.ITEM_TYPE_LOCKABLE;
    public static final short ITEM_TYPE_LOCK = ItemTypes.ITEM_TYPE_LOCK;
    public static final short ITEM_TYPE_KEY = ItemTypes.ITEM_TYPE_KEY;
    public static final short ITEM_TYPE_HASDATA = ItemTypes.ITEM_TYPE_HASDATA;
    public static final short ITEM_TYPE_COMBINE = ItemTypes.ITEM_TYPE_COMBINE;
    public static final short ITEM_TYPE_COMBINECOLD = ItemTypes.ITEM_TYPE_COMBINECOLD;
    public static final short ITEM_TYPE_TURNABLE = ItemTypes.ITEM_TYPE_TURNABLE;
    public static final short ITEM_TYPE_DECORATION = ItemTypes.ITEM_TYPE_DECORATION;
    public static final short ITEM_TYPE_DRAGGABLE = ItemTypes.ITEM_TYPE_DRAGGABLE;
    public static final short ITEM_TYPE_FLOATING = ItemTypes.ITEM_TYPE_FLOATING;
    public static final short ITEM_TYPE_NOPUT = ItemTypes.ITEM_TYPE_NOPUT;
    public static final short ITEM_TYPE_PASS_FULLDATA = ItemTypes.ITEM_TYPE_PASS_FULLDATA;
    public static final short ITEM_TYPE_BULKCONTAINER = ItemTypes.ITEM_TYPE_BULKCONTAINER;
    public static final short ITEM_TYPE_BULK = ItemTypes.ITEM_TYPE_BULK;
    public static final short ITEM_TYPE_OWNER_DESTROYABLE = ItemTypes.ITEM_TYPE_OWNER_DESTROYABLE;
    public static final short ITEM_TYPE_OWNER_TURNABLE = ItemTypes.ITEM_TYPE_OWNER_TURNABLE;
    public static final short ITEM_TYPE_OWNER_MOVEABLE = ItemTypes.ITEM_TYPE_OWNER_MOVEABLE;
    public static final short ITEM_TYPE_TRANSPORTABLE = ItemTypes.ITEM_TYPE_TRANSPORTABLE;
    public static final short ITEM_TYPE_USES_SPECIFIED_CONTAINER_VOLUME = ItemTypes.ITEM_TYPE_USES_SPECIFIED_CONTAINER_VOLUME;
    public static final short ITEM_TYPE_VIEWABLE_SUBITEMS = ItemTypes.ITEM_TYPE_VIEWABLE_SUBITEMS;
    public static final short ITEM_TYPE_HOLLOW_VIEWABLE = ItemTypes.ITEM_TYPE_HOLLOW_VIEWABLE;
    public static final short ITEM_TYPE_NOMOVE = ItemTypes.ITEM_TYPE_NOMOVE;
    public static final short ITEM_TYPE_NOWORKPARENT = ItemTypes.ITEM_TYPE_NOWORKPARENT;
    public static final short ITEM_TYPE_PARENT_ON_GROUND = ItemTypes.ITEM_TYPE_PARENT_ON_GROUND;

    // Weapon types
    public static final short ITEM_TYPE_WEAPON = ItemTypes.ITEM_TYPE_WEAPON;
    public static final short ITEM_TYPE_WEAPON_SLASH = ItemTypes.ITEM_TYPE_WEAPON_SLASH;
    public static final short ITEM_TYPE_WEAPON_PIERCE = ItemTypes.ITEM_TYPE_WEAPON_PIERCE;
    public static final short ITEM_TYPE_WEAPON_CRUSH = ItemTypes.ITEM_TYPE_WEAPON_CRUSH;
    public static final short ITEM_TYPE_WEAPON_AXE = ItemTypes.ITEM_TYPE_WEAPON_AXE;
    public static final short ITEM_TYPE_WEAPON_SWORD = ItemTypes.ITEM_TYPE_WEAPON_SWORD;
    public static final short ITEM_TYPE_WEAPON_KNIFE = ItemTypes.ITEM_TYPE_WEAPON_KNIFE;
    public static final short ITEM_TYPE_WEAPON_MISC = ItemTypes.ITEM_TYPE_WEAPON_MISC;
    public static final short ITEM_TYPE_WEAPON_MELEE = ItemTypes.ITEM_TYPE_WEAPON_MELEE;
    public static final short ITEM_TYPE_WEAPON_BOW = ItemTypes.ITEM_TYPE_WEAPON_BOW;
    public static final short ITEM_TYPE_WEAPON_BOW_UNSTRINGED = ItemTypes.ITEM_TYPE_WEAPON_BOW_UNSTRINGED;
    public static final short ITEM_TYPE_WEAPON_POLEARM = ItemTypes.ITEM_TYPE_WEAPON_POLEARM;
    public static final short ITEM_TYPE_TWOHANDED = ItemTypes.ITEM_TYPE_TWOHANDED;

    // Armor types
    public static final short ITEM_TYPE_SHIELD = ItemTypes.ITEM_TYPE_SHIELD;
    public static final short ITEM_TYPE_ARMOUR = ItemTypes.ITEM_TYPE_ARMOUR;
    public static final short ITEM_TYPE_DRAGONARMOUR = ItemTypes.ITEM_TYPE_DRAGONARMOUR;
    public static final short ITEM_TYPE_CREATURE_WEARABLE = ItemTypes.ITEM_TYPE_CREATURE_WEARABLE;

    // Tool types
    public static final short ITEM_TYPE_TOOL = ItemTypes.ITEM_TYPE_TOOL;
    public static final short ITEM_TYPE_TOOL_FIELD = ItemTypes.ITEM_TYPE_TOOL_FIELD;
    public static final short ITEM_TYPE_TOOL_MINING = ItemTypes.ITEM_TYPE_TOOL_MINING;
    public static final short ITEM_TYPE_TOOL_CARPENTRY = ItemTypes.ITEM_TYPE_TOOL_CARPENTRY;
    public static final short ITEM_TYPE_TOOL_SMITHING = ItemTypes.ITEM_TYPE_TOOL_SMITHING;
    public static final short ITEM_TYPE_TOOL_DIGGING = ItemTypes.ITEM_TYPE_TOOL_DIGGING;
    public static final short ITEM_TYPE_TOOL_COOKING = ItemTypes.ITEM_TYPE_TOOL_COOKING;
    public static final short ITEM_TYPE_IMPROVEITEM = ItemTypes.ITEM_TYPE_IMPROVEITEM;
    public static final short ITEM_TYPE_NO_IMPROVE = ItemTypes.ITEM_TYPE_NO_IMPROVE;
    public static final short ITEM_TYPE_IMPROVE_USES_TYPE_AS_MATERIAL = ItemTypes.ITEM_TYPE_IMPROVE_USES_TYPE_AS_MATERIAL;
    public static final short ITEM_TYPE_TOOLBELT = ItemTypes.ITEM_TYPE_TOOLBELT;
    public static final short ITEM_TYPE_DISARM_TRAP = ItemTypes.ITEM_TYPE_DISARM_TRAP;

    // Food types
    public static final short ITEM_TYPE_FOOD = ItemTypes.ITEM_TYPE_FOOD;
    public static final short ITEM_TYPE_MEAT = ItemTypes.ITEM_TYPE_MEAT;
    public static final short ITEM_TYPE_VEGETABLE = ItemTypes.ITEM_TYPE_VEGETABLE;
    public static final short ITEM_TYPE_FISH = ItemTypes.ITEM_TYPE_FISH;
    public static final short ITEM_TYPE_HERB = ItemTypes.ITEM_TYPE_HERB;
    public static final short ITEM_TYPE_FRUIT = ItemTypes.ITEM_TYPE_FRUIT;
    public static final short ITEM_TYPE_DISH = ItemTypes.ITEM_TYPE_DISH;
    public static final short ITEM_TYPE_EGG = ItemTypes.ITEM_TYPE_EGG;
    public static final short ITEM_TYPE_SPICE = ItemTypes.ITEM_TYPE_SPICE;
    public static final short ITEM_TYPE_MUSHROOM = ItemTypes.ITEM_TYPE_MUSHROOM;
    public static final short ITEM_TYPE_MILK = ItemTypes.ITEM_TYPE_MILK;
    public static final short ITEM_TYPE_CHEESE = ItemTypes.ITEM_TYPE_CHEESE;

    // Food properties
    public static final short ITEM_TYPE_NONUTRITION = ItemTypes.ITEM_TYPE_NONUTRITION;
    public static final short ITEM_TYPE_LOWNUTRITION = ItemTypes.ITEM_TYPE_LOWNUTRITION;
    public static final short ITEM_TYPE_MEDIUMNUTRITION = ItemTypes.ITEM_TYPE_MEDIUMNUTRITION;
    public static final short ITEM_TYPE_GOODNUTRITION = ItemTypes.ITEM_TYPE_GOODNUTRITION;
    public static final short ITEM_TYPE_HIGHNUTRITION = ItemTypes.ITEM_TYPE_HIGHNUTRITION;
    public static final short ITEM_TYPE_FOODMAKER = ItemTypes.ITEM_TYPE_FOODMAKER;
    public static final short ITEM_TYPE_POISON = ItemTypes.ITEM_TYPE_POISON;
    public static final short ITEM_TYPE_BUTCHERED = ItemTypes.ITEM_TYPE_BUTCHERED;
    public static final short ITEM_TYPE_COOKER = ItemTypes.ITEM_TYPE_COOKER;
    public static final short ITEM_TYPE_RECIPE_ITEM = ItemTypes.ITEM_TYPE_RECIPE_ITEM;
    public static final short ITEM_TYPE_USES_FOOD_STATE = ItemTypes.ITEM_TYPE_USES_FOOD_STATE;
    public static final short ITEM_TYPE_FERMENTED = ItemTypes.ITEM_TYPE_FERMENTED;
    public static final short ITEM_TYPE_DISTILLED = ItemTypes.ITEM_TYPE_DISTILLED;
    public static final short ITEM_TYPE_SEALABLE = ItemTypes.ITEM_TYPE_SEALABLE;
    public static final short ITEM_TYPE_FOOD_BONUS_HOT = ItemTypes.ITEM_TYPE_FOOD_BONUS_HOT;
    public static final short ITEM_TYPE_FOOD_BONUS_COLD = ItemTypes.ITEM_TYPE_FOOD_BONUS_COLD;
    public static final short ITEM_TYPE_HARVESTABLE = ItemTypes.ITEM_TYPE_HARVESTABLE;
    public static final short ITEM_TYPE_INGREDIENTS_ONLY = ItemTypes.ITEM_TYPE_INGREDIENTS_ONLY;
    public static final short ITEM_TYPE_COMPONENT_ITEM = ItemTypes.ITEM_TYPE_COMPONENT_ITEM;
    public static final short ITEM_TYPE_FOOD_GROUP = ItemTypes.ITEM_TYPE_FOOD_GROUP;
    public static final short ITEM_TYPE_LARDER = ItemTypes.ITEM_TYPE_LARDER;
    public static final short ITEM_TYPE_INSULATED = ItemTypes.ITEM_TYPE_INSULATED;
    public static final short ITEM_TYPE_POTABLE = ItemTypes.ITEM_TYPE_POTABLE;
    public static final short ITEM_TYPE_SHOW_RAW = ItemTypes.ITEM_TYPE_SHOW_RAW;
    public static final short ITEM_TYPE_CAN_BE_PAPYRUS_WRAPPED = ItemTypes.ITEM_TYPE_CAN_BE_PAPYRUS_WRAPPED;
    public static final short ITEM_TYPE_CAN_BE_RAW_WRAPPED = ItemTypes.ITEM_TYPE_CAN_BE_RAW_WRAPPED;
    public static final short ITEM_TYPE_CAN_BE_CLOTH_WRAPPED = ItemTypes.ITEM_TYPE_CAN_BE_CLOTH_WRAPPED;

    // Liquid types
    public static final short ITEM_TYPE_LIQUID = ItemTypes.ITEM_TYPE_LIQUID;
    public static final short ITEM_TYPE_LIQUID_INFLAMMABLE = ItemTypes.ITEM_TYPE_LIQUID_INFLAMMABLE;
    public static final short ITEM_TYPE_LIQUID_COOKING = ItemTypes.ITEM_TYPE_LIQUID_COOKING;
    public static final short ITEM_TYPE_LIQUID_DRINKABLE = ItemTypes.ITEM_TYPE_LIQUID_DRINKABLE;
    public static final short ITEM_TYPE_COOKING_OIL = ItemTypes.ITEM_TYPE_COOKING_OIL;

    // Material types
    public static final short ITEM_TYPE_WOOD = ItemTypes.ITEM_TYPE_WOOD;
    public static final short ITEM_TYPE_METAL = ItemTypes.ITEM_TYPE_METAL;
    public static final short ITEM_TYPE_LEATHER = ItemTypes.ITEM_TYPE_LEATHER;
    public static final short ITEM_TYPE_CLOTH = ItemTypes.ITEM_TYPE_CLOTH;
    public static final short ITEM_TYPE_STONE = ItemTypes.ITEM_TYPE_STONE;
    public static final short ITEM_TYPE_POTTERY = ItemTypes.ITEM_TYPE_POTTERY;
    public static final short ITEM_TYPE_MELTING = ItemTypes.ITEM_TYPE_MELTING;
    public static final short ITEM_TYPE_GEM = ItemTypes.ITEM_TYPE_GEM;
    public static final short ITEM_TYPE_BRACELET = ItemTypes.ITEM_TYPE_BRACELET;

    // Plant and farming
    public static final short ITEM_TYPE_SEED = ItemTypes.ITEM_TYPE_SEED;
    public static final short ITEM_TYPE_FLOWER = ItemTypes.ITEM_TYPE_FLOWER;
    public static final short ITEM_TYPE_PLANTABLE = ItemTypes.ITEM_TYPE_PLANTABLE;
    public static final short ITEM_TYPE_PLANT_ONE_A_WEEK = ItemTypes.ITEM_TYPE_PLANT_ONE_A_WEEK;
    public static final short ITEM_TYPE_PLANTED_FLOWERPOT = ItemTypes.ITEM_TYPE_PLANTED_FLOWERPOT;
    public static final short ITEM_TYPE_POTTED = ItemTypes.ITEM_TYPE_POTTED;
    public static final short ITEM_TYPE_NATURE_PLANTABLE = ItemTypes.ITEM_TYPE_NATURE_PLANTABLE;
    public static final short ITEM_TYPE_TRELLIS = ItemTypes.ITEM_TYPE_TRELLIS;
    public static final short ITEM_TYPE_SPAWNSTREES = ItemTypes.ITEM_TYPE_SPAWNSTREES;
    public static final short ITEM_TYPE_KILLSTREES = ItemTypes.ITEM_TYPE_KILLSTREES;
    public static final short ITEM_TYPE_DECORATION_WHEN_PLANTED = ItemTypes.ITEM_TYPE_DECORATION_WHEN_PLANTED;

    // Light and fire
    public static final short ITEM_TYPE_LIGHT = ItemTypes.ITEM_TYPE_LIGHT;
    public static final short ITEM_TYPE_LIGHT_BRIGHT = ItemTypes.ITEM_TYPE_LIGHT_BRIGHT;
    public static final short ITEM_TYPE_ALWAYS_LIT = ItemTypes.ITEM_TYPE_ALWAYS_LIT;
    public static final short ITEM_TYPE_FLICKERING = ItemTypes.ITEM_TYPE_FLICKERING;
    public static final short ITEM_TYPE_FIRE = ItemTypes.ITEM_TYPE_FIRE;
    public static final short ITEM_TYPE_OILCONSUMING = ItemTypes.ITEM_TYPE_OILCONSUMING;
    public static final short ITEM_TYPE_BRAZIER = ItemTypes.ITEM_TYPE_BRAZIER;
    public static final short ITEM_TYPE_STREETLAMP = ItemTypes.ITEM_TYPE_STREETLAMP;

    // Vehicles
    public static final short ITEM_TYPE_VEHICLE = ItemTypes.ITEM_TYPE_VEHICLE;
    public static final short ITEM_TYPE_VEHICLE_DRAGGED = ItemTypes.ITEM_TYPE_VEHICLE_DRAGGED;
    public static final short ITEM_TYPE_CART = ItemTypes.ITEM_TYPE_CART;
    public static final short ITEM_TYPE_LEADCREATURE = ItemTypes.ITEM_TYPE_LEADCREATURE;
    public static final short ITEM_TYPE_LEAD_MULTIPLE_CREATURES = ItemTypes.ITEM_TYPE_LEAD_MULTIPLE_CREATURES;
    public static final short ITEM_TYPE_HITCH_TARGET = ItemTypes.ITEM_TYPE_HITCH_TARGET;
    public static final short ITEM_TYPE_WIND = ItemTypes.ITEM_TYPE_WIND;
    public static final short ITEM_TYPE_DREDGE = ItemTypes.ITEM_TYPE_DREDGE;
    public static final short ITEM_TYPE_WARMACHINE = ItemTypes.ITEM_TYPE_WARMACHINE;
    public static final short ITEM_TYPE_HOVER = ItemTypes.ITEM_TYPE_HOVER;
    public static final short ITEM_TYPE_LOADED = ItemTypes.ITEM_TYPE_LOADED;

    // Building and structures
    public static final short ITEM_TYPE_BED = ItemTypes.ITEM_TYPE_BED;
    public static final short ITEM_TYPE_CHAIR = ItemTypes.ITEM_TYPE_CHAIR;
    public static final short ITEM_TYPE_OUTSIDE_ONLY = ItemTypes.ITEM_TYPE_OUTSIDE_ONLY;
    public static final short ITEM_TYPE_INSIDE_ONLY = ItemTypes.ITEM_TYPE_INSIDE_ONLY;
    public static final short ITEM_TYPE_SURFACE_ONLY = ItemTypes.ITEM_TYPE_SURFACE_ONLY;
    public static final short ITEM_TYPE_USE_GROUND_ONLY = ItemTypes.ITEM_TYPE_USE_GROUND_ONLY;
    public static final short ITEM_TYPE_TILE_ALIGNED = ItemTypes.ITEM_TYPE_TILE_ALIGNED;
    public static final short ITEM_TYPE_ONE_PER_TILE = ItemTypes.ITEM_TYPE_ONE_PER_TILE;
    public static final short ITEM_TYPE_TEN_PER_TILE = ItemTypes.ITEM_TYPE_TEN_PER_TILE;
    public static final short ITEM_TYPE_FOUR_PER_TILE = ItemTypes.ITEM_TYPE_FOUR_PER_TILE;
    public static final short ITEM_TYPE_ONE_PER_TILEBORDER = ItemTypes.ITEM_TYPE_ONE_PER_TILEBORDER;
    public static final short ITEM_TYPE_SIGN = ItemTypes.ITEM_TYPE_SIGN;
    public static final short ITEM_TYPE_CARPET = ItemTypes.ITEM_TYPE_CARPET;
    public static final short ITEM_TYPE_TAPESTRY = ItemTypes.ITEM_TYPE_TAPESTRY;
    public static final short ITEM_TYPE_TENT = ItemTypes.ITEM_TYPE_TENT;
    public static final short ITEM_TYPE_MINEDOOR = ItemTypes.ITEM_TYPE_MINEDOOR;

    // Special structures
    public static final short ITEM_TYPE_HUGEALTAR = ItemTypes.ITEM_TYPE_HUGEALTAR;
    public static final short ITEM_TYPE_DESTROY_HUGEALTAR = ItemTypes.ITEM_TYPE_DESTROY_HUGEALTAR;
    public static final short ITEM_TYPE_GUARD_TOWER = ItemTypes.ITEM_TYPE_GUARD_TOWER;
    public static final short ITEM_TYPE_VILLAGEDEED = ItemTypes.ITEM_TYPE_VILLAGEDEED;
    public static final short ITEM_TYPE_HOMESTEADDEED = ItemTypes.ITEM_TYPE_HOMESTEADDEED;
    public static final short ITEM_TYPE_DOMAIN = ItemTypes.ITEM_TYPE_DOMAIN;
    public static final short ITEM_TYPE_KINGDOM_MARKER = ItemTypes.ITEM_TYPE_KINGDOM_MARKER;

    // Decay and durability
    public static final short ITEM_TYPE_INDESTRUCTIBLE = ItemTypes.ITEM_TYPE_INDESTRUCTIBLE;
    public static final short ITEM_TYPE_REPAIRABLE = ItemTypes.ITEM_TYPE_REPAIRABLE;
    public static final short ITEM_TYPE_TEMPORARY = ItemTypes.ITEM_TYPE_TEMPORARY;
    public static final short ITEM_TYPE_POSITIVE_DECAY = ItemTypes.ITEM_TYPE_POSITIVE_DECAY;
    public static final short ITEM_TYPE_VISIBLEDECAY = ItemTypes.ITEM_TYPE_VISIBLEDECAY;
    public static final short ITEM_TYPE_DECAYDESTROYS = ItemTypes.ITEM_TYPE_DECAYDESTROYS;
    public static final short ITEM_TYPE_DESTROYABLE = ItemTypes.ITEM_TYPE_DESTROYABLE;
    public static final short ITEM_TYPE_DECAY_ON_DEED = ItemTypes.ITEM_TYPE_DECAY_ON_DEED;

    // Trading and economy
    public static final short ITEM_TYPE_NOTRADE = ItemTypes.ITEM_TYPE_NOTRADE;
    public static final short ITEM_TYPE_FULLPRICE = ItemTypes.ITEM_TYPE_FULLPRICE;
    public static final short ITEM_TYPE_MATERIAL_PRICEEFFECT = ItemTypes.ITEM_TYPE_MATERIAL_PRICEEFFECT;
    public static final short ITEM_TYPE_NOBANK = ItemTypes.ITEM_TYPE_NOBANK;
    public static final short ITEM_TYPE_ALWAYS_BANKABLE = ItemTypes.ITEM_TYPE_ALWAYS_BANKABLE;
    public static final short ITEM_TYPE_NOSELLBACK = ItemTypes.ITEM_TYPE_NOSELLBACK;
    public static final short ITEM_TYPE_COIN = ItemTypes.ITEM_TYPE_COIN;

    // Naming and description
    public static final short ITEM_TYPE_NORENAME = ItemTypes.ITEM_TYPE_NORENAME;
    public static final short ITEM_TYPE_NAMED = ItemTypes.ITEM_TYPE_NAMED;
    public static final short ITEM_TYPE_DESC_IS_EXAM = ItemTypes.ITEM_TYPE_DESC_IS_EXAM;
    public static final short ITEM_TYPE_DESC_IS_NAME = ItemTypes.ITEM_TYPE_DESC_IS_NAME;
    public static final short ITEM_TYPE_PLURAL_NAME = ItemTypes.ITEM_TYPE_PLURAL_NAME;
    public static final short ITEM_TYPE_CAN_HAVE_INSCRIPTION = ItemTypes.ITEM_TYPE_CAN_HAVE_INSCRIPTION;

    // Color and appearance
    public static final short ITEM_TYPE_COLOR = ItemTypes.ITEM_TYPE_COLOR;
    public static final short ITEM_TYPE_COLORABLE = ItemTypes.ITEM_TYPE_COLORABLE;
    public static final short ITEM_TYPE_SUPPORTS_SECONDARY_COLOR = ItemTypes.ITEM_TYPE_SUPPORTS_SECONDARY_COLOR;
    public static final short ITEM_TYPE_COLORCOMPONENT = ItemTypes.ITEM_TYPE_COLORCOMPONENT;
    public static final short ITEM_TYPE_SMEARABLE = ItemTypes.ITEM_TYPE_SMEARABLE;
    public static final short ITEM_TYPE_USE_REAL_TEMPLATE_ICON = ItemTypes.ITEM_TYPE_USE_REAL_TEMPLATE_ICON;
    public static final short ITEM_TYPE_USES_REAL_TEMPLATE = ItemTypes.ITEM_TYPE_USES_REAL_TEMPLATE;

    // Magic and special
    public static final short ITEM_TYPE_MAGIC = ItemTypes.ITEM_TYPE_MAGIC;
    public static final short ITEM_TYPE_ARTIFACT = ItemTypes.ITEM_TYPE_ARTIFACT;
    public static final short ITEM_TYPE_UNIQUE = ItemTypes.ITEM_TYPE_UNIQUE;
    public static final short ITEM_TYPE_ENCHANT_JEWELRY = ItemTypes.ITEM_TYPE_ENCHANT_JEWELRY;
    public static final short ITEM_TYPE_OVERRIDEENCHANT = ItemTypes.ITEM_TYPE_OVERRIDEENCHANT;
    public static final short ITEM_TYPE_MAGICAL_STAFF = ItemTypes.ITEM_TYPE_MAGICAL_STAFF;
    public static final short ITEM_TYPE_TRANSMUTABLE = ItemTypes.ITEM_TYPE_TRANSMUTABLE;
    public static final short ITEM_TYPE_RUNE = ItemTypes.ITEM_TYPE_RUNE;
    public static final short ITEM_TYPE_NOT_RUNEABLE = ItemTypes.ITEM_TYPE_NOT_RUNEABLE;
    public static final short ITEM_TYPE_NOT_SPELL_TARGET = ItemTypes.ITEM_TYPE_NOT_SPELL_TARGET;

    // Healing
    public static final short ITEM_TYPE_HEALING_POWER_1 = ItemTypes.ITEM_TYPE_HEALING_POWER_1;
    public static final short ITEM_TYPE_HEALING_POWER_2 = ItemTypes.ITEM_TYPE_HEALING_POWER_2;
    public static final short ITEM_TYPE_HEALING_POWER_3 = ItemTypes.ITEM_TYPE_HEALING_POWER_3;
    public static final short ITEM_TYPE_HEALING_POWER_4 = ItemTypes.ITEM_TYPE_HEALING_POWER_4;
    public static final short ITEM_TYPE_HEALING_POWER_5 = ItemTypes.ITEM_TYPE_HEALING_POWER_5;

    // Server and technical
    public static final short ITEM_TYPE_BODYPART = ItemTypes.ITEM_TYPE_BODYPART;
    public static final short ITEM_TYPE_INVENTORY = ItemTypes.ITEM_TYPE_INVENTORY;
    public static final short ITEM_TYPE_INVENTORY_GROUP = ItemTypes.ITEM_TYPE_INVENTORY_GROUP;
    public static final short ITEM_TYPE_EQUIPMENTSLOT = ItemTypes.ITEM_TYPE_EQUIPMENTSLOT;
    public static final short ITEM_TYPE_SERVERBOUND = ItemTypes.ITEM_TYPE_SERVERBOUND;
    public static final short ITEM_TYPE_SERVERPORTAL = ItemTypes.ITEM_TYPE_SERVERPORTAL;
    public static final short ITEM_TYPE_ALWAYSPOLL = ItemTypes.ITEM_TYPE_ALWAYSPOLL;
    public static final short ITEM_TYPE_RECHARGEABLE = ItemTypes.ITEM_TYPE_RECHARGEABLE;
    public static final short ITEM_TYPE_FORM = ItemTypes.ITEM_TYPE_FORM;
    public static final short ITEM_TYPE_ABILITY = ItemTypes.ITEM_TYPE_ABILITY;
    public static final short ITEM_TYPE_HAS_EXTRA_DATA = ItemTypes.ITEM_TYPE_HAS_EXTRA_DATA;

    // Player and tutorial
    public static final short ITEM_TYPE_NEWBIEITEM = ItemTypes.ITEM_TYPE_NEWBIEITEM;
    public static final short ITEM_TYPE_DEATHPROT = ItemTypes.ITEM_TYPE_DEATHPROT;
    public static final short ITEM_TYPE_ROYAL = ItemTypes.ITEM_TYPE_ROYAL;
    public static final short ITEM_TYPE_TUTORIAL = ItemTypes.ITEM_TYPE_TUTORIAL;
    public static final short ITEM_TYPE_CHALL_NEWBIE = ItemTypes.ITEM_TYPE_CHALL_NEWBIE;
    public static final short ITEM_TYPE_NODISCARD = ItemTypes.ITEM_TYPE_NODISCARD;
    public static final short ITEM_TYPE_INSTADISCARD = ItemTypes.ITEM_TYPE_INSTADISCARD;

    // Miscellaneous
    public static final short ITEM_TYPE_COMPASS = ItemTypes.ITEM_TYPE_COMPASS;
    public static final short ITEM_TYPE_TRAP = ItemTypes.ITEM_TYPE_TRAP;
    public static final short ITEM_TYPE_PUPPET = ItemTypes.ITEM_TYPE_PUPPET;
    public static final short ITEM_TYPE_MEDITATION = ItemTypes.ITEM_TYPE_MEDITATION;
    public static final short ITEM_TYPE_MISSION = ItemTypes.ITEM_TYPE_MISSION;
    public static final short ITEM_TYPE_NOT_MISSION = ItemTypes.ITEM_TYPE_NOT_MISSION;
    public static final short ITEM_TYPE_CRUDE = ItemTypes.ITEM_TYPE_CRUDE;
    public static final short ITEM_TYPE_MINABLE = ItemTypes.ITEM_TYPE_MINABLE;
    public static final short ITEM_TYPE_WARTARGET = ItemTypes.ITEM_TYPE_WARTARGET;
    public static final short ITEM_TYPE_SOURCESPRING = ItemTypes.ITEM_TYPE_SOURCESPRING;
    public static final short ITEM_TYPE_SOURCE = ItemTypes.ITEM_TYPE_SOURCE;
    public static final short ITEM_TYPE_SPRINGFILLED = ItemTypes.ITEM_TYPE_SPRINGFILLED;
    public static final short ITEM_TYPE_MASSPRODUCTION = ItemTypes.ITEM_TYPE_MASSPRODUCTION;
    public static final short ITEM_TYPE_NEVER_SHOW_CREATION_WINDOW_OPTION = ItemTypes.ITEM_TYPE_NEVER_SHOW_CREATION_WINDOW_OPTION;
    public static final short ITEM_TYPE_NO_CREATE = ItemTypes.ITEM_TYPE_NO_CREATE;
    public static final short ITEM_TYPE_RECYCLED = ItemTypes.ITEM_TYPE_RECYCLED;
    public static final short ITEM_TYPE_UNFINISHED_NOTAKE = ItemTypes.ITEM_TYPE_UNFINISHED_NOTAKE;
    public static final short ITEM_TYPE_UNFIRED = ItemTypes.ITEM_TYPE_UNFIRED;
    public static final short ITEM_TYPE_USEMATERIAL_AND_KINGDOM = ItemTypes.ITEM_TYPE_USEMATERIAL_AND_KINGDOM;
    public static final short ITEM_TYPE_CREATES_WITH_LOCK = ItemTypes.ITEM_TYPE_CREATES_WITH_LOCK;
    public static final short ITEM_TYPE_PEGABLE = ItemTypes.ITEM_TYPE_PEGABLE;

    // Fishing
    public static final short ITEM_TYPE_FISHING_REEL = ItemTypes.ITEM_TYPE_FISHING_REEL;
    public static final short ITEM_TYPE_FISHING_LINE = ItemTypes.ITEM_TYPE_FISHING_LINE;
    public static final short ITEM_TYPE_FISHING_FLOAT = ItemTypes.ITEM_TYPE_FISHING_FLOAT;
    public static final short ITEM_TYPE_FISHING_HOOK = ItemTypes.ITEM_TYPE_FISHING_HOOK;
    public static final short ITEM_TYPE_FISHING_BAIT = ItemTypes.ITEM_TYPE_FISHING_BAIT;

    // Roads and pavements
    public static final short ITEM_TYPE_ROAD_MARKER = ItemTypes.ITEM_TYPE_ROAD_MARKER;
    public static final short ITEM_TYPE_PAVEABLE = ItemTypes.ITEM_TYPE_PAVEABLE;
    public static final short ITEM_TYPE_CAVE_PAVEABLE = ItemTypes.ITEM_TYPE_CAVE_PAVEABLE;
    public static final short ITEM_TYPE_SHOWS_SLOPES = ItemTypes.ITEM_TYPE_SHOWS_SLOPES;
}

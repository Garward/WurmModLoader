package com.garward.wurmmodloader.modsupport;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.garward.wurmmodloader.modloader.internal.ReflectionUtil;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.registry.IdAllocator;
import com.garward.wurmmodloader.core.registry.Registries;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;

/**
 * Builder class for creating {@link ItemTemplate} instances in Wurm Unlimited mods.
 * 
 * <p>This builder provides a fluent API for constructing item templates with various properties
 * such as size, descriptions, types, dimensions, and behaviors. It simplifies the process of
 * creating custom items for mods by encapsulating the complex {@link ItemTemplateFactory} API.</p>
 * 
 * <p>Usage example:</p>
 * <pre><code>
 * ItemTemplate myItem = new ItemTemplateBuilder("my.mod.item")
 *     .name("Magic Sword", "Magic Swords", "A powerful enchanted blade")
 *     .size(3)
 *     .descriptions("legendary", "fine", "worn", "broken")
 *     .itemTypes(new short[] {ItemTypes.ITEM_TYPE_WEAPON, ItemTypes.ITEM_TYPE_METAL})
 *     .weightGrams(2500)
 *     .material(Materials.MATERIAL_IRON)
 *     .value(1000)
 *     .difficulty(50.0f)
 *     .dimensions(10, 2, 1)
 *     .combatDamage(50)
 *     .build();
 * </code></pre>
 * 
 * <p><strong>Thread Safety:</strong> This class is NOT thread-safe. Each instance should only
 * be used by a single thread. If multiple threads need to create item templates, each should
 * create their own instance of this builder.</p>
 * 
 * <p><strong>Lifecycle:</strong> Instances are typically created during mod initialization
 * phase, usually in the {@code onItemTemplatesCreated()} method of a mod class. Once built,
 * the resulting {@link ItemTemplate} is registered with the game and should not be modified.</p>
 * 
 * @since 1.0.0
 * @see ItemTemplate
 * @see ItemTemplateFactory
 */
public class ItemTemplateBuilder {

	private int templateId;
	private ResourceLocation resourceLocation;
	private String name;
	private String plural;
	private int size = 3;
	private String itemDescriptionSuperb = "superb"; 
	private String itemDescriptionNormal = "good";
	private String itemDescriptionBad = "ok";
	private String itemDescriptionRotten = "poor";
	private String itemDescriptionLong;
	private short[] itemTypes;
	private short imageNumber;
	private short behaviourType;
	private int combatDamage = 0;
	private long decayTime = 9072000L;
	private int centimetersX;
	private int centimetersY;
	private int centimetersZ;
	private int primarySkill = -10;
	private byte[] bodySpaces = MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY;
	private String modelName;
	private float difficulty;
	private int weightGrams;
	private byte material;
	private int value = 0;
	private boolean isTraded = false;
	private int dyeAmountOverrideGrams = 0;
	private boolean hasContainerSizes = false;
	private int containerSizeX;
	private int containerSizeY;
	private int containerSizeZ;
	private int maxItemCount = -1;
	private int maxItemWeight = -1;
	private int dyePrimaryAmountRequired = 0;
	private int dyeSecondaryAmountRequired = 0;
	private String secondaryItemName;
	private int foodGroup;
	private boolean hasNutritionValues;
	private int calories;
	private int carbs;
	private int fats;
	private int proteins;
	private int alcoholStrength;
	private int grows;
	private int crushsTo;
	private int harvestTo;
	private int pickSeeds;

	/**
	 * Constructs a new ItemTemplateBuilder with a ResourceLocation.
	 *
	 * <p>This is the recommended constructor for new code. It uses the modern registry system
	 * with {@link IdAllocator} for ID management and automatically registers the template
	 * in {@link Registries#ITEMS} when {@link #build()} is called.</p>
	 *
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * ResourceLocation id = new ResourceLocation("mymod", "magic_sword");
	 * ItemTemplate sword = new ItemTemplateBuilder(id)
	 *     .name("Magic Sword", "Magic Swords", "A powerful enchanted blade")
	 *     .weightGrams(2500)
	 *     .build();
	 *
	 * // Template is now registered and can be retrieved:
	 * Optional<ItemTemplate> retrieved = Registries.ITEMS.get(id);
	 * }</pre>
	 *
	 * @param location the namespaced resource location for this item
	 * @since 2.0.0
	 * @see ResourceLocation
	 * @see IdAllocator
	 * @see Registries#ITEMS
	 */
	public ItemTemplateBuilder(ResourceLocation location) {
		this.resourceLocation = location;
		this.templateId = IdAllocator.getInstance().allocate(location);
	}

	/**
	 * Constructs a new ItemTemplateBuilder with the specified identifier.
	 *
	 * <p>The identifier is used to generate a unique template ID for this item.
	 * It should be unique across all mods to prevent conflicts.</p>
	 *
	 * <p><strong>Deprecated:</strong> This constructor uses the legacy string-based identification
	 * system. New code should use {@link #ItemTemplateBuilder(ResourceLocation)} instead, which
	 * provides proper namespace isolation and registry integration.</p>
	 *
	 * @param identifier a unique string identifier for this item template
	 * @since 1.0.0
	 * @deprecated Use {@link #ItemTemplateBuilder(ResourceLocation)} instead for proper namespace
	 *             isolation and registry integration. This constructor will convert the identifier
	 *             to a ResourceLocation with the default "wurm" namespace.
	 * @see ResourceLocation#of(String)
	 * @see IdAllocator#allocate(ResourceLocation)
	 */
	@Deprecated
	public ItemTemplateBuilder(String identifier) {
		this(ResourceLocation.of(identifier));
	}

	/**
	 * Sets the basic naming and description properties for the item.
	 * 
	 * <p>This method sets the item's name, plural form, and long description.
	 * These are fundamental properties that define how the item appears in-game.</p>
	 * 
	 * @param name the singular name of the item (e.g., "sword")
	 * @param plural the plural form of the item name (e.g., "swords")
	 * @param description the long description of the item (e.g., "A sharp blade forged from steel")
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder name(String name, String plural, String description) {
		this.name = name;
		this.plural = plural;
		this.itemDescriptionLong = description;
		return this;
	}
	
	/**
	 * Sets the size category of the item.
	 * 
	 * <p>Size affects how the item is handled in the game world, including
	 * how it's displayed in containers and how much space it occupies.</p>
	 * 
	 * @param size the size category (typically 1-5, where 1 is tiny and 5 is huge)
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder size(int size) {
		this.size = size;
		return this;
	}

	/**
	 * Sets the quality-based descriptions for the item.
	 * 
	 * <p>These descriptions are used to describe the item based on its quality level.
	 * They appear when examining items in the game.</p>
	 * 
	 * @param itemDescriptionSuperb description for superb quality items (e.g., "flawless")
	 * @param itemDescriptionNormal description for normal quality items (e.g., "good")
	 * @param itemDescriptionBad description for bad quality items (e.g., "worn")
	 * @param itemDescriptionRotten description for rotten/damaged items (e.g., "broken")
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder descriptions(String itemDescriptionSuperb, String itemDescriptionNormal, String itemDescriptionBad, String itemDescriptionRotten) {
		this.itemDescriptionSuperb = itemDescriptionSuperb;
		this.itemDescriptionNormal = itemDescriptionNormal;
		this.itemDescriptionBad = itemDescriptionBad;
		this.itemDescriptionRotten = itemDescriptionRotten;
		return this;
	}

	/**
	 * Sets the item type flags for this item.
	 * 
	 * <p>Item types define the behavior and properties of the item, such as
	 * whether it's a weapon, food, container, etc. These are defined in {@code ItemTypes}.</p>
	 * 
	 * @param itemTypes array of item type flags (e.g., {@code new short[] {ItemTypes.ITEM_TYPE_WEAPON}})
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see com.wurmonline.server.items.ItemTypes
	 */
	public ItemTemplateBuilder itemTypes(short[] itemTypes) {
		this.itemTypes = itemTypes;
		return this;
	}

	/**
	 * Sets the weight of the item in grams.
	 * 
	 * <p>Weight affects how much the item contributes to a player's carried weight
	 * and how it behaves physically in the game world.</p>
	 * 
	 * @param weightGrams the weight of the item in grams
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder weightGrams(int weightGrams) {
		this.weightGrams = weightGrams;
		return this;
	}

	/**
	 * Sets the material type of the item.
	 * 
	 * <p>Material affects the item's appearance, properties, and crafting requirements.
	 * Materials are defined in {@code Materials}.</p>
	 * 
	 * @param material2 the material type (e.g., {@code Materials.MATERIAL_IRON})
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see com.wurmonline.server.items.Materials
	 */
	public ItemTemplateBuilder material(byte material2) {
		this.material = material2;
		return this;
	}

	/**
	 * Sets the base value of the item in copper coins.
	 * 
	 * <p>This determines the item's worth when trading with merchants or other players.
	 * Higher values make the item more expensive.</p>
	 * 
	 * @param value2 the base value in copper coins
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder value(int value2) {
		this.value = value2;
		return this;
	}

	/**
	 * Sets whether the item is traded on the kingdom trade network.
	 * 
	 * <p>Traded items can be bought and sold through merchant networks,
	 * making them more readily available to players.</p>
	 * 
	 * @param isTraded2 {@code true} if the item should be traded, {@code false} otherwise
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder isTraded(boolean isTraded2) {
		this.isTraded = isTraded2;
		return this;
	}

	/**
	 * Deprecated method for setting armor type.
	 * 
	 * <p>This method is deprecated and has no effect. Armor type should be
	 * set through other means or is determined by the item's properties.</p>
	 * 
	 * @param armourType2 the armor type (ignored)
	 * @return this builder instance for method chaining
	 * @deprecated This method is no longer used and has no effect
	 * @since 1.0.0
	 */
	@Deprecated
	public ItemTemplateBuilder armourType(int armourType2) {
		return this;
	}
	
	/**
	 * Sets the difficulty level for crafting this item.
	 * 
	 * <p>Higher difficulty values require more skill to craft successfully
	 * and may have lower success rates.</p>
	 * 
	 * @param difficulty2 the crafting difficulty (0.0 = trivial, 100.0 = nearly impossible)
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder difficulty(float difficulty2) {
		this.difficulty = difficulty2;
		return this;
	}

	/**
	 * Sets the 3D model name for this item.
	 * 
	 * <p>This determines how the item appears visually in the game world.
	 * The model must exist in the client's data files.</p>
	 * 
	 * @param modelName2 the name of the 3D model file (without extension)
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder modelName(String modelName2) {
		this.modelName = modelName2;
		return this;
	}

	/**
	 * Sets the body spaces this item occupies when equipped.
	 * 
	 * <p>For wearable items, this determines which slots on the character
	 * the item uses when equipped. Defined in {@code BodySpaces}.</p>
	 * 
	 * @param bodySpaces2 array of body space identifiers
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see com.wurmonline.server.items.BodySpaces
	 */
	public ItemTemplateBuilder bodySpaces(byte[] bodySpaces2) {
		this.bodySpaces = bodySpaces2;
		return this;
	}

	/**
	 * Sets the primary skill used for crafting this item.
	 * 
	 * <p>This determines which skill governs the crafting of this item.
	 * Skills are defined in {@code SkillList}.</p>
	 * 
	 * @param primarySkill2 the skill identifier (e.g., {@code SkillList.SMITHING})
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see com.wurmonline.server.skills.SkillList
	 */
	public ItemTemplateBuilder primarySkill(int primarySkill2) {
		this.primarySkill = primarySkill2;
		return this;
	}

	/**
	 * Sets the physical dimensions of the item in centimeters.
	 * 
	 * <p>These dimensions affect how the item appears in the world and
	 * how it interacts with other objects.</p>
	 * 
	 * @param centimetersX2 width in centimeters
	 * @param centimetersY2 height in centimeters
	 * @param centimetersZ2 length in centimeters
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder dimensions(int centimetersX2, int centimetersY2, int centimetersZ2) {
		this.centimetersX = centimetersX2;
		this.centimetersY = centimetersY2;
		this.centimetersZ = centimetersZ2;
		return this;
	}

	/**
	 * Sets the decay time for the item in milliseconds.
	 * 
	 * <p>This determines how long the item will last before decaying
	 * when placed in the world or in certain conditions.</p>
	 * 
	 * @param decayTime2 decay time in milliseconds (default is 9072000L = 15 weeks)
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder decayTime(long decayTime2) {
		this.decayTime = decayTime2;
		return this;
	}

	/**
	 * Sets the combat damage value for weapon items.
	 * 
	 * <p>For weapons, this determines the base damage dealt in combat.
	 * Higher values result in more damage to opponents.</p>
	 * 
	 * @param combatDamage2 the combat damage value
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder combatDamage(int combatDamage2) {
		this.combatDamage = combatDamage2;
		return this;
	}

	/**
	 * Sets the behavior type of the item.
	 * 
	 * <p>Behavior type determines how the item interacts with the game world,
	 * such as how it's picked up, used, or displayed.</p>
	 * 
	 * @param behaviourType2 the behavior type identifier
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder behaviourType(short behaviourType2) {
		this.behaviourType = behaviourType2;
		return this;
	}

	/**
	 * Sets the image number for the item's inventory icon.
	 * 
	 * <p>This determines which icon is displayed for the item in inventory screens.
	 * Icons are defined in the client's image files.</p>
	 * 
	 * @param imageNumber the image identifier
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder imageNumber(short imageNumber) {
		this.imageNumber = imageNumber;
		return this;
	}

	/**
	 * Resolve a registered dynamic-icon string id (e.g.
	 * {@code "mastercraft:eternal_orb"}) to its allocated wire-protocol short
	 * and assign it as this template's {@code imageNumber}.
	 *
	 * <p>Throws {@link IllegalStateException} if the id is not registered —
	 * silent fallback to 0 would hide the typical modder mistakes (filename
	 * typo, PNG missing from {@code mods/<modname>/icons/}, scanner skipped
	 * the file because it wasn't 32×32). Catch the failure here so the boot
	 * log names the offending template.
	 *
	 * @param stringId canonical {@code <modname>:<relpath without .png>} id used at scan time
	 * @return this builder
	 * @since 0.4.1
	 */
	public ItemTemplateBuilder icon(String stringId) {
		short s = com.garward.wurmmodloader.core.icon.IconRegistry.lookup(stringId);
		if (s < 0) {
			throw new IllegalStateException(
				"icon(\"" + stringId + "\") — no such id registered. "
				+ "Drop a 32×32 PNG at mods/<modname>/icons/<relpath>.png "
				+ "or call IconRegistry.register(stringId, packUri) before building the template.");
		}
		this.imageNumber = s;
		return this;
	}

	/**
	 * Sets the dye amount override in grams.
	 * 
	 * <p>This overrides the default amount of dye required to color this item.
	 * Useful for items that should require more or less dye than normal.</p>
	 * 
	 * @param dyeAmountOverrideGrams the dye amount in grams
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder dyeAmountOverrideGrams(short dyeAmountOverrideGrams) {
		this.dyeAmountOverrideGrams = dyeAmountOverrideGrams;
		return this;
	}

	/**
	 * Sets the container dimensions for items that can hold other items.
	 * 
	 * <p>For containers, this defines the internal space available for storing items.
	 * This automatically marks the item as having container sizes.</p>
	 * 
	 * @param sizeX the internal width in some unit
	 * @param sizeY the internal height in some unit
	 * @param sizeZ the internal depth in some unit
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder containerSize(int sizeX, int sizeY, int sizeZ) {
		this.hasContainerSizes = true;
		this.containerSizeX = sizeX;
		this.containerSizeY = sizeY;
		this.containerSizeZ = sizeZ;
		return this;
	}

	/**
	 * Sets the maximum number of items this container can hold.
	 * 
	 * <p>For containers, this limits how many individual items can be placed inside,
	 * regardless of their size or weight.</p>
	 * 
	 * @param maxItemCount the maximum number of items (-1 for no limit)
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;
		return this;
	}

	/**
	 * Sets the maximum total weight this container can hold.
	 * 
	 * <p>For containers, this limits the total weight of items that can be placed inside.</p>
	 * 
	 * @param maxItemWeight the maximum weight in grams (-1 for no limit)
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder maxItemWeight(int maxItemWeight) {
		this.maxItemWeight = maxItemWeight;
		return this;
	}

	/**
	 * Sets the amount of primary dye required for this item.
	 * 
	 * <p>This determines how much dye is needed to change the primary color of the item.</p>
	 * 
	 * @param dyePrimaryAmountRequired the amount of dye in grams
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder dyePrimaryAmountGrams(int dyePrimaryAmountRequired) {
		this.dyePrimaryAmountRequired = dyePrimaryAmountRequired;
		return this;
	}

	/**
	 * Sets the amount of secondary dye required for this item.
	 * 
	 * <p>This determines how much dye is needed to change the secondary color of the item.</p>
	 * 
	 * @param dyeSecondaryAmountRequired the amount of dye in grams
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder dyeSecondaryAmountGrams(int dyeSecondaryAmountRequired) {
		this.dyeSecondaryAmountRequired = dyeSecondaryAmountRequired;
		return this;
	}

	/**
	 * Sets the secondary item name and dye amount for this item.
	 * 
	 * <p>This is used for items that produce a secondary item when processed
	 * (e.g., plants that produce seeds) and sets the dye requirements for that process.</p>
	 * 
	 * @param secondaryItemName the name of the secondary item produced
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder secondaryItemName(String secondaryItemName) {
		this.secondaryItemName = secondaryItemName;
		return this;
	}
	
	/**
	 * Sets the food group template ID for this food item.
	 * 
	 * <p>For food items, this determines which food group the item belongs to,
	 * affecting nutrition and dietary properties.</p>
	 * 
	 * @param foodGroupTemplateId the template ID of the food group
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder foodGroup(final int foodGroupTemplateId) {
		this.foodGroup = foodGroupTemplateId;
		return this;
	}
	
	/**
	 * Sets the nutrition values for this food item.
	 * 
	 * <p>For food items, this defines the nutritional content provided when consumed.</p>
	 * 
	 * @param calories the calorie content
	 * @param carbs the carbohydrate content
	 * @param fats the fat content
	 * @param proteins the protein content
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder nutritionValues(final int calories, final int carbs, final int fats, final int proteins) {
		this.hasNutritionValues = true;
		this.calories = calories;
		this.carbs = carbs;
		this.fats = fats;
		this.proteins = proteins;
		return this;
	}
	
	/**
	 * Sets the alcohol strength for this beverage item.
	 * 
	 * <p>For drinkable items, this determines the alcohol content which affects
	 * player intoxication levels when consumed.</p>
	 * 
	 * @param newAlcoholStrength the alcohol strength percentage
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder alcoholStrength(final int newAlcoholStrength) {
		this.alcoholStrength = newAlcoholStrength;
		return this;
	}

	/**
	 * Sets the template ID of the item this item grows into.
	 * 
	 * <p>For growable items like plants, this determines what the item transforms into
	 * as it matures or under certain conditions.</p>
	 * 
	 * @param growsTemplateId the template ID of the item it grows into
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder grows(final int growsTemplateId) {
		this.grows = growsTemplateId;
		return this;
	}

	/**
	 * Sets the template ID of the item this item crushes into.
	 * 
	 * <p>For items that can be crushed (e.g., plants), this determines what item
	 * is produced when the crushing action is performed.</p>
	 * 
	 * @param toTemplateId the template ID of the crushed result
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder setCrushsTo(final int toTemplateId) {
		this.crushsTo = toTemplateId;
		return this;
	}

	/**
	 * Sets the template ID of the item this item harvests into.
	 * 
	 * <p>For harvestable items like plants, this determines what item is produced
	 * when the harvesting action is performed.</p>
	 * 
	 * @param toTemplateId the template ID of the harvested result
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder harvestsTo(final int toTemplateId) {
		this.harvestTo = toTemplateId;
		return this;
	}
	
	/**
	 * Sets the template ID of the seeds this item produces when picked.
	 * 
	 * <p>For plant items, this determines what seed item is produced when the
	 * picking seeds action is performed.</p>
	 * 
	 * @param seedTemplateId the template ID of the seeds produced
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ItemTemplateBuilder pickSeeds(final int seedTemplateId) {
		this.pickSeeds = seedTemplateId;
		return this;
	}

	/**
	 * Builds an ItemTemplate with the specified properties.
	 * 
	 * <p>This method creates an {@link ItemTemplate} instance with all the properties
	 * that have been set on this builder. It's an alternative to the parameterless {@link #build()}
	 * method that allows setting all properties in one call.</p>
	 * 
	 * <p><strong>Note:</strong> This method will override any properties previously
	 * set using the individual setter methods.</p>
	 * 
	 * @param name the singular name of the item
	 * @param size the size category of the item
	 * @param plural the plural form of the item name
	 * @param itemDescriptionSuperb description for superb quality items
	 * @param itemDescriptionNormal description for normal quality items
	 * @param itemDescriptionBad description for bad quality items
	 * @param itemDescriptionRotten description for rotten/damaged items
	 * @param itemDescriptionLong the long description of the item
	 * @param itemTypes array of item type flags
	 * @param imageNumber the inventory icon image identifier
	 * @param behaviourType the behavior type identifier
	 * @param combatDamage the combat damage value for weapons
	 * @param decayTime the decay time in milliseconds
	 * @param centimetersX width in centimeters
	 * @param centimetersY height in centimeters
	 * @param centimetersZ length in centimeters
	 * @param primarySkill the primary crafting skill identifier
	 * @param bodySpaces array of body space identifiers for equipment
	 * @param modelName the 3D model name
	 * @param difficulty the crafting difficulty
	 * @param weightGrams the weight in grams
	 * @param material the material type
	 * @param value the base value in copper coins
	 * @param isTraded whether the item is traded on merchant networks
	 * @return the created ItemTemplate instance
	 * @throws IOException if there is an error creating the item template
	 * @since 1.0.0
	 */
	public ItemTemplate build(final String name, int size, final String plural, final String itemDescriptionSuperb, final String itemDescriptionNormal, final String itemDescriptionBad, final String itemDescriptionRotten, final String itemDescriptionLong, final short[] itemTypes, final short imageNumber,
		  	final short behaviourType, final int combatDamage, final long decayTime, final int centimetersX, final int centimetersY, final int centimetersZ, final int primarySkill, final byte[] bodySpaces, final String modelName, final float difficulty, final int weightGrams, final byte material, int value, boolean isTraded)
			throws IOException {

		this.name(name, plural, itemDescriptionLong);
		this.size(size);
		this.descriptions(itemDescriptionSuperb, itemDescriptionNormal, itemDescriptionBad, itemDescriptionRotten);
		this.itemTypes(itemTypes);
		this.imageNumber(imageNumber);
		this.behaviourType(behaviourType);
		this.combatDamage(combatDamage);
		this.decayTime(decayTime);
		this.dimensions(centimetersX, centimetersY, centimetersZ);
		this.primarySkill(primarySkill);
		this.bodySpaces(bodySpaces);
		this.modelName(modelName);
		this.difficulty(difficulty);
		this.weightGrams(weightGrams);
		this.material(material);
		this.value(value);
		this.isTraded(isTraded);

		return build();
	}
	
	/**
	 * Deprecated build method with armor type parameter.
	 * 
	 * <p>This method is deprecated and simply delegates to the version without
	 * the armorType parameter, as that parameter is no longer used.</p>
	 * 
	 * @param name the singular name of the item
	 * @param size the size category of the item
	 * @param plural the plural form of the item name
	 * @param itemDescriptionSuperb description for superb quality items
	 * @param itemDescriptionNormal description for normal quality items
	 * @param itemDescriptionBad description for bad quality items
	 * @param itemDescriptionRotten description for rotten/damaged items
	 * @param itemDescriptionLong the long description of the item
	 * @param itemTypes array of item type flags
	 * @param imageNumber the inventory icon image identifier
	 * @param behaviourType the behavior type identifier
	 * @param combatDamage the combat damage value for weapons
	 * @param decayTime the decay time in milliseconds
	 * @param centimetersX width in centimeters
	 * @param centimetersY height in centimeters
	 * @param centimetersZ length in centimeters
	 * @param primarySkill the primary crafting skill identifier
	 * @param bodySpaces array of body space identifiers for equipment
	 * @param modelName the 3D model name
	 * @param difficulty the crafting difficulty
	 * @param weightGrams the weight in grams
	 * @param material the material type
	 * @param value the base value in copper coins
	 * @param isTraded whether the item is traded on merchant networks
	 * @param armourType the armor type (ignored)
	 * @return the created ItemTemplate instance
	 * @throws IOException if there is an error creating the item template
	 * @deprecated Use {@link #build(String, int, String, String, String, String, String, String, short[], short, short, int, long, int, int, int, int, byte[], String, float, int, byte, int, boolean)} instead
	 * @since 1.0.0
	 */
	@Deprecated
	public ItemTemplate build(final String name, int size, final String plural, final String itemDescriptionSuperb, final String itemDescriptionNormal, final String itemDescriptionBad, final String itemDescriptionRotten, final String itemDescriptionLong, final short[] itemTypes, final short imageNumber,
			final short behaviourType, final int combatDamage, final long decayTime, final int centimetersX, final int centimetersY, final int centimetersZ, final int primarySkill, final byte[] bodySpaces, final String modelName, final float difficulty, final int weightGrams, final byte material, int value, boolean isTraded, int armourType)
			throws IOException {
		return build(name, size, plural, itemDescriptionSuperb, itemDescriptionNormal, itemDescriptionBad, itemDescriptionRotten, itemDescriptionLong, itemTypes, imageNumber,
				behaviourType, combatDamage, decayTime, centimetersX, centimetersY, centimetersZ, primarySkill, bodySpaces, modelName, difficulty, weightGrams, material, value, isTraded);
	}

	/**
	 * Builds an ItemTemplate with the properties set through the builder methods.
	 *
	 * <p>This method creates an {@link ItemTemplate} instance with all the properties
	 * that have been set on this builder. It uses the {@link ItemTemplateFactory} to
	 * create the actual template and then applies any additional properties that require
	 * post-creation configuration.</p>
	 *
	 * <p>If this builder was constructed with a {@link ResourceLocation}, the template
	 * will be automatically registered in {@link Registries#ITEMS} for easy retrieval.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method is NOT thread-safe and should
	 * only be called from the main server thread during mod initialization.</p>
	 *
	 * <p><strong>Lifecycle:</strong> This method should typically be called during
	 * the mod initialization phase, usually in the {@code onItemTemplatesCreated()} method.</p>
	 *
	 * @return the created ItemTemplate instance
	 * @throws IOException if there is an error creating the item template
	 * @since 1.0.0
	 */
	public ItemTemplate build() throws IOException {
		try {
			ItemTemplate template = ItemTemplateFactory.getInstance().createItemTemplate(templateId, size, name, plural, itemDescriptionSuperb, itemDescriptionNormal, itemDescriptionBad, itemDescriptionRotten, itemDescriptionLong, itemTypes, imageNumber, behaviourType, combatDamage, decayTime, centimetersX,
					centimetersY, centimetersZ, primarySkill, bodySpaces, modelName, difficulty, weightGrams, material, value, isTraded, dyeAmountOverrideGrams);

			if (hasContainerSizes) {
				template.setContainerSize(containerSizeX, containerSizeY, containerSizeZ);
			}
			if (maxItemCount >= 0) {
				template.setMaxItemCount(maxItemCount);
			}
			if (maxItemWeight >= 0) {
				template.setMaxItemWeight(maxItemWeight);
			}

			template.setDyeAmountGrams(dyePrimaryAmountRequired);
			if (secondaryItemName != null) {
				template.setSecondryItem(secondaryItemName, dyeSecondaryAmountRequired);
			}

			if (hasNutritionValues) {
				template.setNutritionValues(calories, carbs, fats, proteins);
			}

			if (alcoholStrength > 0) {
				ReflectionUtil.callPrivateMethod(template, ReflectionUtil.getMethod(ItemTemplate.class, "setAlcoholStrength"), alcoholStrength);
			}

			if (foodGroup > 0) {
				ReflectionUtil.callPrivateMethod(template, ReflectionUtil.getMethod(ItemTemplate.class, "setFoodGroup"), foodGroup);
			}

			if (crushsTo > 0) {
				ReflectionUtil.callPrivateMethod(template, ReflectionUtil.getMethod(ItemTemplate.class, "setCrushsTo"), crushsTo);
			}

			if (pickSeeds > 0) {
				ReflectionUtil.callPrivateMethod(template, ReflectionUtil.getMethod(ItemTemplate.class, "setPickSeeds"), pickSeeds);
			}

			if (grows > 0) {
				ReflectionUtil.callPrivateMethod(template, ReflectionUtil.getMethod(ItemTemplate.class, "setGrows"), grows);
			}

			if (harvestTo > 0) {
				ReflectionUtil.callPrivateMethod(template, ReflectionUtil.getMethod(ItemTemplate.class, "setHarvestsTo"), harvestTo);
			}

			// Register with the registry system if ResourceLocation was provided
			if (resourceLocation != null) {
				Registries.ITEMS.register(resourceLocation, template);
			}

			return template;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

}
<file name="CreatureTemplateBuilder.java">
<content>
package com.garward.wurmmodloader.modsupport;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wurmonline.server.combat.ArmourTemplate;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;

import com.wurmonline.server.creatures.AttackAction;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.skills.Skills;
import com.wurmonline.server.skills.SkillsFactory;
import com.wurmonline.shared.constants.ItemMaterials;

/**
 * Builder class for creating {@link CreatureTemplate} instances with customizable properties.
 * 
 * <p>This class provides a fluent API for constructing creature templates with various
 * attributes such as physical characteristics, combat abilities, skills, sounds, and behaviors.
 * It uses reflection to access private fields and methods of the Wurm Unlimited server classes
 * to set properties that are not normally accessible through the public API.</p>
 * 
 * <p>Usage example:</p>
 * <pre><code>
 * CreatureTemplate template = new CreatureTemplateBuilder("mycreature")
 *     .name("My Creature")
 *     .description("A custom creature for my mod")
 *     .modelName("model.creature.mycreature")
 *     .types(new int[]{CreatureTypes.C_TYPE_SENTIENT, CreatureTypes.C_TYPE_HERBIVORE})
 *     .bodyType(BodyTypes.BODY_TYPE_HUMANOID)
 *     .dimension((short)180, (short)60, (short)30)
 *     .naturalArmour(5.0f)
 *     .damages(3.0f, 4.0f, 2.0f, 1.0f, 0.0f)
 *     .speed(1.0f)
 *     .skill(102, 30.0f) // Body strength
 *     .skill(104, 25.0f) // Body control
 *     .build();
 * </code></pre>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. Each instance should
 * only be used by a single thread. Multiple threads should create separate instances.</p>
 * 
 * <p><strong>Lifecycle:</strong> Instances are typically created during mod initialization
 * and used to register new creature templates with the game. Once {@link #build()} is called,
 * the builder should not be reused.</p>
 * 
 * @since 1.0.0
 * @see CreatureTemplate
 * @see CreatureTemplateFactory
 */
public class CreatureTemplateBuilder {
	
	/**
	 * Helper class for managing reflection access to private fields and methods.
	 * 
	 * <p>This class initializes and caches reflection objects for accessing private
	 * members of {@link CreatureTemplate} and {@link CreatureTemplateFactory} classes.
	 * It is instantiated once during class loading and shared among all builder instances.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This class is thread-safe as all fields
	 * are final and initialized during construction.</p>
	 * 
	 * @since 1.0.0
	 */
	private static class RefHelper {

		private final Field reputation;
		private final Field hasHands;
		private final Field isHorse;
		private final Method createCreatureTemplate;
		private final Method setAlignment;
		private final Method setDenMaterial;
		private final Method setDenName;
		private final Method setMaxGroupAttackSize;
		private final Method setBaseCombatRating;
		private final Method setArmourType;
		private final Method setMaxAge;
		private final Method setKickDamString;

		/**
		 * Constructs a new RefHelper instance, initializing all reflection objects.
		 * 
		 * @throws HookException if any reflection operations fail
		 */
		public RefHelper() {
			try {
				reputation = ReflectionUtil.getField(CreatureTemplate.class, "reputation");
				hasHands = ReflectionUtil.getField(CreatureTemplate.class, "hasHands");
				isHorse = ReflectionUtil.getField(CreatureTemplate.class, "isHorse");
				createCreatureTemplate = ReflectionUtil.getMethod(CreatureTemplateFactory.class, "createCreatureTemplate");
				setAlignment = ReflectionUtil.getMethod(CreatureTemplate.class, "setAlignment");
				setDenMaterial = ReflectionUtil.getMethod(CreatureTemplate.class, "setDenMaterial");
				setDenName = ReflectionUtil.getMethod(CreatureTemplate.class, "setDenName");
				setMaxGroupAttackSize = ReflectionUtil.getMethod(CreatureTemplate.class, "setMaxGroupAttackSize");
				setBaseCombatRating = ReflectionUtil.getMethod(CreatureTemplate.class, "setBaseCombatRating");
				setArmourType = ReflectionUtil.getMethod(CreatureTemplate.class, "setArmourType");
				setMaxAge = ReflectionUtil.getMethod(CreatureTemplate.class, "setMaxAge");
				setKickDamString = ReflectionUtil.getMethod(CreatureTemplate.class, "setKickDamString");
			} catch (NoSuchFieldException | NoSuchMethodException e) {
				throw new HookException(e);
			}
		}
	}
	
	private static final RefHelper REFHELPER = new RefHelper();
	
	private int templateId;

	private Map<Integer, Float> skills = new HashMap<>();

	private int[] types;

	private String name, plural;

	private String description;

	private int maxAge;

	private float baseCombatRating;

	private int maxGroupAttackSize;

	private String denName;

	private byte denMaterial;

	private float maxPercentOfCreatures;

	private boolean usesNewAttacks;

	private ArmourTemplate.ArmourType armourType = ArmourTemplate.ARMOUR_TYPE_CLOTH;

	private byte bodyType;

	private List<AttackAction> primaryAttackActions = new LinkedList<>();

	private List<AttackAction> secondaryAttackActions = new LinkedList<>();

	private String modelName;

	private float maxX;

	private float minX;

	private float minY;

	private float maxY;

	private String handDamString;

	private String kickDamString;

	private String headbuttDamString;

	private short vision;

	private byte sex;

	private short centimetersHigh;

	private short centimetersLong;

	private short centimetersWide;

	private String deathSndMale;

	private String deathSndFemale;

	private String hitSndMale;

	private String hitSndFemale;

	private float naturalArmour;

	private float handDam;

	private float kickDam;

	private float biteDam;

	private float headDam;

	private float breathDam;

	private float speed;

	private int moveRate;

	private int[] itemsButchered;

	private int maxHuntDist;

	private int aggressive;

	private boolean hasBounds;

	private byte combatDamageType;

	private float alignment;

	private boolean isHorse;

	private byte meatMaterial;

	private int colorRed = 255;

	private int colorGreen = 255;

	private int colorBlue = 255;

	private int sizeModX = 64;

	private int sizeModY = 64;

	private int sizeModZ = 64;

	private byte fireRadius;

	private boolean onFire;

	private boolean glowing;

	private int[] combatMoves;

	private boolean isEggLayer;

	private int eggTemplate;

	private int childTemplate;

	private byte daysOfPregnancy;

	private boolean hasHands;

	private boolean keepSex;

	private int maxPopulationOfCreatures;

	private int paintMode;

	private float bonusCombatRating;

	private float fireResistance;

	private float coldResistance;

	private float diseaseResistance;

	private float physicalResistance;

	private float pierceResistance;

	private float slashResistance;

	private float crushResistance;

	private float biteResistance;

	private float poisonResistance;

	private float waterResistance;

	private float acidResistance;

	private float internalResistance;

	private float fireVulnerability;

	private float coldVulnerability;

	private float diseaseVulnerability;

	private float physicalVulnerability;

	private float pierceVulnerability;

	private float slashVulnerability;

	private float crushVulnerability;

	private float biteVulnerability;

	private float poisonVulnerability;

	private float waterVulnerability;

	private float acidVulnerability;

	private float internalVulnerability;

	private int leaderTemplateId = -1;

	private float offZ;

	private int reputation = 100;

	/**
	 * Constructs a new CreatureTemplateBuilder with the specified template ID.
	 * 
	 * @param id the template ID to use for the creature template
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder(int id) {
		this.templateId = id;
		defaultSkills();
	}

	/**
	 * Constructs a new CreatureTemplateBuilder with an automatically generated template ID
	 * based on the provided identifier string.
	 * 
	 * @param identifier the string identifier used to generate a unique template ID
	 * @since 1.0.0
	 * @see IdFactory#getIdFor(String, IdType)
	 */
	public CreatureTemplateBuilder(String identifier) {
		this(IdFactory.getIdFor(identifier, IdType.CREATURETEMPLATE));
	}

	/**
	 * Constructs a new CreatureTemplateBuilder with comprehensive creature properties.
	 * 
	 * <p>This constructor initializes the builder with a full set of creature properties
	 * using default meat material ({@link ItemMaterials#MATERIAL_MEAT}).</p>
	 * 
	 * @param identifier the string identifier used to generate a unique template ID
	 * @param name the name of the creature
	 * @param description the description of the creature
	 * @param modelName the 3D model name for the creature
	 * @param types array of creature type flags
	 * @param bodyType the body type of the creature
	 * @param vision the vision range of the creature
	 * @param sex the sex of the creature
	 * @param centimetersHigh the height of the creature in centimeters
	 * @param centimetersLong the length of the creature in centimeters
	 * @param centimetersWide the width of the creature in centimeters
	 * @param deathSndMale the death sound for male creatures
	 * @param deathSndFemale the death sound for female creatures
	 * @param hitSndMale the hit sound for male creatures
	 * @param hitSndFemale the hit sound for female creatures
	 * @param naturalArmour the natural armour value of the creature
	 * @param handDam the hand damage value
	 * @param kickDam the kick damage value
	 * @param biteDam the bite damage value
	 * @param headDam the head damage value
	 * @param breathDam the breath damage value
	 * @param speed the movement speed of the creature
	 * @param moveRate the move rate of the creature
	 * @param itemsButchered array of item template IDs that can be butchered from this creature
	 * @param maxHuntDist the maximum hunting distance
	 * @param aggress the aggression level of the creature
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder(final String identifier, final String name, final String description, final String modelName, final int[] types, final byte bodyType, final short vision, final byte sex, final short centimetersHigh, final short centimetersLong, final short centimetersWide,
			final String deathSndMale, final String deathSndFemale, final String hitSndMale, final String hitSndFemale, final float naturalArmour, final float handDam, final float kickDam, final float biteDam, final float headDam, final float breathDam, final float speed, final int moveRate,
			final int[] itemsButchered, final int maxHuntDist, final int aggress) {
		this(identifier, name, description, modelName, types, bodyType, vision, sex, centimetersHigh, centimetersLong, centimetersWide, deathSndMale, deathSndFemale, hitSndMale, hitSndFemale, naturalArmour, handDam, kickDam, biteDam, headDam, breathDam, speed, moveRate, itemsButchered, maxHuntDist, aggress, ItemMaterials.MATERIAL_MEAT);
	}

	/**
	 * Constructs a new CreatureTemplateBuilder with comprehensive creature properties.
	 * 
	 * @param identifier the string identifier used to generate a unique template ID
	 * @param name the name of the creature
	 * @param description the description of the creature
	 * @param modelName the 3D model name for the creature
	 * @param types array of creature type flags
	 * @param bodyType the body type of the creature
	 * @param vision the vision range of the creature
	 * @param sex the sex of the creature
	 * @param centimetersHigh the height of the creature in centimeters
	 * @param centimetersLong the length of the creature in centimeters
	 * @param centimetersWide the width of the creature in centimeters
	 * @param deathSndMale the death sound for male creatures
	 * @param deathSndFemale the death sound for female creatures
	 * @param hitSndMale the hit sound for male creatures
	 * @param hitSndFemale the hit sound for female creatures
	 * @param naturalArmour the natural armour value of the creature
	 * @param handDam the hand damage value
	 * @param kickDam the kick damage value
	 * @param biteDam the bite damage value
	 * @param headDam the head damage value
	 * @param breathDam the breath damage value
	 * @param speed the movement speed of the creature
	 * @param moveRate the move rate of the creature
	 * @param itemsButchered array of item template IDs that can be butchered from this creature
	 * @param maxHuntDist the maximum hunting distance
	 * @param aggress the aggression level of the creature
	 * @param meatMaterial the material type for the creature's meat
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder(final String identifier, final String name, final String description, final String modelName, final int[] types, final byte bodyType, final short vision, final byte sex, final short centimetersHigh, final short centimetersLong, final short centimetersWide,
			final String deathSndMale, final String deathSndFemale, final String hitSndMale, final String hitSndFemale, final float naturalArmour, final float handDam, final float kickDam, final float biteDam, final float headDam, final float breathDam, final float speed, final int moveRate,
			final int[] itemsButchered, final int maxHuntDist, final int aggress, final byte meatMaterial) {
		this(identifier);
		name(name);
		description(description);
		modelName(modelName);
		types(types);
		bodyType(bodyType);
		vision(vision);
		sex(sex);
		dimension(centimetersHigh, centimetersLong, centimetersWide);
		deathSounds(deathSndMale, deathSndFemale);
		hitSounds(hitSndMale, hitSndFemale);
		naturalArmour(naturalArmour);
		damages(handDam, kickDam, biteDam, headDam, breathDam);
		speed(speed);
		moveRate(moveRate);
		itemsButchered(itemsButchered);
		maxHuntDist(maxHuntDist);
		aggressive(aggress);
		meatMaterial(meatMaterial);
	}

	/**
	 * Sets the damage values for various attack types.
	 * 
	 * @param handDam2 the hand damage value
	 * @param kickDam2 the kick damage value
	 * @param biteDam2 the bite damage value
	 * @param headDam2 the head damage value
	 * @param breathDam2 the breath damage value
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder damages(float handDam2, float kickDam2, float biteDam2, float headDam2, float breathDam2) {
		this.handDam = handDam2;
		this.kickDam = kickDam2;
		this.biteDam = biteDam2;
		this.headDam = headDam2;
		this.breathDam = breathDam2;
		return this;
	}

	/**
	 * Sets the movement speed of the creature.
	 * 
	 * @param speed the movement speed value
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder speed(float speed) {
		this.speed = speed;
		return this;
	}

	/**
	 * Sets the move rate of the creature.
	 * 
	 * @param moveRate the move rate value
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder moveRate(int moveRate) {
		this.moveRate = moveRate;
		return this;
	}

	/**
	 * Sets the items that can be butchered from this creature.
	 * 
	 * @param itemsButchered array of item template IDs that can be butchered
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder itemsButchered(int[] itemsButchered) {
		this.itemsButchered = itemsButchered;
		return this;
	}

	/**
	 * Sets the maximum hunting distance for this creature.
	 * 
	 * @param maxHuntDist the maximum hunting distance
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder maxHuntDist(int maxHuntDist) {
		this.maxHuntDist = maxHuntDist;
		return this;
	}

	/**
	 * Sets the aggression level of the creature.
	 * 
	 * @param aggress the aggression level
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder aggressive(int aggress) {
		this.aggressive = aggress;
		return this;
	}

	/**
	 * Sets the meat material type for this creature.
	 * 
	 * @param meatMaterial the material type for the creature's meat
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder meatMaterial(byte meatMaterial) {
		this.meatMaterial = meatMaterial;
		return this;
	}

	/**
	 * Sets the natural armour value of the creature.
	 * 
	 * @param naturalArmour2 the natural armour value
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder naturalArmour(float naturalArmour2) {
		this.naturalArmour = naturalArmour2;
		return this;
	}

	/**
	 * Sets the hit sounds for male and female creatures.
	 * 
	 * @param hitSndMale2 the hit sound for male creatures
	 * @param hitSndFemale2 the hit sound for female creatures
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder hitSounds(String hitSndMale2, String hitSndFemale2) {
		this.hitSndMale = hitSndMale2;
		this.hitSndFemale = hitSndFemale2;
		return this;
	}

	/**
	 * Sets the death sounds for male and female creatures.
	 * 
	 * @param deathSndMale the death sound for male creatures
	 * @param deathSndFemale the death sound for female creatures
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder deathSounds(String deathSndMale, String deathSndFemale) {
		this.deathSndMale = deathSndMale;
		this.deathSndFemale = deathSndFemale;
		return this;
	}

	/**
	 * Sets the physical dimensions of the creature.
	 * 
	 * @param centimetersHigh2 the height of the creature in centimeters
	 * @param centimetersLong2 the length of the creature in centimeters
	 * @param centimetersWide2 the width of the creature in centimeters
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder dimension(short centimetersHigh2, short centimetersLong2, short centimetersWide2) {
		this.centimetersHigh = centimetersHigh2;
		this.centimetersLong = centimetersLong2;
		this.centimetersWide = centimetersWide2;
		return this;
	}

	/**
	 * Sets the sex of the creature.
	 * 
	 * @param sex the sex of the creature
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder sex(byte sex) {
		this.sex = sex;
		return this;
	}

	/**
	 * Sets the vision range of the creature.
	 * 
	 * @param vision the vision range
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder vision(short vision) {
		this.vision = vision;
		return this;
	}

	/**
	 * Sets the body type of the creature.
	 * 
	 * @param bodyType the body type
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder bodyType(byte bodyType) {
		this.bodyType = bodyType;
		return this;
	}

	/**
	 * Sets the 3D model name for the creature.
	 * 
	 * @param modelName the 3D model name
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder modelName(String modelName) {
		this.modelName = modelName;
		return this;
	}

	/**
	 * Sets default skills for the creature.
	 * 
	 * <p>This method initializes the creature with default skill values of 20.0 for:
	 * <ul>
	 * <li>Body Strength (102)</li>
	 * <li>Body Stamina (104)</li>
	 * <li>Body Control (103)</li>
	 * <li>Mind Logic (100)</li>
	 * <li>Mind Speed (101)</li>
	 * <li>Soul Depth (105)</li>
	 * <li>Soul Strength (106)</li>
	 * </ul></p>
	 * 
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder defaultSkills() {
		skills.put(102, 20.0f);
		skills.put(104, 20.0f);
		skills.put(103, 20.0f);
		skills.put(100, 20.0f);
		skills.put(101, 20.0f);
		skills.put(105, 20.0f);
		skills.put(106, 20.0f);
		return this;
	}

	/**
	 * Adds or updates a skill for the creature.
	 * 
	 * @param skillNumber the skill identifier
	 * @param startValue the starting value for the skill
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder skill(int skillNumber, float startValue) {
		this.skills.put(skillNumber, startValue);
		return this;
	}

	/**
	 * Sets the creature type flags.
	 * 
	 * @param types array of creature type flags
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder types(int[] types) {
		this.types = types;
		return this;
	}

	/**
	 * Sets the name of the creature.
	 * 
	 * @param name the name of the creature
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the plural name of the creature.
	 * 
	 * @param plural the plural name of the creature
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder plural(String plural) {
		this.plural = plural;
		return this;
	}

	/**
	 * Sets the description of the creature.
	 * 
	 * @param description the description of the creature
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder description(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Adds a primary attack action to the creature.
	 * 
	 * @param attackAction the attack action to add
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder addPrimaryAttack(AttackAction attackAction) {
		primaryAttackActions.add(attackAction);
		return this;
	}

	/**
	 * Adds a secondary attack action to the creature.
	 * 
	 * @param attackAction the attack action to add
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder addSecondaryAttack(AttackAction attackAction) {
		secondaryAttackActions.add(attackAction);
		return this;
	}

	/**
	 * Set headbutt damage string
	 * @param headbuttDamString headbutt damage string
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder headbuttDamString(String headbuttDamString) {
		this.headbuttDamString = headbuttDamString;
		return this;
	}

	/**
	 * Creature is an egg layer
	 * @param eggTemplate Egg template. -1 disabled egg laying
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder eggLayer(int eggTemplate) {
		this.isEggLayer = eggTemplate != -1;
		this.eggTemplate = eggTemplate;
		return this;
	}
	
	/**
	 * Set days of pregnancy
	 * @param daysOfPregnancy days of pregnancy
	 * @return this
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder daysOfPregnancy(byte daysOfPregnancy) {
		this.daysOfPregnancy = daysOfPregnancy;
		return this;
	}
	
	/**
	 * Set child template id
	 * 
	 * @param childTemplate child template id
	 * @return this
	 * @since 1.0.0
	 */
	public CreatureTemplateBuilder childTemplate(int childTemplate) {
		this.childTemplate = childTemplate;
		return this;
	}

	/**
	 * Builds and returns the configured {@link CreatureTemplate}.
	 * 
	 * <p>This method creates a new {@link CreatureTemplate} instance with all the
	 * properties configured through the builder methods. It handles the complex process
	 * of using reflection to set private fields and invoke private methods on the
	 * creature template object.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is not thread-safe
	 * and should only be called from a single thread.</p>
	 * 
	 * @return the constructed {@link CreatureTemplate}
	 * @throws HookException if any reflection operations fail during template creation
	 * @since 1.0.0
	 * @see CreatureTemplate
	 */
	public CreatureTemplate build() {
		try {
			final Skills skills = SkillsFactory.createSkills(name);
			for (Entry<Integer, Float> skillEntry : this.skills.entrySet()) {
				skills.learnTemp(skillEntry.getKey(), skillEntry.getValue());
			}

			final CreatureTemplate temp = createCreatureTemplate(templateId, name, plural==null ? name+"s" : plural, description, modelName, types, bodyType, skills, vision, sex, centimetersHigh, centimetersLong, centimetersWide, deathSndMale, deathSndFemale, hitSndMale, hitSndFemale, naturalArmour, handDam, kickDam, biteDam,
					headDam, breathDam, speed, moveRate, itemsButchered, maxHuntDist, aggressive, meatMaterial);

			if (hasBounds)
				temp.setBoundsValues(minX, minY, maxX, maxY);

			if (this.handDamString != null)
				temp.setHandDamString(handDamString);

			if (this.kickDamString != null)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setKickDamString, kickDamString);

			if (this.headbuttDamString != null) {
				temp.setHeadbuttDamString(headbuttDamString);
			}

			if (maxAge > 0)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setMaxAge, maxAge);

			if (armourType != null)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setArmourType, armourType);

			if (baseCombatRating > 0)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setBaseCombatRating, baseCombatRating);

			if (combatDamageType > 0)
				temp.combatDamageType = combatDamageType;

			if (maxGroupAttackSize > 0)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setMaxGroupAttackSize, maxGroupAttackSize);

			if (denName != null)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setDenName, denName);

			if (denMaterial > 0)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setDenMaterial, denMaterial);

			if (maxPercentOfCreatures > 0)
				temp.setMaxPercentOfCreatures(maxPercentOfCreatures);

			if (alignment != 0)
				ReflectionUtil.callPrivateMethod(temp, REFHELPER.setAlignment, alignment);

			if (isHorse)
				ReflectionUtil.setPrivateField(temp, REFHELPER.isHorse, isHorse);

			if (usesNewAttacks)
				temp.setUsesNewAttacks(usesNewAttacks);

			for (AttackAction attackAction : primaryAttackActions) {
				temp.addPrimaryAttack(attackAction);
			}
			for (AttackAction attackAction : secondaryAttackActions) {
				temp.addSecondaryAttack(attackAction);
			}

			temp.setColorRed(colorRed);
			temp.setColorGreen(colorGreen);
			temp.setColorBlue(colorBlue);

			temp.setSizeModX(sizeModX);
			temp.setSizeModY(sizeModY);
			temp.setSizeModZ(sizeModZ);
			
			temp.offZ = offZ;

			temp.setGlowing(glowing);
			if (onFire) {
				temp.setOnFire(onFire);
				temp.setFireRadius(fireRadius);
			}
			if (combatMoves != null) {
				temp.setCombatMoves(combatMoves);
			}

			if (isEggLayer) {
				temp.setEggLayer(this.isEggLayer);
				temp.setEggTemplateId(this.eggTemplate);
			}
			
			if (childTemplate != 0) {
				temp.setChildTemplateId(childTemplate);
			}
			
			if (daysOfPregnancy != 0) {
				temp.setDaysOfPregnancy(daysOfPregnancy);
			}

			if (hasHands) {
				ReflectionUtil.setPrivateField(temp, REFHELPER.hasHands, hasHands);
			}

			temp.setKeepSex(keepSex);

			if (maxPopulationOfCreatures > 0) {
				temp.setMaxPopulationOfCreatures(maxPopulationOfCreatures);
			}

			temp.setPaintMode(paintMode);

			temp.setBonusCombatRating(bonusCombatRating);

			temp.fireResistance = fireResistance;
			temp.coldResistance = coldResistance;
			temp.diseaseResistance = diseaseResistance;
			temp.physicalResistance = physicalResistance;
			temp.pierceResistance = pierceResistance;
			temp.slashResistance = slashResistance;
			temp.crushResistance = crushResistance;
			temp.biteResistance = biteResistance;
			temp.poisonResistance = poisonResistance;
			temp.waterResistance = waterResistance;
			temp.acidResistance = acidResistance;
			temp.internalResistance = internalResistance;

			temp.fireVulnerability = fireVulnerability;
			temp.coldVulnerability = coldVulnerability;
			temp.diseaseVulnerability = diseaseVulnerability;
			temp.physicalVulnerability = physicalVulnerability;
			temp.pierceVulnerability = pierceVulnerability;
			temp.slashVulnerability = slashVulnerability;
			temp.crushVulnerability = crushVulnerability;
			temp.biteVulnerability = biteVulnerability;
			temp.poisonVulnerability = poisonVulnerability;
			temp.waterVulnerability = waterVulnerability;
			temp.acidVulnerability = acidVulnerability;
			temp.internalVulnerability = internalVulnerability;

			temp.setLeaderTemplateId(leaderTemplateId);
			
			if (this.reputation != 100) {
				ReflectionUtil.setPrivateField(temp, REFHELPER.reputation, reputation);
			}
			
			

			return temp;
		} catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException e) {
			throw new HookException(e);
		}
	}

	/**
	 * Creates a new CreatureTemplate using reflection.
	 * 
	 * @param id the template ID
	 * @param name the name of the creature
	 * @param plural the plural name of the creature
	 * @param longDesc the long description of the creature
	 * @param modelName the 3D model name
	 * @param types array of creature type flags
	 * @param bodyType the body type
	 * @param skills the skills object
	 * @param vision the vision range
	 * @param sex the sex
	 * @param centimetersHigh the height in centimeters
	 * @param centimetersLong the length in centimeters
	 * @param centimetersWide the width in centimeters
	 * @param deathSndMale the male death sound
	 * @param deathSndFemale the female death sound
	 * @param hitSndMale the male hit sound
	 * @param hitSndFemale the female hit sound
	 * @param naturalArmour the natural armour value
	 * @param handDam the hand damage value
	 * @param kickDam the kick damage value
	 * @param biteDam the bite damage value
	 * @param headDam the head damage value
	 * @param breathDam the breath damage value
	 * @param speed the movement speed
	 * @param moveRate the move rate
	 * @param itemsButchered array of butchered items
	 * @param maxHuntDist the maximum hunting distance
	 * @param aggress the aggression level
	 * @param meatMaterial the meat material type
	 * @return the created CreatureTemplate
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalAccessException if access to a private member is denied
	 * @throws IllegalArgumentException if an illegal argument is passed
	 * @throws InvocationTargetException if the invoked method throws an exception
	 * @since 1.0.0
	 */
	private static CreatureTemplate createCreatureTemplate(final int id, final String name, final String plural, final String longDesc, final String modelName, final int[] types, final byte bodyType, final Skills skills, final short vision, final byte sex, final short centimetersHigh, final short centimetersLong,
			final short centimetersWide, final String deathSndMale, final String deathSndFemale, final String hitSndMale, final String hitSndFemale, final float naturalArmour, final float handDam, final float kickDam, final float biteDam, final float headDam, final float breathDam,
			final float speed, final int moveRate, final int[] itemsButchered, final int maxHuntDist, final int aggress, final byte meatMaterial) throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		return ReflectionUtil.callPrivateMethod(CreatureTemplateFactory.getInstance(), REFHELPER.createCreatureTemplate, id, name, plural, longDesc, modelName, types, bodyType, skills, vision, sex, centimetersHigh, centimetersLong, centimetersWide,
				deathSndMale, deathSndFemale, hitSndMale, hitSndFemale, naturalArmour, handDam, kickDam, biteDam, headDam, breathDam, speed, moveRate, itemsButchered, maxHuntDist, aggress, meatMaterial);
	}

	/**
	 * Sets the bounds values for the creature
<file name="ModCreatures.java">
<content>
package com.garward.wurmmodloader.modsupport.creatures;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.callbacks.CallbackApi;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviour;
import com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.Traits;
import com.wurmonline.shared.util.StringUtilities;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

/**
 * Manages custom creatures and their traits in the Wurm Unlimited server environment.
 * 
 * <p>This class provides functionality for registering custom creatures with unique traits,
 * models, and behaviors. It hooks into the game's creature creation system to inject custom
 * templates and modify trait handling for modded creatures.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre><code>
 * // Initialize the mod creatures system
 * ModCreatures.init();
 * 
 * // Create and register a custom creature
 * ModCreature myCreature = new ModCreature() {
 *     // Implementation details...
 * };
 * ModCreatures.addCreature(myCreature);
 * </code></pre>
 * 
 * <p><b>Lifecycle:</b> This class should be initialized during mod loading phase,
 * typically in your mod's {@code onServerStarted()} method. Creatures must be added
 * after initialization but before the game world is fully loaded.</p>
 * 
 * <p><b>Thread Safety:</b> This class is not thread-safe. All operations should
 * be performed on the main server thread during the mod loading phase.</p>
 * 
 * @since 1.0.0
 * @see ModCreature
 * @see CustomTrait
 */
public class ModCreatures {
	
	/**
	 * Enumeration of custom traits that can be assigned to creatures.
	 * 
	 * <p>These traits extend the default Wurm trait system with additional options
	 * for custom creature coloring and identification. Each trait is assigned a unique
	 * number that corresponds to a bit in the creature's trait bitmask.</p>
	 * 
	 * <p><b>Thread Safety:</b> This enum is thread-safe as it contains only
	 * immutable values.</p>
	 * 
	 * @since 1.0.0
	 */
	enum CustomTrait {
		
		/**
		 * Ebony black trait (trait number 23).
		 * @since 1.0.0
		 */
		ebonyblack(23),
		
		/**
		 * Piebald pinto trait (trait number 24).
		 * @since 1.0.0
		 */
		piebaldpinto(24),
		
		/**
		 * Blood bay trait (trait number 25).
		 * @since 1.0.0
		 */
		bloodbay(25),
		
		/**
		 * Custom trait 4 (trait number 26).
		 * @since 1.0.0
		 */
		Custom4(26),
		
		/**
		 * Skewbald pinto trait (trait number 30).
		 * @since 1.0.0
		 */
		skewbaldpinto(30),
		
		/**
		 * Gold buckskin trait (trait number 31).
		 * @since 1.0.0
		 */
		goldbuckskin(31),
		
		/**
		 * Black silver trait (trait number 32).
		 * @since 1.0.0
		 */
		blacksilver(32),
		
		/**
		 * Appaloosa trait (trait number 33).
		 * @since 1.0.0
		 */
		appaloosa(33),
		
		/**
		 * Chestnut trait (trait number 34).
		 * @since 1.0.0
		 */
		chestnut(34),
		
		/**
		 * Custom trait 12 (trait number 35).
		 * @since 1.0.0
		 */
		Custom12(35),
		
		/**
		 * Custom trait 13 (trait number 36).
		 * @since 1.0.0
		 */
		Custom13(36),
		
		/**
		 * Custom trait 14 (trait number 37).
		 * @since 1.0.0
		 */
		Custom14(37),
		
		/**
		 * Custom trait 15 (trait number 38).
		 * @since 1.0.0
		 */
		Custom15(38),
		
		/**
		 * Custom trait 16 (trait number 39).
		 * @since 1.0.0
		 */
		Custom16(39),
		
		/**
		 * Custom trait 17 (trait number 40).
		 * @since 1.0.0
		 */
		Custom17(40),
		
		/**
		 * Custom trait 18 (trait number 41).
		 * @since 1.0.0
		 */
		Custom18(41),
		
		/**
		 * Custom trait 19 (trait number 42).
		 * @since 1.0.0
		 */
		Custom19(42),
		
		/**
		 * Custom trait 20 (trait number 43).
		 * @since 1.0.0
		 */
		Custom20(43),
		
		/**
		 * Custom trait 21 (trait number 44).
		 * @since 1.0.0
		 */
		Custom21(44),
		
		/**
		 * Custom trait 22 (trait number 45).
		 * @since 1.0.0
		 */
		Custom22(45),
		
		/**
		 * Custom trait 23 (trait number 46).
		 * @since 1.0.0
		 */
		Custom23(46),
		
		/**
		 * Custom trait 24 (trait number 47).
		 * @since 1.0.0
		 */
		Custom24(47),
		
		/**
		 * Custom trait 25 (trait number 48).
		 * @since 1.0.0
		 */
		Custom25(48),
		
		/**
		 * Custom trait 26 (trait number 49).
		 * @since 1.0.0
		 */
		Custom26(49),
		
		/**
		 * Custom trait 27 (trait number 50).
		 * @since 1.0.0
		 */
		Custom27(50),
		
		/**
		 * Custom trait 28 (trait number 51).
		 * @since 1.0.0
		 */
		Custom28(51),
		
		/**
		 * Custom trait 29 (trait number 52).
		 * @since 1.0.0
		 */
		Custom29(52),
		
		/**
		 * Custom trait 30 (trait number 53).
		 * @since 1.0.0
		 */
		Custom30(53),
		
		/**
		 * Custom trait 31 (trait number 54).
		 * @since 1.0.0
		 */
		Custom31(54),
		
		/**
		 * Custom trait 32 (trait number 55).
		 * @since 1.0.0
		 */
		Custom32(55),
		
		/**
		 * Custom trait 33 (trait number 56).
		 * @since 1.0.0
		 */
		Custom33(56),
		
		/**
		 * Custom trait 34 (trait number 57).
		 * @since 1.0.0
		 */
		Custom34(57),
		
		/**
		 * Custom trait 35 (trait number 58).
		 * @since 1.0.0
		 */
		Custom35(58),
		
		/**
		 * Custom trait 36 (trait number 59).
		 * @since 1.0.0
		 */
		Custom36(59),
		
		/**
		 * Custom trait 37 (trait number 60).
		 * @since 1.0.0
		 */
		Custom37(60),
		
		/**
		 * Custom trait 38 (trait number 61).
		 * @since 1.0.0
		 */
		Custom38(61),
		
		/**
		 * Custom trait 39 (trait number 62).
		 * @since 1.0.0
		 */
		Custom39(62),
		;
		
		private static long customTraits;
		private int number;
		
		static {
			customTraits = 0;
			for (CustomTrait trait : values()) {
				customTraits |= 1l << trait.getTraitNumber();
			}
		}

		/**
		 * Constructs a new custom trait with the specified trait number.
		 * 
		 * @param number the trait number (0-63)
		 * @since 1.0.0
		 */
		private CustomTrait(int number) {
			this.number = number;
		}
		
		/**
		 * Gets the trait number for this custom trait.
		 * 
		 * @return the trait number
		 * @since 1.0.0
		 */
		public int getTraitNumber() {
			return number;
		}
		
		/**
		 * Gets the trait name for this custom trait.
		 * 
		 * @return the trait name, which is the same as the enum constant name
		 * @since 1.0.0
		 */
		public String getTraitName() {
			return name();
		}
		
		/**
		 * Checks if the given trait number represents a custom trait.
		 * 
		 * @param number the trait number to check
		 * @return {@code true} if the number represents a custom trait, {@code false} otherwise
		 * @since 1.0.0
		 */
		public static boolean isCustomTrait(int number) {
			if (number <= 0 || number > 63)
				return false;
			return (customTraits & (1l << number)) != 0;
		}
	}
	
	private static List<ModCreature> creatures = new LinkedList<>();
	private static Map<Integer, ModCreature> creaturesById = new HashMap<>();
	private static boolean inited;
	
	/**
	 * Initializes the mod creatures system by setting up necessary hooks and callbacks.
	 * 
	 * <p>This method registers hooks with the Wurm Unlimited server to intercept creature
	 * creation, trait assignment, and other related processes. It also initializes custom
	 * traits and integrates with the vehicle behavior system.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is not thread-safe and should only be
	 * called from the main server thread during mod initialization.</p>
	 * 
	 * <p><b>Lifecycle:</b> This method should be called once during mod loading,
	 * before any creatures are added.</p>
	 * 
	 * @throws HookException if there is an error setting up the hooks
	 * @since 1.0.0
	 * @see #addCreature(ModCreature)
	 */
	public static void init() {
		if (inited)
			return;
		
		final ClassPool classPool = HookManager.getInstance().getClassPool();
		try {
			CtClass ctEncounter = classPool.get("com.wurmonline.server.zones.Encounter");
			ctEncounter.setModifiers(Modifier.setPublic(ctEncounter.getModifiers()));
			CtConstructor ctConstructor = ctEncounter.getConstructor(Descriptor.ofConstructor(new CtClass[0]));
			ctConstructor.setModifiers(Modifier.setPublic(ctConstructor.getModifiers()));
		} catch (NotFoundException e) {
			throw new HookException(e);
		}

		ModVehicleBehaviours.init();

		// com.wurmonline.server.creatures.CreatureTemplateCreator.createCreatureTemplates()
		HookManager.getInstance().registerHook("com.wurmonline.server.creatures.CreatureTemplateCreator", "createCreatureTemplates", "()V", new InvocationHandlerFactory() {

			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						method.invoke(proxy, args);
						
						for (ModCreature creature : creatures) {
							CreatureTemplate creatureTemplate = creature.createCreateTemplateBuilder().build();
							creaturesById.put(creatureTemplate.getTemplateId(), creature);
							
							ModVehicleBehaviour vehicleBehaviour = creature.getVehicleBehaviour();
							if (vehicleBehaviour != null) {
								ModVehicleBehaviours.addCreatureVehicle(creatureTemplate.getTemplateId(), vehicleBehaviour);
							}
						}
						
						return null;
					}
				};
			}
		});
		
		// com.wurmonline.server.zones.SpawnTable.createEncounters()
		HookManager.getInstance().registerHook("com.wurmonline.server.zones.SpawnTable", "createEncounters", "()V", new InvocationHandlerFactory() {
			
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {
					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						method.invoke(proxy, args);
						
						for (ModCreature creature : creatures) {
							creature.addEncounters();
						}
						
						return null;
					}
				};
			}
		});
		
		// com.wurmonline.server.creatures.Traits.initialiseTraits()
		HookManager.getInstance().registerHook("com.wurmonline.server.creatures.Traits", "initialiseTraits", "()V", new InvocationHandlerFactory() {
			
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {
					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						method.invoke(proxy, args);
						
						boolean[] neutralTraits = ReflectionUtil.getPrivateField(Traits.class, ReflectionUtil.getField(Traits.class, "neutralTraits"));
						
						for (CustomTrait customTrait : CustomTrait.values()) {
							neutralTraits[customTrait.getTraitNumber()] = true;
						}
						
						return null;
					}
				};
			}
		});
		
		try {
			/**
			 * Replace call for Traits.getTraitString() with a call to GmSetTraitsCallbacks.getTraitString()
			 * where custom colors and custom traits are checked first before falling back to Traits.getTraitString()
			 */
			CtClass ctGmSetTraits = classPool.get("com.wurmonline.server.questions.GmSetTraits");
			HookManager.getInstance().addCallback(ctGmSetTraits, "modcreatures", new GmSetTraitsCallbacks());
			
			// com.wurmonline.server.questions.GmSetTraits.sendQuestion()
			ctGmSetTraits.getMethod("sendQuestion", "()V").instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					if (m.getClassName().equals("com.wurmonline.server.creatures.Traits") && m.getMethodName().equals("getTraitString")) {
						m.replace("$_ = modcreatures.getTraitString(creature, $1);");
					}
				}
			});
		
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
		
		try {
			final CtClass ctCreature = classPool.get("com.wurmonline.server.creatures.Creature");
			HookManager.getInstance().addCallback(ctCreature, "modcreatures", new CreatureCallbacks());

			{
				// com.wurmonline.server.creatures.Creature.getModelName()
				String code = "{ String name = modcreatures.getModelName($0); if (name != null) { return name; }; }";
				ctCreature.getMethod("getModelName", Descriptor.ofMethod(classPool.get("java.lang.String"), new CtClass[0])).insertBefore(code);
			}
			
			{
				// com.wurmonline.server.creatures.Creature.getColourName()
				String code = "{ String colour = modcreatures.getColourName($0); if (colour != null) { return colour; }; }";
				ctCreature.getMethod("getColourName", Descriptor.ofMethod(classPool.get("java.lang.String"), new CtClass[0])).insertBefore(code);
			}
			
			{
				// com.wurmonline.server.creatures.Creature.getColourName(int trait)
				String code = "{ String colour = modcreatures.getColourName($0, $1); if (colour != null) { return colour; }; }";
				ctCreature.getMethod("getColourName", Descriptor.ofMethod(classPool.get("java.lang.String"), new CtClass[] { classPool.get("int") })).insertBefore(code);
			}
			
			
			ctCreature.getMethod("die", "(ZLjava/lang/String;Z)V").instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					if (f.getClassName().equals("com.wurmonline.server.creatures.CreatureTemplate") && f.getFieldName().equals("isHorse")) {
						f.replace("{ $_ = modcreatures.hasTraits(this.getTemplate().getTemplateId()) || $proceed($$); }");
					}
				}
				
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					if (m.getClassName().equals("com.wurmonline.server.creatures.CreatureTemplate") && m.getMethodName().equals("getColourName")) {
						m.replace("{ String color = modcreatures.getColourName(this); if (color != null) { $_ = color; } else { $_ = $proceed($$); } }");
					}
				}
				
			});
			
			ctCreature.getMethod("mate", Descriptor.ofMethod(classPool.get("void"), new CtClass[] {ctCreature, ctCreature})).instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					if (m.getClassName().equals("com.wurmonline.server.creatures.Traits") && m.getMethodName().equals("calcNewTraits") && m.getSignature().equals("(DZJJ)J")) {
						m.replace("$_ = modcreatures.calcNewTraits($1, $2, this, father);");
					}
				}
			});
			
			
			for (CtMethod method : ctCreature.getMethods()) {
				if (method.getName().equals("doNew")) {
					method.instrument(new ExprEditor() {
						@Override
						public void edit(MethodCall m) throws CannotCompileException {
							if (m.getClassName().equals("com.wurmonline.server.creatures.Creature") && m.getMethodName().equals("isHorse")) {
								StringBuffer code = new StringBuffer();
								code.append("$_ = !modcreatures.assignTraits($0) && $proceed($$);");
								m.replace(code.toString());
							}
						}
					});
				}
			}
		
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
		
		

		inited = true;
	}
	
	/**
	 * Adds a custom creature to the mod creatures system.
	 * 
	 * <p>The creature will be registered with the game and its template will be created
	 * during the next creature template creation cycle. The creature's encounters will also
	 * be added to spawn tables.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is not thread-safe and should only be
	 * called from the main server thread.</p>
	 * 
	 * <p><b>Lifecycle:</b> Creatures must be added after {@link #init()} has been
	 * called but before the game world is fully loaded.</p>
	 * 
	 * <p><b>Usage Example:</b></p>
	 * <pre><code>
	 * ModCreature dragon = new ModCreature() {
	 *     // Dragon implementation...
	 * };
	 * ModCreatures.addCreature(dragon);
	 * </code></pre>
	 * 
	 * @param creature the custom creature to add
	 * @throws RuntimeException if the mod creatures system has not been initialized
	 * @since 1.0.0
	 * @see #init()
	 */
	public static void addCreature(ModCreature creature) {
		if (!inited) {
			throw new RuntimeException("ModCreatures was not inited");
		}
		creatures.add(creature);
	}
	
	/**
	 * Retrieves the mod creature associated with the specified template ID.
	 * 
	 * <p>Returns the {@link ModCreature} instance that was registered with the given
	 * template ID, or {@code null} if no such creature exists.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param templateId the template ID of the creature to retrieve
	 * @return the mod creature associated with the template ID, or {@code null} if not found
	 * @since 1.0.0
	 */
	public static ModCreature getModCreature(int templateId) {
		return creaturesById.get(templateId);
	}
	
	/**
	 * Checks if the creature with the specified template ID has custom traits.
	 * 
	 * <p>Returns {@code true} if a mod creature with the given template ID exists
	 * and that creature has custom traits enabled.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param templateId the template ID to check
	 * @return {@code true} if the creature has custom traits, {@code false} otherwise
	 * @since 1.0.0
	 */
	public static boolean hasTraits(int templateId) {
		return creaturesById.get(templateId) != null && creaturesById.get(templateId).hasTraits();
	}

	/**
	 * Gets the trait name for the specified creature.
	 * 
	 * <p>If the creature has custom traits, this method returns the name of the first
	 * custom trait found on the creature. If no custom trait name is defined, it falls back
	 * to the default trait name.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param creature the creature to get the trait name for
	 * @return the trait name, or {@code null} if the creature doesn't have custom traits
	 * @since 1.0.0
	 */
	public static String getTraitName(Creature creature) {
		ModCreature c = creaturesById.get(creature.getTemplate().getTemplateId());
		if (c != null && c.hasTraits()) {
			for (CustomTrait customTrait : CustomTrait.values()) {
				int trait = customTrait.getTraitNumber();
				if (creature.hasTrait(trait)) {
					Optional<String> name = Optional.ofNullable(c.getTraitName(trait));
					return name.orElseGet(() -> customTrait.getTraitName());
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the color name for the specified creature.
	 * 
	 * <p>If the creature has custom traits, this method returns the color name associated
	 * with the first custom trait found on the creature.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param creature the creature to get the color name for
	 * @return the color name, or {@code null} if the creature doesn't have custom traits
	 * @since 1.0.0
	 */
	public static String getColourName(Creature creature) {
		ModCreature c = creaturesById.get(creature.getTemplate().getTemplateId());
		if (c != null && c.hasTraits()) {
			for (CustomTrait customTrait : CustomTrait.values()) {
				int trait = customTrait.getTraitNumber();
				if (creature.hasTrait(trait)) {
					return c.getColourName(trait);
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the model name for the specified creature.
	 * 
	 * <p>If the creature has custom traits, this method constructs a model name based
	 * on the creature's base model name, trait name, sex, and disease status.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param creature the creature to get the model name for
	 * @return the constructed model name, or {@code null} if the creature doesn't have custom traits
	 * @since 1.0.0
	 */
	public static String getModelName(Creature creature) {
		String traitName = getTraitName(creature);
		if (traitName != null) {
			final StringBuilder s = new StringBuilder();
			s.append(creature.getTemplate().getModelName());
			s.append('.');
			s.append(traitName);
			if (creature.getStatus().getSex() == 0) {
				s.append(".male");
			}
			if (creature.getStatus().getSex() == 1) {
				s.append(".female");
			}
			if (creature.getStatus().disease > 0) {
				s.append(".diseased");
			}
			return s.toString();
		}
		return null;
	}
	
	/**
	 * Assigns traits to the specified creature.
	 * 
	 * <p>If the creature has custom traits enabled, this method delegates to the creature's
	 * trait assignment implementation to set the appropriate trait bits.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param creature the creature to assign traits to
	 * @since 1.0.0
	 */
	public static void assignTraits(Creature creature) {
		ModCreature c = creaturesById.get(creature.getTemplate().getTemplateId());
		if (c != null && c.hasTraits()) {
			c.assignTraits(new TraitsSetter() {
				
				@Override
				public void setTraitBit(int i, boolean b) {
					creature.getStatus().setTraitBit(i, b);
				}
			});
		}
	}

	/**
	 * Checks if the specified trait number represents a custom trait.
	 * 
	 * <p>Delegates to {@link CustomTrait#isCustomTrait(int)} to determine if the trait
	 * is a custom mod trait.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param trait the trait number to check
	 * @return {@code true} if the trait is a custom trait, {@code false} otherwise
	 * @since 1.0.0
	 * @see CustomTrait#isCustomTrait(int)
	 */
	public static boolean isCustomTrait(int trait) {
		return CustomTrait.isCustomTrait(trait);
	}
	
	/**
	 * Calculates new traits for breeding between two creatures.
	 * 
	 * <p>If the mother creature has custom traits, this method delegates to the mother's
	 * trait calculation implementation. Otherwise, it falls back to the default Wurm trait
	 * calculation.</p>
	 * 
	 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
	 * 
	 * @param breederSkill the breeder's skill level
	 * @param inbred whether the breeding is inbred
	 * @param mother the mother creature
	 * @param father the father creature
	 * @return the calculated traits as a bitmask
	 * @since 1.0.0
	 */
	public static long calcNewTraits(final double breederSkill, final boolean inbred, final Creature mother, final Creature father) {
		
		long mothertraits = ModTraits.getTraits(mother);
		long fathertraits = ModTraits.getTraits(father);
		
		ModCreature modMother = ModCreatures.getModCreature(mother.getTemplate().getTemplateId());
		if (modMother == null || !modMother.hasTraits()) {
			return Traits.calcNewTraits(breederSkill, inbred, mothertraits, fathertraits);
		}
		
		return modMother.calcNewTraits(breederSkill, inbred, mothertraits, fathertraits);
	}
	
	/**
	 * Callbacks for GM set traits functionality.
	 * 
	 * <p>Provides custom implementations for trait-related methods used by the GM tools.</p>
	 * 
	 * <p><b>Thread Safety:</b> This class is thread-safe.</p>
	 * 
	 * @since 1.0.0
	 */
	private static class GmSetTraitsCallbacks {
		/**
		 * Gets the trait string representation for a creature's trait.
		 * 
		 * <p>If the creature has custom traits and the specified trait is a custom trait,
		 * this method returns the custom color name or trait name. Otherwise, it falls back
		 * to the default trait string.</p>
		 * 
		 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
		 * 
		 * @param creature the creature
		 * @param trait the trait number
		 * @return the trait string representation
		 * @since 1.0.0
		 */
		@CallbackApi
		public String getTraitString(Creature creature, int trait) {
			ModCreature modCreature = ModCreatures.getModCreature(creature.getTemplate().getTemplateId());
			if (modCreature != null && isCustomTrait(trait)) {
				String colorName = modCreature.getColourName(trait);
				if (colorName != null)
					return StringUtilities.raiseFirstLetterOnly(colorName);
				
				String traitName = modCreature.getTraitName(trait);
				if (traitName != null)
					return traitName;
			}
			
			return Traits.getTraitString(trait);
		}
	}
	
	/**
	 * Callbacks for creature-related functionality.
	 * 
	 * <p>Provides custom implementations for various creature methods that are hooked
	 * into the Wurm Unlimited server.</p>
	 * 
	 * <p><b>Thread Safety:</b> This class is thread-safe.</p>
	 * 
	 * @since 1.0.0
	 */
	private static class CreatureCallbacks {
		
		/**
		 * Gets the model name for a creature.
		 * 
		 * <p>Delegates to {@link ModCreatures#getModelName(Creature)}.</p>
		 * 
		 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
		 * 
		 * @param creature the creature
		 * @return the model name, or {@code null} if not applicable
		 * @since 1.0.0
		 * @see ModCreatures#getModelName(Creature)
		 */
		@CallbackApi
		public String getModelName(Creature creature) {
			return ModCreatures.getModelName(creature);
		}
		
		/**
		 * Gets the color name for a creature.
		 * 
		 * <p>Delegates to {@link ModCreatures#getColourName(Creature)}.</p>
		 * 
		 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
		 * 
		 * @param creature the creature
		 * @return the color name, or {@code null} if not applicable
		 * @since 1.0.0
		 * @see ModCreatures#getColourName(Creature)
		 */
		@CallbackApi
		public String getColourName(Creature creature) {
			return ModCreatures.getColourName(creature);
		}
		
		/**
		 * Gets the color name for a specific trait of a creature.
		 * 
		 * <p>If the creature has custom traits, returns the color name for the specified
		 * trait or an empty string if no color name is defined. Otherwise, returns {@code null}.</p>
		 * 
		 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
		 * 
		 * @param creature the creature
		 * @param trait the trait number
		 * @return the color name, empty string, or {@code null}
		 * @since 1.0.0
		 */
		@CallbackApi
		public String getColourName(Creature creature, int trait) {
			ModCreature c = ModCreatures.getModCreature(creature.getTemplate().getTemplateId());
			if (c != null && c.hasTraits()) {
				// The creature has traits. Return the color name or an empty string
				String color = c.getColourName(trait);
				if (color != null) {
					return color;
				}
				return "";
			}
			return null;
		}
		
		
		/**
		 * Gets the trait name for a creature.
		 * 
		 * <p>Delegates to {@link ModCreatures#getTraitName(Creature)}.</p>
		 * 
		 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
		 * 
		 * @return the trait name, or {@code null} if not applicable
		 * @since 1.0.0
		 * @see ModCreatures#getTraitName(Creature)
		 */
		@CallbackApi
		public String getTraitName() {
			return ModCreatures.getTraitName(null);
		}
		
		/**
		 * Checks if a creature template has custom traits.
		 * 
		 * <p>Delegates to {@link ModCreatures#hasTraits(int)}.</p>
		 * 
		 * <p><b>Thread Safety:</b> This method is thread-safe.</p>
		 * 
		 * @param templateId the template ID to check
		 * @return {@code true} if the template has custom traits, {@code false} otherwise
		 * @since 1.0.0
		 * @see ModCreatures#hasTraits(int)
		 */
		@CallbackApi
		public boolean hasTraits(int templateId) {
			return ModCreatures.hasTraits(templateId);
		}
		
		/**
		 * Calculates new traits for breeding between two creatures.
		 * 
		 * <p>Delegates to {@link ModCreatures#calcNewTraits(double, boolean, Creature, Creature)}.
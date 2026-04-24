package com.garward.wurmmodloader.modloader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.garward.wurmmodloader.modloader.internal.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.internal.interfaces.Initable;
import com.garward.wurmmodloader.modloader.interfaces.ModEntry;
import com.garward.wurmmodloader.modloader.interfaces.ModListener;
import com.garward.wurmmodloader.modloader.internal.interfaces.PreInitable;
import com.garward.wurmmodloader.api.interfaces.Versioned;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import com.garward.wurmmodloader.modcomm.intra.ModIntraServer;
import com.garward.wurmmodloader.modloader.internal.DefrostingClassLoader;
import com.garward.wurmmodloader.modloader.internal.ModInfo;
import com.garward.wurmmodloader.modloader.internal.ModInstanceBuilder;
import com.garward.wurmmodloader.modloader.internal.ReflectionUtil;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.internal.dependency.DependencyResolver;
import com.garward.wurmmodloader.modsupport.actions.ActionEntryBuilder;
import com.garward.wurmmodloader.modsupport.items.ModItems;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

/**
 * WurmModLoader's main mod loader implementation - completely independent of Ago's modloader-shared.
 *
 * <p>Uses {@link ModDiscovery} for flexible mod layout support:
 * <ul>
 *   <li>Legacy subfolder: {@code mods/modname/modname.jar}</li>
 *   <li>Flat with properties: {@code mods/modname.jar} + {@code mods/modname.properties}</li>
 *   <li>Modern flat: {@code mods/modname.jar} (metadata from JAR manifest)</li>
 *   <li>Versioned flat: {@code mods/armoury-0.8.0.jar} (auto-detected)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ModLoader implements Versioned {

	private static final Logger logger = Logger.getLogger(ModLoader.class.getName());
	private final ModDiscovery modDiscovery;
	private final Class<? extends WurmServerMod> modClass;

	public ModLoader() {
		this.modClass = WurmServerMod.class;
		this.modDiscovery = new ModDiscovery();
	}

	/**
	 * Helper methods to check for both internal and public API interfaces
	 */
	private boolean isConfigurable(Object mod) {
		return mod instanceof Configurable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.Configurable;
	}

	private boolean isPreInitable(Object mod) {
		return mod instanceof PreInitable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.PreInitable;
	}

	private boolean isInitable(Object mod) {
		return mod instanceof Initable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.Initable;
	}

	/**
	 * Discovers and loads all mods from the specified directory using our modern discovery system.
	 *
	 * @param modDir the directory containing mods
	 * @return list of loaded mod entries
	 * @throws IOException if mod loading fails
	 */
	public List<? extends ModEntry<WurmServerMod>> loadModsFromModDir(Path modDir) throws IOException {
		String version = this.getVersion();
		logger.info(String.format("[ModLoader] Modern WurmModLoader version %s", version));

		String modLoaderProvided = "modloader";
		if (version != null && !version.isEmpty()) {
			modLoaderProvided = modLoaderProvided + "@" + version;
		}

		LinkedHashSet<String> provided = new LinkedHashSet<>();
		provided.add(modLoaderProvided);

		String steamVersion = this.getGameVersion();
		provided.add("wurmunlimited@" + steamVersion);
		logger.info(String.format("[ModLoader] Game version %s", steamVersion));

		// Use our modern discovery system
		List<ModInfo> unorderedMods = modDiscovery.discoverMods(modDir);
		logger.info(String.format("[ModLoader] Discovered %d mods before dependency resolution", unorderedMods.size()));

		ModInstanceBuilder<? extends WurmServerMod> entryBuilder = new ModInstanceBuilder<>(this.modClass);

		// Resolve dependencies and create mod instances
		DependencyResolver<ModInfo> resolver = new DependencyResolver<>();
		List<ModInfo> orderedMods = resolver
				.provided(Collections.singleton(modLoaderProvided))
				.order(unorderedMods);

		List<Entry> mods = orderedMods.stream()
				.map(modInfo -> {
					logger.fine("[ModLoader] Loading mod: " + modInfo.getName());
					modInfo.getProperties().put("steamVersion", steamVersion);
					WurmServerMod mod = entryBuilder.createModInstance(modInfo);
					return new Entry(modInfo, mod);
				})
				.collect(Collectors.toList());

		// Log loaded mods
		mods.forEach(modEntry -> {
			String implementationVersion = modEntry.mod.getVersion();
			if (implementationVersion == null || implementationVersion.isEmpty()) {
				implementationVersion = "unversioned";
			}
			logger.info(String.format("[ModLoader] Loading %s as %s (%s)",
					modEntry.mod.getClass().getName(), modEntry.getName(), implementationVersion));
		});

		// Phase 1: configure() for PreInitable/Initable mods with Configurable
		mods.stream()
				.filter(modEntry -> (isInitable(modEntry.mod) || isPreInitable(modEntry.mod))
						&& isConfigurable(modEntry.mod))
				.forEach(modEntry -> {
					logger.fine("[ModLoader] Configuring (early): " + modEntry.getName());
					if (modEntry.mod instanceof Configurable) {
						((Configurable) modEntry.mod).configure(modEntry.getProperties());
					} else {
						((com.garward.wurmmodloader.modloader.interfaces.Configurable) modEntry.mod).configure(modEntry.getProperties());
					}
				});

		// Phase 2: preInit() for all PreInitable mods
		// NOTE: ModComm initialization moved to DelegatedLauncher to prevent class freezing
		mods.stream()
				.filter(modEntry -> isPreInitable(modEntry.mod))
				.forEach(modEntry -> {
					logger.fine("[ModLoader] PreInit: " + modEntry.getName());
					if (modEntry.mod instanceof PreInitable) {
						((PreInitable) modEntry.mod).preInit();
					} else {
						((com.garward.wurmmodloader.modloader.interfaces.PreInitable) modEntry.mod).preInit();
					}
				});

		// Phase 3: Framework preInit hook
		logger.fine("[ModLoader] Framework preInit");
		this.preInit();

		// Phase 4: init() for all Initable mods
		mods.stream()
				.filter(modEntry -> isInitable(modEntry.mod))
				.forEach(modEntry -> {
					logger.fine("[ModLoader] Init: " + modEntry.getName());
					if (modEntry.mod instanceof Initable) {
						((Initable) modEntry.mod).init();
					} else {
						((com.garward.wurmmodloader.modloader.interfaces.Initable) modEntry.mod).init();
					}
				});

		// Phase 5: Framework init hook
		logger.fine("[ModLoader] Framework init");
		this.init();

		// Phase 6: configure() for non-Initable, non-PreInitable mods with Configurable
		mods.stream()
				.filter(modEntry -> !(isInitable(modEntry.mod))
						&& !(isPreInitable(modEntry.mod))
						&& isConfigurable(modEntry.mod))
				.forEach(modEntry -> {
					logger.fine("[ModLoader] Configuring (late): " + modEntry.getName());
					if (modEntry.mod instanceof Configurable) {
						((Configurable) modEntry.mod).configure(modEntry.getProperties());
					} else {
						((com.garward.wurmmodloader.modloader.interfaces.Configurable) modEntry.mod).configure(modEntry.getProperties());
					}
				});

		// Phase 7: modInitialized() callbacks for ModListener mods. Legacy
		// org.gotti.* ModListener extends the canonical one and default-bridges
		// to its legacy signature, so a single scan covers both paths.
		mods.stream()
				.filter(modEntry -> modEntry.mod instanceof ModListener)
				.forEach(modEntry -> {
					logger.fine("[ModLoader] ModListener callbacks: " + modEntry.getName());
					mods.forEach(mod -> ((ModListener) modEntry.mod).modInitialized(mod));
				});

		logger.info(String.format("[ModLoader] Successfully loaded %d mods", mods.size()));
		ModRegistry.getInstance().register(mods);
		return mods;
	}

	@Override
	public String getVersion() {
		// Return our modern version
		return "0.8.0";
	}

	/**
	 * Gets the Wurm Unlimited game version by loading SteamVersion class.
	 *
	 * @return game version string
	 * @throws HookException if version cannot be determined
	 */
	public String getGameVersion() {
		ClassPool classPool = HookManager.getInstance().getClassPool();
		try (DefrostingClassLoader loader = new DefrostingClassLoader(classPool)) {
			Class<?> clazz = loader.loadClass("com.wurmonline.shared.constants.SteamVersion");
			Method getCurrentVersion = ReflectionUtil.getMethod(clazz, "getCurrentVersion");
			return getCurrentVersion.invoke(clazz).toString();
		} catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException | CannotCompileException | NotFoundException e) {
			// SteamVersion stub throws RuntimeException("Unimplemented")
			// This is expected when running with modloader - return a default version
			logger.warning("[ModLoader] Could not determine game version, using default: " + e.getMessage());
			return "1.9.0.0";
		}
	}

	/**
	 * Framework hook for ModComm initialization.
	 */
	protected void modcommInit() {
		System.out.println("[ModLoader] DEBUG: modcommInit() called - initializing ModComm");

		// Initialize legacy ModComm (org.gotti package)
		org.gotti.wurmunlimited.modcomm.ModComm.init();
		System.out.println("[ModLoader] DEBUG: Legacy ModComm initialized successfully");

		// Initialize ModIntraServer
		ModIntraServer.init();
		System.out.println("[ModLoader] DEBUG: ModIntraServer initialized successfully");

		// Initialize database performance optimizations
		com.garward.wurmmodloader.performance.DatabaseOptimizer.init();
		System.out.println("[ModLoader] DEBUG: DatabaseOptimizer initialized successfully");
	}

	/**
	 * Framework hook for preInit phase.
	 *
	 * NOTE: CreatureStatusBatcher moved to DelegatedLauncher (after ModSupport.init())
	 * to avoid freezing Creature class before ModCreatures can add callbacks.
	 */
	protected void preInit() {
		// Empty now - performance optimizations moved to DelegatedLauncher
	}

	/**
	 * Framework hook for init phase.
	 *
	 * NOTE: ModActions, ModItems, ModCreatures are now initialized early in DelegatedLauncher
	 * to avoid class freezing issues. They are no longer initialized here.
	 */
	protected void init() {
		// Initialize new package (Phase 6+ mods)
		ActionEntryBuilder.init();
		// ModItems.init() - MOVED TO DelegatedLauncher early boot phase
		// ModActions.init() - MOVED TO DelegatedLauncher early boot phase
		// ModCreatures.init() - MOVED TO DelegatedLauncher early boot phase

		// CRITICAL: Also initialize legacy package (for legacy mods) via reflection
		try {
			Class<?> legacyBuilder = Class.forName("org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder");
			legacyBuilder.getMethod("init").invoke(null);
		} catch (Exception e) {
			// Legacy module not present - this is OK for Phase 6+ only deployments
			System.out.println("Note: Legacy ActionEntryBuilder not found (Phase 6+ mode)");
		}
	}

	/**
	 * Internal entry wrapper that implements ModEntry interface.
	 */
	private class Entry extends ModInfo implements ModEntry<WurmServerMod>, org.gotti.wurmunlimited.modloader.interfaces.ModEntry<WurmServerMod> {
		private final WurmServerMod mod;

		public Entry(ModInfo modInfo, WurmServerMod mod) {
			super(modInfo.getProperties(), modInfo.getName(), modInfo.getPropsFile(), modInfo.getConfigFile());
			this.mod = mod;
		}

		@Override
		public WurmServerMod getWurmMod() {
			return mod;
		}

		@Override
		public ClassLoader getModClassLoader() {
			return mod.getClass().getClassLoader();
		}
	}
}

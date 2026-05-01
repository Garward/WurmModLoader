package com.garward.wurmmodloader.modloader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.internal.dependency.DependencyResolver;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.Initable;
import com.garward.wurmmodloader.modloader.interfaces.ModEntry;
import com.garward.wurmmodloader.modloader.interfaces.ModListener;
import com.garward.wurmmodloader.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.Versioned;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

public abstract class ModLoaderShared<T extends Versioned> implements Versioned {
	
	private static Logger logger = Logger.getLogger(ModLoaderShared.class.getName());
	
	private class Entry extends ModInfo implements ModEntry<T>, org.gotti.wurmunlimited.modloader.interfaces.ModEntry<T> {

		T mod;

		public Entry(T mod, Properties properties, String name) {
			super(properties, name);
			this.mod = mod;
		}

		@Override
		public T getWurmMod() {
			return mod;
		}

		@Override
		public ClassLoader getModClassLoader() {
			return mod.getClass().getClassLoader();
		}
	}
	
	private Class<? extends T> modClass;
	
	public ModLoaderShared(Class<? extends T> modClass) {
		this.modClass = modClass;
	}

	protected abstract void modcommInit();
	protected abstract void preInit();
	protected abstract void init();

	/**
	 * Helper methods to check for both internal and public API interfaces
	 */
	private boolean isConfigurable(Object mod) {
		return mod instanceof Configurable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.Configurable;
	}

	private boolean isPreInitable(Object mod) {
		return mod instanceof PreInitable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.PreInitable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
	}

	private boolean isInitable(Object mod) {
		return mod instanceof Initable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.Initable ||
		       mod instanceof com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
	}

	/**
	 * Find installed mods and read the properties.
	 * <p>
	 * It loads the properties in the following order with later entries overriding earlier ones
	 * <ul>
	 * <li>Properties from mods/modname/modname.jar!META-INF/org.gotti.wurmunlimited.modloader/modname.properties</li>
	 * <li>Properties from mods/modname.properties</li>
	 * <li>Properties from mods/modname.config</li>
	 * </ul>
	 * <p>
	 * The mod will be set to ondemand loading if properties from the jar file exist but no .properties file. This will
	 * enable support mods like httpserver to load without a .properties file when needed. Mods can set depend.ondemand=false
	 * in the properties file in the jar to force loading the mod. Setting depend.ondemand=false in modname.config will then
	 * disable the mod per user request, although removing the files of the mod would probably be a better option.
	 * <p>
	 *
	 * @param modDir mods folder
	 * @return
	 * @throws IOException
	 */
	private List<ModInfo> discoverMods(Path modDir) throws IOException {
		final List<ModInfo> unorderedMods = new ArrayList<>();
		final Set<String> handled = new HashSet<>();
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modDir, "*.properties")) {
			for (Path modInfo : directoryStream) {
				String modName = modInfo.getFileName().toString().replaceAll("\\.properties$", "");
				Path modJar = modDir.resolve(modName).resolve(modName + ".jar");
				ModInfo mod = loadModFromInfo(modName, modInfo, modJar);
				unorderedMods.add(mod);
				handled.add(modName);
			}
		}
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modDir, path -> Files.isDirectory(path) && !handled.contains(path.getFileName().toString()))) {
			for (Path modInfo : directoryStream) {
				String modName = modInfo.getFileName().toString();
				Path modJar = modDir.resolve(modName).resolve(modName + ".jar");
				if (Files.exists(modJar)) {
					ModInfo mod = loadModFromInfo(modName, null, modJar);
					unorderedMods.add(mod);
					handled.add(modName);
				}
			}
		}
		
		return unorderedMods;
	}
	
	public List<? extends ModEntry<T>> loadModsFromModDir(Path modDir) throws IOException {
		BootLogger.startBoot();

		final String version = this.getVersion();
		logger.info(String.format("ModLoader version %1$s", version));

		String modLoaderProvided = "modloader";
		if (version != null && !version.isEmpty()) {
			modLoaderProvided += "@" + version;
		}

		Set<String> provided = new LinkedHashSet<>();
		provided.add(modLoaderProvided);

		String steamVersion = getGameVersion();
		provided.add("wurmunlimited@" + steamVersion);
		logger.info(String.format("Game version %1$s", steamVersion));

		// Phase 1: Mod Discovery
		BootLogger.startPhase(1, 7, "Mod Discovery");
		final List<ModInfo> unorderedMods = discoverMods(modDir);
		BootLogger.phaseStat("DISCOVERY", String.format("Found %d mod(s) in %s", unorderedMods.size(), modDir));
		BootLogger.completePhase(1, 7);

		// Phase 2: Dependency Resolution
		BootLogger.startPhase(2, 7, "Dependency Resolution");
		ModInstanceBuilder<T> entryBuilder = new ModInstanceBuilder<T>(modClass);
		BootLogger.PhaseTracker loadTracker = new BootLogger.PhaseTracker("MODLOADER");
		List<Entry> mods = new DependencyResolver<ModInfo>().provided(Collections.singleton(modLoaderProvided)).order(unorderedMods).stream().map(modInfo -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modInfo.getName(), "load")) {
				modInfo.getProperties().put("steamVersion", steamVersion);
				Entry entry = new Entry(entryBuilder.createModInstance(modInfo), modInfo.getProperties(), modInfo.getName());
				loadTracker.recordSuccess();
				return entry;
			} catch (Exception e) {
				loadTracker.recordError(modInfo.getName() + ": " + e.getMessage());
				throw e;
			}
		}).collect(Collectors.toList());

		mods.stream().forEach(modEntry -> {
			String implementationVersion = modEntry.mod.getVersion();
			if (implementationVersion == null || implementationVersion.isEmpty()) {
				implementationVersion = "unversioned";
			}
			logger.info(String.format("Loading %1$s as %2$s (%3$s)", modEntry.mod.getClass().getName(),  modEntry.getName(), implementationVersion));
		});
		loadTracker.logResults();
		if (loadTracker.getErrorCount() > 0) {
			BootLogger.completePhaseWithErrors(2, 7, loadTracker.getErrorCount());
		} else {
			BootLogger.completePhase(2, 7);
		}

		// Phase 3: Configuration
		BootLogger.startPhase(3, 7, "Mod Configuration");
		BootLogger.PhaseTracker configTracker = new BootLogger.PhaseTracker("CONFIGURE");
		mods.stream().filter(modEntry -> (isInitable(modEntry.mod) || isPreInitable(modEntry.mod)) && isConfigurable(modEntry.mod)).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "configure")) {
				if (modEntry.mod instanceof Configurable) {
					((Configurable) modEntry.mod).configure(modEntry.getProperties());
				} else {
					((com.garward.wurmmodloader.modloader.interfaces.Configurable) modEntry.mod).configure(modEntry.getProperties());
				}
				configTracker.recordSuccess();
			} catch (Exception e) {
				configTracker.recordError(modEntry.getName() + ": " + e.getMessage());
				throw e;
			}
		});
		configTracker.logResults();
		if (configTracker.getErrorCount() > 0) {
			BootLogger.completePhaseWithErrors(3, 7, configTracker.getErrorCount());
		} else {
			BootLogger.completePhase(3, 7);
		}

		// Phase 4: PreInit (Bytecode Hooks)
		BootLogger.startPhase(4, 7, "PreInit (Bytecode Hooks)");
		BootLogger.PhaseTracker preInitTracker = new BootLogger.PhaseTracker("PREINIT");

		try (EarlyLoadingChecker c = EarlyLoadingChecker.init("ModComm", "init")) {
			modcommInit();
		}

		mods.stream().filter(modEntry -> isPreInitable(modEntry.mod)).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "preinit")) {
				if (modEntry.mod instanceof PreInitable) {
					((PreInitable)modEntry.mod).preInit();
				} else if (modEntry.mod instanceof com.garward.wurmmodloader.modloader.interfaces.PreInitable) {
					((com.garward.wurmmodloader.modloader.interfaces.PreInitable)modEntry.mod).preInit();
				} else {
					((com.garward.wurmmodloader.modloader.interfaces.WurmServerMod)modEntry.mod).preInit();
				}
				preInitTracker.recordSuccess();
			} catch (Exception e) {
				preInitTracker.recordError(modEntry.getName() + ": " + e.getMessage());
				throw e;
			}
		});

		// Warn about mods with no PreInitable
		long modsWithoutPreInit = mods.stream().filter(modEntry -> !isPreInitable(modEntry.mod)).count();
		if (modsWithoutPreInit > 0) {
			preInitTracker.recordWarning(String.format("%d mod(s) have no PreInitable (no bytecode hooks)", modsWithoutPreInit));
		}

		preInit();
		preInitTracker.logResults();
		if (preInitTracker.getErrorCount() > 0) {
			BootLogger.completePhaseWithErrors(4, 7, preInitTracker.getErrorCount());
		} else if (preInitTracker.getWarningCount() > 0) {
			BootLogger.completePhaseWithWarnings(4, 7, preInitTracker.getWarningCount());
		} else {
			BootLogger.completePhase(4, 7);
		}

		// Phase 5: Init (Action Registration, Item Creation)
		BootLogger.startPhase(5, 7, "Init (Actions, Items, Creatures)");
		BootLogger.PhaseTracker initTracker = new BootLogger.PhaseTracker("INIT");

		mods.stream().filter(modEntry -> isInitable(modEntry.mod)).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "init")) {
				if (modEntry.mod instanceof Initable) {
					((Initable)modEntry.mod).init();
				} else if (modEntry.mod instanceof com.garward.wurmmodloader.modloader.interfaces.Initable) {
					((com.garward.wurmmodloader.modloader.interfaces.Initable)modEntry.mod).init();
				} else {
					((com.garward.wurmmodloader.modloader.interfaces.WurmServerMod)modEntry.mod).init();
				}
				initTracker.recordSuccess();
			} catch (Exception e) {
				initTracker.recordError(modEntry.getName() + ": " + e.getMessage());
				throw e;
			}
		});

		// Warn about mods with no Initable
		long modsWithoutInit = mods.stream().filter(modEntry -> !isInitable(modEntry.mod)).count();
		if (modsWithoutInit > 0) {
			initTracker.recordWarning(String.format("%d mod(s) have no Initable (no init phase)", modsWithoutInit));
		}

		init();
		initTracker.logResults();
		if (initTracker.getErrorCount() > 0) {
			BootLogger.completePhaseWithErrors(5, 7, initTracker.getErrorCount());
		} else if (initTracker.getWarningCount() > 0) {
			BootLogger.completePhaseWithWarnings(5, 7, initTracker.getWarningCount());
		} else {
			BootLogger.completePhase(5, 7);
		}

		// Phase 6: Legacy Configuration (old-style mods)
		BootLogger.startPhase(6, 7, "Legacy Mod Configuration");
		BootLogger.PhaseTracker legacyConfigTracker = new BootLogger.PhaseTracker("LEGACY");
		mods.stream().filter(modEntry -> !(isInitable(modEntry.mod) || isPreInitable(modEntry.mod)) && isConfigurable(modEntry.mod)).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "configure")) {
				if (modEntry.mod instanceof Configurable) {
					((Configurable) modEntry.mod).configure(modEntry.getProperties());
				} else {
					((com.garward.wurmmodloader.modloader.interfaces.Configurable) modEntry.mod).configure(modEntry.getProperties());
				}
				legacyConfigTracker.recordSuccess();
			} catch (Exception e) {
				legacyConfigTracker.recordError(modEntry.getName() + ": " + e.getMessage());
				throw e;
			}
		});
		legacyConfigTracker.logResults();
		if (legacyConfigTracker.getTotalCount() == 0) {
			BootLogger.phaseStat("LEGACY", "No legacy mods detected");
		}
		if (legacyConfigTracker.getErrorCount() > 0) {
			BootLogger.completePhaseWithErrors(6, 7, legacyConfigTracker.getErrorCount());
		} else {
			BootLogger.completePhase(6, 7);
		}

		// Phase 7: ModListener Notifications
		BootLogger.startPhase(7, 7, "ModListener Notifications");
		BootLogger.PhaseTracker listenerTracker = new BootLogger.PhaseTracker("LISTENERS");
		mods.stream().filter(modEntry -> modEntry.mod instanceof ModListener).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "modListener")) {
				mods.stream().forEach(mod -> ((ModListener)modEntry.mod).modInitialized(mod));
				listenerTracker.recordSuccess();
			} catch (Exception e) {
				listenerTracker.recordError(modEntry.getName() + ": " + e.getMessage());
				throw e;
			}
		});
		listenerTracker.logResults();
		if (listenerTracker.getErrorCount() > 0) {
			BootLogger.completePhaseWithErrors(7, 7, listenerTracker.getErrorCount());
		} else {
			BootLogger.completePhase(7, 7);
		}

		// Calculate total errors and warnings
		int totalErrors = loadTracker.getErrorCount() + configTracker.getErrorCount() +
			preInitTracker.getErrorCount() + initTracker.getErrorCount() +
			legacyConfigTracker.getErrorCount() + listenerTracker.getErrorCount();
		int totalWarnings = preInitTracker.getWarningCount() + initTracker.getWarningCount();

		BootLogger.completeBoot(totalErrors, totalWarnings);

		return mods;
	}
	
	/**
	 * Load the default properties from mods/modname/modname.jar!META-INF/org.gotti.wurmunlimited.modloader/modname.properties
	 *
	 * @param modName Modname
	 * @param jarFile Jar-file of the mod
	 * @return Properties
	 * @throws IOException
	 */
	private Properties loadDefaultPropertiesFromJar(String modName, Path jarFile) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri()), new HashMap<>())) {
			Path propsFile = fs.getPath("/META-INF/" + ModLoaderShared.class.getPackage().getName() + "/" + modName + ".properties");
			if (Files.exists(propsFile)) {
				logger.log(Level.INFO, "Reading " + jarFile.toString() + "!" + propsFile.toString());
				try (InputStream inputStream = Files.newInputStream(propsFile)) {
					Properties properties = new Properties();
					properties.load(inputStream);
					return properties;
				}
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
		return new Properties();
	}
	
	/**
	 * Extract the packaged config file
	 * @param modName Mod name
	 * @param jarFile Mod jar
	 * @param configFile Target config file
	 * @throws IOException
	 */
	private void copyConfigFromJar(String modName, Path jarFile, Path configFile) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri()), new HashMap<>())) {
			Path configTemplate = fs.getPath("/META-INF/" + ModLoaderShared.class.getPackage().getName() + "/" + modName + ".config");
			if (Files.exists(configTemplate)) {
				logger.log(Level.INFO, "Copying " + jarFile + "!" + configTemplate + " to " + configFile);
				Files.copy(configTemplate, configFile);
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Load mod properties from .properties and .config files
	 * @param modName Modname
	 * @param modInfo properties file
	 * @param jarFile jar file
	 * @return Mod properties
	 * @throws IOException
	 */
	private ModInfo loadModFromInfo(String modName, Path modInfo, Path jarFile) throws IOException {
		Path configFile = Paths.get("mods", modName + ".config");
		
		Properties properties = new Properties();
		if (jarFile != null && Files.exists(jarFile)) {
			if (modInfo == null || !Files.exists(modInfo)) {
				properties.put("depend.ondemand", "true");
			}
			properties.putAll(loadDefaultPropertiesFromJar(modName, jarFile));
			
			if (!Files.exists(configFile)) {
				copyConfigFromJar(modName, jarFile, configFile);
			}
		}

		if (modInfo != null) {
			logger.log(Level.INFO, "Reading " + modInfo.toString());
			try (InputStream inputStream = Files.newInputStream(modInfo)) {
				properties.load(inputStream);
			}
		}

		if (Files.exists(configFile)) {
			try (InputStream inputStream = Files.newInputStream(configFile)) {
				logger.log(Level.INFO, "Reading " + configFile.toString());
				properties.load(inputStream);
			}
		}
		
		return new ModInfo(properties, modName);
	}
	
	/**
	 * Get the server version as announced on steam
	 * @return Steam game version
	 */
	public String getGameVersion() {
		final ClassPool classPool = HookManager.getInstance().getClassPool();
		
		try (DefrostingClassLoader loader = new DefrostingClassLoader(classPool)) {
			final Class<?> clazz = loader.loadClass("com.wurmonline.shared.constants.SteamVersion");
			final Method getCurrentVersion = ReflectionUtil.getMethod(clazz, "getCurrentVersion");
			return getCurrentVersion.invoke(clazz).toString();
		} catch (NotFoundException | CannotCompileException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new HookException(e);
		}
	}
}

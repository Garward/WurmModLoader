package com.garward.wurmmodloader.serverlauncher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.interfaces.ModEntry;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import com.garward.wurmmodloader.core.bytecode.PatchSettings;
import com.garward.wurmmodloader.core.registry.SystemBootstrap;
import com.garward.wurmmodloader.modloader.ModLoader;
import com.garward.wurmmodloader.modloader.server.ServerHook;

public class DelegatedLauncher {

	private static final Logger LOGGER = Logger.getLogger(DelegatedLauncher.class.getName());
	private static volatile boolean loggingConfigured = false;

	public static void main(String[] args) {

		try {
			configureFileLogging();
			com.garward.wurmmodloader.debug.ShutdownForensics.install();
			String[] sanitizedArgs = processInternalFlags(args);

			// Capture the world-folder name from "start=<Name>" (case-insensitive,
			// matches WU's GuiCommandLineArgument.START) into a system property.
			// ServerHook reads this during early config sync. Using a property
			// (not a ServerHook setter) keeps us from class-loading ServerHook
			// here — it transitively references com.wurmonline.* which would
			// freeze Player before CapabilityHooks.installHooks() can patch it.
			for (String arg : sanitizedArgs) {
				if (arg != null && arg.length() > 6
						&& arg.regionMatches(true, 0, "start=", 0, 6)) {
					String worldName = arg.substring(6).trim();
					if (!worldName.isEmpty()) {
						System.setProperty("wurmmodloader.launchWorldFolder", worldName);
					}
					break;
				}
			}
			// CRITICAL: Install capability hooks FIRST, before ANY Wurm classes are loaded
			// This must happen before mods load, before ServerHook is created, before
			// anything
			System.out.println("[DelegatedLauncher] Installing capability hooks...");
			LOGGER.info("Installing capability hooks...");
			com.garward.wurmmodloader.core.capability.CapabilityHooks.installHooks();
			System.out.println("[DelegatedLauncher] Capability hooks installed");
			LOGGER.info("Capability hooks installed successfully");

			// CRITICAL: Load mods and run preInit() SECOND, BEFORE SystemBootstrap
			// This allows legacy mods to patch classes BEFORE bytecode patches load/freeze them
			// For drag-and-drop legacy mod compatibility (e.g., bag of holding, old mods)
			ModLoader modLoader = new ModLoader();
			System.out.println("[DelegatedLauncher] About to load mods...");
			LOGGER.info("DEBUG: About to load mods from mods/ directory");

			// Look for mods directory - check current directory first, then parent
			Path modsPath = Paths.get("mods");
			if (!Files.exists(modsPath)) {
				modsPath = Paths.get("../mods");
				LOGGER.info("DEBUG: mods/ not found in current directory, trying ../mods");
			}

			normalizeLegacyModProperties(modsPath);

			List<? extends ModEntry<com.garward.wurmmodloader.modloader.interfaces.WurmServerMod>> wurmMods = modLoader.loadModsFromModDir(modsPath);
			LOGGER.info("DEBUG: loadModsFromModDir returned list of size: " + wurmMods.size());
			if (wurmMods.isEmpty()) {
				LOGGER.warning("DEBUG: No mods were loaded! Check modloader-shared logs above for errors");
			} else {
				for (ModEntry<com.garward.wurmmodloader.modloader.interfaces.WurmServerMod> mod : wurmMods) {
					LOGGER.info(
							"DEBUG: Loaded mod: " + mod.getName() + " (" + mod.getWurmMod().getClass().getName() + ")");
				}
			}

			// CRITICAL: Initialize ModSupport framework hooks BEFORE bytecode patches
			// ModSupport needs to add callbacks to classes while they're still unfrozen
			System.out.println("[DelegatedLauncher] Initializing ModSupport framework hooks (ModActions, ModItems, ModCreatures)...");
			LOGGER.info("Initializing ModSupport framework hooks before bytecode patches...");
			com.garward.wurmmodloader.modsupport.actions.ModActions.init();
			com.garward.wurmmodloader.modsupport.items.ModItems.init();
			com.garward.wurmmodloader.modsupport.creatures.ModCreatures.init();
			System.out.println("[DelegatedLauncher] ModSupport framework hooks installed successfully");
			LOGGER.info("ModSupport framework hooks installed successfully");

			// CRITICAL: Initialize CreatureStatusBatcher AFTER ModCreatures adds callbacks
			// CreatureStatusBatcher hooks DbCreatureStatus which references Creature

			System.out.println("[DelegatedLauncher] Initializing CreatureStatusBatcher...");
			LOGGER.info("Initializing CreatureStatusBatcher for performance optimization");
			com.garward.wurmmodloader.performance.CreatureStatusBatcher.init();
			System.out.println("[DelegatedLauncher] Performance optimizations initialized");
			LOGGER.info("CreatureStatusBatcher initialized successfully");

			// Apply DbCreatureStatus patch for custom database columns
			System.out.println("[DelegatedLauncher] Applying DbCreatureStatusPatch...");
			LOGGER.info("Applying DbCreatureStatus bytecode patch for custom database columns");
			com.garward.wurmmodloader.core.bytecode.patches.DbCreatureStatusPatch.apply();
			LOGGER.info("DbCreatureStatusPatch applied successfully");

			// Apply DbDeity patch for deity database save/load events
			System.out.println("[DelegatedLauncher] Applying DbDeityPatch...");
			LOGGER.info("Applying DbDeity bytecode patch for deity database events");
			com.garward.wurmmodloader.core.bytecode.patches.DbDeityPatch.apply();
			LOGGER.info("DbDeityPatch applied successfully");

			// Apply DbStructure patch for structure database save/load events
			System.out.println("[DelegatedLauncher] Applying DbStructurePatch...");
			LOGGER.info("Applying DbStructure bytecode patch for structure database events");
			com.garward.wurmmodloader.core.bytecode.patches.DbStructurePatch.apply();
			LOGGER.info("DbStructurePatch applied successfully");

			// Apply CommandReader patch for console GM commands
			System.out.println("[DelegatedLauncher] Applying CommandReaderPatch...");
			LOGGER.info("Applying CommandReader bytecode patch for console GM commands");
			com.garward.wurmmodloader.core.bytecode.patches.CommandReaderPatch.apply();
			LOGGER.info("CommandReaderPatch applied successfully");

			// Initialize ModComm BEFORE bytecode patches
			// ModComm needs to modify Player class before patches that reference PlayerInfo/DbPlayerInfo load it
			System.out.println("[DelegatedLauncher] Initializing ModComm...");
			LOGGER.info("Initializing ModComm before bytecode patches...");
			com.garward.wurmmodloader.modcomm.ModComm.init();
			System.out.println("[DelegatedLauncher] ModComm initialized");
			LOGGER.info("ModComm initialized successfully");

			// CRITICAL: Apply bytecode patches AFTER mods' preInit(), ModSupport, and ModComm
			// Legacy mods patch classes first, ModSupport adds callbacks, ModComm modifies Player, then bytecode patches freeze classes
			// This allows drag-and-drop legacy mod compatibility
			System.out.println("[DelegatedLauncher] Bootstrapping runtime registries...");
			LOGGER.info("Bootstrapping runtime registries and applying bytecode patches...");
			SystemBootstrap.initializeAll();
			System.out.println("[DelegatedLauncher] Bytecode patches applied");
			LOGGER.info("Bytecode patches applied successfully");

			// Create ServerHook
			System.out.println("[DelegatedLauncher] Creating ServerHook...");
			LOGGER.info("Creating ServerHook and registering framework hooks...");
			ServerHook serverHook = ServerHook.createServerHook();
			System.out.println("[DelegatedLauncher] ServerHook created");
			LOGGER.info("ServerHook created, all framework hooks registered");

			// Install framework HTTP server subsystem (replaces legacy httpserver mod).
			// Must be AFTER ServerHook creation (event bus exists) and BEFORE the port
			// actually binds — binding is deferred to ServerStartedEvent.
			System.out.println("[DelegatedLauncher] Installing HttpServerSubsystem...");
			LOGGER.info("Installing HttpServerSubsystem...");
			com.garward.wurmmodloader.httpserver.HttpServerSubsystem.install();

			// Eagerly initialize console GM router on ServerStartedEvent so the
			// "type #help" banner prints right after boot instead of lazily on
			// first keystroke.
			com.garward.wurmmodloader.core.console.ConsoleGMCommandRouter.installEagerInit();

			// Register each loaded mod with the HTTP subsystem so ModHttpServer.serve(mod, ...)
			// can resolve the mod's name. Replaces legacy ModListener.modInitialized() path.
			for (ModEntry<WurmServerMod> mod : wurmMods) {
				com.garward.wurmmodloader.httpserver.HttpServerSubsystem.register(mod);
			}

			// Register ServerPacks bridge for icon pack distribution
			com.garward.wurmmodloader.core.icon.IconPackServerPacksBridge bridge = new com.garward.wurmmodloader.core.icon.IconPackServerPacksBridge();
			for (ModEntry<WurmServerMod> mod : wurmMods) {
				bridge.modInitialized(mod);
			}

			// Add mods to ServerHook and initialize
			serverHook.addMods(wurmMods);
			serverHook.addVersionHandler(modLoader.getVersion(), modLoader.getGameVersion(), wurmMods);

			LOGGER.info("===================================");
			LOGGER.info("Inspecting active classloaders before server launch...");
			dumpAllClassLoaders();
			LOGGER.info("===================================");

			HookManager.getInstance().initCallbacks();

			// NOTE: Game content registries (ITEMS, CREATURES, ICONS) are NOT frozen yet.
			// They will be frozen in ProxyServerHook.onServerStarted() AFTER all
			// ItemTemplatesCreated listeners have fired during server startup.

			LOGGER.info("===================================");
			LOGGER.info("ModLoader initialization COMPLETE!");
			LOGGER.info("===================================");
			LOGGER.info("Loaded " + wurmMods.size() + " mods");
			LOGGER.info("All mod hooks are installed and ready");
			LOGGER.info("");
			LOGGER.info("Attempting to start Wurm server...");

			// Use HookManager's loader to run the server class
			// This allows the native launcher to provide JNI implementations
			boolean javafxAvailable = isJavaFxAvailable();
			if (!javafxAvailable) {
				LOGGER.info("JavaFX not detected on classpath; falling back to headless ServerLauncher.");
			}

			// Try GUI classes first (they handle server selection) when JavaFX exists, then
			// ServerLauncher (headless)
			String[] serverClasses = javafxAvailable
					? new String[] {
							"com.wurmonline.server.gui.WurmServerGuiMainDeferred",
							"com.wurmonline.server.gui.WurmServerGuiMain",
							"com.wurmonline.server.ServerLauncher"
					}
					: new String[] { "com.wurmonline.server.ServerLauncher" };

			for (String className : serverClasses) {
				try {
					LOGGER.info("Starting server via: " + className);
					// Use Javassist Loader's run method - this delegates com.wurmonline.* to parent
					// The parent classloader allows native JNI implementations to work
					HookManager.getInstance().getLoader().run(className, sanitizedArgs);
					return; // If we get here, server started successfully
				} catch (ClassNotFoundException e) {
					LOGGER.info("Class not found: " + className + ", trying next...");
					continue;
				} catch (NoClassDefFoundError e) {
					if ("javafx/application/Application".equals(e.getMessage())) {
						LOGGER.warning(
								"JavaFX not on classpath; skipping GUI launcher " + className
										+ " and trying next option (headless fallback will be used).");
						continue;
					}
					throw e;
				} catch (RuntimeException e) {
					if (e.getMessage() != null && e.getMessage().equals("Unimplemented")) {
						LOGGER.info("Class is stub: " + className + ", trying next...");
						continue;
					}
					throw e;
				}
			}

			LOGGER.warning("Could not start server - no valid entry point found");
			LOGGER.warning("Server classes appear to be stubs - native implementation may be missing");
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static String[] processInternalFlags(String[] args) {
		List<String> passthrough = new ArrayList<>();
		for (String arg : args) {
			if ("--force-bytecode-conflicts".equalsIgnoreCase(arg)) {
				PatchSettings.setForceConflictsEnabled(true);
				LOGGER.info("Force applying bytecode patches despite conflicts (--force-bytecode-conflicts)");
				continue; // do not pass the internal flag down to Wurm
			}
			if ("--continue-on-patch-error".equalsIgnoreCase(arg)) {
				PatchSettings.setContinueOnErrorEnabled(true);
				LOGGER.info("Continuing after bytecode patch failures (--continue-on-patch-error)");
				continue;
			}
			if (arg.startsWith("--test-events")) {
				String mode = "ON_SERVER_START"; // default
				if (arg.contains("=")) {
					mode = arg.substring(arg.indexOf('=') + 1);
				}
				System.setProperty("wurmmodloader.test.events", "true");
				System.setProperty("wurmmodloader.test.events.mode", mode);
				LOGGER.info("Event testing enabled: mode=" + mode + " (--test-events)");
				continue;
			}
			passthrough.add(arg);
		}
		return passthrough.toArray(new String[0]);
	}

	private static void configureFileLogging() {
		if (loggingConfigured) {
			return;
		}
		synchronized (DelegatedLauncher.class) {
			if (loggingConfigured) {
				return;
			}

			try {
				String rootOverride = System.getProperty("wurmmodloader.serverdir", ".");
				Path serverRoot = Paths.get(rootOverride).toAbsolutePath().normalize();
				Path logsDir = serverRoot.resolve("logs");
				Files.createDirectories(logsDir);
				String pattern = logsDir.resolve("wurmmodloader.%g.log").toString();
				FileHandler fileHandler = new FileHandler(pattern, 5 * 1024 * 1024, 5, true);
				fileHandler.setFormatter(new SimpleFormatter());
				fileHandler.setEncoding(StandardCharsets.UTF_8.name());
				fileHandler.setLevel(Level.ALL);

				Logger root = Logger.getLogger("");
				root.setLevel(Level.INFO);
				root.addHandler(fileHandler);

				Path consoleLog = logsDir.resolve("console.log");
				ConsoleTee.install(consoleLog);

				loggingConfigured = true;
				String message = "[ModLoader] File logging enabled at " + pattern;
				System.out.println(message);
				LOGGER.info(message);
			} catch (IOException | SecurityException e) {
				LOGGER.log(Level.WARNING, "Failed to configure log file handler", e);
			}
		}
	}

	private static void normalizeLegacyModProperties(Path modsPath) {
		if (modsPath == null || !Files.isDirectory(modsPath)) {
			return;
		}

		try (DirectoryStream<Path> properties = Files.newDirectoryStream(modsPath, "*.properties")) {
			for (Path propsFile : properties) {
				normalizeClassPathEntries(modsPath, propsFile);
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to normalize legacy mod properties", e);
		}
	}

	private static void normalizeClassPathEntries(Path modsPath, Path propsFile) {
		List<String> lines;
		try {
			lines = Files.readAllLines(propsFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "Skipping unreadable properties file " + propsFile, e);
			return;
		}

		boolean changed = false;
		List<String> rewritten = new ArrayList<>(lines.size());
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith("classpath=")) {
				int idx = line.indexOf('=');
				if (idx == -1) {
					rewritten.add(line);
					continue;
				}
				String value = line.substring(idx + 1);
				String normalized = rebuildClasspath(modsPath, value);
				if (!Objects.equals(value, normalized)) {
					changed = true;
					line = "classpath=" + normalized;
				}
			}
			rewritten.add(line);
		}

		if (!changed) {
			return;
		}

		try {
			String systemNewLine = System.lineSeparator();
			StringBuilder builder = new StringBuilder();
			Iterator<String> iterator = rewritten.iterator();
			while (iterator.hasNext()) {
				String line = iterator.next();
				if (line != null) {
					builder.append(line);
				}
				if (iterator.hasNext()) {
					builder.append(systemNewLine);
				}
			}
			builder.append(systemNewLine);
			Files.write(propsFile, builder.toString().getBytes(StandardCharsets.UTF_8));
			LOGGER.info(() -> "[ModLoader] Normalized classpath entries in " + propsFile.getFileName());
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to rewrite classpath for " + propsFile, e);
		}
	}

	private static String rebuildClasspath(Path modsPath, String value) {
		String[] entries = value.split(",");
		List<String> result = new ArrayList<>(entries.length);
		for (String entry : entries) {
			String trimmed = entry.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			Path resolved = modsPath.resolve(trimmed);
			if (Files.exists(resolved) && Files.isSymbolicLink(resolved)) {
				try {
					Files.deleteIfExists(resolved);
					LOGGER.info(() -> "[ModLoader] Removed legacy symlink " + resolved.getFileName());
				} catch (IOException e) {
					LOGGER.log(Level.FINE, "Failed to delete legacy symlink " + resolved, e);
				}
			}
			boolean resolvedExists = Files.exists(resolved) && Files.isRegularFile(resolved);
			if (!resolvedExists) {
				String candidate = tryResolveNested(modsPath, trimmed);
				if (candidate != null) {
					trimmed = candidate;
				}
			}
			result.add(trimmed.replace('\\', '/'));
		}
		return String.join(",", result);
	}

	private static String tryResolveNested(Path modsPath, String jarName) {
		String stem = jarName;
		int slash = stem.lastIndexOf('/');
		if (slash != -1) {
			stem = stem.substring(slash + 1);
		}
		int dot = stem.indexOf('.');
		if (dot != -1) {
			stem = stem.substring(0, dot);
		}

		Path direct = modsPath.resolve(stem).resolve(jarName);
		if (Files.exists(direct)) {
			return stem + "/" + jarName;
		}

		try (java.util.stream.Stream<Path> matches = Files.find(modsPath, 2,
				(path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().equals(jarName))) {
			Path found = matches.findFirst().orElse(null);
			if (found != null) {
				return modsPath.relativize(found).toString().replace('\\', '/');
			}
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "Failed searching for nested jar " + jarName, e);
		}
		return null;
	}

	private static boolean isJavaFxAvailable() {
		try {
			Class.forName("javafx.application.Application", false, DelegatedLauncher.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | NoClassDefFoundError e) {
			return false;
		}
	}

	private static void dumpAllClassLoaders() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		int depth = 0;
		LOGGER.info("──────────────────────────────────────────");
		LOGGER.info("[WML] Dumping all active classloaders and their URLs:");

		while (cl != null) {
			LOGGER.info("ClassLoader [" + depth + "]: " + cl.getClass().getName());
			if (cl instanceof java.net.URLClassLoader) {
				for (java.net.URL url : ((java.net.URLClassLoader) cl).getURLs()) {
					String indent = new String(new char[depth + 1]).replace('\0', ' ');
					LOGGER.info(indent + "→ " + url.getFile());
				}
			}
			cl = cl.getParent();
			depth++;
		}
		LOGGER.info("──────────────────────────────────────────");
	}
}

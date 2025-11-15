package com.garward.wurmmodloader.serverlauncher;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.ModLoader;
import com.garward.wurmmodloader.core.registry.SystemBootstrap;
import com.garward.wurmmodloader.core.bytecode.PatchSettings;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modloader.server.ServerHook;

// Direct imports for ModComm - no need for reflection!
import org.gotti.wurmunlimited.modcomm.ModComm;

public class DelegatedLauncher {

    public static void main(String[] args) {

        try {
            String[] sanitizedArgs = processInternalFlags(args);
            ServerHook serverHook = ServerHook.createServerHook();

			ModLoader modLoader = new ModLoader();
			Logger.getLogger(DelegatedLauncher.class.getName()).info("DEBUG: About to load mods from mods/ directory");
			List<? extends ModEntry<WurmServerMod>> wurmMods = modLoader.loadModsFromModDir(Paths.get("mods"));
			Logger.getLogger(DelegatedLauncher.class.getName()).info("DEBUG: loadModsFromModDir returned list of size: " + wurmMods.size());
			if (wurmMods.isEmpty()) {
				Logger.getLogger(DelegatedLauncher.class.getName()).warning("DEBUG: No mods were loaded! Check modloader-shared logs above for errors");
			} else {
				for (ModEntry<WurmServerMod> mod : wurmMods) {
					Logger.getLogger(DelegatedLauncher.class.getName()).info("DEBUG: Loaded mod: " + mod.getName() + " (" + mod.getWurmMod().getClass().getName() + ")");
				}
			}

			serverHook.registerPlayerHooks();

			serverHook.addMods(wurmMods);
			serverHook.addVersionHandler(modLoader.getVersion(), modLoader.getGameVersion(), wurmMods);

			Logger.getLogger(DelegatedLauncher.class.getName())
				.info("DEBUG: Bootstrapping runtime registries before server launch");
			SystemBootstrap.initializeAll();
			Logger.getLogger(DelegatedLauncher.class.getName())
				.info("DEBUG: Runtime registries ready");

			HookManager.getInstance().initCallbacks();

			Logger.getLogger(DelegatedLauncher.class.getName()).info("ModLoader initialization complete, starting Wurm server...");

			// Try GUI classes first (they handle server selection), then ServerLauncher (for headless)
			String[] classes = {
					"com.wurmonline.server.gui.WurmServerGuiMainDeferred",
					"com.wurmonline.server.gui.WurmServerGuiMain",
					"com.wurmonline.server.ServerLauncher"
			};

			for (String classname : classes) {

                try {
                    Logger.getLogger(DelegatedLauncher.class.getName()).info("Attempting to start: " + classname);
                    HookManager.getInstance().getLoader().run(classname, sanitizedArgs);
                    return;
				} catch (ClassNotFoundException e) {
					Logger.getLogger(DelegatedLauncher.class.getName()).info("Class not found: " + classname + ", trying next...");
					continue;
				} catch (RuntimeException e) {
					if (e.getMessage() != null && e.getMessage().equals("Unimplemented")) {
						Logger.getLogger(DelegatedLauncher.class.getName()).info("Class is stub: " + classname + ", trying next...");
						continue;
					}
					throw e;
				}
			}

			throw new ClassNotFoundException("No valid server launcher class found");
		} catch (Throwable e) {
			Logger.getLogger(DelegatedLauncher.class.getName()).log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			System.exit(-1);
		}
    }

    private static String[] processInternalFlags(String[] args) {
        List<String> passthrough = new ArrayList<>();
        for (String arg : args) {
            if ("--force-bytecode-conflicts".equalsIgnoreCase(arg)) {
                PatchSettings.setForceConflictsEnabled(true);
                Logger.getLogger(DelegatedLauncher.class.getName())
                    .info("Force applying bytecode patches despite conflicts (--force-bytecode-conflicts)");
                continue;
            }
            if ("--continue-on-patch-error".equalsIgnoreCase(arg)) {
                PatchSettings.setContinueOnErrorEnabled(true);
                Logger.getLogger(DelegatedLauncher.class.getName())
                    .info("Continuing after bytecode patch failures (--continue-on-patch-error)");
                continue;
            }
            passthrough.add(arg);
        }
        return passthrough.toArray(new String[0]);
    }
}

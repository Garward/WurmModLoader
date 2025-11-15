package org.gotti.wurmunlimited.serverlauncher;

import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ModLoader;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modloader.server.ServerHook;

public class DelegatedLauncher {
	
	public static void main(String[] args) {
		
		try {
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

			ServerHook serverHook = ServerHook.createServerHook();
			serverHook.addMods(wurmMods);
			serverHook.addVersionHandler(modLoader.getVersion(), modLoader.getGameVersion(), wurmMods);
			HookManager.getInstance().initCallbacks();
			
			String[] classes = {
					"com.wurmonline.server.gui.WurmServerGuiMainDeferred",
					"com.wurmonline.server.gui.WurmServerGuiMain"
			};
			
			for (String classname : classes) {
				
				try {
					HookManager.getInstance().getLoader().run(classname, args);
					return;
				} catch (ClassNotFoundException e) {
					continue;
				}
			}
			
			throw new ClassNotFoundException("com.wurmonline.server.gui.WurmServerGuiMain");
		} catch (Throwable e) {
			Logger.getLogger(DelegatedLauncher.class.getName()).log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

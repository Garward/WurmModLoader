package com.garward.wurmmodloader.serverlauncher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.Loader;

public class ServerLauncher {

	public static void main(String[] args) {
		try {

			initLogger();

			Loader loader = HookManager.getInstance().getLoader();
			loader.delegateLoadingOf("javafx.");
			loader.delegateLoadingOf("com.sun.");
			loader.delegateLoadingOf("org.controlsfx.");
			loader.delegateLoadingOf("impl.org.controlsfx");
			loader.delegateLoadingOf("com.mysql.");
			loader.delegateLoadingOf("org.sqlite.");
			loader.delegateLoadingOf("com.garward.wurmmodloader.modloader.internal.classhooks.");
			loader.delegateLoadingOf("javassist.");
			// NOTE: Do NOT delegate com.wurmonline.* - mods need access during configure()
			// NOTE: Do NOT delegate logging/flyway - causes classloader conflicts with mods

			Thread.currentThread().setContextClassLoader(loader);

			loader.run("com.garward.wurmmodloader.serverlauncher.DelegatedLauncher", args);
		} catch (Throwable e) {
			Logger.getLogger(ServerLauncher.class.getName()).log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static void initLogger() throws SecurityException {
		// Use externally configured loggers
		if (System.getProperty("java.util.logging.config.file") != null) {
			return;
		}
		if (System.getProperty("java.util.logging.config.class") != null) {
			return;
		}

		// Use a provided logging.properties file
		Path loggingPropertiesFile = Paths.get("logging.properties");
		if (Files.isRegularFile(loggingPropertiesFile)) {
			System.setProperty("java.util.logging.config.file", loggingPropertiesFile.toString());
			return;
		}
	}

}

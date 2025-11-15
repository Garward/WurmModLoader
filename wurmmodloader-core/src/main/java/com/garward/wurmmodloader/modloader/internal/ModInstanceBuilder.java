package com.garward.wurmmodloader.modloader.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;

/**
 * Helper which creates mod instance from a mod class.
 *
 * @param <T>
 */
public class ModInstanceBuilder<T> {

	private static Logger logger = Logger.getLogger(ModInstanceBuilder.class.getName());

	private Map<String, ClassLoader> classLoaders = new HashMap<>();
	private Class<? extends T> modClass;

	public ModInstanceBuilder(Class<? extends T> modClass) {
		this.modClass = modClass;
	}

	public T createModInstance(ModInfo entry) {

		Properties properties = entry.getProperties();
		String modInfo = entry.getName();
		final String className = properties.getProperty("classname");
		if (className == null) {
			throw new HookException("Missing property classname for mod " + modInfo);
		}

		try {
			Loader loader = HookManager.getInstance().getLoader();
			final String classpath = properties.getProperty("classpath");

			final ClassLoader classloader;
			if (classpath != null) {
				final Boolean sharedClassLoader = Boolean.valueOf(properties.getProperty("sharedClassLoader", "false"));
				ClassLoader[] dependencies = entry.getImport().stream().map(classLoaders::get).filter(Objects::nonNull).toArray(ClassLoader[]::new);
				classloader = createClassLoader(entry.getName(), classpath, loader, sharedClassLoader, dependencies);
				if (!sharedClassLoader) {
					classLoaders.put(entry.getName(), classloader);
				}
			} else {
				classloader = loader;
			}

			return classloader.loadClass(className).asSubclass(modClass).newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NotFoundException | MalformedURLException e) {
			throw new HookException(e);
		}
	}

	private List<Path> getClassLoaderEntries(String modname, String classpath) {
		List<Path> pathEntries = new ArrayList<>();

		String[] entries = classpath.split(",");
		for (String entry : entries) {
			// Determine search root: if entry has path separator, it's subfolder layout
			// Otherwise it's flat layout and should search in mods/ root
			Path modPath;
			String searchPattern;

			if (entry.contains("/") || entry.contains("\\")) {
				// Subfolder layout: "modname/mod.jar" or "modname\\mod.jar"
				// Search in mods/ root and use full entry as pattern
				modPath = Paths.get("mods");
				searchPattern = entry;
			} else {
				// Flat layout: "mod.jar" with no path separator
				// Check if subfolder exists first (legacy priority)
				Path subfolderPath = Paths.get("mods", modname);
				if (Files.exists(subfolderPath) && Files.isDirectory(subfolderPath)) {
					// Subfolder exists, search there
					modPath = subfolderPath;
					searchPattern = entry;
				} else {
					// No subfolder, search in mods/ root
					modPath = Paths.get("mods");
					searchPattern = entry;
				}
			}

			FileSystem fs = modPath.getFileSystem();
			final PathMatcher matcher = fs.getPathMatcher("glob:" + searchPattern);
			SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path p = modPath.relativize(file);
					if (matcher.matches(p)) {
						pathEntries.add(file);
					}
					return FileVisitResult.CONTINUE;
				}
			};

			try {
				Files.walkFileTree(modPath, visitor);
			} catch (IOException e) {
				throw new HookException(e);
			}
		}

		return pathEntries;
	}

	/**
	 * Create the classloader
	 * 
	 * @param modname
	 *            Mod name
	 * @param classpath
	 *            Classpath entries
	 * @param loader
	 *            Parent loader
	 * @param shared
	 *            Do we want a shared classloader?
	 * @param dependencies
	 *            Dependencies to import when NOT using a shared classloader
	 * @return Classloader
	 * @throws MalformedURLException
	 * @throws NotFoundException
	 */
	private ClassLoader createClassLoader(String modname, String classpath, Loader loader, Boolean shared, ClassLoader... dependencies) throws MalformedURLException, NotFoundException {
		List<Path> pathEntries = getClassLoaderEntries(modname, classpath);
		logger.log(Level.INFO, "Classpath: " + pathEntries.toString());

		if (shared) {
			final ClassPool classPool = HookManager.getInstance().getClassPool();
			for (Path path : pathEntries) {
				classPool.appendClassPath(path.toString());
			}
			return loader;
		} else {
			List<URL> urls = new ArrayList<>();
			for (Path path : pathEntries) {
				urls.add(path.toUri().toURL());
			}
			ClassLoader parent = loader;
			if (dependencies != null && dependencies.length > 0) {
				parent = new JoinClassLoader(parent, dependencies);
			}
			return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
		}
	}
}
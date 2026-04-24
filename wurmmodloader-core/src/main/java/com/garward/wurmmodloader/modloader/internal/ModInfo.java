package com.garward.wurmmodloader.modloader.internal;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.garward.wurmmodloader.modloader.internal.dependency.DependencyProvider;

/**
 * Information about a mod.
 */
public class ModInfo implements DependencyProvider {
	private Properties properties;
	private String name;
	private Path propsFile;
	private Path configFile;
	public ModInfo(Properties properties, String name) {
		this(properties, name, null, null);
	}
	public ModInfo(Properties properties, String name, Path propsFile, Path configFile) {
		this.properties = properties;
		this.name = name;
		this.propsFile = propsFile;
		this.configFile = configFile;
	}
	/** External mod.properties (or legacy modname.properties) on disk, if any. */
	public Path getPropsFile() {
		return propsFile;
	}
	/** External mod.config (or legacy modname.config) on disk, if any. */
	public Path getConfigFile() {
		return configFile;
	}
	/** Replace the live properties view (used during reload). */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	@Override
	public String getName() {
		return name;
	}
	public Properties getProperties() {
		return properties;
	}
	@Override
	public Collection<String> getRequires() {
		final List<String> set = parseList(getProperties().getProperty("depend.requires", ""));
		set.addAll(getImport());
		return set;
	}
	@Override
	public Collection<String> getConflicts() {
		return parseList(getProperties().getProperty("depend.conflicts", ""));
	}
	@Override
	public Collection<String> getBefore() {
		return parseList(getProperties().getProperty("depend.suggests", ""));
	}
	@Override
	public Collection<String> getAfter() {
		return parseList(getProperties().getProperty("depend.precedes", ""));
	}
	@Override
	public boolean isOnDemand() {
		return Boolean.parseBoolean(getProperties().getProperty("depend.ondemand", "false"));
	}
	public Collection<String> getImport() {
		return parseList(getProperties().getProperty("depend.import", ""));
	}
	
	private List<String> parseList(String list) {
		return Arrays.stream(list.split(","))
				.map(String::trim)
				.filter(string -> !string.isEmpty())
				.collect(Collectors.toList());
	}
}
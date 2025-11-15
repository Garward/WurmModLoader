package org.gotti.wurmunlimited.modloader.interfaces;

import java.util.Properties;

/**
 * Legacy interface for backward compatibility.
 * Mod entry.
 */
public interface ModEntry<T> {
	String getName();
	Properties getProperties();
	T getWurmMod();
	ClassLoader getModClassLoader();
}

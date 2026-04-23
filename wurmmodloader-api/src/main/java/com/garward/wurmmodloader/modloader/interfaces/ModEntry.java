package com.garward.wurmmodloader.modloader.interfaces;

import java.util.Properties;

/**
 * Canonical public mod-entry interface. Represents one activated mod seen by
 * the loader — exposes its name, loaded properties, the mod instance itself,
 * and the classloader it was loaded under.
 *
 * @param <T> concrete mod type (typically {@link WurmServerMod})
 */
public interface ModEntry<T> {

    String getName();

    Properties getProperties();

    T getWurmMod();

    ClassLoader getModClassLoader();
}

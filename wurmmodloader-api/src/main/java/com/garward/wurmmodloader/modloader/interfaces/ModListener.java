package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Canonical public mod-initialization listener. The loader invokes
 * {@link #modInitialized(ModEntry)} once per activated mod after each has
 * finished its own {@code init()} / {@code onServerStarted()} lifecycle —
 * letting listeners react to which mods are present.
 */
public interface ModListener {

    void modInitialized(ModEntry<?> entry);
}

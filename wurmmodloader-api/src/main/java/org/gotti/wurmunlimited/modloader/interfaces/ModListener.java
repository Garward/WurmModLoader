package org.gotti.wurmunlimited.modloader.interfaces;

/**
 * Legacy-compat ModListener. Extends the canonical
 * {@link com.garward.wurmmodloader.modloader.interfaces.ModListener} so that
 * legacy mods implementing this interface still fire from the loader's
 * single {@code instanceof ModListener} scan. The default bridge forwards
 * the canonical callback to the legacy signature below — the loader's Entry
 * impl implements both ModEntry variants, so the cast is always safe.
 *
 * <p>New mods should implement the canonical public {@code ModListener}
 * instead of this one.</p>
 */
public interface ModListener extends com.garward.wurmmodloader.modloader.interfaces.ModListener {

    void modInitialized(ModEntry<?> entry);

    @Override
    default void modInitialized(com.garward.wurmmodloader.modloader.interfaces.ModEntry<?> entry) {
        modInitialized((ModEntry<?>) entry);
    }
}

package org.gotti.wurmunlimited.modloader.interfaces;

/**
 * Legacy interface for backward compatibility with mods compiled against the original modloader.
 * <p>
 * This interface now extends the new Initable interface to unify both old and new mods
 * under a single type hierarchy. This allows ModLoader to accept both legacy and modern mods.
 * </p>
 * <p>
 * This interface MUST remain in the org.gotti.wurmunlimited package to maintain binary compatibility
 * with existing mods. Do NOT move or rename this interface.
 * </p>
 *
 * @deprecated Use {@link com.garward.wurmmodloader.modloader.interfaces.Initable} for new mods
 * @since 1.0.0
 */
@Deprecated
public interface Initable extends com.garward.wurmmodloader.modloader.interfaces.Initable {
	// All methods inherited from modern interface
}

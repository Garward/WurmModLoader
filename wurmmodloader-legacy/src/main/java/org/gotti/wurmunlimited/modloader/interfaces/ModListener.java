package org.gotti.wurmunlimited.modloader.interfaces;

/**
 * Legacy compatibility interface for ModListener.
 *
 * <p>{@link ModListener#modInitialized(ModEntry)} is called for each activated mod after they have been initialized.</p>
 *
 * <p>The interface can be used on a mod if it needs to know which mods are available.</p>
 *
 * @author ago
 */
public interface ModListener {

	/**
	 * Called once for each activated mod after it has been initialized.
	 *
	 * @param entry Mod entry
	 */
	void modInitialized(ModEntry<?> entry);

}

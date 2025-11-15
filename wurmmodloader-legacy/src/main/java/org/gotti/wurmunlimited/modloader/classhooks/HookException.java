package org.gotti.wurmunlimited.modloader.classhooks;

/**
 * Legacy HookException for backward compatibility.
 * Extends the internal HookException to maintain binary compatibility with mods
 * compiled against the original modloader.
 */
public class HookException extends com.garward.wurmmodloader.modloader.internal.classhooks.HookException {

	public HookException(Throwable e) {
		super(e);
	}

	public HookException(String message) {
		super(message);
	}

	private static final long serialVersionUID = -8955817806357327378L;

}

package org.gotti.wurmunlimited.modloader.classhooks;

import java.lang.reflect.InvocationHandler;

/**
 * Legacy InvocationHandlerFactory for backward compatibility.
 * Extends the internal interface to maintain binary compatibility with mods
 * compiled against the original modloader.
 */
public interface InvocationHandlerFactory extends com.garward.wurmmodloader.modloader.internal.classhooks.InvocationHandlerFactory {

	InvocationHandler createInvocationHandler();

}

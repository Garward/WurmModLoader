package com.garward.wurmmodloader.modloader.internal.classhooks;

import java.lang.reflect.InvocationHandler;

public interface InvocationHandlerFactory {
	
	InvocationHandler createInvocationHandler();

}

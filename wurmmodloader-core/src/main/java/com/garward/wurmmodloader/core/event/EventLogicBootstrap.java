package com.garward.wurmmodloader.core.event;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridges runtime-scanned event logic handlers into the EventBus.
 */
public final class EventLogicBootstrap {

    private static final Logger LOGGER = Logger.getLogger(EventLogicBootstrap.class.getName());

    private static final EventBus EVENT_BUS = EventBus.getInstance();

    private EventLogicBootstrap() {}

    public static void registerHandlerClass(Class<?> handlerClass) {
        if (!isEventLogicCandidate(handlerClass)) {
            LOGGER.log(Level.FINEST, "Skipping non-event-logic class: {0}",
                handlerClass != null ? handlerClass.getName() : "null");
            return;
        }

        try {
            Constructor<?> constructor = handlerClass.getDeclaredConstructor();
            if (!Modifier.isPublic(constructor.getModifiers()) ||
                !Modifier.isPublic(handlerClass.getModifiers())) {
                constructor.setAccessible(true);
            }
            Object instance = constructor.newInstance();
            EVENT_BUS.register(instance);
            LOGGER.log(Level.FINE, "Registered event logic handler: {0}", handlerClass.getName());
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING,
                "Failed to instantiate event logic handler: " + handlerClass.getName(), e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                "Unexpected error while registering event logic handler: " + handlerClass.getName(), e);
            throw e;
        }
    }

    static boolean isEventLogicCandidate(Class<?> candidate) {
        if (candidate == null) {
            return false;
        }
        int modifiers = candidate.getModifiers();
        if (candidate.isInterface() || Modifier.isAbstract(modifiers) || Modifier.isPrivate(modifiers)) {
            return false;
        }
        if (!hasAccessibleNoArgConstructor(candidate)) {
            return false;
        }
        return hasSubscriberMethods(candidate);
    }

    private static boolean hasAccessibleNoArgConstructor(Class<?> type) {
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasSubscriberMethods(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubscribeEvent.class)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
}

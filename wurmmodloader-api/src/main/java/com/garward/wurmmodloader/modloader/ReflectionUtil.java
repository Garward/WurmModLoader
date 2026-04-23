package com.garward.wurmmodloader.modloader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Public reflection helper for mods. Walks the class hierarchy to find
 * fields/methods regardless of declaring class, and provides accessibility-
 * toggling wrappers for private-member access.
 */
public final class ReflectionUtil {

    private ReflectionUtil() {}

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && !parent.equals(Object.class)) {
            fields.addAll(getAllFields(parent));
        }
        return fields;
    }

    private static List<Method> getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>(Arrays.asList(clazz.getDeclaredMethods()));
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && !parent.equals(Object.class)) {
            methods.addAll(getAllMethods(parent));
        }
        return methods;
    }

    public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        for (Field field : getAllFields(clazz)) {
            if (field.getName().equals(fieldName)) return field;
        }
        throw new NoSuchFieldException(fieldName);
    }

    public static Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        return getMethod(clazz, methodName, null);
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>[] signature) throws NoSuchMethodException {
        for (Method method : getAllMethods(clazz)) {
            if (method.getName().equals(methodName)
                    && (signature == null || Arrays.equals(signature, method.getParameterTypes()))) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    public static <T> void setPrivateField(Object object, Field field, T value)
            throws IllegalArgumentException, IllegalAccessException, ClassCastException {
        boolean isAccessible = field.isAccessible();
        field.setAccessible(true);
        Field modifiersField = null;
        int originalModifiers = field.getModifiers();
        boolean isFinal = java.lang.reflect.Modifier.isFinal(originalModifiers);
        try {
            if (isFinal) {
                try {
                    modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, originalModifiers & ~java.lang.reflect.Modifier.FINAL);
                } catch (NoSuchFieldException e) {
                    // Java 12+ — field doesn't exist, try the set anyway.
                }
            }
            field.set(object, value);
        } finally {
            if (isFinal && modifiersField != null) {
                try { modifiersField.setInt(field, originalModifiers); } catch (IllegalAccessException ignored) {}
            }
            field.setAccessible(isAccessible);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getPrivateField(Object object, Field field)
            throws IllegalArgumentException, IllegalAccessException, ClassCastException {
        boolean isAccessible = field.isAccessible();
        field.setAccessible(true);
        try {
            return (T) field.get(object);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T callPrivateMethod(Object target, Method method, Object... args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean isAccessible = method.isAccessible();
        method.setAccessible(true);
        try {
            return (T) method.invoke(target, args);
        } finally {
            method.setAccessible(isAccessible);
        }
    }

    public static <T> T callPrivateConstructor(Constructor<T> constructor, Object... args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
        boolean isAccessible = constructor.isAccessible();
        constructor.setAccessible(true);
        try {
            return constructor.newInstance(args);
        } finally {
            constructor.setAccessible(isAccessible);
        }
    }
}

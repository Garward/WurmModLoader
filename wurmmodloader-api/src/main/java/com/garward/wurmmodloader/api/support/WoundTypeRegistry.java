package com.garward.wurmmodloader.api.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Replacement for the legacy WoundAssist helper.
 */
public final class WoundTypeRegistry {

    private static final Map<String, Byte> NAME_TO_TYPE;
    private static final Map<Byte, String> TYPE_TO_NAME;

    static {
        Map<String, Byte> names = new HashMap<>();
        Map<Byte, String> ids = new HashMap<>();

        register(names, ids, "crush", (byte) 0);
        register(names, ids, "slash", (byte) 1);
        register(names, ids, "pierce", (byte) 2);
        register(names, ids, "bite", (byte) 3);
        register(names, ids, "burn", (byte) 4);
        register(names, ids, "poison", (byte) 5);
        register(names, ids, "infection", (byte) 6);
        register(names, ids, "water", (byte) 7);
        register(names, ids, "cold", (byte) 8);
        register(names, ids, "internal", (byte) 9);
        register(names, ids, "acid", (byte) 10);

        NAME_TO_TYPE = Collections.unmodifiableMap(names);
        TYPE_TO_NAME = Collections.unmodifiableMap(ids);
    }

    private WoundTypeRegistry() {
    }

    private static void register(Map<String, Byte> names, Map<Byte, String> ids, String name, byte type) {
        names.put(name, type);
        ids.put(type, name);
    }

    public static byte getWoundType(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Wound type token cannot be null");
        }
        Byte value = NAME_TO_TYPE.get(token.toLowerCase(Locale.ROOT));
        if (value != null) {
            return value;
        }
        return Byte.parseByte(token.trim());
    }

    public static String getWoundName(byte woundType) {
        return TYPE_TO_NAME.getOrDefault(woundType, "unknown");
    }

    public static String getWoundName(int woundType) {
        return getWoundName((byte) woundType);
    }
}

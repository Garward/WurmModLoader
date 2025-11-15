package com.garward.wurmmodloader.api.support;

import com.wurmonline.server.combat.ArmourTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility that mirrors the classic ArmourAssist helpers from SinduskLibrary.
 * Provides stable name/ID lookups for Wurm armour types without requiring the legacy mod.
 */
public final class ArmourTypeRegistry {

    private static final Map<String, Integer> NAME_TO_ID;
    private static final Map<Integer, String> ID_TO_NAME;
    private static final Map<Integer, ArmourTemplate.ArmourType> ID_TO_TYPE;
    private static final List<ArmourTemplate.ArmourType> ARMOUR_TYPES;

    static {
        Map<String, Integer> nameMap = new HashMap<>();
        Map<Integer, String> idMap = new HashMap<>();
        Map<Integer, ArmourTemplate.ArmourType> typeMap = new HashMap<>();
        List<ArmourTemplate.ArmourType> types = new ArrayList<>();

        register(nameMap, idMap, typeMap, types, 0, "none", ArmourTemplate.ARMOUR_TYPE_NONE);
        register(nameMap, idMap, typeMap, types, 1, "leather", ArmourTemplate.ARMOUR_TYPE_LEATHER);
        register(nameMap, idMap, typeMap, types, 2, "studded", ArmourTemplate.ARMOUR_TYPE_STUDDED);
        register(nameMap, idMap, typeMap, types, 3, "chain", ArmourTemplate.ARMOUR_TYPE_CHAIN);
        register(nameMap, idMap, typeMap, types, 4, "plate", ArmourTemplate.ARMOUR_TYPE_PLATE);
        register(nameMap, idMap, typeMap, types, 5, "ring", ArmourTemplate.ARMOUR_TYPE_RING);
        register(nameMap, idMap, typeMap, types, 6, "cloth", ArmourTemplate.ARMOUR_TYPE_CLOTH);
        register(nameMap, idMap, typeMap, types, 7, "scale", ArmourTemplate.ARMOUR_TYPE_SCALE);
        register(nameMap, idMap, typeMap, types, 8, "splint", ArmourTemplate.ARMOUR_TYPE_SPLINT);
        register(nameMap, idMap, typeMap, types, 9, "drake", ArmourTemplate.ARMOUR_TYPE_LEATHER_DRAGON);
        register(nameMap, idMap, typeMap, types, 10, "dragonscale", ArmourTemplate.ARMOUR_TYPE_SCALE_DRAGON);

        NAME_TO_ID = Collections.unmodifiableMap(nameMap);
        ID_TO_NAME = Collections.unmodifiableMap(idMap);
        ID_TO_TYPE = Collections.unmodifiableMap(typeMap);
        ARMOUR_TYPES = Collections.unmodifiableList(types);
    }

    private ArmourTypeRegistry() {
    }

    private static void register(Map<String, Integer> nameMap,
                                 Map<Integer, String> idMap,
                                 Map<Integer, ArmourTemplate.ArmourType> typeMap,
                                 List<ArmourTemplate.ArmourType> types,
                                 int id,
                                 String name,
                                 ArmourTemplate.ArmourType type) {
        nameMap.put(name, id);
        idMap.put(id, name);
        typeMap.put(id, type);
        if (type != null) {
            types.add(type);
        }
    }

    public static ArmourTemplate.ArmourType getArmourType(String name) {
        int id = getArmourTypeId(name);
        return getArmourType(id);
    }

    public static ArmourTemplate.ArmourType getArmourType(int armourType) {
        return ID_TO_TYPE.get(armourType);
    }

    public static int getArmourTypeId(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Armour type token cannot be null");
        }
        String key = token.toLowerCase(Locale.ROOT);
        Integer mapped = NAME_TO_ID.get(key);
        if (mapped != null) {
            return mapped;
        }
        return Integer.parseInt(token.trim());
    }

    public static String getArmourTypeName(int armourType) {
        return ID_TO_NAME.getOrDefault(armourType, "unknown");
    }

    public static List<ArmourTemplate.ArmourType> getRegisteredTypes() {
        return ARMOUR_TYPES;
    }
}

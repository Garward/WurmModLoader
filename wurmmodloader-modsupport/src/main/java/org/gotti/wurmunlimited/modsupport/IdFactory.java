package org.gotti.wurmunlimited.modsupport;

import java.util.List;
import java.util.Map.Entry;

/**
 * Legacy IdFactory wrapper for backward compatibility.
 * Delegates to the new implementation.
 */
public final class IdFactory {

    private IdFactory() {
        // Prevent instantiation
    }

    public static int getIdFor(String identifier, IdType idType) {
        return com.garward.wurmmodloader.modsupport.IdFactory.getIdFor(identifier, idType.toInternal());
    }

    public static int getExistingIdFor(String identifier, IdType idType) {
        return com.garward.wurmmodloader.modsupport.IdFactory.getExistingIdFor(identifier, idType.toInternal());
    }

    public static List<Entry<String, Integer>> getIdsFor(IdType idType) {
        return com.garward.wurmmodloader.modsupport.IdFactory.getIdsFor(idType.toInternal());
    }
}

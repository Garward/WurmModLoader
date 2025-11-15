package org.gotti.wurmunlimited.modsupport.items;

/**
 * Legacy ModItems wrapper for backward compatibility.
 * Delegates all static methods to the new implementation.
 */
public final class ModItems {

    private ModItems() {
        // Prevent instantiation
    }

    public static void init() {
        com.garward.wurmmodloader.modsupport.items.ModItems.init();
    }

    public static void addModelNameProvider(int templateId, ModelNameProvider modelNameProvider) {
        com.garward.wurmmodloader.modsupport.items.ModItems.addModelNameProvider(templateId,
            modelNameProvider != null ? modelNameProvider::getModelName : null);
    }
}

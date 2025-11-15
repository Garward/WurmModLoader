package org.gotti.wurmunlimited.modsupport.items;

/**
 * Legacy ItemTemplateBuilder wrapper for backward compatibility.
 * Delegates to the new implementation.
 */
public class ItemTemplateBuilder extends com.garward.wurmmodloader.modsupport.ItemTemplateBuilder {

    public ItemTemplateBuilder(String identifier) {
        super(identifier);
    }
}

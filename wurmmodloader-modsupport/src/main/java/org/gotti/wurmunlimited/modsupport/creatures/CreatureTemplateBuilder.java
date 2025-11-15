package org.gotti.wurmunlimited.modsupport.creatures;

/**
 * Legacy CreatureTemplateBuilder wrapper for backward compatibility.
 * Delegates to the new implementation.
 */
public class CreatureTemplateBuilder extends com.garward.wurmmodloader.modsupport.CreatureTemplateBuilder {

    public CreatureTemplateBuilder(int id) {
        super(id);
    }

    public CreatureTemplateBuilder(String identifier) {
        super(identifier);
    }
}

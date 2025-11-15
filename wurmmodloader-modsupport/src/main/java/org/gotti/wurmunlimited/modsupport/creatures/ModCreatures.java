package org.gotti.wurmunlimited.modsupport.creatures;

/**
 * Legacy ModCreatures wrapper for backward compatibility.
 * Delegates all static methods to the new implementation.
 */
public final class ModCreatures {

    private ModCreatures() {
        // Prevent instantiation
    }

    public static void init() {
        com.garward.wurmmodloader.modsupport.creatures.ModCreatures.init();
    }

    public static void addCreature(ModCreature creature) {
        com.garward.wurmmodloader.modsupport.creatures.ModCreatures.addCreature(
            (com.garward.wurmmodloader.modsupport.creatures.ModCreature) creature);
    }

    public static com.garward.wurmmodloader.modsupport.creatures.ModCreature getModCreature(int templateId) {
        return com.garward.wurmmodloader.modsupport.creatures.ModCreatures.getModCreature(templateId);
    }

    public static boolean hasTraits(int templateId) {
        return com.garward.wurmmodloader.modsupport.creatures.ModCreatures.hasTraits(templateId);
    }
}

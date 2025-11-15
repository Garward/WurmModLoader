package org.gotti.wurmunlimited.modsupport.vehicles;

/**
 * Legacy ModVehicleBehaviours wrapper for backward compatibility.
 * Delegates all static methods to the new implementation.
 */
public final class ModVehicleBehaviours {

    private ModVehicleBehaviours() {
        // Prevent instantiation
    }

    public static void init() {
        com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviours.init();
    }

    public static void addCreatureVehicle(int creatureTemplateId, ModVehicleBehaviour vehicle) {
        com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviours.addCreatureVehicle(
            creatureTemplateId,
            (com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviour) vehicle);
    }

    public static void addItemVehicle(int itemTemplateId, ModVehicleBehaviour vehicle) {
        com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviours.addItemVehicle(
            itemTemplateId,
            (com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviour) vehicle);
    }
}

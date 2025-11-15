package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bytecode patch for com.wurmonline.server.deities.DbDeity.
 *
 * <p>Adds hooks for deity database save/load operations, allowing mods to:
 * <ul>
 *   <li>Add custom columns to DEITIES table</li>
 *   <li>Save mod-specific data alongside vanilla deity data</li>
 *   <li>Load mod-specific data when deities are loaded from database</li>
 * </ul>
 *
 * <p><strong>Events Fired:</strong></p>
 * <ul>
 *   <li>{@code DeityDbSaveEvent} - When deity is being saved (save() method)</li>
 *   <li>{@code DeityDbLoadEvent} - When deity is being loaded (constructor)</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class DbDeityPatch {

    private static final Logger LOGGER = Logger.getLogger(DbDeityPatch.class.getName());

    /**
     * Apply the bytecode patch to DbDeity.
     *
     * <p>This must be called during Phase 3 (preInit) before the class is loaded.</p>
     *
     * @throws Exception if patching fails
     */
    public static void apply() throws Exception {
        LOGGER.info("[BytecodePatch] Applying DbDeityPatch...");

        CtClass ctClass = HookManager.getInstance().getClassPool().get("com.wurmonline.server.deities.DbDeity");

        // Patch constructor to fire load event
        patchConstructor(ctClass);

        // Patch save() method to fire save event
        patchSaveMethod(ctClass);

        LOGGER.info("[BytecodePatch] DbDeityPatch applied successfully");
    }

    /**
     * Patch constructor to fire DeityDbLoadEvent.
     *
     * <p>The constructor is called when deities are loaded from database.
     * We insert the load event after the super() call completes.</p>
     */
    private static void patchConstructor(CtClass ctClass) throws Exception {
        try {
            // DbDeity has one constructor:
            // (int num, String nam, byte align, byte aSex, byte pow, double aFaith, int aHolyItem, int _favor, float _attack, float _vitality, boolean create)
            CtConstructor[] constructors = ctClass.getDeclaredConstructors();

            if (constructors.length == 0) {
                LOGGER.warning("[BytecodePatch] No constructors found in DbDeity");
                return;
            }

            CtConstructor constructor = constructors[0];

            // Insert after super() call
            // $1 = num (deity ID), $2 = nam (deity name)
            String loadEventCode =
                "try {" +
                "    com.garward.wurmmodloader.modloader.server.ProxyServerHook.fireDeityDbLoadEvent(" +
                "        $1, $2, null);" +  // deityId, deityName, resultSet (null for now)
                "} catch (Exception e) {" +
                "    java.util.logging.Logger.getLogger(\"DbDeityPatch\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to fire DeityDbLoadEvent\", e);" +
                "}";

            constructor.insertAfter(loadEventCode);

            LOGGER.info("[BytecodePatch] Patched DbDeity constructor for load events");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[BytecodePatch] Failed to patch DbDeity constructor", e);
            throw e;
        }
    }

    /**
     * Patch save() method to fire DeityDbSaveEvent.
     *
     * <p>We insert code BEFORE the vanilla save logic to allow mods to register
     * custom columns. The custom data is saved via DeityDatabaseManager immediately.</p>
     */
    private static void patchSaveMethod(CtClass ctClass) throws Exception {
        try {
            CtMethod saveMethod = ctClass.getDeclaredMethod("save");

            // Insert BEFORE vanilla save - this fires the event and lets mods register columns
            // this.number = deity ID, this.name = deity name
            String beforeSaveCode =
                "try {" +
                "    com.garward.wurmmodloader.modloader.server.ProxyServerHook.fireDeityDbSaveEvent(" +
                "        this.number, this.name);" +  // Pass deity ID and name
                "} catch (Exception e) {" +
                "    java.util.logging.Logger.getLogger(\"DbDeityPatch\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to fire DeityDbSaveEvent\", e);" +
                "}";

            saveMethod.insertBefore(beforeSaveCode);

            LOGGER.info("[BytecodePatch] Patched DbDeity.save() for save events");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[BytecodePatch] Failed to patch DbDeity.save()", e);
            throw e;
        }
    }
}

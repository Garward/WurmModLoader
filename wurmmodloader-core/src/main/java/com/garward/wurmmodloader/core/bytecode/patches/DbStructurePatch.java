package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.CtClass;
import javassist.CtMethod;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bytecode patch for com.wurmonline.server.structures.DbStructure.
 *
 * <p>Adds hooks for structure database save/load operations, allowing mods to:
 * <ul>
 *   <li>Add custom columns to STRUCTURES table</li>
 *   <li>Save mod-specific data alongside vanilla structure data</li>
 *   <li>Load mod-specific data when structures are loaded from database</li>
 * </ul>
 *
 * <p><strong>Events Fired:</strong></p>
 * <ul>
 *   <li>{@code StructureDbSaveEvent} - When structure is being saved (save() method)</li>
 *   <li>{@code StructureDbLoadEvent} - When structure is being loaded (load() method)</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class DbStructurePatch {

    private static final Logger LOGGER = Logger.getLogger(DbStructurePatch.class.getName());

    /**
     * Apply the bytecode patch to DbStructure.
     *
     * <p>This must be called during Phase 3 (preInit) before the class is loaded.</p>
     *
     * @throws Exception if patching fails
     */
    public static void apply() throws Exception {
        LOGGER.info("[BytecodePatch] Applying DbStructurePatch...");

        CtClass ctClass = HookManager.getInstance().getClassPool().get("com.wurmonline.server.structures.DbStructure");

        // Patch load() method to fire load event
        patchLoadMethod(ctClass);

        // Patch save() method to fire save event
        patchSaveMethod(ctClass);

        LOGGER.info("[BytecodePatch] DbStructurePatch applied successfully");
    }

    /**
     * Patch load() method to fire StructureDbLoadEvent.
     *
     * <p>The load() method is called when structures are loaded from database.
     * We insert the load event after the result set has been populated with structure data.</p>
     *
     * <p><strong>Technical Note:</strong> We use a wrapper method approach instead of direct
     * ResultSet reference because Javassist has trouble accessing local variables in try blocks
     * when using line-based insertion.</p>
     */
    private static void patchLoadMethod(CtClass ctClass) throws Exception {
        try {
            CtMethod loadMethod = ctClass.getDeclaredMethod("load");

            // Instead of trying to reference 'rs' directly (which fails with "no such field"),
            // we create a helper method that gets called and re-queries the structure.
            // This is less efficient but avoids the Javassist local variable scope issue.

            // The event will be fired at the END of the load() method, after all data is loaded.
            // At this point, all structure data is in memory (in 'this'), so we don't need
            // the ResultSet anymore - we can fire the event and let mods query the database
            // themselves if they need additional data.

            // Build the injected code without inline comments (they break Javassist compilation)
            String loadEventCode =
                "try {" +
                "    if (this.getWurmId() > 0) {" +
                "        com.garward.wurmmodloader.core.database.StructureDbLoadHelper.fireLoadEvent(" +
                "            this.getWurmId(), this.getName());" +
                "    }" +
                "} catch (Exception e) {" +
                "    java.util.logging.Logger.getLogger(\"DbStructurePatch\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to fire StructureDbLoadEvent\", e);" +
                "}";

            // Insert at the END of the method, after all vanilla loading is complete
            // This ensures all structure data is populated before mods see the event
            loadMethod.insertAfter(loadEventCode);

            LOGGER.info("[BytecodePatch] Patched DbStructure.load() for load events");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[BytecodePatch] Failed to patch DbStructure.load()", e);
            throw e;
        }
    }

    /**
     * Patch save() method to fire StructureDbSaveEvent.
     *
     * <p>We insert code BEFORE the vanilla save logic to allow mods to register
     * custom columns. The custom data is saved via StructureDatabaseManager immediately.</p>
     */
    private static void patchSaveMethod(CtClass ctClass) throws Exception {
        try {
            CtMethod saveMethod = ctClass.getDeclaredMethod("save");

            // Insert BEFORE vanilla save - this fires the event and lets mods register columns
            // this.getWurmId() = structure ID, this.getName() = structure name
            String beforeSaveCode =
                "try {" +
                "    com.garward.wurmmodloader.modloader.server.ProxyServerHook.fireStructureDbSaveEvent(" +
                "        this.getWurmId(), this.getName());" +  // Pass structure ID and name
                "} catch (Exception e) {" +
                "    java.util.logging.Logger.getLogger(\"DbStructurePatch\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to fire StructureDbSaveEvent\", e);" +
                "}";

            saveMethod.insertBefore(beforeSaveCode);

            LOGGER.info("[BytecodePatch] Patched DbStructure.save() for save events");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[BytecodePatch] Failed to patch DbStructure.save()", e);
            throw e;
        }
    }
}

package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bytecode patch for com.wurmonline.server.creatures.DbCreatureStatus.
 *
 * <p>Adds hooks for creature database save/load operations, allowing mods to:
 * <ul>
 *   <li>Add custom columns to CREATURES table</li>
 *   <li>Save mod-specific data alongside vanilla creature data</li>
 *   <li>Load mod-specific data when creatures are loaded from database</li>
 * </ul>
 *
 * <p><strong>Events Fired:</strong></p>
 * <ul>
 *   <li>{@code CreatureDbSaveEvent} - When creature status is being saved</li>
 *   <li>{@code CreatureDbLoadEvent} - When creature status is being loaded</li>
 * </ul>
 *
 * <p><strong>Integration with CreatureStatusBatcher:</strong></p>
 * <p>This patch works alongside CreatureStatusBatcher for optimal performance.
 * Custom column saves are batched with vanilla saves (every 100ms).</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class DbCreatureStatusPatch {

    private static final Logger LOGGER = Logger.getLogger(DbCreatureStatusPatch.class.getName());

    /**
     * Apply the bytecode patch to DbCreatureStatus.
     *
     * <p>This must be called during Phase 3 (preInit) before the class is loaded.</p>
     *
     * @throws Exception if patching fails
     */
    public static void apply() throws Exception {
        LOGGER.info("[BytecodePatch] Applying DbCreatureStatusPatch...");

        CtClass ctClass = HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.DbCreatureStatus");

        // Patch constructor to fire load event
        patchConstructor(ctClass);

        // Patch save() method to fire save event
        patchSaveMethod(ctClass);

        LOGGER.info("[BytecodePatch] DbCreatureStatusPatch applied successfully");
    }

    /**
     * Patch constructor to fire CreatureDbLoadEvent.
     *
     * <p>The constructor is called when creatures are loaded from database.
     * We insert the load event after the super() call completes.</p>
     */
    private static void patchConstructor(CtClass ctClass) throws Exception {
        try {
            // DbCreatureStatus has one constructor: (Creature, float, float, float, int)
            CtConstructor[] constructors = ctClass.getDeclaredConstructors();

            if (constructors.length == 0) {
                LOGGER.warning("[BytecodePatch] No constructors found in DbCreatureStatus");
                return;
            }

            CtConstructor constructor = constructors[0];

            // Insert after super() call
            String loadEventCode =
                "try {" +
                "    com.garward.wurmmodloader.modloader.server.ProxyServerHook.fireCreatureDbLoadEvent(" +
                "        $1, null);" +  // creature, resultSet (null for now - will be added in future if needed)
                "} catch (Exception e) {" +
                "    java.util.logging.Logger.getLogger(\"DbCreatureStatusPatch\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to fire CreatureDbLoadEvent\", e);" +
                "}";

            constructor.insertAfter(loadEventCode);

            LOGGER.info("[BytecodePatch] Patched DbCreatureStatus constructor for load events");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[BytecodePatch] Failed to patch DbCreatureStatus constructor", e);
            throw e;
        }
    }

    /**
     * Patch save() method to fire CreatureDbSaveEvent.
     *
     * <p>We insert code BEFORE the vanilla save logic to allow mods to register
     * custom columns. Then we insert code AFTER to actually save the custom data.</p>
     */
    private static void patchSaveMethod(CtClass ctClass) throws Exception {
        try {
            CtMethod saveMethod = ctClass.getDeclaredMethod("save");

            // Insert BEFORE vanilla save - this fires the event and lets mods register columns
            String beforeSaveCode =
                "try {" +
                "    com.garward.wurmmodloader.modloader.server.ProxyServerHook.fireCreatureDbSaveEvent(" +
                "        this.statusHolder);" +  // Pass the creature
                "} catch (Exception e) {" +
                "    java.util.logging.Logger.getLogger(\"DbCreatureStatusPatch\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to fire CreatureDbSaveEvent\", e);" +
                "}";

            saveMethod.insertBefore(beforeSaveCode);

            // Chain the hidden SQLException as cause of the IOException vanilla throws,
            // and log the full context (SQL + params) at SEVERE so we actually see what's
            // failing. CreatureStatusBatcher's cause-unwrap already surfaces the IOException,
            // but vanilla strips cause chaining. This also logs directly because the
            // vanilla logger.log(WARNING, ..., sqex) never reaches our file handler.
            saveMethod.instrument(new ExprEditor() {
                @Override
                public void edit(NewExpr e) throws CannotCompileException {
                    if ("java.io.IOException".equals(e.getClassName())) {
                        e.replace(
                            "{ " +
                            "  java.util.logging.Logger.getLogger(\"DbCreatureStatusPatch\")" +
                            "    .log(java.util.logging.Level.SEVERE, " +
                            "         \"[DbCreatureStatus.save] SQL failure for wurmId=\" + id + " +
                            "         \" inventoryId=\" + this.inventoryId + \" bodyId=\" + this.bodyId + " +
                            "         \" statusExists=\" + this.isStatusExists(), sqex); " +
                            "  $_ = new java.io.IOException($1, sqex); " +
                            "}");
                    }
                }
            });

            LOGGER.info("[BytecodePatch] Patched DbCreatureStatus.save() for save events");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[BytecodePatch] Failed to patch DbCreatureStatus.save()", e);
            throw e;
        }
    }
}

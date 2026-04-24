package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.core.menu.CreationEntryMenuIds;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Routes all Create-menu leaf ids through {@link CreationEntryMenuIds} so mod-added recipes
 * with template ids {@code > 22767} don't silently overflow the vanilla {@code (short)(10000+id)}
 * encoding into the negative range — which the client interprets as a submenu header, nesting
 * every subsequent sibling underneath the overflowed entry.
 *
 * <p>Three call sites:</p>
 * <ul>
 *   <li>{@code ItemBehaviour.addCreationEntrys} — {@code new ActionEntry((short)(10000+id), ...)}</li>
 *   <li>{@code CreationWindowMethods.addCreationEntriesToPartialList} — {@code buffer.putShort((short)(10000+id))}</li>
 *   <li>{@code ItemBehaviour.action} — {@code getCreationEntry(stid, ttid, itemToCreate)} decode</li>
 * </ul>
 *
 * <p>Recovery trick: vanilla encodes via {@code (short)(10000 + templateId)}. To recover the
 * original int templateId from the pre-computed short we use {@code ($1 & 0xFFFF) - 10000},
 * which works for both vanilla positive-short ids and overflow negative-short ids.</p>
 */
public final class CreateMenuIdOverflowPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreateMenuIdOverflowPatch.class.getName());

    private static final String REGISTRY = CreationEntryMenuIds.class.getName();
    private static final String ACTION_ENTRY = "com.wurmonline.server.behaviours.ActionEntry";
    private static final String CREATION_MATRIX = "com.wurmonline.server.items.CreationMatrix";

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.ItemBehaviour"; }
    @Override public String methodName()      { return "<createMenuIdOverflow>"; }
    @Override public String methodDescriptor(){ return "(multiple)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool pool = hookManager.getClassPool();

            CtClass itemBehaviour = pool.get("com.wurmonline.server.behaviours.ItemBehaviour");
            if (itemBehaviour.isFrozen()) itemBehaviour.defrost();

            CtMethod addCreationEntrys = itemBehaviour.getDeclaredMethod("addCreationEntrys");
            addCreationEntrys.instrument(new ExprEditor() {
                @Override
                public void edit(NewExpr e) throws CannotCompileException {
                    if (!ACTION_ENTRY.equals(e.getClassName())) return;
                    try {
                        if (e.getConstructor().getParameterTypes().length != 4) return;
                    } catch (NotFoundException nfe) {
                        return;
                    }
                    e.replace(
                        "{ short __wmlMenuId = " + REGISTRY + ".encode((((int)$1) & 0xFFFF) - 10000);"
                      + "  $_ = new " + ACTION_ENTRY + "(__wmlMenuId, $2, $3, $4); }"
                    );
                }
            });

            CtClass creationWindowMethods = pool.get("com.wurmonline.server.items.CreationWindowMethods");
            if (creationWindowMethods.isFrozen()) creationWindowMethods.defrost();

            CtMethod addPartial = creationWindowMethods.getDeclaredMethod("addCreationEntriesToPartialList");
            addPartial.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (!"putShort".equals(m.getMethodName())) return;
                    if (!"java.nio.ByteBuffer".equals(m.getClassName())) return;
                    m.replace(
                        "{ short __wmlMenuId = $1;"
                      + "  int __wmlDecoded = (((int)__wmlMenuId) & 0xFFFF) - 10000;"
                      + "  if (__wmlDecoded > 0) { __wmlMenuId = " + REGISTRY + ".encode(__wmlDecoded); }"
                      + "  $_ = $proceed(__wmlMenuId); }"
                    );
                }
            });

            CtMethod actionMethod = itemBehaviour.getMethod(
                "action",
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;"
              + "Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;SF)Z"
            );
            actionMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (!"getCreationEntry".equals(m.getMethodName())) return;
                    if (!CREATION_MATRIX.equals(m.getClassName())) return;
                    try {
                        if (m.getMethod().getParameterTypes().length != 3) return;
                    } catch (NotFoundException nfe) {
                        return;
                    }
                    m.replace(
                        "{ int __wmlRealId = " + REGISTRY + ".decode($3 + 10000);"
                      + "  $_ = $proceed($1, $2, __wmlRealId); }"
                    );
                }
            });

            LOGGER.info("Registered CreateMenuIdOverflowPatch (encode x2 + decode x1)");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreateMenuIdOverflowPatch", e);
        }
    }

    @Override public int priority() { return 60; }

    @Override
    public Collection<String> conflictKeys() {
        return Arrays.asList(
            BytecodeConflictKeys.CREATE_MENU_ID_ENCODE,
            BytecodeConflictKeys.CREATE_MENU_ID_DECODE
        );
    }
}

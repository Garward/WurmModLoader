package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.CodeReplacer;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.internal.classhooks.LocalNameLookup;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Patches Item container methods to support dynamic container sizing.
 *
 * <p>This patch enables mods to override container dimensions and volume on a per-item basis
 * by redirecting template-based container queries to instance methods. This allows effects
 * like "Bag of Holding" spells that magically enlarge containers.
 *
 * <p><strong>What it does:</strong>
 * <ul>
 *   <li>Replaces {@code template.getContainerSizeX/Y/Z()} with {@code this.getContainerSizeX/Y/Z()}</li>
 *   <li>Replaces {@code target.getVolume()} with {@code target.getContainerVolume()} in volume checks</li>
 * </ul>
 *
 * <p><strong>Affected methods:</strong>
 * <ul>
 *   <li>{@code Item.insertItem(Item, boolean)}</li>
 *   <li>{@code Item.testInsertHollowItem(Item, boolean)}</li>
 *   <li>{@code Item.moveToItem(Creature, long, boolean)}</li>
 * </ul>
 *
 * <p><strong>Use case:</strong> Mods can hook {@code Item.getContainerSizeX/Y/Z()} and
 * {@code Item.getContainerVolume()} to return modified values based on spell effects,
 * enchantments, or other per-item state.
 *
 * @since 1.0.0
 */
public final class ItemContainerPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemContainerPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Item";
    }

    @Override
    public String methodName() {
        // This patch affects multiple methods
        return "insertItem,testInsertHollowItem,moveToItem";
    }

    @Override
    public String methodDescriptor() {
        return "multiple";
    }

    @Override
    public String displayName() {
        return "ItemContainerPatch (dynamic container sizing)";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctItem = classPool.get(targetClassName());

            // Check if class is already frozen
            if (ctItem.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping ItemContainerPatch - Item class already frozen");
                return;
            }

            // Editor to replace template.getContainerX() with this.getContainerX()
            ExprEditor exprEditor = new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.items.ItemTemplate".equals(m.getClassName())) {
                        if ("getContainerSizeX".equals(m.getMethodName())) {
                            m.replace("$_ = this.getContainerSizeX();");
                        } else if ("getContainerSizeY".equals(m.getMethodName())) {
                            m.replace("$_ = this.getContainerSizeY();");
                        } else if ("getContainerSizeZ".equals(m.getMethodName())) {
                            m.replace("$_ = this.getContainerSizeZ();");
                        }
                    }
                }
            };

            // Apply to insertItem(Item, boolean)
            String descriptor = Descriptor.ofMethod(
                CtClass.booleanType,
                new CtClass[] { classPool.get("com.wurmonline.server.items.Item"), CtClass.booleanType }
            );
            ctItem.getMethod("insertItem", descriptor).instrument(exprEditor);
            LOGGER.info("Patched Item.insertItem() for dynamic container sizing");

            // Apply to testInsertHollowItem(Item, boolean)
            ctItem.getMethod("testInsertHollowItem", descriptor).instrument(exprEditor);
            LOGGER.info("Patched Item.testInsertHollowItem() for dynamic container sizing");

            // Apply to moveToItem(Creature, long, boolean) - volume calculation fix
            descriptor = Descriptor.ofMethod(
                CtClass.booleanType,
                new CtClass[] {
                    classPool.get("com.wurmonline.server.creatures.Creature"),
                    CtClass.longType,
                    CtClass.booleanType
                }
            );
            CtMethod moveToItemMethod = ctItem.getMethod("moveToItem", descriptor);

            // Compact class file before bytecode manipulation
            ctItem.getClassFile().compact();

            MethodInfo methodInfo = moveToItemMethod.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            LocalVariableAttribute localVarAttr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);

            if (localVarAttr != null) {
                LocalNameLookup localNames = new LocalNameLookup(localVarAttr);

                // Replace: target.getVolume() - volAvail
                // With:    target.getContainerVolume() - volAvail
                Bytecode search = new Bytecode(methodInfo.getConstPool());
                search.addAload(localNames.get("target"));
                search.addInvokevirtual("com/wurmonline/server/items/Item", "getVolume", "()I");
                search.addIload(localNames.get("volAvail"));
                search.add(Bytecode.ISUB);

                Bytecode replace = new Bytecode(methodInfo.getConstPool());
                replace.addAload(localNames.get("target"));
                replace.addInvokevirtual("com/wurmonline/server/items/Item", "getContainerVolume", "()I");
                replace.addIload(localNames.get("volAvail"));
                replace.add(Bytecode.ISUB);

                new CodeReplacer(codeAttribute).replaceCode(search.get(), replace.get());
                LOGGER.info("Patched Item.moveToItem() volume calculation for dynamic sizing");
            } else {
                LOGGER.warning("Could not find local variable table for Item.moveToItem() - volume patch skipped");
            }

            LOGGER.info("ItemContainerPatch applied successfully");

        } catch (NotFoundException | CannotCompileException | BadBytecode e) {
            throw new IllegalStateException("Unable to install ItemContainerPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping ItemContainerPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] Dynamic container sizing may not work - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        // High priority - other mods may depend on this for container functionality
        return 80;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ITEM_CONTAINER);
    }
}

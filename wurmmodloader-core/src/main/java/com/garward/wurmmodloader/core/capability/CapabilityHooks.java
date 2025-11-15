package com.garward.wurmmodloader.core.capability;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import java.util.logging.Logger;

/**
 * Installs Javassist hooks to inject ICapabilityProvider into Wurm classes.
 *
 * <p>This class adds the ICapabilityProvider interface to Player, Creature, Item,
 * and Tile classes, allowing them to store and retrieve capabilities.</p>
 *
 * <p>Called during ProxyServerHook initialization (very early in server startup).</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0 (Phase 5.5)
 */
public class CapabilityHooks {

    private static final Logger logger = Logger.getLogger(CapabilityHooks.class.getName());

    /**
     * Install all capability hooks.
     * Must be called during framework initialization, before any game classes are loaded.
     */
    public static void installHooks() {
        try {
            logger.info("Installing capability hooks...");

            installPlayerHooks();
            installCreatureHooks();
            installItemHooks();
            // Note: Tile hooks are more complex, implement later if needed

            logger.info("Capability hooks installed successfully");

        } catch (Exception e) {
            logger.severe("Failed to install capability hooks: " + e.getMessage());
            throw new HookException(e);
        }
    }

    /**
     * Add ICapabilityProvider to Player class.
     */
    private static void installPlayerHooks() throws NotFoundException, CannotCompileException {
        ClassPool classPool = HookManager.getInstance().getClassPool();
        CtClass ctPlayer = classPool.get("com.wurmonline.server.players.Player");

        // Add interface
        CtClass ctICapabilityProvider = classPool.get("com.garward.wurmmodloader.api.capability.ICapabilityProvider");
        ctPlayer.addInterface(ctICapabilityProvider);

        // Add getCapability method
        CtMethod getCapability = CtMethod.make(
            "public Object getCapability(com.garward.wurmmodloader.api.capability.Capability capability) {" +
            "    return com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()" +
            "        .getPlayerCapability(this.getWurmId(), capability);" +
            "}",
            ctPlayer
        );
        ctPlayer.addMethod(getCapability);

        // Add hasCapability method
        CtMethod hasCapability = CtMethod.make(
            "public boolean hasCapability(com.garward.wurmmodloader.api.capability.Capability capability) {" +
            "    return com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()" +
            "        .hasPlayerCapability(this.getWurmId(), capability);" +
            "}",
            ctPlayer
        );
        ctPlayer.addMethod(hasCapability);

        logger.info("Installed ICapabilityProvider on Player class");
    }

    /**
     * Add ICapabilityProvider to Creature class.
     */
    private static void installCreatureHooks() throws NotFoundException, CannotCompileException {
        ClassPool classPool = HookManager.getInstance().getClassPool();
        CtClass ctCreature = classPool.get("com.wurmonline.server.creatures.Creature");

        // Add interface
        CtClass ctICapabilityProvider = classPool.get("com.garward.wurmmodloader.api.capability.ICapabilityProvider");
        ctCreature.addInterface(ctICapabilityProvider);

        // Add getCapability method
        CtMethod getCapability = CtMethod.make(
            "public Object getCapability(com.garward.wurmmodloader.api.capability.Capability capability) {" +
            "    return com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()" +
            "        .getCreatureCapability(this.getWurmId(), capability);" +
            "}",
            ctCreature
        );
        ctCreature.addMethod(getCapability);

        // Add hasCapability method
        CtMethod hasCapability = CtMethod.make(
            "public boolean hasCapability(com.garward.wurmmodloader.api.capability.Capability capability) {" +
            "    return com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()" +
            "        .hasCreatureCapability(this.getWurmId(), capability);" +
            "}",
            ctCreature
        );
        ctCreature.addMethod(hasCapability);

        logger.info("Installed ICapabilityProvider on Creature class");
    }

    /**
     * Add ICapabilityProvider to Item class.
     */
    private static void installItemHooks() throws NotFoundException, CannotCompileException {
        ClassPool classPool = HookManager.getInstance().getClassPool();
        CtClass ctItem = classPool.get("com.wurmonline.server.items.Item");

        // Add interface
        CtClass ctICapabilityProvider = classPool.get("com.garward.wurmmodloader.api.capability.ICapabilityProvider");
        ctItem.addInterface(ctICapabilityProvider);

        // Add getCapability method
        CtMethod getCapability = CtMethod.make(
            "public Object getCapability(com.garward.wurmmodloader.api.capability.Capability capability) {" +
            "    return com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()" +
            "        .getItemCapability(this.getWurmId(), capability);" +
            "}",
            ctItem
        );
        ctItem.addMethod(getCapability);

        // Add hasCapability method
        CtMethod hasCapability = CtMethod.make(
            "public boolean hasCapability(com.garward.wurmmodloader.api.capability.Capability capability) {" +
            "    return com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()" +
            "        .hasItemCapability(this.getWurmId(), capability);" +
            "}",
            ctItem
        );
        ctItem.addMethod(hasCapability);

        logger.info("Installed ICapabilityProvider on Item class");
    }

    /**
     * Add ICapabilityProvider to Tile class.
     *
     * Note: Tiles use integer IDs instead of long wurmIds, so this is slightly different.
     * Also, tiles are more complex - implementing later if needed.
     */
    private static void installTileHooks() throws NotFoundException, CannotCompileException {
        // TODO: Implement tile capability hooks if needed
        // Tiles are more complex because:
        // 1. They use int IDs instead of long wurmIds
        // 2. They don't have a simple getWurmId() method
        // 3. Need to calculate tile ID from x/y coordinates
        //
        // For now, focus on Player/Creature/Item which are more commonly used

        logger.info("Tile capability hooks not yet implemented (deferred)");
    }
}

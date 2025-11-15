package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Mirrors the legacy creature death instrumentation that fired CreatureDeathEvent/PlayerDeathEvent.
 */
public final class CreatureDeathPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureDeathPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "die";
    }

    @Override
    public String methodDescriptor() {
        return "(ZLjava/lang/String;)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctCreature.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureDeathPatch - Creature class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] CreatureDeathEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            CtMethod dieMethod = ctCreature.getMethod(methodName(), methodDescriptor());

            String proxyClass = ProxyServerHook.class.getName();

            // Capture attackers BEFORE die() clears them
            StringBuilder captureCode = new StringBuilder();
            captureCode.append("{\n");
            captureCode.append("    try {\n");
            captureCode.append("        java.lang.reflect.Field attackersField = com.wurmonline.server.creatures.Creature.class.getDeclaredField(\"attackers\");\n");
            captureCode.append("        attackersField.setAccessible(true);\n");
            captureCode.append("        java.util.Map attackersMap = (java.util.Map)attackersField.get(this);\n");
            captureCode.append("        if (attackersMap != null && !attackersMap.isEmpty()) {\n");
            captureCode.append("            ").append(proxyClass).append(".captureAttackers(this.getWurmId(), new java.util.HashMap(attackersMap));\n");
            captureCode.append("        }\n");
            captureCode.append("    } catch (Exception e) {\n");
            captureCode.append("        e.printStackTrace();\n");
            captureCode.append("    }\n");
            captureCode.append("}\n");

            dieMethod.insertBefore(captureCode.toString());

            // Fire event AFTER die() completes, using damage tracking to determine killer
            StringBuilder eventCode = new StringBuilder();
            eventCode.append("{\n");
            eventCode.append("    try {\n");
            eventCode.append("        com.wurmonline.server.creatures.Creature killer = null;\n");
            eventCode.append("        \n");
            eventCode.append("        // Get top damage dealer from damage tracking\n");
            eventCode.append("        java.util.Map damageMap = ").append(proxyClass).append(".getAndRemoveDamageTracking(this.getWurmId());\n");
            eventCode.append("        System.out.println(\"[CreatureDeathPatch] \" + this.getName() + \" died, damageMap: \" + (damageMap == null ? \"null\" : (damageMap.isEmpty() ? \"empty\" : damageMap.size() + \" entries\")));\n");
            eventCode.append("        if (damageMap != null && !damageMap.isEmpty()) {\n");
            eventCode.append("            // Find attacker who dealt most damage\n");
            eventCode.append("            long topDamagerId = -1L;\n");
            eventCode.append("            double maxDamage = 0.0;\n");
            eventCode.append("            java.util.Iterator it = damageMap.entrySet().iterator();\n");
            eventCode.append("            while (it.hasNext()) {\n");
            eventCode.append("                java.util.Map.Entry entry = (java.util.Map.Entry) it.next();\n");
            eventCode.append("                Long attackerId = (Long) entry.getKey();\n");
            eventCode.append("                Double damage = (Double) entry.getValue();\n");
            eventCode.append("                System.out.println(\"[CreatureDeathPatch]   Attacker \" + attackerId + \" dealt \" + damage + \" damage\");\n");
            eventCode.append("                if (damage.doubleValue() > maxDamage) {\n");
            eventCode.append("                    maxDamage = damage.doubleValue();\n");
            eventCode.append("                    topDamagerId = attackerId.longValue();\n");
            eventCode.append("                }\n");
            eventCode.append("            }\n");
            eventCode.append("            if (topDamagerId != -1L) {\n");
            eventCode.append("                // Use framework CreatureResolver (handles Wurm registries)\n");
            eventCode.append("                killer = com.garward.wurmmodloader.core.eventlogic.CreatureResolver.getCreatureOrNull(topDamagerId);\n");
            eventCode.append("                System.out.println(\"[CreatureDeathPatch]   CreatureResolver.getCreatureOrNull(\" + topDamagerId + \") -> \" + (killer != null ? killer.getName() + \" (player=\" + killer.isPlayer() + \")\" : \"null\"));\n");
            eventCode.append("            }\n");
            eventCode.append("        }\n");
            eventCode.append("        \n");
            eventCode.append("        // Fall back to getLatestAttackers() if damage tracking didn't find anyone\n");
            eventCode.append("        if (killer == null) {\n");
            eventCode.append("            long[] attackerIds = this.getLatestAttackers();\n");
            eventCode.append("            System.out.println(\"[CreatureDeathPatch]   getLatestAttackers(): \" + (attackerIds == null ? \"null\" : attackerIds.length + \" attackers\"));\n");
            eventCode.append("            if (attackerIds != null && attackerIds.length > 0) {\n");
            eventCode.append("                System.out.println(\"[CreatureDeathPatch]     Using attacker[0]: \" + attackerIds[0]);\n");
            eventCode.append("                killer = com.garward.wurmmodloader.core.eventlogic.CreatureResolver.getCreatureOrNull(attackerIds[0]);\n");
            eventCode.append("                System.out.println(\"[CreatureDeathPatch]     Resolved to: \" + (killer != null ? killer.getName() + \" (player=\" + killer.isPlayer() + \")\" : \"null\"));\n");
            eventCode.append("            }\n");
            eventCode.append("        }\n");
            eventCode.append("        \n");
            eventCode.append("        if (this.isPlayer()) {\n");
            eventCode.append("            ").append(proxyClass).append(".firePlayerDeathEvent((com.wurmonline.server.players.Player) this, killer);\n");
            eventCode.append("        } else {\n");
            eventCode.append("            ").append(proxyClass).append(".fireCreatureDeathEvent(this, killer);\n");
            eventCode.append("        }\n");
            eventCode.append("    } catch (Exception e) {\n");
            eventCode.append("        e.printStackTrace();\n");
            eventCode.append("    }\n");
            eventCode.append("}\n");

            dieMethod.insertAfter(eventCode.toString());
            LOGGER.info("Registered CreatureDeathEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureDeathPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureDeathPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] CreatureDeathEvent will not fire - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_DEATH);
    }
}

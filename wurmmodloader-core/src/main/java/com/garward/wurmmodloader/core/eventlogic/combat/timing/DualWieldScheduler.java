package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.combat.CombatDualWieldEvent;
import com.wurmonline.server.creatures.CombatHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Triggers off-hand swings when the {@link CombatDualWieldEvent} pipeline signals a potential dual-wield action.
 */
public class DualWieldScheduler {

    private static final Logger LOGGER = Logger.getLogger(DualWieldScheduler.class.getName());
    private static final Method ATTACK_METHOD = resolveAttackMethod();

    private final DualWieldRegistry registry;

    public DualWieldScheduler() {
        this(DualWieldRegistry.getInstance());
    }

    DualWieldScheduler(DualWieldRegistry registry) {
        this.registry = registry;
    }

    @SubscribeEvent
    public void onDualWield(CombatDualWieldEvent event) {
        Creature attacker = event.getAttacker();
        registry.findProfile(attacker).ifPresent(profile -> {
            Item ready = findReadyWeapon(attacker, profile, event.getTimeDelta());
            if (ready == null) {
                return;
            }
            CombatHandler handler = attacker.getCombatHandler();
            if (handler == null) {
                return;
            }
            if (invokeAttack(handler, event.getDefender(), ready)) {
                event.setCancelled(true);
            }
        });
    }

    private Item findReadyWeapon(Creature attacker, DualWieldProfile profile, float delta) {
        if (attacker == null) {
            return null;
        }
        Item[] secondary = attacker.getSecondaryWeapons();
        if (secondary.length == 0) {
            return null;
        }
        CombatHandler handler = attacker.getCombatHandler();
        if (handler == null) {
            return null;
        }
        for (Item weapon : secondary) {
            if (!profile.allowWeapon(weapon)) {
                continue;
            }
            float swingTime = handler.getSpeed(weapon);
            float timer = attacker.addToWeaponUsed(weapon, delta);
            if (timer > swingTime) {
                attacker.deductFromWeaponUsed(weapon, swingTime);
                return weapon;
            }
        }
        return null;
    }

    private boolean invokeAttack(CombatHandler handler, Creature defender, Item weapon) {
        if (ATTACK_METHOD == null) {
            return false;
        }
        try {
            ATTACK_METHOD.invoke(handler, defender, weapon, true);
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.log(Level.WARNING, "Failed to trigger dual wield attack", e);
            return false;
        }
    }

    private static Method resolveAttackMethod() {
        try {
            Method method = CombatHandler.class.getDeclaredMethod("attack",
                Creature.class, Item.class, boolean.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Unable to access CombatHandler#attack", e);
            return null;
        }
    }
}

package com.garward.wurmmodloader.modsupport.combat;

import com.wurmonline.server.combat.Weapon;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin mod-facing wrapper around {@link Weapon} registration.
 *
 * <p>Vanilla {@code new Weapon(...)} inserts into a static
 * {@code Weapon.weapons} map keyed by template id. Re-calling the constructor
 * with the same template id overwrites that entry, so registering a weapon
 * and re-registering it later are the same operation. This class exposes
 * that fact as an explicit {@link #register} / {@link #reregister} pair so
 * mods doing hot-reload don't have to know the implementation detail.
 *
 * <p>Typical usage inside a {@code Configurable + Reloadable} mod:
 * <pre>
 * &#64;Override
 * public void onServerStarted() {
 *     WeaponRegistry.register(templateId, damage, speed, crit, reach, weightGroup, parry, skillPenalty);
 * }
 *
 * &#64;Override
 * public void onReload() {
 *     WeaponRegistry.reregister(templateId, damage, speed, crit, reach, weightGroup, parry, skillPenalty);
 * }
 * </pre>
 *
 * <p><b>Caveats.</b> Re-registration is not strictly atomic — a combat swing
 * resolving on another thread during the exact moment of overwrite may read
 * a torn mix of old/new fields. In practice Wurm's combat loop is
 * coarse-grained enough that this is invisible; reserve re-registration for
 * GM-driven tuning, not per-tick edits.
 */
public final class WeaponRegistry {

    private static final Logger LOGGER = Logger.getLogger(WeaponRegistry.class.getName());

    private WeaponRegistry() {}

    /**
     * Register a weapon for the given template id. Same as {@code new Weapon(...)}.
     * Constructor args mirror vanilla — see {@link Weapon#Weapon(int, float, float, float, int, int, float, double)}.
     */
    public static void register(int templateId,
                                float damage,
                                float speed,
                                float critChance,
                                int reach,
                                int weightGroup,
                                float parryPercent,
                                double skillPenalty) {
        new Weapon(templateId, damage, speed, critChance, reach, weightGroup, parryPercent, skillPenalty);
    }

    /**
     * Overwrite the existing {@link Weapon} entry for the given template id with
     * new stats. Safe to call whether or not a prior entry exists; the vanilla
     * constructor unconditionally puts into the static map.
     *
     * <p>Log-tagged at FINE so {@code #reloadmods} output stays clean.
     */
    public static void reregister(int templateId,
                                  float damage,
                                  float speed,
                                  float critChance,
                                  int reach,
                                  int weightGroup,
                                  float parryPercent,
                                  double skillPenalty) {
        try {
            new Weapon(templateId, damage, speed, critChance, reach, weightGroup, parryPercent, skillPenalty);
            LOGGER.log(Level.FINE,
                    "WeaponRegistry: re-registered template {0} (dmg={1}, speed={2})",
                    new Object[] { templateId, damage, speed });
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                    "WeaponRegistry: re-register failed for template " + templateId, t);
        }
    }
}

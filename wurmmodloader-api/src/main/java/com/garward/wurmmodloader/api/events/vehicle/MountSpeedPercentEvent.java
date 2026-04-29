package com.garward.wurmmodloader.api.events.vehicle;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired at the entry of {@link Creature#getMountSpeedPercent(boolean)} —
 * the per-creature speed fraction that vanilla {@code
 * Vehicle.calculateNewMountSpeed} multiplies into the final byte mount
 * speed. This is the layer mounts use to fold in horseshoes, saddle, hunger,
 * damage, traits, oakshell, and rider movement modifier.
 *
 * <p>Compare with {@link VehicleSpeedCalculationEvent}, which fires
 * <em>after</em> the byte clamp on {@code Vehicle.calculateNewMountSpeed}.
 * Use this event to <strong>replace</strong> the per-creature percent
 * formula; use {@code VehicleSpeedCalculationEvent} to nudge the final
 * already-clamped vehicle speed.</p>
 *
 * <p>{@link #getPercent()} starts at 0 and {@link #isOverridden()} starts
 * false. If at least one listener calls {@link #setPercent(float)}, the
 * patched return path uses that value instead of vanilla's. If nothing
 * subscribes (or everyone leaves it untouched), vanilla logic runs as
 * normal.</p>
 *
 * <p>{@link #isMounting()} matches the vanilla parameter — {@code true}
 * during the initial mount calculation, {@code false} during the periodic
 * re-poll inside {@code Creature.pollMount}.</p>
 */
public class MountSpeedPercentEvent extends Event {

    private final Creature creature;
    private final boolean mounting;
    private float percent;
    private boolean overridden;

    public MountSpeedPercentEvent(Creature creature, boolean mounting) {
        this.creature = creature;
        this.mounting = mounting;
        this.percent = 0f;
        this.overridden = false;
    }

    public Creature getCreature() {
        return creature;
    }

    public boolean isMounting() {
        return mounting;
    }

    /**
     * The override percent. Only meaningful when {@link #isOverridden()} is
     * true.
     */
    public float getPercent() {
        return percent;
    }

    /**
     * Override the per-creature mount speed percent. The patched method
     * will return this value verbatim; vanilla's saddle/horseshoe/trait
     * math is skipped.
     */
    public void setPercent(float percent) {
        this.percent = percent;
        this.overridden = true;
    }

    public boolean isOverridden() {
        return overridden;
    }
}

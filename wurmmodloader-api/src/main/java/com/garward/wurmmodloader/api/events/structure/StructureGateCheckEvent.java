package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired immediately before vanilla gates a structural-planning check on skill
 * or material availability. Listeners set {@link #setBypass(boolean)} to allow
 * the performer past the check; if no listener bypasses, vanilla's gate runs
 * normally.
 *
 * <p>This is the generic "can this performer skip requirements" hook — used
 * today by {@code gmtools} to make GM (power ≥ 2) planning frictionless, and
 * available to any mod implementing role-based permissions, sandbox modes,
 * creative gear, etc. One event shape covers every gate: new patch points can
 * be added without a new event type — just pass the right {@link Subject} and
 * {@link Phase}.</p>
 *
 * <p>Semantics are bypass-only: listeners can <em>allow</em> the action past a
 * gate, never veto one. Use {@code StructurePlanningCheckEvent} for denial.</p>
 */
public class StructureGateCheckEvent extends Event {

    /** What's being planned. Walls/floors are distinct because vanilla picker filters them independently. */
    public enum Subject { WALL, FLOOR, FENCE, BRIDGE }

    /** Which gate tier is being checked. Subject × Phase disambiguates the call-site when multiple patches share a listener. */
    public enum Phase {
        /** Basic elevation/level knowledge check. */
        LEVEL,
        /** Per-type skill knowledge threshold (wall shape, floor type, fence kind). */
        SKILL,
        /** Inventory material check (planks, nails, stone…). */
        MATERIAL,
        /** Geometric/placement legality (slope, road, overlap). */
        PLACEMENT
    }

    private final Creature performer;
    private final Subject subject;
    private final Phase phase;
    private final int heightOffset;
    private boolean bypass = false;

    public StructureGateCheckEvent(Creature performer, Subject subject, Phase phase, int heightOffset) {
        this.performer = performer;
        this.subject = subject;
        this.phase = phase;
        this.heightOffset = heightOffset;
    }

    public Creature getPerformer() { return performer; }
    public Subject getSubject()    { return subject; }
    public Phase getPhase()        { return phase; }
    public int getHeightOffset()   { return heightOffset; }

    public boolean isBypassed()            { return bypass; }
    public void setBypass(boolean bypass)  { this.bypass = bypass; }
}

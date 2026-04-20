package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired when a creature stops being someone's pet.
 *
 * <p>Covers three cases:
 * <ul>
 *   <li>{@link Reason#DIED} — the pet died (fired from {@code Creature.die})</li>
 *   <li>{@link Reason#UNTAMED} — loyalty decayed to zero or vanilla untame fired</li>
 *   <li>{@link Reason#MANUAL} — explicit release by the owner</li>
 * </ul>
 *
 * <p>{@code formerOwnerId} is preserved even when the owner is offline so
 * mods can update persistent rosters (multi-pet capability lists) without
 * requiring the owner to be loaded.</p>
 *
 * @since 1.0.0
 */
public class PetReleasedEvent extends Event {

    public enum Reason { DIED, UNTAMED, MANUAL }

    private final Creature pet;
    private final long formerOwnerId;
    private final Reason reason;

    public PetReleasedEvent(Creature pet, long formerOwnerId, Reason reason) {
        this.pet = pet;
        this.formerOwnerId = formerOwnerId;
        this.reason = reason;
    }

    public Creature getPet() { return pet; }
    public long getFormerOwnerId() { return formerOwnerId; }
    public Reason getReason() { return reason; }
}

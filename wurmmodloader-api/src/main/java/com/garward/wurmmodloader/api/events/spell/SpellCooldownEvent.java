package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired from {@code Spell.touchCooldown} before the cooldown expiry is recorded.
 * Listeners may override the cooldown duration (in milliseconds) or skip the cooldown
 * entirely by setting it to {@code 0} or negative.
 *
 * <p>Vanilla cooldowns are the per-spell lockout that prevents re-casting. Use this to
 * build per-spell cooldown reductions, shared cooldowns across spell groups, or
 * removing cooldowns for GM/test flows.</p>
 */
public class SpellCooldownEvent extends Event {

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final String casterName;
    private final long originalCooldownMs;
    private long modifiedCooldownMs;

    public SpellCooldownEvent(int spellId, String spellName,
                              long casterId, String casterName,
                              long originalCooldownMs) {
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.casterName = casterName;
        this.originalCooldownMs = originalCooldownMs;
        this.modifiedCooldownMs = originalCooldownMs;
    }

    public int getSpellId() {
        return spellId;
    }

    public String getSpellName() {
        return spellName;
    }

    public long getCasterId() {
        return casterId;
    }

    public String getCasterName() {
        return casterName;
    }

    /** Cooldown duration in ms as declared on the spell. */
    public long getOriginalCooldownMs() {
        return originalCooldownMs;
    }

    public long getModifiedCooldownMs() {
        return modifiedCooldownMs;
    }

    /** Override the cooldown. Set to 0 or negative to skip the cooldown entirely. */
    public void setModifiedCooldownMs(long ms) {
        this.modifiedCooldownMs = ms;
    }

    /** Multiply the current cooldown by a factor (e.g. 0.5 to halve). */
    public void multiply(double factor) {
        this.modifiedCooldownMs = (long) (this.modifiedCooldownMs * factor);
    }

    /** Cancel the cooldown entirely. */
    public void cancel() {
        this.modifiedCooldownMs = 0L;
    }
}

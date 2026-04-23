package com.garward.wurmmodloader.api.events.village;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.questions.VillageFoundationQuestion;

/**
 * Fired at the entry of {@link VillageFoundationQuestion}'s foundation/expansion
 * commit ({@code parseVillageFoundationQuestion5}). Lets village mods veto
 * expansion (upkeep/guard/size/placement) before vanilla processes the answer.
 *
 * <p>The {@link #isExpanding()} flag distinguishes a fresh foundation from an
 * expansion of an existing village.</p>
 */
public class VillageExpansionCheckEvent extends CancellableEvent {

    private final VillageFoundationQuestion question;
    private final boolean expanding;

    public VillageExpansionCheckEvent(VillageFoundationQuestion question, boolean expanding) {
        this.question = question;
        this.expanding = expanding;
    }

    public VillageFoundationQuestion getQuestion() {
        return question;
    }

    /** True for an expansion of an existing village; false for fresh foundation. */
    public boolean isExpanding() {
        return expanding;
    }
}

package com.garward.wurmmodloader.api.events.village;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.villages.GuardPlan;

/**
 * Fired at the entry of {@code GuardPlan.pollUpkeep()}. Lets upkeep/guard mods
 * replace or skip the vanilla upkeep drain.
 *
 * <p>Cancellation returns {@code false} from {@code pollUpkeep} (no disband,
 * no drain). Listeners replacing the drain should mutate the plan's balance
 * themselves via reflection on {@link GuardPlan}.</p>
 */
public class GuardPlanPollEvent extends CancellableEvent {

    private final GuardPlan plan;

    public GuardPlanPollEvent(GuardPlan plan) {
        this.plan = plan;
    }

    public GuardPlan getPlan() {
        return plan;
    }
}

package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.combat.WeaponUseEvent;

/**
 * Normalises weapon timers after {@link WeaponUseEvent} based on registered policies.
 */
public class WeaponTimerReset {

    private final WeaponTimerRegistry registry;

    public WeaponTimerReset() {
        this(WeaponTimerRegistry.getInstance());
    }

    WeaponTimerReset(WeaponTimerRegistry registry) {
        this.registry = registry;
    }

    @SubscribeEvent
    public void onWeaponUse(WeaponUseEvent event) {
        registry.findPolicy(event.getWeapon()).ifPresent(policy ->
            event.setNewValue(policy.getResetValue()));
    }
}

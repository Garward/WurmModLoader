package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.combat.CombatSwingSpeedEvent;
import com.wurmonline.server.items.Item;

/**
 * Applies {@link SwingSpeedProfile} rules to {@link CombatSwingSpeedEvent}.
 */
public class SwingSpeedAdjuster {

    private final SwingSpeedRegistry registry;

    public SwingSpeedAdjuster() {
        this(SwingSpeedRegistry.getInstance());
    }

    SwingSpeedAdjuster(SwingSpeedRegistry registry) {
        this.registry = registry;
    }

    @SubscribeEvent
    public void onSwingSpeed(CombatSwingSpeedEvent event) {
        Item weapon = event.getWeapon();
        registry.findProfile(weapon).ifPresent(profile -> {
            float swing = event.getSwingSpeed();
            if (profile.isRarityEnabled() && weapon != null && weapon.getRarity() > 0) {
                swing -= weapon.getRarity() * profile.getRarityReductionPerTier();
            }
            swing = Math.max(profile.getMinimumSwingSeconds(), swing);
            event.setSwingSpeed(swing);
        });
    }
}

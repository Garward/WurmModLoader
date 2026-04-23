package com.garward.wurmmodloader.api.events.eventlogic.area;

/** Radius + skill-level tier for an area action. */
public final class AreaActionDef {
    public final int radius;
    public final float level;

    public AreaActionDef(int radius, float level) {
        this.radius = radius;
        this.level = level;
    }
}

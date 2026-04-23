package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired from {@code Actions.isActionAllowedOnVehicle(short)} to let mods extend
 * the vanilla whitelist of actions permitted while mounted/seated. Listeners
 * receive the vanilla decision via {@link #isAllowed()} and may override it
 * with {@link #setAllowed(boolean)}.
 *
 * <p>Primarily used by mods like BetterDig / BetterFarm that want
 * digging/farming to be allowed from carts.</p>
 */
public class ActionAllowedOnVehicleEvent extends Event {

    private final short action;
    private boolean allowed;

    public ActionAllowedOnVehicleEvent(short action, boolean allowed) {
        this.action = action;
        this.allowed = allowed;
    }

    public short getAction()      { return action; }
    public boolean isAllowed()    { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }
}

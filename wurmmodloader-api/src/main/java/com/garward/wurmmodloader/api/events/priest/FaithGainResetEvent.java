package com.garward.wurmmodloader.api.events.priest;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

/**
 * Fired at the entry of {@code Players.resetFaithGain()} (the daily tick that
 * clears per-player faith-gain counters). Lets priest/faith mods replace or
 * skip the reset.
 *
 * <p>Cancellation skips the call to {@code PlayerInfoFactory.resetFaithGain()}.
 * Listeners replacing the behaviour should perform their own reset inside the
 * handler.</p>
 */
public class FaithGainResetEvent extends CancellableEvent {

    public FaithGainResetEvent() {}
}

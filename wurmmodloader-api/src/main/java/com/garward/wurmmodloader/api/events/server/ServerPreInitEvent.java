package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fires immediately before {@code Villages.loadVillages()} — the earliest
 * framework-observable point where the zones database connection is open
 * but no village / town / zone state has been pulled into memory yet.
 *
 * <p>This is the hook for subsystems that need to <b>seed or repair</b>
 * world DB rows before the server reads them. Canonical use case: the
 * custom-map world-seeding bootstrap, which checks whether {@code VILLAGES}
 * contains sane rows for the currently-loaded map size and — if not —
 * writes starter-town rows, altar items, and fallback spawn coords into
 * the database before the server loads them.
 *
 * <p>Anything that mutates {@code wurmzones.db} or {@code wurmitems.db}
 * after {@link ServerStartedEvent} fires will require a server restart to
 * take effect; doing the work at {@code ServerPreInitEvent} lets it
 * activate on the same boot.
 *
 * <p><b>Not cancellable.</b>
 *
 * @since 1.1.0
 */
public class ServerPreInitEvent extends Event {

    public ServerPreInitEvent() {
        super(false);
    }
}

package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fires once per boot at {@code ServerStartedEvent} time, carrying the
 * framework's world-seeder decision.
 *
 * <p>Mods should subscribe to this instead of {@link ServerPreInitEvent} for
 * any logic that needs the picked tile, because PreInit fires before the
 * vanilla world finishes loading — mods that want to spawn their own items
 * or NPCs at the seed location need the full world to be up.
 *
 * <p>Always fires — even when the seeder skipped or failed. Check
 * {@link WorldSeedResult#getOutcome()} before acting on coordinates.
 */
public class WorldSeededEvent extends Event {

    private final WorldSeedResult result;

    public WorldSeededEvent(WorldSeedResult result) {
        super(false);
        this.result = result;
    }

    public WorldSeedResult getResult() {
        return result;
    }
}

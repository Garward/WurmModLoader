package com.garward.wurmmodloader.api.worldseed;

import com.garward.wurmmodloader.api.events.server.WorldSeedResult;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Static access to the current boot's world-seed outcome.
 *
 * <p>Preferred mod-side access pattern:
 * <pre>{@code
 * @SubscribeEvent
 * public void onWorldSeeded(WorldSeededEvent e) {
 *     WorldSeedResult r = e.getResult();
 *     if (r.isSeeded()) spawnMyMerchantNear(r.getCenterTileX(), r.getCenterTileY());
 * }
 * }</pre>
 *
 * <p>For mods that need the value outside an event handler (e.g. on a timer
 * or deferred task), use {@link #getResult()}.
 */
public final class WorldSeedAPI {

    private static final AtomicReference<WorldSeedResult> CURRENT = new AtomicReference<>();

    private WorldSeedAPI() {}

    /** Called by core at PreInit time with the decided result. Mods: do not call. */
    public static void publish(WorldSeedResult r) {
        CURRENT.set(r);
    }

    /**
     * @return the boot's seed result, or null if PreInit hasn't run yet
     *         (i.e. you're asking before the framework reached that stage).
     */
    public static WorldSeedResult getResult() {
        return CURRENT.get();
    }
}

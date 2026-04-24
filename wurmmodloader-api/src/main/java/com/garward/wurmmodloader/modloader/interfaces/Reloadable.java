package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Optional mod interface. Mods implementing this receive an {@link #onReload()}
 * callback when the framework's {@code #reloadmods} console/GM command is
 * invoked. Intended for mods that load configuration outside the standard
 * {@code .properties}/{@code .config} pipeline (e.g. YAML, JSON, custom
 * formats), or that need to rebuild caches, reset timers, or re-index state
 * after a live config change.
 *
 * <p><strong>Invocation order on reload:</strong></p>
 * <ol>
 *   <li>The framework re-reads {@code mod.properties} and {@code mod.config}
 *       from disk.</li>
 *   <li>{@link Configurable#configure(java.util.Properties)} is invoked with
 *       the fresh merged properties (if the mod is {@code Configurable}).</li>
 *   <li>{@link #onReload()} is invoked for mods implementing this interface.</li>
 * </ol>
 *
 * <p><strong>Idempotency caveat:</strong> Reload does <em>not</em> tear down
 * the mod or its event subscriptions. If {@code configure()} registers
 * listeners, acquires resources, or mutates global state, the implementation
 * is responsible for making those operations idempotent — e.g. clearing
 * internal collections before re-populating them.</p>
 *
 * @since 0.8.1
 */
public interface Reloadable {

    /**
     * Called after the framework has re-applied fresh properties to this mod.
     * Implementations should re-parse any ancillary config files (YAML, JSON,
     * route tables, etc.) and refresh in-memory state.
     *
     * <p>Exceptions thrown from this method are caught and logged by the
     * framework; they will not abort the wider reload batch.</p>
     */
    void onReload();
}

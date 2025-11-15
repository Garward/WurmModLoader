package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Interface for mods that need to perform initialization during server startup.
 *
 * <p>This interface extends the internal Initable interface to ensure compatibility
 * with the ModLoader's lifecycle management.</p>
 *
 * <p><strong>Note:</strong> This interface is redundant for most modern mods.
 * {@link WurmServerMod} already provides an {@code init()} method,
 * so mods can simply implement WurmServerMod and override {@code init()}
 * without implementing this interface separately.</p>
 *
 * <p>This interface exists for consistency with the modloader architecture
 * and for mods that may want to explicitly declare their use of the init phase.</p>
 *
 * <p><strong>Lifecycle Order:</strong></p>
 * <ol>
 *   <li>{@link Configurable#configure(java.util.Properties)} - Load configuration (if implemented)</li>
 *   <li>{@link PreInitable#preInit()} - Register bytecode hooks (if implemented)</li>
 *   <li>{@link #init()} - Register content and actions</li>
 * </ol>
 *
 * <p><strong>Example (Explicit):</strong></p>
 * <pre>{@code
 * public class MyMod implements WurmServerMod, Initable {
 *     @Override
 *     public void init() {
 *         // Register custom items, creatures, actions
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Example (Recommended - Just use WurmServerMod):</strong></p>
 * <pre>{@code
 * public class MyMod implements WurmServerMod {
 *     @Override
 *     public void init() {
 *         // Register custom items, creatures, actions
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see WurmServerMod
 * @see WurmServerMod#init()
 */
public interface Initable {

    /**
     * Perform initialization during server startup.
     *
     * <p>This method is called after {@link PreInitable#preInit()} and after
     * the Wurm server has loaded its core systems. Use this method to:</p>
     * <ul>
     *   <li>Register custom item templates</li>
     *   <li>Register custom creature templates</li>
     *   <li>Register custom actions</li>
     *   <li>Initialize mod systems that depend on Wurm classes</li>
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method is called from the main
     * server thread during initialization.</p>
     */
    void init();
}

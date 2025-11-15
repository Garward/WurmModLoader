package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Interface for mods that need to perform early initialization before server startup.
 *
 * <p>This interface extends the internal PreInitable interface to ensure compatibility
 * with the ModLoader's lifecycle management.</p>
 *
 * <p><strong>Note:</strong> This interface is redundant for most modern mods.
 * {@link WurmServerMod} already provides a {@code preInit()} default method,
 * so mods can simply implement WurmServerMod and override {@code preInit()}
 * without implementing this interface separately.</p>
 *
 * <p>This interface exists for consistency with the modloader architecture
 * and for mods that may want to explicitly declare their use of the preInit phase.</p>
 *
 * <p><strong>Lifecycle Order:</strong></p>
 * <ol>
 *   <li>{@link Configurable#configure(java.util.Properties)} - Load configuration (if implemented)</li>
 *   <li>{@link #preInit()} - Register bytecode hooks and early initialization</li>
 *   <li>{@link WurmServerMod#init()} - Register content and actions</li>
 * </ol>
 *
 * <p><strong>Example (Explicit):</strong></p>
 * <pre>{@code
 * public class MyMod implements WurmServerMod, PreInitable {
 *     @Override
 *     public void preInit() {
 *         // Register bytecode hooks
 *         ModActions.init();
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Example (Recommended - Just use WurmServerMod):</strong></p>
 * <pre>{@code
 * public class MyMod implements WurmServerMod {
 *     @Override
 *     public void preInit() {
 *         // Register bytecode hooks
 *         ModActions.init();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see WurmServerMod
 * @see WurmServerMod#preInit()
 */
public interface PreInitable {

    /**
     * Perform early initialization before server startup.
     *
     * <p>This method is called before {@link WurmServerMod#init()} and before
     * the Wurm server initializes its core systems. Use this method to:</p>
     * <ul>
     *   <li>Register bytecode hooks using HookManager</li>
     *   <li>Initialize action systems with {@code ModActions.init()}</li>
     *   <li>Modify server class behavior with Javassist</li>
     * </ul>
     *
     * <p><strong>Warning:</strong> Most Wurm server classes are not yet loaded
     * at this point. Attempting to access Wurm server objects or templates
     * will likely fail. Use {@link WurmServerMod#init()} for standard
     * initialization tasks.</p>
     *
     * <p><strong>Thread Safety:</strong> This method is called from the main
     * server thread during initialization.</p>
     */
    void preInit();
}

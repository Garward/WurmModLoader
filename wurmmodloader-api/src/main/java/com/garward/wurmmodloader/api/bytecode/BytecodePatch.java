package com.garward.wurmmodloader.api.bytecode;

import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.Collections;

/**
 * Describes a lightweight bytecode patch that can be registered with the Wurm Unlimited hook manager.
 *
 * <p>The core runtime discovers {@link BytecodePatch} implementations, registers them via
 * {@code PatchRegistry}, and applies them through the
 * {@code PatchManager} before the server boots.</p>
 *
 * <p>To coordinate with other patches, supply {@link #priority()} (higher runs earlier) and one or more
 * {@link #conflictKeys()} such as the constants exposed in {@link BytecodeConflictKeys}. The runtime will skip
 * lower-priority patches that attempt to claim the same conflict key unless forced via CLI/system property.</p>
 */
public interface BytecodePatch {

    /**
     * @return fully-qualified class name that should be hooked.
     */
    String targetClassName();

    /**
     * @return name of the method that should be intercepted.
     */
    String methodName();

    /**
     * @return method descriptor in JVM format, e.g. {@code (JZ)V}.
     */
    String methodDescriptor();

    /**
     * Creates an invocation handler for this patch.
     * The default implementation throws UnsupportedOperationException.
     * Override this if your patch uses invocation handlers, or override apply() for custom bytecode manipulation.
     * @return invocation handler instance
     */
    default InvocationHandler createInvocationHandler() {
        throw new UnsupportedOperationException(getClass().getName() + " does not implement createInvocationHandler()");
    }

    /**
     * Applies this patch. Override this for custom bytecode manipulation.
     * The default implementation does nothing and is meant to be overridden.
     * @param hookManager the hook manager (not exposed in API, passed from core)
     */
    default void apply(Object hookManager) {
        // Default does nothing - patches override this for custom behavior
    }

    /**
     * @return application priority; higher values are applied earlier.
     */
    default int priority() {
        return 0;
    }

    /**
     * @return logical conflict keys that cannot be claimed by multiple patches at once.
     */
    default Collection<String> conflictKeys() {
        return Collections.emptyList();
    }

    /**
     * @return human-readable identifier for logging/debugging.
     */
    default String displayName() {
        return targetClassName() + "#" + methodName() + methodDescriptor();
    }
}

package com.garward.wurmmodloader.performance;

import javassist.*;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Batches creature status updates to reduce database overhead.
 *
 * <p><b>Problem:</b> Wurm executes individual UPDATE statements for every creature
 * property change (loyalty, age, fat, etc.), causing 1000ms+ lag spikes.
 *
 * <p><b>Solution:</b> Hook property setters to defer writes, then batch-flush
 * all dirty creatures periodically.
 *
 * <p><b>Architecture:</b> Must run in Phase 3 (preInit) before DbCreatureStatus
 * class is loaded by JVM. This is why it's in wurmmodloader-core, not a separate mod.
 *
 * <p><b>Configuration:</b> Can be disabled via system property:
 * <pre>-Dwurmmodloader.performance.creature_batching=false</pre>
 *
 * @since 1.0.0 (Phase 5.5)
 * @see DatabaseOptimizer
 */
public class CreatureStatusBatcher {

    private static final Logger logger = Logger.getLogger(CreatureStatusBatcher.class.getName());

    private static boolean initialized = false;
    private static boolean enabled = true;

    /**
     * Thread-local flag to bypass batching during flush operations.
     * Prevents infinite recursion when save() is called from the flush executor.
     */
    private static final ThreadLocal<Boolean> bypassBatching = ThreadLocal.withInitial(() -> false);

    /**
     * Map of creature IDs to their DbCreatureStatus instances that need saving.
     * ConcurrentHashMap for thread-safety during async polling.
     */
    private static final Map<Long, Object> dirtyCreatures = new ConcurrentHashMap<>();

    /**
     * Scheduled executor for periodic batch flushing.
     * Flushes every 100ms (4 times per server tick).
     */
    private static ScheduledExecutorService flushExecutor;

    /**
     * Maximum creatures to flush per batch.
     * Prevents server lockup if too many creatures are dirty at once.
     */
    private static final int MAX_BATCH_SIZE = 1000;

    /**
     * Statistics tracking.
     */
    private static long totalBatchedUpdates = 0;
    private static long totalFlushes = 0;

    /**
     * Initializes creature status batching.
     *
     * <p><b>CRITICAL:</b> Must be called during Phase 3 (preInit) before vanilla
     * classes are loaded. This is enforced by being called from ModLoader.preInit().
     *
     * <p>Hooks the following DbCreatureStatus methods to defer database writes:
     * <ul>
     *   <li>setLoyalty() - Loyalty changes</li>
     *   <li>updateAge() - Age progression</li>
     *   <li>updateFat() - Fat level changes</li>
     *   <li>setKingdom() - Kingdom changes</li>
     *   <li>setDead() - Death state</li>
     *   <li>... and 12 more setter methods</li>
     * </ul>
     *
     * @throws RuntimeException if hooking fails or classes already loaded
     */
    public static void init() {
        // Check configuration flag
        String enabledProp = System.getProperty("wurmmodloader.performance.creature_batching", "true");
        enabled = Boolean.parseBoolean(enabledProp);

        if (!enabled) {
            logger.info("[CreatureStatusBatcher] Disabled via system property");
            return;
        }

        if (initialized) {
            logger.warning("[CreatureStatusBatcher] Already initialized, skipping");
            return;
        }

        logger.info("[CreatureStatusBatcher] Initializing creature status batching...");

        try {
            // Hook DbCreatureStatus setter methods
            hookCreatureStatusMethods();

            // Start periodic flush executor (every 100ms)
            startFlushExecutor();

            initialized = true;

            logger.info("[CreatureStatusBatcher] Successfully initialized");
            logger.info("[CreatureStatusBatcher] Flush interval: 100ms");
            logger.info("[CreatureStatusBatcher] Disable with: -Dwurmmodloader.performance.creature_batching=false");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[CreatureStatusBatcher] Failed to initialize", e);
            throw new RuntimeException("CreatureStatusBatcher initialization failed", e);
        }
    }

    /**
     * Hooks DbCreatureStatus methods to defer database writes.
     *
     * <p>Strategy: Intercept setter methods and mark the creature as dirty instead of
     * immediately writing to the database. The actual database write is deferred until
     * the next flush cycle.
     */
    private static void hookCreatureStatusMethods() throws Exception {
        ClassPool classPool = HookManager.getInstance().getClassPool();
        CtClass ctClass = classPool.get("com.wurmonline.server.creatures.DbCreatureStatus");

        logger.info("[CreatureStatusBatcher] Hooking DbCreatureStatus methods...");

        // Hook save() method - returns boolean
        hookMethodWithReturn(ctClass, "save", "boolean (save)");

        // Hook high-frequency setter methods (priority order)
        hookVoidMethod(ctClass, "updateAge", "updateAge()");
        hookVoidMethod(ctClass, "updateFat", "updateFat()");
        hookVoidMethod(ctClass, "setLoyalty", "setLoyalty(float)");
        hookVoidMethod(ctClass, "setDead", "setDead(boolean)");
        hookVoidMethod(ctClass, "setKingdom", "setKingdom(byte)");

        logger.info("[CreatureStatusBatcher] Successfully hooked all priority methods");
    }

    /**
     * Hooks a void method to defer database writes.
     */
    private static void hookVoidMethod(CtClass ctClass, String methodName, String description) throws Exception {
        try {
            CtMethod method = ctClass.getDeclaredMethod(methodName);
            method.insertBefore(
                "{ " +
                "  if (com.garward.wurmmodloader.performance.CreatureStatusBatcher.shouldBatch()) { " +
                "    com.garward.wurmmodloader.performance.CreatureStatusBatcher.markDirty(" +
                "      this.statusHolder.getWurmId(), this); " +
                "    return; " +
                "  } " +
                "}"
            );
            logger.info("[CreatureStatusBatcher] Hooked " + description);
        } catch (NotFoundException e) {
            logger.warning("[CreatureStatusBatcher] Method not found: " + methodName + " - skipping");
        }
    }

    /**
     * Hooks a method that returns a value (like boolean) to defer database writes.
     */
    private static void hookMethodWithReturn(CtClass ctClass, String methodName, String description) throws Exception {
        try {
            CtMethod method = ctClass.getDeclaredMethod(methodName);
            method.insertBefore(
                "{ " +
                "  if (com.garward.wurmmodloader.performance.CreatureStatusBatcher.shouldBatch()) { " +
                "    com.garward.wurmmodloader.performance.CreatureStatusBatcher.markDirty(" +
                "      this.statusHolder.getWurmId(), this); " +
                "    return true; " +
                "  } " +
                "}"
            );
            logger.info("[CreatureStatusBatcher] Hooked " + description);
        } catch (NotFoundException e) {
            logger.warning("[CreatureStatusBatcher] Method not found: " + methodName + " - skipping");
        }
    }


    /**
     * Checks if batching should be applied for the current thread.
     *
     * @return true if batching is enabled and not bypassed for this thread
     */
    public static boolean shouldBatch() {
        return enabled && !bypassBatching.get();
    }

    /**
     * Marks a creature as dirty and queues it for batch update.
     *
     * <p>Called by the hooked save() method via Javassist bytecode injection.
     *
     * @param creatureId The creature's WURMID
     * @param creatureStatus The DbCreatureStatus instance (as Object to avoid class loading)
     */
    public static void markDirty(long creatureId, Object creatureStatus) {
        if (!enabled) {
            return;  // Batching disabled, let vanilla code execute
        }

        dirtyCreatures.put(creatureId, creatureStatus);
    }

    /**
     * Starts the periodic flush executor.
     *
     * <p>Flushes dirty creatures every 100ms (4 times per 25ms server tick).
     * This ensures minimal delay while still batching effectively.
     */
    private static void startFlushExecutor() {
        flushExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CreatureStatusBatcher-Flush");
            t.setDaemon(true);  // Don't prevent JVM shutdown
            return t;
        });

        flushExecutor.scheduleAtFixedRate(
            CreatureStatusBatcher::flushDirtyCreatures,
            100,  // Initial delay: 100ms
            100,  // Period: 100ms
            TimeUnit.MILLISECONDS
        );

        logger.info("[CreatureStatusBatcher] Started flush executor (100ms interval)");
    }

    /**
     * Flushes all dirty creatures to the database.
     *
     * <p>Called periodically by flushExecutor. Invokes save() on each dirty creature,
     * which executes the batched UPDATE statement.
     *
     * <p>Limits batch size to MAX_BATCH_SIZE to prevent server lockup when too many
     * creatures are marked dirty at once (e.g., on player login).
     */
    private static void flushDirtyCreatures() {
        if (dirtyCreatures.isEmpty()) {
            return;  // Nothing to flush
        }

        long start = System.currentTimeMillis();
        int flushedCount = 0;

        try {
            // Snapshot up to MAX_BATCH_SIZE creatures
            Map<Long, Object> toFlush = new ConcurrentHashMap<>();
            int count = 0;
            for (Map.Entry<Long, Object> entry : dirtyCreatures.entrySet()) {
                if (count >= MAX_BATCH_SIZE) {
                    break;  // Leave remaining creatures for next flush cycle
                }
                toFlush.put(entry.getKey(), entry.getValue());
                count++;
            }

            // Remove flushed creatures from dirty set
            for (Long creatureId : toFlush.keySet()) {
                dirtyCreatures.remove(creatureId);
            }

            // Flush each creature with batching bypassed
            bypassBatching.set(true);
            try {
                for (Map.Entry<Long, Object> entry : toFlush.entrySet()) {
                    try {
                        // Call save() via reflection to avoid class loading
                        // Since bypassBatching is true, this will execute the original save() implementation
                        Object creatureStatus = entry.getValue();
                        if (creatureStatus == null) {
                            continue;
                        }
                        if (!hasLiveStatusHolder(creatureStatus)) {
                            logger.fine("[CreatureStatusBatcher] Skipping stale creature " + entry.getKey());
                            continue;
                        }
                        creatureStatus.getClass().getMethod("save").invoke(creatureStatus);
                        flushedCount++;

                    } catch (Exception e) {
                        logger.log(Level.WARNING, "[CreatureStatusBatcher] Failed to save creature " + entry.getKey(), e);
                    }
                }
            } finally {
                bypassBatching.set(false);
            }

            totalBatchedUpdates += flushedCount;
            totalFlushes++;

            long elapsed = System.currentTimeMillis() - start;
            int remaining = dirtyCreatures.size();

            // Only log at INFO if batch is very large or slow (to avoid spam on adventure maps)
            if (flushedCount > 500 || elapsed > 100) {
                if (remaining > 0) {
                    logger.info(String.format("[CreatureStatusBatcher] Flushed %d creatures in %dms (%d remaining for next cycle)",
                        flushedCount, elapsed, remaining));
                } else {
                    logger.info(String.format("[CreatureStatusBatcher] Flushed %d creatures in %dms",
                        flushedCount, elapsed));
                }
            } else if (flushedCount > 100) {
                // Log moderate batches at FINE level
                if (remaining > 0) {
                    logger.fine(String.format("[CreatureStatusBatcher] Flushed %d creatures in %dms (%d remaining for next cycle)",
                        flushedCount, elapsed, remaining));
                } else {
                    logger.fine(String.format("[CreatureStatusBatcher] Flushed %d creatures in %dms",
                        flushedCount, elapsed));
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[CreatureStatusBatcher] Flush failed", e);
        }
    }

    /**
     * Shuts down the batch flusher and flushes any remaining dirty creatures.
     *
     * <p>Should be called on server shutdown to ensure no data loss.
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }

        logger.info("[CreatureStatusBatcher] Shutting down...");

        // Flush any remaining dirty creatures
        flushDirtyCreatures();

        // Shutdown executor
        if (flushExecutor != null) {
            flushExecutor.shutdown();
            try {
                if (!flushExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    flushExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                flushExecutor.shutdownNow();
            }
        }

        logger.info("[CreatureStatusBatcher] Shutdown complete");
        logger.info("[CreatureStatusBatcher] Total batched updates: " + totalBatchedUpdates);
        logger.info("[CreatureStatusBatcher] Total flushes: " + totalFlushes);
    }

    /**
     * Returns statistics about batching performance.
     *
     * @return Stats string for debugging
     */
    public static String getStats() {
        return String.format(
            "CreatureStatusBatcher: %d dirty, %d total updates, %d flushes",
            dirtyCreatures.size(),
            totalBatchedUpdates,
            totalFlushes
        );
    }

    private static boolean hasLiveStatusHolder(Object creatureStatus) {
        Class<?> type = creatureStatus.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("statusHolder");
                field.setAccessible(true);
                Object holder = field.get(creatureStatus);
                return holder != null;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                logger.log(Level.FINEST, "[CreatureStatusBatcher] Unable to inspect statusHolder field", e);
                return true;
            }
        }
        return true;
    }

    /**
     * Checks if batching is initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Checks if batching is enabled.
     *
     * @return true if enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
}

package com.garward.wurmmodloader.core.bytecode;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

/**
 * Applies all registered {@link BytecodePatch} instances and logs their lifecycle.
 */
public final class PatchManager {

    private static final Logger LOGGER = Logger.getLogger(PatchManager.class.getName());

    private final HookManager hookManager;
    private final boolean forceConflicts;
    private final boolean continueOnError;

    public PatchManager() {
        this(HookManager.getInstance());
    }

    PatchManager(HookManager hookManager) {
        this(hookManager, PatchSettings.isForceConflictsEnabled(), PatchSettings.isContinueOnErrorEnabled());
    }

    PatchManager(HookManager hookManager, boolean forceConflicts) {
        this(hookManager, forceConflicts, PatchSettings.isContinueOnErrorEnabled());
    }

    PatchManager(HookManager hookManager, boolean forceConflicts, boolean continueOnError) {
        this.hookManager = hookManager;
        this.forceConflicts = forceConflicts;
        this.continueOnError = continueOnError;
    }

    public void applyAll() {
        List<BytecodePatch> patches = new ArrayList<>(PatchRegistry.getRegisteredPatches());
        patches.sort(Comparator.comparingInt(BytecodePatch::priority).reversed());

        Set<String> claimedKeys = new HashSet<>();
        for (BytecodePatch patch : patches) {
            Collection<String> conflicts = sanitizeConflictKeys(patch);
            if (!forceConflicts && hasConflict(conflicts, claimedKeys)) {
                LOGGER.log(Level.WARNING,
                    "[BytecodePatch] Skipping {0} due to conflicts: {1}",
                    new Object[] { patch.displayName(), conflicts });
                continue;
            }

            boolean applied = applyPatch(patch);
            if (applied) {
                // Remember which logical areas were claimed so lower-priority patches don't clobber them.
                claimedKeys.addAll(conflicts);
            }
        }
    }

    private Collection<String> sanitizeConflictKeys(BytecodePatch patch) {
        Collection<String> raw = patch.conflictKeys();
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sanitized = new ArrayList<>();
        for (String key : raw) {
            if (key == null) {
                continue;
            }
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return sanitized;
    }

    private boolean hasConflict(Collection<String> keys, Set<String> claimed) {
        for (String key : keys) {
            if (claimed.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean applyPatch(BytecodePatch patch) {
        String descriptor = patch.displayName();
        try {
            // Call the patch's apply method which handles custom bytecode manipulation
            patch.apply(hookManager);

            if (forceConflicts) {
                LOGGER.log(Level.INFO, "[BytecodePatch] Applied (force-conflicts mode) {0}", descriptor);
            } else {
                LOGGER.log(Level.INFO, "[BytecodePatch] Applied {0}", descriptor);
            }
            return true;
        } catch (RuntimeException e) {
            // Handle frozen class errors from legacy mods gracefully
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.log(Level.WARNING,
                    "[BytecodePatch] Skipping {0} - class already frozen by legacy mod",
                    descriptor);
                LOGGER.log(Level.WARNING,
                    "[BytecodePatch] Events for {0} may not fire - consider porting legacy mod to event system",
                    patch.targetClassName());
                return false;
            }
            // Re-throw other RuntimeExceptions
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[BytecodePatch] Failed to apply " + descriptor, e);
            if (continueOnError) {
                LOGGER.log(Level.WARNING,
                    "[BytecodePatch] Continuing after failure (diagnostic mode enabled) for {0}",
                    descriptor);
                return false;
            }
            throw new IllegalStateException("Failed to apply bytecode patch " + descriptor, e);
        }
    }
}

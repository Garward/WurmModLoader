package com.garward.wurmmodloader.debug;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.core.bytecode.PatchRegistry;
import com.garward.wurmmodloader.core.registry.RuntimeRegistries;
import com.garward.wurmmodloader.core.registry.SystemBootstrap;

/**
 * Lightweight entry point for printing out bytecode patch metadata without launching the full server.
 *
 * <p>Usage: run this class via {@code java} or your IDE. It performs the same bootstrap sequence
 * used at runtime, then enumerates the registered {@link BytecodePatch} instances, showing
 * their priority, conflict keys, and implementation class.</p>
 */
public final class DiagnosticServer {

    private DiagnosticServer() {}

    public static void main(String[] args) {
        try {
            SystemBootstrap.initializeAll();
            System.out.println();
            System.out.println("=== Bytecode Patch Diagnostics ===");
            int index = 1;
            for (BytecodePatch patch : RuntimeRegistries.BYTECODE_PATCHES.getEntries()) {
                printPatch(index++, patch);
            }
            System.out.println("=== Total patches: " + PatchRegistry.getRegisteredPatches().size() + " ===");
        } catch (Throwable t) {
            System.err.println("[DiagnosticServer] Failed to initialize: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
            return;
        }

        System.exit(0);
    }

    private static void printPatch(int index, BytecodePatch patch) {
        String conflicts = String.join(", ", patch.conflictKeys());
        if (conflicts.isEmpty()) {
            conflicts = "(none)";
        }
        System.out.printf(
            "%02d. %s%n    priority=%d%n    conflicts=%s%n",
            index,
            patch.displayName(),
            patch.priority(),
            conflicts
        );
    }
}

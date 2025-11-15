package com.garward.wurmmodloader.core.registry;

import com.garward.wurmmodloader.core.bytecode.CoreBytecodePatches;
import com.garward.wurmmodloader.core.bytecode.PatchManager;
import com.garward.wurmmodloader.core.bytecode.PatchRegistry;
import com.garward.wurmmodloader.core.event.EventLogicClasspathScanner;
import com.garward.wurmmodloader.core.eventlogic.combat.timing.DualWieldScheduler;
import com.garward.wurmmodloader.core.eventlogic.combat.timing.SwingSpeedAdjuster;
import com.garward.wurmmodloader.core.eventlogic.combat.timing.WeaponTimerReset;
import com.garward.wurmmodloader.core.eventlogic.materials.MaterialEventHandler;
import com.garward.wurmmodloader.core.ui.UIFramework;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class SystemBootstrap {

    private static final Logger LOGGER = Logger.getLogger(SystemBootstrap.class.getName());
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static void initializeAll() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            LOGGER.fine("[BOOTSTRAP] SystemBootstrap already initialized; skipping.");
            return;
        }

        System.out.println("[BOOTSTRAP] Initializing WurmModLoader systems...");

        try {
            // 1. Initialize bytecode patches first
            PatchRegistry.reset();
            CoreBytecodePatches.registerAll();
            RuntimeRegistries.BYTECODE_PATCHES.initializeAll();
            new PatchManager().applyAll();

        // 2. Discover and register event logic before enabling the registry
        RuntimeRegistries.EVENT_LOGIC.register(MaterialEventHandler.class);
        RuntimeRegistries.EVENT_LOGIC.register(SwingSpeedAdjuster.class);
        RuntimeRegistries.EVENT_LOGIC.register(WeaponTimerReset.class);
        RuntimeRegistries.EVENT_LOGIC.register(DualWieldScheduler.class);
        EventLogicClasspathScanner.scanAndRegister();
        RuntimeRegistries.EVENT_LOGIC.initializeAll();

            // 3. Run general systems (e.g., metrics, reflection scanners, UI framework)
            RuntimeRegistries.SYSTEMS.register(UIFramework.AutoInitializer::register);
            RuntimeRegistries.SYSTEMS.initializeAll();

            // 4. Freeze runtime registries (framework-level)
            RuntimeRegistries.BYTECODE_PATCHES.freeze();
            RuntimeRegistries.EVENT_LOGIC.freeze();
            RuntimeRegistries.SYSTEMS.freeze();

            // NOTE: Game content registries (ITEMS, CREATURES, ICONS, etc.) are NOT frozen here.
            // They will be frozen AFTER mods have registered their content in DelegatedLauncher.

            System.out.println("[BOOTSTRAP] All registries initialized and frozen.");
        } catch (RuntimeException | Error e) {
            INITIALIZED.set(false);
            throw e;
        }
    }

    private SystemBootstrap() {}
}

package com.garward.wurmmodloader.core.registry;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.core.bytecode.PatchRegistry;
import com.garward.wurmmodloader.core.event.EventLogicBootstrap;

public final class RuntimeRegistries {

    public static final RuntimeRegistry<BytecodePatch> BYTECODE_PATCHES =
        new RuntimeRegistry<>("bytecode_patches", PatchRegistry::register);

    public static final RuntimeRegistry<Class<?>> EVENT_LOGIC =
        new RuntimeRegistry<>("event_logic", EventLogicBootstrap::registerHandlerClass);

    public static final RuntimeRegistry<Runnable> SYSTEMS =
        new RuntimeRegistry<>("systems", Runnable::run);

    private RuntimeRegistries() {}
}

package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches Item container volume/size methods to fire ContainerVolumeEvent.
 * Allows mods to modify container capacity and dimensions.
 *
 * Targets:
 * - com.wurmonline.server.items.Item.getContainerVolume()
 * - com.wurmonline.server.items.Item.getContainerSizeX()
 * - com.wurmonline.server.items.Item.getContainerSizeY()
 * - com.wurmonline.server.items.Item.getContainerSizeZ()
 */
public class ContainerVolumePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ContainerVolumePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Item";
    }

    @Override
    public String methodName() {
        return "getContainerVolume"; // Primary method, will patch others too
    }

    @Override
    public String methodDescriptor() {
        return "()I";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctItem = classPool.get(targetClassName());

            patchContainerMethods(ctItem);

            LOGGER.info("[BytecodePatch] Registered ContainerVolumePatch successfully");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ContainerVolumePatch", e);
        }
    }

    private void patchContainerMethods(CtClass ctItem) throws NotFoundException, CannotCompileException {
        // Patch getContainerVolume()
        try {
            CtMethod getVolumeMethod = ctItem.getDeclaredMethod("getContainerVolume");
            getVolumeMethod.insertBefore(
                "{ " +
                "  if (this.isHollow()) { " +
                "    int originalVolume = this.template.getContainerVolume(); " +
                "    int modifiedVolume = " + ProxyServerHook.class.getName() + ".fireContainerVolumeEvent(" +
                "      this.getWurmId(), " +
                "      this.getName(), " +
                "      originalVolume, " +
                "      0 " + // VolumeType.VOLUME ordinal
                "    ); " +
                "    if (modifiedVolume != originalVolume) { " +
                "      return modifiedVolume; " +
                "    } " +
                "  } " +
                "}"
            );
        } catch (NotFoundException e) {
            LOGGER.warning("[ContainerVolumePatch] getContainerVolume() not found, skipping");
        }

        // Patch getContainerSizeX()
        try {
            CtMethod getSizeXMethod = ctItem.getDeclaredMethod("getContainerSizeX");
            getSizeXMethod.insertBefore(
                "{ " +
                "  if (this.isHollow()) { " +
                "    int originalSize = this.template.getContainerSizeX(); " +
                "    int modifiedSize = " + ProxyServerHook.class.getName() + ".fireContainerVolumeEvent(" +
                "      this.getWurmId(), " +
                "      this.getName(), " +
                "      originalSize, " +
                "      1 " + // VolumeType.SIZE_X ordinal
                "    ); " +
                "    if (modifiedSize != originalSize) { " +
                "      return modifiedSize; " +
                "    } " +
                "  } " +
                "}"
            );
        } catch (NotFoundException e) {
            LOGGER.warning("[ContainerVolumePatch] getContainerSizeX() not found, skipping");
        }

        // Patch getContainerSizeY()
        try {
            CtMethod getSizeYMethod = ctItem.getDeclaredMethod("getContainerSizeY");
            getSizeYMethod.insertBefore(
                "{ " +
                "  if (this.isHollow()) { " +
                "    int originalSize = this.template.getContainerSizeY(); " +
                "    int modifiedSize = " + ProxyServerHook.class.getName() + ".fireContainerVolumeEvent(" +
                "      this.getWurmId(), " +
                "      this.getName(), " +
                "      originalSize, " +
                "      2 " + // VolumeType.SIZE_Y ordinal
                "    ); " +
                "    if (modifiedSize != originalSize) { " +
                "      return modifiedSize; " +
                "    } " +
                "  } " +
                "}"
            );
        } catch (NotFoundException e) {
            LOGGER.warning("[ContainerVolumePatch] getContainerSizeY() not found, skipping");
        }

        // Patch getContainerSizeZ()
        try {
            CtMethod getSizeZMethod = ctItem.getDeclaredMethod("getContainerSizeZ");
            getSizeZMethod.insertBefore(
                "{ " +
                "  if (this.isHollow()) { " +
                "    int originalSize = this.template.getContainerSizeZ(); " +
                "    int modifiedSize = " + ProxyServerHook.class.getName() + ".fireContainerVolumeEvent(" +
                "      this.getWurmId(), " +
                "      this.getName(), " +
                "      originalSize, " +
                "      3 " + // VolumeType.SIZE_Z ordinal
                "    ); " +
                "    if (modifiedSize != originalSize) { " +
                "      return modifiedSize; " +
                "    } " +
                "  } " +
                "}"
            );
        } catch (NotFoundException e) {
            LOGGER.warning("[ContainerVolumePatch] getContainerSizeZ() not found, skipping");
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton("CONTAINER_VOLUME");
    }
}

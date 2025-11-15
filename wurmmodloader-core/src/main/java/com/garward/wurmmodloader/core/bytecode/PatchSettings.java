package com.garward.wurmmodloader.core.bytecode;

/**
 * Centralizes feature flags that influence how bytecode patches are applied.
 */
public final class PatchSettings {

    private static final String FORCE_CONFLICTS_PROPERTY = "wurmmodloader.bytecode.forceConflicts";
    private static final String CONTINUE_ON_ERROR_PROPERTY = "wurmmodloader.bytecode.continueOnError";

    private PatchSettings() {}

    /**
     * @return true if users requested that conflicting patches are force-applied (best-effort) instead of skipped.
     */
    public static boolean isForceConflictsEnabled() {
        return Boolean.getBoolean(FORCE_CONFLICTS_PROPERTY);
    }

    /**
     * Sets the force flag programmatically (e.g., CLI parsing) so future lookups can respect it.
     */
    public static void setForceConflictsEnabled(boolean enabled) {
        System.setProperty(FORCE_CONFLICTS_PROPERTY, Boolean.toString(enabled));
    }

    /**
     * @return true if users requested that patch application should continue after failures (diagnostic mode).
     */
    public static boolean isContinueOnErrorEnabled() {
        return Boolean.getBoolean(CONTINUE_ON_ERROR_PROPERTY);
    }

    /**
     * Enables/disables the continue-on-error diagnostic flag.
     */
    public static void setContinueOnErrorEnabled(boolean enabled) {
        System.setProperty(CONTINUE_ON_ERROR_PROPERTY, Boolean.toString(enabled));
    }
}

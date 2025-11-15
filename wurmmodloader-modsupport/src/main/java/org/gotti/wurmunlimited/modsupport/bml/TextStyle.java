package org.gotti.wurmunlimited.modsupport.bml;

/**
 * Legacy TextStyle enum wrapper for backward compatibility.
 * Delegates to the new implementation.
 */
public enum TextStyle {
    BOLD,
    ITALIC,
    BOLDITALIC;

    public String getType() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

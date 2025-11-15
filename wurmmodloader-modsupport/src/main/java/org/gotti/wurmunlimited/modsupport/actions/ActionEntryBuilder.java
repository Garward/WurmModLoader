package org.gotti.wurmunlimited.modsupport.actions;

/**
 * Legacy ActionEntryBuilder wrapper for backward compatibility.
 * Delegates to the new implementation.
 */
public class ActionEntryBuilder extends com.garward.wurmmodloader.modsupport.actions.ActionEntryBuilder {

    public ActionEntryBuilder(short number, String actionString, String verbString) {
        super(number, actionString, verbString);
    }

    public ActionEntryBuilder(short number, String actionString, String verbString, int[] types) {
        super(number, actionString, verbString, types);
    }
}

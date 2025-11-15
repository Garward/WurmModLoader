package org.gotti.wurmunlimited.modloader.classhooks;

import javassist.bytecode.LocalVariableAttribute;

/**
 * Legacy compatibility alias for LocalNameLookup.
 *
 * @deprecated Use {@link com.garward.wurmmodloader.modloader.internal.classhooks.LocalNameLookup} instead
 */
@Deprecated
public class LocalNameLookup extends com.garward.wurmmodloader.modloader.internal.classhooks.LocalNameLookup {

    public LocalNameLookup(LocalVariableAttribute attribute) {
        super(attribute);
    }
}

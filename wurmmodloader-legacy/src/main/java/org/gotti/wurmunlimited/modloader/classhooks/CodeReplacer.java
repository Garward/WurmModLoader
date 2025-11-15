package org.gotti.wurmunlimited.modloader.classhooks;

import javassist.bytecode.CodeAttribute;

/**
 * Legacy compatibility alias for CodeReplacer.
 *
 * @deprecated Use {@link com.garward.wurmmodloader.modloader.internal.classhooks.CodeReplacer} instead
 */
@Deprecated
public class CodeReplacer extends com.garward.wurmmodloader.modloader.internal.classhooks.CodeReplacer {

    public CodeReplacer(CodeAttribute codeAttribute) {
        super(codeAttribute);
    }
}

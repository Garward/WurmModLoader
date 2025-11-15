package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bytecode patch for com.wurmonline.server.console.CommandReader.
 *
 * <p>Adds support for GM commands to be executed directly from the server console
 * without requiring login. This is a groundbreaking feature that nobody has implemented
 * for Wurm Unlimited before.</p>
 *
 * <p><strong>What This Does:</strong></p>
 * <ul>
 *   <li>Detects console input starting with '#' (GM commands)</li>
 *   <li>Routes GM commands to ConsoleGMCommandRouter for execution</li>
 *   <li>Maintains existing 'shutdown' command functionality</li>
 *   <li>All other commands trigger "Unknown command" warning</li>
 * </ul>
 *
 * <p><strong>Safety:</strong></p>
 * <ul>
 *   <li>Commands run in modloader classloader (isolated from Wurm)</li>
 *   <li>Commands execute on main server thread (not background thread)</li>
 *   <li>No Player object required - uses reflection instead</li>
 *   <li>Always executes at GM power level 5</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CommandReaderPatch {

    private static final Logger LOGGER = Logger.getLogger(CommandReaderPatch.class.getName());

    /**
     * Apply the bytecode patch to CommandReader.
     *
     * <p>This must be called during Phase 3 (preInit) before the class is loaded.</p>
     *
     * @throws Exception if patching fails
     */
    public static void apply() throws Exception {
        LOGGER.info("[BytecodePatch] Applying CommandReaderPatch...");

        CtClass ctClass = HookManager.getInstance().getClassPool()
            .get("com.wurmonline.server.console.CommandReader");

        CtMethod runMethod = ctClass.getDeclaredMethod("run");

        // We need to replace the "Unknown command" warning with our GM command check
        // Original code (line 35-39):
        //   if (nextLine.equals("shutdown")) {
        //       this.server.shutDown();
        //       break;
        //   }
        //   logger.warning("Unknown command: " + nextLine);
        //
        // New code:
        //   if (nextLine.startsWith("#")) {
        //       com.garward.wurmmodloader.core.console.ConsoleGMCommandRouter.queueCommand(nextLine);
        //   } else if (nextLine.equals("shutdown")) {
        //       this.server.shutDown();
        //       break;
        //   } else {
        //       logger.warning("Unknown command: " + nextLine);
        //   }

        // Use ExprEditor to replace the logger.warning() call
        runMethod.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws javassist.CannotCompileException {
                // Find logger.warning("Unknown command: " + nextLine)
                if (m.getMethodName().equals("warning") && m.getClassName().contains("Logger")) {
                    // Replace with our GM command check
                    String replacement =
                        "{" +
                        "    if (nextLine.startsWith(\"#\")) {" +
                        "        try {" +
                        "            com.garward.wurmmodloader.core.console.ConsoleGMCommandRouter.queueCommand(nextLine);" +
                        "        } catch (Exception e) {" +
                        "            System.out.println(\"[Console GM] Failed to queue command: \" + e.getMessage());" +
                        "        }" +
                        "    } else {" +
                        "        $proceed($$);" +  // Call original logger.warning()
                        "    }" +
                        "}";
                    m.replace(replacement);
                }
            }
        });

        LOGGER.info("[BytecodePatch] CommandReaderPatch applied successfully");
    }
}

package com.garward.wurmmodloader.core.console;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Searchable index of GM help lines (vanilla + framework additions), used by
 * the in-game {@code #help <query>} fuzzy lookup. Vanilla lines are extracted
 * once at boot from the bytecode of {@code Communicator.sendDeityHelp(int)}
 * so the list always matches whatever WU build is running.
 */
public final class GmHelpIndex {

    private static final Logger LOGGER = Logger.getLogger(GmHelpIndex.class.getName());

    public static final class Entry {
        public final String line;       // full help text as displayed
        public final int minPower;      // best-effort minimum power required
        public final boolean framework; // true = our addendum, false = vanilla

        Entry(String line, int minPower, boolean framework) {
            this.line = line;
            this.minPower = minPower;
            this.framework = framework;
        }
    }

    private static final List<Entry> ENTRIES = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean vanillaPopulated = false;

    private GmHelpIndex() {}

    /** Adds a framework help line with its minimum power. Called from bridge install. */
    public static void addFramework(String line, int minPower) {
        ENTRIES.add(new Entry(line, minPower, true));
    }

    /**
     * Walk {@code sendDeityHelp(int)}'s bytecode and capture every string
     * constant, pairing it with the most recent integer compared against
     * {@code power} as the inferred min-power for that line.
     *
     * <p>Best-effort: a few non-power-gated branches (test-server, mayAppoint*)
     * inherit whatever power level was last set. Close enough for filtering.
     */
    public static synchronized void populateVanilla(CtClass communicator) {
        if (vanillaPopulated) return;
        try {
            CtMethod m = communicator.getDeclaredMethod("sendDeityHelp");
            MethodInfo mi = m.getMethodInfo();
            CodeAttribute ca = mi.getCodeAttribute();
            if (ca == null) return;
            ConstPool cp = ca.getConstPool();
            CodeIterator it = ca.iterator();

            int lastInt = -1;
            int currentPower = 0;

            while (it.hasNext()) {
                int pos = it.next();
                int op = it.byteAt(pos);
                switch (op) {
                    case Opcode.ICONST_0: lastInt = 0; break;
                    case Opcode.ICONST_1: lastInt = 1; break;
                    case Opcode.ICONST_2: lastInt = 2; break;
                    case Opcode.ICONST_3: lastInt = 3; break;
                    case Opcode.ICONST_4: lastInt = 4; break;
                    case Opcode.ICONST_5: lastInt = 5; break;
                    case Opcode.BIPUSH: lastInt = (byte) it.byteAt(pos + 1); break;
                    case Opcode.SIPUSH: lastInt = it.s16bitAt(pos + 1); break;
                    case Opcode.IF_ICMPLT:
                    case Opcode.IF_ICMPLE:
                    case Opcode.IF_ICMPGE:
                    case Opcode.IF_ICMPGT:
                    case Opcode.IF_ICMPEQ:
                    case Opcode.IF_ICMPNE:
                        if (lastInt >= 0 && lastInt <= 5) currentPower = lastInt;
                        break;
                    case Opcode.LDC: {
                        int idx = it.byteAt(pos + 1);
                        captureString(cp, idx, currentPower);
                        break;
                    }
                    case Opcode.LDC_W: {
                        int idx = it.u16bitAt(pos + 1);
                        captureString(cp, idx, currentPower);
                        break;
                    }
                    default:
                        break;
                }
            }
            vanillaPopulated = true;
            int vanillaCount = (int) ENTRIES.stream().filter(e -> !e.framework).count();
            LOGGER.info("[GmHelpIndex] Indexed " + vanillaCount + " vanilla help lines.");
        } catch (NotFoundException | BadBytecode e) {
            LOGGER.log(Level.WARNING,
                "[GmHelpIndex] Failed to scan sendDeityHelp; #help <query> will only see framework entries.",
                e);
        }
    }

    private static void captureString(ConstPool cp, int idx, int currentPower) {
        if (cp.getTag(idx) != ConstPool.CONST_String) return;
        String s = cp.getStringInfo(idx);
        if (s == null || s.isEmpty()) return;
        // Skip the header line and short strings that aren't help text.
        if (s.length() < 4) return;
        if ("Current available commands:".equals(s)) return;
        ENTRIES.add(new Entry(s, currentPower, false));
    }

    /**
     * Substring (case-insensitive) over the help line, gated by caller power.
     * Returns entries the caller can actually use, vanilla then framework.
     */
    public static List<Entry> search(String query, int callerPower) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return Collections.emptyList();
        List<Entry> hits = new ArrayList<>();
        synchronized (ENTRIES) {
            for (Entry e : ENTRIES) {
                if (e.minPower > callerPower) continue;
                if (e.line.toLowerCase(Locale.ROOT).contains(q)) hits.add(e);
            }
        }
        // Order: vanilla (by power asc) then framework
        hits.sort((a, b) -> {
            if (a.framework != b.framework) return a.framework ? 1 : -1;
            return Integer.compare(a.minPower, b.minPower);
        });
        return hits;
    }

    public static int size() { return ENTRIES.size(); }
}

package com.garward.wurmmodloader.core.menu;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stable {@code short} menu ids for Create-menu leaf {@code ActionEntry}s.
 *
 * <p>Vanilla encodes leaves as {@code (short)(10000 + templateId)}, which
 * overflows into the negative range when {@code templateId > 22767} — the
 * client then mistakes the overflowed short for a submenu header and
 * nests every subsequent sibling entry underneath it. This registry gives
 * overflow-prone template ids a safe positive short and keeps the low-id
 * vanilla encoding untouched for backwards compatibility.</p>
 *
 * <p>Short space (positive half): the wire format reserves {@code [10001, Short.MAX_VALUE]}
 * for Create-menu leaves. Vanilla uses {@code [10001, 11438]} directly;
 * we carve the high end ({@link #REMAP_BASE}..{@link Short#MAX_VALUE}) for
 * remapped ids. Collision is impossible because vanilla's direct encoding
 * never exceeds {@code 10000 + 22767 = 32767}, and we only remap templates
 * with id {@code > 22767} — those would overflow, so by definition cannot
 * share the short space with a legitimate low-id encoding.</p>
 *
 * <p>Stability: once a template id is seen, its assigned short is permanent
 * for the life of the server process. The mapping is not persisted across
 * restarts; the client rebuilds menus from scratch on every right-click, so
 * it never caches these ids between menu opens.</p>
 */
public final class CreationEntryMenuIds {

    private static final int DIRECT_ENCODE_MAX = 22767;
    private static final short REMAP_BASE = 30000;

    private static final ConcurrentHashMap<Integer, Short> templateIdToMenuId = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Short, Integer> menuIdToTemplateId = new ConcurrentHashMap<>();
    private static short nextRemapId = REMAP_BASE;

    private CreationEntryMenuIds() {}

    public static short encode(int templateId) {
        if (templateId <= DIRECT_ENCODE_MAX) {
            return (short) (10000 + templateId);
        }
        Short cached = templateIdToMenuId.get(templateId);
        if (cached != null) return cached;
        return allocate(templateId);
    }

    private static synchronized short allocate(int templateId) {
        Short cached = templateIdToMenuId.get(templateId);
        if (cached != null) return cached;
        if (nextRemapId == Short.MAX_VALUE) {
            throw new IllegalStateException(
                "CreationEntryMenuIds pool exhausted: " +
                (Short.MAX_VALUE - REMAP_BASE) + " remapped entries allocated");
        }
        short id = nextRemapId++;
        templateIdToMenuId.put(templateId, id);
        menuIdToTemplateId.put(id, templateId);
        return id;
    }

    public static int decode(int action) {
        if (action >= REMAP_BASE && action <= Short.MAX_VALUE) {
            Integer tid = menuIdToTemplateId.get((short) action);
            if (tid != null) return tid;
        }
        return action - 10000;
    }
}

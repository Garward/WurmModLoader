package com.garward.wurmmodloader.api.titles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Public mod-facing entry point for registering custom entries in the
 * vanilla {@code Titles.Title} enum. Mods queue {@link TitleDefinition}s
 * during their {@code preInit()} phase; the framework's title-injection
 * patch consumes the queue once during boot and writes the new entries
 * into {@code Titles.Title}'s static initializer.
 *
 * <p>Mirrors the surface of {@code net.bdew.wurm.tools.server.ModTitles}
 * so existing mod authors can port over without behaviour changes.</p>
 */
public final class TitleRegistry {

    private static final List<TitleDefinition> QUEUE = new ArrayList<>();
    private static volatile boolean drained;

    private TitleRegistry() {}

    /**
     * Queue a custom title for injection.
     *
     * @param id         numeric title id (must be unique across all mods).
     * @param maleName   display name shown to male characters.
     * @param femaleName display name shown to female characters.
     * @param skillId    skill number for skill-bound titles, or {@code -1} for general titles.
     * @param type       title tier — {@code "NORMAL"}, {@code "MINOR"}, {@code "MASTER"}, or {@code "LEGENDARY"}.
     */
    public static synchronized void addTitle(int id, String maleName, String femaleName, int skillId, String type) {
        if (drained) {
            throw new IllegalStateException(
                "TitleRegistry.addTitle(" + id + ", \"" + maleName + "\") called after the title-injection "
                    + "patch already ran. Register titles from preInit().");
        }
        QUEUE.add(new TitleDefinition(id, maleName, femaleName, skillId, type));
    }

    public static void addTitle(int id, String name, int skillId, String type) {
        addTitle(id, name, name, skillId, type);
    }

    public static void addTitle(int id, String maleName, String femaleName) {
        addTitle(id, maleName, femaleName, -1, "NORMAL");
    }

    public static void addTitle(int id, String name) {
        addTitle(id, name, name, -1, "NORMAL");
    }

    /** Framework-internal: snapshot the queue and mark it drained. */
    public static synchronized List<TitleDefinition> drain() {
        drained = true;
        return Collections.unmodifiableList(new ArrayList<>(QUEUE));
    }

    /** Framework-internal: whether {@link #drain()} has already been called. */
    public static boolean isDrained() {
        return drained;
    }
}

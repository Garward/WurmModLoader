package com.garward.wurmmodloader.core.icon;

import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.icon.IconType;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.registry.Registries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for registering and managing icons in the icon registry.
 *
 * <p>Two complementary APIs live here:
 * <ul>
 *   <li><strong>Legacy {@code Icon}-shaped API</strong> — {@link #registerCustom},
 *       {@link #registerVanilla}, {@link #registerPlaceholder}, {@link #register}.
 *       Used by {@code materialsystem} and the bundled {@code Iconzz} mod. Auto-allocates
 *       custom IDs starting at {@link Icon#FIRST_CUSTOM_ICON} (1680).</li>
 *   <li><strong>Dynamic-icon SPI</strong> — {@link #register(String, String)} +
 *       {@link #lookup(String)} + {@link #entries()}. Used by the icon scanner
 *       and the {@code IconRegistrySync} ModComm sender. Persists every allocation
 *       to {@link #PERSISTENCE_FILE} so existing items keep their image_id across
 *       restarts.</li>
 * </ul>
 *
 * <p><strong>Wire-protocol cap:</strong> custom IDs are stored as {@code short} on
 * the wire, so allocation hard-fails once the next ID would exceed
 * {@link Icon#MAX_CUSTOM_ICON} (32767). That gives ~30k mod-defined icons; if a
 * server hits the cap it has bigger problems than this registry.
 *
 * <p>Thread safety: all mutators are synchronized through the underlying
 * {@code Registries.ICONS} map plus a class-level monitor for the persistence
 * file. The {@link AtomicInteger} counter is monotonic — IDs are never reused,
 * even after a registration fails downstream.
 *
 * @since 1.0.0
 */
public final class IconRegistry {

    private static final Logger logger = Logger.getLogger(IconRegistry.class.getName());

    /**
     * Starting icon ID for custom mod icons.
     */
    private static final int CUSTOM_ICON_START = Icon.FIRST_CUSTOM_ICON;

    /**
     * Persistence file. Lives next to {@code IdAllocator}'s allocation file so
     * the framework's serializable state is co-located.
     */
    private static final Path PERSISTENCE_FILE =
        Paths.get("mods", "WurmModLoader", "icon_registry.json");

    /**
     * Atomic counter for allocating sequential custom icon IDs.
     */
    private static final AtomicInteger nextCustomIconId = new AtomicInteger(CUSTOM_ICON_START);

    /**
     * Dynamic-SPI table: stringId → allocated short.
     * Keyed by the canonical {@code namespace:path} string a mod hands in.
     */
    private static final Map<String, Short> dynamicIds = new LinkedHashMap<>();

    /**
     * Dynamic-SPI table: allocated short → packUri (e.g.
     * {@code pack:framework-icons/mastercraft/eternal_orb.png}).
     */
    private static final Map<Short, String> dynamicUris = new LinkedHashMap<>();

    private static volatile boolean persistenceLoaded = false;

    /**
     * Private constructor to prevent instantiation.
     */
    private IconRegistry() {
        throw new AssertionError("IconRegistry should not be instantiated");
    }

    /**
     * Registers a vanilla icon reference.
     *
     * @param id the resource location identifier (e.g., "wurm:armor_plate_chest")
     * @param vanillaIconId the vanilla icon ID (must be 0-1679)
     * @return the registered Icon
     * @since 1.0.0
     */
    public static Icon registerVanilla(ResourceLocation id, int vanillaIconId) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        if (vanillaIconId < 0 || vanillaIconId > Icon.MAX_VANILLA_ICON) {
            throw new IllegalArgumentException("Vanilla icon ID must be 0-1679, got: " + vanillaIconId);
        }

        Icon icon = new Icon(id, vanillaIconId, "vanilla", IconType.VANILLA);
        Registries.ICONS.register(id, icon);
        logger.info("Registered vanilla icon: " + id + " → iconId=" + vanillaIconId);
        return icon;
    }

    /**
     * Registers a custom mod icon with automatic ID allocation.
     *
     * <p>Idempotent for repeat callers with the same {@code id}: returns the
     * existing {@link Icon} rather than allocating a new ID. This keeps DB-bound
     * items stable across mod reloads.
     *
     * @param id the resource location identifier
     * @param packFile the icon pack file name (e.g., "mymod_icons.png")
     * @return the registered Icon (existing or newly allocated)
     * @since 1.0.0
     */
    public static Icon registerCustom(ResourceLocation id, String packFile) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        if (packFile == null) {
            throw new IllegalArgumentException("Pack file cannot be null");
        }

        loadPersistenceIfNeeded();

        synchronized (IconRegistry.class) {
            // If this stringId already has a dynamic short, reuse it so the
            // legacy API and the dynamic SPI agree on identity.
            String stringId = id.toString();
            Short existing = dynamicIds.get(stringId);
            if (existing != null) {
                Optional<Icon> hit = Registries.ICONS.get(id);
                if (hit.isPresent()) {
                    return hit.get();
                }
                Icon icon = new Icon(id, existing & 0xFFFF, packFile, IconType.CUSTOM);
                Registries.ICONS.register(id, icon);
                return icon;
            }

            int allocated = allocateNextLocked();
            Icon icon = new Icon(id, allocated, packFile, IconType.CUSTOM);
            Registries.ICONS.register(id, icon);

            String packUri = "pack:framework-icons/" + id.getNamespace() + "/" + packFile;
            dynamicIds.put(stringId, (short) allocated);
            dynamicUris.put((short) allocated, packUri);
            persistLocked();

            logger.info("Registered custom icon: " + id + " → iconId=" + allocated +
                       ", sheet=" + icon.getSheetNumber() + ", packFile=" + packFile);
            return icon;
        }
    }

    /**
     * Registers a placeholder/fallback icon.
     *
     * @param id the resource location identifier
     * @return the registered placeholder Icon
     * @since 1.0.0
     */
    public static Icon registerPlaceholder(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }

        Icon icon = new Icon(id, Icon.FALLBACK_ICON_ID, "vanilla", IconType.PLACEHOLDER);
        Registries.ICONS.register(id, icon);
        logger.info("Registered placeholder icon: " + id + " → iconId=" + Icon.FALLBACK_ICON_ID);
        return icon;
    }

    /**
     * Registers a custom icon with an explicit icon ID.
     *
     * @param id the resource location identifier
     * @param iconId the explicit icon ID to use (recommended: 1680+)
     * @param packFile the icon pack file name
     * @param type the icon type
     * @return the registered Icon
     * @since 1.0.0
     */
    public static Icon register(ResourceLocation id, int iconId, String packFile, IconType type) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        if (iconId < 0) {
            throw new IllegalArgumentException("Icon ID cannot be negative: " + iconId);
        }
        if (packFile == null) {
            throw new IllegalArgumentException("Pack file cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Icon type cannot be null");
        }

        Icon icon = new Icon(id, iconId, packFile, type);
        Registries.ICONS.register(id, icon);
        return icon;
    }

    // ---------------------------------------------------------------------
    // Dynamic-icon SPI
    // ---------------------------------------------------------------------

    /**
     * Registers a dynamic mod icon by string id and pack URI, returning the
     * allocated wire-protocol {@code short}.
     *
     * <p>Idempotent: a repeat call with the same {@code stringId} returns the
     * existing short and does not re-write persistence. The {@code packUri} on
     * a repeat call is ignored — the first registration wins so DB-bound items
     * keep their image_id across configuration drift.
     *
     * <p>Allocates monotonically from {@link Icon#FIRST_CUSTOM_ICON}; throws
     * {@link IllegalStateException} once the next ID would overflow
     * {@link Icon#MAX_CUSTOM_ICON}.
     *
     * @param stringId canonical mod-defined identifier, e.g. {@code "mastercraft:eternal_orb"}
     * @param packUri  resolvable URI for the icon image, e.g.
     *                 {@code "pack:framework-icons/mastercraft/eternal_orb.png"}
     * @return the allocated wire-protocol short (always in {@code [1680, 32767]})
     * @throws IllegalArgumentException if either argument is null/blank
     * @throws IllegalStateException    if the registry is full
     * @since 0.4.1
     */
    public static short register(String stringId, String packUri) {
        if (stringId == null || stringId.isEmpty()) {
            throw new IllegalArgumentException("stringId cannot be null/empty");
        }
        if (packUri == null || packUri.isEmpty()) {
            throw new IllegalArgumentException("packUri cannot be null/empty");
        }

        loadPersistenceIfNeeded();

        synchronized (IconRegistry.class) {
            Short existing = dynamicIds.get(stringId);
            if (existing != null) {
                return existing;
            }

            int allocated = allocateNextLocked();
            short s = (short) allocated;
            dynamicIds.put(stringId, s);
            dynamicUris.put(s, packUri);
            persistLocked();
            logger.info("Registered dynamic icon: " + stringId + " → short=" + (allocated & 0xFFFF) +
                       ", uri=" + packUri);
            return s;
        }
    }

    /**
     * Looks up the wire-protocol short for a registered string id.
     *
     * @param stringId the canonical identifier passed to {@link #register(String, String)}
     * @return the allocated short, or {@code -1} if not registered
     * @since 0.4.1
     */
    public static short lookup(String stringId) {
        if (stringId == null) {
            return -1;
        }
        loadPersistenceIfNeeded();
        synchronized (IconRegistry.class) {
            Short s = dynamicIds.get(stringId);
            return s == null ? (short) -1 : s;
        }
    }

    /**
     * Returns an immutable snapshot of all dynamic-SPI entries, keyed by
     * allocated short, value being the pack URI. Used by the ModComm sender
     * to ship the registry to clients on connect.
     *
     * @since 0.4.1
     */
    public static Map<Short, String> entries() {
        loadPersistenceIfNeeded();
        synchronized (IconRegistry.class) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(dynamicUris));
        }
    }

    // ---------------------------------------------------------------------
    // Existing query helpers
    // ---------------------------------------------------------------------

    public static Optional<Icon> get(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        return Registries.ICONS.get(id);
    }

    public static Icon getOrFallback(ResourceLocation id) {
        return get(id).orElse(createFallbackIcon(id));
    }

    public static boolean contains(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        return Registries.ICONS.containsKey(id);
    }

    public static int size() {
        return Registries.ICONS.size();
    }

    public static long countCustomIcons() {
        return Registries.ICONS.values()
                .filter(Icon::isCustom)
                .count();
    }

    public static long countVanillaIcons() {
        return Registries.ICONS.values()
                .filter(Icon::isVanilla)
                .count();
    }

    public static int getNextCustomIconId() {
        return nextCustomIconId.get();
    }

    public static boolean isFrozen() {
        return Registries.ICONS.isFrozen();
    }

    private static Icon createFallbackIcon(ResourceLocation id) {
        return new Icon(id, Icon.FALLBACK_ICON_ID, "vanilla", IconType.PLACEHOLDER);
    }

    // ---------------------------------------------------------------------
    // Allocation + persistence (caller holds IconRegistry.class monitor)
    // ---------------------------------------------------------------------

    private static int allocateNextLocked() {
        int allocated;
        while (true) {
            int current = nextCustomIconId.get();
            if (current > Icon.MAX_CUSTOM_ICON) {
                throw new IllegalStateException(
                    "Icon registry exhausted at id=" + current +
                    " (max=" + Icon.MAX_CUSTOM_ICON + "); cannot allocate further");
            }
            if (nextCustomIconId.compareAndSet(current, current + 1)) {
                allocated = current;
                break;
            }
        }
        return allocated;
    }

    private static void loadPersistenceIfNeeded() {
        if (persistenceLoaded) {
            return;
        }
        synchronized (IconRegistry.class) {
            if (persistenceLoaded) {
                return;
            }
            try {
                if (Files.exists(PERSISTENCE_FILE)) {
                    parseInto(new String(Files.readAllBytes(PERSISTENCE_FILE), StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                logger.log(Level.WARNING,
                    "Failed to load icon registry persistence from " + PERSISTENCE_FILE, e);
            }
            persistenceLoaded = true;
        }
    }

    private static void parseInto(String json) {
        // Minimal hand-rolled parse for the single-shape document we write.
        // Format: {"entries":[{"id":"<str>","short":<num>,"uri":"<str>"}, ...]}
        int max = CUSTOM_ICON_START;
        int idx = json.indexOf("\"entries\"");
        if (idx < 0) {
            return;
        }
        int from = json.indexOf('[', idx);
        int to = json.lastIndexOf(']');
        if (from < 0 || to <= from) {
            return;
        }
        String body = json.substring(from + 1, to);
        int cursor = 0;
        while (cursor < body.length()) {
            int objStart = body.indexOf('{', cursor);
            if (objStart < 0) break;
            int objEnd = body.indexOf('}', objStart);
            if (objEnd < 0) break;
            String entry = body.substring(objStart + 1, objEnd);

            String id = readString(entry, "\"id\"");
            String shortStr = readPrimitive(entry, "\"short\"");
            String uri = readString(entry, "\"uri\"");

            if (id != null && shortStr != null && uri != null) {
                try {
                    int s = Integer.parseInt(shortStr.trim());
                    if (s < CUSTOM_ICON_START || s > Icon.MAX_CUSTOM_ICON) {
                        logger.warning("Skipping persisted icon out of range: " + id + " short=" + s);
                    } else {
                        short asShort = (short) s;
                        dynamicIds.put(id, asShort);
                        dynamicUris.put(asShort, uri);
                        if (s + 1 > max) {
                            max = s + 1;
                        }
                    }
                } catch (NumberFormatException nfe) {
                    logger.warning("Malformed short in icon registry: " + shortStr);
                }
            }
            cursor = objEnd + 1;
        }
        final int hardMax = max;
        nextCustomIconId.updateAndGet(curr -> Math.max(curr, hardMax));
        logger.info("Loaded icon registry persistence: " + dynamicIds.size() + " entries, nextId=" +
                    nextCustomIconId.get());
    }

    private static String readString(String entry, String key) {
        int k = entry.indexOf(key);
        if (k < 0) return null;
        int colon = entry.indexOf(':', k + key.length());
        if (colon < 0) return null;
        int q1 = entry.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = entry.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return entry.substring(q1 + 1, q2);
    }

    private static String readPrimitive(String entry, String key) {
        int k = entry.indexOf(key);
        if (k < 0) return null;
        int colon = entry.indexOf(':', k + key.length());
        if (colon < 0) return null;
        int comma = entry.indexOf(',', colon + 1);
        int end = comma < 0 ? entry.length() : comma;
        return entry.substring(colon + 1, end).trim();
    }

    private static void persistLocked() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"entries\":[");
        boolean first = true;
        for (Map.Entry<String, Short> e : dynamicIds.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            String uri = dynamicUris.get(e.getValue());
            sb.append("{\"id\":\"").append(escape(e.getKey()))
              .append("\",\"short\":").append(e.getValue() & 0xFFFF)
              .append(",\"uri\":\"").append(escape(uri == null ? "" : uri)).append("\"}");
        }
        sb.append("]}");
        try {
            Path parent = PERSISTENCE_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(PERSISTENCE_FILE, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.log(Level.WARNING,
                "Failed to persist icon registry to " + PERSISTENCE_FILE, e);
        }
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}

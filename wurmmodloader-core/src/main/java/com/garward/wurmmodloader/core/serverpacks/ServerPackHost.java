package com.garward.wurmmodloader.core.serverpacks;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.api.events.ModQueryEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.serverpacks.ServerPackOptions;
import com.garward.wurmmodloader.api.serverpacks.ServerPacks;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.IChannelListener;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.garward.wurmmodloader.modcomm.PacketReader;
import com.garward.wurmmodloader.modcomm.PacketWriter;

import com.wurmonline.server.Players;
import com.wurmonline.server.players.Player;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Framework-owned server-pack host. Replaces the community
 * {@code mods/serverpacks/ServerPackMod}: same wire formats, same
 * ModComm channel names, same {@code serverpacks:add_pack} action, same
 * HTTP endpoint registration — but lives inside the framework, no
 * cross-classloader reflection needed when callers like
 * {@code IconPackServerPacksBridge} push packs.
 *
 * <p>Lifecycle: {@link #initialize()} runs from {@code ServerHook.fireOnServerStarted}
 * (next to the other framework channels). At that point ModComm is live; mod-driven
 * pack registrations may already have queued via {@link #addServerPack} calls
 * routed through the static façade. The host keeps the in-memory registry, so
 * registrations made before {@code initialize()} (i.e. before HTTP / channels exist)
 * are notified to players when they connect.
 *
 * <p>Implements the new {@code com.garward.wurmmodloader.api.serverpacks.ServerPacks}.
 * Legacy mods compiled against {@code com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks}
 * (e.g. WyvernMods' {@code ServerPackHandler}) reach this host through
 * {@link LegacyServerPacksAdapter}, which is what
 * {@code ServerPacks.getInstance()} on the legacy interface returns.
 */
public final class ServerPackHost implements ServerPacks {

    private static final Logger logger = Logger.getLogger(ServerPackHost.class.getName());

    /** Canonical ModComm channel. */
    public static final String CHANNEL = "com.garward.serverpacks";
    /** Legacy Ago-era channel name — kept as a read/write alias. */
    public static final String LEGACY_CHANNEL = "ago.serverpacks";

    private static final byte CMD_REFRESH = 0x01;

    private static final ServerPackHost INSTANCE = new ServerPackHost();

    private final Map<String, PackInfo> packs = new ConcurrentHashMap<>();

    private volatile Channel channel;
    private volatile Channel legacyChannel;
    private volatile String prefix;
    private volatile boolean initialized = false;

    private ServerPackHost() {}

    public static ServerPackHost getInstance() {
        return INSTANCE;
    }

    /**
     * One-shot framework bootstrap. Registers ModComm channels, subscribes to
     * {@code serverpacks:add_pack} ModActionEvents, and arranges HTTP endpoint
     * registration via {@link ServerStartedEvent}.
     *
     * <p>Idempotent — second call is a no-op.
     */
    public static synchronized void initialize() {
        if (INSTANCE.initialized) {
            return;
        }
        INSTANCE.initialized = true;
        INSTANCE.registerChannels();
        EventBus.getInstance().register(INSTANCE);
        logger.info("[ServerPacks] Framework host initialized — registered " + CHANNEL
                + (INSTANCE.legacyChannel != null ? " + " + LEGACY_CHANNEL + " (alias)" : " (no legacy alias)"));
    }

    private void registerChannels() {
        IChannelListener canonicalListener = new IChannelListener() {
            @Override
            public void onPlayerConnected(Player player) {
                announceIfHttpReady(player);
            }

            @Override
            public void handleMessage(Player player, ByteBuffer message) {
                dispatchCommand(player, message);
            }
        };
        channel = ModComm.registerChannel(CHANNEL, canonicalListener);

        // Legacy alias. Skip registration if another mod (e.g. upstream Ago
        // org.gotti.wurmunlimited.mods.serverpacks.ServerPackMod) already owns
        // the channel — let them be the single source of truth so we don't
        // double-announce or fight over the listener slot.
        if (ModComm.getChannel(LEGACY_CHANNEL) != null) {
            logger.info("[ServerPacks] " + LEGACY_CHANNEL
                    + " already registered by another mod; skipping legacy alias");
        } else {
            IChannelListener legacyListener = new IChannelListener() {
                @Override
                public void onPlayerConnected(Player player) {
                    Channel canonical = channel;
                    if (canonical != null && canonical.isActiveForPlayer(player)) {
                        return;
                    }
                    announceIfHttpReady(player);
                }

                @Override
                public void handleMessage(Player player, ByteBuffer message) {
                    dispatchCommand(player, message);
                }
            };
            legacyChannel = ModComm.registerChannel(LEGACY_CHANNEL, legacyListener);
        }
    }

    private void announceIfHttpReady(Player player) {
        ModQueryEvent query = new ModQueryEvent("httpserver:is_running");
        EventBus.getInstance().post(query);
        Boolean running = (Boolean) query.get("running");
        if (running == null || !running) {
            logger.log(Level.WARNING, "HTTP server did not start properly. No server packs will be delivered.");
            return;
        }
        notifyPlayer(player, packs);
    }

    private void dispatchCommand(Player player, ByteBuffer message) {
        try (PacketReader reader = new PacketReader(message)) {
            byte cmd = reader.readByte();
            switch (cmd) {
                case CMD_REFRESH:
                    ServerPackChannelListener.sendModelRefresh(player);
                    break;
                default:
                    logger.log(Level.WARNING, String.format("Unknown channel command 0x%02x", 128 + cmd));
                    break;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Bind the HTTP endpoint once {@link ServerStartedEvent} fires —
     * httpserver is itself a framework subsystem, but its endpoint registry
     * isn't ready until server start.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ModActionEvent registerEndpoint = new ModActionEvent("httpserver:register_endpoint");
        registerEndpoint.set("modName", "serverpacks");
        registerEndpoint.set("pattern", Pattern.compile("^/(?<path>[^/]*)$"));
        registerEndpoint.set("handler", (java.util.function.Function<String, InputStream>) this::servePack);
        EventBus.getInstance().post(registerEndpoint);

        this.prefix = registerEndpoint.getString("prefix");
        if (prefix == null) {
            logger.severe("[ServerPacks] Failed to register pack HTTP handler — packs cannot be served");
            return;
        }
        logger.info("[ServerPacks] Registered HTTP endpoint at: " + prefix);
    }

    /**
     * ModActionEvent bridge: external mods may publish packs by firing
     * {@code serverpacks:add_pack} with {@code name}, {@code data | path},
     * and optional {@code force}/{@code prepend} flags. Kept verbatim from
     * the community mod so existing third-party integrations keep working.
     */
    @SubscribeEvent
    public void onModAction(ModActionEvent event) {
        try {
            if (!"serverpacks:add_pack".equals(event.getEventType())) {
                return;
            }
            String name = event.getString("name");
            byte[] data = (byte[]) event.get("data");
            Path path = (Path) event.get("path");
            Boolean force = event.getBoolean("force");
            Boolean prepend = event.getBoolean("prepend");

            if (name == null) {
                logger.warning("serverpacks:add_pack requires 'name' parameter");
                return;
            }
            if (data == null && path == null) {
                logger.warning("serverpacks:add_pack requires either 'data' or 'path' parameter");
                return;
            }

            List<ServerPackOptions> optionsList = new ArrayList<>();
            if (Boolean.TRUE.equals(force)) optionsList.add(ServerPackOptions.FORCE);
            if (Boolean.TRUE.equals(prepend)) optionsList.add(ServerPackOptions.PREPEND);
            ServerPackOptions[] options = optionsList.toArray(new ServerPackOptions[0]);

            if (data != null) {
                addServerPack(name, data, options);
            } else {
                addServerPack(name, path, options);
            }
            event.setHandled(true);
            logger.info("[ServerPacks] Added pack via event API: " + name);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling serverpacks:add_pack", e);
        }
    }

    // ====== ServerPacks (new API) ======

    @Override
    public void addServerPack(Path path, ServerPackOptions... options) {
        try {
            String sha1 = sha1Hex(path);
            putPack(sha1, new PackInfo(path, options));
            logger.info("[ServerPacks] Added pack " + sha1 + " from " + path);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void addServerPack(byte[] data, ServerPackOptions... options) {
        try {
            String sha1 = sha1Hex(new ByteArrayInputStream(data));
            putPack(sha1, new PackInfo(data, options));
            logger.info("[ServerPacks] Added pack " + sha1 + " (" + data.length + " bytes)");
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void addServerPack(String name, byte[] data, ServerPackOptions... options) {
        checkPackName(name);
        putPack(name, new PackInfo(data, options));
        logger.info("[ServerPacks] Added pack " + name + " (" + data.length + " bytes)");
    }

    @Override
    public void addServerPack(String name, Path path, ServerPackOptions... options) {
        checkPackName(name);
        putPack(name, new PackInfo(path, options));
        logger.info("[ServerPacks] Added pack " + name + " from " + path);
    }

    // ====== Pack registry + announce ======

    private void putPack(String name, PackInfo info) {
        fillManifest(info);
        packs.put(name, info);
        notifyPlayers(Collections.singletonMap(name, info));
    }

    private static void checkPackName(String name) {
        for (char c : name.toCharArray()) {
            if (c == '.' || c == '/' || c == '%' || c == '?' || c == '#') {
                throw new IllegalArgumentException(name);
            }
        }
    }

    private Channel pickChannelFor(Player player) {
        if (channel != null && channel.isActiveForPlayer(player)) return channel;
        if (legacyChannel != null && legacyChannel.isActiveForPlayer(player)) {
            logger.log(Level.FINE, "[ServerPacks] player {0} on legacy ago.serverpacks channel — update client mod",
                player.getName());
            return legacyChannel;
        }
        return null;
    }

    private void notifyPlayer(Player player, Map<String, PackInfo> packsToAnnounce) {
        Channel out = pickChannelFor(player);
        if (out == null) return;
        if (prefix == null) return;
        try {
            ModQueryEvent uriQuery = new ModQueryEvent("httpserver:get_uri");
            EventBus.getInstance().post(uriQuery);
            URI baseUri = (URI) uriQuery.get("uri");
            if (baseUri == null) {
                logger.warning("HTTP server not running, cannot notify player of server packs");
                return;
            }
            URI uri = baseUri.resolve(prefix);
            final boolean canonical = (out == channel);
            try (PacketWriter writer = new PacketWriter()) {
                writer.writeInt(packsToAnnounce.size());
                for (Map.Entry<String, PackInfo> entry : packsToAnnounce.entrySet()) {
                    final String packId = entry.getKey();
                    final PackInfo info = entry.getValue();
                    final Set<String> options = new LinkedHashSet<>();
                    if (info.prepend) options.add("prepend");
                    if (info.force) options.add("force");
                    final String query = options.isEmpty()
                        ? ""
                        : options.stream().collect(Collectors.joining("&", "?", ""));
                    final URI packUri = uri.resolve(packId);
                    writer.writeUTF(packId);
                    writer.writeUTF(packUri.toString() + query);
                    if (canonical) {
                        writer.writeUTF(info.sha256 == null ? "" : info.sha256);
                        writer.writeLong(info.size);
                    }
                }
                out.sendMessage(player, writer.getBytes());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void notifyPlayers(Map<String, PackInfo> packsToAnnounce) {
        if (this.prefix == null) return;
        for (Player player : Players.getInstance().getPlayers()) {
            if (pickChannelFor(player) != null) {
                notifyPlayer(player, packsToAnnounce);
            }
        }
    }

    private InputStream servePack(String packid) {
        try {
            PackInfo info = packs.get(packid);
            if (info != null && info.data != null) {
                return new ByteArrayInputStream(info.data);
            }
            if (info != null && info.path != null) {
                return Files.newInputStream(info.path);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }

    // ====== Manifest hashing ======

    private static String sha1Hex(Path packPath) throws IOException, NoSuchAlgorithmException {
        try (InputStream is = Files.newInputStream(packPath)) {
            return sha1Hex(is);
        }
    }

    private static String sha1Hex(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            if (n > 0) md.update(buf, 0, n);
        }
        return javax.xml.bind.DatatypeConverter.printHexBinary(md.digest());
    }

    private static String sha256Hex(Path packPath) throws IOException, NoSuchAlgorithmException {
        try (InputStream is = Files.newInputStream(packPath)) {
            return sha256Hex(is);
        }
    }

    private static String sha256Hex(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            if (n > 0) md.update(buf, 0, n);
        }
        return javax.xml.bind.DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
    }

    private static void fillManifest(PackInfo info) {
        try {
            if (info.data != null) {
                info.sha256 = sha256Hex(new ByteArrayInputStream(info.data));
                info.size = info.data.length;
            } else if (info.path != null) {
                info.sha256 = sha256Hex(info.path);
                info.size = Files.size(info.path);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, "failed to hash pack for manifest", e);
        }
    }
}

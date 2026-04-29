package com.garward.wurmmodloader.modsupport.declarativeui;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.IChannelListener;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.garward.wurmmodloader.modcomm.PacketReader;
import com.garward.wurmmodloader.modcomm.PacketWriter;
import com.wurmonline.server.players.Player;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Built-in framework service that owns the {@code com.garward.ui} declarative
 * UI channel. Wired up by {@code DelegatedLauncher} during modloader bootstrap
 * — no separate mod jar required, and no opt-in needed by other mods.
 *
 * <p>Exposes a {@link ModActionEvent}-based API so server mods can push
 * windows to clients without taking a direct dependency:
 *
 * <ul>
 *   <li>{@code ui:open_window} — inputs: {@code player} (Player), {@code windowId},
 *       {@code title}, {@code width}, {@code height}, {@code tree} ({@link WidgetNode})</li>
 *   <li>{@code ui:update_bindings} — inputs: {@code player}, {@code windowId},
 *       {@code values} (Map&lt;String,String&gt;)</li>
 *   <li>{@code ui:close_window} — inputs: {@code player}, {@code windowId}</li>
 *   <li>{@code ui:show_window} / {@code ui:hide_window}</li>
 * </ul>
 *
 * <p>When the client sends a button action, the service re-posts it as a
 * {@code ui:action} ModActionEvent with {@code player}, {@code windowId},
 * {@code action}, {@code payload} for other server mods to consume.
 */
public final class DeclarativeUiService {

    public static final String CHANNEL = "com.garward.ui";

    private static final byte OP_MOUNT   = 0x01;
    private static final byte OP_UNMOUNT = 0x02;
    private static final byte OP_BIND    = 0x03;
    private static final byte OP_SHOW    = 0x05;
    private static final byte OP_HIDE    = 0x06;
    private static final byte OP_ACTION  = 0x10;

    private static final Logger logger = Logger.getLogger(DeclarativeUiService.class.getName());

    private Channel channel;

    /**
     * Registers the {@code com.garward.ui} ModComm channel and subscribes to
     * the {@code ui:*} {@link ModActionEvent}s on the global {@link EventBus}.
     * Idempotent — calling twice rebinds the channel listener and re-registers
     * the subscriber.
     */
    public void init() {
        // Reload-safe: drop any stale registration from a prior boot/instance
        // before re-registering with this instance's listener.
        ModComm.unregisterChannel(CHANNEL);
        channel = ModComm.registerChannel(CHANNEL, new IChannelListener() {
            @Override
            public void handleMessage(Player player, ByteBuffer message) {
                try (PacketReader reader = new PacketReader(message)) {
                    byte op = reader.readByte();
                    if (op != OP_ACTION) {
                        logger.warning("[DeclarativeUI] unexpected client op: 0x" + Integer.toHexString(op & 0xff));
                        return;
                    }
                    String windowId = reader.readUTF();
                    String action = reader.readUTF();
                    String payload = reader.readUTF();

                    ModActionEvent ev = new ModActionEvent("ui:action");
                    ev.set("player", player);
                    ev.set("windowId", windowId);
                    ev.set("action", action);
                    ev.set("payload", payload);
                    EventBus.getInstance().post(ev);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "[DeclarativeUI] failed to decode client packet", e);
                }
            }
        });
        EventBus.getInstance().register(this);
        logger.info("[DeclarativeUI] Registered channel: " + CHANNEL);
    }

    @SubscribeEvent
    public void onModAction(ModActionEvent event) {
        try {
            switch (event.getEventType()) {
                case "ui:open_window":     handleOpen(event); break;
                case "ui:update_bindings": handleBind(event); break;
                case "ui:close_window":    handleUnmount(event); break;
                case "ui:show_window":     handleToggle(event, OP_SHOW); break;
                case "ui:hide_window":     handleToggle(event, OP_HIDE); break;
                default: return;
            }
            event.setHandled(true);
        } catch (Exception e) {
            logger.log(Level.WARNING, "[DeclarativeUI] error handling " + event.getEventType(), e);
            event.setCancelled(true);
            event.setCancelReason(e.getMessage());
        }
    }

    private void handleOpen(ModActionEvent event) throws IOException {
        Player player = (Player) event.get("player");
        String windowId = event.getString("windowId");
        String title = event.getString("title");
        int width = event.getInt("width");
        int height = event.getInt("height");
        WidgetNode tree = (WidgetNode) event.get("tree");
        if (player == null || windowId == null || tree == null) {
            throw new IllegalArgumentException("open_window requires player, windowId, tree");
        }

        try (PacketWriter w = new PacketWriter()) {
            w.writeByte(OP_MOUNT);
            w.writeUTF(windowId);
            w.writeUTF(title == null ? "" : title);
            w.writeInt(width);
            w.writeInt(height);
            writeTree(w, tree);
            channel.sendMessage(player, w.getBytes());
        }
    }

    private void handleUnmount(ModActionEvent event) throws IOException {
        Player player = (Player) event.get("player");
        String windowId = event.getString("windowId");
        try (PacketWriter w = new PacketWriter()) {
            w.writeByte(OP_UNMOUNT);
            w.writeUTF(windowId);
            channel.sendMessage(player, w.getBytes());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleBind(ModActionEvent event) throws IOException {
        Player player = (Player) event.get("player");
        String windowId = event.getString("windowId");
        Map<String, String> values = (Map<String, String>) event.get("values");
        if (values == null) return;

        try (PacketWriter w = new PacketWriter()) {
            w.writeByte(OP_BIND);
            w.writeUTF(windowId);
            w.writeShort(values.size());
            for (Map.Entry<String, String> e : values.entrySet()) {
                w.writeUTF(e.getKey());
                w.writeUTF(e.getValue() == null ? "" : e.getValue());
            }
            channel.sendMessage(player, w.getBytes());
        }
    }

    private void handleToggle(ModActionEvent event, byte op) throws IOException {
        Player player = (Player) event.get("player");
        String windowId = event.getString("windowId");
        try (PacketWriter w = new PacketWriter()) {
            w.writeByte(op);
            w.writeUTF(windowId);
            channel.sendMessage(player, w.getBytes());
        }
    }

    private static void writeTree(PacketWriter w, WidgetNode node) throws IOException {
        w.writeUTF(node.type);
        w.writeShort(node.props.size());
        for (Map.Entry<String, String> e : node.props.entrySet()) {
            w.writeUTF(e.getKey());
            w.writeUTF(e.getValue() == null ? "" : e.getValue());
        }
        w.writeShort(node.children.size());
        for (WidgetNode child : node.children) {
            writeTree(w, child);
        }
    }
}

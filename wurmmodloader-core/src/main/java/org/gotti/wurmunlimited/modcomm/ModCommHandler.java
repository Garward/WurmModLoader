package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.server.players.Player;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.logging.Logger;

public class ModCommHandler {
    private static final Logger logger = Logger.getLogger(ModCommHandler.class.getName());

    public static void handlePacket(Player player, ByteBuffer msg) {
        logger.info("[Legacy ModCommHandler] DEBUG: handlePacket called for player " + player.getName());
        try {
            byte type = msg.get();
            logger.info("[Legacy ModCommHandler] DEBUG: Packet type: 0x" + String.format("%02X", type));

            switch (type) {
                case ModCommConstants.PACKET_MESSAGE:
                    logger.info("[Legacy ModCommHandler] DEBUG: PACKET_MESSAGE received");
                    handlePacketMessage(player, msg);
                    break;
                case ModCommConstants.PACKET_CHANNELS:
                    logger.info("[Legacy ModCommHandler] DEBUG: PACKET_CHANNELS received (client handshake)");
                    handlePacketChannels(player, msg);
                    break;
                default:
                    logger.info("[Legacy ModCommHandler] DEBUG: Unknown packet type");
                    ModComm.logWarning(String.format("Unknown packet from player %s (%d)", player, type));
            }
        } catch (Exception e) {
            logger.severe("[Legacy ModCommHandler] ERROR: Exception in handlePacket");
            e.printStackTrace();
            ModComm.logException(String.format("Error handling packet from player %s", player.getName()), e);
        }
    }

    private static void handlePacketChannels(Player player, ByteBuffer msg) throws IOException {
        PacketReader reader = new PacketReader(msg);
        HashSet<Channel> toActivate = new HashSet<>();

        byte version = reader.readByte();
        int n = reader.readInt();

        ModComm.logInfo(String.format("Received client handshake from %s, %d channels, protocol version %d", player.getName(), n, version));

        while (n-- > 0) {
            String channel = reader.readUTF();
            if (ModComm.channels.containsKey(channel))
                toActivate.add(ModComm.channels.get(channel));
        }

        ModComm.logInfo(String.format("Activating %d channels for player %s", toActivate.size(), player.getName()));

        ModComm.getPlayerConnection(player).activate(version, toActivate);

        try (PacketWriter writer = new PacketWriter()) {
            writer.writeByte(ModCommConstants.CMD_MODCOMM);
            writer.writeByte(ModCommConstants.PACKET_CHANNELS);
            writer.writeByte(ModCommConstants.PROTO_VERSION);
            writer.writeInt(toActivate.size());
            for (Channel channel : toActivate) {
                writer.writeInt(channel.id);
                writer.writeUTF(channel.name);
            }
            SocketConnection conn = player.getCommunicator().getConnection();
            ByteBuffer buff = conn.getBuffer();
            buff.put(writer.getBytes());
            conn.flush();
        }

        for (Channel channel : toActivate) {
            try {
                channel.listener.onPlayerConnected(player);
            } catch (Exception e) {
                ModComm.logException(String.format("Error in channel %s onPlayerConnected", channel.name), e);
            }
        }
    }

    private static void handlePacketMessage(Player player, ByteBuffer msg) {
        int id = msg.getInt();
        if (!ModComm.idMap.containsKey(id)) {
            ModComm.logWarning(String.format("Message on unregistered channel %d from player %s", id, player.getName()));
            return;
        }
        Channel ch = ModComm.idMap.get(id);
        if (!ch.isActiveForPlayer(player)) {
            ModComm.logWarning(String.format("Message on inactive channel %s from player %s", ch.name, player.getName()));
            return;
        }
        try {
            ch.listener.handleMessage(player, msg.slice());
        } catch (Exception e) {
            ModComm.logException(String.format("Error in channel handler %s for player %s", ch.name, player.getName()), e);
        }
    }
}

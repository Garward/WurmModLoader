package com.garward.wurmmodloader.modloader.interfaces;

import com.wurmonline.server.Message;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;

/**
 * Interface for handling channel messages in the Wurm mod loader system.
 *
 * <p>This listener provides hooks for intercepting and processing messages sent to various
 * communication channels including kingdom, village, and alliance channels. Implementations
 * can choose to process, modify, or block messages based on their content or context.</p>
 *
 * @since 1.0.0
 */
public interface ChannelMessageListener {

	/**
	 * Handles messages sent to kingdom channels.
	 *
	 * @param message the message being sent to the kingdom channel
	 * @return MessagePolicy indicating how to handle the message
	 * @since 1.0.0
	 */
	default MessagePolicy onKingdomMessage(Message message) {
		return MessagePolicy.PASS;
	}

	/**
	 * Handles messages sent to village channels.
	 *
	 * @param village the village the message is being sent to
	 * @param message the message being sent to the village channel
	 * @return MessagePolicy indicating how to handle the message
	 * @since 1.0.0
	 */
	default MessagePolicy onVillageMessage(Village village, Message message) {
		return MessagePolicy.PASS;
	}

	/**
	 * Handles messages sent to alliance channels.
	 *
	 * @param alliance the PvP alliance the message is being sent to
	 * @param message the message being sent to the alliance channel
	 * @return MessagePolicy indicating how to handle the message
	 * @since 1.0.0
	 */
	default MessagePolicy onAllianceMessage(PvPAlliance alliance, Message message) {
		return MessagePolicy.PASS;
	}

}

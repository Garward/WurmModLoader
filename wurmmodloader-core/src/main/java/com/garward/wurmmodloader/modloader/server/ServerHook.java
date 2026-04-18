package com.garward.wurmmodloader.modloader.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

// Use modern interfaces
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import com.garward.wurmmodloader.modloader.internal.interfaces.ModEntry;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import com.garward.wurmmodloader.api.events.base.EventPriority;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.creature.CombatDamageEvent;
import com.garward.wurmmodloader.api.events.creature.CreatureDeathEvent;
import com.garward.wurmmodloader.api.events.creature.CreatureSpawnEvent;
import com.garward.wurmmodloader.api.events.creature.CreatureDbSaveEvent;
import com.garward.wurmmodloader.api.events.creature.CreatureDbLoadEvent;
import com.garward.wurmmodloader.api.events.creature.MountEquipmentCheckEvent;
import com.garward.wurmmodloader.api.events.combat.shield.ShieldCheckEvent;
import com.garward.wurmmodloader.api.events.combat.shield.ShieldDamageEvent;
import com.garward.wurmmodloader.api.events.combat.CombatCriticalHitEvent;
import com.garward.wurmmodloader.api.events.combat.CombatDualWieldEvent;
import com.garward.wurmmodloader.api.events.combat.CombatSwingSpeedEvent;
import com.garward.wurmmodloader.api.events.combat.OpportunityAttackEvent;
import com.garward.wurmmodloader.api.events.combat.WeaponUseEvent;
import com.garward.wurmmodloader.api.events.item.ItemDropEvent;
import com.garward.wurmmodloader.api.events.item.ItemEnchantmentStringsEvent;
import com.garward.wurmmodloader.api.events.item.ItemExamineEvent;
import com.garward.wurmmodloader.api.events.item.ItemTemplatesCreatedEvent;
import com.garward.wurmmodloader.api.events.item.ItemTradeEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialBonusEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialDamageModifierEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialDecayModifierEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialImpBonusEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialRepairTimeEvent;
import com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent;
import com.garward.wurmmodloader.api.events.action.ActionTimeCalculationEvent;
import com.garward.wurmmodloader.api.events.action.ActionSpeedModifierEvent;
import com.garward.wurmmodloader.api.events.movement.MovementBroadcastEvent;
import com.garward.wurmmodloader.api.events.movement.PlayerMovementBroadcastEvent;
import com.garward.wurmmodloader.api.events.skill.SkillAdvanceEvent;
import com.garward.wurmmodloader.api.events.player.PlayerDeathEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent;
import com.garward.wurmmodloader.api.events.server.ServerPollEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.server.ServerStoppingEvent;
import com.garward.wurmmodloader.api.events.vehicle.VehicleMountEvent;
import com.garward.wurmmodloader.api.events.vehicle.VehicleSpeedCalculationEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.core.registry.SystemBootstrap;
import com.garward.wurmmodloader.core.legacy.LegacyListenerBridge;
import com.garward.wurmmodloader.core.testing.eventsim.EventTestCommandHandler;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.garward.wurmmodloader.modcomm.intra.ModIntraServer;
import com.garward.wurmmodloader.modloader.interfaces.ChannelMessageListener;
import com.garward.wurmmodloader.modloader.interfaces.ItemTemplatesCreatedListener;
import com.garward.wurmmodloader.modloader.interfaces.PlayerLoginListener;
import com.garward.wurmmodloader.modloader.interfaces.PlayerMessageListener;
import com.garward.wurmmodloader.modloader.interfaces.ServerPollListener;
import com.garward.wurmmodloader.modloader.interfaces.ServerShutdownListener;
import com.garward.wurmmodloader.modloader.interfaces.ServerStartedListener;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.Message;
import com.wurmonline.server.creatures.AttackAction;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.combat.Weapon;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;

/**
 * Central event dispatcher for WurmModLoader framework.
 *
 * <p><strong>Debug Logging:</strong></p>
 * <p>Enable verbose event debug logging by adding {@code -DeventDebug=true} to your server launch arguments.
 * This will log detailed information about:
 * <ul>
 *   <li>All events firing with key parameters (creature names, item IDs, damage values, etc.)</li>
 *   <li>Event results (cancelled, modified values)</li>
 *   <li>Helpful for verifying @SubscribeEvent handlers are working correctly</li>
 *   <li>Useful for debugging invisible mechanics (damage scaling, stat calculations, etc.)</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
public class ServerHook {

	private static final Logger logger = Logger.getLogger(ServerHook.class.getName());

	/**
	 * Enable verbose event debug logging.
	 * Set via system property: -DeventDebug=true
	 */
	private static final boolean DEBUG = Boolean.getBoolean("eventDebug");

	// Rate limiting for high-frequency events (to prevent log spam)
	private static final java.util.concurrent.ConcurrentHashMap<String, EventCounter> eventCounters =
		new java.util.concurrent.ConcurrentHashMap<>();
	private static final long RATE_LIMIT_WINDOW_MS = 30000; // 30 seconds

	private static class EventCounter {
		long count = 0;
		long windowStart = System.currentTimeMillis();

		synchronized boolean shouldLog() {
			long now = System.currentTimeMillis();
			if (now - windowStart >= RATE_LIMIT_WINDOW_MS) {
				// Log summary and reset
				if (count > 0) {
					return true; // Log the summary
				}
				count = 0;
				windowStart = now;
			}
			count++;
			return false;
		}

		synchronized long getCountAndReset() {
			long result = count;
			count = 0;
			windowStart = System.currentTimeMillis();
			return result;
		}
	}

	// Modern event system
	private final EventBus eventBus = EventBus.getInstance();
	private final LegacyListenerBridge legacyBridge = new LegacyListenerBridge(eventBus);

	// Event testing command handler
	private EventTestCommandHandler eventTestCommandHandler = null;

	// Legacy listener system (maintained for backward compatibility)
	Listeners<ServerStartedListener, Void> serverStarted = new Listeners<>(ServerStartedListener.class);
	Listeners<ServerShutdownListener, Void> serverShutdown = new Listeners<>(ServerShutdownListener.class);
	Listeners<ItemTemplatesCreatedListener, Void> itemTemplatesCreated = new Listeners<>(
			ItemTemplatesCreatedListener.class);
	Listeners<PlayerMessageListener, MessagePolicy> playerMessage = new Listeners<>(PlayerMessageListener.class);
	Listeners<PlayerLoginListener, Void> playerLogin = new Listeners<>(PlayerLoginListener.class);
	Listeners<ServerPollListener, Void> serverPoll = new Listeners<>(ServerPollListener.class);
	Listeners<ChannelMessageListener, MessagePolicy> channelMessage = new Listeners<>(ChannelMessageListener.class);

	List<Listeners<?, ?>> handlers = Arrays.asList(serverStarted, serverShutdown, itemTemplatesCreated, playerMessage,
			playerLogin, serverPoll, channelMessage);

	protected ServerHook() {
	}

	public void addMods(List<? extends ModEntry<WurmServerMod>> wurmMods) {
		wurmMods.forEach(entry -> {
			WurmServerMod mod = entry.getWurmMod();

			// Register with legacy listener system
			handlers.forEach(handler -> handler.add(mod));

			// Register with modern event system
			// First, register mod itself with EventBus (for @SubscribeEvent methods)
			eventBus.register(mod);

			// Then, register legacy listeners via bridge
			legacyBridge.registerLegacyListeners(mod);
		});
	}

	private String formatVersion(String name, String version) {
		return String.format("%s version: %s", name, version == null ? "unversioned" : version);
	}

	public void addVersionHandler(String modloaderVersion, String gameVersion,
			List<? extends ModEntry<WurmServerMod>> wurmMods) {
		playerMessage.add(new PlayerMessageListener() {

			@Override
			public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
				if (communicator.getPlayer().getPower() > MiscConstants.POWER_HERO && message != null
						&& message.startsWith("#versions")) {
					List<String> versions = new ArrayList<>();
					versions.add(formatVersion("game", gameVersion));
					versions.add(formatVersion("modloader", modloaderVersion));
					wurmMods.forEach(
							entry -> versions.add(formatVersion(entry.getName(), entry.getWurmMod().getVersion())));

					versions.forEach(version -> communicator.sendNormalServerMessage(version));
					return MessagePolicy.DISCARD;
				}
				return MessagePolicy.PASS;
			}

			@Override
			public boolean onPlayerMessage(Communicator communicator, String message) {
				// unused legacy
				return false;
			}
		});
	}

	public void fireOnServerStarted() {
		logger.info("[ServerHook] DEBUG: Bootstrapping runtime registries");
		SystemBootstrap.initializeAll();

		// DIRECT CALL like Ago's original - legacy ModComm is now in core module
		logger.info("[ServerHook] DEBUG: Calling legacy ModComm.serverStarted()");
		org.gotti.wurmunlimited.modcomm.ModComm.serverStarted();
		logger.info("[ServerHook] DEBUG: Successfully called legacy ModComm.serverStarted()");

		// Also call new package ModComm
		logger.info("[ServerHook] DEBUG: Calling new ModComm.serverStarted()");
		ModComm.serverStarted();
		ModIntraServer.serverStarted();

		// Initialize WML_SYNC channel for client-server prediction
		logger.info("[ServerHook] DEBUG: Initializing WML_SYNC channel");
		com.garward.wurmmodloader.sync.WMLSyncChannel.initialize();
		logger.info("[ServerHook] DEBUG: WML_SYNC channel initialized");

		// Initialize WML_CAPABILITIES channel for server mod detection
		logger.info("[ServerHook] DEBUG: Initializing WML_CAPABILITIES channel");
		com.garward.wurmmodloader.capabilities.WMLCapabilitiesChannel.initialize();
		logger.info("[ServerHook] DEBUG: WML_CAPABILITIES channel initialized");

		// Initialize capability system (Phase 5.5)
		logger.info("[ServerHook] DEBUG: Initializing CapabilityManager");
		com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance().initialize();
		logger.info("[ServerHook] DEBUG: CapabilityManager initialized");

		// Fire CapabilityRegistrationEvent (mods register their capabilities)
		logger.info("[ServerHook] DEBUG: Firing CapabilityRegistrationEvent");
		eventBus.post(new CapabilityRegistrationEvent());
		logger.info("[ServerHook] DEBUG: Capability registration complete");

		// Schedule delayed database sync (30 seconds after server start to ensure DB pool is ready)
		// Note: Memory config was already applied in syncServerConfigBeforeLoad()
		logger.info("[ServerHook] DEBUG: Scheduling delayed database sync in 30 seconds");
		scheduleDatabaseSync();

		// Fire legacy listeners
		logger.info("[ServerHook] DEBUG: Firing legacy serverStarted listeners");
		serverStarted.fire(listener -> listener.onServerStarted());

		// Post modern event
		logger.info("[ServerHook] DEBUG: Posting ServerStartedEvent");
		eventBus.post(new ServerStartedEvent());

		// Register event test command handler (/framework testEvents)
		try {
			String serverDir = System.getProperty("user.dir", ".");
			eventTestCommandHandler = new EventTestCommandHandler(serverDir);
			eventTestCommandHandler.register();
			logger.info("[ServerHook] EventTestCommandHandler registered - use '/framework testEvents' command");
		} catch (Exception e) {
			logger.warning("[ServerHook] Failed to register EventTestCommandHandler: " + e.getMessage());
		}

		// Event testing (if enabled)
		if (Boolean.getBoolean("wurmmodloader.test.events")) {
			String modeStr = System.getProperty("wurmmodloader.test.events.mode", "ON_SERVER_START");
			com.garward.wurmmodloader.core.event.EventTester.TriggerMode mode =
				com.garward.wurmmodloader.core.event.EventTester.parseTriggerMode(modeStr);

			if (mode == com.garward.wurmmodloader.core.event.EventTester.TriggerMode.ON_SERVER_START) {
				logger.info("[ServerHook] DEBUG: Running EventTester in ON_SERVER_START mode");
				com.garward.wurmmodloader.core.event.EventTester tester =
					new com.garward.wurmmodloader.core.event.EventTester(mode);
				tester.fireAllEvents();
			}
		}

		// Freeze game content registries AFTER all startup events have fired
		// This includes ItemTemplatesCreated, ServerStarted, etc.
		logger.info("[ServerHook] DEBUG: Freezing game content registries (ITEMS, CREATURES, ICONS, etc.)");
		com.garward.wurmmodloader.core.registry.Registries.freezeAll();
		logger.info("[ServerHook] DEBUG: Game content registries frozen");
	}

	public void fireOnServerShutdown() {
		// Fire legacy listeners
		serverShutdown.fire(listener -> listener.onServerShutdown());

		// Post modern event
		eventBus.post(new ServerStoppingEvent());
	}

	public void fireOnItemTemplatesCreated() {
		// Fire legacy listeners
		itemTemplatesCreated.fire(listener -> listener.onItemTemplatesCreated());

		// Post modern event
		eventBus.post(new ItemTemplatesCreatedEvent());

		// Generate icon packs after all icons registered
		com.garward.wurmmodloader.core.icon.IconPackServerHook.generateIconPacksOnStartup();
	}

	public boolean fireOnMessage(Communicator communicator, String message, String title) {
		// Fire modern PlayerMessageEvent to EventBus
		com.garward.wurmmodloader.api.events.player.PlayerMessageEvent event =
			new com.garward.wurmmodloader.api.events.player.PlayerMessageEvent(communicator, message, title);
		eventBus.post(event);

		// If modern event was cancelled, respect that
		if (event.isCancelled()) {
			return true; // Discard message
		}

		// Also fire legacy listeners for backward compatibility
		return playerMessage.fire(listener -> listener.onPlayerMessage(communicator, message, title),
				() -> MessagePolicy.PASS, MessagePolicy.ANY_DISCARDED)
				.orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}

	public void fireOnPlayerLogin(Player player) {
		logger.info("[ServerHook] DEBUG: fireOnPlayerLogin called for player " + player.getName());

		// DIRECT CALL like Ago's original - legacy ModComm is now in core module
		logger.info("[ServerHook] DEBUG: Calling legacy ModComm.playerConnected()");
		org.gotti.wurmunlimited.modcomm.ModComm.playerConnected(player);
		logger.info("[ServerHook] DEBUG: Successfully called legacy ModComm.playerConnected()");

		// Also call new package ModComm
		logger.info("[ServerHook] DEBUG: Calling new ModComm.playerConnected()");
		ModComm.playerConnected(player);

		logger.info("[ServerHook] DEBUG: Firing player login listeners");
		playerLogin.fire(listener -> listener.onPlayerLogin(player));

		// Send server capabilities to client
		logger.info("[ServerHook] DEBUG: Sending server capabilities to " + player.getName());
		com.garward.wurmmodloader.capabilities.WMLCapabilitiesChannel.sendCapabilitiesToPlayer(player);

		// Event testing (if enabled and mode is ON_PLAYER_LOGIN)
		if (Boolean.getBoolean("wurmmodloader.test.events")) {
			String modeStr = System.getProperty("wurmmodloader.test.events.mode", "ON_SERVER_START");
			com.garward.wurmmodloader.core.event.EventTester.TriggerMode mode =
				com.garward.wurmmodloader.core.event.EventTester.parseTriggerMode(modeStr);

			if (mode == com.garward.wurmmodloader.core.event.EventTester.TriggerMode.ON_PLAYER_LOGIN) {
				// Only fire once for first player
				String firedKey = "wurmmodloader.test.events.fired";
				if (!Boolean.getBoolean(firedKey)) {
					System.setProperty(firedKey, "true");
					logger.info("[ServerHook] DEBUG: Running EventTester in ON_PLAYER_LOGIN mode (first player: " + player.getName() + ")");
					com.garward.wurmmodloader.core.event.EventTester tester =
						new com.garward.wurmmodloader.core.event.EventTester(mode);
					tester.fireAllEvents();
				}
			}
		}
	}

	public void fireOnPlayerLogout(Player player) {
		playerLogin.fire(listener -> listener.onPlayerLogout(player));
	}

	public boolean firePlayerSkillLoss(com.wurmonline.server.creatures.Creature creature) {
		// Note: Mods can check for resurrection stone themselves if needed
		// Post modern event
		com.garward.wurmmodloader.api.events.player.PlayerSkillLossEvent event =
			new com.garward.wurmmodloader.api.events.player.PlayerSkillLossEvent(creature, false);
		eventBus.post(event);

		// Return cancellation status
		return event.isCancelled();
	}

	public void fireOnServerPoll() {
		// Fire legacy listeners
		serverPoll.fire(listener -> listener.onServerPoll());

		// Post modern event
		eventBus.post(new ServerPollEvent());
	}

	public boolean fireOnKingdomMessage(Message message) {
		return channelMessage.fire(listener -> listener.onKingdomMessage(message), () -> MessagePolicy.PASS,
				MessagePolicy.ANY_DISCARDED).orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}

	public boolean fireOnVillageMessage(Village village, Message message) {
		return channelMessage.fire(listener -> listener.onVillageMessage(village, message), () -> MessagePolicy.PASS,
				MessagePolicy.ANY_DISCARDED).orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}

	public boolean fireOnAllianceMessage(PvPAlliance alliance, Message message) {
		return channelMessage.fire(listener -> listener.onAllianceMessage(alliance, message), () -> MessagePolicy.PASS,
				MessagePolicy.ANY_DISCARDED).orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}

	// ========================================================================
	// Creature and Combat Events
	// ========================================================================

	public void fireCreatureDeath(com.wurmonline.server.creatures.Creature victim,
	                              com.wurmonline.server.creatures.Creature killer) {
		if (DEBUG) {
			logger.info(String.format("[Event] CreatureDeathEvent: victim=%s (player=%s), killer=%s (player=%s)",
				victim.getName(), victim.isPlayer(),
				killer != null ? killer.getName() : "null",
				killer != null ? killer.isPlayer() : "N/A"));
		}

		// Post modern event
		eventBus.post(new CreatureDeathEvent(victim, killer));

		if (DEBUG) {
			logger.info("[Event] CreatureDeathEvent: completed");
		}
	}

	public String fireCreatureExamine(com.wurmonline.server.creatures.Creature creature,
	                                  String examineText) {
		// Post modern event
		com.garward.wurmmodloader.api.events.creature.CreatureExamineEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureExamineEvent(creature, examineText);
		eventBus.post(event);

		// Return modified examine text
		return event.getExamineText();
	}

	public double fireCombatDamage(com.wurmonline.server.creatures.Creature attacker,
	                               com.wurmonline.server.creatures.Creature defender,
	                               double damage,
	                               byte woundType,
	                               int bodyPart) {
		if (DEBUG) {
			EventCounter counter = eventCounters.computeIfAbsent("CombatDamageEvent", k -> new EventCounter());
			if (counter.shouldLog()) {
				long count = counter.getCountAndReset();
				logger.info(String.format("[Event] CombatDamageEvent: fired %d times in last 30 seconds", count));
			}
		}

		// Post modern event
		CombatDamageEvent event =
			new CombatDamageEvent(attacker, defender, damage, woundType, bodyPart);
		eventBus.post(event);

		// Track damage for loot system (use modified damage from event)
		if (!event.isCancelled() && attacker != null && defender != null && event.getDamage() > 0) {
			ProxyServerHook.trackDamage(defender.getWurmId(), attacker.getWurmId(), event.getDamage());
		}

		// Return modified damage (or original if not changed)
		// If event was cancelled, return 0 damage
		return event.isCancelled() ? 0.0 : event.getDamage();
	}

	public void fireCreatureSpawn(com.wurmonline.server.creatures.Creature creature) {
		if (DEBUG) {
			logger.info(String.format("[Event] CreatureSpawnEvent: creature=%s (player=%s, template=%s)",
				creature.getName(), creature.isPlayer(),
				creature.getTemplate() != null ? creature.getTemplate().getName() : "null"));
		}

		// Post modern event
		eventBus.post(new CreatureSpawnEvent(creature));

		if (DEBUG) {
			logger.info("[Event] CreatureSpawnEvent: completed");
		}
	}

	public void fireCreaturePositionUpdated(com.wurmonline.server.creatures.Creature creature,
	                                         float x, float y, float z, float rot, long bridgeId) {
		if (DEBUG) {
			logger.info(String.format("[Event] CreaturePositionUpdatedEvent: creature=%s, pos=(%.2f, %.2f, %.2f), rot=%.2f, bridgeId=%d",
				creature.getName(), x, y, z, rot, bridgeId));
		}

		// Post modern event
		eventBus.post(new com.garward.wurmmodloader.api.events.creature.CreaturePositionUpdatedEvent(
			creature, x, y, z, rot, bridgeId));

		if (DEBUG) {
			logger.info("[Event] CreaturePositionUpdatedEvent: completed");
		}
	}

	public void fireCreatureDbSave(com.wurmonline.server.creatures.Creature creature) {
		if (DEBUG) {
			logger.info(String.format("[Event] CreatureDbSaveEvent: creature=%s (wurmId=%d)",
				creature.getName(), creature.getWurmId()));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.creature.CreatureDbSaveEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureDbSaveEvent(creature);
		eventBus.post(event);

		// Handle custom column additions and data saving
		com.garward.wurmmodloader.core.database.CreatureDatabaseManager.getInstance()
			.handleSaveEvent(event, creature.getWurmId());

		if (DEBUG) {
			logger.info("[Event] CreatureDbSaveEvent: completed");
		}
	}

	public void fireCreatureDbLoad(com.wurmonline.server.creatures.Creature creature,
	                                java.sql.ResultSet resultSet) {
		if (DEBUG) {
			logger.info(String.format("[Event] CreatureDbLoadEvent: creature=%s (wurmId=%d)",
				creature.getName(), creature.getWurmId()));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.creature.CreatureDbLoadEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureDbLoadEvent(creature, resultSet);
		eventBus.post(event);

		if (DEBUG) {
			logger.info("[Event] CreatureDbLoadEvent: completed");
		}
	}

	public void fireDeityDbSave(int deityId, String deityName) {
		if (DEBUG) {
			logger.info(String.format("[Event] DeityDbSaveEvent: deity=%s (id=%d)",
				deityName, deityId));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.deity.DeityDbSaveEvent event =
			new com.garward.wurmmodloader.api.events.deity.DeityDbSaveEvent(deityId, deityName);
		eventBus.post(event);

		// Handle custom column additions and data saving
		com.garward.wurmmodloader.core.database.DeityDatabaseManager.getInstance()
			.handleSaveEvent(event, deityId);

		if (DEBUG) {
			logger.info("[Event] DeityDbSaveEvent: completed");
		}
	}

	public void fireDeityDbLoad(int deityId, String deityName, java.sql.ResultSet resultSet) {
		if (DEBUG) {
			logger.info(String.format("[Event] DeityDbLoadEvent: deity=%s (id=%d)",
				deityName, deityId));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.deity.DeityDbLoadEvent event =
			new com.garward.wurmmodloader.api.events.deity.DeityDbLoadEvent(deityId, deityName, resultSet);
		eventBus.post(event);

		if (DEBUG) {
			logger.info("[Event] DeityDbLoadEvent: completed");
		}
	}

	public void fireStructureDbSave(long structureId, String structureName) {
		if (DEBUG) {
			logger.info(String.format("[Event] StructureDbSaveEvent: structure=%s (id=%d)",
				structureName, structureId));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.structure.StructureDbSaveEvent event =
			new com.garward.wurmmodloader.api.events.structure.StructureDbSaveEvent(structureId, structureName);
		eventBus.post(event);

		// Handle custom column additions and data saving
		com.garward.wurmmodloader.core.database.StructureDatabaseManager.getInstance()
			.handleSaveEvent(event, structureId);

		if (DEBUG) {
			logger.info("[Event] StructureDbSaveEvent: completed");
		}
	}

	public void fireStructureDbLoad(long structureId, String structureName, java.sql.ResultSet resultSet) {
		if (DEBUG) {
			logger.info(String.format("[Event] StructureDbLoadEvent: structure=%s (id=%d)",
				structureName, structureId));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.structure.StructureDbLoadEvent event =
			new com.garward.wurmmodloader.api.events.structure.StructureDbLoadEvent(structureId, structureName, resultSet);
		eventBus.post(event);

		if (DEBUG) {
			logger.info("[Event] StructureDbLoadEvent: completed");
		}
	}


    public void fireMovementBroadcast(
            Communicator communicator,
            long creatureId,
            float x,
            float y,
            int rotation,
            boolean moving
    ) {
        Creature watcher = null;
        try {
            // Communicator usually has a getPlayer() / getCreature() accessor.
            // If decompiled name differs, Claude can swap this to the exact method.
            watcher = communicator.getPlayer();
        } catch (Throwable ignored) {
            // Safe: watcher stays null if we can't resolve it.
        }

        if (DEBUG) {
            logger.info(String.format(
                    "[Event] MovementBroadcast: watcher=%s, creatureId=%d, x=%.2f, y=%.2f, rot=%d, moving=%s",
                    watcher != null ? watcher.getName() : "null",
                    creatureId,
                    x,
                    y,
                    rotation,
                    moving
            ));
        }

        eventBus.post(new MovementBroadcastEvent(
                watcher,
                creatureId,
                x,
                y,
                rotation,
                moving
        ));

        if (DEBUG) {
            logger.info("[Event] MovementBroadcast: completed");
        }
    }

        public void firePlayerMovementBroadcast(
            Communicator communicator,
            float x,
            float y,
            float z,
            float rotation,
            boolean moving
    ) {
        Creature player = null;
        try {
            // adjust this accessor name to whatever the decompile shows
            player = communicator.getPlayer();
        } catch (Throwable ignored) {
            // leave player as null if we can't resolve it
        }

        if (DEBUG) {
            logger.info(String.format(
                    "[Event] PlayerMovementBroadcast: player=%s, x=%.2f, y=%.2f, z=%.2f, rot=%.2f, moving=%s",
                    player != null ? player.getName() : "null",
                    x, y, z, rotation, moving
            ));
        }

        eventBus.post(new PlayerMovementBroadcastEvent(
                player,
                x,
                y,
                z,
                rotation,
                moving
        ));

        if (DEBUG) {
            logger.info("[Event] PlayerMovementBroadcast: completed");
        }
    }


	public boolean fireCreatureBreed(com.wurmonline.server.creatures.Creature performer,
	                                  com.wurmonline.server.creatures.Creature target,
	                                  short breedType,
	                                  com.wurmonline.server.behaviours.Action action,
	                                  float counter) {
		if (DEBUG) {
			logger.info(String.format("[Event] CreatureBreedEvent: performer=%s, target=%s, breedType=%d, counter=%.2f",
				performer.getName(), target.getName(), breedType, counter));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.creature.CreatureBreedEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureBreedEvent(
				performer, target, breedType, action, counter);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] CreatureBreedEvent: cancelled=%s", event.isCancelled()));
		}

		return event.isCancelled();
	}

	public boolean fireActionFatigue(com.wurmonline.server.creatures.Creature performer,
	                                  long subject,
	                                  long target,
	                                  short action,
	                                  boolean defaultFatigue) {
		if (DEBUG) {
			logger.info(String.format("[Event] ActionFatigueEvent: performer=%s, action=%d, defaultFatigue=%s",
				performer.getName(), action, defaultFatigue));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.action.ActionFatigueEvent event =
			new com.garward.wurmmodloader.api.events.action.ActionFatigueEvent(
				performer, subject, target, action, defaultFatigue);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] ActionFatigueEvent: finalFatigue=%s", event.isFatigue()));
		}

		return event.isFatigue();
	}

	public int fireCropHarvest(com.wurmonline.server.creatures.Creature performer,
	                            int tilex,
	                            int tiley,
	                            boolean onSurface,
	                            int tile,
	                            float counter,
	                            com.wurmonline.server.items.Item tool,
	                            int quantity) {
		if (DEBUG) {
			logger.info(String.format("[Event] CropHarvestEvent: performer=%s, tile=(%d,%d), quantity=%d",
				performer.getName(), tilex, tiley, quantity));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.farming.CropHarvestEvent event =
			new com.garward.wurmmodloader.api.events.farming.CropHarvestEvent(
				performer, tilex, tiley, onSurface, tile, counter, tool, quantity);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] CropHarvestEvent: finalQuantity=%d", event.getQuantity()));
		}

		return event.getQuantity();
	}

	public boolean fireCropGrowth(int tilex, int tiley, int tile, byte data, byte farmData) {
		if (DEBUG) {
			int cropAge = (data >> 4) & 0x7;
			logger.info(String.format("[Event] CropGrowthEvent: tile=(%d,%d), cropAge=%d", tilex, tiley, cropAge));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.farming.CropGrowthEvent event =
			new com.garward.wurmmodloader.api.events.farming.CropGrowthEvent(
				tilex, tiley, tile, data, farmData);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] CropGrowthEvent: cancelled=%s", event.isCancelled()));
		}

		return event.isCancelled();
	}

	public boolean firePriestRestrictionCheck(com.wurmonline.server.creatures.Creature creature,
	                                           String context,
	                                           boolean defaultIsPriest) {
		if (DEBUG) {
			logger.info(String.format("[Event] PriestRestrictionCheckEvent: creature=%s, context=%s, defaultIsPriest=%s",
				creature.getName(), context, defaultIsPriest));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.player.PriestRestrictionCheckEvent event =
			new com.garward.wurmmodloader.api.events.player.PriestRestrictionCheckEvent(
				creature, context, defaultIsPriest);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] PriestRestrictionCheckEvent: finalIsPriest=%s", event.isPriest()));
		}

		return event.isPriest();
	}

	public Object[] firePrayerFaith(long playerId, byte numFaith, long lastFaith) {
		if (DEBUG) {
			logger.info(String.format("[Event] PrayerFaithEvent: playerId=%d, numFaith=%d, lastFaith=%d",
				playerId, numFaith, lastFaith));
		}

		// Post modern event
		com.garward.wurmmodloader.api.events.player.PrayerFaithEvent event =
			new com.garward.wurmmodloader.api.events.player.PrayerFaithEvent(
				playerId, numFaith, lastFaith);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] PrayerFaithEvent: finalNumFaith=%d, finalLastFaith=%d",
				event.getNumFaith(), event.getLastFaith()));
		}

		return new Object[] { event.getNumFaith(), event.getLastFaith() };
	}

	public CombatAttackResult fireCombatAttack(com.wurmonline.server.creatures.Creature attacker,
	                                           com.wurmonline.server.creatures.Creature defender,
	                                           int combatCounter,
	                                           boolean opportunity,
	                                           float actionCounter,
	                                           com.wurmonline.server.behaviours.Action action) {
		// Post modern event
		com.garward.wurmmodloader.api.events.combat.CombatAttackEvent event =
			new com.garward.wurmmodloader.api.events.combat.CombatAttackEvent(
				attacker, defender, combatCounter, opportunity, actionCounter, action);
		eventBus.post(event);

		// Return result with cancellation status
		return new CombatAttackResult(event.isCancelled(), event.getResult());
	}

	/**
	 * Result holder for CombatAttackEvent.
	 * Used to communicate both cancellation status and attack result back to bytecode.
	 */
	public static class CombatAttackResult {
		public final boolean cancelled;
		public final boolean attackResult;

		public CombatAttackResult(boolean cancelled, boolean attackResult) {
			this.cancelled = cancelled;
			this.attackResult = attackResult;
		}
	}

	public boolean fireSpecialMoveSend(com.wurmonline.server.creatures.Creature creature) {
		// Post modern event
		com.garward.wurmmodloader.api.events.combat.SpecialMoveSendEvent event =
			new com.garward.wurmmodloader.api.events.combat.SpecialMoveSendEvent(creature);
		eventBus.post(event);

		// Return cancellation status
		return event.isCancelled();
	}

	public SpecialMoveResult fireSpecialMoveHandle(com.wurmonline.server.creatures.Creature performer,
	                                                com.wurmonline.server.creatures.Creature target,
	                                                short action,
	                                                float counter) {
		// Post modern event
		com.garward.wurmmodloader.api.events.combat.SpecialMoveHandleEvent event =
			new com.garward.wurmmodloader.api.events.combat.SpecialMoveHandleEvent(
				performer, target, action, counter);
		eventBus.post(event);

		// Return result with cancellation status
		return new SpecialMoveResult(event.isCancelled(), event.getResult());
	}

	/**
	 * Result holder for SpecialMoveHandleEvent.
	 * Used to communicate both cancellation status and handler result back to bytecode.
	 */
	public static class SpecialMoveResult {
		public final boolean cancelled;
		public final boolean handlerResult;

		public SpecialMoveResult(boolean cancelled, boolean handlerResult) {
			this.cancelled = cancelled;
			this.handlerResult = handlerResult;
		}
	}

	public ShieldCheckEvent fireShieldCheck(com.wurmonline.server.creatures.Creature attacker,
	                                        com.wurmonline.server.creatures.Creature defender,
	                                        com.wurmonline.server.items.Item weapon,
	                                        com.wurmonline.server.items.Item shield) {
		ShieldCheckEvent event = new ShieldCheckEvent(defender, attacker, weapon, shield);
		eventBus.post(event);
		return event;
	}

	public double fireShieldDamage(com.wurmonline.server.creatures.Creature defender,
	                               com.wurmonline.server.creatures.Creature attacker,
	                               com.wurmonline.server.items.Item shield,
	                               double damage) {
		ShieldDamageEvent event = new ShieldDamageEvent(defender, attacker, shield, damage);
		eventBus.post(event);
		return event.isCancelled() ? Double.NaN : event.getDamage();
	}

	public double fireWeaponStat(Weapon weapon, Item item, byte material, WeaponStatQueryEvent.StatType type, double baseValue) {
		WeaponStatQueryEvent event = new WeaponStatQueryEvent(weapon, item, material, type, baseValue);
		eventBus.post(event);
		return event.getValue();
	}

	public float fireCombatCriticalHitChance(com.wurmonline.server.creatures.Creature attacker,
	                                         com.wurmonline.server.creatures.Creature defender,
	                                         com.wurmonline.server.items.Item weapon,
	                                         AttackAction attackAction,
	                                         float baseChance,
	                                         boolean usingNewCombatSystem) {
		CombatCriticalHitEvent event = new CombatCriticalHitEvent(attacker, defender, weapon, attackAction,
			usingNewCombatSystem, baseChance);
		eventBus.post(event);
		return event.getCritChance();
	}

	public OpportunityAttackEvent fireOpportunityAttack(com.wurmonline.server.creatures.Creature defender,
	                                                    com.wurmonline.server.creatures.Creature trespasser,
	                                                    double skillResult,
	                                                    double difficulty,
	                                                    byte opportunityCounter,
	                                                    int usedOpportunityAttacks,
	                                                    int combatCounter,
	                                                    float actionCounter) {
		OpportunityAttackEvent event = new OpportunityAttackEvent(defender, trespasser, skillResult, difficulty,
			opportunityCounter, usedOpportunityAttacks, combatCounter, actionCounter);
		eventBus.post(event);
		return event;
	}

	public float fireMaterialDamageModifier(Item item, byte material, float baseModifier) {
		MaterialDamageModifierEvent event = new MaterialDamageModifierEvent(item, material, baseModifier);
		eventBus.post(event);
		return (float) event.getModifier();
	}

	public float fireMaterialDecayModifier(Item item, byte material, float baseModifier) {
		MaterialDecayModifierEvent event = new MaterialDecayModifierEvent(item, material, baseModifier);
		eventBus.post(event);
		return (float) event.getModifier();
	}

	public float fireMaterialImpBonus(Item item, byte material, float baseBonus) {
		MaterialImpBonusEvent event = new MaterialImpBonusEvent(item, material, baseBonus);
		eventBus.post(event);
		return (float) event.getBonus();
	}

	public float fireMaterialRepairTime(Item item, byte material, float baseModifier) {
		MaterialRepairTimeEvent event = new MaterialRepairTimeEvent(item, material, baseModifier);
		eventBus.post(event);
		return event.getTimeModifier();
	}

	public double fireMaterialBonus(MaterialBonusEvent.BonusType type, Object context, byte material, double baseBonus) {
		MaterialBonusEvent event = new MaterialBonusEvent(context, material, type, baseBonus);
		eventBus.post(event);
		return event.getBonus();
	}

	public float fireActionTime(com.wurmonline.server.creatures.Creature performer,
	                            com.wurmonline.server.items.Item source,
	                            com.wurmonline.server.items.Item target,
	                            float baseTime) {
		ActionTimeCalculationEvent event = new ActionTimeCalculationEvent(performer, source, target, baseTime);
		eventBus.post(event);
		return event.getTime();
	}

	public float fireActionSpeed(com.wurmonline.server.creatures.Creature performer,
	                             int staminaNeeded,
	                             float baseModifier) {
		ActionSpeedModifierEvent event = new ActionSpeedModifierEvent(performer, null, baseModifier);
		eventBus.post(event);
		return event.getModifier();
	}

    public SkillAdvanceEvent fireSkillAdvance(Skill skill, Item item, double difficulty, double bonus) {
        if (DEBUG) {
            EventCounter counter = eventCounters.computeIfAbsent("SkillAdvanceEvent", k -> new EventCounter());
            if (counter.shouldLog()) {
                long count = counter.getCountAndReset();
                logger.info(String.format("[Event] SkillAdvanceEvent: fired %d times in last 30 seconds", count));
            }
        }

        SkillAdvanceEvent event = new SkillAdvanceEvent(skill, item, difficulty, bonus);
        eventBus.post(event);

        return event;
    }

	public float fireCombatSwingSpeed(com.wurmonline.server.creatures.Creature attacker,
	                                  com.wurmonline.server.items.Item weapon,
	                                  float baseSpeed) {
		CombatSwingSpeedEvent event = new CombatSwingSpeedEvent(attacker, weapon, baseSpeed);
		eventBus.post(event);
		return event.getSwingSpeed();
	}

	public CombatDualWieldEvent fireCombatDualWield(com.wurmonline.server.creatures.Creature attacker,
	                                               com.wurmonline.server.creatures.Creature defender,
	                                               com.wurmonline.server.items.Item offhand,
	                                               float delta) {
		CombatDualWieldEvent event = new CombatDualWieldEvent(attacker, defender, offhand, delta);
		eventBus.post(event);
		return event;
	}

	public WeaponUseEvent fireWeaponUse(com.wurmonline.server.creatures.Creature creature,
	                                    com.wurmonline.server.items.Item weapon,
	                                    float previousValue,
	                                    float newValue) {
		WeaponUseEvent event = new WeaponUseEvent(creature, weapon, previousValue, newValue);
		eventBus.post(event);
		return event;
	}

	public void firePlayerDeath(com.wurmonline.server.players.Player player,
	                            com.wurmonline.server.creatures.Creature killer) {
		if (DEBUG) {
			logger.info(String.format("[Event] PlayerDeathEvent: player=%s, killer=%s",
				player.getName(),
				killer != null ? killer.getName() : "null"));
		}

		// Post modern event
		eventBus.post(new PlayerDeathEvent(player, killer));

		if (DEBUG) {
			logger.info("[Event] PlayerDeathEvent: completed");
		}
	}

	// ========================================================================
	// Vehicle/Mount Events
	// ========================================================================

	public boolean fireVehicleMount(com.wurmonline.server.creatures.Creature rider,
	                                com.wurmonline.server.creatures.Creature mount,
	                                com.wurmonline.server.behaviours.Vehicle vehicle,
	                                int seatNumber,
	                                boolean asDriver) {
		if (DEBUG) {
			logger.info(String.format("[Event] VehicleMountEvent: rider=%s, mount=%s (creature), seat=%d, asDriver=%s",
				rider.getName(), mount.getName(), seatNumber, asDriver));
		}

		// Post modern event
		VehicleMountEvent event =
			new VehicleMountEvent(rider, mount, vehicle, seatNumber, asDriver);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] VehicleMountEvent: cancelled=%s", event.isCancelled()));
		}

		// Return true if cancelled
		return event.isCancelled();
	}

	public boolean fireVehicleMount(com.wurmonline.server.creatures.Creature rider,
	                                com.wurmonline.server.items.Item mount,
	                                com.wurmonline.server.behaviours.Vehicle vehicle,
	                                int seatNumber,
	                                boolean asDriver) {
		if (DEBUG) {
			logger.info(String.format("[Event] VehicleMountEvent: rider=%s, mount=%s (item), seat=%d, asDriver=%s",
				rider.getName(), mount.getName(), seatNumber, asDriver));
		}

		// Post modern event
		VehicleMountEvent event =
			new VehicleMountEvent(rider, mount, vehicle, seatNumber, asDriver);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] VehicleMountEvent: cancelled=%s", event.isCancelled()));
		}

		// Return true if cancelled
		return event.isCancelled();
	}

	public float fireVehicleSpeedCalculation(com.wurmonline.server.behaviours.Vehicle vehicle,
	                                         com.wurmonline.server.creatures.Creature mount,
	                                         com.wurmonline.server.creatures.Creature rider,
	                                         float baseSpeed,
	                                         boolean mounting) {
		// Post modern event
		VehicleSpeedCalculationEvent event =
			new VehicleSpeedCalculationEvent(
				vehicle, mount, rider, baseSpeed, mounting);
		eventBus.post(event);

		// Return modified speed
		return event.getFinalSpeed();
	}

	public float fireVehicleSpeedCalculation(com.wurmonline.server.behaviours.Vehicle vehicle,
	                                         com.wurmonline.server.items.Item mount,
	                                         com.wurmonline.server.creatures.Creature rider,
	                                         float baseSpeed,
	                                         boolean mounting) {
		// Post modern event
		VehicleSpeedCalculationEvent event =
			new VehicleSpeedCalculationEvent(
				vehicle, mount, rider, baseSpeed, mounting);
		eventBus.post(event);

		// Return modified speed
		return event.getFinalSpeed();
	}

	public boolean fireMountEquipmentCheck(com.wurmonline.server.creatures.Creature mount,
	                                       com.wurmonline.server.items.Item equipment) {
		// Post modern event
		MountEquipmentCheckEvent event =
			new MountEquipmentCheckEvent(mount, equipment);
		eventBus.post(event);

		// Return true if incompatible (cancelled)
		return event.isCancelled();
	}

	// ========================================================================
	// Item Events (Phase 3)
	// ========================================================================

	public String fireItemExamine(com.wurmonline.server.items.Item item,
	                              com.wurmonline.server.creatures.Creature examiner,
	                              com.wurmonline.server.creatures.Creature owner,
	                              String originalText) {
		if (DEBUG) {
			EventCounter counter = eventCounters.computeIfAbsent("ItemExamineEvent", k -> new EventCounter());
			if (counter.shouldLog()) {
				long count = counter.getCountAndReset();
				logger.info(String.format("[Event] ItemExamineEvent: fired %d times in last 30 seconds", count));
			}
		}

		// Post modern event (owner resolved by framework)
		ItemExamineEvent event =
			new ItemExamineEvent(item, examiner, owner);
		eventBus.post(event);

		// Append additional description if mods added any
		if (event.hasAdditionalDescription()) {
			return originalText + "\n" + event.getAdditionalDescription();
		}
		return originalText;
	}

	public void fireItemEnchantmentStrings(com.wurmonline.server.items.Item item,
	                                      com.wurmonline.server.creatures.Creature examiner) {
		if (DEBUG) {
			logger.info(String.format("[Event] ItemEnchantmentStringsEvent: item=%s (id=%d), examiner=%s",
				item.getName(), item.getWurmId(), examiner.getName()));
		}

		// Post modern event
		ItemEnchantmentStringsEvent event =
			new ItemEnchantmentStringsEvent(item, examiner);
		eventBus.post(event);

		if (DEBUG) {
			logger.info("[Event] ItemEnchantmentStringsEvent: completed");
		}
	}

	public boolean fireItemDrop(com.wurmonline.server.items.Item item,
	                           com.wurmonline.server.creatures.Creature dropper,
	                           boolean onGround) {
		if (DEBUG) {
			logger.info(String.format("[Event] ItemDropEvent: item=%s (id=%d), dropper=%s, onGround=%s",
				item.getName(), item.getWurmId(), dropper.getName(), onGround));
		}

		// Post modern event
		ItemDropEvent event =
			new ItemDropEvent(item, dropper, onGround);
		eventBus.post(event);

		if (DEBUG) {
			logger.info(String.format("[Event] ItemDropEvent: cancelled=%s", event.isCancelled()));
		}

		// Return true if cancelled
		return event.isCancelled();
	}

	public boolean fireItemTrade(com.wurmonline.server.items.Item item,
	                            com.wurmonline.server.items.TradingWindow window) {
		try {
			// Extract giver and receiver from TradingWindow via reflection
			java.lang.reflect.Field ownerField = window.getClass().getDeclaredField("windowowner");
			ownerField.setAccessible(true);
			com.wurmonline.server.creatures.Creature giver =
				(com.wurmonline.server.creatures.Creature) ownerField.get(window);

			java.lang.reflect.Field watcherField = window.getClass().getDeclaredField("watcher");
			watcherField.setAccessible(true);
			com.wurmonline.server.creatures.Creature receiver =
				(com.wurmonline.server.creatures.Creature) watcherField.get(window);

			if (DEBUG) {
				logger.info(String.format("[Event] ItemTradeEvent: item=%s (id=%d), giver=%s, receiver=%s",
					item.getName(), item.getWurmId(), giver.getName(), receiver.getName()));
			}

			// Post modern event
			ItemTradeEvent event =
				new ItemTradeEvent(item, giver, receiver);
			eventBus.post(event);

			if (DEBUG) {
				logger.info(String.format("[Event] ItemTradeEvent: cancelled=%s", event.isCancelled()));
			}

			// Return true if cancelled
			return event.isCancelled();

		} catch (Exception e) {
			logger.log(java.util.logging.Level.WARNING,
				"Failed to extract trading window participants", e);
			return false; // Allow trade on error
		}
	}

	/**
	 * Fire BodyMenuPopulateEvent when body context menu is being populated.
	 * Allows mods to add custom menu entries.
	 *
	 * @param performer The player opening the body menu
	 * @param bodyPart The body item being right-clicked
	 * @param menuEntries The modifiable list of menu entries
	 */
	public void fireBodyMenuPopulate(com.wurmonline.server.creatures.Creature performer,
	                                 com.wurmonline.server.items.Item bodyPart,
	                                 java.util.List<com.wurmonline.server.behaviours.ActionEntry> menuEntries) {
		logger.info(String.format("[Event] BodyMenuPopulateEvent START: performer=%s, bodyPart=%s, entries=%d",
			performer.getName(), bodyPart.getName(), menuEntries.size()));

		try {
			// Post modern event
			logger.info("[Event] Creating BodyMenuPopulateEvent...");
			com.garward.wurmmodloader.api.events.player.BodyMenuPopulateEvent event =
				new com.garward.wurmmodloader.api.events.player.BodyMenuPopulateEvent(
					performer, bodyPart, menuEntries);

			logger.info("[Event] Posting BodyMenuPopulateEvent to event bus...");
			eventBus.post(event);

			logger.info(String.format("[Event] BodyMenuPopulateEvent COMPLETED: entries=%d", menuEntries.size()));
		} catch (Exception e) {
			logger.log(java.util.logging.Level.SEVERE, "[Event] CRASH in BodyMenuPopulateEvent", e);
			throw e;
		}
	}

	/**
	 * Load and sync server configuration from file to database.
	 *
	 * <p>Uses reflection to get world folder name and server ID from Wurm classes.
	 * Must be called AFTER database is ready (during server startup).</p>
	 */
	private static volatile boolean configSyncCompleted = false;
	private static volatile int configSyncRetries = 0;
	private static final int MAX_CONFIG_SYNC_RETRIES = 10; // Increased to allow more time for database to become ready

	private void loadAndSyncServerConfig() {
		boolean isRetry = configSyncRetries > 0;

		try {
			// Get world folder name from Constants.dbHost via reflection
			// dbHost looks like: "Adventure/sqlite/wurmplayers.db"
			Class<?> constantsClass = Class.forName("com.wurmonline.server.Constants");
			java.lang.reflect.Field dbHostField = constantsClass.getField("dbHost");
			String dbHost = (String) dbHostField.get(null);

			// Extract world folder name (first path component)
			String worldFolder = dbHost;
			if (worldFolder.contains("/")) {
				worldFolder = worldFolder.substring(0, worldFolder.indexOf('/'));
			}
			if (worldFolder.contains("\\")) {
				worldFolder = worldFolder.substring(0, worldFolder.indexOf('\\'));
			}

			if (!isRetry) {
				logger.info("[ServerConfigLoader] Detected world folder: " + worldFolder);
			}

			// Get server ID from Servers.getLocalServerId() via reflection
			Class<?> serversClass = Class.forName("com.wurmonline.server.Servers");
			java.lang.reflect.Method getLocalServerIdMethod = serversClass.getMethod("getLocalServerId");
			int serverId = (int) getLocalServerIdMethod.invoke(null);

			if (!isRetry) {
				logger.info("[ServerConfigLoader] Detected server ID: " + serverId);
			}

			// Load configuration (generates from database if file doesn't exist)
			com.garward.wurmmodloader.config.ServerConfig config =
				com.garward.wurmmodloader.config.ServerConfigLoader.load(worldFolder, serverId);

			if (!isRetry) {
				logger.info("[ServerConfigLoader] Configuration loaded successfully");
			}

			// Sync to database (only writes if config differs from current values)
			boolean syncSuccess = com.garward.wurmmodloader.config.ServerConfigSync.syncToDatabase(config, serverId);

			if (syncSuccess) {
				logger.info("[ServerConfigLoader] Configuration sync completed successfully");
				configSyncCompleted = true;
			} else {
				logger.info("[ServerConfigLoader] Configuration sync skipped - will retry automatically");
			}

		} catch (com.garward.wurmmodloader.config.ConfigException e) {
			logger.log(java.util.logging.Level.SEVERE,
				"[ServerConfigLoader] FATAL: Config validation failed - please review your server_config.yaml file", e);
			throw new RuntimeException("Server configuration is invalid", e);
		} catch (java.sql.SQLException e) {
			// Database connection not ready yet - this is expected during early server startup
			if (e.getMessage() != null && e.getMessage().contains("closed")) {
				logger.warning("[ServerConfigLoader] Database connection not ready yet - skipping config sync");
				logger.warning("[ServerConfigLoader] This is normal during server startup - server will use database defaults");
				logger.info("[ServerConfigLoader] Config sync will be retried after database initialization");
			} else {
				logger.log(java.util.logging.Level.WARNING,
					"[ServerConfigLoader] Database error during config sync - continuing with database defaults", e);
			}
			// Don't throw - allow server to start with database defaults
		} catch (Exception e) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerConfigLoader] Failed to load server configuration - continuing with database defaults", e);
			// Don't throw - allow server to start with database defaults
		}
	}

	/**
	 * Schedule a delayed retry of config sync if it hasn't completed yet.
	 * Retries up to 3 times with 5-second intervals.
	 */
	/**
	 * Schedule delayed database sync to happen after server is fully started.
	 * Waits 30 seconds to ensure database connection pool is fully initialized.
	 */
	private void scheduleDatabaseSync() {
		logger.info("[ServerConfigSync] Scheduling database sync in 30 seconds (after DB pool initializes)");

		java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
			try {
				logger.info("[ServerConfigSync] ===== DELAYED DATABASE SYNC (After Server Fully Started) =====");

				// Detect world folder
				String serverRoot = System.getProperty("user.dir");
				java.io.File serverDir = new java.io.File(serverRoot);
				String worldFolder = null;

				for (java.io.File dir : serverDir.listFiles()) {
					if (dir.isDirectory() && !dir.getName().startsWith(".")) {
						java.io.File dbFile = new java.io.File(dir, "sqlite/wurmlogin.db");
						if (dbFile.exists() && dbFile.isFile()) {
							worldFolder = dir.getName();
							break;
						}
					}
				}

				if (worldFolder == null) {
					logger.warning("[ServerConfigSync] Could not find world folder - skipping database sync");
					return;
				}

				int estimatedServerId = 11455;
				com.garward.wurmmodloader.config.ServerConfig config =
					com.garward.wurmmodloader.config.ServerConfigLoader.load(worldFolder, estimatedServerId);

				// Now sync to database (pool should be ready after 30 seconds)
				boolean success = com.garward.wurmmodloader.config.ServerConfigSync.syncToDatabase(config, estimatedServerId);

				if (success) {
					logger.info("[ServerConfigSync] ✅ Database sync completed successfully");
					configSyncCompleted = true;
				} else {
					logger.warning("[ServerConfigSync] Database sync failed - config remains in memory only");
					logger.warning("[ServerConfigSync] Changes will not persist across server restarts");
				}

			} catch (Exception e) {
				logger.log(java.util.logging.Level.WARNING,
					"[ServerConfigSync] Database sync failed", e);
			}
		}, 30, java.util.concurrent.TimeUnit.SECONDS);
	}

	public static ServerHook createServerHook() {
		return ProxyServerHook.getInstance();
	}

	public void registerPlayerHooks() {
		// Overridden in ProxyServerHook
	}

	// ========== SPELL SYSTEM EVENT HANDLERS ==========

	protected float fireItemDamage(long itemId, String itemName, float damage, float currentDamage) {
		com.garward.wurmmodloader.api.events.item.ItemDamageEvent event =
			new com.garward.wurmmodloader.api.events.item.ItemDamageEvent(itemId, itemName, damage, currentDamage);
		eventBus.post(event);

		if (event.isCancelled()) {
			return 0.0f;
		}
		return event.getModifiedDamage();
	}

	protected int fireContainerVolume(long itemId, String itemName, int value, int volumeType) {
		com.garward.wurmmodloader.api.events.item.ContainerVolumeEvent.VolumeType type =
			com.garward.wurmmodloader.api.events.item.ContainerVolumeEvent.VolumeType.values()[volumeType];
		com.garward.wurmmodloader.api.events.item.ContainerVolumeEvent event =
			new com.garward.wurmmodloader.api.events.item.ContainerVolumeEvent(itemId, itemName, value, type);
		eventBus.post(event);
		return event.getModifiedValue();
	}

	protected double fireSkillDifficulty(long performerId, String performerName,
	                                    int skillId, String skillName,
	                                    long toolId, String toolName, double difficulty) {
		com.garward.wurmmodloader.api.events.skill.SkillDifficultyEvent event =
			new com.garward.wurmmodloader.api.events.skill.SkillDifficultyEvent(
				performerId, performerName, skillId, skillName, toolId, toolName, difficulty);
		eventBus.post(event);
		return event.getModifiedDifficulty();
	}

	protected int fireStaminaCost(long creatureId, String creatureName,
	                             int cost, int currentStamina, String actionType) {
		com.garward.wurmmodloader.api.events.creature.StaminaCostEvent event =
			new com.garward.wurmmodloader.api.events.creature.StaminaCostEvent(
				creatureId, creatureName, cost, currentStamina, actionType);
		eventBus.post(event);
		return event.getModifiedCost();
	}

	protected int fireSpellFavorCost(long casterId, String casterName,
	                                int spellId, String spellName,
	                                int cost, float currentFavor) {
		com.garward.wurmmodloader.api.events.spell.SpellFavorCostEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellFavorCostEvent(
				casterId, casterName, spellId, spellName, cost, currentFavor);
		eventBus.post(event);
		return event.getModifiedCost();
	}

	protected float fireCombatRating(long creatureId, String creatureName, float rating) {
		com.garward.wurmmodloader.api.events.combat.CombatRatingEvent event =
			new com.garward.wurmmodloader.api.events.combat.CombatRatingEvent(creatureId, creatureName, rating);
		eventBus.post(event);
		return event.getModifiedRating();
	}

	/**
	 * Sync server config BEFORE Servers.loadAllServers() is called.
	 * This is called by ServerConfigLoadPatch bytecode injection.
	 *
	 * <p><strong>Strategy: Two-phase config sync</strong>
	 * <ul>
	 *   <li><strong>Phase 1 (here - early):</strong> Apply config to server memory → most settings work immediately</li>
	 *   <li><strong>Phase 2 (fireOnServerStarted):</strong> Update database → config persists for next restart</li>
	 * </ul>
	 *
	 * <p>This avoids SQLite connection pool corruption while still making most config changes work immediately.
	 */
	public void syncServerConfigBeforeLoad() {
		try {
			// Detect world folder from filesystem (look for directories with sqlite/ subfolder)
			String serverRoot = System.getProperty("user.dir");
			java.io.File serverDir = new java.io.File(serverRoot);

			String worldFolder = null;

			// Find world folder by looking for directories containing sqlite/wurmlogin.db
			for (java.io.File dir : serverDir.listFiles()) {
				if (dir.isDirectory() && !dir.getName().startsWith(".")) {
					java.io.File dbFile = new java.io.File(dir, "sqlite/wurmlogin.db");
					if (dbFile.exists() && dbFile.isFile()) {
						worldFolder = dir.getName();
						break;
					}
				}
			}

			if (worldFolder == null) {
				logger.warning("[ServerConfigSync] Could not find world folder - skipping early config sync");
				return;
			}

			logger.info("[ServerConfigSync] ===== EARLY CONFIG SYNC (Before Server Reads Database) =====");
			logger.info("[ServerConfigSync] World: " + worldFolder);

			// We need server ID but can't safely get it without triggering class init
			// The server ID will be in the database, but we can't query it without corrupting the pool
			// So we'll use a placeholder and get it later
			int estimatedServerId = 11455; // Common default, will be corrected later

			// Load config from YAML
			com.garward.wurmmodloader.config.ServerConfig config =
				com.garward.wurmmodloader.config.ServerConfigLoader.load(worldFolder, estimatedServerId);

			logger.info("[ServerConfigSync] Loaded config from YAML: " + config);

			// Apply config DIRECTLY to server memory (bypassing database entirely)
			// This makes most settings work immediately without corrupting the connection pool
			com.garward.wurmmodloader.config.ServerConfigSync.applyConfigToServerMemory(config, estimatedServerId);

			logger.info("[ServerConfigSync] ✅ Config applied to server memory (most settings active now)");
			logger.info("[ServerConfigSync] ℹ️  Database will be updated after server fully starts");
			logger.info("[ServerConfigSync] ℹ️  Some settings may require restart to fully apply");

			configSyncCompleted = false; // Will be set true after database sync

		} catch (Exception e) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerConfigSync] Early config sync failed (non-fatal)", e);
		}
	}

	// ========== WML_SYNC MODCOMM CHANNEL EVENTS ==========

	/**
	 * Fire MovementIntentReceivedEvent when client sends movement intent via WML_SYNC channel.
	 */
	public void fireMovementIntentReceived(Player player, long seqId, byte inputState) {
		com.garward.wurmmodloader.api.events.sync.MovementIntentReceivedEvent event =
			new com.garward.wurmmodloader.api.events.sync.MovementIntentReceivedEvent(player, seqId, inputState);
		eventBus.post(event);
	}

	/**
	 * Fire PredictionStateReceivedEvent when client sends predicted position for debugging.
	 */
	public void firePredictionStateReceived(Player player, long seqId, float x, float y, float height) {
		com.garward.wurmmodloader.api.events.sync.PredictionStateReceivedEvent event =
			new com.garward.wurmmodloader.api.events.sync.PredictionStateReceivedEvent(player, seqId, x, y, height);
		eventBus.post(event);
	}

}

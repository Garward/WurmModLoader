package com.garward.wurmmodloader.modloader.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

// Use modern interfaces
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import com.garward.wurmmodloader.modloader.interfaces.ModEntry;
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
import com.garward.wurmmodloader.api.events.skill.SkillGainMultiplierEvent;
import com.garward.wurmmodloader.api.events.player.PlayerDeathEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent;
import com.garward.wurmmodloader.api.events.server.ServerPollEvent;
import com.garward.wurmmodloader.api.events.server.ServerFullyReadyEvent;
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

	/**
	 * Per-creature event trace. Pass a comma-separated list of wurmIds via
	 * {@code -DtraceCreatureIds=12345,67890} (or a single id). When any
	 * creature-bearing fire method involves a matching id, the framework logs
	 * the event name and participating creature names. Zero-cost when unset.
	 */
	private static final java.util.Set<Long> TRACE_CREATURE_IDS = parseTraceIds();

	private static java.util.Set<Long> parseTraceIds() {
		String prop = System.getProperty("traceCreatureIds", System.getProperty("traceCreatureId", ""));
		if (prop == null || prop.isEmpty()) {
			return java.util.Collections.emptySet();
		}
		java.util.Set<Long> ids = new java.util.HashSet<>();
		for (String part : prop.split(",")) {
			try {
				ids.add(Long.parseLong(part.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return ids;
	}

	private static void traceCreature(String eventName, com.wurmonline.server.creatures.Creature... participants) {
		if (TRACE_CREATURE_IDS.isEmpty() || participants == null) {
			return;
		}
		for (com.wurmonline.server.creatures.Creature c : participants) {
			if (c != null && TRACE_CREATURE_IDS.contains(c.getWurmId())) {
				StringBuilder names = new StringBuilder();
				for (com.wurmonline.server.creatures.Creature p : participants) {
					if (names.length() > 0) names.append(", ");
					names.append(p == null ? "null" : p.getName() + "#" + p.getWurmId());
				}
				logger.info("[CreatureTrace " + c.getWurmId() + "] " + eventName + " [" + names + "]");
				return;
			}
		}
	}

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

	/**
	 * Fired at the top of {@code Villages.loadVillages()} via bytecode patch.
	 * Zones DB connection is open, no village state in memory yet. This is
	 * the seeding/repair hook.
	 */
	public void fireOnServerPreInit() {
		logger.info("[ServerHook] ServerPreInit — running core seeders, then posting event");
		try {
			com.garward.wurmmodloader.core.worldseed.WorldSeedBootstrap.run();
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.SEVERE,
				"[ServerHook] WorldSeedBootstrap threw", t);
		}
		try {
			eventBus.post(new com.garward.wurmmodloader.api.events.server.ServerPreInitEvent());
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.SEVERE,
				"[ServerHook] ServerPreInitEvent subscriber threw", t);
		}
	}

	public void fireOnServerStarted() {
		logger.info("[ServerHook] DEBUG: Bootstrapping runtime registries");
		SystemBootstrap.initializeAll();

		// Each call is isolated: a legacy shim throwing must not skip the
		// canonical garward ModComm init or the WML channels below — otherwise
		// the first client handshake NPEs.
		try {
			logger.info("[ServerHook] DEBUG: Calling legacy ModComm.serverStarted()");
			org.gotti.wurmunlimited.modcomm.ModComm.serverStarted();
			logger.info("[ServerHook] DEBUG: Successfully called legacy ModComm.serverStarted()");
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerHook] legacy ModComm.serverStarted() failed (continuing)", t);
		}

		try {
			logger.info("[ServerHook] DEBUG: Calling new ModComm.serverStarted()");
			ModComm.serverStarted();
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerHook] garward ModComm.serverStarted() failed (continuing)", t);
		}

		try {
			ModIntraServer.serverStarted();
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerHook] ModIntraServer.serverStarted() failed (continuing)", t);
		}

		// Initialize WML_SYNC channel for client-server prediction
		logger.info("[ServerHook] DEBUG: Initializing WML_SYNC channel");
		com.garward.wurmmodloader.sync.WMLSyncChannel.initialize();
		logger.info("[ServerHook] DEBUG: WML_SYNC channel initialized");

		// Initialize WML_CAPABILITIES channel for server mod detection
		logger.info("[ServerHook] DEBUG: Initializing WML_CAPABILITIES channel");
		com.garward.wurmmodloader.capabilities.WMLCapabilitiesChannel.initialize();
		logger.info("[ServerHook] DEBUG: WML_CAPABILITIES channel initialized");

		// Initialize wml.serverinfo channel for server URL auto-discovery
		com.garward.wurmmodloader.core.serverinfo.ServerInfoChannel.initialize();

		// Initialize capability system (Phase 5.5)
		logger.info("[ServerHook] DEBUG: Initializing CapabilityManager");
		com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance().initialize();
		logger.info("[ServerHook] DEBUG: CapabilityManager initialized");

		// Fire CapabilityRegistrationEvent (mods register their capabilities)
		logger.info("[ServerHook] DEBUG: Firing CapabilityRegistrationEvent");
		eventBus.post(new CapabilityRegistrationEvent());
		logger.info("[ServerHook] DEBUG: Capability registration complete");

		// NOTE: DB-dependent config sync now runs from fireOnServerFullyReady(),
		// which fires when CommandReader.run begins — after the DB pool, Steam,
		// and all async subsystems have actually settled. The previous "sleep 30s
		// after ServerStartedEvent" was a workaround for this event firing too
		// early; ServerFullyReadyEvent makes it unnecessary.

		// Fire legacy listeners
		logger.info("[ServerHook] DEBUG: Firing legacy serverStarted listeners");
		serverStarted.fire(listener -> listener.onServerStarted());

		// Post modern event
		logger.info("[ServerHook] DEBUG: Posting ServerStartedEvent");
		eventBus.post(new ServerStartedEvent());

		// Run vanilla-API village creation (configurable; off = mods materialize via event).
		try {
			com.garward.wurmmodloader.core.worldseed.WorldSeedCompletionHandler.run();
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerHook] WorldSeedCompletionHandler threw", t);
		}

		// Post WorldSeededEvent carrying the PreInit-decided seed outcome (if any).
		try {
			com.garward.wurmmodloader.api.events.server.WorldSeedResult seedResult =
				com.garward.wurmmodloader.api.worldseed.WorldSeedAPI.getResult();
			if (seedResult == null) {
				// Seeder didn't run (PreInit hook missing?) — publish a FAILED_INFRASTRUCTURE sentinel
				// so subscribers still fire and can branch on outcome.
				seedResult = new com.garward.wurmmodloader.api.events.server.WorldSeedResult(
					com.garward.wurmmodloader.api.events.server.WorldSeedResult.Outcome.FAILED_INFRASTRUCTURE,
					0, 0, 0, 0, 0);
				com.garward.wurmmodloader.api.worldseed.WorldSeedAPI.publish(seedResult);
			}
			logger.info("[ServerHook] Posting WorldSeededEvent: " + seedResult);
			eventBus.post(new com.garward.wurmmodloader.api.events.server.WorldSeededEvent(seedResult));
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerHook] WorldSeededEvent dispatch threw", t);
		}

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
		// Capture the caller stack of Server.shutDown() — the bytecode hook
		// fires us before any cleanup runs, so the calling frames are still
		// on the stack and tell us who triggered the shutdown.
		try {
			com.garward.wurmmodloader.debug.ShutdownForensics.logServerShutdownCaller();
		} catch (Throwable ignore) {}

		// Flush + stop the creature-save batcher *before* anything else so its
		// scheduled executor doesn't race with DB shutdown. Its flushes open
		// fresh JDBC connections; if a backend (e.g. embedded Postgres) has
		// begun shutting down, those connections fail with "database is
		// shutting down" and we lose the batched deltas.
		try {
			com.garward.wurmmodloader.performance.CreatureStatusBatcher.shutdown();
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerHook] CreatureStatusBatcher.shutdown threw", t);
		}

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

		// Send framework-level server info (HTTP URI, version)
		com.garward.wurmmodloader.core.serverinfo.ServerInfoChannel.sendToPlayer(player);

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
		traceCreature("CreatureDeathEvent", victim, killer);
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

	public boolean fireTameAttempt(com.wurmonline.server.creatures.Creature performer,
	                                com.wurmonline.server.creatures.Creature target,
	                                com.garward.wurmmodloader.api.events.creature.TameAttemptEvent.Source source) {
		traceCreature("TameAttemptEvent", performer, target);
		if (DEBUG) {
			logger.info(String.format("[Event] TameAttemptEvent: performer=%s, target=%s, source=%s",
				performer.getName(), target.getName(), source));
		}
		com.garward.wurmmodloader.api.events.creature.TameAttemptEvent event =
			new com.garward.wurmmodloader.api.events.creature.TameAttemptEvent(performer, target, source);
		eventBus.post(event);
		return event.isCancelled();
	}

	public void fireTameComplete(com.wurmonline.server.creatures.Creature performer,
	                              com.wurmonline.server.creatures.Creature target,
	                              com.garward.wurmmodloader.api.events.creature.TameAttemptEvent.Source source,
	                              double power) {
		traceCreature("TameCompleteEvent", performer, target);
		if (DEBUG) {
			logger.info(String.format("[Event] TameCompleteEvent: performer=%s, target=%s, source=%s, power=%.2f",
				performer.getName(), target.getName(), source, power));
		}
		eventBus.post(new com.garward.wurmmodloader.api.events.creature.TameCompleteEvent(
			performer, target, source, power));
	}

	public void firePetReleased(com.wurmonline.server.creatures.Creature pet,
	                             long formerOwnerId,
	                             com.garward.wurmmodloader.api.events.creature.PetReleasedEvent.Reason reason) {
		traceCreature("PetReleasedEvent", pet);
		if (DEBUG) {
			logger.info(String.format("[Event] PetReleasedEvent: pet=%s, formerOwnerId=%d, reason=%s",
				pet.getName(), formerOwnerId, reason));
		}
		eventBus.post(new com.garward.wurmmodloader.api.events.creature.PetReleasedEvent(
			pet, formerOwnerId, reason));
	}

	public String fireCreatureExamine(com.wurmonline.server.creatures.Creature creature,
	                                  String examineText) {
		traceCreature("CreatureExamineEvent", creature);
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
		traceCreature("CombatDamageEvent", attacker, defender);
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
		traceCreature("CreatureSpawnEvent", creature);
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
		traceCreature("CreaturePositionUpdatedEvent", creature);
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
		traceCreature("CreatureDbSaveEvent", creature);
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
		traceCreature("CreatureDbLoadEvent", creature);
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
		traceCreature("CreatureBreedEvent", performer, target);
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
		traceCreature("CombatAttackEvent", attacker, defender);
		// Post modern event
		com.garward.wurmmodloader.api.events.combat.CombatAttackEvent event =
			new com.garward.wurmmodloader.api.events.combat.CombatAttackEvent(
				attacker, defender, combatCounter, opportunity, actionCounter, action);
		eventBus.post(event);

		// Return result with cancellation status
		return new CombatAttackResult(event.isCancelled(), event.getResult());
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

    public SkillGainMultiplierEvent fireSkillGainMultiplier(Skill skill,
                                                            double check,
                                                            double power,
                                                            double learnMod,
                                                            float times,
                                                            double skillDivider,
                                                            double vanillaBonus,
                                                            double vanillaMultiplier,
                                                            boolean vanillaWouldAdvance) {
        if (DEBUG) {
            EventCounter counter = eventCounters.computeIfAbsent("SkillGainMultiplierEvent", k -> new EventCounter());
            if (counter.shouldLog()) {
                long count = counter.getCountAndReset();
                logger.info(String.format("[Event] SkillGainMultiplierEvent: fired %d times in last 30 seconds", count));
            }
        }

        SkillGainMultiplierEvent event = new SkillGainMultiplierEvent(
                skill, check, power, learnMod, times, skillDivider,
                vanillaBonus, vanillaMultiplier, vanillaWouldAdvance);
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

	// start=<WorldName> is captured by DelegatedLauncher into the system property
	// "wurmmodloader.launchWorldFolder" before CapabilityHooks runs. We deliberately
	// avoid a setter on this class — class-loading ServerHook that early would
	// freeze Player before bytecode patches can apply.

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
	 * Fired from {@code CommandReaderPatch} insertBefore on
	 * {@code CommandReader.run} — the true "server fully settled" moment.
	 * Runs the DB-dependent config sync synchronously (pool is hot by now),
	 * then posts {@link ServerFullyReadyEvent} for mods to subscribe to.
	 *
	 * <p>Previously the sync ran on a 30-second delayed executor because
	 * {@link ServerStartedEvent} fires before Steam connect / DB pool warmup.
	 * That workaround is no longer needed.
	 */
	public void fireOnServerFullyReady() {
		try {
			runDatabaseConfigSync();
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerConfigSync] Database sync failed", t);
		}

		try {
			eventBus.post(new ServerFullyReadyEvent());
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerHook] ServerFullyReadyEvent subscriber threw", t);
		}
	}

	private void runDatabaseConfigSync() throws Exception {
		logger.info("[ServerConfigSync] ===== DATABASE SYNC (CommandReader ready — pool is live) =====");

		// Use Constants.dbHost — the authoritative active-world pointer Wurm
		// itself reads against. Directory scanning picks the wrong world when
		// multiple sqlite scaffolds exist side by side.
		String worldFolder = null;
		try {
			Class<?> constantsClass = Class.forName("com.wurmonline.server.Constants");
			java.lang.reflect.Field dbHostField = constantsClass.getField("dbHost");
			String dbHost = (String) dbHostField.get(null);
			if (dbHost != null) {
				worldFolder = dbHost;
				int slash = worldFolder.indexOf('/');
				if (slash >= 0) worldFolder = worldFolder.substring(0, slash);
				int back = worldFolder.indexOf('\\');
				if (back >= 0) worldFolder = worldFolder.substring(0, back);
			}
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerConfigSync] Could not read Constants.dbHost — falling back to directory scan", t);
		}

		if (worldFolder == null || worldFolder.isEmpty()) {
			String serverRoot = System.getProperty("user.dir");
			java.io.File serverDir = new java.io.File(serverRoot);
			java.io.File[] children = serverDir.listFiles();
			if (children != null) {
				for (java.io.File dir : children) {
					if (dir.isDirectory() && !dir.getName().startsWith(".")) {
						java.io.File dbFile = new java.io.File(dir, "sqlite/wurmlogin.db");
						if (dbFile.exists() && dbFile.isFile()) {
							worldFolder = dir.getName();
							break;
						}
					}
				}
			}
		}

		if (worldFolder == null) {
			logger.warning("[ServerConfigSync] Could not find world folder - skipping database sync");
			return;
		}

		logger.info("[ServerConfigSync] Active world: " + worldFolder);

		int serverId;
		try {
			Class<?> serversClass = Class.forName("com.wurmonline.server.Servers");
			serverId = (int) serversClass.getMethod("getLocalServerId").invoke(null);
		} catch (Throwable t) {
			logger.log(java.util.logging.Level.WARNING,
				"[ServerConfigSync] Could not read Servers.getLocalServerId() — falling back to 11455", t);
			serverId = 11455;
		}

		com.garward.wurmmodloader.config.ServerConfig config =
			com.garward.wurmmodloader.config.ServerConfigLoader.load(worldFolder, serverId);

		boolean success = com.garward.wurmmodloader.config.ServerConfigSync.syncToDatabase(config, serverId);

		if (success) {
			logger.info("[ServerConfigSync] ✅ Database sync completed successfully");
			configSyncCompleted = true;
		} else {
			logger.warning("[ServerConfigSync] Database sync failed - config remains in memory only");
			logger.warning("[ServerConfigSync] Changes will not persist across server restarts");
		}
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

	protected int fireSpellCastingTime(int spellId, String spellName,
	                                   long casterId, String casterName,
	                                   int originalTime) {
		com.garward.wurmmodloader.api.events.spell.SpellCastingTimeEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellCastingTimeEvent(
				spellId, spellName, casterId, casterName, originalTime);
		eventBus.post(event);
		return event.getModifiedTime();
	}

	protected long fireSpellCooldown(int spellId, String spellName,
	                                 long casterId, String casterName,
	                                 long originalCooldownMs) {
		com.garward.wurmmodloader.api.events.spell.SpellCooldownEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellCooldownEvent(
				spellId, spellName, casterId, casterName, originalCooldownMs);
		eventBus.post(event);
		return event.getModifiedCooldownMs();
	}

	protected double fireSpellPower(int spellId, String spellName,
	                                long casterId, String casterName,
	                                double originalPower) {
		com.garward.wurmmodloader.api.events.spell.SpellPowerEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellPowerEvent(
				spellId, spellName, casterId, casterName, originalPower);
		eventBus.post(event);
		return event.getModifiedPower();
	}

	protected boolean fireSpellCastAttempt(int spellId, String spellName,
	                                       long casterId, String casterName) {
		com.garward.wurmmodloader.api.events.spell.SpellCastAttemptEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellCastAttemptEvent(
				spellId, spellName, casterId, casterName);
		eventBus.post(event);
		return event.isCancelled();
	}

	protected boolean fireSpellEffect(int spellId, String spellName,
	                                  long casterId, String casterName,
	                                  double power, boolean negative) {
		com.garward.wurmmodloader.api.events.spell.SpellEffectEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellEffectEvent(
				spellId, spellName, casterId, casterName, power, negative);
		eventBus.post(event);
		return event.isCancelled();
	}

	protected int fireSpellDifficulty(int spellId, String spellName,
	                                  int originalDifficulty, boolean forItem) {
		com.garward.wurmmodloader.api.events.spell.SpellDifficultyEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellDifficultyEvent(
				spellId, spellName, originalDifficulty, forItem);
		eventBus.post(event);
		return event.getModifiedDifficulty();
	}

	protected void fireDeitySpellRegistration(int deityNumber, String deityName,
	                                          int spellId, String spellName, boolean added) {
		com.garward.wurmmodloader.api.events.spell.DeitySpellRegistrationEvent event =
			new com.garward.wurmmodloader.api.events.spell.DeitySpellRegistrationEvent(
				deityNumber, deityName, spellId, spellName, added);
		eventBus.post(event);
	}

	protected boolean fireSacrificeAcceptance(long itemId, int templateId, boolean originalAccepted) {
		com.garward.wurmmodloader.api.events.priest.SacrificeAcceptanceEvent event =
			new com.garward.wurmmodloader.api.events.priest.SacrificeAcceptanceEvent(
				itemId, templateId, originalAccepted);
		eventBus.post(event);
		return event.getModifiedAccepted();
	}

	protected float fireSacrificeFavorValue(int deityNumber, long itemId, int templateId, float originalValue) {
		com.garward.wurmmodloader.api.events.priest.SacrificeFavorValueEvent event =
			new com.garward.wurmmodloader.api.events.priest.SacrificeFavorValueEvent(
				deityNumber, itemId, templateId, originalValue);
		eventBus.post(event);
		return event.getModifiedValue();
	}

	protected float fireSacrificeFavorModifier(int deityNumber, long itemId, int templateId, float originalModifier) {
		com.garward.wurmmodloader.api.events.priest.SacrificeFavorModifierEvent event =
			new com.garward.wurmmodloader.api.events.priest.SacrificeFavorModifierEvent(
				deityNumber, itemId, templateId, originalModifier);
		eventBus.post(event);
		return event.getModifiedModifier();
	}

	protected double fireSpellResist(int spellId, String spellName,
	                                 long casterId, long targetId,
	                                 int difficulty, double originalResist) {
		com.garward.wurmmodloader.api.events.spell.SpellResistEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellResistEvent(
				spellId, spellName, casterId, targetId, difficulty, originalResist);
		eventBus.post(event);
		return event.getModifiedResist();
	}

	protected boolean fireSpellVisibility(int spellId, String spellName,
	                                      long casterId, long targetId, String targetType) {
		com.garward.wurmmodloader.api.events.spell.SpellVisibilityEvent.Target type;
		try {
			type = com.garward.wurmmodloader.api.events.spell.SpellVisibilityEvent.Target.valueOf(targetType);
		} catch (IllegalArgumentException e) {
			type = com.garward.wurmmodloader.api.events.spell.SpellVisibilityEvent.Target.CREATURE;
		}
		com.garward.wurmmodloader.api.events.spell.SpellVisibilityEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellVisibilityEvent(
				spellId, spellName, casterId, targetId, type);
		eventBus.post(event);
		return event.isCancelled();
	}

	protected boolean fireSpellPrecondition(int spellId, String spellName,
	                                        long casterId, String casterName,
	                                        long targetId, String targetType,
	                                        boolean originalAllowed) {
		com.garward.wurmmodloader.api.events.spell.SpellPreconditionEvent.TargetType type;
		try {
			type = com.garward.wurmmodloader.api.events.spell.SpellPreconditionEvent.TargetType.valueOf(targetType);
		} catch (IllegalArgumentException e) {
			type = com.garward.wurmmodloader.api.events.spell.SpellPreconditionEvent.TargetType.CREATURE;
		}
		com.garward.wurmmodloader.api.events.spell.SpellPreconditionEvent event =
			new com.garward.wurmmodloader.api.events.spell.SpellPreconditionEvent(
				spellId, spellName, casterId, casterName, targetId, type, originalAllowed);
		eventBus.post(event);
		return event.getModifiedAllowed();
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

			// 1. Authoritative: start=<WorldName> captured by DelegatedLauncher before
			//    WU had a chance to parse its own args. Avoids filesystem guessing
			//    and is immune to stale 'currentdir' markers from prior crashed boots.
			String launched = System.getProperty("wurmmodloader.launchWorldFolder");
			if (launched != null && !launched.isEmpty()) {
				java.io.File candidate = new java.io.File(serverDir, launched);
				java.io.File dbFile = new java.io.File(candidate, "sqlite/wurmlogin.db");
				if (candidate.isDirectory() && dbFile.exists()) {
					worldFolder = launched;
					logger.info("[ServerConfigSync] Using launch arg start=" + launched);
				} else {
					logger.warning("[ServerConfigSync] start=" + launched
						+ " given but folder/db missing — falling back to markers");
				}
			}

			// 2. Fallback: folder WU itself flagged current via 'currentdir' marker.
			if (worldFolder == null) {
				for (java.io.File dir : serverDir.listFiles()) {
					if (dir.isDirectory() && !dir.getName().startsWith(".")) {
						java.io.File marker = new java.io.File(dir, "currentdir");
						java.io.File dbFile = new java.io.File(dir, "sqlite/wurmlogin.db");
						if (marker.exists() && dbFile.exists()) {
							worldFolder = dir.getName();
							break;
						}
					}
				}
			}

			// Fallback: no currentdir marker found — pick first directory with a
			// wurmlogin.db so older worlds (pre-GameFolder era) still sync.
			if (worldFolder == null) {
				for (java.io.File dir : serverDir.listFiles()) {
					if (dir.isDirectory() && !dir.getName().startsWith(".")) {
						java.io.File dbFile = new java.io.File(dir, "sqlite/wurmlogin.db");
						if (dbFile.exists() && dbFile.isFile()) {
							worldFolder = dir.getName();
							logger.warning("[ServerConfigSync] No 'currentdir' marker found on any world folder — "
								+ "falling back to first match: " + worldFolder
								+ ". This may load the wrong world's config if you have multiple worlds.");
							break;
						}
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

	// ========================================================================
	// Database backend SPI events
	// ========================================================================

	/**
	 * Fire DatabaseBackendSelectionEvent from DbConnector.initialize() so that mods
	 * have a chance to call DatabaseBackendRegistry.register(...) before vanilla
	 * factories are built.
	 */
	public void fireDatabaseBackendSelection() {
		if (DEBUG) {
			logger.info("[Event] DatabaseBackendSelectionEvent: firing");
		}
		eventBus.post(new com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent());
		if (DEBUG) {
			logger.info("[Event] DatabaseBackendSelectionEvent: completed");
		}
	}

	/**
	 * Fire DatabaseBackendBootstrapEvent after a backend wins registration and
	 * before per-schema factories are instantiated. Gives the backend a seam for
	 * DDL bootstrap (e.g. {@code CREATE DATABASE IF NOT EXISTS}).
	 */
	public void fireDatabaseBackendBootstrap(
			com.garward.wurmmodloader.api.database.DatabaseBackend backend) {
		if (DEBUG) {
			logger.info("[Event] DatabaseBackendBootstrapEvent: firing for "
				+ (backend == null ? "null" : backend.getName()));
		}
		eventBus.post(new com.garward.wurmmodloader.api.events.database.DatabaseBackendBootstrapEvent(backend));
		if (DEBUG) {
			logger.info("[Event] DatabaseBackendBootstrapEvent: completed");
		}
	}

	/**
	 * Fire DatabaseConnectionOpenedEvent after a ConnectionFactory produces a new JDBC Connection.
	 */
	public void fireDatabaseConnectionOpened(com.wurmonline.server.database.WurmDatabaseSchema schema,
	                                          java.sql.Connection connection) {
		if (DEBUG) {
			logger.info(String.format("[Event] DatabaseConnectionOpenedEvent: schema=%s", schema));
		}
		eventBus.post(new com.garward.wurmmodloader.api.events.database.DatabaseConnectionOpenedEvent(
			schema, connection));
	}

	/**
	 * Fire DatabaseMigrationStartingEvent before Flyway migration runs.
	 */
	public void fireDatabaseMigrationStarting(com.wurmonline.server.database.WurmDatabaseSchema schema) {
		if (DEBUG) {
			logger.info(String.format("[Event] DatabaseMigrationStartingEvent: schema=%s", schema));
		}
		eventBus.post(new com.garward.wurmmodloader.api.events.database.DatabaseMigrationStartingEvent(schema));
	}

	/**
	 * Fire DatabaseMigrationCompletedEvent after Flyway migration succeeds.
	 */
	public void fireDatabaseMigrationCompleted(com.wurmonline.server.database.WurmDatabaseSchema schema) {
		if (DEBUG) {
			logger.info(String.format("[Event] DatabaseMigrationCompletedEvent: schema=%s", schema));
		}
		eventBus.post(new com.garward.wurmmodloader.api.events.database.DatabaseMigrationCompletedEvent(schema));
	}

	// ========================================================================
	// Trade / Village / ItemMove event dispatchers
	// ========================================================================

	/**
	 * Fires TradeInitiateEvent then NpcTradePermissionCheckEvent from the NPC's
	 * startTrading entry. Returns true if the trade should be aborted.
	 *
	 * <p>The "player" side is resolved as the other party on the current Trade.
	 * If the deny-reason on the permission event is set, the player receives an
	 * alert message.</p>
	 */
	public boolean fireTradeSessionStart(com.wurmonline.server.creatures.Creature npc) {
		com.wurmonline.server.items.Trade trade = npc.getTrade();
		com.wurmonline.server.creatures.Creature player = null;
		if (trade != null) {
			com.wurmonline.server.creatures.Creature c1 = trade.creatureOne;
			com.wurmonline.server.creatures.Creature c2 = trade.creatureTwo;
			player = (c1 != null && c1 != npc) ? c1 : c2;
		}

		com.garward.wurmmodloader.api.events.trade.TradeInitiateEvent initEvent =
			new com.garward.wurmmodloader.api.events.trade.TradeInitiateEvent(npc, player);
		eventBus.post(initEvent);
		if (DEBUG) {
			logger.info(String.format("[Event] TradeInitiateEvent: npc=%s player=%s cancelled=%s",
				npc.getName(), player == null ? "null" : player.getName(), initEvent.isCancelled()));
		}
		if (initEvent.isCancelled()) {
			return true;
		}

		com.garward.wurmmodloader.api.events.trade.NpcTradePermissionCheckEvent permEvent =
			new com.garward.wurmmodloader.api.events.trade.NpcTradePermissionCheckEvent(npc, player);
		eventBus.post(permEvent);
		if (DEBUG) {
			logger.info(String.format("[Event] NpcTradePermissionCheckEvent: npc=%s player=%s cancelled=%s reason=%s",
				npc.getName(), player == null ? "null" : player.getName(),
				permEvent.isCancelled(), permEvent.getDenyReason()));
		}
		if (permEvent.isCancelled()) {
			if (player != null && permEvent.getDenyReason() != null) {
				try {
					player.getCommunicator().sendAlertServerMessage(permEvent.getDenyReason());
				} catch (Exception ignore) {}
			}
			return true;
		}
		return false;
	}

	/**
	 * Fires TradeBalanceEvent. Returns true to skip the vanilla balance pass.
	 */
	public boolean fireTradeBalance(com.wurmonline.server.creatures.TradeHandler handler) {
		com.garward.wurmmodloader.api.events.trade.TradeBalanceEvent event =
			new com.garward.wurmmodloader.api.events.trade.TradeBalanceEvent(handler);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] TradeBalanceEvent: cancelled=%s", event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires ShopDiffEvent. Returns the (possibly mutated) money value to be
	 * added to {@code Trade.shopDiff}.
	 */
	public long fireShopDiff(com.wurmonline.server.items.Trade trade,
	                         long money,
	                         long currentShopDiff) {
		com.garward.wurmmodloader.api.events.trade.ShopDiffEvent event =
			new com.garward.wurmmodloader.api.events.trade.ShopDiffEvent(trade, money, currentShopDiff);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] ShopDiffEvent: in=%d shopDiff=%d out=%d",
				money, currentShopDiff, event.getMoney()));
		}
		return event.getMoney();
	}

	/**
	 * Fires MountSpeedPercentEvent. Returns a Float override or null if no
	 * listener overrode the value (callers should then run vanilla logic).
	 */
	public Float fireMountSpeedPercent(com.wurmonline.server.creatures.Creature creature,
	                                   boolean mounting) {
		com.garward.wurmmodloader.api.events.vehicle.MountSpeedPercentEvent event =
			new com.garward.wurmmodloader.api.events.vehicle.MountSpeedPercentEvent(creature, mounting);
		eventBus.post(event);
		if (event.isOverridden()) {
			return Float.valueOf(event.getPercent());
		}
		return null;
	}

	/**
	 * Fires ItemMoveCheckEvent. Returns true if the move should be rejected.
	 * On cancel with a deny-reason, the mover receives an alert.
	 */
	public boolean fireItemMoveCheck(com.wurmonline.server.items.Item item,
	                                 com.wurmonline.server.creatures.Creature mover,
	                                 long targetId,
	                                 boolean lastMove) {
		com.garward.wurmmodloader.api.events.item.ItemMoveCheckEvent event =
			new com.garward.wurmmodloader.api.events.item.ItemMoveCheckEvent(item, mover, targetId, lastMove);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] ItemMoveCheckEvent: item=%s target=%d cancelled=%s reason=%s",
				item == null ? "null" : item.getName(), targetId, event.isCancelled(), event.getDenyReason()));
		}
		if (event.isCancelled()) {
			if (mover != null && event.getDenyReason() != null) {
				try {
					mover.getCommunicator().sendAlertServerMessage(event.getDenyReason());
				} catch (Exception ignore) {}
			}
			return true;
		}
		return false;
	}

	/**
	 * Fires VillageExpansionCheckEvent. Returns true to abort the foundation/
	 * expansion. Uses reflection to read the private {@code expanding} flag.
	 */
	public boolean fireVillageExpansionCheck(com.wurmonline.server.questions.VillageFoundationQuestion question) {
		boolean expanding = false;
		try {
			java.lang.reflect.Field f = question.getClass().getDeclaredField("expanding");
			f.setAccessible(true);
			expanding = f.getBoolean(question);
		} catch (NoSuchFieldException | IllegalAccessException ignore) {}

		com.garward.wurmmodloader.api.events.village.VillageExpansionCheckEvent event =
			new com.garward.wurmmodloader.api.events.village.VillageExpansionCheckEvent(question, expanding);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] VillageExpansionCheckEvent: expanding=%s cancelled=%s",
				expanding, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires TerrainModificationEvent. Returns true to cancel the dig.
	 */
	public boolean fireTerrainModification(com.wurmonline.server.creatures.Creature performer,
	                                       com.wurmonline.server.items.Item tool,
	                                       int tileX, int tileY, int tile,
	                                       float counter, boolean corner) {
		com.garward.wurmmodloader.api.events.structure.TerrainModificationEvent event =
			new com.garward.wurmmodloader.api.events.structure.TerrainModificationEvent(
				performer, tool, tileX, tileY, tile, counter, corner);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] TerrainModificationEvent: tile=(%d,%d) cancelled=%s",
				tileX, tileY, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires GuardPlanPollEvent. Returns true to skip the vanilla upkeep drain.
	 */
	public boolean fireGuardPlanPoll(com.wurmonline.server.villages.GuardPlan plan) {
		com.garward.wurmmodloader.api.events.village.GuardPlanPollEvent event =
			new com.garward.wurmmodloader.api.events.village.GuardPlanPollEvent(plan);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] GuardPlanPollEvent: cancelled=%s", event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires StructurePlanningCheckEvent. Returns true to deny placement; sends
	 * the deny-reason to the performer as an alert if set.
	 */
	public boolean fireStructurePlanningCheck(com.wurmonline.server.creatures.Creature performer,
	                                          com.wurmonline.server.items.Item tool,
	                                          int tileX, int tileY, int tile) {
		com.garward.wurmmodloader.api.events.structure.StructurePlanningCheckEvent event =
			new com.garward.wurmmodloader.api.events.structure.StructurePlanningCheckEvent(
				performer, tool, tileX, tileY, tile);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] StructurePlanningCheckEvent: tile=(%d,%d) cancelled=%s reason=%s",
				tileX, tileY, event.isCancelled(), event.getDenyReason()));
		}
		if (event.isCancelled()) {
			if (performer != null && event.getDenyReason() != null) {
				try {
					performer.getCommunicator().sendAlertServerMessage(event.getDenyReason());
				} catch (Exception ignore) {}
			}
			return true;
		}
		return false;
	}

	/**
	 * Fires StructureGateCheckEvent. Returns true when a listener bypassed the gate.
	 */
	public boolean fireStructureGateCheck(com.wurmonline.server.creatures.Creature performer,
	                                      com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent.Subject subject,
	                                      com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent.Phase phase,
	                                      int heightOffset) {
		com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent event =
			new com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent(
				performer, subject, phase, heightOffset);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] StructureGateCheckEvent: subject=%s phase=%s bypass=%s",
				subject, phase, event.isBypassed()));
		}
		return event.isBypassed();
	}

	/**
	 * Fires QuestionAnswerEvent. Returns true to skip the vanilla
	 * {@code Question.answer(Properties)} dispatch.
	 */
	public boolean fireQuestionAnswer(com.wurmonline.server.questions.Question question,
	                                  java.util.Properties answers) {
		com.garward.wurmmodloader.api.events.player.QuestionAnswerEvent event =
			new com.garward.wurmmodloader.api.events.player.QuestionAnswerEvent(question, answers);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] QuestionAnswerEvent: type=%s cancelled=%s",
				question == null ? "null" : question.getClass().getSimpleName(), event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires SacrificePostEvent (observer-only, non-cancellable) after
	 * {@code MethodsReligion.sacrifice} resolves.
	 */
	public void fireSacrificePost(com.wurmonline.server.behaviours.Action action,
	                              com.wurmonline.server.creatures.Creature performer,
	                              com.wurmonline.server.items.Item altar,
	                              boolean done) {
		com.garward.wurmmodloader.api.events.priest.SacrificePostEvent event =
			new com.garward.wurmmodloader.api.events.priest.SacrificePostEvent(action, performer, altar, done);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] SacrificePostEvent: performer=%s done=%s",
				performer == null ? "null" : performer.getName(), done));
		}
	}

	/**
	 * Fires ContainerInsertionCheckEvent. Returns true to reject the insertion.
	 */
	public boolean fireContainerInsertionCheck(com.wurmonline.server.items.Item container,
	                                           com.wurmonline.server.items.Item incoming,
	                                           boolean testItemCount) {
		com.garward.wurmmodloader.api.events.item.ContainerInsertionCheckEvent event =
			new com.garward.wurmmodloader.api.events.item.ContainerInsertionCheckEvent(
				container, incoming, testItemCount);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] ContainerInsertionCheckEvent: container=%s incoming=%s cancelled=%s",
				container == null ? "null" : container.getName(),
				incoming == null ? "null" : incoming.getName(),
				event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires TrellisPruningEvent. Returns true to abort the prune.
	 */
	public boolean fireTrellisPruning(com.wurmonline.server.behaviours.Action action,
	                                  com.wurmonline.server.creatures.Creature performer,
	                                  com.wurmonline.server.items.Item sickle,
	                                  com.wurmonline.server.items.Item trellis,
	                                  float counter) {
		com.garward.wurmmodloader.api.events.farming.TrellisPruningEvent event =
			new com.garward.wurmmodloader.api.events.farming.TrellisPruningEvent(
				action, performer, sickle, trellis, counter);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] TrellisPruningEvent: performer=%s cancelled=%s",
				performer == null ? "null" : performer.getName(), event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires FaithGainResetEvent. Returns true to skip the vanilla reset.
	 */
	public boolean fireFaithGainReset() {
		com.garward.wurmmodloader.api.events.priest.FaithGainResetEvent event =
			new com.garward.wurmmodloader.api.events.priest.FaithGainResetEvent();
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] FaithGainResetEvent: cancelled=%s", event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires CreatureMovementSpeedEvent. Returns the (possibly modified) speed.
	 */
	public float fireCreatureMovementSpeed(com.wurmonline.server.creatures.Creature creature, float speed) {
		com.garward.wurmmodloader.api.events.creature.CreatureMovementSpeedEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureMovementSpeedEvent(creature, speed);
		eventBus.post(event);
		return event.getSpeed();
	}

	/**
	 * Fires CreatureIsFlyingEvent. Returns the (possibly flipped) flag.
	 */
	public boolean fireCreatureIsFlying(com.wurmonline.server.creatures.Creature creature, boolean flying) {
		com.garward.wurmmodloader.api.events.creature.CreatureIsFlyingEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureIsFlyingEvent(creature, flying);
		eventBus.post(event);
		return event.isFlying();
	}

	/**
	 * Fires PosZCalculationEvent. Returns the (possibly overridden) Z.
	 */
	public float firePosZCalculation(float posX, float posY,
	                                 com.wurmonline.server.zones.VolaTile tile,
	                                 boolean onSurface, boolean floating,
	                                 float currentPosZ,
	                                 com.wurmonline.server.creatures.Creature creature,
	                                 long bridgeId, float resolvedZ) {
		com.garward.wurmmodloader.api.events.movement.PosZCalculationEvent event =
			new com.garward.wurmmodloader.api.events.movement.PosZCalculationEvent(
				posX, posY, tile, onSurface, floating, currentPosZ, creature, bridgeId, resolvedZ);
		eventBus.post(event);
		return event.getResolvedZ();
	}

	/**
	 * Fires PathFinderCanPassEvent. Returns the (possibly overridden) passability.
	 */
	public boolean firePathFinderCanPass(com.wurmonline.server.creatures.Creature creature,
	                                     com.wurmonline.server.creatures.ai.PathTile from,
	                                     com.wurmonline.server.creatures.ai.PathTile to,
	                                     boolean canPass) {
		com.garward.wurmmodloader.api.events.movement.PathFinderCanPassEvent event =
			new com.garward.wurmmodloader.api.events.movement.PathFinderCanPassEvent(creature, from, to, canPass);
		eventBus.post(event);
		return event.canPass();
	}

	/**
	 * Fires CreatureSetTargetEvent. Returns the (possibly rewritten) target id,
	 * or {@link com.garward.wurmmodloader.api.events.creature.CreatureSetTargetEvent#CANCEL_SENTINEL}
	 * if the event was cancelled.
	 */
	public long fireCreatureSetTarget(com.wurmonline.server.creatures.Creature creature,
	                                  long targetId, boolean switchTarget) {
		com.garward.wurmmodloader.api.events.creature.CreatureSetTargetEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureSetTargetEvent(creature, targetId, switchTarget);
		eventBus.post(event);
		if (event.isCancelled()) {
			return com.garward.wurmmodloader.api.events.creature.CreatureSetTargetEvent.CANCEL_SENTINEL;
		}
		return event.getTargetId();
	}

	/**
	 * Fires CreatureMovementTickEvent. Returns {@code true} if cancelled.
	 */
	public boolean fireCreatureMovementTick(com.wurmonline.server.creatures.Creature creature,
	                                        boolean rotateFromBlocker) {
		com.garward.wurmmodloader.api.events.movement.CreatureMovementTickEvent event =
			new com.garward.wurmmodloader.api.events.movement.CreatureMovementTickEvent(creature, rotateFromBlocker);
		eventBus.post(event);
		return event.isCancelled();
	}

	/**
	 * Fires ZoneSpawnAttemptEvent. Returns {@code true} if cancelled.
	 */
	public boolean fireZoneSpawnAttempt(com.wurmonline.server.zones.Zone zone,
	                                    int tileX, int tileY, boolean spawnKingdom) {
		com.garward.wurmmodloader.api.events.creature.ZoneSpawnAttemptEvent event =
			new com.garward.wurmmodloader.api.events.creature.ZoneSpawnAttemptEvent(zone, tileX, tileY, spawnKingdom);
		eventBus.post(event);
		return event.isCancelled();
	}

	/**
	 * Fires CreaturePollMovementEvent. Returns the (possibly overridden) moved-flag.
	 */
	/**
	 * Fires CreaturePollMovementPreEvent. Returns {@code true} if cancelled.
	 */
	public boolean fireCreaturePollMovementPre(com.wurmonline.server.creatures.Creature creature, long delta) {
		com.garward.wurmmodloader.api.events.movement.CreaturePollMovementPreEvent event =
			new com.garward.wurmmodloader.api.events.movement.CreaturePollMovementPreEvent(creature, delta);
		eventBus.post(event);
		return event.isCancelled();
	}

	public boolean fireCreaturePollMovement(com.wurmonline.server.creatures.Creature creature,
	                                        long delta, boolean moved) {
		com.garward.wurmmodloader.api.events.movement.CreaturePollMovementEvent event =
			new com.garward.wurmmodloader.api.events.movement.CreaturePollMovementEvent(creature, delta, moved);
		eventBus.post(event);
		return event.didMove();
	}

	/**
	 * Fires CreatureTemplateColorEvent. Returns the (possibly modified) color value.
	 */
	public int fireCreatureTemplateColor(com.wurmonline.server.creatures.CreatureTemplate template,
	                                     com.garward.wurmmodloader.api.events.creature.CreatureTemplateColorEvent.Channel channel,
	                                     int value) {
		com.garward.wurmmodloader.api.events.creature.CreatureTemplateColorEvent event =
			new com.garward.wurmmodloader.api.events.creature.CreatureTemplateColorEvent(template, channel, value);
		eventBus.post(event);
		return event.getValue();
	}

	/**
	 * Fires TerrainFlattenEvent. Returns true to cancel the flatten.
	 */
	public boolean fireTerrainFlatten(com.wurmonline.server.creatures.Creature performer,
	                                  com.wurmonline.server.items.Item tool,
	                                  int tile, int tileX, int tileY,
	                                  float counter,
	                                  com.wurmonline.server.behaviours.Action action) {
		com.garward.wurmmodloader.api.events.structure.TerrainFlattenEvent event =
			new com.garward.wurmmodloader.api.events.structure.TerrainFlattenEvent(
				performer, tool, tile, tileX, tileY, counter, action);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] TerrainFlattenEvent: tile=(%d,%d) cancelled=%s",
				tileX, tileY, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires TerrainPackEvent. Returns true to cancel the pack.
	 */
	public boolean fireTerrainPack(com.wurmonline.server.creatures.Creature performer,
	                               com.wurmonline.server.items.Item tool,
	                               int tileX, int tileY, boolean onSurface,
	                               int tile, float counter,
	                               com.wurmonline.server.behaviours.Action action) {
		com.garward.wurmmodloader.api.events.structure.TerrainPackEvent event =
			new com.garward.wurmmodloader.api.events.structure.TerrainPackEvent(
				performer, tool, tileX, tileY, onSurface, tile, counter, action);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] TerrainPackEvent: tile=(%d,%d) cancelled=%s",
				tileX, tileY, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires TerrainCultivateEvent. Returns true to cancel the cultivate.
	 */
	public boolean fireTerrainCultivate(com.wurmonline.server.creatures.Creature performer,
	                                    com.wurmonline.server.items.Item tool,
	                                    int tileX, int tileY, boolean onSurface,
	                                    int tile, float counter) {
		com.garward.wurmmodloader.api.events.farming.TerrainCultivateEvent event =
			new com.garward.wurmmodloader.api.events.farming.TerrainCultivateEvent(
				performer, tool, tileX, tileY, onSurface, tile, counter);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] TerrainCultivateEvent: tile=(%d,%d) cancelled=%s",
				tileX, tileY, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires CaveMineEvent. Returns true to cancel the mine action
	 * (cancel returns true from mine() — action-loop done/abort semantics).
	 */
	public boolean fireCaveMine(com.wurmonline.server.behaviours.Action action,
	                            com.wurmonline.server.creatures.Creature performer,
	                            com.wurmonline.server.items.Item source,
	                            int tileX, int tileY, short mineAction,
	                            float counter, int dir,
	                            com.wurmonline.math.TilePos digTilePos) {
		com.garward.wurmmodloader.api.events.structure.CaveMineEvent event =
			new com.garward.wurmmodloader.api.events.structure.CaveMineEvent(
				action, performer, source, tileX, tileY, mineAction, counter, dir, digTilePos);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] CaveMineEvent: tile=(%d,%d) mineAction=%d cancelled=%s",
				tileX, tileY, mineAction, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires ActionAllowedOnVehicleEvent. Returns the possibly-overridden verdict.
	 */
	public boolean fireActionAllowedOnVehicle(short action, boolean vanillaAllowed) {
		com.garward.wurmmodloader.api.events.action.ActionAllowedOnVehicleEvent event =
			new com.garward.wurmmodloader.api.events.action.ActionAllowedOnVehicleEvent(action, vanillaAllowed);
		eventBus.post(event);
		return event.isAllowed();
	}

	/**
	 * Fires CaveTileActionEvent. Returns true to cancel (action-loop done/abort).
	 */
	public boolean fireCaveTileAction(com.wurmonline.server.behaviours.Action action,
	                                  com.wurmonline.server.creatures.Creature performer,
	                                  com.wurmonline.server.items.Item source,
	                                  int tileX, int tileY, boolean onSurface, int heightOffset,
	                                  int tile, int dir, short actionShort, float counter) {
		com.garward.wurmmodloader.api.events.structure.CaveTileActionEvent event =
			new com.garward.wurmmodloader.api.events.structure.CaveTileActionEvent(
				action, performer, source, tileX, tileY, onSurface, heightOffset,
				tile, dir, actionShort, counter);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] CaveTileActionEvent: tile=(%d,%d) action=%d cancelled=%s",
				tileX, tileY, actionShort, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires CaveTileGetBehavioursEvent. Listeners mutate the live entries list.
	 */
	public void fireCaveTileGetBehaviours(com.wurmonline.server.creatures.Creature performer,
	                                      com.wurmonline.server.items.Item source,
	                                      int tileX, int tileY, boolean onSurface, int tile, int dir,
	                                      java.util.List<com.wurmonline.server.behaviours.ActionEntry> entries) {
		com.garward.wurmmodloader.api.events.structure.CaveTileGetBehavioursEvent event =
			new com.garward.wurmmodloader.api.events.structure.CaveTileGetBehavioursEvent(
				performer, source, tileX, tileY, onSurface, tile, dir, entries);
		eventBus.post(event);
	}

	/**
	 * Fires SurfaceRockActionEvent. Returns true to cancel (action-loop done/abort).
	 */
	public boolean fireSurfaceRockAction(com.wurmonline.server.behaviours.Action action,
	                                     com.wurmonline.server.creatures.Creature performer,
	                                     com.wurmonline.server.items.Item source,
	                                     int tileX, int tileY, boolean onSurface, int heightOffset,
	                                     int tile, short actionShort, float counter) {
		com.garward.wurmmodloader.api.events.structure.SurfaceRockActionEvent event =
			new com.garward.wurmmodloader.api.events.structure.SurfaceRockActionEvent(
				action, performer, source, tileX, tileY, onSurface, heightOffset,
				tile, actionShort, counter);
		eventBus.post(event);
		if (DEBUG) {
			logger.info(String.format("[Event] SurfaceRockActionEvent: tile=(%d,%d) action=%d cancelled=%s",
				tileX, tileY, actionShort, event.isCancelled()));
		}
		return event.isCancelled();
	}

	/**
	 * Fires SurfaceMiningSlopeLowerCheckEvent and resolves the override. Returns
	 * the per-tick decision: lower-slope (true) or chip-away (false). When no
	 * listener overrides, the vanilla roll ({@code Server.rand.nextFloat()
	 * &lt; naturalChance}) is performed inline so this method always answers
	 * with the same semantics vanilla would.
	 */
	public boolean fireSurfaceMiningSlopeLower(com.wurmonline.server.creatures.Creature performer,
	                                            com.wurmonline.server.items.Item source,
	                                            float naturalChance) {
		com.garward.wurmmodloader.api.events.structure.SurfaceMiningSlopeLowerCheckEvent event =
			new com.garward.wurmmodloader.api.events.structure.SurfaceMiningSlopeLowerCheckEvent(
				performer, source, naturalChance);
		eventBus.post(event);
		Boolean override = event.getOverride();
		boolean decision = override != null
			? override.booleanValue()
			: (com.wurmonline.server.Server.rand.nextFloat() < naturalChance);
		if (DEBUG) {
			logger.info(String.format("[Event] SurfaceMiningSlopeLowerCheck: chance=%.3f override=%s -> %s",
				naturalChance, override, decision));
		}
		return decision;
	}

	/**
	 * Fires SurfaceMiningSurroundCheckEvent. Returns {@code true} if vanilla
	 * abort should run (and emits the vanilla message itself in that case),
	 * {@code false} if a listener overrode the check to allow mining despite
	 * non-rock surroundings.
	 */
	public boolean fireSurfaceMiningSurroundCheck(com.wurmonline.server.creatures.Creature performer,
	                                              com.wurmonline.server.items.Item source) {
		com.garward.wurmmodloader.api.events.structure.SurfaceMiningSurroundCheckEvent event =
			new com.garward.wurmmodloader.api.events.structure.SurfaceMiningSurroundCheckEvent(
				performer, source);
		eventBus.post(event);
		Boolean override = event.getOverride();
		boolean bypass = Boolean.TRUE.equals(override);
		if (DEBUG) {
			logger.info(String.format("[Event] SurfaceMiningSurroundCheck: override=%s bypass=%s",
				override, bypass));
		}
		if (bypass) {
			return false;
		}
		try {
			performer.getCommunicator().sendNormalServerMessage(
				"The surrounding area needs to be rock before you mine.", (byte) 3);
		} catch (Throwable ignore) {}
		return true;
	}

	/**
	 * Fires SurfaceRockGetBehavioursEvent. Listeners mutate the live entries list.
	 */
	public void fireSurfaceRockGetBehaviours(com.wurmonline.server.creatures.Creature performer,
	                                         com.wurmonline.server.items.Item source,
	                                         int tileX, int tileY, boolean onSurface, int tile,
	                                         java.util.List<com.wurmonline.server.behaviours.ActionEntry> entries) {
		com.garward.wurmmodloader.api.events.structure.SurfaceRockGetBehavioursEvent event =
			new com.garward.wurmmodloader.api.events.structure.SurfaceRockGetBehavioursEvent(
				performer, source, tileX, tileY, onSurface, tile, entries);
		eventBus.post(event);
	}

	/**
	 * Fires DirtDestinationResolveEvent and returns the (possibly replaced)
	 * target item. Returning {@code null} means "use vanillaTarget".
	 */
	public com.wurmonline.server.items.Item fireDirtDestinationResolve(
			com.wurmonline.server.items.Item dirt,
			com.wurmonline.server.creatures.Creature performer,
			com.wurmonline.server.items.Item tool,
			com.wurmonline.server.items.Item vanillaTarget,
			boolean dredging, boolean toPile, String contextName) {
		com.garward.wurmmodloader.api.events.farming.DirtDestinationResolveEvent.Context ctx =
			com.garward.wurmmodloader.api.events.farming.DirtDestinationResolveEvent.Context.valueOf(contextName);
		com.garward.wurmmodloader.api.events.farming.DirtDestinationResolveEvent event =
			new com.garward.wurmmodloader.api.events.farming.DirtDestinationResolveEvent(
				dirt, performer, tool, vanillaTarget, dredging, toPile, ctx);
		eventBus.post(event);
		return event.getResolvedTarget();
	}

	/**
	 * Fires DirtSourceResolveEvent and returns the (possibly replaced)
	 * carried-item lookup result.
	 */
	public com.wurmonline.server.items.Item fireDirtSourceResolve(
			com.wurmonline.server.creatures.Creature performer,
			int templateId,
			com.wurmonline.server.items.Item vanillaFound,
			String contextName) {
		com.garward.wurmmodloader.api.events.farming.DirtSourceResolveEvent.Context ctx =
			com.garward.wurmmodloader.api.events.farming.DirtSourceResolveEvent.Context.valueOf(contextName);
		com.garward.wurmmodloader.api.events.farming.DirtSourceResolveEvent event =
			new com.garward.wurmmodloader.api.events.farming.DirtSourceResolveEvent(
				performer, templateId, vanillaFound, ctx);
		eventBus.post(event);
		return event.getResolvedItem();
	}

	/**
	 * Fires DigCapacityOverrideEvent. vanillaValue is 1/0 for boolean gates,
	 * raw int for count/volume gates. Returns overridden value (same marshal).
	 */
	public long fireDigCapacityOverride(
			com.wurmonline.server.creatures.Creature performer,
			com.wurmonline.server.items.Item tool,
			com.wurmonline.server.items.Item target,
			String kindName, long vanillaValue, boolean toPile, boolean dredging) {
		com.garward.wurmmodloader.api.events.farming.DigCapacityOverrideEvent.Kind kind =
			com.garward.wurmmodloader.api.events.farming.DigCapacityOverrideEvent.Kind.valueOf(kindName);
		com.garward.wurmmodloader.api.events.farming.DigCapacityOverrideEvent event =
			new com.garward.wurmmodloader.api.events.farming.DigCapacityOverrideEvent(
				performer, tool, target, kind, vanillaValue, toPile, dredging);
		eventBus.post(event);
		return event.getOverrideValue();
	}

	/**
	 * Fires ActionPerformRequestEvent. Returns {@code true} if cancelled.
	 */
	public boolean fireActionPerformRequest(com.wurmonline.server.creatures.Creature performer,
			long subjectWurmId, long targetWurmId, short actionShort) {
		com.garward.wurmmodloader.api.events.action.ActionPerformRequestEvent event =
			new com.garward.wurmmodloader.api.events.action.ActionPerformRequestEvent(
				performer, subjectWurmId, targetWurmId, actionShort);
		eventBus.post(event);
		return event.isCancelled();
	}

	/**
	 * Fires ActionMenuBuildEvent so listeners can mutate the live
	 * availableActions list before it ships to the client.
	 */
	public void fireActionMenuBuild(com.wurmonline.server.creatures.Communicator communicator,
			java.util.List<com.wurmonline.server.behaviours.ActionEntry> availableActions,
			String helpString, boolean sendToSelectBar) {
		com.garward.wurmmodloader.api.events.action.ActionMenuBuildEvent event =
			new com.garward.wurmmodloader.api.events.action.ActionMenuBuildEvent(
				communicator, availableActions, helpString, sendToSelectBar);
		eventBus.post(event);
	}

	/**
	 * Fires TileMenuBuildEvent. Target-aware tile menu injection path.
	 */
	public void fireTileMenuBuild(com.wurmonline.server.creatures.Creature performer,
			long target, boolean onSurface, com.wurmonline.server.items.Item source,
			java.util.List<com.wurmonline.server.behaviours.ActionEntry> availableActions,
			String helpString) {
		com.garward.wurmmodloader.api.events.action.TileMenuBuildEvent event =
			new com.garward.wurmmodloader.api.events.action.TileMenuBuildEvent(
				performer, target, onSurface, source, availableActions, helpString);
		eventBus.post(event);
	}

	/**
	 * Fires ItemMenuBuildEvent. Target-aware item menu injection path.
	 */
	public void fireItemMenuBuild(com.wurmonline.server.creatures.Creature performer,
			long targetId, com.wurmonline.server.items.Item source,
			java.util.List<com.wurmonline.server.behaviours.ActionEntry> availableActions,
			String helpString) {
		com.garward.wurmmodloader.api.events.action.ItemMenuBuildEvent event =
			new com.garward.wurmmodloader.api.events.action.ItemMenuBuildEvent(
				performer, targetId, source, availableActions, helpString);
		eventBus.post(event);
	}

	/**
	 * Fires TileDirtConsumeEvent. Returns {@code true} if a listener claimed
	 * consumption of the dirt pile.
	 */
	public boolean fireTileDirtConsume(com.wurmonline.server.behaviours.Action action,
			com.wurmonline.server.creatures.Creature performer,
			com.wurmonline.server.items.Item source) {
		com.garward.wurmmodloader.api.events.farming.TileDirtConsumeEvent event =
			new com.garward.wurmmodloader.api.events.farming.TileDirtConsumeEvent(
				action, performer, source);
		eventBus.post(event);
		return event.isConsumed();
	}

	/**
	 * Fires PlanterItemAcceptEvent. Returns the (possibly overridden) accepted flag.
	 */
	public boolean firePlanterItemAccept(com.wurmonline.server.creatures.Creature performer,
			com.wurmonline.server.items.Item herb,
			com.wurmonline.server.items.Item planter,
			String kindName, boolean vanillaValue) {
		com.garward.wurmmodloader.api.events.farming.PlanterItemAcceptEvent.Kind kind =
			com.garward.wurmmodloader.api.events.farming.PlanterItemAcceptEvent.Kind.valueOf(kindName);
		com.garward.wurmmodloader.api.events.farming.PlanterItemAcceptEvent event =
			new com.garward.wurmmodloader.api.events.farming.PlanterItemAcceptEvent(
				performer, herb, planter, kind, vanillaValue);
		eventBus.post(event);
		return event.isAccepted();
	}

	/**
	 * Fires BulkStackNameEvent. Returns the canonical bulk-stacking name.
	 */
	public String fireBulkStackName(com.wurmonline.server.items.Item item, String vanillaName) {
		com.garward.wurmmodloader.api.events.item.BulkStackNameEvent event =
			new com.garward.wurmmodloader.api.events.item.BulkStackNameEvent(item, vanillaName);
		eventBus.post(event);
		return event.getResolvedName();
	}

}

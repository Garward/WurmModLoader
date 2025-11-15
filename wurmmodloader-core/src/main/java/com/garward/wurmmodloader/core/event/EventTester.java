package com.garward.wurmmodloader.core.event;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dynamically discovers and fires all events for testing purposes.
 *
 * <p>This utility class helps prevent event bugs by:
 * <ul>
 *   <li>Scanning all event classes in the API</li>
 *   <li>Creating test instances with mock/null data</li>
 *   <li>Firing each event through the EventBus</li>
 *   <li>Logging success/failure for each event</li>
 * </ul>
 *
 * <p>Usage: Enable with {@code --test-events} or {@code --test-events=ON_PLAYER_LOGIN}
 *
 * @since 1.0.0
 */
public class EventTester {

    private static final Logger LOGGER = Logger.getLogger(EventTester.class.getName());
    private static final String EVENT_PACKAGE = "com.garward.wurmmodloader.api.events";

    private final EventBus eventBus;
    private final TriggerMode mode;

    /**
     * When to trigger event testing.
     */
    public enum TriggerMode {
        /** Fire all events immediately after server starts */
        ON_SERVER_START,
        /** Fire all events when first player logs in */
        ON_PLAYER_LOGIN,
        /** Only fire events when explicitly requested */
        MANUAL
    }

    /**
     * Creates an event tester with the specified trigger mode.
     *
     * @param mode when to trigger event testing
     */
    public EventTester(TriggerMode mode) {
        this.eventBus = EventBus.getInstance();
        this.mode = mode;
    }

    /**
     * Scans and fires all discoverable events.
     *
     * @return number of events successfully fired
     */
    public int fireAllEvents() {
        LOGGER.info("==================================================");
        LOGGER.info("EVENT TESTER: Discovering and firing all events");
        LOGGER.info("Trigger mode: " + mode);
        LOGGER.info("==================================================");

        List<Class<?>> eventClasses = discoverEventClasses();
        LOGGER.info("Discovered " + eventClasses.size() + " event classes");

        int successCount = 0;
        int failureCount = 0;

        for (Class<?> eventClass : eventClasses) {
            try {
                Event event = createTestEvent(eventClass);
                if (event != null) {
                    LOGGER.info("Firing test event: " + eventClass.getSimpleName());
                    eventBus.post(event);
                    successCount++;
                    LOGGER.info("  ✓ Successfully fired " + eventClass.getSimpleName());
                } else {
                    LOGGER.warning("  ✗ Could not create test instance for " + eventClass.getSimpleName());
                    failureCount++;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "  ✗ Failed to fire " + eventClass.getSimpleName(), e);
                failureCount++;
            }
        }

        LOGGER.info("==================================================");
        LOGGER.info("EVENT TESTER COMPLETE:");
        LOGGER.info("  Success: " + successCount + " events");
        LOGGER.info("  Failure: " + failureCount + " events");
        LOGGER.info("==================================================");

        return successCount;
    }

    /**
     * Discovers all Event subclasses in the API package.
     *
     * @return list of event classes
     */
    private List<Class<?>> discoverEventClasses() {
        List<Class<?>> eventClasses = new ArrayList<>();

        // Use the reliable fallback list - works in all scenarios (dev, JAR, etc.)
        scanPackageForEvents(EVENT_PACKAGE, eventClasses);

        if (eventClasses.isEmpty()) {
            LOGGER.warning("No event classes discovered! Check EventTester configuration.");
        }

        return eventClasses;
    }

    /**
     * Recursively scans a directory for Event classes.
     */
    private void scanDirectory(File directory, String packageName, List<Class<?>> eventClasses) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), eventClasses);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (isConcreteEventClass(clazz)) {
                        eventClasses.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Skip classes that can't be loaded
                    LOGGER.fine("Could not load class: " + className);
                }
            }
        }
    }

    /**
     * Scans a JAR file for Event classes.
     */
    private void scanJarForEvents(URL resource, List<Class<?>> eventClasses) {
        // For JAR scanning, we'll use a simpler approach
        // List known event packages explicitly
        String[] subpackages = {
            "action", "base", "combat", "creature", "item",
            "player", "server", "skill", "vehicle",
            "item.material", "combat.shield", "combat.weapon"
        };

        for (String subpackage : subpackages) {
            String fullPackage = EVENT_PACKAGE + "." + subpackage;
            try {
                // Try to load known event classes from each subpackage
                // This is a simplified approach - we can enhance it later
                scanPackageForEvents(fullPackage, eventClasses);
            } catch (Exception e) {
                LOGGER.fine("Could not scan package: " + fullPackage);
            }
        }
    }

    /**
     * Attempts to load event classes from a package.
     */
    private void scanPackageForEvents(String packageName, List<Class<?>> eventClasses) {
        // Fallback: manually registered event classes
        // This ensures events are discoverable even when running from JARs
        String[] knownEvents = {
            // Action events
            "com.garward.wurmmodloader.api.events.action.ActionSpeedModifierEvent",
            "com.garward.wurmmodloader.api.events.action.ActionTimeCalculationEvent",

            // Combat events
            "com.garward.wurmmodloader.api.events.combat.CombatAttackEvent",
            "com.garward.wurmmodloader.api.events.combat.CombatCriticalHitEvent",
            "com.garward.wurmmodloader.api.events.combat.CombatDualWieldEvent",
            "com.garward.wurmmodloader.api.events.combat.CombatSwingSpeedEvent",
            "com.garward.wurmmodloader.api.events.combat.OpportunityAttackEvent",
            "com.garward.wurmmodloader.api.events.combat.SpecialMoveHandleEvent",
            "com.garward.wurmmodloader.api.events.combat.SpecialMoveSendEvent",
            "com.garward.wurmmodloader.api.events.combat.WeaponUseEvent",

            // Combat shield events
            "com.garward.wurmmodloader.api.events.combat.shield.ShieldCheckEvent",
            "com.garward.wurmmodloader.api.events.combat.shield.ShieldDamageEvent",

            // Combat weapon events
            "com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent",

            // Creature events
            "com.garward.wurmmodloader.api.events.creature.CombatDamageEvent",
            "com.garward.wurmmodloader.api.events.creature.CreatureDeathEvent",
            "com.garward.wurmmodloader.api.events.creature.CreatureSpawnEvent",
            "com.garward.wurmmodloader.api.events.creature.MountEquipmentCheckEvent",

            // Item events
            "com.garward.wurmmodloader.api.events.item.ItemDropEvent",
            "com.garward.wurmmodloader.api.events.item.ItemEnchantmentStringsEvent",
            "com.garward.wurmmodloader.api.events.item.ItemExamineEvent",
            "com.garward.wurmmodloader.api.events.item.ItemTemplatesCreatedEvent",
            "com.garward.wurmmodloader.api.events.item.ItemTradeEvent",

            // Item material events
            "com.garward.wurmmodloader.api.events.item.material.MaterialBonusEvent",
            "com.garward.wurmmodloader.api.events.item.material.MaterialDamageModifierEvent",
            "com.garward.wurmmodloader.api.events.item.material.MaterialDecayModifierEvent",
            "com.garward.wurmmodloader.api.events.item.material.MaterialImpBonusEvent",
            "com.garward.wurmmodloader.api.events.item.material.MaterialRepairTimeEvent",

            // Player events
            "com.garward.wurmmodloader.api.events.player.ChannelMessageEvent",
            "com.garward.wurmmodloader.api.events.player.PlayerDeathEvent",
            "com.garward.wurmmodloader.api.events.player.PlayerLoginEvent",
            "com.garward.wurmmodloader.api.events.player.PlayerLogoutEvent",
            "com.garward.wurmmodloader.api.events.player.PlayerMessageEvent",
            "com.garward.wurmmodloader.api.events.player.PlayerSkillLossEvent",

            // Server events
            "com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent",
            "com.garward.wurmmodloader.api.events.server.ServerPollEvent",
            "com.garward.wurmmodloader.api.events.server.ServerStartedEvent",
            "com.garward.wurmmodloader.api.events.server.ServerStoppingEvent",

            // Skill events
            "com.garward.wurmmodloader.api.events.skill.SkillAdvanceEvent",
            "com.garward.wurmmodloader.api.events.skill.SkillCheckEvent",

            // Vehicle events
            "com.garward.wurmmodloader.api.events.vehicle.VehicleMountEvent",
            "com.garward.wurmmodloader.api.events.vehicle.VehicleSpeedCalculationEvent"
        };

        for (String className : knownEvents) {
            try {
                Class<?> clazz = Class.forName(className);
                if (isConcreteEventClass(clazz) && !eventClasses.contains(clazz)) {
                    eventClasses.add(clazz);
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // Skip events that can't be loaded
                LOGGER.fine("Could not load event class: " + className);
            }
        }
    }

    /**
     * Checks if a class is a concrete (non-abstract) Event subclass.
     */
    private boolean isConcreteEventClass(Class<?> clazz) {
        return Event.class.isAssignableFrom(clazz)
            && !Modifier.isAbstract(clazz.getModifiers())
            && !Modifier.isInterface(clazz.getModifiers())
            && !clazz.equals(Event.class);
    }

    /**
     * Creates a test instance of an event with mock data.
     *
     * @param eventClass the event class to instantiate
     * @return test event instance, or null if cannot create
     */
    @SuppressWarnings("unchecked")
    private Event createTestEvent(Class<?> eventClass) {
        try {
            // Try constructors in order of preference
            Constructor<?>[] constructors = eventClass.getDeclaredConstructors();

            // Sort constructors by parameter count (prefer fewer parameters)
            java.util.Arrays.sort(constructors, (a, b) ->
                Integer.compare(a.getParameterCount(), b.getParameterCount()));

            for (Constructor<?> constructor : constructors) {
                try {
                    constructor.setAccessible(true);
                    Object[] args = createMockArguments(constructor.getParameterTypes());
                    return (Event) constructor.newInstance(args);
                } catch (Exception e) {
                    // Try next constructor
                    continue;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not create test event for " + eventClass.getName(), e);
        }

        return null;
    }

    /**
     * Creates mock arguments for a constructor.
     */
    private Object[] createMockArguments(Class<?>[] parameterTypes) {
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = createMockValue(parameterTypes[i]);
        }

        return args;
    }

    /**
     * Creates a mock value for a parameter type.
     */
    private Object createMockValue(Class<?> type) {
        // Primitives
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        if (type == char.class) return '\0';

        // Strings
        if (type == String.class) return "test";

        // Wurm classes - use null (events should handle null checks)
        if (Creature.class.isAssignableFrom(type)) return null;
        if (Player.class.isAssignableFrom(type)) return null;
        if (Item.class.isAssignableFrom(type)) return null;

        // Arrays
        if (type.isArray()) {
            return java.lang.reflect.Array.newInstance(type.getComponentType(), 0);
        }

        // Everything else - null
        return null;
    }

    /**
     * Gets the trigger mode for this tester.
     *
     * @return trigger mode
     */
    public TriggerMode getMode() {
        return mode;
    }

    /**
     * Parses a trigger mode from a string.
     *
     * @param value string value (e.g., "ON_SERVER_START")
     * @return trigger mode, or ON_SERVER_START if invalid
     */
    public static TriggerMode parseTriggerMode(String value) {
        if (value == null || value.isEmpty()) {
            return TriggerMode.ON_SERVER_START;
        }

        try {
            return TriggerMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid trigger mode: " + value + ", defaulting to ON_SERVER_START");
            return TriggerMode.ON_SERVER_START;
        }
    }
}

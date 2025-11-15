package com.garward.wurmmodloader.mods.oversizedclub.capability;

import com.garward.wurmmodloader.api.capability.Capability;
import com.garward.wurmmodloader.api.registry.ResourceLocation;

/**
 * ═════════════════════════════════════════════════════════════════════════
 * TUTORIAL: Capabilities System - Attaching Custom Data to Game Objects
 * ═════════════════════════════════════════════════════════════════════════
 *
 * <p><b>What is a Capability?</b></p>
 * A capability is a type-safe way to attach custom data to game objects (Items, Players,
 * Creatures, Tiles) without managing your own database code. The framework handles:
 * - Lazy loading (only loads when first accessed)
 * - Automatic persistence (saves to database when modified)
 * - Thread-safe storage (ConcurrentHashMap under the hood)
 * - Namespaced IDs (prevents conflicts with other mods)
 *
 * <p><b>How to Create a Capability:</b></p>
 * <ol>
 *   <li>Create a data class (e.g., {@link ItemLevel}) with your custom fields</li>
 *   <li>Create a capability class (this file) implementing {@link Capability}</li>
 *   <li>Register it in {@code @SubscribeEvent} handler for {@link com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent}</li>
 *   <li>Access it anywhere: {@code item.getCapability(ItemLevelCapability.INSTANCE)}</li>
 * </ol>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Register capability during server startup
 * @SubscribeEvent
 * public void onCapabilityRegistration(CapabilityRegistrationEvent event) {
 *     event.registerItemCapability(ItemLevelCapability.INSTANCE);
 * }
 *
 * // Use capability anywhere in your code
 * @SubscribeEvent
 * public void onItemExamine(ItemExamineEvent event) {
 *     Item item = event.getItem();
 *
 *     // Cast to ICapabilityProvider (injected by framework via Javassist)
 *     ICapabilityProvider provider = (ICapabilityProvider) item;
 *
 *     // Get capability data (lazy loaded from database if needed)
 *     ItemLevel level = provider.getCapability(ItemLevelCapability.INSTANCE);
 *
 *     // Modify data - framework automatically marks as dirty and saves
 *     level.addExperience(50);
 *
 *     // Display in examine text
 *     event.addDescription("\n[" + level.toString() + "]");
 * }
 * }</pre>
 *
 * <p><b>Why Use Capabilities Instead of Manual Database Code?</b></p>
 * <ul>
 *   <li>No more writing database schemas, DAOs, managers (~1000 lines saved!)</li>
 *   <li>Framework handles ALL persistence automatically</li>
 *   <li>Type-safe (compiler catches errors)</li>
 *   <li>Namespaced (no conflicts with other mods)</li>
 *   <li>Professional SQLite config (WAL mode, busy timeout, indexes)</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0 (Phase 5.5)
 * @see ItemLevel The data class this capability manages
 * @see com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent Where to register capabilities
 */
public class ItemLevelCapability implements Capability<ItemLevel> {

    // ═════════════════════════════════════════════════════════════════════
    // STEP 1: Create a singleton instance
    // ═════════════════════════════════════════════════════════════════════
    // Use this INSTANCE when registering and accessing the capability
    /** Singleton instance - use this everywhere to reference this capability */
    public static final ItemLevelCapability INSTANCE = new ItemLevelCapability();

    // ═════════════════════════════════════════════════════════════════════
    // STEP 2: Define a unique ID using ResourceLocation
    // ═════════════════════════════════════════════════════════════════════
    // Format: "yourmodname:capability_name"
    // This prevents conflicts with other mods' capabilities
    /** Unique identifier in namespace:path format (prevents mod conflicts) */
    private static final ResourceLocation ID = new ResourceLocation("oversizedclub", "item_level");

    private ItemLevelCapability() {
        // Private constructor - forces use of INSTANCE singleton
    }

    // ═════════════════════════════════════════════════════════════════════
    // STEP 3: Implement Capability<T> interface methods
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Returns the data class type this capability manages.
     * Used by the framework for type-safe casting.
     */
    @Override
    public Class<ItemLevel> getType() {
        return ItemLevel.class;
    }

    /**
     * Returns the unique ID for this capability.
     * Must be globally unique across all mods (use your mod's namespace!).
     */
    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * Creates a new default instance when capability is first accessed.
     * Called when:
     * - Item/Player/Creature/Tile doesn't have this capability yet
     * - Database doesn't have data for this entity
     *
     * @return Default instance (Level 1, 0 XP in this example)
     */
    @Override
    public ItemLevel createDefaultInstance() {
        // New items start at level 1 with 0 experience
        return new ItemLevel();
    }

    /**
     * Converts capability data to a string for database storage.
     * Choose a format that's easy to parse:
     * - CSV (simple, used here): "1,0"
     * - JSON (complex objects): "{\"level\":1,\"exp\":0}"
     * - Custom delimited: "level=1;exp=0"
     *
     * Framework calls this when saving to database.
     *
     * @param instance The capability data to serialize
     * @return String representation for database storage
     */
    @Override
    public String serialize(ItemLevel instance) {
        // Simple CSV format: level,experience
        return instance.getLevel() + "," + instance.getExperience();
    }

    /**
     * Converts database string back to capability data object.
     * Must be able to parse the format from serialize().
     *
     * Framework calls this when loading from database.
     *
     * IMPORTANT: Always handle parse errors gracefully!
     * - Return createDefaultInstance() if parsing fails
     * - Don't throw exceptions (would crash server on corrupted data)
     *
     * @param data String from database (from serialize())
     * @return Capability data object, or default if parsing fails
     */
    @Override
    public ItemLevel deserialize(String data) {
        try {
            // Parse CSV format: "level,experience"
            String[] parts = data.split(",");
            int level = Integer.parseInt(parts[0]);
            int exp = Integer.parseInt(parts[1]);
            return new ItemLevel(level, exp);
        } catch (Exception e) {
            // If deserialization fails (corrupted data, wrong format, etc.)
            // Return default instead of crashing the server
            return createDefaultInstance();
        }
    }
}

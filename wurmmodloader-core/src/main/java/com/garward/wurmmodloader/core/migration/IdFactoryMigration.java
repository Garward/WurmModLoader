package com.garward.wurmmodloader.core.migration;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.registry.IdAllocator;
import com.garward.wurmmodloader.modsupport.ModSupportDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migrates ID allocations from the old SQL-based IdFactory to the new JSON-based IdAllocator.
 *
 * <p>This migration utility ensures that existing mods using the legacy IdFactory
 * system can seamlessly transition to the new registry-based system without losing
 * their ID allocations.</p>
 *
 * <h2>Migration Process</h2>
 * <ol>
 *   <li>Reads all ID mappings from ModSupportDb SQL table (IDS)</li>
 *   <li>Converts string identifiers to ResourceLocation format</li>
 *   <li>Imports IDs into IdAllocator's JSON storage</li>
 *   <li>Preserves all existing ID assignments</li>
 * </ol>
 *
 * <h2>Data Safety</h2>
 * <ul>
 *   <li>Original SQL data is never deleted (read-only)</li>
 *   <li>Migration is idempotent (safe to run multiple times)</li>
 *   <li>Existing JSON allocations are not overwritten</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Migrate all ID types
 * IdFactoryMigration.migrateAllTypes();
 *
 * // Migrate specific type
 * IdFactoryMigration.migrateType("ITEMTEMPLATE");
 * }</pre>
 *
 * @since 2.0.0
 */
public class IdFactoryMigration {

    private static final Logger logger = Logger.getLogger(IdFactoryMigration.class.getName());
    private static boolean migrationCompleted = false;

    /**
     * Migrates all ID types from SQL to JSON.
     *
     * <p>This method reads all entries from the ModSupportDb IDS table and imports them
     * into the IdAllocator JSON storage. It handles all ID types (ITEMTEMPLATE,
     * CREATURETEMPLATE, etc.) in a single operation.</p>
     *
     * <p>The migration is safe to run multiple times - existing allocations in JSON
     * will not be overwritten.</p>
     *
     * @return number of IDs successfully migrated
     */
    public static synchronized int migrateAllTypes() {
        if (migrationCompleted) {
            logger.info("Migration already completed in this session");
            return 0;
        }

        logger.info("Starting ID migration from SQL (IdFactory) to JSON (IdAllocator)");
        int totalMigrated = 0;

        try (Connection dbcon = ModSupportDb.getModSupportDb()) {
            if (!ModSupportDb.hasTable(dbcon, "IDS")) {
                logger.info("No IDS table found - nothing to migrate");
                migrationCompleted = true;
                return 0;
            }

            Map<String, Integer> sqlMappings = readAllIdsFromSql(dbcon);
            logger.info(String.format("Found %d ID mappings in SQL database", sqlMappings.size()));

            IdAllocator allocator = IdAllocator.getInstance();
            int migrated = 0;
            int skipped = 0;

            for (Map.Entry<String, Integer> entry : sqlMappings.entrySet()) {
                String identifier = entry.getKey();
                int id = entry.getValue();

                try {
                    // Convert old identifier to ResourceLocation
                    ResourceLocation location = convertIdentifierToResourceLocation(identifier);

                    // Check if already exists in JSON
                    int existingId = allocator.allocate(location);

                    if (existingId == id) {
                        // ID matches - either already migrated or consistent
                        migrated++;
                    } else {
                        // ID mismatch - log warning but don't overwrite
                        logger.warning(String.format(
                            "ID mismatch for %s: SQL=%d, JSON=%d (keeping JSON value)",
                            identifier, id, existingId
                        ));
                        skipped++;
                    }

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to migrate: " + identifier, e);
                    skipped++;
                }
            }

            // Save migrated data
            try {
                allocator.save();
            } catch (java.io.IOException e) {
                logger.log(Level.SEVERE, "Failed to save migrated IDs to JSON", e);
                throw new RuntimeException("Failed to save migrated IDs", e);
            }

            totalMigrated = migrated;
            logger.info(String.format(
                "Migration complete: %d migrated, %d skipped, %d total",
                migrated, skipped, sqlMappings.size()
            ));

            migrationCompleted = true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL migration failed", e);
            throw new RuntimeException("Failed to migrate IDs from SQL to JSON", e);
        }

        return totalMigrated;
    }

    /**
     * Reads all ID mappings from the SQL database.
     *
     * @param dbcon database connection
     * @return map of identifier -> ID
     * @throws SQLException if database read fails
     */
    private static Map<String, Integer> readAllIdsFromSql(Connection dbcon) throws SQLException {
        Map<String, Integer> mappings = new HashMap<>();

        String query = "SELECT NAME, TYPE, ID FROM IDS ORDER BY TYPE, ID";
        try (PreparedStatement ps = dbcon.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("NAME");
                String type = rs.getString("TYPE");
                int id = rs.getInt("ID");

                // Create composite key: type:name
                String key = type + ":" + name;
                mappings.put(key, id);

                logger.fine(String.format("Read from SQL: %s -> %d", key, id));
            }
        }

        return mappings;
    }

    /**
     * Converts old string identifier to ResourceLocation format.
     *
     * <p>Handles various identifier formats:</p>
     * <ul>
     *   <li>"mymod.item" -> ResourceLocation("wurm", "mymod.item")</li>
     *   <li>"mymod:item" -> ResourceLocation("mymod", "item")</li>
     *   <li>"ITEMTEMPLATE:mymod.item" -> ResourceLocation("wurm", "mymod.item")</li>
     * </ul>
     *
     * @param identifier old-style identifier
     * @return ResourceLocation
     */
    private static ResourceLocation convertIdentifierToResourceLocation(String identifier) {
        // Strip type prefix if present (e.g., "ITEMTEMPLATE:mymod.item")
        if (identifier.contains(":")) {
            String[] parts = identifier.split(":", 2);
            // Check if first part is a type name (all uppercase)
            if (parts[0].equals(parts[0].toUpperCase()) && parts.length == 2) {
                identifier = parts[1];
            }
        }

        // Use ResourceLocation.of() to handle namespace parsing
        return ResourceLocation.of(identifier);
    }

    /**
     * Checks if migration is needed.
     *
     * @return true if SQL database has IDs that need migration
     */
    public static boolean isMigrationNeeded() {
        try (Connection dbcon = ModSupportDb.getModSupportDb()) {
            if (!ModSupportDb.hasTable(dbcon, "IDS")) {
                return false;
            }

            // Check if table has any data
            try (PreparedStatement ps = dbcon.prepareStatement("SELECT COUNT(*) FROM IDS");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Could not check if migration is needed", e);
        }

        return false;
    }

    /**
     * Gets migration statistics.
     *
     * @return map of statistics
     */
    public static Map<String, Integer> getMigrationStats() {
        Map<String, Integer> stats = new HashMap<>();

        try (Connection dbcon = ModSupportDb.getModSupportDb()) {
            if (ModSupportDb.hasTable(dbcon, "IDS")) {
                // Count SQL entries
                try (PreparedStatement ps = dbcon.prepareStatement("SELECT COUNT(*) FROM IDS");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.put("sqlEntries", rs.getInt(1));
                    }
                }

                // Count by type
                try (PreparedStatement ps = dbcon.prepareStatement(
                        "SELECT TYPE, COUNT(*) as CNT FROM IDS GROUP BY TYPE");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("TYPE");
                        int count = rs.getInt("CNT");
                        stats.put("sql_" + type, count);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Could not get migration stats", e);
        }

        // Get JSON entries count
        stats.put("jsonEntries", IdAllocator.getInstance().getAllocations().size());
        stats.put("migrationCompleted", migrationCompleted ? 1 : 0);

        return stats;
    }

    /**
     * Resets migration state (for testing).
     */
    static void resetMigrationState() {
        migrationCompleted = false;
    }
}

package com.garward.wurmmodloader.mods.upgradetree;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all upgrades and player progress.
 * Loads upgrade definitions from JSON config.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class UpgradeTreeManager {
    private static final Logger logger = Logger.getLogger(UpgradeTreeManager.class.getName());
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static UpgradeTreeManager instance;

    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();
    private final Map<Long, Set<String>> playerUpgrades = new ConcurrentHashMap<>();
    private int upgradesPerPage = 5;

    private UpgradeTreeManager() {
        // Private constructor for singleton
    }

    public static UpgradeTreeManager getInstance() {
        if (instance == null) {
            instance = new UpgradeTreeManager();
        }
        return instance;
    }

    /**
     * Load upgrades from JSON config file.
     */
    public void loadUpgradesFromConfig(File configFile) {
        logger.info("[UpgradeTree] Loading upgrades from: " + configFile.getAbsolutePath());

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            // Load config settings
            if (root.has("config")) {
                JsonObject config = root.getAsJsonObject("config");
                if (config.has("upgradesPerPage")) {
                    upgradesPerPage = config.get("upgradesPerPage").getAsInt();
                }
            }

            // Load upgrades
            if (root.has("upgrades")) {
                JsonArray upgradesArray = root.getAsJsonArray("upgrades");
                for (JsonElement element : upgradesArray) {
                    Upgrade upgrade = parseUpgrade(element.getAsJsonObject());
                    upgrades.put(upgrade.getId(), upgrade);
                    logger.info("[UpgradeTree] Loaded upgrade: " + upgrade.getId() + " (" + upgrade.getName() + ")");
                }
            }

            logger.info("[UpgradeTree] Loaded " + upgrades.size() + " upgrades");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[UpgradeTree] Failed to load upgrades config", e);
        }
    }

    /**
     * Parse a single upgrade from JSON.
     */
    private Upgrade parseUpgrade(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        String description = json.get("description").getAsString();
        int tier = json.get("tier").getAsInt();
        int cost = json.get("cost").getAsInt();

        // Parse requirements
        List<String> requirements = new ArrayList<>();
        if (json.has("requirements")) {
            JsonArray reqArray = json.getAsJsonArray("requirements");
            for (JsonElement req : reqArray) {
                requirements.add(req.getAsString());
            }
        }

        int minUpgradesRequired = 0;
        if (json.has("minUpgradesRequired")) {
            minUpgradesRequired = json.get("minUpgradesRequired").getAsInt();
        }

        // Parse effects
        List<UpgradeEffect> effects = new ArrayList<>();
        if (json.has("effects")) {
            JsonArray effectsArray = json.getAsJsonArray("effects");
            for (JsonElement effectEl : effectsArray) {
                UpgradeEffect effect = parseEffect(effectEl.getAsJsonObject());
                effects.add(effect);
            }
        }

        return new Upgrade(id, name, description, tier, cost, requirements, minUpgradesRequired, effects);
    }

    /**
     * Parse a single effect from JSON.
     */
    private UpgradeEffect parseEffect(JsonObject json) {
        String type = json.get("type").getAsString();
        Map<String, Object> parameters = new HashMap<>();

        // Extract all non-type, non-comment fields as parameters
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            if (key.equals("type") || key.equals("comment")) {
                continue;
            }

            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                if (value.getAsJsonPrimitive().isNumber()) {
                    parameters.put(key, value.getAsNumber());
                } else if (value.getAsJsonPrimitive().isBoolean()) {
                    parameters.put(key, value.getAsBoolean());
                } else {
                    parameters.put(key, value.getAsString());
                }
            }
        }

        return new UpgradeEffect(type, parameters);
    }

    /**
     * Get all available upgrades.
     */
    public Collection<Upgrade> getAllUpgrades() {
        return upgrades.values();
    }

    /**
     * Get a specific upgrade by ID.
     */
    public Upgrade getUpgrade(String id) {
        return upgrades.get(id);
    }

    /**
     * Get upgrades for a specific tier.
     */
    public List<Upgrade> getUpgradesForTier(int tier) {
        List<Upgrade> result = new ArrayList<>();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.getTier() == tier) {
                result.add(upgrade);
            }
        }
        return result;
    }

    /**
     * Check if a player has unlocked a specific upgrade.
     */
    public boolean hasUpgrade(long playerId, String upgradeId) {
        Set<String> playerUpgradeSet = playerUpgrades.get(playerId);
        if (playerUpgradeSet == null) {
            // Load from database
            loadPlayerUpgrades(playerId);
            playerUpgradeSet = playerUpgrades.get(playerId);
        }
        return playerUpgradeSet != null && playerUpgradeSet.contains(upgradeId);
    }

    /**
     * Get all unlocked upgrades for a player.
     */
    public Set<String> getPlayerUpgrades(long playerId) {
        Set<String> playerUpgradeSet = playerUpgrades.get(playerId);
        if (playerUpgradeSet == null) {
            loadPlayerUpgrades(playerId);
            playerUpgradeSet = playerUpgrades.get(playerId);
        }
        return playerUpgradeSet != null ? new HashSet<>(playerUpgradeSet) : new HashSet<>();
    }

    /**
     * Get count of unlocked upgrades for a player.
     */
    public int getPlayerUpgradeCount(long playerId) {
        return getPlayerUpgrades(playerId).size();
    }

    /**
     * Check if player meets all requirements for an upgrade.
     */
    public boolean meetsRequirements(long playerId, Upgrade upgrade) {
        // Check prerequisite upgrades
        for (String reqId : upgrade.getRequirements()) {
            if (!hasUpgrade(playerId, reqId)) {
                return false;
            }
        }

        // Check minimum upgrade count requirement
        if (upgrade.getMinUpgradesRequired() > 0) {
            int count = getPlayerUpgradeCount(playerId);
            if (count < upgrade.getMinUpgradesRequired()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Unlock an upgrade for a player.
     * NOTE: This does NOT deduct power - that must be done separately!
     */
    public boolean unlockUpgrade(long playerId, String upgradeId) {
        Upgrade upgrade = getUpgrade(upgradeId);
        if (upgrade == null) {
            logger.warning("[UpgradeTree] Unknown upgrade: " + upgradeId);
            return false;
        }

        if (hasUpgrade(playerId, upgradeId)) {
            logger.warning("[UpgradeTree] Player " + playerId + " already has " + upgradeId);
            return false;
        }

        // Save to database
        try (Connection conn = getDbConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO player_upgrades (player_id, upgrade_id, unlocked_at) VALUES (?, ?, ?)"
            );
            ps.setLong(1, playerId);
            ps.setString(2, upgradeId);
            ps.setLong(3, System.currentTimeMillis() / 1000);
            ps.executeUpdate();

            // Update cache
            Set<String> playerUpgradeSet = playerUpgrades.computeIfAbsent(playerId, k -> new HashSet<>());
            playerUpgradeSet.add(upgradeId);

            logger.info("[UpgradeTree] Player " + playerId + " unlocked " + upgradeId);
            return true;

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[UpgradeTree] Failed to unlock upgrade", e);
            return false;
        }
    }

    /**
     * Load player's unlocked upgrades from database.
     */
    private void loadPlayerUpgrades(long playerId) {
        Set<String> playerUpgradeSet = new HashSet<>();

        try (Connection conn = getDbConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT upgrade_id FROM player_upgrades WHERE player_id = ?"
            );
            ps.setLong(1, playerId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                playerUpgradeSet.add(rs.getString("upgrade_id"));
            }

            playerUpgrades.put(playerId, playerUpgradeSet);
            logger.fine("[UpgradeTree] Loaded " + playerUpgradeSet.size() + " upgrades for player " + playerId);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[UpgradeTree] Failed to load player upgrades", e);
            playerUpgrades.put(playerId, new HashSet<>());
        }
    }

    /**
     * Get database connection (uses SQLite file in mods directory).
     *
     * <p>This mod uses its own isolated database rather than the shared modsupport
     * database to keep upgrade data self-contained within the mod's directory.</p>
     *
     * @return active database connection
     * @throws SQLException if connection fails
     */
    private Connection getDbConnection() throws SQLException {
        File dbFile = new File("mods/upgradetree/upgradetree.db");
        dbFile.getParentFile().mkdirs();  // Ensure directory exists
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try {
            // Ensure SQLite driver is loaded (following framework pattern)
            Class.forName(SQLITE_DRIVER);
            return java.sql.DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Failed to load SQLite driver", e);
        }
    }

    /**
     * Initialize database tables.
     */
    public void initDatabase() {
        try (Connection conn = getDbConnection()) {
            // Player upgrades table
            PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_upgrades (" +
                "    player_id INTEGER NOT NULL," +
                "    upgrade_id TEXT NOT NULL," +
                "    unlocked_at INTEGER," +
                "    PRIMARY KEY (player_id, upgrade_id)" +
                ")"
            );
            ps.executeUpdate();

            logger.info("[UpgradeTree] Database initialized");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[UpgradeTree] Failed to initialize database", e);
        }
    }

    public int getUpgradesPerPage() {
        return upgradesPerPage;
    }

    /**
     * Clear all cached player data (for testing/reload).
     */
    public void clearCache() {
        playerUpgrades.clear();
    }
}

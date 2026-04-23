package com.garward.wurmmodloader.core.console;

import java.lang.reflect.Method;

/**
 * Reflection utilities for safely accessing Wurm server classes.
 *
 * <p>All GM console commands use reflection to access Wurm's internal classes
 * without direct compilation dependencies. This ensures the modloader can work
 * across different Wurm versions and maintains classloader isolation.</p>
 *
 * <p><strong>Classloader Safety:</strong> All reflection happens in the modloader's
 * classloader, not Wurm's system classloader. This prevents crashes and ensures
 * proper isolation.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ServerReflectionUtil {

    /**
     * Get the Players singleton instance.
     *
     * @return Players instance (as Object - cast not needed)
     * @throws Exception if reflection fails
     */
    public static Object getPlayersInstance() throws Exception {
        Class<?> playersClass = Class.forName("com.wurmonline.server.Players");
        Method getInstance = playersClass.getMethod("getInstance");
        return getInstance.invoke(null);
    }

    /**
     * Get all online players.
     *
     * @return Array of Player objects
     * @throws Exception if reflection fails
     */
    public static Object[] getOnlinePlayers() throws Exception {
        Object playersInstance = getPlayersInstance();
        Method getPlayers = playersInstance.getClass().getMethod("getPlayers");
        return (Object[]) getPlayers.invoke(playersInstance);
    }

    /**
     * Get a player by name.
     *
     * @param playerName Player name (case-insensitive)
     * @return Player object or null if not found
     * @throws Exception if reflection fails
     */
    public static Object getPlayerByName(String playerName) throws Exception {
        Object playersInstance = getPlayersInstance();
        Method getPlayer = playersInstance.getClass().getMethod("getPlayer", String.class);
        return getPlayer.invoke(playersInstance, playerName);
    }

    /**
     * Get player's name.
     *
     * @param player Player object
     * @return Player name
     * @throws Exception if reflection fails
     */
    public static String getPlayerName(Object player) throws Exception {
        if (player == null) return null;
        Method getName = player.getClass().getMethod("getName");
        return (String) getName.invoke(player);
    }

    /**
     * Get player's GM power level.
     *
     * @param player Player object
     * @return Power level (0-5, where 5 is highest GM)
     * @throws Exception if reflection fails
     */
    public static int getPlayerPower(Object player) throws Exception {
        if (player == null) return 0;
        Method getPower = player.getClass().getMethod("getPower");
        return (int) getPower.invoke(player);
    }

    /**
     * Kick a player from the server.
     *
     * <p>Disconnects the player's communicator, forcing them to disconnect.</p>
     *
     * @param player Player object to kick
     * @throws Exception if reflection fails
     */
    public static void kickPlayer(Object player) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        // Get the player's communicator
        Method getCommunicator = player.getClass().getMethod("getCommunicator");
        Object communicator = getCommunicator.invoke(player);

        if (communicator == null) {
            throw new IllegalStateException("Player has no communicator (already offline?)");
        }

        // Disconnect the communicator
        Method disconnect = communicator.getClass().getMethod("disconnect");
        disconnect.invoke(communicator);
    }

    /**
     * Get the Server singleton instance.
     *
     * @return Server instance
     * @throws Exception if reflection fails
     */
    public static Object getServerInstance() throws Exception {
        Class<?> serverClass = Class.forName("com.wurmonline.server.Server");
        Method getInstance = serverClass.getMethod("getInstance");
        return getInstance.invoke(null);
    }

    /**
     * Check if server is running.
     *
     * @return true if server is running, false otherwise
     */
    public static boolean isServerRunning() {
        try {
            Object server = getServerInstance();
            return server != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Set player's GM power level.
     *
     * @param player Player object
     * @param powerLevel Power level (0-5)
     * @throws Exception if reflection fails
     */
    public static void setPlayerPower(Object player, int powerLevel) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (powerLevel < 0 || powerLevel > 5) {
            throw new IllegalArgumentException("Power level must be 0-5");
        }

        Method setPower = player.getClass().getMethod("setPower", byte.class);
        setPower.invoke(player, (byte) powerLevel);
    }

    /**
     * Ban a player by name.
     *
     * @param playerName Player name to ban
     * @param reason Ban reason
     * @param days Days to ban (0 = permanent)
     * @throws Exception if reflection fails
     */
    public static void banPlayer(String playerName, String reason, int days) throws Exception {
        Class<?> playersClass = Class.forName("com.wurmonline.server.Players");
        Method banMethod = playersClass.getMethod("ban", String.class, String.class, int.class);
        banMethod.invoke(null, playerName, reason, days);
    }

    /**
     * Get current Wurm time.
     *
     * @return Current Wurm time
     * @throws Exception if reflection fails
     */
    public static long getWurmTime() throws Exception {
        Class<?> wurmCalendarClass = Class.forName("com.wurmonline.server.WurmCalendar");
        java.lang.reflect.Field currentTimeField = wurmCalendarClass.getField("currentTime");
        return (long) currentTimeField.get(null);
    }

    /**
     * Set Wurm time.
     *
     * @param time New Wurm time
     * @throws Exception if reflection fails
     */
    public static void setWurmTime(long time) throws Exception {
        Class<?> wurmCalendarClass = Class.forName("com.wurmonline.server.WurmCalendar");
        java.lang.reflect.Field currentTimeField = wurmCalendarClass.getField("currentTime");
        currentTimeField.set(null, time);
    }

    /**
     * Get player position X.
     *
     * @param player Player object
     * @return X coordinate
     * @throws Exception if reflection fails
     */
    public static float getPlayerPosX(Object player) throws Exception {
        Method getPosX = player.getClass().getMethod("getPosX");
        return (float) getPosX.invoke(player);
    }

    /**
     * Get player position Y.
     *
     * @param player Player object
     * @return Y coordinate
     * @throws Exception if reflection fails
     */
    public static float getPlayerPosY(Object player) throws Exception {
        Method getPosY = player.getClass().getMethod("getPosY");
        return (float) getPosY.invoke(player);
    }

    /**
     * Set player position (teleport).
     *
     * @param player Player object
     * @param x X coordinate
     * @param y Y coordinate
     * @throws Exception if reflection fails
     */
    public static void setPlayerPosition(Object player, float x, float y) throws Exception {
        // Set the teleport destination
        Method setTeleportPoints = player.getClass().getMethod("setTeleportPoints", float.class, float.class, int.class, int.class);
        setTeleportPoints.invoke(player, x, y, 0, 0);

        // Actually trigger the teleport
        Method startTeleporting = player.getClass().getMethod("startTeleporting");
        startTeleporting.invoke(player);

        // Sync the client position (critical for client-side rendering)
        Method getCommunicator = player.getClass().getMethod("getCommunicator");
        Object communicator = getCommunicator.invoke(player);
        Method sendTeleport = communicator.getClass().getMethod("sendTeleport", boolean.class);
        sendTeleport.invoke(communicator, false);
    }

    /**
     * Schedule server shutdown.
     *
     * @param minutes Minutes until shutdown
     * @param reason Shutdown reason
     * @throws Exception if reflection fails
     */
    public static void scheduleShutdown(int minutes, String reason) throws Exception {
        Object server = getServerInstance();
        Method startShutdown = server.getClass().getMethod("startShutdown", int.class, String.class);
        startShutdown.invoke(server, minutes * 60, reason);
    }

    /**
     * Get number of online players.
     *
     * @return Player count
     * @throws Exception if reflection fails
     */
    public static int getOnlinePlayerCount() throws Exception {
        Object[] players = getOnlinePlayers();
        return players != null ? players.length : 0;
    }

    /**
     * Create item for player.
     *
     * @param player Player object
     * @param templateId Item template ID
     * @param quality Quality level (1-100)
     * @return Created item object or null if failed
     * @throws Exception if reflection fails
     */
    public static Object createItemForPlayer(Object player, int templateId, float quality) throws Exception {
        // ItemFactory.createItem is static — no singleton. Use the (int, float, String) overload
        // that creates the item without a world position, then insert into the player's inventory.
        Class<?> itemFactoryClass = Class.forName("com.wurmonline.server.items.ItemFactory");
        Method createItem = itemFactoryClass.getMethod("createItem", int.class, float.class, String.class);
        Object item = createItem.invoke(null, templateId, quality, null);

        Method getInventory = player.getClass().getMethod("getInventory");
        Object inventory = getInventory.invoke(player);

        Class<?> itemClass = Class.forName("com.wurmonline.server.items.Item");
        Method insertItem = itemClass.getMethod("insertItem", itemClass);
        insertItem.invoke(inventory, item);

        return item;
    }

    /**
     * Give skill to player.
     *
     * @param player Player object
     * @param skillId Skill ID
     * @param amount Amount to set (0-100)
     * @throws Exception if reflection fails
     */
    public static void giveSkill(Object player, int skillId, double amount) throws Exception {
        // Get player skills
        Method getSkills = player.getClass().getMethod("getSkills");
        Object skills = getSkills.invoke(player);

        // Get or learn skill
        Method getSkillOrLearn = skills.getClass().getMethod("getSkillOrLearn", int.class);
        Object skill = getSkillOrLearn.invoke(skills, skillId);

        // Set skill level.
        // Skill.setKnowledge gates its entire update block (in-memory assign + save +
        // Communicator.sendUpdateSkill) behind `if (aKnowledge < 100.0)`. Passing exactly
        // 100 is a silent no-op — no DB write, no live client update. Clamp just below.
        double clamped = Math.min(amount, 99.9999);
        Method setKnowledge = skill.getClass().getMethod("setKnowledge", double.class, boolean.class);
        setKnowledge.invoke(skill, clamped, false);
    }

    /**
     * Spawn creature at position.
     *
     * @param templateName Creature template name (e.g., "troll", "spider")
     * @param x X coordinate
     * @param y Y coordinate
     * @param layer Layer (0 = surface, -1 = cave)
     * @return Spawned creature object or null
     * @throws Exception if reflection fails
     */
    public static Object spawnCreature(String templateName, float x, float y, int layer) throws Exception {
        // Get CreatureTemplateFactory
        Class<?> templateFactoryClass = Class.forName("com.wurmonline.server.creatures.CreatureTemplateFactory");
        Method getInstance = templateFactoryClass.getMethod("getInstance");
        Object templateFactory = getInstance.invoke(null);

        // Get template by name
        Method getTemplate = templateFactoryClass.getMethod("getTemplate", String.class);
        Object template = getTemplate.invoke(templateFactory, templateName);

        // Get template ID
        Method getTemplateId = template.getClass().getMethod("getTemplateId");
        int templateId = (int) getTemplateId.invoke(template);

        // Spawn creature
        Class<?> creatureClass = Class.forName("com.wurmonline.server.creatures.Creature");
        Method doNew = creatureClass.getMethod("doNew", int.class, float.class, float.class, float.class, int.class, String.class, byte.class);
        Object creature = doNew.invoke(null, templateId, x, y, 0f, layer, templateName, (byte) 0);

        return creature;
    }

    /**
     * Set weather type.
     *
     * @param weatherType Weather type (0=clear, 1=light, 2=medium, 3=heavy rain/snow)
     * @throws Exception if reflection fails
     */
    public static void setWeather(int weatherType) throws Exception {
        Class<?> serverClass = Class.forName("com.wurmonline.server.Server");
        Object server = getServerInstance();

        Method setWeather = serverClass.getMethod("setWeather", int.class);
        setWeather.invoke(server, weatherType);
    }

    /**
     * Get player's skill level.
     *
     * @param player Player object
     * @param skillId Skill ID
     * @return Skill level or 0 if not found
     * @throws Exception if reflection fails
     */
    public static double getSkillLevel(Object player, int skillId) throws Exception {
        Method getSkills = player.getClass().getMethod("getSkills");
        Object skills = getSkills.invoke(player);

        Method getSkill = skills.getClass().getMethod("getSkill", int.class);
        Object skill = getSkill.invoke(skills, skillId);

        if (skill == null) {
            return 0.0;
        }

        Method getKnowledge = skill.getClass().getMethod("getKnowledge");
        return (double) getKnowledge.invoke(skill);
    }

    /**
     * Send message to player.
     *
     * @param player Player object
     * @param message Message to send
     * @throws Exception if reflection fails
     */
    public static void sendMessage(Object player, String message) throws Exception {
        Method getCommunicator = player.getClass().getMethod("getCommunicator");
        Object communicator = getCommunicator.invoke(player);

        Method sendNormalServerMessage = communicator.getClass().getMethod("sendNormalServerMessage", String.class);
        sendNormalServerMessage.invoke(communicator, message);
    }

    /**
     * Toggle global chat.
     *
     * @param enabled true to enable, false to disable
     * @throws Exception if reflection fails
     */
    public static void toggleGlobalChat(boolean enabled) throws Exception {
        Class<?> playersClass = Class.forName("com.wurmonline.server.Players");
        Method toggleGlobal = playersClass.getMethod("toggleGlobalChat", boolean.class);
        toggleGlobal.invoke(null, enabled);
    }

    /**
     * Get all creature template names.
     *
     * @return List of creature template names
     * @throws Exception if reflection fails
     */
    public static java.util.List<String> getAllCreatureTemplateNames() throws Exception {
        Class<?> templateFactoryClass = Class.forName("com.wurmonline.server.creatures.CreatureTemplateFactory");
        Method getInstance = templateFactoryClass.getMethod("getInstance");
        Object templateFactory = getInstance.invoke(null);

        // Get all templates
        Method getTemplates = templateFactoryClass.getMethod("getTemplates");
        Object templatesArray = getTemplates.invoke(templateFactory);

        java.util.List<String> names = new java.util.ArrayList<>();
        if (templatesArray != null && templatesArray.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(templatesArray);
            for (int i = 0; i < length; i++) {
                Object template = java.lang.reflect.Array.get(templatesArray, i);
                if (template != null) {
                    Method getName = template.getClass().getMethod("getName");
                    String name = (String) getName.invoke(template);
                    if (name != null && !name.isEmpty()) {
                        names.add(name.toLowerCase());
                    }
                }
            }
        }

        return names;
    }

    /**
     * Get all item template IDs and names.
     *
     * @return Map of template ID to name
     * @throws Exception if reflection fails
     */
    public static java.util.Map<Integer, String> getAllItemTemplates() throws Exception {
        Class<?> itemTemplateFactoryClass = Class.forName("com.wurmonline.server.items.ItemTemplateFactory");
        Method getInstance = itemTemplateFactoryClass.getMethod("getInstance");
        Object factory = getInstance.invoke(null);

        java.util.Map<Integer, String> templates = new java.util.HashMap<>();

        try {
            // Get all templates - Wurm stores these in a HashMap internally
            java.lang.reflect.Field templatesField = itemTemplateFactoryClass.getDeclaredField("templates");
            templatesField.setAccessible(true);
            Object templatesMap = templatesField.get(factory);

            if (templatesMap instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<Integer, Object> map = (java.util.Map<Integer, Object>) templatesMap;

                for (java.util.Map.Entry<Integer, Object> entry : map.entrySet()) {
                    Object template = entry.getValue();
                    if (template != null) {
                        Method getName = template.getClass().getMethod("getName");
                        String name = (String) getName.invoke(template);
                        templates.put(entry.getKey(), name);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: just log that we couldn't get all templates
            System.out.println("[Console GM] Could not load full item template list: " + e.getMessage());
        }

        return templates;
    }

    /**
     * Get all skill IDs and names.
     *
     * @return Map of skill ID to name
     * @throws Exception if reflection fails
     */
    public static java.util.Map<Integer, String> getAllSkills() throws Exception {
        Class<?> skillsClass = Class.forName("com.wurmonline.server.skills.SkillList");
        java.util.Map<Integer, String> skills = new java.util.HashMap<>();

        try {
            java.lang.reflect.Field[] fields = skillsClass.getDeclaredFields();
            int modMask = java.lang.reflect.Modifier.STATIC | java.lang.reflect.Modifier.FINAL;

            for (java.lang.reflect.Field field : fields) {
                if ((field.getModifiers() & modMask) != modMask) continue;
                if (field.getType() != int.class) continue;

                String fieldName = field.getName();
                if (fieldName.startsWith("GROUP_")) continue;

                try {
                    int skillId = field.getInt(null);
                    // Skip sentinel category IDs (e.g. SKILLS=Integer.MAX_VALUE, FAITH=0x7FFFFFFD)
                    if (skillId < 0 || skillId > 100000) continue;

                    String skillName = fieldName.replace("SKILL_", "").replace("_", " ").toLowerCase();
                    skills.put(skillId, skillName);
                } catch (Exception ignored) {
                    // Skip fields we can't access
                }
            }
        } catch (Exception e) {
            System.out.println("[Console GM] Could not load full skill list: " + e.getMessage());
        }

        return skills;
    }

    /**
     * Find item template by name (fuzzy match).
     *
     * @param name Item name to search for
     * @return Template ID or -1 if not found
     * @throws Exception if reflection fails
     */
    public static int findItemTemplateByName(String name) throws Exception {
        java.util.Map<Integer, String> templates = getAllItemTemplates();
        String normalizedSearch = name.toLowerCase();

        // First try exact match
        for (java.util.Map.Entry<Integer, String> entry : templates.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }

        // Then try contains match
        for (java.util.Map.Entry<Integer, String> entry : templates.entrySet()) {
            if (entry.getValue().toLowerCase().contains(normalizedSearch)) {
                return entry.getKey();
            }
        }

        return -1;
    }

    /**
     * Find skill ID by name (fuzzy match).
     *
     * @param name Skill name to search for
     * @return Skill ID or -1 if not found
     * @throws Exception if reflection fails
     */
    public static int findSkillByName(String name) throws Exception {
        java.util.Map<Integer, String> skills = getAllSkills();
        String normalizedSearch = name.toLowerCase();

        // First try exact match
        for (java.util.Map.Entry<Integer, String> entry : skills.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }

        // Then try contains match
        for (java.util.Map.Entry<Integer, String> entry : skills.entrySet()) {
            if (entry.getValue().toLowerCase().contains(normalizedSearch)) {
                return entry.getKey();
            }
        }

        return -1;
    }
}

package com.garward.wurmmodloader.config;

/**
 * Root server configuration data model.
 *
 * <p>This class holds all server configuration settings that sync to the SERVERS table.
 * All fields are mutable to allow YAML deserialization.</p>
 *
 * <p><strong>Classloader Isolation:</strong> This class and all nested classes are loaded
 * in the modloader's classloader ONLY. They have NO dependencies on Wurm classes.</p>
 *
 * <p><strong>Initialization Timing:</strong> Config loading occurs AFTER:
 * <ul>
 *   <li>Bytecode patches are applied</li>
 *   <li>Capabilities and registries are initialized</li>
 *   <li>Database connections are fully ready</li>
 * </ul>
 *
 * <p><strong>===== WURM UNLIMITED GM COMMANDS REFERENCE =====</strong></p>
 * <p>All GM commands start with # and require appropriate power level (1-5, where 5 is highest).
 * These are in-game commands, NOT console commands. Console only has "shutdown".</p>
 *
 * <p><strong>PLAYER MANAGEMENT:</strong></p>
 * <ul>
 *   <li>#kick &lt;player&gt; - Kick player from server</li>
 *   <li>#ban &lt;player&gt; &lt;reason&gt; - Ban player permanently</li>
 *   <li>#pardon &lt;player&gt; - Remove ban</li>
 *   <li>#mute &lt;player&gt; - Mute player in chat</li>
 *   <li>#unmute &lt;player&gt; - Unmute player</li>
 *   <li>#who - List online players</li>
 *   <li>#plimit &lt;number&gt; - Set max player limit</li>
 *   <li>#invuln - Toggle GM invulnerability</li>
 *   <li>#invis - Toggle GM invisibility</li>
 * </ul>
 *
 * <p><strong>SERVER CONTROL:</strong></p>
 * <ul>
 *   <li>#sdown &lt;seconds&gt; &lt;reason&gt; - Schedule server shutdown</li>
 *   <li>#b &lt;message&gt; - Broadcast message to all players</li>
 *   <li>#a &lt;message&gt; - Announcement (red text)</li>
 *   <li>#timemod &lt;value&gt; - Modify time speed</li>
 *   <li>#setserver - Modify server settings (use with caution)</li>
 * </ul>
 *
 * <p><strong>WORLD MANAGEMENT:</strong></p>
 * <ul>
 *   <li>#maxcreatures &lt;number&gt; - Set max creature count</li>
 *   <li>#respawn &lt;type&gt; - Respawn creatures of type</li>
 *   <li>#uniques - List unique creatures</li>
 *   <li>#startx/starty - Set spawn coordinates</li>
 *   <li>#flattenRock - Flatten rock layer (power 4+)</li>
 *   <li>#flattenDirt - Flatten dirt layer (power 4+)</li>
 * </ul>
 *
 * <p><strong>DEBUGGING:</strong></p>
 * <ul>
 *   <li>#lagstatus - Show server performance</li>
 *   <li>#checkCreatures - Verify creature data</li>
 *   <li>#checkItems - Verify item data</li>
 *   <li>#locateitem &lt;id&gt; - Find item by ID</li>
 *   <li>#locatehorse &lt;name&gt; - Find horse by name</li>
 * </ul>
 *
 * <p><strong>See SERVER_CONFIG_RESEARCH.md for complete command documentation.</strong></p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ServerConfig {

    /**
     * Config schema version for future migrations.
     * Current version: 1
     */
    public int version = 1;

    /**
     * Server identity settings (name, MOTD, etc.)
     */
    public ServerIdentityConfig server = new ServerIdentityConfig();

    /**
     * Spawn point coordinates for new players
     */
    public SpawnConfig spawns = new SpawnConfig();

    /**
     * Skill and progression settings
     */
    public SkillsConfig skills = new SkillsConfig();

    /**
     * Combat and action settings
     */
    public CombatConfig combat = new CombatConfig();

    /**
     * Creature settings
     */
    public CreaturesConfig creatures = new CreaturesConfig();

    /**
     * World settings (growth, terrain, etc.)
     */
    public WorldConfig world = new WorldConfig();

    /**
     * Economy settings (deeds, traders, upkeep)
     */
    public EconomyConfig economy = new EconomyConfig();

    /**
     * Player settings
     */
    public PlayersConfig players = new PlayersConfig();

    /**
     * Server properties (SERVERPROPERTIES table settings)
     */
    public ServerPropertiesConfig properties = new ServerPropertiesConfig();

    /**
     * Default constructor for YAML deserialization.
     */
    public ServerConfig() {
        // Intentionally empty - fields have default initializers
    }

    /**
     * Server identity configuration.
     */
    public static class ServerIdentityConfig {
        // ===================================================================
        // BASIC IDENTITY
        // ===================================================================

        /** Server display name (shown in server browser) */
        public String name = "Wurm Unlimited Server";

        /** Message of the Day (shown to players on login) */
        public String motd = "";

        /** Map name (e.g., "Adventure", "Riverweave", "Creative") */
        public String mapName = "";

        /** Steam server password (leave empty for no password) */
        public String steamPassword = "";

        /** Server password for player connections (leave empty for no password) */
        public String serverPassword = "";

        /** Home server kingdom (1=Jenn-Kellon, 2=Mol-Rehan, 3=HOTS/Libila, 4=Freedom) */
        public byte homeServerKingdom = 1;

        // ===================================================================
        // NETWORK CONFIGURATION
        // ===================================================================

        /** External IP address (public IP for players to connect) */
        public String externalIp = "127.0.1.1";

        /** External port (public port for players to connect) */
        public String externalPort = "3724";

        /** Internal/Intra-server IP address (for server-to-server communication) */
        public String internalIp = "127.0.0.1";

        /** Internal/Intra-server port */
        public String internalPort = "48020";

        /** RMI (Remote Method Invocation) port */
        public String rmiPort = "7220";

        /** RMI registration port */
        public String rmiRegPort = "7221";

        /** Intra-server password (for server cluster authentication) */
        public String intraServerPassword = "";

        /** Maximum number of players allowed on the server */
        public int maxPlayers = 200;

        /**
         * Default kingdom for players (1=Freedom/Jenn-Kellon, 2=Mol-Rehan, 3=HOTS, 4=Freedom).
         * On Freedom servers, players can choose any kingdom. On PvP servers, this determines
         * which kingdom new players join by default.
         */
        public byte kingdom = 4;

        // ===================================================================
        // SERVER TYPE FLAGS
        // ===================================================================
        // These determine the server's gameplay mode and rules

        /**
         * PvP server (enables player vs player combat everywhere).
         * PvP servers have kingdoms at war, deed raiding, and full loot on death.
         * Freedom (non-PvP) servers have safe zones and limited PvP areas.
         */
        public boolean pvp = false;

        /**
         * Epic server (connects to Epic cluster with mission system).
         * Epic servers share a mission system and have special scenarios.
         * Requires Epic server cluster configuration.
         */
        public boolean epic = false;

        /**
         * Challenge server (temporary competitive servers with leaderboards).
         * Challenge servers reset after a fixed duration and track player achievements.
         * Used for seasonal competitions.
         */
        public boolean challenge = false;

        /**
         * Home server (players can set this as their home).
         * Players can only have one home server. Home servers allow faster
         * respawn and bind location mechanics.
         */
        public boolean homeServer = true;

        /**
         * Entry server (new players can spawn here).
         * Entry servers are the first servers new accounts connect to.
         * Usually Freedom servers with newbie protection.
         */
        public boolean entryServer = false;

        /**
         * Login server (handles authentication for server cluster).
         * Only one server in a cluster should be the login server.
         * Required for multi-server setups.
         */
        public boolean loginServer = false;

        /**
         * Test server (for development and testing).
         * Test servers have special admin commands and debugging enabled.
         * Not recommended for production use.
         */
        public boolean isTest = false;

        /**
         * Local server (single-player or LAN only).
         * Local servers don't connect to external services and have
         * simplified networking.
         */
        public boolean local = false;

        // ===================================================================
        // SPAWN BEHAVIOR
        // ===================================================================

        /**
         * Random spawns (players spawn at random safe locations).
         * When enabled, new players spawn at random locations instead of
         * fixed spawn points. Useful for spreading out the population.
         */
        public boolean randomSpawns = false;

        // ===================================================================
        // ADMIN CONFIGURATION
        // ===================================================================

        /**
         * CA (Community Assistant) help group (-1 = none, 0+ = specific CA group).
         * CAs are volunteer moderators who can help players and moderate chat.
         * This assigns the server to a specific CA group for coverage.
         */
        public byte caHelpGroup = -1;
    }

    /**
     * Spawn point configuration for all kingdoms.
     *
     * <p><strong>Coordinate System:</strong> Wurm uses a tile-based coordinate system.
     * Coordinates are in tiles, where each tile is 4x4 meters. Map sizes vary:
     * <ul>
     *   <li>2048x2048 tiles (8192x8192 meters) - Standard large map</li>
     *   <li>1024x1024 tiles (4096x4096 meters) - Medium map</li>
     *   <li>512x512 tiles (2048x2048 meters) - Small map</li>
     * </ul>
     *
     * <p><strong>How to Find Coordinates:</strong></p>
     * <ul>
     *   <li>In-game: Press F2 to open the console, type <code>/who</code> to see your coordinates</li>
     *   <li>As GM: Use <code>#gps</code> or <code>#locate &lt;player&gt;</code></li>
     *   <li>Map editors: Most map editors show coordinates when you hover over tiles</li>
     * </ul>
     *
     * <p><strong>Choosing Good Spawn Points:</strong></p>
     * <ul>
     *   <li>Near water sources (lakes, rivers, ocean)</li>
     *   <li>Flat terrain (easier building for new players)</li>
     *   <li>Away from aggressive creature spawns</li>
     *   <li>Near roads or deeds (on Freedom servers)</li>
     *   <li>Near starter deeds or tutorial areas</li>
     * </ul>
     */
    public static class SpawnConfig {
        // ===================================================================
        // JENN-KELLON (JK) / FREEDOM SPAWN POINTS
        // ===================================================================
        // Default spawn for Freedom Isles servers
        // JK kingdom color: White/Silver

        /** Jenn-Kellon spawn X coordinate (0 = map center) */
        public int jennKellonX = 0;

        /** Jenn-Kellon spawn Y coordinate (0 = map center) */
        public int jennKellonY = 0;

        // ===================================================================
        // MOL-REHAN (MR) SPAWN POINTS
        // ===================================================================
        // Mol-Rehan kingdom (nature-themed, ranger kingdom)
        // MR kingdom color: Green

        /** Mol-Rehan spawn X coordinate (0 = map center) */
        public int molRehanX = 0;

        /** Mol-Rehan spawn Y coordinate (0 = map center) */
        public int molRehanY = 0;

        // ===================================================================
        // HORDE OF THE SUMMONED (HOTS) SPAWN POINTS
        // ===================================================================
        // Also called "Libila's followers" or "Blacklighters"
        // HOTS kingdom color: Black/Red
        //
        // NOTE: In code, HOTS is often referred to as "Lib" (Libila)

        /** HOTS/Libila spawn X coordinate (0 = map center) */
        public int hotsX = 0;

        /** HOTS/Libila spawn Y coordinate (0 = map center) */
        public int hotsY = 0;

        // ===================================================================
        // NOTES
        // ===================================================================
        // - On Freedom (non-PvP) servers, all spawn points usually use the same coordinates
        // - On PvP servers, each kingdom spawns in different locations (their home territories)
        // - If randomSpawns is enabled, these coordinates are ignored
        // - Spawning in water or inside rock will cause players to spawn at map center instead
    }

    /**
     * Skills configuration.
     *
     * <p><strong>How Skills Work in Wurm:</strong></p>
     * <ul>
     *   <li>Skills range from 1.0 to 100.0</li>
     *   <li>Skill gain is based on difficulty vs skill level</li>
     *   <li>Higher skills gain slower (logarithmic progression)</li>
     *   <li>Skill gain rate affects ALL skill gains globally</li>
     * </ul>
     *
     * <p><strong>Console Commands:</strong></p>
     * <ul>
     *   <li><code>/setserver skillgainrate &lt;multiplier&gt;</code> - Set skill gain rate (GM only)</li>
     *   <li><code>/setserver skills &lt;player&gt; &lt;skill&gt; &lt;value&gt;</code> - Set specific skill (GM only)</li>
     * </ul>
     */
    public static class SkillsConfig {
        // ===================================================================
        // SKILL GAIN RATE
        // ===================================================================
        /**
         * Skill gain rate multiplier (affects ALL skills globally).
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>1.0 = Official Wurm Online rates (very slow, realistic MMO progression)</li>
         *   <li>2.0 = 2x faster skill gain (faster but still challenging)</li>
         *   <li>5.0 = 5x faster skill gain (recommended for small servers)</li>
         *   <li>10.0 = 10x faster skill gain (good for testing or casual play)</li>
         *   <li>50.0 = 50x faster skill gain (very fast, good for learning mechanics)</li>
         *   <li>100.0 = 100x faster (nearly instant skills, for sandbox/creative mode)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Higher rates make the game easier and reduce long-term progression</li>
         *   <li>Official Wurm Online uses 1.0 (can take years to reach 100 in a skill)</li>
         *   <li>Most Wurm Unlimited servers use 3.0-10.0 for better pacing</li>
         *   <li>This setting does NOT affect characteristic gains (strength, stamina, etc.)</li>
         * </ul>
         */
        public float gainRate = 1.0f;

        // ===================================================================
        // STARTING SKILLS
        // ===================================================================
        /**
         * Starting skill values for new characters.
         *
         * <p><strong>Skill Categories:</strong></p>
         * <ul>
         *   <li><strong>Basic:</strong> Digging, mining, carpentry, blacksmithing, masonry, farming, etc.</li>
         *   <li><strong>Mind Logic:</strong> Mind speed, mind logic (affects skill timers and calculations)</li>
         *   <li><strong>Fighting:</strong> Fighting skill, weapon skills, shields, archery</li>
         *   <li><strong>Body Control:</strong> Body control, body stamina, body strength</li>
         *   <li><strong>Overall:</strong> Multiplier for all starting skills</li>
         * </ul>
         *
         * <p><strong>Official Wurm Online Defaults:</strong></p>
         * <ul>
         *   <li>Basic: 1.0 (starts from scratch)</li>
         *   <li>Mind Logic: 20.0 (starter boost for quality of life)</li>
         *   <li>Fighting: 1.0 (starts from scratch)</li>
         *   <li>Body Control: 20.0 (starter boost for survivability)</li>
         *   <li>Overall: 1.0 (no multiplier)</li>
         * </ul>
         *
         * <p><strong>Recommended for Wurm Unlimited:</strong></p>
         * <ul>
         *   <li>Basic: 20.0-30.0 (skip early grind, start crafting sooner)</li>
         *   <li>Mind Logic: 20.0-25.0 (faster actions, better quality of life)</li>
         *   <li>Fighting: 10.0-20.0 (can defend against weak creatures)</li>
         *   <li>Body Control: 20.0-25.0 (better stamina and movement)</li>
         *   <li>Overall: 1.0-1.5 (optional boost for all skills)</li>
         * </ul>
         */
        public StartingSkillsConfig starting = new StartingSkillsConfig();

        public static class StartingSkillsConfig {
            /**
             * Basic skills starting value (affects: digging, mining, masonry, carpentry,
             * blacksmithing, weaponsmithing, armoursmithing, bowery, fletching, leatherworking,
             * tailoring, ropemaking, pottery, cooking, farming, animal husbandry, etc.)
             *
             * <p>Range: 1.0-100.0 (recommend 20.0-30.0 for Unlimited servers)</p>
             */
            public float basic = 20.0f;

            /**
             * Mind logic skills starting value (affects: mind speed, mind logic).
             * Mind speed affects action timers - higher = faster actions.
             *
             * <p>Range: 1.0-100.0 (recommend 20.0-25.0)</p>
             */
            public float mindLogic = 20.0f;

            /**
             * Fighting skills starting value (affects: fighting, aggressive fighting,
             * defensive fighting, normal fighting, swords, axes, knives, clubs, mauls,
             * shields, archery, etc.)
             *
             * <p>Range: 1.0-100.0 (recommend 10.0-20.0 for Unlimited servers)</p>
             */
            public float fighting = 1.0f;

            /**
             * Body control starting value (affects movement speed, climbing, swimming).
             * Higher body control = faster movement and better stamina efficiency.
             *
             * <p>Range: 1.0-100.0 (recommend 20.0-25.0)</p>
             */
            public float bodyControl = 20.0f;

            /**
             * Overall skill level multiplier for ALL starting skills.
             * This multiplies all other starting skill values.
             *
             * <p>Examples:</p>
             * <ul>
             *   <li>1.0 = Use values as configured above (no change)</li>
             *   <li>1.5 = All skills start 50% higher</li>
             *   <li>2.0 = All skills start at double the configured values</li>
             * </ul>
             *
             * <p>Range: 0.1-10.0 (recommend 1.0)</p>
             */
            public float overall = 1.0f;
        }
    }

    /**
     * Combat and action configuration.
     *
     * <p><strong>How Combat Works in Wurm:</strong></p>
     * <ul>
     *   <li>Combat is turn-based with action timers</li>
     *   <li>Action speed affects ALL timed actions (combat, crafting, gathering, etc.)</li>
     *   <li>Combat Rating (CR) affects damage and hit chance</li>
     *   <li>CR is based on skills, stats, equipment quality, and wounds</li>
     * </ul>
     *
     * <p><strong>Console Commands:</strong></p>
     * <ul>
     *   <li><code>#setserver actiontimer &lt;value&gt;</code> - Set action speed (GM only)</li>
     *   <li><code>#setserver crmod &lt;value&gt;</code> - Set CR modifier (GM only)</li>
     * </ul>
     */
    public static class CombatConfig {
        // ===================================================================
        // ACTION SPEED
        // ===================================================================
        /**
         * Action timer divisor (affects ALL timed actions globally).
         * Vanilla {@code ACTIONTIMER} — Wurm divides hardcoded action durations
         * by this value, so <strong>higher = faster</strong>.
         *
         * <p><strong>What This Controls:</strong></p>
         * <ul>
         *   <li>Combat action speed (swing timers)</li>
         *   <li>Crafting action timers</li>
         *   <li>Gathering actions (mining, digging, foraging, etc.)</li>
         *   <li>Building and terraforming</li>
         *   <li>ALL timer-based actions in the game</li>
         * </ul>
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>1.0 = Official Wurm Online speed (realistic, slow-paced)</li>
         *   <li>2.0 = 2x faster actions</li>
         *   <li>4.0 = 4x faster actions (very fast, good for small servers)</li>
         *   <li>10.0 = 10x faster actions (extremely fast, instant gratification)</li>
         *   <li>0.5 = 2x slower actions (more difficult, more tactical)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Higher values = faster actions (2.0 = half the timer = twice as fast)</li>
         *   <li>This is one of the most impactful settings for server pacing</li>
         *   <li>Most Wurm Unlimited servers use 2.0-4.0 (2x-4x faster)</li>
         *   <li>Too fast can make the game feel rushed and reduce immersion</li>
         *   <li>Official WO uses 1.0 (actions can take 30+ seconds at low skills)</li>
         * </ul>
         */
        public float actionSpeed = 1.0f;

        // ===================================================================
        // COMBAT RATING MODIFIER
        // ===================================================================
        /**
         * Combat Rating (CR) modifier for damage calculations.
         *
         * <p><strong>What Combat Rating Does:</strong></p>
         * <ul>
         *   <li>Higher CR = higher damage output</li>
         *   <li>CR is calculated from: skills, stats, equipment, wounds</li>
         *   <li>CR modifier scales all combat damage globally</li>
         * </ul>
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>1.0 = Official Wurm Online damage (balanced, realistic combat)</li>
         *   <li>1.5 = 50% more damage (faster PvE, more dangerous PvP)</li>
         *   <li>2.0 = 2x damage (very fast combat, low time-to-kill)</li>
         *   <li>0.5 = 50% less damage (tanky combat, longer fights)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Affects both player and creature damage</li>
         *   <li>Higher values make PvE faster but reduce combat depth</li>
         *   <li>Lower values make healing more important</li>
         *   <li>Most servers use 1.0-1.5 for balanced gameplay</li>
         * </ul>
         */
        public float ratingModifier = 1.0f;

        // ===================================================================
        // HUNT OF THE ANCIENTS (HOTA) DELAY
        // ===================================================================
        /**
         * Hunt of the Ancients (HotA) event delay in Wurm hours.
         *
         * <p><strong>What is HotA:</strong></p>
         * <p>Hunt of the Ancients is a server-wide PvP event where a pillar spawns
         * and kingdoms compete to hold it. The winning kingdom gets kingdom-wide bonuses
         * like faster skill gain and better crop growth.</p>
         *
         * <p><strong>Timing:</strong></p>
         * <ul>
         *   <li>1 Wurm hour = 3 real-time minutes</li>
         *   <li>1 Wurm day = 24 Wurm hours = 72 real-time minutes (1.2 hours)</li>
         *   <li>1 Wurm week = 7 Wurm days = 168 Wurm hours = 8.4 real hours</li>
         * </ul>
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>2160 = 90 Wurm days = 108 real hours (4.5 real days) - Official default</li>
         *   <li>1080 = 45 Wurm days = 54 real hours (2.25 real days)</li>
         *   <li>720 = 30 Wurm days = 36 real hours (1.5 real days)</li>
         *   <li>168 = 1 Wurm week = 8.4 real hours (same-day events)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Only relevant for PvP servers with kingdom competition</li>
         *   <li>Freedom (PvE) servers typically don't use HotA</li>
         *   <li>Shorter delays = more frequent kingdom conflicts</li>
         *   <li>Official WO uses 2160 (about 4.5 real days between events)</li>
         * </ul>
         */
        public int hotaDelay = 2160; // 90 Wurm days = ~4.5 real days
    }

    /**
     * Creatures configuration.
     *
     * <p><strong>Console Commands:</strong></p>
     * <ul>
     *   <li><code>#maxcreatures &lt;number&gt;</code> - Set max creatures (GM)</li>
     *   <li><code>#respawn &lt;type&gt;</code> - Respawn creatures (GM)</li>
     *   <li><code>#calcCreatures</code> - Recalculate creature counts (GM)</li>
     * </ul>
     */
    public static class CreaturesConfig {
        /**
         * Maximum total creatures on server (includes all mobs: aggressive, passive, uniques).
         *
         * <p><strong>Performance Impact:</strong> Higher counts increase CPU/RAM usage.</p>
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>1000 = Default (suitable for single-player or small servers)</li>
         *   <li>5000 = Medium population (requires decent hardware)</li>
         *   <li>10000+ = High population (for large servers with good hardware)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Wurm automatically spawns creatures to reach this limit</li>
         *   <li>Creatures include: animals, monsters, npcs, guards</li>
         *   <li>Too few = empty world, too many = lag and overcrowding</li>
         *   <li>Recommend 2-5 creatures per active player</li>
         * </ul>
         */
        public int maxTotal = 1000;

        /**
         * Percentage of aggressive (hostile) creatures (0.0-100.0).
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>0.0 = Peaceful server (no aggressive spawns)</li>
         *   <li>10.0 = Easy difficulty (mostly passive animals)</li>
         *   <li>30.0 = Default Wurm (balanced challenge)</li>
         *   <li>50.0 = Hard difficulty (half of all creatures hostile)</li>
         *   <li>80.0+ = Extreme difficulty (survival horror mode)</li>
         * </ul>
         *
         * <p><strong>Aggressive Creatures Include:</strong></p>
         * <ul>
         *   <li>Trolls, spiders, wolves, bears, wild boars</li>
         *   <li>Scorpions, crocodiles, hell horses, lava creatures</li>
         *   <li>Dragons, drakes, and other dangerous creatures</li>
         * </ul>
         */
        public float percentAggressive = 10.0f;

        /**
         * Animal breeding timer in milliseconds (0 = use Wurm default).
         *
         * <p><strong>What This Controls:</strong> Time between breeding attempts for animals
         * (horses, cattle, pigs, chickens, hell horses, etc.)</p>
         *
         * <p><strong>Timing Reference:</strong></p>
         * <ul>
         *   <li>0 = Use Wurm default (~5-6 Wurm days between pregnancies)</li>
         *   <li>86400000 = 1 real day (24 hours)</li>
         *   <li>43200000 = 12 real hours</li>
         *   <li>3600000 = 1 real hour (very fast breeding)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Lower values = faster breeding = easier to get 5-speed horses</li>
         *   <li>Breeding also affected by animal conditions and player care</li>
         *   <li>0 is recommended (lets Wurm handle breeding naturally)</li>
         * </ul>
         */
        public long breedingTimer = 0L;
    }

    /**
     * World configuration (growth rates, terrain, resource respawn).
     *
     * <p><strong>Console Commands:</strong></p>
     * <ul>
     *   <li><code>#setserver treegrowth &lt;value&gt;</code> - Set tree growth (GM)</li>
     *   <li><code>#setserver fieldgrowth &lt;milliseconds&gt;</code> - Set crop growth (GM)</li>
     *   <li><code>#harvest</code> - Force harvest crops (GM, power 3+)</li>
     * </ul>
     */
    public static class WorldConfig {
        /**
         * Tree growth rate (controls tree aging and spreading).
         *
         * <p><strong>What This Does:</strong></p>
         * <ul>
         *   <li>Controls tree aging (sprout → young → mature → old → very old)</li>
         *   <li>Affects tree spreading (new sprouts from nearby trees)</li>
         *   <li>Influences chopped stump respawn time</li>
         * </ul>
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>1 = Very slow (takes months for trees to age)</li>
         *   <li>10 = Slow (takes weeks for mature trees)</li>
         *   <li>20 = Default Wurm (balanced, realistic growth)</li>
         *   <li>50 = Fast (trees age and spread quickly)</li>
         *   <li>100 = Very fast (rapid reforestation)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Higher values = faster tree growth = easier woodcutting</li>
         *   <li>Lower values = slower = deforestation matters more</li>
         *   <li>Most servers use 20-50 for good forest management</li>
         * </ul>
         */
        public int treeGrowth = 20;

        /**
         * Crop/field growth time in milliseconds (time from planted to harvestable).
         *
         * <p><strong>Timing Reference:</strong></p>
         * <ul>
         *   <li>86400000 = 24 real hours (1 full real day)</li>
         *   <li>43200000 = 12 real hours (overnight growth)</li>
         *   <li>21600000 = 6 real hours (multiple harvests per day)</li>
         *   <li>3600000 = 1 real hour (very fast farming)</li>
         * </ul>
         *
         * <p><strong>What This Controls:</strong></p>
         * <ul>
         *   <li>Time for crops to grow from sown to harvestable</li>
         *   <li>Affects: wheat, oat, rye, barley, corn, pumpkins, cotton, etc.</li>
         *   <li>Does NOT affect tree growth (see treeGrowth above)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Official Wurm: 86400000 (24 real hours per harvest)</li>
         *   <li>Most Unlimited servers: 21600000-43200000 (6-12 hours)</li>
         *   <li>Lower = more active farming, higher yield per day</li>
         *   <li>Too fast can make food trivial and reduce farming depth</li>
         * </ul>
         */
        public long fieldGrowthTime = 86400000L; // 24 real hours

        /**
         * Tunneling hits required to mine one action (lower = faster mining).
         *
         * <p><strong>What This Does:</strong> Controls how many "hits" of mining are needed
         * to complete one mining action (removing one rock cube from tunnel wall).</p>
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>51 = Official Wurm (realistic, slow mining)</li>
         *   <li>25 = 2x faster mining</li>
         *   <li>10 = 5x faster mining (recommended for small servers)</li>
         *   <li>1 = Nearly instant mining (creative mode)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Mining is affected by: skill, pickaxe quality, rock type</li>
         *   <li>Lower values = faster terraforming and underground builds</li>
         *   <li>Official rate (51) can make underground projects take months</li>
         *   <li>Most Unlimited servers use 10-25 for better pacing</li>
         * </ul>
         */
        public int tunnelingHits = 51;
    }

    /**
     * Economy configuration (deeds, upkeep, traders, money).
     *
     * <p><strong>Console Commands:</strong></p>
     * <ul>
     *   <li><code>#addmoney &lt;player&gt; &lt;irons&gt;</code> - Give money (GM)</li>
     *   <li><code>#worth &lt;player&gt;</code> - Check player wealth (GM)</li>
     *   <li><code>#setserver upkeep &lt;true|false&gt;</code> - Toggle upkeep (GM)</li>
     * </ul>
     *
     * <p><strong>Money System:</strong></p>
     * <ul>
     *   <li>1 copper = 1 iron coin</li>
     *   <li>1 silver = 100 copper = 100 irons</li>
     *   <li>1 gold = 100 silver = 10,000 irons</li>
     * </ul>
     */
    public static class EconomyConfig {
        /**
         * Village upkeep enabled (deeds require monthly upkeep payments).
         *
         * <p><strong>What Upkeep Does:</strong></p>
         * <ul>
         *   <li>Deeds must pay monthly silver based on deed size and guards</li>
         *   <li>If upkeep runs out, deed becomes disband-able by anyone</li>
         *   <li>Encourages active play and prevents deed spam</li>
         * </ul>
         *
         * <p><strong>Recommendations:</strong></p>
         * <ul>
         *   <li>true = Realistic server economy, prevents abandoned deeds</li>
         *   <li>false = Casual/PvE servers, easier for solo/new players</li>
         *   <li>Consider false on small servers with few players</li>
         * </ul>
         */
        public boolean upkeepEnabled = true;

        /**
         * Maximum deed size in tiles (0 = unlimited, N = max NxN deed).
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>0 = Unlimited deed size (default, allows huge deeds)</li>
         *   <li>51 = Maximum 51x51 tile deed (official PvP server limit)</li>
         *   <li>101 = Maximum 101x101 tile deed (large but reasonable)</li>
         *   <li>151 = Maximum 151x151 tile deed (very large)</li>
         * </ul>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Larger deeds cost more upkeep (if enabled)</li>
         *   <li>Unlimited (0) can lead to entire maps being deeded</li>
         *   <li>PvP servers typically limit to 51-101 for balance</li>
         * </ul>
         */
        public int maxDeedSize = 0;

        /**
         * Free deeds (no founding cost, deeds are free to create).
         *
         * <p><strong>Normal Deed Costs:</strong> Deeds normally cost 1 gold (10,000 irons)
         * plus additional costs for size and guards.</p>
         *
         * <p><strong>Recommendations:</strong></p>
         * <ul>
         *   <li>true = Good for newbie-friendly servers, easier start</li>
         *   <li>false = More realistic, encourages economy and trading</li>
         *   <li>Consider true on PvE servers with upkeep disabled</li>
         * </ul>
         */
        public boolean freeDeeds = false;

        /**
         * Trader money settings (NPC traders in deeds).
         */
        public TraderConfig traders = new TraderConfig();

        /**
         * Kingdom coffers starting money (when kingdom resets or server starts).
         * In iron coins (10,000 = 1 gold).
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>0 = Kingdoms start broke (earn money through taxes/HOTA)</li>
         *   <li>1000000+ = Kingdom can immediately hire guards/build</li>
         *   <li>Only relevant for PvP servers with kingdom mechanics</li>
         * </ul>
         */
        public int kingdomStartingMoney = 0;

        public static class TraderConfig {
            /**
             * Trader maximum money in iron coins (100 irons = 1 silver).
             *
             * <p>Default: 500,000 irons = 5,000 silver = 50 gold</p>
             *
             * <p><strong>What Traders Do:</strong> NPC traders buy/sell items and generate
             * passive income for deed owners based on trade volume.</p>
             */
            public int maxMoney = 500000; // 50 gold

            /**
             * Trader starting money in iron coins.
             *
             * <p>Default: 10,000 irons = 100 silver = 1 gold</p>
             *
             * <p><strong>Notes:</strong> Traders gain money over time through trades.
             * Starting money affects how quickly they can buy items from players.</p>
             */
            public int startingMoney = 10000; // 1 gold
        }
    }

    /**
     * Server properties configuration (SERVERPROPERTIES table).
     *
     * <p>These settings control advanced server behavior and are stored in
     * the SERVERPROPERTIES table (key-value pairs). They override some SERVERS
     * table settings and add additional configuration options.</p>
     */
    public static class ServerPropertiesConfig {
        /**
         * Multi-kingdom mode (CRITICAL for kingdom behavior).
         *
         * <p><strong>What This Controls:</strong></p>
         * <ul>
         *   <li>false = Freedom server (no kingdoms, all players are friends)</li>
         *   <li>true = PvP server with kingdoms (JK, MR, HOTS can war)</li>
         * </ul>
         *
         * <p><strong>IMPORTANT:</strong> Setting this to false disables kingdom selection
         * at character creation and prevents spawn villages from attacking players.</p>
         */
        public boolean multiKingdom = false;

        /**
         * Epic mode (connects to Epic cluster mission system).
         * Duplicates SERVERS.EPIC but stored as a property.
         */
        public boolean epic = false;

        /**
         * Allow Chaos kingdom (enables Libila/HOTS kingdom).
         *
         * <p><strong>What This Controls:</strong></p>
         * <ul>
         *   <li>false = Players cannot join Chaos/HOTS</li>
         *   <li>true = Chaos kingdom available (PvP only)</li>
         * </ul>
         */
        public boolean allowChaos = false;

        /**
         * Newbie-friendly mode (enables tutorial and starter gear).
         *
         * <p><strong>What This Provides:</strong></p>
         * <ul>
         *   <li>Newbie protection from PvP</li>
         *   <li>Starter tools and food</li>
         *   <li>Tutorial messages</li>
         * </ul>
         */
        public boolean newbieFriendly = true;

        /**
         * Spy prevention (prevents viewing other players' skills/stats).
         *
         * <p><strong>What This Controls:</strong></p>
         * <ul>
         *   <li>false = Players can examine each other freely</li>
         *   <li>true = Players cannot see others' exact skills</li>
         * </ul>
         */
        public boolean spyPrevention = false;

        /**
         * Enable NPCs (traders, guards, spirit templars, etc.).
         */
        public boolean npcs = true;

        /**
         * Enable end-game items (artifacts, dragon armor, etc.).
         */
        public boolean endGameItems = true;

        /**
         * Auto-networking (automatic UPnP port forwarding).
         */
        public boolean autoNetworking = true;

        /**
         * Enable PnP port forwarding (auto-configure router).
         */
        public boolean enablePnpPortForward = true;

        /**
         * Steam query port (for server browser visibility).
         */
        public int steamQueryPort = 27016;

        /**
         * Admin password (for remote administration).
         * Leave empty for no password.
         */
        public String adminPassword = "";
    }

    /**
     * Players configuration.
     *
     * <p><strong>Console Commands:</strong></p>
     * <ul>
     *   <li><code>#plimit &lt;number&gt;</code> - Set player limit (GM)</li>
     *   <li><code>#who</code> - List online players (GM)</li>
     *   <li><code>#online</code> - Show online count (GM)</li>
     * </ul>
     */
    public static class PlayersConfig {
        /**
         * Maximum concurrent players allowed online.
         *
         * <p><strong>Examples:</strong></p>
         * <ul>
         *   <li>1-10 = Single-player or small friend group</li>
         *   <li>50 = Small community server</li>
         *   <li>200 = Medium server (default)</li>
         *   <li>500+ = Large server (requires good hardware and network)</li>
         * </ul>
         *
         * <p><strong>Performance Impact:</strong> More players = higher CPU, RAM, and bandwidth usage.</p>
         *
         * <p><strong>Notes:</strong></p>
         * <ul>
         *   <li>Wurm Unlimited can technically support 1000+ players</li>
         *   <li>Practical limit depends on server hardware and map size</li>
         *   <li>Official Wurm Online servers typically cap at 200-500</li>
         * </ul>
         */
        public int maxPlayers = 200;

        /**
         * Player limit can be overridden by GMs using #plimit command.
         *
         * <p><strong>Recommendations:</strong></p>
         * <ul>
         *   <li>true = Allows GMs to adjust limit in-game (flexible)</li>
         *   <li>false = Hard cap, GMs cannot override (strict)</li>
         * </ul>
         */
        public boolean limitOverridable = true;
    }

    @Override
    public String toString() {
        return String.format("ServerConfig[version=%d, server=%s, skills.gainRate=%.2f, combat.actionSpeed=%.2f]",
            version, server.name, skills.gainRate, combat.actionSpeed);
    }
}

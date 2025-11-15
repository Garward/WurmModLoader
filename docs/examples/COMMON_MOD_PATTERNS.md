# Common Mod Development Patterns

This guide demonstrates common patterns and best practices for developing mods with WurmModLoader.

## Table of Contents

1. [Basic Mod Structure](#basic-mod-structure)
2. [Creating Custom Items](#creating-custom-items)
3. [Creating Custom Actions](#creating-custom-actions)
4. [Creating Custom Creatures](#creating-custom-creatures)
5. [Player Login/Logout Handling](#player-loginlogout-handling)
6. [Custom Questions/Dialogs](#custom-questionsdialogs)
7. [Server Lifecycle Management](#server-lifecycle-management)
8. [Bytecode Hooks](#bytecode-hooks)
9. [Configuration Management](#configuration-management)
10. [Error Handling](#error-handling)

---

## Basic Mod Structure

### Minimal Mod

The simplest possible mod that does nothing but load:

```java
package com.example.mymod;

import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

public class MyMod implements WurmServerMod {
    // Minimal mod - no initialization needed
}
```

### Complete Mod Template

A complete template with all common lifecycle hooks:

```java
package com.example.mymod;

import com.garward.wurmmodloader.modloader.interfaces.*;
import com.wurmonline.server.players.Player;

import java.util.logging.Logger;

public class MyMod implements WurmServerMod,
                               ServerStartedListener,
                               ItemTemplatesCreatedListener,
                               PlayerLoginListener,
                               ServerShutdownListener {

    private static final Logger logger = Logger.getLogger(MyMod.class.getName());

    @Override
    public void preInit() {
        logger.info("MyMod: Pre-initializing...");
        // Register bytecode hooks here
    }

    @Override
    public void init() {
        logger.info("MyMod: Initializing...");
        // Perform standard initialization
    }

    @Override
    public void onItemTemplatesCreated() {
        logger.info("MyMod: Item templates ready");
        // Create custom items here
    }

    @Override
    public void onServerStarted() {
        logger.info("MyMod: Server started");
        // Perform post-startup tasks
    }

    @Override
    public void onPlayerLogin(Player player) {
        logger.info(player.getName() + " logged in");
    }

    @Override
    public void onPlayerLogout(Player player) {
        logger.info(player.getName() + " logged out");
    }

    @Override
    public void onServerShutdown() {
        logger.info("MyMod: Shutting down...");
        // Save data, close resources
    }
}
```

---

## Creating Custom Items

### Simple Custom Item

```java
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import com.garward.wurmmodloader.modloader.interfaces.ItemTemplatesCreatedListener;

@Override
public void onItemTemplatesCreated() {
    try {
        ItemTemplateBuilder builder = new ItemTemplateBuilder("mymod.simple.sword");
        builder.name("Simple Sword", "simple swords", "A basic custom sword");
        builder.descriptions("excellent", "good", "ok", "poor");
        builder.itemTypes(new short[] {
            ItemTypes.ITEM_TYPE_METAL,
            ItemTypes.ITEM_TYPE_WEAPON,
            ItemTypes.ITEM_TYPE_WEAPON_SLASH
        });
        builder.imageNumber((short) 123);
        builder.behaviourType(BehaviourList.itemBehaviour);
        builder.combatDamage(7);
        builder.decayTime(9072000L);
        builder.dimensions(1, 10, 80);
        builder.weightGrams(3000);
        builder.material(Materials.MATERIAL_IRON);
        builder.value(5000);
        builder.difficulty(30.0f);
        builder.isPrimarySkill(true);
        builder.build();

        logger.info("Created custom item: Simple Sword");
    } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create simple sword", e);
    }
}
```

### Food Item

```java
@Override
public void onItemTemplatesCreated() {
    try {
        ItemTemplateBuilder builder = new ItemTemplateBuilder("mymod.custom.bread");
        builder.name("Custom Bread", "custom breads", "Delicious homemade bread");
        builder.itemTypes(new short[] {
            ItemTypes.ITEM_TYPE_FOOD,
            ItemTypes.ITEM_TYPE_PLANTABLE
        });
        builder.imageNumber((short) 456);
        builder.behaviourType(BehaviourList.itemBehaviour);
        builder.weightGrams(200);
        builder.material(Materials.MATERIAL_FOOD);
        builder.dimensions(5, 5, 5);
        builder.decayTime(604800L); // 1 week
        builder.difficulty(5.0f);
        builder.nutrition((short) 5000);
        builder.build();

        logger.info("Created food item: Custom Bread");
    } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create custom bread", e);
    }
}
```

---

## Creating Custom Actions

### Simple Custom Action

```java
import com.garward.wurmmodloader.modsupport.actions.*;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

@Override
public void preInit() {
    // Initialize ModActions
    ModActions.init();

    // Register the action
    try {
        int actionId = ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Greet", "greeting")
                .range(4)
                .build();
        ModActions.registerAction(actionEntry);

        // Register the performer
        ModActions.registerActionPerformer(new GreetActionPerformer(actionId));

        logger.info("Registered Greet action with ID: " + actionId);
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to register Greet action", e);
    }
}

// Action performer class
public class GreetActionPerformer implements ActionPerformer {
    private final int actionId;

    public GreetActionPerformer(int actionId) {
        this.actionId = actionId;
    }

    @Override
    public short getActionId() {
        return (short) actionId;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source,
                         Item target, short num, float counter) {
        if (counter == 1.0f) {
            performer.getCommunicator().sendNormalServerMessage(
                "You greet the " + target.getName() + " warmly."
            );
            return propagate(action,
                           ActionPropagation.FINISH_ACTION,
                           ActionPropagation.NO_SERVER_PROPAGATION,
                           ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
        }
        return false;
    }
}
```

### Item-to-Item Action

```java
public class CombineItemsPerformer extends ActionPerformerBase {

    public CombineItemsPerformer(int actionId) {
        super(actionId);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source,
                         Item target, short num, float counter) {

        // Validate items
        if (source.getTemplateId() != TARGET_TEMPLATE_ID) {
            performer.getCommunicator().sendNormalServerMessage(
                "You cannot combine those items."
            );
            return true;
        }

        // Perform action over time
        if (counter == 1.0f) {
            performer.getCommunicator().sendNormalServerMessage(
                "You start combining the items..."
            );

            int time = Actions.getStandardActionTime(performer,
                performer.getSkills().getSkillOrLearn(SkillList.SMITHING_BLACKSMITHING),
                source, 0.0);

            performer.sendActionControl("Combining", true, time);
            action.setTimeLeft(time);

            return false; // Continue action
        }

        // Action time elapsed
        if (counter * 10.0f > action.getTimeLeft()) {
            // Combine the items
            try {
                Item result = ItemFactory.createItem(RESULT_TEMPLATE_ID,
                    source.getQualityLevel(), performer.getName());

                performer.getInventory().insertItem(result);
                Items.destroyItem(source.getWurmId());
                Items.destroyItem(target.getWurmId());

                performer.getCommunicator().sendNormalServerMessage(
                    "You successfully combine the items!"
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to create combined item", e);
                performer.getCommunicator().sendNormalServerMessage(
                    "Something went wrong..."
                );
            }
            return true; // Finish action
        }

        return false; // Continue waiting
    }
}
```

---

## Creating Custom Creatures

### Simple Custom Creature

```java
import com.garward.wurmmodloader.modsupport.CreatureTemplateBuilder;

@Override
public void init() {
    try {
        int templateId = IdFactory.getIdFor("mymod.goblin", IdType.CREATURETEMPLATE);

        CreatureTemplateBuilder builder = new CreatureTemplateBuilder(templateId, "goblin");
        builder.name("Goblin", "Goblins", "A small green creature");
        builder.modelName("model.creature.humanoid.goblin");
        builder.boundsValues(-0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 1.5f);
        builder.dimension(30, 30, 150);
        builder.alignment(-100f);
        builder.handDamage(5);
        builder.kickDamage(3);
        builder.headbuttDamage(4);
        builder.speed(0.8f);
        builder.moveRate(100);
        builder.combatMoveRate(3);
        builder.maxAge(50);
        builder.vision(5);
        builder.sex((byte) 0); // Male
        builder.centimetersHigh(150);
        builder.centimetersLong(50);
        builder.centimetersWide(50);
        builder.naturalArmour((byte) 2);
        builder.baseCombatRating(5.0f);

        // Skills
        builder.skill(SkillList.BODY_STRENGTH, 25.0f);
        builder.skill(SkillList.BODY_STAMINA, 30.0f);
        builder.skill(SkillList.BODY_CONTROL, 20.0f);
        builder.skill(SkillList.MIND_LOGICAL, 15.0f);
        builder.skill(SkillList.MIND_SPEED, 20.0f);
        builder.skill(SkillList.SOUL_STRENGTH, 10.0f);
        builder.skill(SkillList.SOUL_DEPTH, 10.0f);
        builder.skill(SkillList.WEAPONLESS_FIGHTING, 30.0f);

        builder.build();

        logger.info("Created custom creature: Goblin with ID " + templateId);
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to create goblin creature", e);
    }
}
```

---

## Player Login/Logout Handling

### Track Player Sessions

```java
import java.util.HashMap;
import java.util.Map;

public class SessionTracker implements PlayerLoginListener {
    private final Map<Long, PlayerSession> sessions = new HashMap<>();

    @Override
    public void onPlayerLogin(Player player) {
        long playerId = player.getWurmId();
        PlayerSession session = new PlayerSession(playerId, System.currentTimeMillis());
        sessions.put(playerId, session);

        player.getCommunicator().sendNormalServerMessage(
            "Welcome back! You have " + getPlayerPoints(playerId) + " points."
        );
    }

    @Override
    public void onPlayerLogout(Player player) {
        long playerId = player.getWurmId();
        PlayerSession session = sessions.remove(playerId);

        if (session != null) {
            long duration = System.currentTimeMillis() - session.getLoginTime();
            logger.info(player.getName() + " played for " + (duration / 1000) + " seconds");
            saveSessionData(session);
        }
    }

    private int getPlayerPoints(long playerId) {
        // Load from database or cache
        return 0;
    }

    private void saveSessionData(PlayerSession session) {
        // Save to database
    }

    private static class PlayerSession {
        private final long playerId;
        private final long loginTime;

        public PlayerSession(long playerId, long loginTime) {
            this.playerId = playerId;
            this.loginTime = loginTime;
        }

        public long getLoginTime() {
            return loginTime;
        }
    }
}
```

---

## Custom Questions/Dialogs

### Simple Question Dialog

```java
import com.garward.wurmmodloader.modsupport.questions.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

import java.util.Properties;

public class MyCustomQuestion implements ModQuestion {

    @Override
    public void answer(Question question, Properties answers) {
        String response = answers.getProperty("response");

        if ("yes".equals(response)) {
            question.getResponder().getCommunicator().sendNormalServerMessage(
                "You chose yes!"
            );
        } else {
            question.getResponder().getCommunicator().sendNormalServerMessage(
                "You chose no!"
            );
        }
    }

    @Override
    public void sendQuestion(Question question) {
        StringBuilder buf = new StringBuilder();
        buf.append(ModQuestions.getBmlHeader(question));
        buf.append("text{text='Would you like to proceed?'}");
        buf.append("radio{group='response';id='yes';text='Yes';selected='true'}");
        buf.append("radio{group='response';id='no';text='No'}");
        buf.append(ModQuestions.createAnswerButton2(question));
        buf.append("}\n}\n");

        sendBml(question, 300, 200, true, true, buf);
    }
}

// Show the question to a player
public void showQuestionToPlayer(Creature player) {
    Question question = ModQuestions.createQuestion(
        player,
        "Confirmation",
        "Confirm your choice",
        player.getWurmId(),
        new MyCustomQuestion()
    );

    question.sendQuestion();
}
```

---

## Server Lifecycle Management

### Proper Resource Management

```java
public class ResourceManager implements ServerStartedListener, ServerShutdownListener {
    private ScheduledExecutorService scheduler;
    private DatabaseConnection dbConnection;

    @Override
    public void onServerStarted() {
        logger.info("Starting background tasks...");

        // Initialize scheduler
        scheduler = Executors.newScheduledThreadPool(2);

        // Schedule recurring task (every 5 minutes)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performMaintenance();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Maintenance task failed", e);
            }
        }, 5, 5, TimeUnit.MINUTES);

        // Initialize database
        try {
            dbConnection = DatabaseConnection.create();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to connect to database", e);
        }
    }

    @Override
    public void onServerShutdown() {
        logger.info("Shutting down resources...");

        // Shutdown scheduler gracefully
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close database connection
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing database", e);
            }
        }
    }

    private void performMaintenance() {
        logger.info("Running maintenance tasks...");
        // Perform cleanup, cache updates, etc.
    }
}
```

---

## Bytecode Hooks

### Hook a Server Method

```java
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import javassist.*;

@Override
public void preInit() {
    try {
        // Get the class pool
        ClassPool classPool = HookManager.getInstance().getClassPool();

        // Get the target class
        CtClass ctClass = classPool.get("com.wurmonline.server.items.Item");

        // Get the method to hook
        CtMethod method = ctClass.getMethod("getName",
            "(ZZ)Ljava/lang/String;");

        // Insert code at the beginning of the method
        method.insertBefore(
            "{ " +
            "  if ($1 == true) {" +
            "    System.out.println(\"Getting item name with capitalization\");" +
            "  }" +
            "}"
        );

        logger.info("Successfully hooked Item.getName()");
    } catch (NotFoundException | CannotCompileException e) {
        logger.log(Level.SEVERE, "Failed to hook Item.getName()", e);
    }
}
```

---

## Configuration Management

### Load and Use Configuration

```java
import java.io.*;
import java.util.Properties;

public class ConfigurableMod implements WurmServerMod {
    private Properties config;
    private int customValue;
    private boolean featureEnabled;

    @Override
    public void init() {
        loadConfiguration();

        customValue = Integer.parseInt(
            config.getProperty("customValue", "100")
        );

        featureEnabled = Boolean.parseBoolean(
            config.getProperty("featureEnabled", "true")
        );

        logger.info("Loaded configuration: customValue=" + customValue +
                   ", featureEnabled=" + featureEnabled);
    }

    private void loadConfiguration() {
        config = new Properties();
        File configFile = new File("mods/mymod.properties");

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
                logger.info("Loaded configuration from " + configFile);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load configuration", e);
            }
        } else {
            // Create default configuration
            config.setProperty("customValue", "100");
            config.setProperty("featureEnabled", "true");

            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                config.store(fos, "MyMod Configuration");
                logger.info("Created default configuration at " + configFile);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to save default config", e);
            }
        }
    }
}
```

---

## Error Handling

### Robust Error Handling

```java
public class SafeMod implements WurmServerMod, ServerStartedListener {

    @Override
    public void preInit() {
        // Always wrap risky operations in try-catch
        try {
            performRiskyOperation();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed during pre-initialization", e);
            // Decide whether to throw or continue
            // throw new RuntimeException("Critical failure", e);
        }
    }

    @Override
    public void onServerStarted() {
        // Handle specific exceptions differently
        try {
            initializeDatabaseConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
            // Attempt fallback
            enableOfflineMode();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during startup", e);
        }
    }

    private void performRiskyOperation() {
        // Validate inputs
        if (someCondition == null) {
            logger.warning("Skipping risky operation: condition not met");
            return;
        }

        // Perform operation with timeout
        try {
            Future<?> future = executor.submit(() -> {
                // Long running task
            });

            future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.log(Level.WARNING, "Operation timed out", e);
            future.cancel(true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Operation failed", e);
        }
    }

    private void initializeDatabaseConnection() throws SQLException {
        // Implement with proper exception handling
    }

    private void enableOfflineMode() {
        logger.info("Enabling offline mode as fallback");
        // Implement fallback behavior
    }
}
```

---

## Best Practices Summary

1. **Always use loggers** instead of System.out.println()
2. **Validate inputs** before performing operations
3. **Handle exceptions** appropriately - don't let them crash the server
4. **Clean up resources** in onServerShutdown()
5. **Use IdFactory** for persistent IDs
6. **Test bytecode hooks** carefully - they can break the server
7. **Document your mod's** configuration options
8. **Version your mod** using the Versioned interface
9. **Be thread-safe** if using background threads
10. **Log important operations** for debugging

---

## Additional Resources

- [API Surface Area](../API_SURFACE_AREA.md) - Complete API reference
- [WurmServerModLauncher Wiki](https://github.com/ago1024/WurmServerModLauncher/wiki) - Legacy documentation (many patterns still apply)
- [Example Mods](../../examples/) - Working example mods in this repository

# WurmModLoader UI API Guide

**Complete guide for creating custom UI windows and context menus**

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Core Components](#core-components)
4. [API Reference](#api-reference)
5. [Migration Guide](#migration-guide)
6. [Best Practices](#best-practices)
7. [Examples](#examples)

---

## Overview

The WurmModLoader UI API provides a high-level, type-safe abstraction over Wurm's BML (Basic Markup Language) system, making it easy to create interactive windows and context menu entries without writing raw BML or implementing low-level interfaces.

### Why Use the UI API?

**Before (Old Pattern):**
```java
// 115+ lines across 3 files:
// - PowerScalingStatsQuestion implements ModQuestion (50+ lines)
// - ViewStatsActionPerformer implements ActionPerformer (65+ lines)
// - Manual registration in PowerScalingMod (event handlers, action IDs)
```

**After (UI API):**
```java
// 30 lines in 1 file:
MenuEntry entry = MenuEntry.builder("Power Fantasy")
    .onClick(player -> {
        UIWindow window = UIWindow.builder("Stats")
            .addText("Power: " + getPower(player))
            .build();
        WindowManager.open(player, window);
    })
    .build();

ContextMenuRegistry.getInstance().register("MyMod", MenuTarget.BODY, entry);
```

### Key Benefits

✓ **70% less boilerplate** - No need to implement ModQuestion or ActionPerformer
✓ **Fluent API** - Build windows with readable, chainable method calls
✓ **Automatic action ID management** - No manual ID allocation
✓ **Built-in visibility filters** - Show menus based on power level, permissions, etc.
✓ **Type-safe** - Compile-time checking instead of runtime BML errors
✓ **Thread-safe** - Safe for concurrent mod access
✓ **Zero Wurm imports in mods** - Framework handles all game class interactions

---

## Quick Start

### 1. Create a Simple Window

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    MenuEntry entry = MenuEntry.builder("View Stats")
        .onClick(player -> showStatsWindow((Creature) player))
        .build();

    ContextMenuRegistry.getInstance().register("MyMod", MenuTarget.BODY, entry);
}

private void showStatsWindow(Creature player) {
    UIWindow window = UIWindow.builder("Player Stats")
        .width(400).height(300)
        .addText("Player: " + player.getName(), true) // bold
        .addText("Level: " + getLevel(player))
        .build();

    WindowManager.open(player, window);
}
```

### 2. Add Interactive Elements

```java
UIWindow window = UIWindow.builder("Settings")
    .addText("Choose your class:")
    .addDropdown("class", "Class", "Warrior", "Mage", "Rogue")
    .addInput("name", "Character Name", "")
    .addButton("save", "Save")
    .onAnswer((p, answers) -> {
        String className = answers.getProperty("class");
        String charName = answers.getProperty("name");
        // Save settings...
    })
    .build();
```

### 3. Add Visibility Filters

```java
MenuEntry entry = MenuEntry.builder("Admin Panel")
    .onlyFor(player -> isAdmin((Creature) player))
    .onClick(player -> showAdminPanel((Creature) player))
    .build();
```

---

## Core Components

### 1. **UIWindow** - Window Builder

Creates BML windows with fluent API.

```java
UIWindow window = UIWindow.builder("Title")
    .width(500).height(400)           // Dimensions
    .addText("Hello!", true)          // Bold text
    .addHeader("Section Title")       // Header with spacing
    .addSeparator()                   // Horizontal line
    .addInput("field", "Label", "")   // Text input
    .addDropdown("sel", "Pick", opts) // Dropdown
    .addButton("btn", "Click Me")     // Button
    .onAnswer((player, answers) -> {  // Form submission
        // Handle answers
    })
    .build();
```

### 2. **WindowManager** - Window Display

Opens windows for players.

```java
WindowManager.open(player, window);

// Check if player has window open
boolean hasWindow = WindowManager.hasOpenWindow(playerId);
String title = WindowManager.getOpenWindowTitle(playerId);
```

### 3. **ContextMenuRegistry** - Menu Registration

Manages context menu entries with automatic action ID allocation and submenu organization.

```java
ContextMenuRegistry registry = ContextMenuRegistry.getInstance();

MenuEntry entry = MenuEntry.builder("My Action")
    .actionVerb("performing")
    .onlyFor(player -> checkCondition(player))
    .onClick(player -> doAction(player))
    .build();

registry.register("ModName", MenuTarget.BODY, entry);
// Appears under: Body Menu > Mods > My Action
```

**Submenu Organization:** All mod buttons are automatically grouped under a "Mods" submenu to keep the context menu clean and organized.

### 4. **MenuEntry** - Menu Builder

Creates context menu entries with visibility filters.

```java
MenuEntry entry = MenuEntry.builder("View Stats")
    .actionVerb("viewing")            // Action verb (e.g., "viewing", "opening")
    .onlyOwnBody()                    // Only visible on own body
    .onlyFor(predicate)               // Custom visibility filter
    .onClick(player -> {...})         // Click handler
    .opensWindow(window)              // Shortcut to open window
    .build();
```

### 5. **VisibilityPredicate** - Visibility Filter

Functional interface for filtering menu visibility.

```java
VisibilityPredicate adminOnly = player -> isAdmin(player);
VisibilityPredicate highLevel = player -> getLevel(player) >= 50;

// Combine predicates
VisibilityPredicate combined = adminOnly.or(highLevel);

MenuEntry entry = MenuEntry.builder("Special Feature")
    .onlyFor(combined)
    .build();
```

---

## API Reference

### UIWindow Methods

| Method | Description |
|--------|-------------|
| `builder(title)` | Creates a new window builder |
| `width(px)` | Sets window width |
| `height(px)` | Sets window height |
| `addText(text)` | Adds plain text |
| `addText(text, bold)` | Adds text (optionally bold) |
| `addHeader(text)` | Adds header with spacing |
| `addSeparator()` | Adds horizontal separator |
| `addButton(id, label)` | Adds button |
| `addInput(id, label, default)` | Adds text input |
| `addDropdown(id, label, opts)` | Adds dropdown |
| `onAnswer(callback)` | Sets answer callback |
| `build()` | Builds the window |

### MenuEntry Methods

| Method | Description |
|--------|-------------|
| `builder(label)` | Creates a new entry builder |
| `actionVerb(verb)` | Sets action verb (default: "viewing") |
| `onlyFor(predicate)` | Adds visibility filter |
| `requirePower(min)` | Requires minimum power level |
| `onlyOwnBody()` | Only visible on own body |
| `onClick(handler)` | Sets click handler |
| `opensWindow(window)` | Opens window on click |
| `build()` | Builds the entry |

### ContextMenuRegistry Methods

| Method | Description |
|--------|-------------|
| `getInstance()` | Gets singleton instance |
| `register(modName, target, entry)` | Registers menu entry |
| `getEntries(target)` | Gets all entries for target |
| `getEntryByActionId(id)` | Gets entry by action ID |

### MenuTarget Enum

| Value | Description |
|-------|-------------|
| `BODY` | Body part context menus |
| `ITEM` | Item context menus |
| `CREATURE` | Creature context menus |
| `TILE` | Tile context menus |

---

## Migration Guide

### Migrating from Manual ModQuestion Implementation

**Old Pattern:**
```java
// PowerScalingStatsQuestion.java (50+ lines)
public class PowerScalingStatsQuestion implements ModQuestion {
    private final Creature player;

    public PowerScalingStatsQuestion(Creature player) {
        this.player = player;
    }

    @Override
    public void sendQuestion(Question question) {
        StringBuilder buf = new StringBuilder();
        buf.append(ModQuestions.getBmlHeader(question));
        buf.append("text{type='bold';text='Power Scaling Stats'}");
        buf.append("text{text=''}");
        buf.append("text{text='Total Power: " + totalPower + "'}");
        // ... more manual BML construction
        buf.append(ModQuestions.createAnswerButton2(question));
        player.getCommunicator().sendBml(300, 300, true, true,
            buf.toString(), 200, 200, 200, "Power Scaling Stats");
    }

    @Override
    public void answer(Question question, Properties answers) {
        // Handle answers
    }
}

// ViewStatsActionPerformer.java (65+ lines)
public class ViewStatsActionPerformer implements ActionPerformer {
    @Override
    public short getActionId() {
        return (short) PowerScalingMod.ACTION_VIEW_POWER_STATS;
    }

    @Override
    public boolean action(Action action, Creature performer, Item target,
                         short num, float counter) {
        // Verify action ID, target, etc...
        PowerScalingStatsQuestion question = new PowerScalingStatsQuestion(performer);
        Question q = ModQuestions.createQuestion(performer, "Power Scaling Stats",
            "View your power level", performer.getWurmId(), question);
        q.sendQuestion();
        return propagate(action, ActionPropagation.FINISH_ACTION,
                       ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
    }
}

// PowerScalingMod.java
public static int ACTION_VIEW_POWER_STATS;

@Override
public void preInit() {
    // ...
}

@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    ACTION_VIEW_POWER_STATS = ModActions.getNextActionId();
    ModActions.registerAction(new ActionEntryBuilder(
        (short) ACTION_VIEW_POWER_STATS,
        "Power Fantasy",
        "viewing"
    ).build());
    ModActions.registerActionPerformer(new ViewStatsActionPerformer());
}

@SubscribeEvent
public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
    if (!event.getBodyPart().isBodyPartAttached()) return;
    if (event.getBodyPart().getOwnerId() != event.getPerformer().getWurmId()) return;
    event.addMenuItem(ModActions.getAction(ACTION_VIEW_POWER_STATS));
}
```

**New Pattern (UI API):**
```java
// PowerScalingMod.java - That's it, just one method!
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    MenuEntry entry = MenuEntry.builder("Power Fantasy")
        .actionVerb("viewing")
        .onClick(player -> {
            Creature p = (Creature) player;
            int power = manager.getPlayerPowerLevel(p.getWurmId());

            UIWindow window = UIWindow.builder("Power Scaling Stats")
                .addText("Power Scaling Stats", true)
                .addText("")
                .addText("Total Power: " + power)
                .addText("Damage: " + manager.getDamageMultiplier(power) + "x")
                .addText("Defense: " + manager.getDefenseMultiplier(power) + "x")
                .build();

            WindowManager.open(player, window);
        })
        .build();

    ContextMenuRegistry.getInstance().register("PowerScaling", MenuTarget.BODY, entry);
}
```

**Result:** 115 lines → 30 lines (74% reduction)

---

## Best Practices

### 1. Window Sizing

```java
// Good - reasonable sizes
UIWindow window = UIWindow.builder("Stats")
    .width(400).height(300)  // Fits most screens
    .build();

// Avoid - too large
UIWindow huge = UIWindow.builder("Too Big")
    .width(1920).height(1080)  // ❌ May not fit
    .build();
```

### 2. Text Formatting

```java
// Good - clear hierarchy
window.addHeader("Section Title")      // Bold + spacing
      .addText("Normal text")
      .addSeparator()                  // Visual break
      .addText("More text");

// Avoid - wall of text
window.addText("Text1")
      .addText("Text2")
      .addText("Text3");  // ❌ Hard to read
```

### 3. Visibility Filters

```java
// Good - specific conditions
MenuEntry entry = MenuEntry.builder("Admin Panel")
    .onlyFor(player -> {
        Creature c = (Creature) player;
        return hasPermission(c, "admin");
    })
    .build();

// Avoid - complex logic in filter
MenuEntry bad = MenuEntry.builder("Complex")
    .onlyFor(player -> {
        // ❌ 50 lines of logic here
    })
    .build();

// Better - extract to method
MenuEntry better = MenuEntry.builder("Complex")
    .onlyFor(this::canAccessFeature)
    .build();
```

### 4. Error Handling

```java
// Good - handle errors gracefully
MenuEntry entry = MenuEntry.builder("View Stats")
    .onClick(player -> {
        try {
            showStatsWindow((Creature) player);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error showing stats", e);
            ((Creature) player).getCommunicator()
                .sendAlertServerMessage("Failed to show stats window");
        }
    })
    .build();
```

### 5. Form Validation

```java
// Good - validate input
UIWindow window = UIWindow.builder("Settings")
    .addInput("multiplier", "Multiplier", "1.0")
    .onAnswer((p, answers) -> {
        String value = answers.getProperty("multiplier");
        try {
            double mult = Double.parseDouble(value);
            if (mult < 0.1 || mult > 10.0) {
                ((Creature) p).getCommunicator()
                    .sendNormalServerMessage("Multiplier must be 0.1-10.0");
                return;
            }
            // Save valid value
        } catch (NumberFormatException e) {
            ((Creature) p).getCommunicator()
                .sendNormalServerMessage("Invalid number format");
        }
    })
    .build();
```

---

## Examples

### Example 1: Simple Stats Display

```java
private void showPlayerStats(Creature player) {
    UIWindow window = UIWindow.builder("Player Stats")
        .width(400).height(300)
        .addHeader("Player Information")
        .addText("Name: " + player.getName())
        .addText("Level: " + getLevel(player))
        .addText("Experience: " + getXP(player))
        .addSeparator()
        .addHeader("Combat Stats")
        .addText("Damage: " + getDamage(player))
        .addText("Defense: " + getDefense(player))
        .build();

    WindowManager.open(player, window);
}
```

### Example 2: Configuration Form

```java
private void showConfigWindow(Creature player) {
    UIWindow window = UIWindow.builder("Mod Configuration")
        .width(500).height(400)
        .addHeader("Power Scaling Settings")
        .addText("Base power multiplier:")
        .addInput("baseMult", "Multiplier", "1.0")
        .addText("")
        .addText("Combat scaling:")
        .addDropdown("scaling", "Scaling", "Linear", "Exponential", "Logarithmic")
        .addText("")
        .addButton("save", "Save Settings")
        .addButton("reset", "Reset to Defaults")
        .onAnswer((p, answers) -> {
            if (answers.containsKey("reset")) {
                resetToDefaults();
                ((Creature) p).getCommunicator()
                    .sendNormalServerMessage("Settings reset to defaults");
            } else if (answers.containsKey("save")) {
                String mult = answers.getProperty("baseMult");
                String scaling = answers.getProperty("scaling");
                saveSettings(mult, scaling);
                ((Creature) p).getCommunicator()
                    .sendNormalServerMessage("Settings saved");
            }
        })
        .build();

    WindowManager.open(player, window);
}
```

### Example 3: Tiered Menu System

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    ContextMenuRegistry registry = ContextMenuRegistry.getInstance();

    // Tier 1 - Always visible
    registry.register("MyMod", MenuTarget.BODY,
        MenuEntry.builder("Basic Features")
            .onClick(player -> showBasicMenu((Creature) player))
            .build()
    );

    // Tier 2 - Requires level 25
    registry.register("MyMod", MenuTarget.BODY,
        MenuEntry.builder("Intermediate Features")
            .onlyFor(player -> getLevel((Creature) player) >= 25)
            .onClick(player -> showIntermediateMenu((Creature) player))
            .build()
    );

    // Tier 3 - Requires level 50
    registry.register("MyMod", MenuTarget.BODY,
        MenuEntry.builder("Advanced Features")
            .onlyFor(player -> getLevel((Creature) player) >= 50)
            .onClick(player -> showAdvancedMenu((Creature) player))
            .build()
    );
}
```

### Example 4: Multi-Step Wizard

```java
private void showWizardStep1(Creature player) {
    UIWindow step1 = UIWindow.builder("Setup Wizard - Step 1 of 3")
        .addHeader("Welcome")
        .addText("This wizard will configure your character.")
        .addText("")
        .addText("Choose your specialization:")
        .addDropdown("spec", "Specialization",
            "Combat", "Crafting", "Exploration")
        .addButton("next", "Next →")
        .onAnswer((p, answers) -> {
            String spec = answers.getProperty("spec");
            showWizardStep2((Creature) p, spec);
        })
        .build();

    WindowManager.open(player, step1);
}

private void showWizardStep2(Creature player, String spec) {
    UIWindow step2 = UIWindow.builder("Setup Wizard - Step 2 of 3")
        .addHeader("Configure " + spec)
        .addText("Specialization: " + spec, true)
        .addText("")
        .addText("Set difficulty:")
        .addDropdown("diff", "Difficulty", "Easy", "Normal", "Hard")
        .addButton("next", "Next →")
        .addButton("back", "← Back")
        .onAnswer((p, answers) -> {
            if (answers.containsKey("back")) {
                showWizardStep1((Creature) p);
            } else {
                String diff = answers.getProperty("diff");
                showWizardStep3((Creature) p, spec, diff);
            }
        })
        .build();

    WindowManager.open(player, step2);
}

// Step 3 similar...
```

---

## Troubleshooting

### Menu Entry Not Appearing

1. **Check initialization:**
   ```java
   // UI Framework should auto-initialize on server start
   // If not, check logs for errors
   ```

2. **Check visibility filter:**
   ```java
   // Test with no filter first
   MenuEntry entry = MenuEntry.builder("Test")
       // .onlyFor(...) // Comment out temporarily
       .onClick(player -> {...})
       .build();
   ```

3. **Check target type:**
   ```java
   // Make sure you're using the right target
   registry.register("MyMod", MenuTarget.BODY, entry);  // For body menus
   ```

### Window Not Displaying

1. **Check player type:**
   ```java
   if (player instanceof Creature && ((Creature) player).isPlayer()) {
       WindowManager.open(player, window);
   }
   ```

2. **Check for exceptions:**
   ```java
   try {
       WindowManager.open(player, window);
   } catch (Exception e) {
       logger.log(Level.SEVERE, "Failed to open window", e);
   }
   ```

### Form Answers Not Working

1. **Check element IDs:**
   ```java
   // IDs must match between addInput/addButton and answers
   window.addInput("myField", "Label", "")

   // In onAnswer:
   answers.getProperty("myField")  // Same ID
   ```

2. **Check button handling:**
   ```java
   .onAnswer((p, answers) -> {
       if (answers.containsKey("myButton")) {
           // Handle button click
       }
   })
   ```

---

## Additional Resources

- **BML Syntax Guide:** `/docs/BML_UI_GUIDE.md`
- **Example Code:** `/docs/examples/ui-api/`
- **Framework Source:** `/wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/ui/`
- **API Interfaces:** `/wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/ui/`

---

## FAQ

**Q: Do I still need to implement ModQuestion?**
A: No! The UI API handles all ModQuestion boilerplate automatically.

**Q: Can I still use the old pattern?**
A: Yes, the UI API is optional. Old mods will continue to work.

**Q: Does this work with existing mods?**
A: Yes, you can mix UI API with manual ModQuestion implementations.

**Q: Is this thread-safe?**
A: Yes, all UI API components are thread-safe.

**Q: Can I create custom BML elements?**
A: For advanced BML, you can still use ModQuestions and build BML manually. The UI API covers common use cases.

**Q: What about item/creature/tile menus?**
A: Currently only BODY menus are implemented. Other targets coming in future updates.

**Q: How do I debug BML issues?**
A: Check server logs for errors. The UI API validates BML syntax automatically.

---

## Version History

- **1.0.0** - Initial release
  - UIWindow builder API
  - WindowManager for display
  - ContextMenuRegistry for BODY menus
  - MenuEntry with visibility filters
  - Auto-initialization on server start

---

*For more information, see the framework source code or ask in the WurmModLoader community.*

# WurmModLoader UI API

**High-level BML & Context Menu API for mod developers**

---

## What is the UI API?

The UI API is a framework-level abstraction that makes it **easy** to create custom UI windows and context menu entries in Wurm Unlimited mods **without** writing raw BML or implementing low-level interfaces.

### Before & After

**Before (Manual Implementation):**
```java
// 115+ lines across 3 files
PowerScalingStatsQuestion implements ModQuestion    // 50+ lines
ViewStatsActionPerformer implements ActionPerformer // 65+ lines
Manual registration in PowerScalingMod               // Event handlers, action IDs
```

**After (UI API):**
```java
// 30 lines in 1 file
MenuEntry entry = MenuEntry.builder("Power Fantasy")
    .onClick(player -> {
        UIWindow window = UIWindow.builder("Power Scaling Stats")
            .addText("Total Power: " + power)
            .addText("Damage: " + damageMultiplier + "x")
            .build();
        WindowManager.open(player, window);
    })
    .build();

ContextMenuRegistry.getInstance().register("PowerScaling", MenuTarget.BODY, entry);
```

**Result:** 70% code reduction!

---

## Features

✅ **Fluent API** - Chainable method calls for readable UI construction
✅ **Automatic Action Management** - No manual action ID allocation
✅ **Visibility Filters** - Show/hide menus based on conditions
✅ **Type-Safe** - Compile-time validation instead of runtime BML errors
✅ **Thread-Safe** - Safe for concurrent mod access
✅ **Zero Wurm Imports in Mods** - Framework handles all game class interactions
✅ **Auto-Initialization** - Automatically initializes on server start

---

## Quick Start

### 1. Create a Context Menu Entry

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    MenuEntry entry = MenuEntry.builder("View Stats")
        .onClick(player -> showStatsWindow((Creature) player))
        .build();

    ContextMenuRegistry.getInstance()
        .register("MyMod", MenuTarget.BODY, entry);

    // This will appear under: Body Menu > Mods > View Stats
}
```

**Note:** All mod buttons are automatically grouped under a **"Mods"** submenu to keep the context menu organized!

### 2. Create a UI Window

```java
private void showStatsWindow(Creature player) {
    UIWindow window = UIWindow.builder("Player Stats")
        .width(400).height(300)
        .addHeader("Player Information")
        .addText("Name: " + player.getName())
        .addText("Level: " + getLevel(player))
        .addSeparator()
        .addText("Damage: " + getDamage(player))
        .build();

    WindowManager.open(player, window);
}
```

### 3. Add Visibility Filters

```java
MenuEntry entry = MenuEntry.builder("Admin Panel")
    .onlyFor(player -> isAdmin((Creature) player))
    .onClick(player -> showAdminPanel((Creature) player))
    .build();

// This will appear under: Mods > Admin Panel (only for admins)
```

### Menu Organization

All mod buttons registered through the UI API are **automatically grouped** under a **"Mods"** submenu:

```
Body Menu (right-click yourself):
  ...vanilla options...
  Mods >
    Example            (from TemplateMod)
    Power Fantasy      (from PowerScaling)
    Admin Panel        (from YourMod, if admin)
    ...other mods...
```

This keeps the context menu clean and organized!

---

## Core Components

| Component | Purpose |
|-----------|---------|
| **UIWindow** | Fluent API for building BML windows |
| **WindowManager** | Opens windows for players |
| **ContextMenuRegistry** | Manages context menu entries |
| **MenuEntry** | Builder for context menu items with filters |
| **VisibilityPredicate** | Functional interface for visibility checks |

---

## Documentation

- **Full Guide:** [`UI_API_GUIDE.md`](UI_API_GUIDE.md) - Complete reference with examples
- **Quick Reference:** [`UI_API_QUICK_REFERENCE.md`](UI_API_QUICK_REFERENCE.md) - Cheat sheet
- **BML Syntax:** [`BML_UI_GUIDE.md`](BML_UI_GUIDE.md) - Underlying BML reference
- **Examples:** [`docs/examples/ui-api/`](examples/ui-api/) - Working code samples

---

## API Reference (Quick)

### UIWindow Methods

```java
UIWindow.builder("Title")
    .width(400).height(300)          // Set dimensions
    .addText("Text")                 // Plain text
    .addText("Bold", true)           // Bold text
    .addHeader("Section")            // Header with spacing
    .addSeparator()                  // Horizontal line
    .addButton("id", "Label")        // Button
    .addInput("id", "Label", "val")  // Text input
    .addDropdown("id", "Label", ...) // Dropdown
    .onAnswer((p, ans) -> {...})     // Answer callback
    .build();
```

### MenuEntry Methods

```java
MenuEntry.builder("Label")
    .actionVerb("viewing")           // Action verb
    .onlyFor(predicate)              // Visibility filter
    .onlyOwnBody()                   // Only own body
    .onClick(player -> {...})        // Click handler
    .opensWindow(window)             // Open window shortcut
    .build();
```

---

## Architecture

### Framework Layer

```
wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/ui/
├── UIWindow.java              # Window interface
├── UIWindowBuilder.java       # Window builder implementation
├── MenuEntry.java             # Menu entry interface + builder
├── MenuTarget.java            # Menu target enum (BODY, ITEM, etc.)
└── VisibilityPredicate.java   # Visibility filter interface

wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/ui/
├── UIFramework.java           # Auto-initialization system
├── WindowManager.java         # Window display manager
└── ContextMenuRegistry.java   # Context menu registry
```

### Integration Points

1. **Bootstrap:** `SystemBootstrap.java` registers `UIFramework.AutoInitializer`
2. **Server Start:** `UIFramework` initializes when server starts
3. **Event System:** `ContextMenuRegistry` subscribes to `BodyMenuPopulateEvent`
4. **Action System:** Uses `ModActions` for action registration

---

## Migration Guide

### From Manual ModQuestion

**Old:**
```java
public class MyQuestion implements ModQuestion {
    @Override
    public void sendQuestion(Question question) {
        StringBuilder buf = new StringBuilder();
        buf.append(ModQuestions.getBmlHeader(question));
        buf.append("text{text='Hello'}");
        buf.append(ModQuestions.createAnswerButton2(question));
        player.getCommunicator().sendBml(...);
    }

    @Override
    public void answer(Question question, Properties answers) {
        // Handle
    }
}
```

**New:**
```java
UIWindow window = UIWindow.builder("Title")
    .addText("Hello")
    .onAnswer((p, ans) -> { /* Handle */ })
    .build();

WindowManager.open(player, window);
```

### From Manual Action Registration

**Old:**
```java
public static int MY_ACTION_ID;

@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    MY_ACTION_ID = ModActions.getNextActionId();
    ModActions.registerAction(new ActionEntryBuilder(...).build());
    ModActions.registerActionPerformer(new MyActionPerformer());
}

@SubscribeEvent
public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
    event.addMenuItem(ModActions.getAction(MY_ACTION_ID));
}
```

**New:**
```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    MenuEntry entry = MenuEntry.builder("My Action")
        .onClick(player -> doAction(player))
        .build();

    ContextMenuRegistry.getInstance()
        .register("MyMod", MenuTarget.BODY, entry);
}
```

---

## Examples

### Simple Stats Display

```java
UIWindow window = UIWindow.builder("Stats")
    .addHeader("Player Stats")
    .addText("Name: " + player.getName())
    .addText("Level: " + level)
    .build();
```

### Configuration Form

```java
UIWindow config = UIWindow.builder("Settings")
    .addDropdown("class", "Class", "Warrior", "Mage")
    .addInput("name", "Name", "")
    .addButton("save", "Save")
    .onAnswer((p, ans) -> {
        String cls = ans.getProperty("class");
        String name = ans.getProperty("name");
        saveSettings(cls, name);
    })
    .build();
```

### Tiered Menus

```java
// Basic - always visible
MenuEntry basic = MenuEntry.builder("Basic Features")
    .onClick(player -> showBasicMenu(player))
    .build();

// Advanced - requires level 50
MenuEntry advanced = MenuEntry.builder("Advanced Features")
    .onlyFor(player -> getLevel(player) >= 50)
    .onClick(player -> showAdvancedMenu(player))
    .build();

registry.register("MyMod", MenuTarget.BODY, basic);
registry.register("MyMod", MenuTarget.BODY, advanced);
```

### Multi-Step Wizard

See [`docs/examples/ui-api/SoulboundGearUIExample.java`](examples/ui-api/SoulboundGearUIExample.java) for complete wizard implementation.

---

## Troubleshooting

### Menu Not Appearing

1. Check logs for initialization errors
2. Verify you're calling `register()` in `onServerStarted()`
3. Test with no visibility filter first
4. Ensure you're using the correct `MenuTarget`

### Window Not Displaying

1. Verify player is a `Creature` instance
2. Check for exceptions in logs
3. Ensure window width/height are reasonable (300-600px)

### Form Answers Not Working

1. Verify element IDs match between `addInput()` and `answers.getProperty()`
2. Check button handling with `answers.containsKey("buttonId")`

---

## Best Practices

✓ Use `addHeader()` for section titles
✓ Add spacing with `addSeparator()`
✓ Keep windows 300-600px wide
✓ Validate form input before processing
✓ Extract complex visibility logic to methods
✓ Handle exceptions gracefully
✓ Log errors for debugging

---

## Limitations

- Currently supports **BODY** menus only (ITEM/CREATURE/TILE coming in future)
- Windows are modal (player must close to continue)
- BML limitations apply (no custom CSS, limited styling)

---

## Version History

**1.0.0** - Initial release
- UIWindow builder API
- WindowManager for display
- ContextMenuRegistry for BODY menus
- MenuEntry with visibility filters
- Auto-initialization on server start

---

## Contributing

To add new UI components:

1. Add interface to `wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/ui/`
2. Add implementation to `wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/ui/`
3. Update `UIWindowBuilder` if adding new BML elements
4. Add examples to `docs/examples/ui-api/`
5. Update documentation

---

## Support

- **Issues:** Open an issue on the WurmModLoader repository
- **Documentation:** See `docs/UI_API_GUIDE.md`
- **Examples:** See `docs/examples/ui-api/`
- **Community:** Ask in WurmModLoader Discord/Forums

---

## License

Part of the WurmModLoader framework. Same license as the main project.

---

**Happy Modding!** 🎮

# UI API Quick Reference

## Simple Window

```java
UIWindow window = UIWindow.builder("Title")
    .width(400).height(300)
    .addText("Hello!")
    .build();

WindowManager.open(player, window);
```

## Context Menu Entry

```java
MenuEntry entry = MenuEntry.builder("My Action")
    .onClick(player -> doSomething(player))
    .build();

ContextMenuRegistry.getInstance()
    .register("ModName", MenuTarget.BODY, entry);

// Appears under: Mods > My Action
```

**Note:** All mod buttons are grouped under a "Mods" submenu!

## Window with Form

```java
UIWindow form = UIWindow.builder("Settings")
    .addInput("name", "Name", "")
    .addDropdown("class", "Class", "Warrior", "Mage")
    .addButton("save", "Save")
    .onAnswer((p, answers) -> {
        String name = answers.getProperty("name");
        String cls = answers.getProperty("class");
        // Handle submission
    })
    .build();
```

## Visibility Filter

```java
MenuEntry entry = MenuEntry.builder("Admin Panel")
    .onlyFor(player -> isAdmin(player))
    .onClick(player -> showAdminPanel(player))
    .build();
```

## All Window Elements

| Method | Usage |
|--------|-------|
| `addText(text)` | Plain text |
| `addText(text, true)` | Bold text |
| `addHeader(text)` | Header with spacing |
| `addSeparator()` | Horizontal line |
| `addButton(id, label)` | Button |
| `addInput(id, label, default)` | Text input |
| `addDropdown(id, label, opts)` | Dropdown |

## Complete Example

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    MenuEntry stats = MenuEntry.builder("View Stats")
        .actionVerb("viewing")
        .onClick(player -> {
            Creature p = (Creature) player;

            UIWindow window = UIWindow.builder("Player Stats")
                .width(400).height(300)
                .addHeader("Stats")
                .addText("Name: " + p.getName())
                .addText("Level: " + getLevel(p))
                .addSeparator()
                .addText("Combat stats:")
                .addText("  Damage: " + getDamage(p))
                .addText("  Defense: " + getDefense(p))
                .build();

            WindowManager.open(player, window);
        })
        .build();

    ContextMenuRegistry.getInstance()
        .register("MyMod", MenuTarget.BODY, stats);
}
```

## Migration Cheat Sheet

| Old Pattern | New Pattern |
|-------------|-------------|
| `implements ModQuestion` | `UIWindow.builder()` |
| `implements ActionPerformer` | `MenuEntry.builder().onClick()` |
| `ModActions.getNextActionId()` | *(automatic)* |
| `ModActions.registerAction()` | *(automatic)* |
| `@SubscribeEvent BodyMenuPopulate` | *(automatic)* |
| `buf.append("text{...}")` | `.addText()` |
| `Question.sendQuestion()` | `WindowManager.open()` |

**Result:** 70% less code!

# Template Mod Example

A comprehensive example mod demonstrating the WurmModLoader UI API without requiring any BML knowledge.

## What It Does

This mod adds **two interactive examples** to your body's context menu (right-click on yourself):

### 1. Simple Example
A basic button that demonstrates:
- Context menu registration
- Simple message sending
- Player interaction

When clicked:
1. You receive an examine text message: *"You examine yourself and feel a sense of wonder."*
2. You receive a thank you message: *"Thank you for clicking the Example Button YourName!"*

### 2. Interactive Questionnaire
A **multi-page questionnaire** that showcases the full power of the modern UI API:
- **Multiple pages** with Next/Previous navigation
- **Dropdown selections** for multiple-choice questions
- **State preservation** across page transitions
- **Dynamic responses** based on user selections
- **Zero BML knowledge required!**

When clicked, opens a 2-page questionnaire where you can:
- Answer multiple-choice questions using dropdowns
- Navigate between pages
- See your previous selections preserved
- Submit and receive a personalized thank you message

**Note:** Each mod automatically gets its own submenu at the top level:
- **TemplateMod** (your mod's submenu)
  - Simple Example
  - Questionnaire

This keeps the context menu clean and organized, with all your mod's features grouped together!

## Purpose

This template serves as a starting point for creating new mods. It demonstrates:

- **Mod Structure**: Proper directory layout and build configuration
- **UI API**: Complete showcase of the modern UI API (context menus, windows, forms)
- **Event System**: How to subscribe to game events using `@SubscribeEvent`
- **Player Interaction**: Simple messages and complex interactive forms
- **State Management**: Preserving data across multiple window pages
- **Zero BML Required**: Build complex UIs without learning BML syntax
- **Minimal Dependencies**: Only requires the core WurmModLoader API

## Building

From the WurmModLoader root directory:

```bash
./gradlew :examples:templatemod:build
```

This creates:
```
build/distribution/
  mods/
    templatemod.properties
    templatemod/
      templatemod.jar
```

## Installation

Copy the entire `build/distribution/mods/` contents to your Wurm server's `mods/` directory:

```bash
cp -r examples/templatemod/build/distribution/mods/* \
  ~/.local/share/Steam/steamapps/common/Wurm\ Unlimited\ Dedicated\ Server/mods/
```

## File Structure

```
templatemod/
├── build.gradle.kts          # Build configuration
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/garward/wurmmodloader/examples/templatemod/
│   │           └── TemplateMod.java    # Main mod class
│   └── dist/
│       └── templatemod.properties      # Mod configuration
└── README.md
```

## Code Walkthrough

### TemplateMod.java

#### Registering Context Menu Buttons

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    // Simple message button
    MenuEntry exampleButton = MenuEntry.builder("Simple Example")
        .actionVerb("clicking")
        .onClick(player -> handleExampleClick((Creature) player))
        .build();

    // Questionnaire button
    MenuEntry questionnaireButton = MenuEntry.builder("Questionnaire")
        .actionVerb("opening")
        .onClick(player -> openQuestionnaire((Creature) player))
        .build();

    // Register both buttons for body menus
    ContextMenuRegistry registry = ContextMenuRegistry.getInstance();
    registry.register("TemplateMod", MenuTarget.BODY, exampleButton);
    registry.register("TemplateMod", MenuTarget.BODY, questionnaireButton);
}
```

The `@SubscribeEvent` annotation registers this method to run when the server starts. It uses the **UI API** to create context menu buttons:

- `MenuEntry.builder("Label")` - Creates a button with the given label
- `.actionVerb("verb")` - Sets the action verb shown to the player
- `.onClick(...)` - Sets what happens when the button is clicked
- `ContextMenuRegistry.getInstance().register(...)` - Registers the button

#### Handling Simple Button Click

```java
private void handleExampleClick(Creature player) {
    String playerName = player.getName();

    // Send examine text (like when examining an item)
    player.getCommunicator().sendNormalServerMessage(
        "You examine yourself and feel a sense of wonder.");

    // Send the thank you message in event text style
    player.getCommunicator().sendSafeServerMessage(
        "Thank you for clicking the Example Button " + playerName + "!");

    logger.info("Example button clicked by: " + playerName);
}
```

This method sends two messages to the player when they click the button.

#### Opening the Questionnaire

```java
private void openQuestionnaire(Creature player) {
    QuestionnaireExample questionnaire = new QuestionnaireExample(player);
    questionnaire.show();
}
```

This creates and displays the multi-page questionnaire - that's it! No BML, no ModQuestion implementation needed.

### QuestionnaireExample.java - The Modern UI API Showcase

This file demonstrates how to build complex, multi-page forms using only the high-level UI API.

#### Building a Window with UIWindow.builder()

```java
private UIWindow buildPage1() {
    return UIWindow.builder("Example Questionnaire - Page 1 of 2")
        .width(500)
        .height(350)
        .addHeader("Question 1: What is your favorite color?")
        .addText("")
        .addDropdown("choice", "Select an option:",
            "Example Option 1",
            "Example Option 2",
            "Example Option 3")
        .addText(page1Selection != null ?
            "Current selection: " + page1Selection :
            "No selection yet")
        .addSeparator()
        .addButton("next", "Next >>")
        .onAnswer((p, answers) -> handlePage1Answer(answers))
        .build();
}
```

**Key takeaways:**
- **Fluent API**: Chain methods to build your UI
- **No BML**: Use `.addHeader()`, `.addText()`, `.addDropdown()`, etc.
- **Form Elements**: Dropdowns, inputs, buttons all built-in
- **Event Handling**: `.onAnswer()` handles form submission

#### Handling Multi-Page Navigation

```java
private void handlePage1Answer(Properties answers) {
    String nextButton = answers.getProperty("next");

    if (nextButton != null) {
        // Save the selection from the dropdown
        String choice = answers.getProperty("choice");
        if (choice != null && !choice.isEmpty()) {
            page1Selection = choice;
        }

        // Navigate to next page
        currentPage = 1;
        showCurrentPage();
    }
}
```

**Key takeaways:**
- **Button Detection**: Check which button was clicked with `answers.getProperty("buttonId")`
- **Form Data**: Get dropdown/input values with `answers.getProperty("fieldId")`
- **State Management**: Save selections to instance variables
- **Page Navigation**: Update page counter and call `showCurrentPage()` to reopen

#### Displaying the Window

```java
private void showCurrentPage() {
    UIWindow window;

    if (currentPage == 0) {
        window = buildPage1();
    } else {
        window = buildPage2();
    }

    WindowManager.open(player, window);
}
```

**Key takeaways:**
- Build the appropriate page based on `currentPage`
- Use `WindowManager.open()` to display the window
- That's it! No Question objects, no sendBml(), no BML strings

### templatemod.properties

```properties
classname=com.garward.wurmmodloader.examples.templatemod.TemplateMod
```

This tells the mod loader which class to instantiate.

## Customization Ideas

To create your own mod based on this template:

1. **Rename the package**: Change `com.garward.wurmmodloader.examples.templatemod` to your own package
2. **Update properties file**: Change the `classname` to match your renamed class
3. **Change button labels**: Modify `MenuEntry.builder("Label")` to your button text
4. **Add more windows**: Copy the `QuestionnaireExample` pattern to create new UIs
5. **Add visibility filters**: Use `.onlyFor(predicate)` to show buttons conditionally
6. **Update build.gradle.kts**: Change `archiveBaseName` to your mod name
7. **Extend the questionnaire**: Add more pages, different form elements, conditional logic

## UI API Features - Zero BML Knowledge Required!

The WurmModLoader UI API provides everything you need without learning BML syntax:

### Context Menu Buttons
```java
MenuEntry button = MenuEntry.builder("My Button")
    .actionVerb("using")
    .onlyFor(player -> /* visibility condition */)  // Optional
    .onClick(player -> /* button action */)
    .build();

ContextMenuRegistry.getInstance().register("MyMod", MenuTarget.BODY, button);
```

### Popup Windows with Forms
```java
UIWindow window = UIWindow.builder("My Window")
    .width(500).height(400)
    .addHeader("Welcome!")
    .addText("Fill out this form:")
    .addInput("name", "Your Name:", "")
    .addDropdown("role", "Your Role:", "Warrior", "Mage", "Rogue")
    .addButton("submit", "Submit")
    .onAnswer((player, answers) -> {
        String name = answers.getProperty("name");
        String role = answers.getProperty("role");
        // Handle submission...
    })
    .build();

WindowManager.open(player, window);
```

### Available UI Elements
- **Text**: `.addText("text")` / `.addHeader("header")` - Display text to the player
- **Separators**: `.addSeparator()` - Visual divider lines
- **Inputs**: `.addInput("id", "label", "default")` - Text input fields
- **Dropdowns**: `.addDropdown("id", "label", "opt1", "opt2", ...)` - Multiple choice
- **Buttons**: `.addButton("id", "label")` - Clickable buttons

### Available Menu Targets
- `MenuTarget.BODY` - Body context menus (right-click on body)
- `MenuTarget.ITEM` - Item context menus
- `MenuTarget.CREATURE` - Creature context menus
- `MenuTarget.TILE` - Tile context menus

### Multi-Page Patterns
See `QuestionnaireExample.java` for a complete working example of:
- Managing page state
- Navigating between pages
- Preserving user input
- Conditional button display

See `docs/UI_API_GUIDE.md` for complete documentation.

## Available Events

Some commonly used events you can subscribe to:

- `ServerStartedEvent` - When the server finishes starting (used in this example)
- `CreatureDeathEvent` - When a creature dies
- `ItemExamineEvent` - When an item is examined
- `CombatDamageEvent` - When combat damage is dealt
- `BodyMenuPopulateEvent` - When a body context menu is populated

See `wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/` for the full list.

## Testing

1. Build the mod
2. Copy to server mods directory
3. Start the server
4. Log in to the game
5. Right-click on your body
6. Click **"TemplateMod"** to open this mod's submenu

### Test the Simple Example:
7. Click **"Simple Example"**
8. You should see two messages:
   - *"You examine yourself and feel a sense of wonder."*
   - *"Thank you for clicking the Example Button YourName!"*

### Test the Questionnaire:
7. Click **"Questionnaire"**
8. A window will open showing "Page 1 of 2"
9. Select an option from the dropdown
10. Click **"Next >>"**
11. You'll see Page 2 with your previous selection preserved
12. Select another option
13. Click **"<< Previous"** to go back (your selection is preserved)
14. Click **"Next >>"** again to return to Page 2
15. Click **"Submit"**
16. You should receive a thank you message showing all your selections!

**Menu Path:** Right-click body → TemplateMod → [Select button]

## License

This example is provided as a template for creating your own mods. Feel free to use and modify it as needed.

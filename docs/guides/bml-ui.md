# Wurm Unlimited BML UI Guide for Modders

**Complete reference for creating custom UI windows in Wurm Unlimited mods**

## Overview

BML (Basic Markup Language) is Wurm's UI markup system for creating custom dialogs and windows. This guide documents the syntax by analyzing vanilla Wurm Question implementations.

---

## Critical Syntax Rules ⚠️

### The #1 Rule: NO Semicolons Between Elements!

**WRONG:**
```java
buf.append("text{text='Line 1'}");  // ❌ NO semicolon here!
buf.append("text{text='Line 2'}");
```

**CORRECT:**
```java
buf.append("text{text='Line 1'}");  // ✅ No semicolon between appends
buf.append("text{text='Line 2'}");
```

### Semicolons ARE Used Inside Container Elements

**Example (table):**
```java
buf.append("table{rows='1';cols='3';");  // ✅ Semicolons between properties
buf.append("text{text='Cell 1'};");      // ✅ Semicolon between table cells
buf.append("text{text='Cell 2'};");
buf.append("text{text='Cell 3'}");
buf.append("}");  // Close table
```

### Quote Style

- **Preferred:** Single quotes `text='value'`
- **Also works:** Double quotes `text="value"`
- **Be consistent** - don't mix in the same element

### No Spaces Around `=`

**CORRECT:** `text='value'`
**WRONG:** `text = 'value'`

---

## Basic Structure Pattern

Every BML window follows this pattern:

```java
@Override
public void sendQuestion(Question question) {
    StringBuilder buf = new StringBuilder();

    // 1. Header (required)
    buf.append(ModQuestions.getBmlHeader(question));

    // 2. Content (your UI elements)
    buf.append("text{text='Welcome!'}");
    buf.append("text{text='Enter your name:'}");
    buf.append("input{id='username';maxchars='20'}");

    // 3. Button (required)
    buf.append(ModQuestions.createAnswerButton2(question));

    // 4. Send to player
    player.getCommunicator().sendBml(
        300,           // width
        300,           // height
        true,          // closeable
        true,          // resizable
        buf.toString(),
        200, 200, 200, // RGB background color
        "Window Title"
    );
}
```

---

## Header Methods

These are provided by `ModQuestions` (use these instead of Question's private methods):

### `getBmlHeader(question)`
Standard header for most windows.

```java
buf.append(ModQuestions.getBmlHeader(question));
```

### `getBmlHeaderWithScroll(question)`
Header with scrollable content area.

```java
buf.append(ModQuestions.getBmlHeaderWithScroll(question));
```

### `getBmlHeaderNoQuestion(question)`
Header without question text.

```java
buf.append(ModQuestions.getBmlHeaderNoQuestion(question));
```

---

## Button Methods

### `createAnswerButton2(question)`
Standard "Submit" button - **most commonly used**.

```java
buf.append(ModQuestions.createAnswerButton2(question));
```

### `createAnswerButton2(question, String text)`
Custom button text.

```java
buf.append(ModQuestions.createAnswerButton2(question, "Accept Quest"));
```

### `createOkAnswerButton(question)`
Standard "OK" button for read-only displays.

```java
buf.append(ModQuestions.createOkAnswerButton(question));
```

---

## Text Display Elements

### `text{}`
Basic text display with optional formatting.

**Simple text:**
```java
buf.append("text{text='This is a simple message'}");
```

**Bold text:**
```java
buf.append("text{type='bold';text='Important!'}");
```

**Bold + italic:**
```java
buf.append("text{type='bolditalic';text='Very Important!'}");
```

**Empty line (spacing):**
```java
buf.append("text{text=''}");
```

**Source:** `AlertServerMessageQuestion.java:157-159`, `AchievementCreation.java:126`

---

### `label{}`
Label text, typically used in forms.

**Simple label:**
```java
buf.append("label{text='Player name:'}");
```

**Bold label:**
```java
buf.append("label{type='bold';text='Settings'}");
```

**Source:** `CreatureCreationQuestion.java:56`, `SkillProgressQuestion.java:121`

---

## Input Elements

### `input{}`
Text input field.

**Basic input:**
```java
buf.append("input{id='playername';maxchars='20'}");
```

**Input with default value:**
```java
buf.append("input{id='age';maxchars='3';text='25'}");
```

**In horizontal layout with label:**
```java
buf.append("harray{label{text='Name:'}input{id='name';maxchars='40'}}");
```

**Attributes:**
- `id='...'` - **Required** - ID to retrieve value from `answer(Properties answers)`
- `maxchars='N'` - Maximum character length
- `text='...'` - Default/prefilled value

**Retrieving value in answer():**
```java
@Override
public void answer(Question question, Properties answers) {
    String name = answers.getProperty("name");  // Get value by ID
}
```

**Source:** `CreatureCreationQuestion.java:56`, `AlertServerMessageQuestion.java:160-167`

---

### `dropdown{}`
Dropdown selection menu.

**Basic dropdown:**
```java
buf.append("dropdown{id='skillchoice';options='Mining,Digging,Masonry'}");
```

**With dynamic options:**
```java
buf.append("dropdown{id='creature';options=\"");
for (int i = 0; i < creatures.length; i++) {
    if (i > 0) buf.append(",");
    buf.append(creatures[i].getName());
}
buf.append("\"}");
```

**Attributes:**
- `id='...'` - **Required** - ID to retrieve selection
- `options='opt1,opt2,opt3'` - Comma-separated list (NO SPACES!)

**Retrieving selected index:**
```java
@Override
public void answer(Question question, Properties answers) {
    String selected = answers.getProperty("skillchoice");
    int index = Integer.parseInt(selected);  // Returns index (0, 1, 2...)
}
```

**Source:** `CreatureCreationQuestion.java:60-70`, `SkillProgressQuestion.java:94-104`

---

### `checkbox{}`
Checkbox for boolean selection.

**Basic checkbox:**
```java
buf.append("checkbox{id='agree';selected='false';text='I agree to terms'}");
```

**Default checked:**
```java
buf.append("checkbox{id='enabled';selected='true';text='Enable feature'}");
```

**Attributes:**
- `id='...'` - **Required** - ID to retrieve state
- `selected='true'` or `selected='false'` - Default state
- `text='...'` - Label text

**Retrieving value:**
```java
@Override
public void answer(Question question, Properties answers) {
    String enabled = answers.getProperty("enabled");
    boolean isChecked = "true".equals(enabled);
}
```

**Source:** `AchievementCreation.java:143`

---

### `radio{}`
Radio button for mutually exclusive selections.

**Radio group example (gender selection):**
```java
buf.append("table{rows='1';cols='3';");
buf.append("text{type='bold';text='Gender'};");
buf.append("radio{group='gender';id='female';text='Female'};");
buf.append("radio{group='gender';id='male';text='Male';selected='true'}");
buf.append("}");
```

**Radio group example (trait selection):**
```java
buf.append("table{rows='1';cols='2';");
buf.append("radio{group='trait';id='0';selected='true'};label{text='None'};");
buf.append("}");
buf.append("table{rows='3';cols='4';");
buf.append("radio{group='trait';id='1'};label{text='Alert'};");
buf.append("radio{group='trait';id='2'};label{text='Angry'};");
buf.append("radio{group='trait';id='3'};label{text='Fierce'};");
// ... more options ...
buf.append("}");
```

**Attributes:**
- `group='...'` - **Required** - Group name (all radios in group must have same name)
- `id='...'` - **Required** - ID of this option
- `text='...'` - Label text
- `selected='true'` - Default selected (only ONE per group!)

**Retrieving value:**
```java
@Override
public void answer(Question question, Properties answers) {
    String selectedId = answers.getProperty("gender");  // Returns ID of selected radio
    boolean isFemale = "female".equals(selectedId);
}
```

**Source:** `CreatureCreationQuestion.java:72-92`

---

### `button{}`
Custom action button (not submit button).

**Basic button:**
```java
buf.append("button{text='Show Details';id='details'}");
```

**Button with tooltip:**
```java
buf.append("button{text='Close';id='close';hover='Close the window'}");
```

**Attributes:**
- `text='...'` - Button label
- `id='...'` - ID to detect which button was clicked
- `hover='...'` - Tooltip text
- `default='true'` - Make this the default action (Enter key)

**Detecting button click:**
```java
@Override
public void answer(Question question, Properties answers) {
    String action = answers.getProperty("details");
    if (action != null) {
        // "details" button was clicked
    }
}
```

**Source:** `CookBookQuestion.java:390`, `CookBookQuestion.java:473`

---

## Layout Containers

### `harray{}`
Horizontal layout - arranges children left-to-right.

**Label + input:**
```java
buf.append("harray{label{text='Name:'}input{id='name';maxchars='40'}}");
```

**Multiple elements:**
```java
buf.append("harray{");
buf.append("label{text='Age:'}");
buf.append("input{id='age';maxchars='3';text='0'}");
buf.append("label{text=' (0 = random)'}");
buf.append("}");
```

**Source:** `CreatureCreationQuestion.java:56-76`, `SkillProgressQuestion.java:121-128`

---

### `varray{}`
Vertical layout - arranges children top-to-bottom.

**Simple vertical stack:**
```java
buf.append("varray{");
buf.append("text{text='Line 1'}");
buf.append("text{text='Line 2'}");
buf.append("text{text='Line 3'}");
buf.append("}");
```

**With rescale:**
```java
buf.append("varray{rescale='true';");
buf.append("text{text='Content here'}");
buf.append("}");
```

**Source:** `CookBookQuestion.java:390`

---

### `table{}`
Grid layout with rows and columns.

**Simple 1x3 table:**
```java
buf.append("table{rows='1';cols='3';");
buf.append("text{type='bold';text='Header'};");
buf.append("text{text='Cell 2'};");
buf.append("text{text='Cell 3'}");
buf.append("}");
```

**Radio button grid (3 rows x 4 cols):**
```java
buf.append("table{rows='3';cols='4';");
buf.append("radio{group='trait';id='1'};label{text='Alert'};");
buf.append("radio{group='trait';id='2'};label{text='Angry'};");
buf.append("radio{group='trait';id='3'};label{text='Fierce'};");
buf.append("radio{group='trait';id='4'};label{text='Slow'};");
// ... 12 total cells (3 rows * 4 cols)
buf.append("}");
```

**Attributes:**
- `rows='N'` - Number of rows
- `cols='N'` - Number of columns
- **Must provide exactly rows × cols children!**

**Source:** `CreatureCreationQuestion.java:71-92`

---

### `border{}`
Bordered container with optional padding.

**Simple border:**
```java
buf.append("border{");
buf.append("text{text='Content inside border'}");
buf.append("}");
```

**Border with size:**
```java
buf.append("border{size='20,20';null;null;");
buf.append("text{text='Content with 20px padding'}");
buf.append(";null;null}");
```

**Complex nested border:**
```java
buf.append("border{border{size='20,25';");
buf.append("null;null;");  // top-left/top-right padding
buf.append("label{type='bold';text='Title'};");  // top-center
buf.append("harray{button{text='Close';id='close'}};");  // top-right content
buf.append("null;");  // close top
buf.append("}");  // close inner border
buf.append("null;");  // left side
buf.append("scroll{vertical='true';varray{");  // center content
buf.append("text{text='Scrollable content'}");
buf.append("}}");  // close scroll and border
buf.append("}");  // close outer border
```

**Attributes:**
- `size='W,H'` - Width and height in pixels

**Source:** `CookBookQuestion.java:390`, `CookBookQuestion.java:473`

---

### `center{}`
Centers child content.

```java
buf.append("center{");
buf.append("text{type='bold';text='Centered Title'}");
buf.append("}");
```

**Source:** `CookBookQuestion.java:390`

---

### `scroll{}`
Scrollable content area.

**Vertical scroll:**
```java
buf.append("scroll{vertical='true';horizontal='false';");
buf.append("varray{");
buf.append("text{text='Line 1'}");
buf.append("text{text='Line 2'}");
// ... many more lines ...
buf.append("}}");  // close varray and scroll
```

**Attributes:**
- `vertical='true'` - Enable vertical scrolling
- `horizontal='true'` - Enable horizontal scrolling

**Source:** `CookBookQuestion.java:390`

---

## Advanced Elements

### `passthrough{}`
Passes hidden data to the answer handler.

```java
buf.append("passthrough{id='id';text='" + question.getId() + "'}");
```

Used internally - rarely needed in custom UIs.

**Source:** `CookBookQuestion.java:390`

---

### `header{}`
Large header text.

```java
buf.append("header{text='Welcome to My Mod'}");
```

**Source:** `CookBookQuestion.java:390`

---

## Complete Working Examples

### Example 1: Simple Stats Display (Read-Only)

```java
@Override
public void sendQuestion(Question question) {
    PowerScalingManager manager = PowerScalingManager.getInstance();

    int totalPower = manager.getPlayerPowerLevel(player.getWurmId());
    float damageMultiplier = manager.getDamageMultiplier(totalPower);
    float defenseMultiplier = manager.getDefenseMultiplier(totalPower);
    float hpMultiplier = manager.getHpMultiplier(totalPower);

    StringBuilder buf = new StringBuilder();
    buf.append(ModQuestions.getBmlHeader(question));
    buf.append("text{type='bold';text='Power Scaling Stats'}");
    buf.append("text{text=''}");  // spacing
    buf.append("text{text='Total Power: " + totalPower + "'}");
    buf.append("text{text='Damage Multiplier: " + String.format("%.1fx", damageMultiplier) + "'}");
    buf.append("text{text='Defense Multiplier: " + String.format("%.1fx", defenseMultiplier) + "'}");
    buf.append("text{text='HP Multiplier: " + String.format("%.1fx", hpMultiplier) + "'}");
    buf.append(ModQuestions.createAnswerButton2(question));

    player.getCommunicator().sendBml(300, 300, true, true, buf.toString(), 200, 200, 200, "Power Scaling Stats");
}
```

**Source:** Working example from PowerScaling mod

---

### Example 2: Input Form

```java
@Override
public void sendQuestion(Question question) {
    StringBuilder buf = new StringBuilder();
    buf.append(ModQuestions.getBmlHeader(question));

    buf.append("text{text='Configure your character:'}");
    buf.append("text{text=''}");

    // Name input
    buf.append("harray{label{text='Name:'}input{id='charname';maxchars='20'}}");

    // Age input
    buf.append("harray{label{text='Age:'}input{id='age';maxchars='3';text='25'}}");

    // Class selection
    buf.append("harray{label{text='Class:'}dropdown{id='class';options='Warrior,Mage,Rogue,Cleric'}}");

    // Gender radio buttons
    buf.append("text{type='bold';text='Gender'}");
    buf.append("table{rows='1';cols='2';");
    buf.append("radio{group='gender';id='male';text='Male';selected='true'};");
    buf.append("radio{group='gender';id='female';text='Female'}");
    buf.append("}");

    // Enable PvP checkbox
    buf.append("checkbox{id='pvp';selected='false';text='Enable PvP mode'}");

    buf.append(ModQuestions.createAnswerButton2(question));

    player.getCommunicator().sendBml(350, 400, true, true, buf.toString(), 200, 200, 200, "Character Setup");
}

@Override
public void answer(Question question, Properties answers) {
    String name = answers.getProperty("charname");
    String ageStr = answers.getProperty("age");
    String classIdx = answers.getProperty("class");
    String gender = answers.getProperty("gender");
    String pvpEnabled = answers.getProperty("pvp");

    int age = Integer.parseInt(ageStr);
    boolean isPvP = "true".equals(pvpEnabled);
    boolean isMale = "male".equals(gender);

    String[] classes = {"Warrior", "Mage", "Rogue", "Cleric"};
    String selectedClass = classes[Integer.parseInt(classIdx)];

    player.getCommunicator().sendNormalServerMessage(
        "Created character: " + name + " (" + selectedClass + "), Age " + age
    );
}
```

**Based on patterns from:** `CreatureCreationQuestion.java`, `AchievementCreation.java`

---

### Example 3: Scrollable List with Table Layout

```java
@Override
public void sendQuestion(Question question) {
    StringBuilder buf = new StringBuilder();
    buf.append(ModQuestions.getBmlHeaderWithScroll(question));

    buf.append("text{type='bold';text='Player Rankings'}");
    buf.append("text{text=''}");

    // Header row
    buf.append("table{rows='1';cols='3';");
    buf.append("text{type='bold';text='Rank'};");
    buf.append("text{type='bold';text='Player'};");
    buf.append("text{type='bold';text='Score'}");
    buf.append("}");

    // Data rows
    String[] players = {"Alice", "Bob", "Charlie"};
    int[] scores = {1250, 980, 750};

    for (int i = 0; i < players.length; i++) {
        buf.append("table{rows='1';cols='3';");
        buf.append("text{text='" + (i + 1) + "'};");
        buf.append("text{text='" + players[i] + "'};");
        buf.append("text{text='" + scores[i] + "'}");
        buf.append("}");
    }

    buf.append(ModQuestions.createAnswerButton2(question));

    player.getCommunicator().sendBml(400, 500, true, true, buf.toString(), 200, 200, 200, "Rankings");
}
```

**Based on patterns from:** `SkillProgressQuestion.java`, `CreatureCreationQuestion.java`

---

## Common Mistakes and Solutions

### ❌ Mistake #1: Adding Semicolons Between Elements

**WRONG:**
```java
buf.append("text{text='Line 1'};");  // ❌ Semicolon at end
buf.append("text{text='Line 2'};");
```

**CORRECT:**
```java
buf.append("text{text='Line 1'}");  // ✅ No semicolon
buf.append("text{text='Line 2'}");
```

**Why it fails:** Causes `<input error>` in the client.

---

### ❌ Mistake #2: Wrong Header Method

**WRONG:**
```java
buf.append(question.getBmlHeader());  // ❌ Private method, can't access
```

**CORRECT:**
```java
buf.append(ModQuestions.getBmlHeader(question));  // ✅ Public wrapper
```

---

### ❌ Mistake #3: Forgetting Dropdown Commas

**WRONG:**
```java
buf.append("dropdown{id='choice';options='Option 1 Option 2 Option 3'}");
```

**CORRECT:**
```java
buf.append("dropdown{id='choice';options='Option1,Option2,Option3'}");
// Or with spaces replaced:
buf.append("dropdown{id='choice';options='Option_1,Option_2,Option_3'}");
```

---

### ❌ Mistake #4: Wrong Table Cell Count

**WRONG:**
```java
buf.append("table{rows='2';cols='2';");  // Says 2x2 = 4 cells
buf.append("text{text='Cell 1'};");
buf.append("text{text='Cell 2'}");       // Only 2 cells! ❌
buf.append("}");
```

**CORRECT:**
```java
buf.append("table{rows='2';cols='2';");
buf.append("text{text='Cell 1'};");
buf.append("text{text='Cell 2'};");
buf.append("text{text='Cell 3'};");
buf.append("text{text='Cell 4'}");  // Exactly 4 cells ✅
buf.append("}");
```

---

### ❌ Mistake #5: Multiple Radios Selected in Same Group

**WRONG:**
```java
buf.append("radio{group='gender';id='male';selected='true'};");   // ❌ Both selected!
buf.append("radio{group='gender';id='female';selected='true'}");  // ❌
```

**CORRECT:**
```java
buf.append("radio{group='gender';id='male';selected='true'};");   // ✅ Only one
buf.append("radio{group='gender';id='female'}");                  // ✅ Default unchecked
```

---

## Debugging Tips

### Enable BML Logging

Add logging to see exactly what BML you're generating:

```java
String finalBml = buf.toString();
Logger.getLogger("MyMod").severe("GENERATED BML:\n" + finalBml);
player.getCommunicator().sendBml(..., finalBml, ...);
```

### Test with Minimal BML First

Start with the absolute minimum and add complexity:

```java
// MINIMAL TEST (always works):
buf.append(ModQuestions.getBmlHeader(question));
buf.append("text{text='Hello World'}");
buf.append(ModQuestions.createAnswerButton2(question));

// Then incrementally add your elements one by one
```

### Check for `<input error>`

If you see `<input error>` in the window:
1. **Semicolon between appends?** Remove it!
2. **Missing closing brace?** Count your `{` and `}`
3. **Wrong attribute syntax?** Check for spaces around `=`
4. **Invalid element name?** Refer to this guide

---

## Reference: All Valid Elements

| Element | Purpose | Example |
|---------|---------|---------|
| `text{}` | Display text | `text{text='Hello'}` |
| `label{}` | Form label | `label{text='Name:'}` |
| `input{}` | Text input | `input{id='name';maxchars='20'}` |
| `dropdown{}` | Select menu | `dropdown{id='opt';options='A,B,C'}` |
| `checkbox{}` | Checkbox | `checkbox{id='agree';selected='false';text='Agree'}` |
| `radio{}` | Radio button | `radio{group='g';id='opt1';text='Option 1'}` |
| `button{}` | Action button | `button{text='Click';id='btn'}` |
| `harray{}` | Horizontal layout | `harray{label{...}input{...}}` |
| `varray{}` | Vertical layout | `varray{text{...}text{...}}` |
| `table{}` | Grid layout | `table{rows='2';cols='2';...}` |
| `border{}` | Border container | `border{size='20,20';...}` |
| `center{}` | Center content | `center{text{...}}` |
| `scroll{}` | Scroll area | `scroll{vertical='true';varray{...}}` |
| `header{}` | Large header | `header{text='Title'}` |
| `passthrough{}` | Hidden data | `passthrough{id='id';text='value'}` |

---

## ModQuestions API Quick Reference

```java
// Headers
ModQuestions.getBmlHeader(question)
ModQuestions.getBmlHeaderWithScroll(question)
ModQuestions.getBmlHeaderNoQuestion(question)
ModQuestions.getBmlHeaderWithScrollAndQuestion(question)
ModQuestions.getBmlHeaderScrollOnly(question)

// Buttons
ModQuestions.createAnswerButton2(question)
ModQuestions.createAnswerButton2(question, "Custom Text")
ModQuestions.createOkAnswerButton(question)
ModQuestions.createAnswerButton3(question)
ModQuestions.createAnswerButtonForNoBorder(question)

// Utility
ModQuestions.createQuestion(performer, title, question, wurmId, modQuestion)
```

---

## Credits

This guide was created by analyzing vanilla Wurm Unlimited Question implementations:
- `AlertServerMessageQuestion.java` - Basic input forms
- `SkillProgressQuestion.java` - Data display with tables
- `CreatureCreationQuestion.java` - Dropdowns, radios, complex forms
- `AchievementCreation.java` - Checkboxes, dynamic content
- `CookBookQuestion.java` - Advanced layouts, scrolling, borders

**Verified working:** PowerScaling mod (WurmModLoader)

---

## License

This documentation is provided as-is for Wurm Unlimited modding community use.

**Last Updated:** November 2024
**Wurm Version:** Build 4596061
**WurmModLoader:** Phase 4+

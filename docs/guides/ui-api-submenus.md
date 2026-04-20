# UI API - Submenu Organization

## Overview

All mod buttons registered through the WurmModLoader UI API are **automatically grouped** under a **"Mods"** submenu in the body context menu. This keeps the UI clean and organized.

---

## Menu Structure

### What Players See

When you right-click your body, you'll see:

```
Body Menu:
  Examine
  Eat
  Drink
  ...vanilla options...

  Mods >                    ← All mod buttons grouped here
    Example                 ← From TemplateMod
    Power Fantasy           ← From PowerScaling
    Soulbound Gear Status   ← From SoulboundGear
    Admin Panel             ← From other mods (if visible)
    ...
```

### Before vs After

**Before (without submenu):**
```
Body Menu:
  Examine
  Eat
  Drink
  Example               ← Clutter!
  Power Fantasy         ← Mixed with vanilla
  Soulbound Gear Status ← Hard to find
  Admin Panel           ← Messy
```

**After (with submenu):**
```
Body Menu:
  Examine
  Eat
  Drink
  Mods >                ← Clean separation
    Example
    Power Fantasy
    Soulbound Gear Status
    Admin Panel
```

---

## Benefits

✅ **Organized** - All mod features in one place
✅ **Clean** - Vanilla menu stays uncluttered
✅ **Discoverable** - Players know where to find mod features
✅ **Scalable** - Works with any number of mods
✅ **Automatic** - No extra code needed

---

## Implementation

The submenu organization is handled automatically by `ContextMenuRegistry`:

```java
@SubscribeEvent
public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
    // Collect all visible mod entries
    List<ActionEntry> submenuItems = new ArrayList<>();
    for (MenuEntry entry : bodyEntries) {
        if (entry.isVisibleFor(event.getPerformer())) {
            submenuItems.add(ModActions.getAction(entry.getActionId()));
        }
    }

    // Add them all under "Mods" submenu
    if (!submenuItems.isEmpty()) {
        event.addSubmenu("Mods", submenuItems);
    }
}
```

---

## For Mod Developers

### No Code Changes Required

When you register a menu entry, it automatically appears under the "Mods" submenu:

```java
MenuEntry entry = MenuEntry.builder("My Feature")
    .onClick(player -> doSomething(player))
    .build();

ContextMenuRegistry.getInstance()
    .register("MyMod", MenuTarget.BODY, entry);

// Automatically appears under: Mods > My Feature
```

### Visibility Filters Still Work

Visibility filters apply to individual entries within the submenu:

```java
MenuEntry adminEntry = MenuEntry.builder("Admin Panel")
    .onlyFor(player -> isAdmin(player))
    .onClick(player -> showAdminPanel(player))
    .build();

// Only visible in "Mods" submenu if player is admin
```

### Empty Submenu Handling

If no mod entries are visible (due to visibility filters), the "Mods" submenu won't appear at all.

---

## Technical Details

### Wurm Submenu Pattern

Wurm uses **negative action IDs** to indicate submenus:

```java
// Create submenu header with negative size
ActionEntry submenuHeader = new ActionEntry(
    (short)(-items.size()),  // Negative = submenu, magnitude = item count
    "Mods",
    "mods"
);

menuEntries.add(submenuHeader);      // Add header first
menuEntries.addAll(submenuItems);    // Add submenu items after
```

### Event Flow

1. `BodyMenuPopulateEvent` fires when player right-clicks body
2. `ContextMenuRegistry.onBodyMenuPopulate()` handles the event
3. All visible mod entries are collected
4. `event.addSubmenu("Mods", submenuItems)` creates the submenu
5. Player sees organized menu

---

## Future Enhancements

Potential future features:

- **Configurable submenu name** - Allow servers to rename "Mods" to something else
- **Category submenus** - Group mods by category (Combat, Crafting, Admin, etc.)
- **Mod-specific submenus** - Allow large mods to create their own submenus
- **Submenu icons** - Add visual indicators

---

## Migration Notes

### For Existing Mods

If you have mods using the old pattern (manual `BodyMenuPopulateEvent` subscription):

**Old:**
```java
@SubscribeEvent
public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
    event.addMenuItem(ModActions.getAction(MY_ACTION_ID));
}
```

**New (UI API):**
```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    MenuEntry entry = MenuEntry.builder("My Feature")
        .onClick(player -> handleClick(player))
        .build();

    ContextMenuRegistry.getInstance()
        .register("MyMod", MenuTarget.BODY, entry);
}
```

The new version automatically gets the submenu organization!

---

## See Also

- **UI_API_README.md** - Complete UI API overview
- **UI_API_GUIDE.md** - Detailed usage guide
- **examples/templatemod/** - Working example using the submenu system

---

*This feature ensures a clean, organized player experience across all WurmModLoader mods!*

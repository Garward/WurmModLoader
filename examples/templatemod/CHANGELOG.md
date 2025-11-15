# Template Mod Changelog

## Updated - Using UI API with Submenu Organization

### Changed
- **Replaced examine event with UI API** - Mod now uses the new WurmModLoader UI API
- **Added "Example" button** - Appears under **Mods > Example** in body context menu
- **Submenu organization** - Button is automatically grouped under "Mods" submenu for clean UI
- **Interactive messages** - Clicking the button sends two messages to the player:
  1. Examine text: "You examine yourself and feel a sense of wonder."
  2. Thank you message: "Thank you for clicking the Example Button [PlayerName]!"

### Technical Changes
- Removed `CreatureExamineEvent` subscription
- Added `ServerStartedEvent` subscription for initialization
- Added `MenuEntry` registration using `ContextMenuRegistry`
- Uses new UI API imports:
  - `com.garward.wurmmodloader.api.ui.MenuEntry`
  - `com.garward.wurmmodloader.api.ui.MenuTarget`
  - `com.garward.wurmmodloader.core.ui.ContextMenuRegistry`

### Benefits
- Demonstrates the new UI API pattern
- Shows best practices for context menu integration
- More interactive than passive examine text
- Easier to understand for new mod developers

## Previous Version

The previous version added examine text to player bodies using `CreatureExamineEvent`.

# Oversized Club Mod

**Example mod demonstrating WurmModLoader's modern event system**

## Overview

This mod adds the "Oversized Club" - a massive two-handed weapon that deals devastating damage but is extremely slow to swing. It's perfect for strong, tanky characters who can afford to wait between swings for maximum impact.

## Weapon Stats

| Property | Value | Comparison |
|----------|-------|------------|
| **Damage** | 40 | Highest of any club (Huge Club: ~28) |
| **Speed** | 80 | Much slower than Huge Club (60) |
| **Weight** | 12kg | Very heavy |
| **Skill** | Huge Club | Part of club fighting tree |
| **Hands** | Two-handed | Cannot use shield |
| **Difficulty** | 40 | Medium-hard to craft |

## Features

- 🔨 **Devastating Damage:** Highest damage per hit of any club weapon
- ⏱️ **Extremely Slow:** Swing speed is intentionally very slow for balance
- 💪 **Strength Required:** Best for high-strength warriors
- 🎯 **Huge Club Skill:** Uses existing skill progression
- 🎨 **Dyeable:** Can be customized with dye
- 🛠️ **Repairable:** Won't be lost forever when damaged

## Crafting

1. **Requirements:**
   - 40+ Carpentry skill
   - Wooden shaft (x2)

2. **Process:**
   - Combine two wooden shafts
   - Uses carpentry skill

3. **Tips:**
   - Heavy item - plan your inventory space
   - High quality = better damage
   - Can be improved with carpentry

## Combat Strategy

### Best For:
- Tank builds with high strength
- PvE against tough single targets
- Crushing through armor
- Boss fights where you can afford slow swings

### Not Ideal For:
- Fast-paced PvP
- Fighting multiple enemies
- Low-strength characters
- Situations requiring quick reactions

## Code Example: Modern Event System

This mod demonstrates WurmModLoader's new event-driven architecture:

### Old Way (Still Works):
```java
public class OversizedClubMod implements WurmServerMod, ItemTemplatesCreatedListener {
    @Override
    public void onItemTemplatesCreated() {
        // Create item...
    }
}
```

### New Way (Cleaner!):
```java
public class OversizedClubMod implements WurmServerMod {
    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        // Create item...
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Log startup info...
    }
}
```

**Benefits:**
- ✅ No need to implement multiple interfaces
- ✅ Cleaner code organization
- ✅ Better IDE support
- ✅ More flexible (priority, cancellation, etc.)
- ✅ Type-safe event handling

## Installation

### For Server Admins:

1. **Download:**
   - Download `oversizedclub-1.0.0.zip` from releases

2. **Extract:**
   ```bash
   cd /path/to/wurm/server
   unzip oversizedclub-1.0.0.zip
   ```

3. **Verify Structure:**
   ```
   mods/
   ├── oversizedclub.properties
   └── oversizedclub/
       └── oversizedclub.jar
   ```

4. **Restart Server:**
   ```bash
   ./WurmServerLauncher-patched start=Adventure
   ```

5. **Verify:**
   - Check logs for "Oversized Club Mod - Server Started"
   - In-game: Craft with two wooden shafts

### For Mod Developers:

Study this mod to learn:
- How to use `@SubscribeEvent` annotations
- How to create custom items with `ItemTemplateBuilder`
- How to handle multiple events in one mod
- Best practices for logging and documentation

## Technical Details

### Dependencies:
- WurmModLoader 1.0.0+ (with Phase 6 Event System)
- Java 8+ (compiled for Java 8 bytecode)

### Events Used:
- `ItemTemplatesCreatedEvent` - Register custom item
- `ServerStartedEvent` - Log startup information

### Item Template ID:
- `garward.oversizedclub` - Namespaced ID for uniqueness

### Combat Mechanics:
- **Damage Type:** Crush (effective against armor)
- **Combat Moves:** 2 (limited attack variety)
- **Body Spaces:** 18 (both hands - can't use shield)
- **Weapon Type:** 1 (bludgeoning weapon)

## Balance Considerations

The Oversized Club is intentionally designed to be a **high-risk, high-reward** weapon:

**Advantages:**
- Highest damage per hit in club category
- Crushes through armor effectively
- Intimidating in PvE

**Disadvantages:**
- Very slow swing speed (combat disadvantage)
- Heavy weight (limits inventory)
- Two-handed (no shield defense)
- Limited attack variety

**Recommended For:**
- PvE tank builds
- Boss encounters
- High-strength characters
- Players who prefer "slow but deadly"

## Future Enhancements

Possible improvements for community versions:
- Special attack moves (stun, knockback)
- Custom sounds/particles
- Crafting requirements (unique materials)
- Level requirements
- Special effects on hit

## License

MIT License - See main WurmModLoader repository

## Credits

- **Created by:** WurmModLoader Team
- **Purpose:** Demonstration of Phase 6 Event System
- **Based on:** Huge Club mechanics from Wurm Unlimited

## Support

- **Issues:** Report on WurmModLoader GitHub
- **Questions:** WurmModLoader Discussions
- **Mod Development:** See WurmModLoader documentation

---

**This mod is an example of how easy it is to create custom content with WurmModLoader's modern event system!** 🎉

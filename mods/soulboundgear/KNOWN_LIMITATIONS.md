# SoulboundGear - Known Limitations

## XP System Not Yet Implemented

**Issue:** XP gain on creature kills is not currently functional.

**Reason:** The XP award system requires hooking `Creature.die()` with Javassist bytecode injection. However, Javassist cannot reference mod classes (like `SoulboundGearManager`) during the `preInit()` phase when bytecode is being compiled. This is a technical limitation of the modloader's class loading order.

**Current Status:**
- ✅ Item binding works (bind items to your character)
- ✅ Soulbound status tracking works
- ✅ Database persistence works
- ❌ XP gain on kills **NOT IMPLEMENTED**
- ❌ Level progression **NOT IMPLEMENTED**
- ❌ Level bonuses **NOT IMPLEMENTED**

**Workaround Options:**

### Option 1: Manual XP Award (Admin/Testing)
For testing purposes, XP can be manually awarded via direct database manipulation:
```sql
UPDATE soulbound_items SET current_xp = current_xp + 1000 WHERE item_wurm_id = <item_id>;
```

### Option 2: Wait for Phase 3 Implementation
A future update will implement XP gain using one of these approaches:
- Post-server-start event hooking (after all classes are loaded)
- Combat action listeners (XP per attack instead of per kill)
- Server tick-based event system
- Integration with PowerScaling mod's kill tracking

### Option 3: PowerScaling Integration (Recommended Path Forward)
The PowerScaling mod already successfully hooks creature deaths (it loads after SoulboundGear). A future integration could:
1. PowerScaling tracks kills and power levels
2. SoulboundGear reads kill data from PowerScaling
3. Award XP based on PowerScaling's kill tracking
4. Both mods benefit from shared infrastructure

## What Currently Works

**Phase 1 Features (Fully Functional):**
- Bind items to your character via `/bind_soul` action
- Check soulbound status via `/check_soulbound` action
- Soulbound items cannot be dropped/traded
- Database persistence (items stay bound after server restart)
- Material integration (elemental damage from MaterialSystem)

**Phase 2 Features (Partially Functional):**
- ✅ All configuration parameters loaded
- ✅ XP calculation formulas implemented
- ✅ Level-up bonus calculations ready
- ❌ XP gain trigger not hooked
- ❌ Level progression messages not shown

## Future Roadmap

**Phase 2 Completion (XP System):**
- Implement XP gain via alternative hook mechanism
- Test and balance XP rates
- Add level-up visual/audio feedback

**Phase 3 (Upgrade Tree):**
- Point spending system
- Skill tree visualization
- Passive ability unlocks

**Phase 4 (Advanced Features):**
- Item socketing for materials
- Stat reallocation
- Prestige system

## Testing XP System (For Development)

To test XP functionality before the hook is implemented:

1. Bind an item using `/bind_soul`
2. Use SQL to manually award XP:
   ```sql
   sqlite3 mods/soulboundgear.db
   UPDATE soulbound_items SET current_xp = 5000 WHERE item_wurm_id = <id>;
   SELECT * FROM soulbound_items;
   ```
3. Check if level-up occurs (formula: level = floor(log(xp / baseXP) / log(1.5)))
4. Use `/check_soulbound` to see updated stats

## Why Not Just Remove XP Code?

The XP system code remains in place because:
1. Configuration is already complete and tested
2. Database schema supports it
3. Formulas and calculations are ready
4. Only the trigger/hook needs implementation
5. Easier to add the hook later than rebuild the entire system

The missing piece is < 50 lines of hook code, which will be added once we solve the class loading issue.

---

**Bottom Line:** SoulboundGear Phase 1 (binding) works perfectly. Phase 2 (XP/leveling) is code-complete but not triggered until we implement an alternative hook mechanism.

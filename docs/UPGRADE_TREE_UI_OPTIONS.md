# Upgrade Tree UI Options for Wurm Unlimited

**Analysis of UI approaches for implementing an upgrade tree mod**

---

## TL;DR Recommendation

**Use BML with a Tier-Based Layout** ⭐

BML is the **only** in-game UI system available in Wurm. While it lacks visual tree graphics, you can create an effective upgrade tree using creative layout patterns. See **Approach #2** below for the best option.

---

## What BML Can Do

✅ **Strengths:**
- Tables for grid layouts (perfect for tier systems)
- Buttons with tooltips for interactivity
- Text formatting (bold, colors via type attribute)
- Radio buttons for exclusive selections
- Checkboxes for multiple selections
- Dropdowns for filtering/categories
- Scrolling for large content
- Dynamic content (rebuild window on selection)

❌ **Limitations:**
- No custom graphics/images
- No drawing primitives (lines, arrows, connections)
- No dynamic repositioning (static layout)
- No nested interactivity
- No visual "tree" lines connecting nodes
- Text-only display

---

## Alternative UI Systems in Wurm?

**None exist for in-game UI.**

Wurm only has:
1. **BML (Question system)** - What we're using
2. **Chat messages** - Text only, no interactivity
3. **Examine text** - Read-only info display
4. **Web interface** - External browser, not in-game

**Conclusion:** BML is your only option for interactive in-game UI.

---

## Upgrade Tree Design Approaches

### Approach #1: Text-Based Tree (ASCII Art)

**Visual Example:**
```
╔══════════════════════════════════════════════╗
║           UPGRADE TREE                        ║
╠══════════════════════════════════════════════╣
║                                               ║
║  Tier 1                                       ║
║  ┌─────────────┐                              ║
║  │ [✓] Base    │ (Unlocked)                   ║
║  └─────┬───────┘                              ║
║        │                                       ║
║  Tier 2│                                       ║
║  ┌─────┴───────┐  ┌────────────┐              ║
║  │ [ ] Sword   │  │ [ ] Armor  │              ║
║  │   Dmg +10%  │  │   Def +10% │              ║
║  │ Cost: 5 pts │  │ Cost: 5 pts│              ║
║  └─────────────┘  └────────────┘              ║
║                                               ║
╚══════════════════════════════════════════════╝
```

**Implementation:**
```java
buf.append("text{type='bold';text='UPGRADE TREE'}");
buf.append("text{text=''}");
buf.append("text{type='bold';text='Tier 1'}");
buf.append("text{text='  [✓] Base Upgrade (Unlocked)'}");
buf.append("text{text=''}");
buf.append("text{type='bold';text='Tier 2 (Requires: Base)'}");
buf.append("text{text='  [ ] Sword Mastery - Damage +10% (Cost: 5 points)'}");
buf.append("text{text='  [ ] Armor Training - Defense +10% (Cost: 5 points)'}");
buf.append("text{text=''}");
buf.append("harray{button{text='Unlock Sword';id='unlock_sword'}button{text='Unlock Armor';id='unlock_armor'}}");
```

**Pros:**
- Simple to implement
- Clear hierarchy
- Works with pure text

**Cons:**
- Not very visual
- Hard to show complex dependencies
- Looks dated

**Rating:** 3/5 ⭐⭐⭐

---

### Approach #2: Tier-Based Table Layout (RECOMMENDED) ⭐

**Visual Example:**
```
╔════════════════════════════════════════════════════╗
║          UPGRADE TREE - TIER SELECTION             ║
╠════════════════════════════════════════════════════╣
║                                                    ║
║  Current Points: 15                                ║
║  Tier: [Tier 1 ▼] [Tier 2 ▼] [Tier 3 ▼]          ║
║                                                    ║
║  ┌─────────────────┬─────────────────┬──────────┐ ║
║  │ Sword Mastery   │ Armor Training  │ HP Boost │ ║
║  │                 │                 │          │ ║
║  │ Damage +10%     │ Defense +10%    │ HP +50   │ ║
║  │                 │                 │          │ ║
║  │ Cost: 5         │ Cost: 5         │ Cost: 3  │ ║
║  │ Requires:       │ Requires:       │ Requires:│ ║
║  │  - Base (✓)     │  - Base (✓)     │  - None  │ ║
║  │                 │                 │          │ ║
║  │ Status: ✓       │ Status: Locked  │ Status: ✓│ ║
║  │                 │                 │          │ ║
║  │ [Unlocked]      │ [UNLOCK (5pts)] │[Unlocked]│ ║
║  └─────────────────┴─────────────────┴──────────┘ ║
║                                                    ║
╚════════════════════════════════════════════════════╝
```

**Implementation:**
```java
@Override
public void sendQuestion(Question question) {
    UpgradeManager mgr = UpgradeManager.getInstance();
    int currentPoints = mgr.getAvailablePoints(player.getWurmId());
    int selectedTier = getCurrentTier();  // From previous selection

    StringBuilder buf = new StringBuilder();
    buf.append(ModQuestions.getBmlHeaderWithScroll(question));

    // Header with points
    buf.append("text{type='bold';text='UPGRADE TREE'}");
    buf.append("text{text='Available Points: " + currentPoints + "'}");
    buf.append("text{text=''}");

    // Tier selection dropdown
    buf.append("harray{label{text='View Tier:'}dropdown{id='tier';options='Tier 1,Tier 2,Tier 3,Tier 4,Tier 5'}}");
    buf.append("text{text=''}");

    // Get upgrades for selected tier
    List<Upgrade> upgrades = mgr.getUpgradesForTier(selectedTier);

    // Display upgrades in table (3 columns)
    int cols = 3;
    int rows = (int) Math.ceil(upgrades.size() / (double) cols);

    for (int row = 0; row < rows; row++) {
        buf.append("table{rows='1';cols='" + cols + "';");

        for (int col = 0; col < cols; col++) {
            int idx = row * cols + col;
            if (idx < upgrades.size()) {
                Upgrade upg = upgrades.get(idx);
                buf.append(buildUpgradeCell(upg, currentPoints));
                if (col < cols - 1) buf.append(";");
            } else {
                // Empty cell
                buf.append("text{text=''}");
                if (col < cols - 1) buf.append(";");
            }
        }
        buf.append("}");
    }

    buf.append(ModQuestions.createAnswerButton2(question));
    player.getCommunicator().sendBml(600, 500, true, true, buf.toString(), 200, 200, 200, "Upgrade Tree");
}

private String buildUpgradeCell(Upgrade upg, int availablePoints) {
    StringBuilder cell = new StringBuilder();
    cell.append("varray{");

    // Name (bold)
    cell.append("text{type='bold';text='" + upg.getName() + "'}");

    // Description
    cell.append("text{text='" + upg.getDescription() + "'}");
    cell.append("text{text=''}");

    // Cost
    cell.append("text{text='Cost: " + upg.getCost() + " points'}");

    // Requirements
    if (upg.hasRequirements()) {
        cell.append("text{text='Requires:'}");
        for (String req : upg.getRequirements()) {
            boolean hasMet = playerHasUpgrade(req);
            cell.append("text{text='  - " + req + (hasMet ? " ✓" : " ✗") + "'}");
        }
    }

    cell.append("text{text=''}");

    // Button
    if (upg.isUnlocked()) {
        cell.append("text{type='bold';text='[UNLOCKED]'}");
    } else if (!upg.canAfford(availablePoints)) {
        cell.append("text{text='[Need " + upg.getCost() + " points]'}");
    } else if (!upg.meetsRequirements()) {
        cell.append("text{text='[Locked - missing prereqs]'}");
    } else {
        cell.append("button{text='UNLOCK (" + upg.getCost() + "pts)';id='unlock_" + upg.getId() + "'}");
    }

    cell.append("}");
    return cell.toString();
}

@Override
public void answer(Question question, Properties answers) {
    // Check if tier dropdown changed
    String tierStr = answers.getProperty("tier");
    if (tierStr != null) {
        int newTier = Integer.parseInt(tierStr) + 1;  // 0-indexed
        setCurrentTier(newTier);
        // Reopen window with new tier
        sendQuestion(question);
        return;
    }

    // Check for unlock button clicks
    for (String key : answers.stringPropertyNames()) {
        if (key.startsWith("unlock_")) {
            String upgradeId = key.substring(7);
            UpgradeManager.getInstance().unlockUpgrade(player, upgradeId);
            player.getCommunicator().sendNormalServerMessage("Unlocked: " + upgradeId);
            // Reopen window to show updated state
            sendQuestion(question);
            return;
        }
    }
}
```

**Pros:**
- Clean, organized layout
- Easy to navigate (tier dropdown)
- Shows all info (cost, requirements, status)
- Interactive (click to unlock)
- Scalable (can have many upgrades per tier)
- Professional look

**Cons:**
- No visual connections between tiers
- Can't see full tree at once
- Requires navigation between tiers

**Rating:** 5/5 ⭐⭐⭐⭐⭐ **RECOMMENDED**

---

### Approach #3: List-Based with Checkboxes

**Visual Example:**
```
╔════════════════════════════════════════════════════╗
║          UPGRADE TREE - LIST VIEW                  ║
╠════════════════════════════════════════════════════╣
║                                                    ║
║  Available Points: 15                              ║
║  Filter: [All Tiers ▼]                             ║
║                                                    ║
║  TIER 1                                            ║
║  ☑ Base Upgrade (Unlocked)                         ║
║                                                    ║
║  TIER 2                                            ║
║  ☑ Sword Mastery - Dmg +10% (Cost: 5) [Unlocked]  ║
║  ☐ Armor Training - Def +10% (Cost: 5) [UNLOCK]   ║
║  ☑ HP Boost - HP +50 (Cost: 3) [Unlocked]          ║
║                                                    ║
║  TIER 3                                            ║
║  ☐ Critical Strike - Crit +15% (Cost: 10)          ║
║     Requires: Sword Mastery ✓                      ║
║     [UNLOCK (10 pts)]                              ║
║                                                    ║
╚════════════════════════════════════════════════════╝
```

**Implementation:**
```java
buf.append("text{type='bold';text='TIER 1'}");
buf.append("checkbox{id='base';selected='true';text='Base Upgrade (Unlocked - Cost: 0)'}");
buf.append("text{text=''}");

buf.append("text{type='bold';text='TIER 2'}");
buf.append("checkbox{id='sword';selected='true';text='Sword Mastery - Damage +10% (Unlocked)'}");
buf.append("checkbox{id='armor';selected='false';text='Armor Training - Defense +10% (Cost: 5)'}");
buf.append("button{text='Unlock Selected';id='unlock'}");
```

**Pros:**
- Simple, compact
- Shows all upgrades in one view
- Easy to see progression
- Checkmarks show unlocked status

**Cons:**
- Limited interactivity (must use separate unlock button)
- Hard to show complex dependencies
- Gets cluttered with many upgrades

**Rating:** 3.5/5 ⭐⭐⭐

---

### Approach #4: Category-Based with Radio Selection

**Visual Example:**
```
╔════════════════════════════════════════════════════╗
║          UPGRADE TREE - CATEGORY VIEW              ║
╠════════════════════════════════════════════════════╣
║                                                    ║
║  Category: [⚔ Combat ▼]                            ║
║  Points: 15 available                              ║
║                                                    ║
║  Select upgrade to unlock:                         ║
║                                                    ║
║  ○ Sword Mastery (UNLOCKED)                        ║
║    Damage +10% | Cost: 5 | Tier 2                  ║
║                                                    ║
║  ● Critical Strike                                 ║
║    Critical +15% | Cost: 10 | Tier 3               ║
║    Requires: Sword Mastery ✓                       ║
║                                                    ║
║  ○ Berserker Rage                                  ║
║    Damage +25%, Defense -10% | Cost: 15 | Tier 4   ║
║    Requires: Critical Strike ✗                     ║
║                                                    ║
║  [UNLOCK SELECTED (10 points)]                     ║
║                                                    ║
╚════════════════════════════════════════════════════╝
```

**Pros:**
- Organized by theme (Combat, Defense, Utility, etc.)
- Radio buttons for clear selection
- Good for mutually exclusive paths

**Cons:**
- Can only unlock one at a time
- Less flexible than buttons
- Harder to show tier progression

**Rating:** 4/5 ⭐⭐⭐⭐

---

### Approach #5: Radio Buttons with Pagination (USER'S IDEA) ⭐⭐

**Visual Example:**
```
╔════════════════════════════════════════════════════╗
║          UPGRADE TREE - PAGE 1/3                   ║
╠════════════════════════════════════════════════════╣
║                                                    ║
║  Available Points: 15                              ║
║  Current Tier: 2                                   ║
║                                                    ║
║  Select an upgrade to unlock:                      ║
║                                                    ║
║  ○ Sword Mastery                                   ║
║    Damage +10% | Cost: 5 points                    ║
║    Requires: Base (✓)                              ║
║                                                    ║
║  ○ Armor Training                                  ║
║    Defense +10% | Cost: 5 points                   ║
║    Requires: Base (✓)                              ║
║                                                    ║
║  ○ HP Boost                                        ║
║    HP +50 | Cost: 3 points                         ║
║    Requires: Base (✓)                              ║
║                                                    ║
║  ○ Stamina Regen                                   ║
║    Stamina +20% | Cost: 4 points                   ║
║    Requires: HP Boost (✗ - locked)                 ║
║                                                    ║
║  [UNLOCK SELECTED]  [NEXT PAGE]  [PREVIOUS PAGE]   ║
║                                                    ║
╚════════════════════════════════════════════════════╝
```

**Implementation:**
```java
public class UpgradeTreeQuestion implements ModQuestion {
    private final Creature player;
    private int currentPage = 0;
    private static final int UPGRADES_PER_PAGE = 5;

    @Override
    public void sendQuestion(Question question) {
        UpgradeManager mgr = UpgradeManager.getInstance();
        List<Upgrade> allUpgrades = mgr.getAllUpgrades();
        int totalPages = (int) Math.ceil(allUpgrades.size() / (double) UPGRADES_PER_PAGE);

        // Get upgrades for current page
        int startIdx = currentPage * UPGRADES_PER_PAGE;
        int endIdx = Math.min(startIdx + UPGRADES_PER_PAGE, allUpgrades.size());
        List<Upgrade> pageUpgrades = allUpgrades.subList(startIdx, endIdx);

        int availablePoints = mgr.getAvailablePoints(player.getWurmId());

        StringBuilder buf = new StringBuilder();
        buf.append(ModQuestions.getBmlHeader(question));

        // Header
        buf.append("text{type='bold';text='UPGRADE TREE - PAGE " + (currentPage + 1) + "/" + totalPages + "'}");
        buf.append("text{text='Available Points: " + availablePoints + "'}");
        buf.append("text{text=''}");

        // Upgrade list as radio buttons
        buf.append("text{text='Select an upgrade to unlock:'}");
        buf.append("text{text=''}");

        for (Upgrade upg : pageUpgrades) {
            boolean canAfford = upg.getCost() <= availablePoints;
            boolean meetsReqs = mgr.meetsRequirements(player, upg);
            boolean isUnlocked = mgr.hasUpgrade(player, upg.getId());

            String status = "";
            if (isUnlocked) {
                status = " (UNLOCKED ✓)";
            } else if (!canAfford) {
                status = " (Need " + upg.getCost() + " points)";
            } else if (!meetsReqs) {
                status = " (Missing prerequisites)";
            }

            // Radio button for this upgrade
            buf.append("radio{group='upgrade';id='" + upg.getId() + "';text='" + upg.getName() + status + "'}");

            // Details
            buf.append("text{text='  " + upg.getDescription() + " | Cost: " + upg.getCost() + " points'}");

            // Requirements
            if (!upg.getRequirements().isEmpty()) {
                StringBuilder reqs = new StringBuilder("  Requires: ");
                for (String reqId : upg.getRequirements()) {
                    boolean hasReq = mgr.hasUpgrade(player, reqId);
                    Upgrade reqUpg = mgr.getUpgrade(reqId);
                    reqs.append(reqUpg.getName());
                    reqs.append(hasReq ? " ✓" : " ✗");
                    reqs.append(", ");
                }
                // Remove trailing comma
                String reqStr = reqs.toString();
                if (reqStr.endsWith(", ")) {
                    reqStr = reqStr.substring(0, reqStr.length() - 2);
                }
                buf.append("text{text='" + reqStr + "'}");
            }

            buf.append("text{text=''}");  // spacing
        }

        // Navigation buttons
        buf.append("harray{");
        buf.append("button{text='UNLOCK SELECTED';id='unlock'}");

        if (currentPage < totalPages - 1) {
            buf.append("button{text='NEXT PAGE';id='nextpage'}");
        }

        if (currentPage > 0) {
            buf.append("button{text='PREVIOUS PAGE';id='prevpage'}");
        }

        buf.append("}");

        buf.append(ModQuestions.createAnswerButton2(question, "Close"));

        player.getCommunicator().sendBml(450, 500, true, true, buf.toString(), 200, 200, 200, "Upgrade Tree");
    }

    @Override
    public void answer(Question question, Properties answers) {
        // Check for navigation
        if (answers.getProperty("nextpage") != null) {
            currentPage++;
            sendQuestion(question);  // Reopen on next page
            return;
        }

        if (answers.getProperty("prevpage") != null) {
            currentPage--;
            sendQuestion(question);  // Reopen on previous page
            return;
        }

        // Check for unlock action
        if (answers.getProperty("unlock") != null) {
            String selectedId = answers.getProperty("upgrade");
            if (selectedId != null) {
                UpgradeManager mgr = UpgradeManager.getInstance();
                Upgrade upg = mgr.getUpgrade(selectedId);

                if (mgr.canUnlock(player, upg)) {
                    mgr.unlockUpgrade(player, selectedId);
                    player.getCommunicator().sendNormalServerMessage("Unlocked: " + upg.getName() + "!");
                    player.getCommunicator().sendNormalServerMessage("Applied: " + upg.getDescription());

                    // Reopen window to show updated state
                    sendQuestion(question);
                } else {
                    player.getCommunicator().sendAlertServerMessage("Cannot unlock " + upg.getName() + " - check requirements!");
                }
            } else {
                player.getCommunicator().sendAlertServerMessage("Please select an upgrade first!");
            }
        }
    }
}
```

**Pros:**
- ⭐ **Simple to implement** - Just radio buttons and navigation
- ⭐ **Clear selection** - Radio buttons make it obvious what's selected
- ⭐ **Paginated** - Handles any number of upgrades
- ⭐ **Good UX** - Easy to understand, navigate
- ⭐ **Proven pattern** - Used in many vanilla Wurm UIs

**Cons:**
- Can only unlock one upgrade at a time
- Must navigate pages to see all options
- Can't see full tree structure at once

**Rating:** 4.5/5 ⭐⭐⭐⭐ **EXCELLENT FOR SIMPLICITY**

**User's Original Idea:** This matches what you described perfectly! Radio buttons for upgrades, with "Next Page" and "Previous Page" buttons for navigation.

---

## Comparison Matrix

| Approach | Ease of Use | Visual Appeal | Scalability | Dependency Display | Implementation Time | Recommendation |
|----------|-------------|---------------|-------------|-------------------|-------------------|----------------|
| Text-Based ASCII | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ | 1 day | Basic |
| **Tier-Based Table** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 2-3 days | **BEST** ⭐ |
| List with Checkboxes | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | 1-2 days | Good |
| Category Radio | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 2 days | Good |
| **Radio + Pagination** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 1 day | **EASIEST** ⭐ |

---

## Implementation Strategy

### Step 1: Backend Data Structure

```java
public class Upgrade {
    private final String id;
    private final String name;
    private final String description;
    private final int tier;
    private final int cost;
    private final List<String> requiredUpgrades;
    private final UpgradeEffect effect;

    public boolean canUnlock(Player player) {
        // Check points
        if (player.getUpgradePoints() < cost) return false;

        // Check prerequisites
        for (String reqId : requiredUpgrades) {
            if (!player.hasUpgrade(reqId)) return false;
        }

        return true;
    }
}

public class UpgradeManager {
    private Map<String, Upgrade> allUpgrades = new HashMap<>();

    public void registerUpgrade(Upgrade upgrade) {
        allUpgrades.put(upgrade.getId(), upgrade);
    }

    public List<Upgrade> getUpgradesForTier(int tier) {
        return allUpgrades.values().stream()
            .filter(u -> u.getTier() == tier)
            .collect(Collectors.toList());
    }

    public void unlockUpgrade(Player player, String upgradeId) {
        Upgrade upgrade = allUpgrades.get(upgradeId);
        if (upgrade.canUnlock(player)) {
            player.spendUpgradePoints(upgrade.getCost());
            player.addUpgrade(upgradeId);
            upgrade.getEffect().apply(player);
        }
    }
}
```

### Step 2: UI Implementation (Tier-Based)

See **Approach #2** code example above.

### Step 3: Persistence

Store unlocked upgrades in database:
```sql
CREATE TABLE player_upgrades (
    player_id INTEGER NOT NULL,
    upgrade_id TEXT NOT NULL,
    unlocked_timestamp INTEGER,
    PRIMARY KEY (player_id, upgrade_id)
);

CREATE TABLE player_upgrade_points (
    player_id INTEGER PRIMARY KEY,
    total_points INTEGER DEFAULT 0,
    spent_points INTEGER DEFAULT 0
);
```

---

## Enhancements

### Dynamic Window Refresh
When player unlocks an upgrade, rebuild and reopen window to show updated state:

```java
@Override
public void answer(Question question, Properties answers) {
    String action = answers.getProperty("unlock_sword");
    if (action != null) {
        unlockUpgrade(player, "sword_mastery");
        // Reopen window with updated state
        PowerScalingStatsQuestion newQ = new PowerScalingStatsQuestion(player);
        Question q = ModQuestions.createQuestion(player, "Upgrade Tree", "", player.getWurmId(), newQ);
        q.sendQuestion();
    }
}
```

### Color Coding (if BML supports)
- ✅ Green text for unlocked
- ❌ Red text for locked
- ⚠️ Yellow for available

```java
// NOTE: BML might not support color. Test this!
buf.append("text{color='0,255,0';text='✓ Unlocked'}");
buf.append("text{color='255,0,0';text='✗ Locked'}");
```

### Tooltips on Buttons
```java
buf.append("button{text='Unlock';id='unlock_sword';hover='Unlock Sword Mastery for 5 points'}");
```

---

## Example: Complete Tier System

```java
// Tier 1 (Base)
registerUpgrade(new Upgrade("base", "Foundation", "Base power", 1, 0, Collections.emptyList()));

// Tier 2 (Branches)
registerUpgrade(new Upgrade("sword", "Sword Mastery", "Damage +10%", 2, 5, Arrays.asList("base")));
registerUpgrade(new Upgrade("armor", "Armor Training", "Defense +10%", 2, 5, Arrays.asList("base")));
registerUpgrade(new Upgrade("hp", "Health Boost", "HP +50", 2, 3, Arrays.asList("base")));

// Tier 3 (Specializations)
registerUpgrade(new Upgrade("crit", "Critical Strike", "Crit +15%", 3, 10, Arrays.asList("sword")));
registerUpgrade(new Upgrade("block", "Shield Wall", "Block +20%", 3, 10, Arrays.asList("armor")));
registerUpgrade(new Upgrade("regen", "Regeneration", "HP regen +5/min", 3, 8, Arrays.asList("hp")));

// Tier 4 (Advanced)
registerUpgrade(new Upgrade("berserk", "Berserker", "Dmg +25%, Def -10%", 4, 15, Arrays.asList("crit", "sword")));
registerUpgrade(new Upgrade("fortress", "Fortress", "Def +30%", 4, 15, Arrays.asList("block", "armor")));

// Tier 5 (Ultimate)
registerUpgrade(new Upgrade("godmode", "Ascension", "All stats +50%", 5, 50, Arrays.asList("berserk", "fortress", "regen")));
```

---

## Conclusion

**Two Recommended Approaches:**

### Best for Complex Trees: Tier-Based Table (Approach #2)
Perfect if you have many branches and want to show multiple upgrades at once.

**Pros:** Professional, organized, shows full tier
**Time:** 2-3 days

### Best for Simplicity: Radio + Pagination (Approach #5) ⭐
Perfect if you want the simplest possible implementation - **exactly what you described!**

**Pros:**
- Easiest to implement (1 day)
- Radio buttons + navigation buttons = done
- Proven vanilla Wurm pattern
- Scales to any number of upgrades

**Implementation:**
```java
// Pseudo-code
for each upgrade on current page:
    - Add radio button with name and status
    - Show description and requirements
    - Show cost

Add buttons:
    - "Unlock Selected"
    - "Next Page" (if not last page)
    - "Previous Page" (if not first page)
```

**Estimated Implementation Time:** 1 day
- Morning: Backend (Upgrade class, Manager, persistence)
- Afternoon: UI (Radio buttons + pagination)
- Evening: Testing and polish

**Recommendation:** Start with **Radio + Pagination** (Approach #5). It's simple, works perfectly for upgrade trees, and you can always enhance it later if needed.

---

## References

- **BML_UI_GUIDE.md** - Complete BML syntax reference
- **Vanilla Examples:**
  - `ChangeMedPathQuestion.java` - Path selection with radio buttons
  - `SelectSpellQuestion.java` - Deity bonuses by level
  - `CreatureCreationQuestion.java` - Complex table layouts

---

**Last Updated:** November 2024
**Author:** WurmModLoader Team

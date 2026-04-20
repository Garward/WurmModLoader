# Custom Body Menu Actions - Investigation & Progress

## 🚨 CRITICAL DISCOVERY (Nov 12, 2025 4:49 PM)

**ModActions.registerAction() IS WORKING! Array is properly expanded, but server STILL crashes!**

### Diagnostic Results

```
[PowerScaling] Registered action ID: 949
[PowerScaling] Actions.actionEntrys.length AFTER registration: 950
[PowerScaling] Is action ID within bounds? true
[PowerScaling] ActionEntry at index 949: Power Fantasy
```

**What This Means:**
- ✅ ModActions.registerAction() successfully expands Actions.actionEntrys from ~500 to 950 elements
- ✅ Action 949 is within bounds (949 < 950)
- ✅ ActionEntry is properly stored at index 949
- ✅ The array access in Action constructor lines 242-243 SHOULD work
- ❌ **Server STILL crashes when button is clicked**

**Conclusion:** The crash is NOT due to array bounds! The array IS big enough. The crash must be happening:
1. Somewhere else that accesses Actions.actionEntrys with a different index
2. In a different array entirely (Actions.actionStrings? Actions.verbStrings?)
3. In code that runs BEFORE the Action constructor
4. Due to reflection/classloader issues with the expanded array

**Next Steps:**
1. Check if there are OTHER arrays in Actions.java that also need expansion
2. Examine BehaviourDispatcher.action() for early validation that might fail
3. Look for Actions.getVerbForAction(949) or similar calls that happen before constructor
4. Check if the reflection-based array expansion is actually persisting

---

## 🎯 PREVIOUS THEORY - ROOT CAUSE IDENTIFIED (Nov 12, 2025 8:12 PM)

**The server crashes in the Action CONSTRUCTOR, not in poll()!**

**NOTE:** This theory was partially correct - the constructor IS involved - but the array bounds issue was a red herring. The array IS properly expanded.

### The Actual Crash Location

**File**: `com/wurmonline/server/behaviours/Action.java` (lines 242-243)
**Decompiled**: `<decompiled-wurm-source>/server_decompiled/com/wurmonline/server/behaviours/Action.java`

```java
// Action constructor - CRASHES HERE!
boolean bl = isEmote = this.action >= 2000 && this.action < 8000;
if (!isEmote) {
    this.isSpell = Actions.actionEntrys[this.getNumber()].isSpell();      // 💥 CRASH!
    this.isOffensive = Actions.actionEntrys[this.getNumber()].isOffensive(); // Index 949 out of bounds
    // ...
}
```

### Complete Action Execution Flow

```
1. Client clicks "Power Fantasy" button (action ID 949)
   ↓
2. Client sends action packet to server (command 97, opcode 0x61)
   ↓
3. Server: BehaviourDispatcher.action() receives packet
   File: BehaviourDispatcher.java:504 or 549
   ↓
4. BehaviourDispatcher creates Action object:
   new Action(creature, subject, target, 949, x, y, z, rotation)
   ↓
5. Action CONSTRUCTOR runs (Action.java:239-265)
   Line 242: this.isSpell = Actions.actionEntrys[949].isSpell()
   ↓
6. 💥 ArrayIndexOutOfBoundsException: 949 >= 500
   ↓
7. Server crashes - NEVER reaches Action.poll() or BodyPartBehaviour.action()
```

### Why All Our Patches Failed

1. **ActionArrayBoundsCheckPatch** → Patched `Action.poll()` but crash happens in **constructor** (before poll)
2. **BodyPartModActionsPatch** → Patched `BodyPartBehaviour.action()` but crash happens **before** this is reached

The Action object creation happens in `BehaviourDispatcher.java`, and the constructor immediately tries to access `Actions.actionEntrys[949]` to check if it's a spell/offensive action.

### Client-Side is NOT the Problem

**Investigation Result**: The client can handle custom action IDs just fine!

**Evidence from decompiled client** (`SimpleServerConnectionClass.java:1940-1952`):
```java
private void reallyHandleCmdAvailableActions(ByteBuffer bb) {
    for (int i = 0; i < actionCount; ++i) {
        short id = bb.getShort();              // Read ANY action ID
        String descr = this.readStringByteLength(bb);
        boolean instant = bb.get() != 0;
        actionList.add(new PlayerAction(id, 65535, descr, instant));  // Create PlayerAction dynamically
    }
}
```

The client creates `PlayerAction` objects **dynamically from whatever IDs the server sends**. There's no hardcoded whitelist. The `PlayerAction.actionIds` static map is just for vanilla constants, not validation.

### The Real Problem: Server-Side Array Access

**Actions.actionEntrys** is a fixed-size array (~500 elements) defined in `Actions.java:991`:
```java
public static ActionEntry[] actionEntrys = new ActionEntry[<some fixed size>];
```

**ModActions DOES try to expand it** (`ModActions.java`):
```java
public static void registerAction(ActionEntry actionEntry) {
    short number = actionEntry.getNumber();
    ActionEntry[] newArray = Arrays.copyOf(Actions.actionEntrys, number + 1);
    newArray[number] = actionEntry;
    ReflectionUtil.setPrivateField(Actions.class, "actionEntrys", newArray);
}
```

**But the Action constructor accesses the array BEFORE BodyPartBehaviour checks anything!**

### What We Know Now

✅ **Menu Population Works**: BodyMenuPopulatePatch fires BodyMenuPopulateEvent
✅ **Menu Appearance Works**: Custom "Power Fantasy" button appears
✅ **Action Registration Works**: ModActions registers action ID 949
✅ **Client Accepts Custom IDs**: Client can handle any action ID server sends
❌ **Server Constructor Crashes**: Action constructor tries to access Actions.actionEntrys[949]
❌ **Array Expansion Timing**: Either reflection fails or array is accessed before expansion

### The Solution

We need to patch the **Action constructor** to skip the array access for ModActions:

```java
// In Action constructor, BEFORE line 242:
if (this.action < 900) {  // Only access array for vanilla actions
    this.isSpell = Actions.actionEntrys[this.getNumber()].isSpell();
    this.isOffensive = Actions.actionEntrys[this.getNumber()].isOffensive();
} else {
    // ModActions - set safe defaults
    this.isSpell = false;
    this.isOffensive = false;
}
```

### Next Steps

1. Create `ActionConstructorPatch` to wrap the array access in bounds check
2. Remove `ActionArrayBoundsCheckPatch` (wrong location)
3. Keep `BodyPartModActionsPatch` for delegating to ActionPerformer (will work once constructor doesn't crash)
4. Test complete flow

---

## Project Structure

### Key Directories
- **Source Code**: `<repo-root>/`
- **Decompiled Wurm Server**: `<decompiled-wurm-source>/server_decompiled/`
- **Server Directory**: `<wurm-server-dir>/` (Windows default: `C:\Program Files (x86)\Steam\steamapps\common\Wurm Unlimited Dedicated Server\`)

### Key Files

**Action Execution Chain**:
- `BehaviourDispatcher.java:504, 549` - Creates Action object
- `Action.java:239-265` - Constructor (💥 crashes here)
- `Action.java:~2359` - poll() method (never reaches this)
- `BodyPartBehaviour.java:173, 268` - action() methods (never reaches this)

**ModActions**:
- `ModActions.java` - registerAction() expands Actions.actionEntrys array
- `ActionPerformerBase.java` - Base class for action handlers

**Patches** (need to update):
- ✅ `BodyMenuPopulatePatch.java` - Works (fires event for menu population)
- ❌ `ActionArrayBoundsCheckPatch.java` - Wrong location (patches poll, crash in constructor)
- ⚠️ `BodyPartModActionsPatch.java` - Right idea, never reached (need constructor fix first)

### Build Commands
```bash
# Clean rebuild
./gradlew clean build

# Deploy to server
./gradlew distribution
cp build/distributions/wurmmodloader-*.zip "<server-dir>/"
cd "<server-dir>" && unzip -o wurmmodloader-*.zip
```

---

## Historical Investigation Notes

(Previous investigation steps preserved for reference - see Git history)

The investigation went through multiple phases:
1. Initially thought `BodyPartBehaviour.action()` wasn't being called
2. Discovered `Action.poll()` was the entry point
3. Created `ActionArrayBoundsCheckPatch` for `poll()` - didn't work
4. **FINAL DISCOVERY**: Crash happens in Action **constructor**, before poll() is called

This explains why:
- All `insertBefore()` patches on action methods never executed (code never reached)
- `insertAfter()` patches work (run after successful execution)
- No diagnostic logs appeared (code path never runs)
- Server crashes silently (exception in constructor)

---

## Success Criteria

When fixed:
1. ✅ Server starts, patches register
2. ✅ Right-click body, menu shows "Power Fantasy" button
3. ✅ Click button
4. ⬅️ **Action constructor skips array access for action 949** (NEW FIX NEEDED)
5. ⬅️ Action.poll() executes
6. ⬅️ BodyPartModActionsPatch intercepts and delegates to ModActions
7. ⬅️ ViewStatsActionPerformer.action() executes
8. ⬅️ Player sees: "=== BODY MENU ACTIONS WORKING! ===" message
9. ✅ No crash

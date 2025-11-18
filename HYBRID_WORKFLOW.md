# Hybrid Workflow: Claude + Qwen Coder

Save 70-80% on Claude tokens by using Claude for research and Qwen for coding.

---

## 🎯 The Strategy

**Claude (expensive, smart)** → Research, architecture, design
**Qwen (free, fast)** → Code generation with targeted context
**Code Index** → Bridge between them (provides minimal context)

---

## 🔄 Workflow

### Phase 1: Research with Claude (Token Cost: Low)

Ask Claude to find relevant code:

```
You: "I want to add a damage multiplier event. Show me similar events."

Claude: [Uses codeindex]
  codeindex search "ItemDamage"
  codeindex pattern event_handlers

  Here's the pattern:
  - ItemDamageEvent extends Event
  - Fired from ItemDamagePatch
  - Hook in ServerHook
  - Follow this structure...
```

**Tokens used: ~2000** (mostly my responses, not file reads)

### Phase 2: Extract Context for Qwen (Free)

```bash
python3 extract_context_for_llm.py search "ItemDamage" \
  "Create CombatDamageMultiplierEvent" > /tmp/qwen_context.txt
```

**Output (~100 tokens):**
```
# Task: Create CombatDamageMultiplierEvent

# Available Classes:
  - com.garward.wurmmodloader.api.events.item.ItemDamageEvent

# Available Methods:
  - ItemDamageEvent(long itemId, String itemName, float damage, float currentDamage)
    // Tags: combat, item, event
  - fireItemDamageEvent(long itemId, String itemName, ...)
    // Tags: combat, item, event, static
```

### Phase 3: Generate Code with Qwen (Free)

Feed to Qwen:
```
Context:
[paste from /tmp/qwen_context.txt]

Task: Create a new event class CombatDamageMultiplierEvent that:
1. Extends Event like ItemDamageEvent does
2. Takes playerId, targetId, and multiplier as parameters
3. Has getter methods for each field
4. Is cancellable
5. Follow the same pattern as ItemDamageEvent

Generate the Java class.
```

**Qwen generates code** using correct class names, methods, patterns.

**Tokens used: 0 (local model)**

### Phase 4: Validation with Claude (Token Cost: Low)

```
You: [paste Qwen's generated code]
    "Does this event class look correct?"

Claude: [Quick review]
  - Checks integration points
  - Verifies it matches patterns
  - Suggests improvements
```

**Tokens used: ~1500** (code review is cheaper than generation)

---

## 📊 Token Comparison

### Traditional (All Claude)

```
You: "Create CombatDamageMultiplierEvent"

Claude:
  - Reads ItemDamageEvent.java (1500 tokens)
  - Reads ItemDamagePatch.java (1200 tokens)
  - Reads ServerHook.java (8000 tokens)
  - Reads Event.java (2000 tokens)
  - Generates code (3000 tokens output)
  - Explains implementation (1000 tokens)

Total: ~16,700 tokens
```

### Hybrid (Claude + Qwen)

```
Phase 1 - Claude Research: 2000 tokens
Phase 2 - Extract Context: 0 tokens (local script)
Phase 3 - Qwen Generation: 0 tokens (local model)
Phase 4 - Claude Review: 1500 tokens

Total: ~3,500 tokens
```

**Savings: 79% (13,200 tokens saved)**

---

## 🛠️ Available Context Extractors

### 1. Search by Name
```bash
python3 extract_context_for_llm.py search "ItemDamage" \
  "Your task description"
```

### 2. Pattern Search
```bash
python3 extract_context_for_llm.py pattern event_handlers \
  "Create new event following this pattern"
```

### 3. File Context
```bash
python3 extract_context_for_llm.py file ServerHook \
  "Add new hook method"
```

---

## 💡 When to Use What

### Use Claude For:

**✅ Architecture Decisions**
- "Should this be an event or a direct hook?"
- "Where does this fit in the framework?"
- "What's the best way to implement X?"

**✅ Code Exploration**
- "How does combat damage work?"
- "Show me all event handlers"
- "Find examples of bytecode patches"

**✅ Complex Logic**
- Multi-file refactoring
- Complex algorithm implementation
- Debugging subtle issues

**✅ Design Review**
- "Is this approach correct?"
- "Does this follow framework patterns?"
- "Any integration issues?"

### Use Qwen For:

**✅ Boilerplate Generation**
- Event classes
- Config classes
- Getters/setters
- Builder patterns

**✅ Repetitive Code**
- Similar methods with different parameters
- Test case generation
- JavaDoc generation

**✅ Straightforward Implementation**
- Once you know WHAT to call
- Following a clear pattern
- Data classes

**✅ Code Transformation**
- Renaming refactors
- Adding logging
- Format changes

---

## 🎯 Example Tasks

### Task: Add New Event Type

**Claude Phase (2000 tokens):**
```
You: "How do I add a new SpellCastEvent?"
Claude: [Uses codeindex to show event pattern]
```

**Extract Phase (100 tokens):**
```bash
python3 extract_context_for_llm.py pattern event_handlers \
  "Create SpellCastEvent" > /tmp/context.txt
```

**Qwen Phase (FREE):**
```
[Paste context + instructions]
Qwen: [Generates event class]
```

**Claude Validation (1500 tokens):**
```
You: "Review this event class"
Claude: [Quick check]
```

**Total: 3600 tokens vs 15000 tokens = 76% savings**

### Task: Add Bytecode Patch

**Claude Phase (2500 tokens):**
```
You: "Show me how bytecode patches work"
Claude: [Explains, shows examples using codeindex]
```

**Extract Phase (150 tokens):**
```bash
python3 extract_context_for_llm.py pattern bytecode_patches \
  "Create spell resistance patch" > /tmp/context.txt
```

**Qwen Phase (FREE):**
```
[Context + clear instructions]
Qwen: [Generates patch class]
```

**Claude Validation (2000 tokens):**
```
You: "Will this patch work correctly?"
Claude: [Reviews bytecode manipulation]
```

**Total: 4650 tokens vs 20000 tokens = 77% savings**

---

## 📈 Expected Token Savings

Based on typical coding tasks:

| Task Type | All Claude | Hybrid | Savings |
|-----------|-----------|--------|---------|
| New event class | 15k | 3.5k | 77% |
| Bytecode patch | 20k | 4.5k | 78% |
| Config class | 8k | 2k | 75% |
| Helper methods | 10k | 3k | 70% |
| Refactoring | 12k | 3.5k | 71% |

**Average: 74% savings on coding tasks**

---

## 🔧 Setup Your Qwen MCP Tool

If you have Qwen accessible via MCP, you can create a workflow:

```bash
# 1. Research with Claude
codeindex search "ItemDamage"

# 2. Extract context
python3 extract_context_for_llm.py search "ItemDamage" \
  "Create damage multiplier" > /tmp/context.txt

# 3. Call Qwen via MCP
# [Your Qwen MCP tool] < /tmp/context.txt

# 4. Validate with Claude
# Paste result back to Claude for review
```

---

## ⚖️ Trade-offs

### Pros:
- **Massive token savings** (70-80% on coding tasks)
- **Faster iteration** (local model is instant)
- **No rate limits** on Qwen
- **Still get Claude's intelligence** for hard problems

### Cons:
- **More steps** in workflow
- **Qwen may need multiple attempts** (less smart than Claude)
- **Context extraction** requires manual step
- **Best for structured tasks** (not exploratory work)

---

## 🎓 Getting Started

1. **Install script:**
   ```bash
   cd ~/Scripts/Games/WurmUnlimited/WurmModLoader
   chmod +x extract_context_for_llm.py
   ```

2. **Try an extraction:**
   ```bash
   python3 extract_context_for_llm.py search "ServerHook" "Test"
   ```

3. **Use the workflow:**
   - Ask Claude to research
   - Extract context
   - Generate with Qwen
   - Validate with Claude

4. **Iterate:**
   - If Qwen's code doesn't work, ask Claude why
   - Refine the context or instructions
   - Try again

---

## 💰 Cost Analysis

If you hit your limit in 5 days and use Claude for ~20 coding tasks/week:

**Current:**
- 20 coding tasks × 15k tokens = 300k tokens
- Plus research/debugging = 500k total
- Lasts: 5 days

**With Hybrid:**
- 20 coding tasks × 4k tokens = 80k tokens
- Plus research/debugging = 280k total
- Lasts: **~9 days** (beyond weekly reset!)

**You'd likely get through the full week comfortably.**

---

## 🚀 Pro Tips

1. **Be specific with Qwen** - It's less smart, needs clear instructions
2. **Use Claude for the hard parts** - Architecture, complex logic
3. **Extract minimal context** - Don't over-provide information
4. **Iterate quickly** - Qwen is fast, don't be afraid to regenerate
5. **Always validate** - Claude's review catches Qwen's mistakes

---

## 📖 See Also

- `README_CODEINDEX.md` - Code index documentation
- `extract_context_for_llm.py` - Context extraction tool
- `CLAUDE.md` - Project structure and conventions

---

**Ready to save tokens!** Try the hybrid workflow on your next coding task.

# 🤖 PURE AUTOMATION: Qwen Code Generation via MCP

**The ultimate workflow**: You ask Claude, Claude generates code using Qwen, all automatic!

---

## 🎯 What This Is

**NEW MCP Endpoint:** `/qwen/generate`

**What it does:**
1. You ask me (Claude) to create code
2. I use code index to understand the pattern
3. I call Qwen API endpoint directly
4. Qwen generates code with smart context
5. I show you the result

**No manual steps. Pure automation.**

---

## 🚀 How It Works

### Old Workflow (Manual)

```
You: "Create a SpellDamageEvent"

Me: "Here's how damage events work..."
    [Uses codeindex to show patterns]

You: [Creates prompt.txt]
You: qwen_smart_codegen.py prompt.txt --search "ItemDamage"

Qwen: [Generates code]

You: [Pastes back to me for review]
```

**Steps: 5 manual actions**

### New Workflow (AUTOMATED)

```
You: "Generate a SpellDamageEvent using Qwen"

Me: [Calls codeindex to understand pattern]
Me: [Calls /qwen/generate with smart context]
Me: "Here's the generated code:"
    [Shows you the result]
```

**Steps: 1 request, zero manual actions!**

---

## 📡 The MCP Endpoint

**Endpoint:** `POST /qwen/generate`

**Request:**
```json
{
  "prompt": "Create SpellDamageEvent class...",
  "search_term": "ItemDamage",
  "dry_run": true
}
```

**Response:**
```json
{
  "success": true,
  "generated_code": "...",
  "context_used": "ItemDamage"
}
```

**Parameters:**
- `prompt` - What to generate (required)
- `search_term` - Code index search for context
- `pattern_type` - Pattern search (event_handlers, bytecode_patches, etc.)
- `context_file` - Specific file for context
- `dry_run` - Return code without writing files (default: false)

---

## 💬 How to Use (Just Ask!)

### Example 1: New Event Class

**You:**
```
"Use Qwen to generate a SpellResistanceEvent class.
It should extend Event, take playerId and resistanceChance,
follow the ItemDamageEvent pattern."
```

**Me (Claude):**
```bash
# I automatically:
# 1. Call codeindex to find ItemDamageEvent pattern
# 2. Call /qwen/generate with smart context
# 3. Show you the result

Generated code:
[Shows the SpellResistanceEvent class]
```

### Example 2: Bytecode Patch

**You:**
```
"Generate a bytecode patch for stamina costs using Qwen.
Base it on existing patches."
```

**Me:**
```bash
# I automatically:
# 1. Query codeindex for bytecode_patches pattern
# 2. Generate with /qwen/generate
# 3. Present result

Generated StaminaCostPatch:
[Shows the patch class]
```

### Example 3: Config Class

**You:**
```
"Use Qwen to create a PowerScalingConfig class
with baseMultiplier, maxLevel, and enabled fields."
```

**Me:**
```bash
# I automatically find config patterns and generate
Generated PowerScalingConfig:
[Shows the config class]
```

---

## 🎯 Automation Flow

```
┌─────────────┐
│     YOU     │ "Generate X with Qwen"
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   CLAUDE    │ Understands request
└──────┬──────┘
       │
       ├─────────► codeindex_search("Pattern")
       │           └──► Returns: signatures, examples
       │
       ├─────────► /qwen/generate
       │           ├─ Prompt: Your request
       │           ├─ Context: Smart extract (~100 tokens)
       │           └──► Qwen API call
       │
       ▼
┌─────────────┐
│    QWEN     │ Generates code
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   CLAUDE    │ Shows you result
└──────┬──────┘
       │
       ▼
┌─────────────┐
│     YOU     │ Review & use
└─────────────┘
```

**Total time: ~30 seconds**
**Your effort: One request**

---

## ⚙️ Setup (Already Done!)

✅ MCP server running on port 8090
✅ `/qwen/generate` endpoint added
✅ Code index integration active
✅ qwen_smart_codegen.py configured

**Just ask and it works!**

---

## 🧪 Direct API Testing

If you want to test the endpoint directly:

```bash
curl -X POST http://localhost:8090/qwen/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create TestEvent class extending Event",
    "search_term": "ItemDamage"
  }'
```

But you don't need to - just ask me and I'll handle it!

---

## 📊 Token Savings (Massive!)

### Traditional Approach

```
You → Claude: "Create SpellDamageEvent"
Claude reads files: 10,000 tokens
Claude generates code: 3,000 tokens
Total: 13,000 tokens
```

### Qwen Manual Approach

```
You → Claude: "How do events work?"
Claude: 2,000 tokens

You → Qwen (manual):
Context: 100 tokens
Generation: 0 tokens (free/OpenRouter)

You → Claude: "Review this"
Claude: 1,500 tokens

Total Claude: 3,500 tokens (73% savings)
```

### Qwen Automated (This!)

```
You → Claude: "Generate with Qwen"
Claude: 500 tokens (just orchestration)

Claude → codeindex: ~50 tokens
Claude → /qwen/generate: 0 tokens (API call)
Qwen → generates: 0 Claude tokens (separate API)

Claude → You: 500 tokens (showing result)

Total Claude: 1,000 tokens (92% savings!)
```

**YOU SAVE 12,000 TOKENS PER CODE GENERATION TASK!**

---

## 🎓 Use Cases

### When to Use This

✅ **Creating new classes** (events, configs, data classes)
✅ **Generating boilerplate** (getters, builders, constructors)
✅ **Following patterns** (when you can point to an example)
✅ **Repetitive code** (similar classes with different fields)
✅ **Standard implementations** (common interfaces)

### When to Use Me (Claude) Directly

🔹 **Complex logic** - Algorithms, multi-file refactoring
🔹 **Architecture decisions** - Design choices, patterns
🔹 **Debugging** - Finding subtle bugs
🔹 **Exploration** - Understanding existing code
🔹 **Review** - Validating generated code

**Best practice:** Use Qwen for generation, use me for validation!

---

## 💡 Pro Tips

### 1. Be Specific
```
❌ "Generate an event"
✅ "Generate SpellResistanceEvent extending Event with playerId and resistanceChance fields"
```

### 2. Reference Patterns
```
✅ "Like ItemDamageEvent but for spells"
✅ "Following the bytecode patch pattern"
✅ "Similar to the config classes"
```

### 3. Let Me Orchestrate
```
✅ "Use Qwen to generate..."
✅ "Generate with Qwen based on..."

I'll automatically:
- Find the right patterns
- Extract smart context
- Call Qwen
- Show you results
```

### 4. Validate Results
```
After generation:
"Does this look correct?"
"Any issues with this code?"
"Should I change anything?"

I'll review and suggest improvements!
```

---

## 🔄 Complete Example

**You:**
```
"I need a CombatDamageMultiplierEvent class. Use Qwen to generate it
based on the ItemDamageEvent pattern. It should have playerId, targetId,
and multiplier fields, extend Event, and be cancellable."
```

**Me (automatically):**
```bash
# Step 1: Query code index
codeindex search "ItemDamage"
# Result: Found ItemDamageEvent pattern

# Step 2: Call Qwen endpoint
POST /qwen/generate {
  "prompt": "Create CombatDamageMultiplierEvent...",
  "search_term": "ItemDamage"
}

# Step 3: Present result
```

**Me:**
```
Here's the generated CombatDamageMultiplierEvent:

[Shows full class with:
- Package declaration
- Imports
- Class extending Event
- Fields: playerId, targetId, multiplier
- Constructor
- Getters
- isCancellable() = true
- Full JavaDoc
]

This follows the ItemDamageEvent pattern. Want me to review it for any issues?
```

**Total time:** 30 seconds
**Your effort:** One request
**Claude tokens:** ~1,000 (vs 13,000 doing it myself)

---

## 📈 Impact on Your Weekly Limit

**Before (All Claude):**
- 20 code generation tasks × 13k tokens = 260k tokens

**With Manual Qwen:**
- 20 tasks × 3.5k tokens = 70k tokens

**With Automated Qwen (This!):**
- 20 tasks × 1k tokens = **20k tokens**

**Savings on code generation alone: 240k tokens!**

Combined with code index exploration savings, you'll easily make it through the full week.

---

## 🎉 Bottom Line

**What you have now:**

1. **Ask me to generate code**
2. **I handle everything**
   - Find patterns via code index
   - Generate via Qwen endpoint
   - Show you results
3. **Review and use**

**Zero manual steps. Pure automation. 92% token savings.**

---

## 🚀 Try It Now!

Just say:

```
"Use Qwen to generate a [ClassName] that [does X],
based on [ExamplePattern]"
```

And watch the magic happen!

---

**Server:** http://localhost:8090/qwen/generate
**Docs:** http://localhost:8090/docs

Everything is ready. Just ask!

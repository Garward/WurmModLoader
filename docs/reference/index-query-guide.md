# Code Index Query Guide

## Quick Start

The index has **100k+ lines** of JSON, so use `query_code_index.py` to extract compact, LLM-friendly views.

## Common Commands

### 1. **Codebase Overview** (Best for initial context)
```bash
python3 query_code_index.py overview
```
**Output:** Stats, tag distribution, package breakdown, priority levels
**Lines:** ~60
**Use case:** Understanding codebase structure and scope

---

### 2. **High Priority Files** (Core framework components)
```bash
python3 query_code_index.py --max-lines 100 high-priority
```
**Output:** Critical files sorted by method count
**Use case:** Finding entry points and main hooks

---

### 3. **Search by Name** (Find specific classes/methods)
```bash
python3 query_code_index.py --max-lines 50 search "ItemDamage"
```
**Output:** All matching methods and classes with file locations
**Use case:** Locating specific functionality

---

### 4. **Search by Tag** (Find related code)
```bash
python3 query_code_index.py --max-lines 80 tag event_handler
```
**Available tags:** `event_handler`, `bytecode_patch`, `hook`, `combat`, `skill`, `item`, `creature`, `magic`, `config`, etc.

**Use case:** Finding all code of a specific type

---

### 5. **Pattern Search** (Semantic queries)
```bash
python3 query_code_index.py --max-lines 80 pattern bytecode_patches
```
**Available patterns:**
- `event_handlers` - All event handler code
- `bytecode_patches` - All bytecode modification code
- `config` - Configuration-related code
- `combat` - Combat system code
- `initialization` - Startup/init code
- `api` - Public API methods

**Use case:** Understanding subsystems

---

### 6. **Module Summary** (Explore directory)
```bash
python3 query_code_index.py --max-lines 100 module "wurmmodloader-api"
```
**Output:** File list with class counts, sorted by complexity
**Use case:** Understanding module structure

---

### 7. **File Details** (Deep dive on one file)
```bash
python3 query_code_index.py file ServerHook
```
**Output:** Full file summary with all methods, imports, package info
**Use case:** Understanding a specific file

---

## Tips for LLM Context

### Start Broad, Then Narrow
```bash
# 1. Get overview
python3 query_code_index.py overview

# 2. Find relevant module
python3 query_code_index.py module "bytecode/patches"

# 3. Search for specific functionality
python3 query_code_index.py search "CombatRating"

# 4. Get file details
python3 query_code_index.py file CombatRatingEvent
```

### Control Output Size
Use `--max-lines N` to limit output (default: 200)

```bash
# Compact for quick reference
python3 query_code_index.py --max-lines 50 pattern event_handlers

# Detailed for deep investigation
python3 query_code_index.py --max-lines 500 high-priority
```

### Combine with grep for precision
```bash
# Find all combat-related event handlers
python3 query_code_index.py pattern event_handlers | grep -i combat
```

---

## Example Workflow: "How do events work?"

```bash
# 1. Find all event handlers
python3 query_code_index.py --max-lines 100 pattern event_handlers

# 2. Look at event API structure
python3 query_code_index.py module "wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events"

# 3. Check core event registration
python3 query_code_index.py search "fireEvent"

# 4. Examine ServerHook implementation
python3 query_code_index.py file ServerHook
```

---

## Index Statistics

- **615 files** indexed
- **1,421 classes** cataloged
- **4,688 methods** tracked
- **185 high priority** files
- **Top packages:** modsupport, core, api, mods
- **Top tags:** accessor, static, override, event_handler, combat

---

## Regenerate Index

```bash
python3 index_code_index.py
```

Regenerate after:
- Adding new files
- Significant refactoring
- Switching branches

# WurmModLoader Code Index System

Complete documentation for the Java codebase indexing and MCP server integration.

---

## 📚 Quick Links

- **[MCP Integration Complete](MCP_INTEGRATION_COMPLETE.md)** - What was done
- **[MCP Aliases Guide](MCP_ALIASES_GUIDE.md)** - Using mcp-start, mcp-stop, etc.
- **[Index Query Guide](INDEX_QUERY_GUIDE.md)** - CLI usage
- **[Codebase Summary](CODEINDEX_SUMMARY.md)** - Complete overview
- **[Cheatsheet](CODEBASE_CHEATSHEET.md)** - 500-line LLM-friendly summary

---

## ⚡ Quick Start (30 seconds)

```bash
# 1. Start MCP server
mcp-start

# 2. Check status
mcp-status

# 3. View in browser
firefox http://localhost:8090/docs
```

Done! Your codebase is now queryable via MCP.

---

## 🎯 What This System Does

### The Problem
- **615 Java files**, 1,421 classes, 4,688 methods
- Raw index is **100k+ lines** of JSON
- Too large for LLM context
- Hard to find relevant code

### The Solution
**3-Part System:**

1. **Index Generator** (`index_code_index.py`)
   - Scans all Java files
   - Extracts classes, methods, annotations
   - Tags by purpose (events, hooks, combat, etc.)
   - Generates structured JSON

2. **Query Tools** (`query_code_index.py`)
   - CLI with 7 commands
   - Filters by tag, pattern, module, priority
   - Returns compact, relevant results

3. **MCP Server** (ClaudeEVO integration)
   - 8 HTTP endpoints
   - Used by LLMs and other tools
   - Auto-reloads on index changes

---

## 📦 Files Created

### Core System
| File | Purpose |
|------|---------|
| `index_code_index.py` | Generate the index |
| `query_code_index.py` | Query from CLI |
| `code_index.json` | The index (100k+ lines) |
| `CODEBASE_CHEATSHEET.md` | LLM-friendly summary (500 lines) |

### MCP Integration
| File | Purpose |
|------|---------|
| `codeindex_server_extension.py` | FastAPI endpoints |
| ClaudeEVO `openwebui_terminal_server.py` | Modified to load index |
| ClaudeEVO `requirements.txt` | Added FastAPI/Uvicorn |

### Documentation
| File | Purpose |
|------|---------|
| `README_CODEINDEX.md` | This file |
| `MCP_INTEGRATION_COMPLETE.md` | Integration summary |
| `MCP_ALIASES_GUIDE.md` | Bash aliases reference |
| `INDEX_QUERY_GUIDE.md` | CLI command reference |
| `CODEBASE_SUMMARY.md` | Complete system overview |

### Scripts
| File | Purpose |
|------|---------|
| `generate_cheatsheet.sh` | Auto-generate cheatsheet |
| Bash aliases in `~/.bash_aliases` | mcp-start, mcp-stop, etc. |

---

## 🚀 Usage Examples

### For Developers (You)

#### CLI Queries
```bash
# Get overview
python3 query_code_index.py overview

# Find all event handlers
python3 query_code_index.py pattern event_handlers

# Search for specific code
python3 query_code_index.py search "ItemDamage"

# Explore a module
python3 query_code_index.py module "wurmmodloader-api"
```

#### MCP Server
```bash
# Start
mcp-start

# Status
mcp-status

# Logs
mcp-logs

# Stop
mcp-stop
```

### For LLMs (Claude, etc.)

When connected to MCP server at `http://localhost:8090`:

**Query: "What's in this codebase?"**
```
LLM calls: GET /codeindex/overview
Returns: Stats, packages, tags (60 lines)
```

**Query: "Find all event handlers"**
```
LLM calls: POST /codeindex/pattern {"pattern_type": "event_handlers"}
Returns: 91 event handlers with locations
```

**Query: "How does ItemDamage work?"**
```
LLM calls: POST /codeindex/search {"term": "ItemDamage"}
Returns: Matching classes/methods with javadoc
```

---

## 📊 Index Statistics

- **Files**: 615 Java files
- **Classes**: 1,421 classes
- **Methods**: 4,688 methods
- **High Priority**: 185 core files
- **Top Package**: `com.garward.wurmmodloader.modsupport` (139 files)
- **Top Tag**: `accessor` (1,189 methods)

---

## 🔄 Maintenance

### Regenerate Index
After code changes:
```bash
cd ~/Scripts/Games/WurmUnlimited/WurmModLoader
python3 index_code_index.py
```

MCP server auto-detects changes (no restart needed).

### Regenerate Cheatsheet
```bash
./generate_cheatsheet.sh > CODEBASE_CHEATSHEET.md
```

---

## 🌐 MCP Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/codeindex/status` | GET | Index availability |
| `/codeindex/overview` | GET | Codebase statistics |
| `/codeindex/search` | POST | Search by name |
| `/codeindex/tag` | POST | Filter by tag |
| `/codeindex/pattern` | POST | Semantic search |
| `/codeindex/module` | POST | Module summary |
| `/codeindex/file` | POST | File details |
| `/codeindex/high-priority` | GET | Core files |

**Interactive Docs**: http://localhost:8090/docs

---

## 🎓 Tutorials

### For New Developers

**Understanding the Codebase:**
```bash
# 1. Get bird's eye view
python3 query_code_index.py overview

# 2. Find core components
python3 query_code_index.py --max-lines 50 high-priority

# 3. Understand event system
python3 query_code_index.py pattern event_handlers

# 4. Deep dive on ServerHook
python3 query_code_index.py file ServerHook
```

### For LLM Development

**Connecting Claude Desktop:**
```json
{
  "mcpServers": {
    "wurm-codeindex": {
      "url": "http://localhost:8090"
    }
  }
}
```

Then ask Claude questions about the codebase!

---

## 💡 Use Cases

### 1. Code Navigation
"Where is combat damage calculated?"
→ Search by tag: `combat`

### 2. Architecture Understanding
"How does the event system work?"
→ Pattern search: `event_handlers`

### 3. Finding Examples
"Show me bytecode patches"
→ Pattern search: `bytecode_patches`

### 4. Module Exploration
"What's in the API module?"
→ Module summary: `wurmmodloader-api`

### 5. Priority Identification
"What are the core files?"
→ High priority list

---

## 🔧 Technical Details

### Index Schema
```json
{
  "file_path": {
    "package": "com.example",
    "classes": ["ClassName"],
    "methods": {
      "methodName": {
        "signature": "methodName(params)",
        "type": "event_handler",
        "tags": ["event", "handler"],
        "javadoc_preview": "...",
        "annotations": ["Override"],
        "line": 123
      }
    },
    "priority_level": "high"
  }
}
```

### Tag System
- **Framework**: bytecode_patch, hook, event, api, core
- **Game Systems**: combat, skill, item, creature, magic
- **Patterns**: handler, initialization, configuration
- **Modifiers**: static, abstract, final, override

---

## ✅ Tested & Working

- ✅ Index generation (615 files)
- ✅ All 7 CLI commands
- ✅ All 8 MCP endpoints
- ✅ Auto-reload on index update
- ✅ Bash aliases (mcp-start, etc.)
- ✅ Interactive documentation
- ✅ Code Index integration in ClaudeEVO

---

## 🎉 Benefits

### For You
- **Faster navigation**: Find code in seconds
- **Better understanding**: See structure at a glance
- **Easy maintenance**: Auto-generated, always current

### For LLMs
- **Instant context**: No grepping required
- **Semantic search**: Find by concept, not keyword
- **Structured data**: Clean JSON, no parsing
- **Priority aware**: Core components highlighted

### For Team
- **Onboarding**: New devs understand codebase quickly
- **Documentation**: Auto-generated from code
- **API discovery**: See all public methods instantly

---

## 🚧 Future Enhancements

Possible additions:
- Call graph analysis
- Dependency mapping
- Field/variable extraction
- Diff view (what changed)
- Cross-reference links
- Test coverage integration

---

## 📖 Full Documentation

See individual guides for details:
- **CLI Usage**: [INDEX_QUERY_GUIDE.md](INDEX_QUERY_GUIDE.md)
- **MCP Setup**: [MCP_INTEGRATION_COMPLETE.md](MCP_INTEGRATION_COMPLETE.md)
- **Bash Aliases**: [MCP_ALIASES_GUIDE.md](MCP_ALIASES_GUIDE.md)
- **System Overview**: [CODEBASE_SUMMARY.md](CODEBASE_SUMMARY.md)

---

## 🙏 Credits

Created for WurmModLoader Java codebase analysis and LLM integration.

**Components:**
- Python-based index generator
- Regex Java parser (no external dependencies)
- FastAPI MCP server integration
- Bash convenience aliases

---

**Ready to use!** Run `mcp-start` and visit http://localhost:8090/docs

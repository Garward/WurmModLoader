# Integrate Code Index with Your MCP Server

## Option 1: Add to Existing ClaudeEVO Server (Recommended)

### Step 1: Copy Extension Module
```bash
cp codeindex_server_extension.py /home/garward/ClaudeEVO/claude/scripts/daemon_tools/
cp query_code_index.py /home/garward/ClaudeEVO/claude/scripts/daemon_tools/
```

### Step 2: Update openwebui_terminal_server.py

Add this import at the top (after other imports):
```python
from codeindex_server_extension import setup_codeindex_endpoints
```

Add this line before the `if __name__ == "__main__"` block:
```python
# Setup code index endpoints
try:
    # Look for code index in common project locations
    index_locations = [
        "/home/garward/Scripts/Games/WurmUnlimited/WurmModLoader/code_index.json",
        str(CLAUDE_ROOT / "code_index.json"),
    ]

    index_path = None
    for loc in index_locations:
        if Path(loc).exists():
            index_path = loc
            break

    if index_path:
        setup_codeindex_endpoints(app, index_path)
        print(f"✅ Code Index loaded from: {index_path}")
    else:
        print("⚠️  No code index found (searched: WurmModLoader, ClaudeEVO)")
except Exception as e:
    print(f"⚠️  Code Index setup failed: {e}")
```

### Step 3: Restart Server
```bash
# Kill existing server
pkill -f openwebui_terminal_server

# Start with new code index support
cd /home/garward/ClaudeEVO/claude && ./scripts/daemon_tools/start_openwebui_server.sh
```

### Step 4: Test Endpoints
```bash
# Check status
curl http://localhost:8090/codeindex/status

# Get overview
curl http://localhost:8090/codeindex/overview

# Search for something
curl -X POST http://localhost:8090/codeindex/search \
  -H "Content-Type: application/json" \
  -d '{"term": "ServerHook", "max_results": 10}'
```

---

## Option 2: Run Standalone Code Index Server

Run on a different port (doesn't require modifying ClaudeEVO):

```bash
cd /home/garward/Scripts/Games/WurmUnlimited/WurmModLoader
python3 codeindex_server_extension.py
```

This runs on **port 8091** (separate from your main MCP server on 8090).

Then add to OpenWebUI:
- URL: `http://localhost:8091`
- Will show up as separate tool functions

---

## New Tools Available (Once Integrated)

When you add to OpenWebUI, these functions become available:

### 1. `codeindex_status`
Check if index is available and up-to-date

### 2. `codeindex_overview`
Get codebase statistics and structure
- Total files, classes, methods
- Tag distribution
- Package breakdown
- Priority levels

### 3. `codeindex_search`
Search by method/class name
```json
{
  "term": "ItemDamage",
  "max_results": 20
}
```

### 4. `codeindex_tag`
Find all code with specific tag
```json
{
  "tag": "event_handler",
  "max_results": 50
}
```

### 5. `codeindex_pattern`
Semantic search for subsystems
```json
{
  "pattern_type": "bytecode_patches",
  "max_results": 80
}
```

Available patterns:
- `event_handlers` - All event handling code
- `bytecode_patches` - Bytecode modification code
- `config` - Configuration code
- `combat` - Combat system code
- `initialization` - Startup code
- `api` - Public API methods

### 6. `codeindex_module`
Get module/directory summary
```json
{
  "module_path": "wurmmodloader-api"
}
```

### 7. `codeindex_file`
Get detailed file information
```json
{
  "file_path": "ServerHook"
}
```

### 8. `codeindex_high_priority`
List high-priority framework files
```
GET /codeindex/high-priority?max_results=20
```

---

## Testing in OpenWebUI

Once integrated and server restarted:

1. Go to **Admin Panel** → **Connections** → **OpenAPI Function Servers**
2. Your server should show new functions: `codeindex_*`
3. In chat, you can now call:
   - "Show me the codebase overview"
   - "Find all event handlers"
   - "What's in the ServerHook file?"
   - "Show combat-related code"

The LLM will automatically use these tools when relevant!

---

## Updating the Index

Regenerate when codebase changes:
```bash
cd /home/garward/Scripts/Games/WurmUnlimited/WurmModLoader
python3 index_code_index.py
```

The server auto-detects changes and reloads the index.

---

## For Claude Code (This Editor)

While the MCP server is great for OpenWebUI, for **Claude Code** I can also:

### Use via Bash (Current Method)
```bash
cd /home/garward/Scripts/Games/WurmUnlimited/WurmModLoader
python3 query_code_index.py overview
```

### Add to Approved Commands
You can add these to my auto-approved list in the instructions above so I can run them without asking.

Already approved:
- `Bash(python3:*)` ✅
- `Bash(chmod:*)` ✅
- `Bash(./generate_cheatsheet.sh:*)` ✅
- `Read(//home/garward/**)` ✅

So I can already use the query tool without permission!

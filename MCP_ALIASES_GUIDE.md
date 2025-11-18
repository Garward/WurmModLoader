# MCP Server Bash Aliases - Quick Reference

## ✅ Aliases Added to ~/.bash_aliases

Six new aliases for easy MCP server management:

### 1. `mcp-start`
Start the MCP server with code index support
```bash
mcp-start
```
**Output:**
```
✅ MCP Server started (PID: 12345)
🔧 Server: http://localhost:8090
📖 Docs: http://localhost:8090/docs
🔍 Code Index: http://localhost:8090/codeindex/status
📝 Logs: tail -f /tmp/mcp_server.log
```

---

### 2. `mcp-stop`
Stop the MCP server
```bash
mcp-stop
```
**Output:**
```
🛑 MCP Server stopped
```

---

### 3. `mcp-restart`
Restart the MCP server
```bash
mcp-restart
```
Equivalent to: `mcp-stop && sleep 2 && mcp-start`

---

### 4. `mcp-status`
Check MCP server status
```bash
mcp-status
```
**Output:**
```
=== MCP Server Status ===
✅ Running (PID: 12345)

=== Port Status ===
✅ Port 8090 - LISTENING

=== Endpoints ===
🔍 Code Index: ✅ Available
🔧 Server: ✅ Responding
```

---

### 5. `mcp-logs`
Follow MCP server logs in real-time
```bash
mcp-logs
```
Equivalent to: `tail -f /tmp/mcp_server.log`

Press `Ctrl+C` to stop following.

---

### 6. `mcp-urls`
Show all available endpoints
```bash
mcp-urls
```
**Output:**
```
🔧 MCP Server: http://localhost:8090
📖 Interactive Docs: http://localhost:8090/docs
📝 OpenAPI Schema: http://localhost:8090/openapi.json
🔍 Code Index Status: http://localhost:8090/codeindex/status
📊 Server Status: http://localhost:8090/tools/status
```

---

## 🚀 Quick Start Workflow

### First Time Setup
```bash
# Aliases are already in ~/.bash_aliases
# Just reload your shell or open a new terminal
```

### Daily Usage
```bash
# Start server
mcp-start

# Check it's running
mcp-status

# View URLs
mcp-urls

# When done
mcp-stop
```

### Debugging
```bash
# Check status first
mcp-status

# If issues, check logs
mcp-logs

# Try restart
mcp-restart
```

---

## 📝 Log Location

- **Server logs**: `/tmp/mcp_server.log`
- **PID file**: `/tmp/mcp_server.pid`

---

## 🔧 What the Server Provides

### Original Features
- `/bash` - Execute shell commands
- `/cd` - Change directory
- `/pwd` - Get current directory
- `/tools/status` - Tool system status

### New Code Index Features
- `/codeindex/status` - Check index availability
- `/codeindex/overview` - Codebase statistics
- `/codeindex/search` - Search by name
- `/codeindex/tag` - Search by tag
- `/codeindex/pattern` - Semantic search
- `/codeindex/module` - Module summary
- `/codeindex/file` - File details
- `/codeindex/high-priority` - High priority files

---

## 🌐 Interactive Documentation

Once server is running:
- Visit: **http://localhost:8090/docs**
- Try endpoints directly in browser
- See full API documentation

---

## 💡 Tips

1. **Auto-start on boot**: Add `mcp-start` to your shell startup if desired
2. **Check before starting**: Run `mcp-status` to see if already running
3. **View all logs**: Use `mcp-logs` for debugging
4. **Quick reference**: Run `mcp-urls` to copy-paste URLs
5. **Restart after updates**: Run `mcp-restart` after regenerating the code index

---

## 🔄 Updating Code Index

When you modify WurmModLoader code:
```bash
cd ~/Scripts/Games/WurmUnlimited/WurmModLoader
python3 index_code_index.py

# No need to restart server - it auto-reloads on next request!
```

---

## 🎯 Example Session

```bash
$ mcp-start
✅ MCP Server started (PID: 12345)
🔧 Server: http://localhost:8090
...

$ mcp-status
=== MCP Server Status ===
✅ Running (PID: 12345)
...

# Work with the server...

$ mcp-stop
🛑 MCP Server stopped
```

---

## ⚠️ Troubleshooting

### Server won't start
```bash
# Check if port is already in use
mcp-status

# Force stop
pkill -f openwebui_terminal_server.py

# Try again
mcp-start
```

### Code index not loading
```bash
# Check status
curl http://localhost:8090/codeindex/status

# Regenerate index
cd ~/Scripts/Games/WurmUnlimited/WurmModLoader
python3 index_code_index.py

# Restart server
mcp-restart
```

---

Ready to use! Just type `mcp-start` in your terminal.

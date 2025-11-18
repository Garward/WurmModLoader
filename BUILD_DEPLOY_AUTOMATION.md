# 🤖 Build & Deploy Automation

**Complete automation for WurmModLoader development workflow**

---

## 🎯 What This Provides

Three automated scripts for the complete build-deploy pipeline:

1. **`build.sh`** - Clean build with stats
2. **`deploy.sh`** - Smart deployment (only copies changed files)
3. **`build-and-deploy.sh`** - Full automation (build + deploy)

Plus convenient bash aliases to run from anywhere!

---

## 📜 The Scripts

### 1. Build Script (`build.sh`)

**What it does:**
- Runs `./gradlew clean build dist`
- Shows build time
- Lists all built artifacts
- Reports success/failure clearly

**Usage:**
```bash
./build.sh
# OR
wurm-build
```

**Output:**
```
======================================================================
🛠️  WurmModLoader Build Script
======================================================================

📂 Project Directory: /home/garward/Scripts/Games/WurmUnlimited/WurmModLoader
🔧 Gradle Version:
Gradle 8.5
JVM:  21.0.5

======================================================================
🏗️  Running: ./gradlew clean build dist
======================================================================

[Gradle output...]

======================================================================
✅ Build Successful!
======================================================================
⏱️  Build Time: 45s

📦 Distribution:
-rw-r--r-- 2.2M WurmModloader-Runtime-0.9.1.zip

📚 Framework JARs:
wurmmodloader-api/build/libs/wurmmodloader-api-0.9.1.jar
wurmmodloader-core/build/libs/wurmmodloader-core-0.9.1.jar
wurmmodloader-legacy/build/libs/wurmmodloader-legacy-0.9.1.jar
wurmmodloader-modsupport/build/libs/wurmmodloader-modsupport-0.9.1.jar

🎮 Mod JARs:
mods/powerscaling/build/libs/powerscaling.jar
mods/upgradetree/build/libs/upgradetree.jar

Ready to deploy! Run: ./deploy.sh
```

---

### 2. Deploy Script (`deploy.sh`)

**What it does:**
- Extracts distribution ZIP to temp location
- Copies framework JARs to server root
- Copies mod JARs to `mods/modname/` folders
- Copies `.properties` files
- **Only copies files that changed** (skips unchanged)
- Shows detailed summary

**Usage:**
```bash
./deploy.sh
# OR
wurm-deploy
```

**Output:**
```
======================================================================
🚀 WurmModLoader Smart Deploy
======================================================================

📦 Distribution: WurmModloader-Runtime-0.9.1.zip
🎯 Server: /home/garward/.../PowerFantasy/Wurm Unlimited Dedicated Server

📂 Extracting distribution to temp...
✓ Extracted to /tmp/wurmmodloader-deploy-12345

======================================================================
📚 Deploying Framework JARs
======================================================================
  ✓ Framework: wurmmodloader-core-0.9.1.jar
  ✓ Framework: wurmmodloader-api-0.9.1.jar
  ⊙ Framework: gson.jar - unchanged
  ⊙ Framework: javassist.jar - unchanged
  ⊙ Framework: snakeyaml-2.2.jar - unchanged

======================================================================
🎮 Deploying Mods
======================================================================
  ✓ Mod: powerscaling
  ⊙ Config: powerscaling.properties - unchanged
  ✓ Mod: upgradetree
  ✓ Config: upgradetree.properties
  ⚠  Mod: soulboundgear - JAR not built (run ./build.sh)

🧹 Cleaning up...
✓ Temp files removed

======================================================================
📊 Deployment Summary
======================================================================
✓ Copied:    5 files
⊙ Unchanged: 6 files

✅ Deployment Complete!

Next steps:
  1. Start your Wurm server
  2. Check logs for mod loading
  3. Test your changes
```

**Key Features:**
- ✓ = File copied (changed)
- ⊙ = File unchanged (skipped)
- ⚠ = Warning (JAR not built)
- ✗ = Error

---

### 3. Build and Deploy Script (`build-and-deploy.sh`)

**What it does:**
- Runs `build.sh`
- If successful, runs `deploy.sh`
- Complete automation in one command

**Usage:**
```bash
./build-and-deploy.sh
# OR
wurm-full
```

**Output:**
```
======================================================================
🤖 WurmModLoader: Full Build & Deploy Automation
======================================================================

[1/2] Building project...

[Full build output...]

✅ Build completed successfully!

======================================================================

[2/2] Deploying to server...

[Full deploy output...]

======================================================================
🎉 Full Automation Complete!
======================================================================

✓ Built successfully
✓ Deployed to server

Your mods are ready to test!
```

---

## 🔧 Bash Aliases

Added to `~/.bash_aliases`:

```bash
wurm-build    # Build only
wurm-deploy   # Deploy only
wurm-full     # Build + deploy
wurm-cd       # cd to WurmModLoader directory
```

**Activate aliases:**
```bash
source ~/.bashrc
# OR
source ~/.bash_aliases
```

**Use from anywhere:**
```bash
# From any directory:
wurm-build         # Builds the project
wurm-deploy        # Deploys to server
wurm-full          # Does both!
```

---

## 📁 What Gets Deployed

### Framework JARs (to server root)
```
Wurm Unlimited Dedicated Server/
├── wurmmodloader-core-0.9.1.jar
├── wurmmodloader-api-0.9.1.jar
├── wurmmodloader-modsupport-0.9.1.jar
├── wurmmodloader-legacy-0.9.1.jar
├── javassist.jar
├── gson.jar
└── snakeyaml-2.2.jar
```

### Mods (to server/mods/)
```
Wurm Unlimited Dedicated Server/mods/
├── powerscaling/
│   └── powerscaling.jar
├── powerscaling.properties
├── upgradetree/
│   └── upgradetree.jar
└── upgradetree.properties
```

---

## 🚀 Typical Workflows

### Workflow 1: Quick Test

**After making code changes:**
```bash
wurm-full
```

**Result:**
- Builds everything
- Deploys to server
- Ready to test immediately

---

### Workflow 2: Build Only

**When you want to build but not deploy yet:**
```bash
wurm-build
```

**Then later:**
```bash
wurm-deploy
```

---

### Workflow 3: Deploy Only

**If build artifacts already exist:**
```bash
wurm-deploy
```

Useful when:
- You just changed a `.properties` file
- You want to redeploy without rebuilding
- You're testing deployment scripts

---

### Workflow 4: Development Cycle

**Typical development loop:**

```bash
# 1. Make code changes
vim mods/powerscaling/src/main/java/...

# 2. Build and deploy
wurm-full

# 3. Test on server
# ... test your changes ...

# 4. Repeat!
```

**Token-efficient workflow with automation:**
```bash
# Generate code with Qwen
echo "Create new feature X" > prompt.txt
qwen_smart_codegen.py prompt.txt --search "RelatedFeature"

# Build and deploy automatically
wurm-full

# Test!
```

---

## 💡 Smart Features

### Only Copies Changed Files

The deploy script uses `cmp` to check if files are identical:
- **Unchanged files:** Skipped (fast!)
- **Changed files:** Copied
- **New files:** Always copied

**Benefits:**
- Fast deployment (seconds, not minutes)
- Minimal disk writes
- Clear visibility of what changed

---

### Handles Missing Builds Gracefully

If a mod isn't built:
```
⚠  Mod: soulboundgear - JAR not built (run ./build.sh)
```

**Not an error!** The script continues and deploys what IS built.

---

### Detailed Summary

Always shows:
- ✓ How many files copied
- ⊙ How many files unchanged
- ✗ Any errors encountered

**Example:**
```
✓ Copied:    3 files
⊙ Unchanged: 8 files
```

**Meaning:** Only 3 files changed, saved time by not copying 8 unchanged files!

---

## 🔄 Integration with Development Tools

### With Code Index

After deployment, regenerate index:
```bash
wurm-full && python3 index_code_index.py
```

### With Git

Commit and deploy:
```bash
git add . && git commit -m "Add feature X" && wurm-full
```

### With Qwen Automation

Generate, build, deploy:
```bash
qwen_smart_codegen.py prompt.txt --search "Feature" && wurm-full
```

---

## 📊 Performance

### Build Time
- Clean build: ~30-60 seconds (depending on changes)
- Incremental build: ~10-20 seconds

### Deploy Time
- First deploy: ~2-3 seconds (copying all files)
- Subsequent deploys: ~0.5-1 second (only changed files)

### Full Workflow
- Code change → deployed: **< 1 minute**

**Before automation:**
- Manual gradle commands
- Manual file copying
- Manual verification
- **Time:** 5-10 minutes

**With automation:**
- One command: `wurm-full`
- **Time:** < 1 minute

**90% time savings!**

---

## 🐛 Troubleshooting

### "Distribution ZIP not found"

**Problem:** Build didn't complete or ZIP wasn't created

**Solution:**
```bash
wurm-build  # Build first
wurm-deploy # Then deploy
```

---

### "Server directory not found"

**Problem:** Server path is wrong in deploy.sh

**Solution:**
Edit `deploy.sh` line 19:
```bash
SERVER_DIR="/your/actual/server/path"
```

---

### Files Not Updating

**Problem:** Server is caching old JARs

**Solution:**
1. Stop server
2. Run `wurm-deploy`
3. Start server

---

### Permissions Error

**Problem:** Can't write to server directory

**Solution:**
```bash
chmod -R u+w "$SERVER_DIR"
```

---

## 🎓 Best Practices

### 1. Always Use `wurm-full`

**Unless** you specifically need to:
- Build without deploying
- Deploy without building

**Why:** One command, complete automation, fewer errors

### 2. Check the Summary

Always read the deployment summary:
```
✓ Copied:    5 files
⊙ Unchanged: 3 files
```

**If nothing copied when you expected changes:**
- Check if build succeeded
- Verify you edited the right files

### 3. Regenerate Index After Major Changes

```bash
wurm-full && python3 index_code_index.py
```

Keeps your code index current for exploration and Qwen generation.

---

## 🔗 Related Tools

**Build & Deploy:**
- `build.sh` - This tool
- `deploy.sh` - This tool
- `build-and-deploy.sh` - This tool

**Code Index:**
- `index_code_index.py` - Index framework code
- `combined_index_query.py` - Query both indexes

**Code Generation:**
- `qwen_smart_codegen.py` - Generate code with smart context

**Server:**
- MCP server (port 8090) - Automation endpoints

---

## 📞 Quick Reference

```bash
# Build Commands
./build.sh                    # Build project
wurm-build                    # Build from anywhere

# Deploy Commands
./deploy.sh                   # Deploy to server
wurm-deploy                   # Deploy from anywhere

# Full Automation
./build-and-deploy.sh         # Build + deploy
wurm-full                     # Full automation from anywhere

# Navigation
wurm-cd                       # Go to WurmModLoader directory

# Combo Workflow
wurm-full && python3 index_code_index.py   # Build, deploy, reindex
```

---

## 🎉 Summary

**What you have:**
✅ Automated build script with clear output
✅ Smart deployment (only copies changed files)
✅ Full automation (build + deploy in one command)
✅ Bash aliases for easy access
✅ Detailed summaries and error reporting

**What changed:**
- Before: 5-10 minutes manual work
- After: < 1 minute with one command

**Integration:**
- Works with code index system
- Compatible with Qwen automation
- Fits into git workflows
- MCP automation ready

**Result:**
🚀 **Development cycle: code → deploy → test in under 1 minute!**

The complete ModLoader development pipeline is now fully automated.

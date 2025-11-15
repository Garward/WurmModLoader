# 🧭 Environment Map

## Primary Paths
- **/home/garward/Scripts/Games/WurmUnlimited/**
  Main Wurm development directory.
- **WurmModLoader/**
  Core modloader Git repo (Gradle multi-project).
- **WurmModLoader-CommunityMods/**
  Project to modernize 30+ open-source mods to the new event API.
- **ModSources/**
  Raw cloned source trees from GitHub or community repos.
- **PowerFantasy/**
  Contains decompiled Wurm server JAR, reference docs, and long-term server plan.

## Build & Deploy Routine
1. `./gradlew clean build dist` inside `WurmModLoader`.
2. Copy build output **(minus /mods)** to  
   `/home/garward/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server`.
3. Delete `server.jar`, rename `server.jar.bak` → `server.jar`.
4. Run `patcher.sh` to apply loader.
5. Launch for testing: `./WurmServerLauncher-patched start=Adventure`.

**Note:** Mod updates alone do not require loader rebuilds.

## Coding Discipline
- Clear separation of concerns.
- Plan → research → audit → modularize → reuse existing logic.
- Bytecode/Javassist strictly confined to WurmModloader core.
- If Hook does not exist in modloader that is used in a mod we want to add it as a new api event

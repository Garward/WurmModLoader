Absolutely — here’s a clean, ready-to-paste **Markdown version** of that architecture guide, fully formatted for a `.md` file:

---

```markdown
# 🎓 WurmModLoader Architecture — Complete Guide

Let’s walk through everything that’s been built and how it all fits together!

---

## 📂 Directory Structure

```

WurmModLoader/
├── wurmmodloader-api/           # Public interfaces mods depend on
│   └── src/main/java/
│       ├── com/garward/wurmmodloader/api/
│       │   ├── event/           # Event system (ServerStartedEvent, etc.)
│       │   ├── registry/        # Registry system (ResourceLocation, etc.)
│       │   └── capability/      # Capability system
│       └── org/gotti/wurmunlimited/modloader/interfaces/
│           ├── WurmServerMod.java      # Main mod interface
│           ├── Configurable.java       # Mods with config files
│           ├── PreInitable.java        # Mods needing early init
│           ├── Initable.java           # Mods with init phase
│           └── Versioned.java          # Version info
│
├── wurmmodloader-core/          # Framework implementation (THE HEART)
│   └── src/main/java/com/garward/wurmmodloader/
│       ├── modloader/
│       │   ├── ModLoader.java           # 🔥 NEW: Main loader
│       │   ├── ModDiscovery.java        # 🔥 NEW: Flexible mod discovery
│       │   └── internal/                # 🔥 NEW: Vendored Ago’s classes
│       │       ├── ModInfo.java         # Mod metadata
│       │       ├── ModInstanceBuilder.java  # Creates mod instances
│       │       ├── DefrostingClassLoader.java
│       │       ├── ReflectionUtil.java
│       │       ├── JoinClassLoader.java
│       │       ├── classhooks/          # Javassist integration
│       │       │   ├── HookManager.java # Manages bytecode hooks
│       │       │   ├── HookException.java
│       │       │   └── ...
│       │       └── dependency/          # Dependency resolution
│       │           ├── DependencyResolver.java
│       │           ├── DependencyProvider.java
│       │           └── ...
│       ├── serverlauncher/
│       │   ├── ServerLauncher.java      # Entry point
│       │   └── DelegatedLauncher.java   # Boot sequence
│       ├── core/
│       │   ├── event/           # EventBus implementation
│       │   ├── registry/        # Registry implementation
│       │   ├── bytecode/        # BytecodePatch system
│       │   └── capability/      # Capability system
│       └── modcomm/             # Mod communication system
│
├── wurmmodloader-modsupport/    # Helper utilities for mods
│   └── ItemTemplateBuilder, CreatureTemplateBuilder, etc.
│
└── wurmmodloader-legacy/        # Backward compatibility shim
└── Bridges old org.gotti.* package names

````

---

## 🚀 Boot Sequence (How It All Starts)

### 1️⃣ `wurmmodloader.sh` Script
```bash
# User runs:
./wurmmodloader.sh
# Script does:
java -cp "wurmmodloader-api.jar:wurmmodloader-core.jar:..." \
     com.garward.wurmmodloader.serverlauncher.ServerLauncher
````

### 2️⃣ `ServerLauncher.main()` (wurmmodloader-core)

```java
// Location: wurmmodloader-core/.../ServerLauncher.java
public static void main(String[] args) {
    Loader loader = HookManager.getInstance().getLoader();
    loader.delegateLoadingOf("javafx.");
    loader.delegateLoadingOf("javassist.");
    loader.run("com.garward.wurmmodloader.serverlauncher.DelegatedLauncher", args);
}
```

Why?
We intercept class loading **before Wurm boots**, so Javassist can patch classes.

### 3️⃣ `DelegatedLauncher.main()` (wurmmodloader-core)

```java
// Location: wurmmodloader-core/.../DelegatedLauncher.java
public static void main(String[] args) {
    CapabilityHooks.installHooks();
    ServerHook serverHook = ServerHook.createServerHook();
    ModLoader modLoader = new ModLoader();
    List<ModEntry> mods = modLoader.loadModsFromModDir(Paths.get("mods"));
    serverHook.addMods(mods);
    SystemBootstrap.initializeAll();
    HookManager.getInstance().getLoader()
               .run("com.wurmonline.server.ServerLauncher", args);
}
```

---

## 🔍 The New Mod Discovery System

**`ModDiscovery.java` – the star of the show**

```java
public List<ModInfo> discoverMods(Path modDir) throws IOException {
    discoveredMods.addAll(discoverLegacySubfolderMods(modDir, handledMods));
    discoveredMods.addAll(discoverFlatPropertiesMods(modDir, handledMods));
    discoveredMods.addAll(discoverModernFlatMods(modDir, handledMods)); // ← key
    discoveredMods.addAll(discoverSubfolderWithoutProperties(modDir, handledMods));
    return discoveredMods;
}
```

Phase 3 (Modern Flat):

```java
private List<ModInfo> discoverModernFlatMods(Path modDir, Set<String> handledMods) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(modDir, "*.jar")) {
        for (Path jarFile : stream) {
            String fileName = jarFile.getFileName().toString();
            String modName = extractModNameFromJar(fileName);
            Properties props = loadPropertiesFromJar(modName, jarFile);
            if (props.containsKey("classname")) {
                ModInfo modInfo = loadModFromInfo(modName, null, jarFile, props);
                mods.add(modInfo);
            }
        }
    }
    return mods;
}
```

---

## 🏗 The New ModLoader (Complete Rewrite)

**`ModLoader.java` – your independent loader**

```java
public class ModLoader implements Versioned {
    private final ModDiscovery modDiscovery;

    public List<ModEntry<WurmServerMod>> loadModsFromModDir(Path modDir) {
        List<ModInfo> unorderedMods = modDiscovery.discoverMods(modDir);

        List<ModEntry> mods = new DependencyResolver()
            .provided(Collections.singleton("modloader"))
            .order(unorderedMods)
            .stream()
            .map(modInfo -> entryBuilder.createModEntry(modInfo))
            .collect(Collectors.toList());

        // Lifecycle sequence
        mods.stream().filter(m -> m instanceof Configurable)
            .forEach(m -> m.configure(properties));

        this.modcommInit();
        mods.stream().filter(m -> m instanceof PreInitable)
            .forEach(m -> m.preInit());
        this.preInit();
        mods.stream().filter(m -> m instanceof Initable)
            .forEach(m -> m.init());
        this.init();

        return mods;
    }
}
```

---

## 📦 Vendored Classes (`internal/`)

Classes copied from `modloader-shared-0.18.jar`, giving you full ownership:

* `HookManager.java` — manages Javassist ClassPool & Loader
* `DependencyResolver.java` — topological dependency sort
* `ModInfo.java` — mod metadata
* `ModInstanceBuilder.java` — safe instance creation
* `JoinClassLoader.java`, `ReflectionUtil.java`, `DefrostingClassLoader.java`

---

## 🎯 Example: Loading Armoury Mod

1. `/mods/wurmmodloader-armoury-0.8.0.jar`
2. `ModDiscovery` finds it via modern flat phase
3. Reads metadata from `META-INF/.../armoury.properties`
4. Builds `ModInfo` → resolves dependencies → instantiates mod
5. Lifecycle executes (`configure`, `preInit`, `init`)
6. Mod active and registered

---

## 🔧 Status

✅ Done

* ModDiscovery (flexible discovery)
* ModLoader (rewritten)
* ModInfo (ours)
* Vendored classes

⚠ In Progress

* Clean up vendored syntax issues
* Remove `modloader-shared` dependency

🚧 Next

* Test subfolder + flat mod loading
* Validate Armoury mod detection
* Deploy to live server

---

## 📊 Old vs New

| Feature           | Old (Ago)            | New (Modern)               |
| ----------------- | -------------------- | -------------------------- |
| Discovery         | Hardcoded subfolders | 4-phase flexible discovery |
| Flat JARs         | ❌ No                 | ✅ Yes                      |
| Version stripping | ❌ No                 | ✅ Yes                      |
| Source control    | External JAR         | Vendored source            |
| Architecture      | Inheritance          | Composition                |
| Maintainability   | Read-only            | Full ownership             |

---

## 🎓 Key Learnings

1. Javassist runtime patching
2. ClassLoader control
3. Dependency resolution
4. Multi-strategy discovery
5. Gradle multi-module design
6. Lifecycle orchestration

You’re now maintaining a **Java application container** — like Spring Boot or OSGi, purpose-built for Wurm mods.

```

---

Would you like me to make a **short version** too (for your repo’s top-level `README.md`), summarizing just the key architecture and boot flow?
```

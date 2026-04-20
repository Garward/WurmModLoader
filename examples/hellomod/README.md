# HelloMod

The smallest possible WurmModLoader mod. One class, one event handler, no
`com.wurmonline.*` imports. Prints a banner to the server log when the server
finishes starting.

Use this as a copy-and-rename starting point. For a full-featured tutorial mod
covering items, capabilities, combat hooks, and crafting recipes, see
[`../oversizedclub/`](../oversizedclub/).

## Files

```
hellomod/
├── build.gradle.kts                            # Gradle build (compileOnly Wurm jars)
├── src/dist/hellomod.properties                # Mod descriptor (classname=…)
└── src/main/java/.../HelloMod.java             # The mod itself
```

## What you'll see in the log

After `./build-and-deploy.sh` and starting the server:

```
INFO: ===========================================
INFO:  HelloMod loaded — WurmModLoader is alive.
INFO: ===========================================
```

If you don't see those lines, see [troubleshooting](../../docs/guides/) (when
written) or check `wurmlog --since-last-restart --errors`.

# WurmModLoader Codebase Cheatsheet
Generated: Sun Nov 16 02:01:32 AM EST 2025

## Overview
{
  "total_files": 615,
  "total_classes": 1421,
  "total_methods": 4688,
  "last_build": "2025-11-16T01:57:08.263661",
  "priority_distribution": {
    "medium": 421,
    "high": 185,
    "low": 9
  },
  "top_packages": {
    "com.garward.wurmmodloader.modsupport": 139,
    "com.garward.wurmmodloader.core": 117,
    "com.garward.wurmmodloader.api": 89,
    "org.gotti.wurmunlimited.modsupport": 62,
    "com.garward.wurmmodloader.mods": 53,
    "com.garward.wurmmodloader.modloader": 48,
    "org.gotti.wurmunlimited.modloader": 27,
    "org.gotti.wurmunlimited.modcomm": 26,
    "com.garward.wurmmodloader.modcomm": 15,
    "com.garward.wurmmodloader.serverlauncher": 7,
    "com.garward.wurmmodloader.config": 7,
    "com.wurmonline.server.spells": 4,
    "com.wurmonline.server.intra": 3,
    "com.wurmonline.server.questions": 3,
    "org.gotti.wurmunlimited.serverlauncher": 3
  },
  "top_tags": {
    "system": 1368,
    "accessor": 1189,
    "static": 876,
    "override": 783,
    "mutator": 314,
    "creature": 279,
    "event": 198,
    "combat": 164,
    "item": 162,
    "initialization": 125,
    "handler": 94,
    "event_handler": 91,
    "skill": 78,
    "bytecode_modification": 70,
    "hook": 55,
    "deprecated": 30,
    "configuration": 27,
    "magic": 21,
    "abstract": 21,
    "final": 5
  }
}

---

## High Priority Files (Top 20)
[
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/server/ServerHook.java",
    "package": "com.garward.wurmmodloader.modloader.server",
    "classes": [
      "ServerHook",
      "EventCounter",
      "CombatAttackResult",
      "SpecialMoveResult"
    ],
    "method_count": 81,
    "key_methods": [
      "shouldLog: method",
      "getCountAndReset: getter",
      "ServerHook: method",
      "addMods: method",
      "formatVersion: event_handler"
    ]
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/server/ProxyServerHook.java",
    "package": "com.garward.wurmmodloader.modloader.server",
    "classes": [
      "ProxyServerHook",
      "is",
      "OpportunityContext"
    ],
    "method_count": 59,
    "key_methods": [
      "ProxyServerHook: method",
      "registerPlayerHooks: override",
      "communicatorMessageHook: override",
      "getInstance: static_method",
      "communicatorChannelHook: static_method"
    ]
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/CoreBytecodePatches.java",
    "package": "com.garward.wurmmodloader.core.bytecode",
    "classes": [
      "CoreBytecodePatches"
    ],
    "method_count": 50,
    "key_methods": [
      "RemoveActionsFinalModifierPatch: method",
      "ItemEnchantmentConstructorPatch: method",
      "ServerStartPatch: method",
      "ServerShutdownPatch: method",
      "ItemTemplatesCreatedPatch: method"
    ]
  },
  {
    "file": "wurmmodloader-api/src/test/java/com/garward/wurmmodloader/api/registry/RegisterEventTest.java",
    "package": "com.garward.wurmmodloader.api.registry",
    "classes": [
      "RegisterEventTest",
      "functionality",
      "for",
      "TestObject",
      "private",
      "TestRegistry"
    ],
    "method_count": 30,
    "key_methods": [
      "setUp: setter",
      "testConstructor: method",
      "testConstructorNullRegistry: method",
      "testGetRegistry: method",
      "testGetRegistryName: method"
    ]
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/testing/eventsim/EventSimulator.java",
    "package": "com.garward.wurmmodloader.core.testing.eventsim",
    "classes": [
      "EventSimulator",
      "not",
      "is"
    ],
    "method_count": 28,
    "key_methods": [
      "EventSimulator: method",
      "runSimulation: method",
      "runNormalMode: method",
      "runRareMode: method",
      "runFullSweepMode: method"
    ]
  },
  {
    "file": "wurmmodloader-core/src/test/java/com/garward/wurmmodloader/modloader/classhooks/HookManagerTest.java",
    "package": "com.garward.wurmmodloader.modloader.classhooks",
    "classes": [
      "HookManagerTest",
      "TestClass"
    ],
    "method_count": 26,
    "key_methods": [
      "staticMethod: static_method",
      "staticPrivateMethod: static_method",
      "method: method",
      "privateMethod: method",
      "voidMethod: method"
    ]
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/eventlogic/materials/MaterialProfile.java",
    "package": "com.garward.wurmmodloader.core.eventlogic.materials",
    "classes": [
      "MaterialProfile",
      "Builder"
    ],
    "method_count": 21,
    "key_methods": [
      "MaterialProfile: method",
      "getMaterialId: getter",
      "damageModifier: method",
      "asOptional: method",
      "decayModifier: method"
    ]
  },
... (2666 more lines truncated)

---

## Event System
[
  {
    "file": "docs/examples/ui-api/SoulboundGearUIExample.java",
    "name": "onServerStarted",
    "signature": "onServerStarted(ServerStartedEvent event)",
    "type": "event_handler",
    "tags": [
      "event_handler"
    ],
    "javadoc": ""
  },
  {
    "file": "docs/examples/ui-api/PowerScalingUIExample.java",
    "name": "onServerStarted",
    "signature": "onServerStarted(ServerStartedEvent event)",
    "type": "event_handler",
    "tags": [
      "event_handler"
    ],
    "javadoc": "Registers the Power Stats menu entry.\nCall this from your mod's onServerStarted() event handler."
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/callbacks/Callbacks.java",
    "name": "CallbackInfo",
    "signature": "CallbackInfo(Supplier<Object> callbackBuilder)",
    "type": "method",
    "tags": [
      "hook"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/callbacks/Callbacks.java",
    "name": "Callbacks",
    "signature": "Callbacks(Loader loader, ClassPool classPool)",
    "type": "method",
    "tags": [
      "hook"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/callbacks/Callbacks.java",
    "name": "addCallback",
    "signature": "addCallback(CtClass targetClass, String callbackName, Object callbackTarget)",
    "type": "method",
    "tags": [
      "hook"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookException.java",
    "name": "HookException",
    "signature": "HookException(String message)",
    "type": "method",
    "tags": [
      "hook"
    ],
    "javadoc": ""
... (3570 more lines truncated)

---

## Bytecode Patches
[
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/debug/DiagnosticServer.java",
    "name": "printPatch",
    "signature": "printPatch(int index, BytecodePatch patch)",
    "type": "static_method",
    "tags": [
      "bytecode_modification",
      "static"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/registry/SystemBootstrap.java",
    "name": "PatchManager",
    "signature": "PatchManager()",
    "type": "method",
    "tags": [
      "bytecode_modification"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/PatchSettings.java",
    "name": "PatchSettings",
    "signature": "PatchSettings()",
    "type": "method",
    "tags": [
      "mutator",
      "configuration",
      "bytecode_modification"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/PatchManager.java",
    "name": "PatchManager",
    "signature": "PatchManager()",
    "type": "method",
    "tags": [
      "bytecode_modification"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/PatchManager.java",
    "name": "applyPatch",
    "signature": "applyPatch(BytecodePatch patch)",
    "type": "method",
    "tags": [
      "bytecode_modification"
    ],
    "javadoc": ""
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/CoreBytecodePatches.java",
    "name": "RemoveActionsFinalModifierPatch",
    "signature": "RemoveActionsFinalModifierPatch()",
    "type": "method",
    "tags": [
... (688 more lines truncated)

---

## API Overview
{
  "module": "wurmmodloader-api",
  "file_count": 109,
  "total_classes": 215,
  "total_methods": 654,
  "files": [
    {
      "file": "src/test/java/com/garward/wurmmodloader/api/registry/RegisterEventTest.java",
      "classes": [
        "RegisterEventTest",
        "functionality",
        "for",
        "TestObject",
        "private",
        "TestRegistry"
      ],
      "method_count": 30
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/ui/UIWindowBuilder.java",
      "classes": [
        "UIWindowBuilder",
        "BMLElement",
        "TextElement",
        "ButtonElement",
        "InputElement",
        "DropdownElement",
        "RadioElement",
        "SimpleUIWindow"
      ],
      "method_count": 18
    },
    {
      "file": "src/test/java/com/garward/wurmmodloader/api/registry/ResourceLocationTest.java",
      "classes": [
        "ResourceLocationTest"
      ],
      "method_count": 18
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/events/vehicle/VehicleSpeedCalculationEvent.java",
      "classes": [
        "VehicleSpeedCalculationEvent"
      ],
      "method_count": 15
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/icon/Icon.java",
      "classes": [
        "provides",
        "is",
        "Icon"
      ],
      "method_count": 14
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/ui/MenuEntry.java",
      "classes": [
        "MenuEntry",
        "MenuEntryBuilder",
        "SimpleMenuEntry"
      ],
      "method_count": 14
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/events/ModActionEvent.java",
      "classes": [
        "ModActionEvent"
      ],
      "method_count": 14
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/events/vehicle/VehicleMountEvent.java",
      "classes": [
        "VehicleMountEvent"
      ],
      "method_count": 13
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/events/ModQueryEvent.java",
      "classes": [
        "ModQueryEvent"
      ],
      "method_count": 12
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/events/skill/SkillDifficultyEvent.java",
      "classes": [
        "SkillDifficultyEvent"
      ],
      "method_count": 12
    },
    {
      "file": "src/main/java/com/garward/wurmmodloader/api/events/deity/DeityDbLoadEvent.java",
      "classes": [
        "DeityDbLoadEvent"
      ],
      "method_count": 11
    },
    {
... (777 more lines truncated)

---

## Core Hooks
[
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookException.java",
    "type": "method",
    "name": "HookException",
    "signature": "HookException(String message)",
    "method_type": "method",
    "javadoc": "",
    "tags": [
      "hook"
    ],
    "line": 7
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookException.java",
    "type": "class",
    "name": "HookException",
    "package": "com.garward.wurmmodloader.modloader.internal.classhooks"
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookManager.java",
    "type": "method",
    "name": "HookManager",
    "signature": "HookManager()",
    "method_type": "method",
    "javadoc": "",
    "tags": [
      "hook"
    ],
    "line": 49
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookManager.java",
    "type": "method",
    "name": "createHook",
    "signature": "createHook(CtClass ctClass, ClassHook classHook)",
    "method_type": "method",
    "javadoc": "",
    "tags": [
      "hook"
    ],
    "line": 191
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookManager.java",
    "type": "method",
    "name": "registerHook",
    "signature": "registerHook(String className, String methodName, String methodType, InvocationHandler invocationHandler)",
    "method_type": "method",
    "javadoc": "Register a hook.\n@param className\n           Class name to hook",
    "tags": [
      "deprecated",
      "hook"
    ],
    "line": 307
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookManager.java",
    "type": "class",
    "name": "HookManager",
    "package": "com.garward.wurmmodloader.modloader.internal.classhooks"
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/HookManager.java",
    "type": "class",
    "name": "hook",
    "package": "com.garward.wurmmodloader.modloader.internal.classhooks"
  },
  {
    "file": "wurmmodloader-core/src/main/java/com/garward/wurmmodloader/modloader/internal/classhooks/ClassHook.java",
    "type": "method",
    "name": "ClassHook",
    "signature": "ClassHook(String methodName, String methodType, InvocationHandlerFactory invocationHandlerFactory)",
    "method_type": "method",
    "javadoc": "",
    "tags": [
      "hook"
    ],
    "line": 7
  },
... (667 more lines truncated)

# WurmModLoader Public API Surface Area

**Version**: 1.0.0-SNAPSHOT
**Date**: 2025-11-03
**Status**: Defined - Phase 3

## Overview

This document defines the **public API surface area** of WurmModLoader. Classes and interfaces listed here are considered part of the stable public API and follow semantic versioning guarantees. Breaking changes to these APIs will only occur in major version updates.

## API Stability Guarantees

### Stable API (1.0.0+)
- **Binary Compatibility**: Classes and methods will not be removed or have signatures changed
- **Behavioral Compatibility**: Documented behavior will not change in incompatible ways
- **Deprecation Policy**: Features will be deprecated for at least one minor version before removal

### Experimental API
- Marked with `@Experimental` annotation
- May change or be removed in minor versions
- Use at your own risk

### Internal API
- Not documented here
- Package-private or marked with `@Internal`
- May change without notice

## Core API Module (wurmmodloader-api)

### Package: `com.garward.wurmmodloader.modloader`

**Classes:**
- `SimpleMod` - Base implementation of WurmServerMod with no-op methods

### Package: `com.garward.wurmmodloader.modloader.interfaces`

**Core Interfaces:**
- `WurmServerMod` - Primary interface that all mods must implement
  - Methods: `init()`, `preInit()`
  - Extends: `org.gotti.wurmunlimited.modloader.interfaces.Versioned`

- `WurmMod` - Alternative base interface for mods

**Lifecycle Listener Interfaces:**
- `ServerStartedListener` - Called when server finishes starting
  - Methods: `onServerStarted()`

- `ServerShutdownListener` - Called when server is shutting down
  - Methods: `onServerShutdown()`

- `ServerPollListener` - Called on each server tick
  - Methods: `onServerPoll()`

**Content Creation Listener Interfaces:**
- `ItemTemplatesCreatedListener` - Called after item templates are loaded
  - Methods: `onItemTemplatesCreated()`

**Communication Listener Interfaces:**
- `PlayerLoginListener` - Called when a player logs in
  - Methods: `onPlayerLogin(Creature)`

- `PlayerMessageListener` - Called when player sends a message
  - Methods: `handleMessage(Creature, String)`, `getMessagePolicy()`

- `ChannelMessageListener` - Called for channel messages
  - Methods: `handleMessage(Creature, String, String)`, `getMessagePolicy()`

**Supporting Interfaces:**
- `MessagePolicy` - Enum defining message handling policy
  - Values: `ALLOW`, `SUPPRESS`, `OVERRIDE`

## ModSupport Module (wurmmodloader-modsupport)

### Package: `com.garward.wurmmodloader.modsupport`

**Core Utility Classes:**
- `IdFactory` - Manages unique ID generation for templates
  - Methods: ID allocation, persistence, retrieval

- `IdType` - Enum of ID types (ITEMTEMPLATE, CREATURETEMPLATE, etc.)

- `IIdType` - Interface for custom ID types

**Template Builders:**
- `ItemTemplateBuilder` - Fluent builder for creating item templates
  - Builder pattern for item properties
  - Material, behavior, size, weight configuration

- `CreatureTemplateBuilder` - Fluent builder for creating creature templates
  - Builder pattern for creature properties
  - Skills, combat, alignment configuration

**Database Support:**
- `ModSupportDb` - Database utilities for mod support operations

**Parsers:**
- `NamedIdParser` - Parses named IDs from configuration
- `NonFreezingNamedIdParser` - Parser variant that doesn't freeze IDs

### Package: `com.garward.wurmmodloader.modsupport.actions`

**Action System Core:**
- `ModActions` - Central registry for custom actions
  - Methods: `init()`, `registerAction()`, `registerActionPerformer()`

- `ModAction` - Decorator for ActionEntry providing extended functionality

- `ActionEntryBuilder` - Fluent builder for ActionEntry objects

**Action Performers:**
- `ActionPerformer` - Interface for action implementations
  - Methods: `action()`, `getActionId()`

- `ActionPerformerBase` - Base class implementing ActionPerformer

- `ActionPerformerChain` - Chains multiple action performers

**Behavior System:**
- `BehaviourProvider` - Interface for providing custom behaviors

- `ChainedBehaviourProvider` - Chains multiple behavior providers

- `WrappedBehaviour` - Wraps existing Wurm behaviors

- `WrappedBehaviourProvider` - Provides wrapped behaviors

**Supporting Types:**
- `ActionPropagation` - Enum controlling action propagation (FINISH_ACTION, SERVER_PROPAGATION, etc.)
- `ActionPerformerBehaviour` - Links ActionPerformer with BehaviourProvider

### Package: `com.garward.wurmmodloader.modsupport.items`

**Item Management:**
- `ModItems` - Utilities for managing custom items

- `ItemIdParser` - Parses item IDs from configuration

- `ModelNameProvider` - Interface for providing model names

### Package: `com.garward.wurmmodloader.modsupport.creatures`

**Creature Management:**
- `ModCreatures` - Utilities for managing custom creatures

- `ModCreature` - Represents a custom creature definition

- `CreatureTemplateParser` - Parses creature templates from files

- `CreatureTypesParser` - Parses creature type definitions

**Traits System:**
- `ModTraits` - Manages custom creature traits

- `TraitsSetter` - Utility for setting creature traits

**Encounter System:**
- `EncounterBuilder` - Fluent builder for creature encounters

### Package: `com.garward.wurmmodloader.modsupport.questions`

**Question/Dialog System:**
- `ModQuestions` - Utilities for creating custom player dialogs
  - Methods: `createQuestion()`, BML helper methods

- `ModQuestion` - Interface for implementing custom questions
  - Methods: `answer()`, `sendQuestion()`, `sendBml()`

### Package: `com.garward.wurmmodloader.modsupport.vehicles`

**Vehicle System:**
- `ModVehicleBehaviours` - Registry for custom vehicle behaviors

- `ModVehicleBehaviour` - Interface for vehicle behavior implementations

- `VehicleFacade` - Interface for vehicle operations

- `VehicleFacadeImpl` - Implementation of VehicleFacade

### Package: `com.garward.wurmmodloader.modsupport.bml`

**BML (Binary Markup Language) Utilities:**
- `BmlBuilder` - Fluent builder for BML content
  - Methods for creating UI elements (buttons, text, inputs, tables)

- `BmlNodeBuilder` - Builder for individual BML nodes

- `TextStyle` - Enum for text styling (BOLD, ITALIC, UNDERLINE, etc.)

### Package: `com.garward.wurmmodloader.modsupport.properties`

**Player Properties:**
- `ModPlayerProperties` - Custom player property management

- `Property` - Represents a custom property

## Integration Points (com.wurmonline.server.*)

These classes live in `com.wurmonline.server` packages but are part of WurmModLoader's public API for integrating with Wurm server:

### Package: `com.wurmonline.server.questions`
- `ModQuestionImpl` - Legacy wrapper for ModQuestion integration

### Package: `com.wurmonline.server.intra`
- `ModIntraServerMessage` - Legacy wrapper for intra-server messaging

### Package: `com.wurmonline.server.combat`
- `CombatEngine` - Combat system hooks

### Package: `com.wurmonline.server.spells`
- `ModSpell` - Custom spell interface
- `ModSpells` - Custom spell registry

## External Dependencies (Stable)

WurmModLoader depends on these external stable APIs:

### From modloader-shared (org.gotti.wurmunlimited)
- `org.gotti.wurmunlimited.modloader.interfaces.Versioned`
- `org.gotti.wurmunlimited.modloader.interfaces.ModEntry`
- `org.gotti.wurmunlimited.modloader.ModLoaderShared`
- `org.gotti.wurmunlimited.modloader.classhooks.*`
- `org.gotti.wurmunlimited.modloader.callbacks.*`

### From Wurm Unlimited
- `com.wurmonline.server.creatures.Creature`
- `com.wurmonline.server.items.Item`
- `com.wurmonline.server.questions.Question`
- `com.wurmonline.server.behaviours.Behaviour`
- `com.wurmonline.server.behaviours.ActionEntry`

## Non-Public API

The following modules are **internal implementation** and not part of the public API:

### wurmmodloader-core
- Internal mod loading implementation
- Hook management internals
- Server integration details

### wurmmodloader-patcher
- Server JAR patching utilities
- Bytecode manipulation internals

### wurmmodloader-legacy
- Backward compatibility shims
- Legacy package adapters

## API Usage Patterns

### Basic Mod Structure
```java
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modloader.interfaces.ServerStartedListener;

public class MyMod implements WurmServerMod, ServerStartedListener {
    @Override
    public void preInit() {
        // Bytecode hooks go here
    }

    @Override
    public void onServerStarted() {
        // Initialization after server starts
    }
}
```

### Creating Custom Items
```java
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import com.garward.wurmmodloader.modsupport.IdFactory;
import com.garward.wurmmodloader.modsupport.IdType;

ItemTemplateBuilder builder = new ItemTemplateBuilder("mymod.custom.item");
builder.name("Custom Item", "custom items", "A custom item");
builder.weightGrams(1000);
builder.build();
```

### Registering Custom Actions
```java
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.garward.wurmmodloader.modsupport.actions.ActionEntryBuilder;

ModActions.init();
int actionId = ModActions.getNextActionId();
ActionEntry entry = new ActionEntryBuilder(actionId, "Do something", "doing")
    .build();
ModActions.registerAction(entry);
```

## Versioning Strategy

WurmModLoader follows **Semantic Versioning 2.0.0**:

- **Major version** (X.0.0): Breaking API changes
- **Minor version** (1.X.0): New features, backward compatible
- **Patch version** (1.0.X): Bug fixes, backward compatible

### Current Version: 1.0.0-SNAPSHOT
- Status: Pre-release, API subject to change
- First stable release: 1.0.0 (after Phase 10)

## API Review Process

Changes to public API must:
1. Be documented in JavaDoc
2. Include usage examples
3. Be reviewed for backward compatibility
4. Be noted in CHANGELOG.md
5. Follow deprecation policy if removing features

## Contact

For questions about the public API:
- GitHub Issues: https://github.com/garward/WurmModLoader/issues
- GitHub Discussions: https://github.com/garward/WurmModLoader/discussions

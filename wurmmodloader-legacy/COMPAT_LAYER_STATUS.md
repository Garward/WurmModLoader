# Legacy Compatibility Layer — Structural Reference

Per-class strategy for how `org.gotti.wurmunlimited.*` maps onto the current `com.garward.wurmmodloader.*` implementations. For a usage overview see [`README.md`](README.md); for edge cases see [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md).

## Tier 1 — Interface extension

Old interfaces extend new ones. Zero runtime overhead, full type compat.

**Mod lifecycle:**
`WurmServerMod`, `WurmMod`, `Configurable`, `PreInitable`, `Initable`,
`ServerStartedListener`, `ServerShutdownListener`, `ServerPollListener`,
`PlayerLoginListener`, `PlayerMessageListener`, `ChannelMessageListener`,
`ItemTemplatesCreatedListener`, `MessagePolicy`.

**Support interfaces:**
`ModQuestion`, `IntraRequest`, `IChannelListener`, `ActionPerformer`,
`BehaviourProvider`, `ModAction`, `IIdType`, `Property`, `TraitsSetter`,
`ModCreature`, `ModelNameProvider`, `VehicleFacade`.

## Tier 2 — Delegating wrappers

Old class is a thin subclass/delegator routing to the new implementation.

- `ModLoader` — extends `com.garward.wurmmodloader.modloader.ModLoader`
- `ModActions`, `ModQuestions` — static delegators
- `ServerHook` — extends new implementation
- `SimpleMod` — extends new implementation

## Tier 3 — Simple extension

Subclasses that inherit everything from the new class.

**Builders:** `ItemTemplateBuilder`, `CreatureTemplateBuilder`, `EncounterBuilder`, `ActionEntryBuilder`, `BmlBuilder`.
**Parsers:** `CreatureTemplateParser`, `CreatureTypesParser`, `ItemIdParser`.
**Launcher:** `ServerLauncher`, `DelegatedLauncher`, `PatchedLauncher`.
**Behaviour:** `WrappedBehaviourProvider`, `ChainedBehaviourProvider`.

## Tier 4 — Full original implementation

Kept verbatim because Java can't extend these shapes cleanly or because old bytecode expects the class to live in this exact form.

**Enums** (can't be extended): `IdType`, `ActionPropagation`, `TextStyle`, `ModVehicleBehaviour`.

**Complex/package-private:**
`Property`, `ActionPerformerChain`, `WrappedBehaviour`, `ActionPerformerBase`,
`ActionPerformerBehaviour`, `BmlNodeBuilder`, `VehicleFacadeImpl`,
`ProxyServerHook`, `Listeners`, `NamedIdParser`, `NonFreezingNamedIdParser`.

**ModComm / ModIntraServer packet layer** (wire-compatible, must be exact):
`ModComm`, `ModCommHandler`, `ModCommConstants`, `Channel`, `PacketReader`,
`PacketWriter`, `PlayerModConnection`, `ModIntraServer`, `ModIntraServerHandler`,
`ModIntraServerConstants`, `BBHelper`, `IntraRequestHandler`,
`GetRemoteTemplatesMessage`, `ModPlayerTransfer`, `TemplateIdMapper`.

**Support classes** (original registries): `IdFactory`, `ModSupportDb`,
`ModCreatures`, `ModItems`, `ModTraits`, `ModPlayerProperties`,
`ModVehicleBehaviours`.

## Wurm-side shims

Wurm server classes instantiated with `org.gotti.*` types need thin wrappers:

- `com.wurmonline.server.questions.ModQuestionImpl` — accepts `org.gotti` `ModQuestion`
- `com.wurmonline.server.intra.ModIntraServerMessage` — accepts `org.gotti` `IntraRequest`

## Compatibility guarantees

**Fully compatible, no mod changes needed:** mod lifecycle interfaces, builder APIs, static utility classes (`ModActions`/`ModQuestions`/`ModItems`/`ModCreatures`), `ModLoader`, `ModComm`, `ModIntraServer`.

**Not compatible:** direct enum casts between `org.gotti.*` and `com.garward.*`, reflective package walks that assume one package holds everything. See `KNOWN_ISSUES.md`.

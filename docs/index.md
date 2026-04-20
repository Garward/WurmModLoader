# WurmModLoader — Server

A drop-in replacement for Ago's `WurmServerLauncher` with an annotation-driven
event bus, centralized bytecode patches, and a typed capability system.

## Quick links

- **[Getting Started](getting-started/index.md)** — onramp for new modders, 10-minute hello-mod, "I want to build…" decision tree
- **[Event Bus Guide](guides/event-bus.md)** — the `@SubscribeEvent` catalog
- **[Extending the Framework](guides/extending-framework.md)** — adding new events + bytecode patches
- **[Porting Existing Mods](migration/porting-existing-mods.md)** — bringing Ago-era mods into the modern framework
- **[Troubleshooting](guides/troubleshooting.md)** — when it breaks
- **[API Surface](reference/api-surface.md)** — public API reference

## Coming from Ago-era modding?

If your last Wurm mod implemented `Configurable` / `PreInitable` / `Initable`
and used Gotti's maven repo, read [Getting Started](getting-started/index.md#questions-you-probably-have-right-now)
first — the Q&A answers the "wait, why?" moments you'll hit on the first port.

## Client mods

Client-side counterpart (UI, HUD, input, rendering) lives in a separate repo:
[WurmModLoader-Client](https://github.com/garward/WurmModLoader-Client).

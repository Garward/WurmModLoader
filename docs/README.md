# WurmModLoader Documentation

Index of all docs for the WurmModLoader framework. For a high-level map of the project itself, see [`../Architecture.MD`](../Architecture.MD).

## Getting Started

- **[`getting-started/index.md`](getting-started/index.md)** — Modder onramp: what changed vs Ago, project layout, hello-mod in 10 minutes, system index
- [`../README.md`](../README.md) — Project overview
- [`../CONTRIBUTING.md`](../CONTRIBUTING.md) — Contribution guidelines

## Guides

Day-to-day how-tos for writing mods.

| Doc | Topic |
|---|---|
| [`guides/custom-map-setup.md`](guides/custom-map-setup.md) | **Running a custom map** — fixes the NW-corner NPC cluster; covers the world-seed bootstrap + `wurmmodloader-world-seed.yaml` |
| [`guides/event-bus.md`](guides/event-bus.md) | `@SubscribeEvent` annotation-driven event system |
| [`guides/extending-framework.md`](guides/extending-framework.md) | **Adding new events & bytecode patches to the framework** (five-file recipe) |
| [`guides/database-backend-spi.md`](guides/database-backend-spi.md) | **Database Backend SPI** — replacing SQLite/MySQL with a custom backend (Postgres, MariaDB) |
| [`guides/questions-api.md`](guides/questions-api.md) | **Server-side popup windows via `ModQuestion`** (low-level: BML + form callback) |
| [`guides/bml-ui.md`](guides/bml-ui.md) | BML (Wurm's markup language) UI construction — syntax reference |
| [`guides/ui-api.md`](guides/ui-api.md) | Full UI API walkthrough |
| [`guides/ui-api-overview.md`](guides/ui-api-overview.md) | UI API overview / entry point |
| [`guides/ui-api-submenus.md`](guides/ui-api-submenus.md) | Organizing UI submenus |
| [`guides/upgrade-tree-ui.md`](guides/upgrade-tree-ui.md) | Upgrade tree UI patterns |
| [`guides/icon-system-reference.md`](guides/icon-system-reference.md) | Icon system reference |
| [`guides/legacy-mod-compatibility.md`](guides/legacy-mod-compatibility.md) | Running Ago-era mods via the legacy bridge |
| [`guides/troubleshooting.md`](guides/troubleshooting.md) | **When it breaks** — mod-didn't-load, patch conflicts, classloader errors, runtime crashes |

## Reference

Lookup material — not tutorials.

| Doc | Topic |
|---|---|
| [`reference/api-surface.md`](reference/api-surface.md) | Public API surface summary |
| [`reference/ui-api-quick-reference.md`](reference/ui-api-quick-reference.md) | UI API cheatsheet |

## Migration

| Doc | Topic |
|---|---|
| [`migration/porting-existing-mods.md`](migration/porting-existing-mods.md) | **"It mostly just works"** — bringing an Ago-era mod here with minimal effort (repackage-only vs modernize) |
| [`migration/from-legacy.md`](migration/from-legacy.md) | Porting mods from the old listener interfaces to the event bus (full interface-by-interface guide) |

## Research & Design Notes

Working design docs and investigation notes — not necessarily current implementation, but useful context.

### Console / GM tooling
- [`research/console-gm/commands-research.md`](research/console-gm/commands-research.md)
- [`research/console-gm/commands-mvp.md`](research/console-gm/commands-mvp.md)
- [`research/console-gm/hybrid-system.md`](research/console-gm/hybrid-system.md)

### Database
- [`research/database/connection-pattern.md`](research/database/connection-pattern.md)
- [`research/database/events-plan.md`](research/database/events-plan.md)
- [`research/database/creature-optimization.md`](research/database/creature-optimization.md)
- [`research/database/village-structure.md`](research/database/village-structure.md)
- [`research/database/zone-deity-events.md`](research/database/zone-deity-events.md)

### Client modloader
- [`research/client-modloader/plan.md`](research/client-modloader/plan.md)
- [`research/client-modloader/custom-menu-progress.md`](research/client-modloader/custom-menu-progress.md)

### Creatures / NPCs
- [`research/creatures/hook-surface.md`](research/creatures/hook-surface.md) — What affects every creature, existing vs missing events, creature ID persistence, how powerscaling plugs in, per-creature tracing plan

### Combat
- [`research/combat/duskombat-formula.md`](research/combat/duskombat-formula.md) — **Full end-to-end DUSKombat damage formula.** Every stage, every multiplier, every constant, with line-number citations. Enough to build a damage calculator from scratch.
- [`research/combat/event-surface-gap.md`](research/combat/event-surface-gap.md) — **Roadmap.** What events the framework needs so no combat mod ever has to read vanilla source or write bytecode patches again.
- [`research/combat/mod-crossref.md`](research/combat/mod-crossref.md) — How Armoury and DUSKombat currently hook combat (and why each is a workaround for a missing event).
- [`research/combat/visibility-plan.md`](research/combat/visibility-plan.md) — Original tiered diagnostic-visibility plan; tactics still useful, strategy superseded by the gap doc.

### Other
- [`research/server-config.md`](research/server-config.md)

## Examples

- **[`../examples/hellomod/`](../examples/hellomod/)** — **Smallest possible mod.** One class, one event handler. Copy this as your starting skeleton.
- **[`../examples/oversizedclub/`](../examples/oversizedclub/)** — **Canonical tutorial mod.** Heavily commented, source-of-truth for how a mod is put together. Read top-to-bottom for items, capabilities, combat hooks, recipes.
- [`../examples/templatemod/`](../examples/templatemod/) — UI-focused template (context menu + questionnaire)
- [`../examples/basic-item-mod/`](../examples/basic-item-mod/), [`../examples/custom-creature/`](../examples/custom-creature/), [`../examples/action-system/`](../examples/action-system/) — Focused single-topic examples
- [`examples/COMMON_MOD_PATTERNS.md`](examples/COMMON_MOD_PATTERNS.md) — Common patterns reference
- [`examples/ui-api/`](examples/ui-api/) — UI API example Java snippets
- [`examples/eventlogic-registries/`](examples/eventlogic-registries/) — **EventLogic registry schemas.** JSON examples for `MaterialProfileRegistry`, `SwingSpeedRegistry`, `WeaponTimerRegistry`, `DualWieldRegistry` plus a one page guide to wiring them up from a mod.

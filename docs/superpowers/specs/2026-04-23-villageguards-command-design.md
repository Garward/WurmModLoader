# `#villageguards` GM Console Command — Design

**Date:** 2026-04-23
**Status:** Approved (design phase)

## Problem

Permanent deeds (`Village.isPermanent == true`, e.g. starter towns / GM-created deeds) cannot have their guard count changed through the in-game token BML interface. Server owners need a console command to adjust guard counts on these deeds without modifying game code or relying on the restricted UI.

## Goal

Add `#villageguards <deedname> <count>` to the framework's console GM command router, mirroring the conventions of the existing `#villageperm` and `#setmayor` commands.

## Non-Goals

- No relative adjustment syntax (`+N` / `-N`) — absolute set only.
- No `--force` override of the engine's max-guard clamp.
- No new framework events, hooks, or bytecode patches.
- No client-side / in-game chat command — console only.
- No changes to `GuardPlan` type (war/peace/etc.); existing plan type is preserved.

## Approach

Pure GM console command using the existing public Wurm API. `Village.plan` is a public field, and `GuardPlan.changePlan(int planType, int newGuardCount)` already handles both directions (spawning new spirit guards and deleting excess ones) and persists the change. The BML-level restriction on permanent deeds lives in the UI layer; the underlying API is unrestricted.

## Behavior

Command: `#villageguards <deedname> <count>`

1. Parse args using the same quoted-name parser as `#villageperm` / `#setmayor`.
2. Resolve the `Village` by name (reuse the lookup helper those commands use). Error if not found.
3. Read `village.plan`. If `null`, print `[Console GM] Village '<name>' has no guard plan.` and return.
4. Validate `count` is a non-negative integer.
5. Compute `int max = GuardPlan.getMaxGuards(village)` (this method already accounts for plan type — quartered for type 1, halved for type 2). Clamp `count` to `[0, max]`. If clamped, log a one-line notice showing the requested vs. effective value.
6. Capture `oldCount = village.plan.getNumHiredGuards()`.
7. Call `village.plan.changePlan(village.plan.getType(), count)`.
8. Print `[Console GM] Set guards for '<deedname>': <oldCount> -> <count>`.

### Error cases (all printed as `[Console GM] ...` like sibling commands)

- Wrong arg count → usage line.
- `count` not a non-negative integer → usage line.
- Village not found → "No village named '<deedname>'."
- `village.plan == null` → "Village '<name>' has no guard plan."

## Files Touched

- `wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/console/ConsoleGMCommandRouter.java`
  - Add `case "villageguards":` to the dispatch switch (~line 289, alongside `villageperm` / `setmayor`).
  - Add a new `handleVillageGuards(String[] args)` method modeled on `handleVillagePerm` / `handleSetMayor`.
  - Add usage line to the help block (~line 353): `  #villageguards <deedname> <count>`.
  - Add example to the example block (~line 388): `  #villageguards "Freedom Landing" 4`.

No other files require changes. No build-config changes.

## Testing

Manual via running server console:

1. **Permanent deed, hire:** `#villageguards "Freedom Landing" 4` on a starter town with 0 guards. Expect spirit guards to spawn over the next plan tick; `#villageguards "Freedom Landing" 0` removes them.
2. **Clamp behavior:** request a count above `GuardPlan.getMaxGuards(village)`; expect the notice line and the count clamped to the max.
3. **Bad args:** missing count, non-numeric count, negative count, unknown deed name — each prints the appropriate usage/error line and does not change state.
4. **Plan-type preservation:** before/after the command, `village.plan.getType()` is unchanged.

No automated tests — the existing `#villageperm` / `#setmayor` commands also have none, and this is a thin wrapper over a public Wurm API method.

## Deployment

Standard: `wurm-full` to build and deploy. Then `python3 index_code_index.py` to refresh the code index.

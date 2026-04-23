# Blueprints — DORMANT

Pivoted to `mods/gmtools/`. In-world GM build tools (instant construct/deconstruct)
are a better foundation than importing DeedPlanner `.dpl` files: once GM actions
exist, blueprint export becomes a record/replay of those actions rather than a
separate XML-parsing code path.

**Status:** scaffolded (base64 → gzip → XML parse works, logs a summary). No
placement logic. Excluded from `settings.gradle.kts`.

**To revive:** uncomment the `mods:blueprints` block in `settings.gradle.kts`
and wire placement through the same handler surface gmtools uses.

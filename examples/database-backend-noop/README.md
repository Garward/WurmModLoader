# database-backend-noop

Reference implementation of the **Database Backend SPI**. Registers a backend
named `noop` that stores each WU schema in a per-schema SQLite file under
`$TMPDIR/wurmmodloader-noop/`. Lets you verify the full SPI wiring —
selection event → registry → bootstrap event → per-schema factories →
connection wrapping → migration — **without** standing up Postgres.

Use this as the skeleton for a real backend (Postgres, MariaDB, H2, etc.).

## What this demonstrates

| SPI touch point                        | File                                                                              |
|----------------------------------------|-----------------------------------------------------------------------------------|
| `@SubscribeEvent` registration          | `NoOpBackendMod.java`                                                             |
| Reacting to `BootstrapEvent` for DDL    | `NoOpBackendMod.java` (logs only; real backends do `CREATE DATABASE`)              |
| Draining pools on `ServerStoppingEvent` | `NoOpBackendMod.java` (logs only; real backends call `pool.close()`)              |
| `DatabaseBackend` impl + dialect        | `NoOpDatabaseBackend.java`                                                        |
| `ConnectionFactory` subclass            | `com/wurmonline/server/database/NoOpConnectionFactory.java`                        |
| `MigrationStrategy` impl                | `com/wurmonline/server/database/migrations/NoOpMigrationStrategy.java`             |

## Why two classes live in `com.wurmonline.*`

Vanilla WU's `ConnectionFactory(String, WurmDatabaseSchema)` constructor is
package-private, and `MigrationResult.newSuccess(...)` / `newError(...)` are
package-private factories. Any real backend must place those two subclasses in
the matching Wurm packages. This is the one exception to the
"no `com.wurmonline.*` imports in mods" rule — it is structurally required
by WU, not a style choice.

## Porting this to Postgres

1. Swap `NoOpConnectionFactory` for one that builds a
   `jdbc:postgresql://host:5432/wurm<schema>` URL and delegates to HikariCP.
2. Flip `NoOpDatabaseBackend#getDialect()` to `Dialect.POSTGRES` so the
   framework's `SqlDialectRewriter` translates vanilla SQLite-isms
   (`INSERT OR IGNORE`, `MAX(lit, expr)`, `DELETE USING LEFT JOIN`) on the fly.
3. In `onBootstrap(...)`, open an admin connection to the Postgres cluster and
   run `CREATE DATABASE IF NOT EXISTS "wurm<schema>"` for each
   `WurmDatabaseSchema.values()`.
4. Swap `NoOpMigrationStrategy` for one that runs Flyway against each
   per-schema database, pointed at your `db/migration/postgres/<schema>/`
   resource tree.

## Expected log output on boot

```
[NoOpBackend] DatabaseBackendSelectionEvent — registering NoOpDatabaseBackend
[DatabaseBackendEventLogic] Overriding vanilla DB config with backend: noop
[NoOpBackend] DatabaseBackendBootstrapEvent fired — backend is noop, dialect=SQLITE
[NoOpBackend] createConnectionFactory(schema=LOGIN)
[NoOpBackend] createConnectionFactory(schema=ITEMS)
... (one per WurmDatabaseSchema)
[NoOpBackend] createMigrationStrategy()
[DatabaseBackendEventLogic] Installed backend noop with 9 connector(s)
[NoOpBackend] migrate() — no-op, returning empty success
```

If you see all three phases in that order, the SPI is wired correctly.

## See also

- [`docs/guides/database-backend-spi.md`](../../docs/guides/database-backend-spi.md) — full SPI guide
- [`wurmmodloader-api/.../database/DatabaseBackend.java`](../../wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/database/DatabaseBackend.java)

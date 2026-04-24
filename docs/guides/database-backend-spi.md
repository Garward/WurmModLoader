## Database Backend SPI

Replace Wurm's built-in SQLite (or MySQL) factory with a custom backend — Postgres, MariaDB, a distributed store, whatever — without patching WU classes yourself. The framework handles the interception; you supply the implementation.

**Why this exists:** SQLite's single-writer-per-file locking is the ceiling on how big a modded WU server can get. `SQLITE_BUSY` errors cascade between unrelated mods and can't be fixed from inside any individual mod. This SPI gives one mod a clean hook to replace the backing store with something that does proper row-level locking.

---

### When to use it

Use the SPI if you're implementing a **full replacement** of WU's database layer. Don't use it to add tables or columns to existing databases — that's what the per-table hook patches (`CreatureDbSaveEvent`, etc.) are for. The SPI is for "I want all 9 WU databases served by something that isn't SQLite."

---

### The two pieces

| Component | Package | Role |
|---|---|---|
| [`DatabaseBackend`](../../wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/database/DatabaseBackend.java) | `com.garward.wurmmodloader.api.database` | Interface you implement. Supplies a `ConnectionFactory` + `MigrationStrategy`. |
| [`DatabaseBackendRegistry`](../../wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/database/DatabaseBackendRegistry.java) | same | Process-wide registry. First `register()` wins — mod load order matters. |

---

### The five events

All live in `com.garward.wurmmodloader.api.events.database`.

| Event | When it fires | Carries | Typical use |
|---|---|---|---|
| `DatabaseBackendSelectionEvent` | Inside `DbConnector.initialize()`, before vanilla factory instantiation. Fires exactly once per server start. | Nothing | Call `DatabaseBackendRegistry.register(...)` here. |
| `DatabaseBackendBootstrapEvent` | Immediately after a backend wins first-wins registration, **before** any per-schema `ConnectionFactory` is instantiated. Fires only if a backend is registered. | `DatabaseBackend backend` | DDL bootstrap: `CREATE DATABASE IF NOT EXISTS` on fresh clusters, role provisioning, extension installs. |
| `DatabaseConnectionOpenedEvent` | Immediately after a connection is successfully created (vanilla factories **or** your custom factory — the framework auto-fires for both). | `WurmDatabaseSchema schema`, `java.sql.Connection connection` | Apply session pragmas, set `search_path`, enable extensions. |
| `DatabaseMigrationStartingEvent` | Before Flyway migrates a given schema. | `WurmDatabaseSchema schema` | Register extra migration locations for your own tables. |
| `DatabaseMigrationCompletedEvent` | After Flyway completes successfully. | `WurmDatabaseSchema schema` | Create extra indexes, seed data. Won't fire on migration failure. |

**SQLite vs MySQL asymmetry:** on SQLite, the migration events fire **per schema** (9 times per boot, one per database). On MySQL, vanilla WU drives Flyway's multi-schema mode in a single call — the events fire **once** with `WurmDatabaseSchema.LOGIN` as the representative schema. If you implement a custom backend, you own the cardinality; choose whichever your driver prefers.

---

### Minimum viable backend

```java
package com.example.postgres;

import com.garward.wurmmodloader.api.database.DatabaseBackend;
import com.wurmonline.server.database.WurmDatabaseSchema;

public final class PostgresBackend implements DatabaseBackend {

    @Override
    public String getName() {
        return "postgres";
    }

    @Override
    public Object createConnectionFactory(Object schema) {
        return new PostgresConnectionFactory((WurmDatabaseSchema) schema);
    }

    @Override
    public Object createMigrationStrategy() {
        return new PostgresMigrationStrategy();
    }
}
```

`PostgresConnectionFactory` must extend `com.wurmonline.server.database.ConnectionFactory`. `PostgresMigrationStrategy` must implement `com.wurmonline.server.database.migrations.MigrationStrategy`. The SPI uses `Object` in the signatures so the public API doesn't carry a hard dependency on WU classes — cast on the way out.

Because `ConnectionFactory`'s constructor and `MigrationResult.newSuccess / newError` are package-private in WU, the subclasses must themselves live in the matching `com.wurmonline.server.database` / `com.wurmonline.server.database.migrations` packages. See [`examples/database-backend-noop/`](../../examples/database-backend-noop/) for a working skeleton — a registered backend that logs every phase and stores each schema in a per-schema SQLite file. Copy that folder, rename, and swap the driver URL to port the example to Postgres / MariaDB.

### Registering the backend

```java
import com.garward.wurmmodloader.api.database.DatabaseBackendRegistry;
import com.garward.wurmmodloader.api.events.SubscribeEvent;
import com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent;

public class PostgresMod {

    @SubscribeEvent
    public void onBackendSelection(DatabaseBackendSelectionEvent event) {
        DatabaseBackendRegistry.register(new PostgresBackend());
    }
}
```

The framework fires `DatabaseBackendSelectionEvent` once, early enough that the registered backend is in place before any `DbConnector.getXxxDbCon()` call resolves. If nothing is registered by the time the event returns, vanilla SQLite (or MySQL, if the server is configured for it) is used unchanged.

---

### Connection-level setup

Your factory just builds and returns the raw JDBC `Connection` — the framework handles the rest. When a custom backend is installed, every `ConnectionFactory` it returns is automatically wrapped by `WrappingConnectionFactory`, which on each `createConnection()` call:

1. Invokes your factory's `createConnection()`
2. Wraps the result through `SqlDialectRewriter.wrap(...)` if `getDialect()` is not `CUSTOM` or `SQLITE`
3. Fires `DatabaseConnectionOpenedEvent` so pragma / search_path subscribers run consistently across all backends

So a minimum factory is just:

```java
public final class PostgresConnectionFactory extends ConnectionFactory {
    public PostgresConnectionFactory(WurmDatabaseSchema schema) {
        super("jdbc:postgresql://.../" + schema.name(), schema);
    }
    @Override
    public Connection createConnection() throws SQLException {
        return dataSource.getConnection();
    }
    @Override public boolean isValid(Connection c) throws SQLException { return c != null && !c.isClosed(); }
    @Override public boolean isStale(long ageMs, Connection c) throws SQLException { return false; }
}
```

No manual `ProxyServerHook` calls, no manual `SqlDialectRewriter.wrap(...)`. Both are footguns the framework now covers.

If you need to opt out of rewriting entirely (custom SQL translation at the driver level), return `Dialect.CUSTOM` from `getDialect()`. The event will still fire; the rewriter will not run.

---

### Bootstrap: creating databases on a fresh cluster

WU immediately opens nine per-schema connections to nine databases. On a
freshly-installed Postgres (or any target where the physical databases don't
exist yet), those connections fail before your `MigrationStrategy` gets a
chance to run. `MigrationStrategy` creates **tables**, not databases.

Subscribe to `DatabaseBackendBootstrapEvent` to run pre-connection DDL:

```java
import com.garward.wurmmodloader.api.events.SubscribeEvent;
import com.garward.wurmmodloader.api.events.database.DatabaseBackendBootstrapEvent;
import com.wurmonline.server.database.WurmDatabaseSchema;

@SubscribeEvent
public void onBootstrap(DatabaseBackendBootstrapEvent event) {
    if (!"postgres".equals(event.getBackend().getName())) return;
    try (Connection admin = DriverManager.getConnection(
             "jdbc:postgresql://host:5432/postgres", user, password)) {
        admin.setAutoCommit(true);
        for (WurmDatabaseSchema s : WurmDatabaseSchema.values()) {
            String db = "wurm" + s.name().toLowerCase();
            try (Statement st = admin.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT 1 FROM pg_database WHERE datname='" + db + "'")) {
                if (!rs.next()) {
                    try (Statement create = admin.createStatement()) {
                        create.execute("CREATE DATABASE \"" + db + "\"");
                    }
                }
            }
        }
    }
}
```

The event fires exactly once per process, only when a backend is registered, and before any per-schema `ConnectionFactory.createConnection()` runs. If your handler throws, the framework logs and proceeds — per-schema connects will then likely fail, which is usually the right failure mode (clearer than silently skipping bootstrap).

---

### Running migrations

Your backend's `MigrationStrategy` is in complete control. Fire `DatabaseMigrationStartingEvent` before each schema's migration and `DatabaseMigrationCompletedEvent` on success, using `ProxyServerHook.fireDatabaseMigrationStartingEvent(schema)` / `fireDatabaseMigrationCompletedEvent(schema)`. The framework does this automatically for the vanilla strategies; custom backends should match the contract.

Other mods that just want to tag along on migrations (adding their own tables without replacing the backend) subscribe to `DatabaseMigrationStartingEvent` and register additional Flyway locations. They shouldn't need to touch the SPI.

#### Flyway migration file layout — canonical convention

Vanilla WU lays its SQLite migrations out like this, on disk:

```
<server-root>/dist/migrations/
├── login/
├── items/v3__cooking_update.sql, v4__weight_changes.sql, …
├── players/
├── creatures/
├── deities/
├── economy/
├── zones/
├── logs/
└── templates/
```

Backends should stay close to that shape so server owners find the same mental model. Two options:

**1. Mirror WU and ship SQL on disk** (good if operators need to edit migrations in place):

```
<server-root>/dist/migrations-<backend>/
├── login/
├── items/V3__cooking_update.sql, …
└── …                                       (one dir per WurmDatabaseSchema.getMigration())
```

Point Flyway at `filesystem:<that-path>/<schema-name>` per migrator, same as `SqliteMigrator` does.

**2. Bundle SQL inside the mod JAR as classpath resources** (recommended — migrations become versioned with the backend binary, nothing to edit at runtime):

```
src/main/resources/db/migration/<backend>/<schema>/
├── V1__init.sql
├── V2__add_indexes.sql
└── R__materialize_views.sql
```

Point Flyway at `classpath:db/migration/<backend>/<schema>` per migrator. Drop `filesystem:` entirely.

WU's `Migrator` base class configures Flyway with these settings — match them so your migrations interleave cleanly with any migration introspection tooling:

| Flyway setting                 | WU value          | Notes                                               |
|--------------------------------|-------------------|-----------------------------------------------------|
| `setTable(...)`                 | `SCHEMA_VERSION`  | Version-tracking table name; **must** match WU      |
| `setSqlMigrationPrefix(...)`    | `v`               | Versioned scripts: `v<N>__<desc>.sql` (lowercase v) |
| `setRepeatableSqlMigrationPrefix(...)` | `r`        | Repeatable scripts: `r__<desc>.sql`                 |
| `setBaselineVersion(...)`       | `1`               | First migration numbered 1, not 0                   |

Filenames are case-sensitive in Flyway, and WU uses lowercase `v` / `r`. If you accept third-party migration bundles written against plain Flyway conventions (uppercase `V` / `R`), you can override the prefixes per-migrator — but keep your own bundled scripts on WU's convention.

---

### First-wins registration

`DatabaseBackendRegistry.register(...)` returns `true` if the backend took effect, `false` if another mod got there first. Runtime conflicts log a warning with both backend names. Practically this means:

- Ship **at most one** `DatabaseBackend`-providing mod per server.
- If you need users to pick between two, build the choice into configuration inside a single mod rather than shipping two competing ones.
- The server owner resolves conflicts by disabling one mod, not by load-order tuning.

---

### SQL dialect rewriting

WU hardcodes 16 `INSERT OR IGNORE` strings (plus a handful of `INSERT IGNORE` MySQL-isms) in `static final String` fields across `Achievement`, `Items`, `PlayerMetaData`, etc. If your backend isn't SQLite or MySQL, these reach the driver as-is and fail.

The framework ships a rewriter. Declare your dialect on the backend:

```java
@Override
public Dialect getDialect() {
    return Dialect.POSTGRES;
}
```

That is the only thing you need to do. The framework's `WrappingConnectionFactory` runs every `Connection` your factory returns through `SqlDialectRewriter.wrap(connection, dialect)` before handing it back to WU. The wrapper is a JDK `Proxy` that intercepts `prepareStatement`, `prepareCall`, and the `Statement.execute*(String, ...)` family, translating SQL strings via `SqlDialectRewriter.rewrite(...)` before delegating. `Dialect.SQLITE` and `Dialect.CUSTOM` skip wrapping (no overhead).

Known rewrites:

| Input (vanilla WU) | Postgres output | MySQL output |
|---|---|---|
| `INSERT OR IGNORE INTO T (...) VALUES (...)` | `INSERT INTO T (...) VALUES (...) ON CONFLICT DO NOTHING` | `INSERT IGNORE INTO T (...) VALUES (...)` |
| `INSERT OR IGNORE INTO T SELECT ...` | `INSERT INTO T SELECT ... ON CONFLICT DO NOTHING` | `INSERT IGNORE INTO T SELECT ...` |
| `INSERT IGNORE INTO T (SELECT ...)` (parenthesized MySQL form in `Items.java`) | `INSERT INTO T SELECT ... ON CONFLICT DO NOTHING` | passthrough |
| `MAX(<lit>, expr)` (SQLite's scalar MAX, used in `LocalSupplyDemand`) | `GREATEST(<lit>, expr)` | passthrough |
| `Tickets.PURGEBADACTIONS` (MySQL `DELETE FROM T USING T LEFT JOIN ...`) | `DELETE FROM TICKETACTIONS WHERE NOT EXISTS (SELECT 1 FROM TICKETS ...)` | passthrough |

If you need rewrites the framework doesn't ship, return `Dialect.CUSTOM` and translate inside your factory directly — the framework then does no rewriting and hands the raw connection back to WU. You can still call `SqlDialectRewriter.wrap(raw, Dialect.POSTGRES)` manually and layer your own `InvocationHandler` on top if you want the stock rewrites plus extras.

---

### Vanilla WU MySQL-only maintenance is skipped for non-MySQL backends

`DbIndexManager.createIndexes()`, `removeIndexes()`, and `repairDatabaseTables()` emit MySQL-only DDL (`ALTER TABLE ... ADD INDEX (col)`, `REPAIR TABLE`). Vanilla early-returns on SQLite; the framework's `DbIndexManagerMaintenancePatch` additionally early-returns when the registered backend's dialect is not SQLite, MySQL, or MariaDB. Vanilla MySQL / MariaDB servers keep the vanilla path.

**Your responsibility as a non-MySQL backend author:** ship index DDL in your Flyway migration files. The framework will not try to translate `ADD INDEX (col)` to `CREATE INDEX ... ON ...(col)` across dialects.

---

### What the SPI doesn't do (yet)

- **Connection pooling.** Vanilla WU uses one connection per schema, auto-commit mode, no pooling. Your backend can wrap a pool (HikariCP, c3p0) internally — but the framework doesn't mandate it. If you do, make sure pooled connections still behave correctly when `isStale()` is called on them by `DbConnector`'s refresh logic.
- **Per-statement interception beyond dialect rewriting.** The rewriter's `Proxy` only touches SQL text. If you want query logging, timing, or prepared-statement caching, wrap `Connection` / `PreparedStatement` inside your backend.
- **Live data migration.** `MigrationStrategy` runs schema DDL via Flyway — it doesn't move rows. See the dedicated section below.

---

### Shutdown drain: closing pools cleanly

Vanilla WU owns exactly one JDBC connection per schema, closes them implicitly on JVM exit, and never notices the difference. A pooled backend (HikariCP, c3p0, pgBouncer sidecar) does not get that for free — if the server process exits before `pool.close()` runs, in-flight transactions are aborted by the database rather than drained, and prepared-statement caches / metric exporters are left with orphaned TCP state.

**Contract:** if your backend owns any background resources (connection pool, exporter thread, async flusher) — subscribe to `ServerStoppingEvent` and drain them there.

```java
@SubscribeEvent
public void onServerStopping(ServerStoppingEvent event) {
    // HikariCP: wait for in-flight queries, then close idle conns + signal the housekeeper
    hikariDataSource.close();
    // Metric exporter thread, if any
    metricsExecutor.shutdown();
    metricsExecutor.awaitTermination(5, TimeUnit.SECONDS);
}
```

`ServerStoppingEvent` fires from `ServerShutdownPatch` early in the shutdown sequence — **before** WU closes its own `DbConnector` connections. That ordering is deliberate: your pool gets to drain its borrowers while the rest of the shutdown still has a working database to write its final state to. See [`wurmmodloader-core/.../bytecode/patches/ServerShutdownPatch.java`](../../wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/patches/ServerShutdownPatch.java) for the exact injection point if you need to reason about ordering against other handlers.

**Use `#shutdown <minutes> <reason>` from the server console for clean shutdowns.** SIGTERM / Ctrl-C skip the orderly sequence and `ServerStoppingEvent` may not fire — the same caveat applies to vanilla WU and isn't SPI-specific. Document this in your backend's README so operators know not to `kill` the Java process casually.

If a handler throws, the framework logs and continues — other subscribers still run and the JVM still exits. Don't rely on shutdown code for durability guarantees that should already be enforced by commit discipline upstream.

---

### Copying an existing SQLite world into your backend

A server owner adopting your backend starts with 9 live SQLite files. `MigrationStrategy` builds the target schema; `DatabaseMigrator` (`com.garward.wurmmodloader.api.database.migration`) copies the rows. It's a one-shot bulk copy — stop the server, run the migrator against copies of the SQLite files, start the server on the new backend.

```java
import com.garward.wurmmodloader.api.database.migration.DatabaseMigrator;
import com.garward.wurmmodloader.api.database.migration.CoercionPolicy;
import com.garward.wurmmodloader.api.database.migration.MigrationReport;

MigrationReport report = DatabaseMigrator.builder()
    .addSource("players",
        DriverManager.getConnection("jdbc:sqlite:/path/to/sqlite/wurmplayers.db"),
        () -> myBackend.createConnectionFor(WurmDatabaseSchema.LOGIN))
    .addSource("items",
        DriverManager.getConnection("jdbc:sqlite:/path/to/sqlite/wurmitems.db"),
        () -> myBackend.createConnectionFor(WurmDatabaseSchema.ITEMS))
    // ... add the other 7 schemas
    .coercion(CoercionPolicy.LENIENT)   // coerce SQLite's loose text-in-INTEGER into real ints
    .batchSize(1000)
    .build()
    .run();

if (report.hasFailures()) {
    report.failures().forEach(e -> System.err.println(e));
}
System.out.println("Migrated " + report.totalRowsInserted() + " rows");
```

**What it does:**

- Enumerates every user table in each source (skips `sqlite_sequence`, `flyway_schema_history`).
- Introspects the target via `DatabaseMetaData.getColumns(...)` to build typed `INSERT` statements and coerce SQLite's loose values — strings in INTEGER columns, empty strings, `"0"`/`"1"` booleans — into the target's strict types under `CoercionPolicy.LENIENT`.
- Wraps each table in a transaction on the target and batches inserts.
- Returns a per-table report: rows read, inserted, coerced, skipped, and any exception.

**What it doesn't do:**

- Live migration. Copy the SQLite files to a safe location first and point the migrator at the copies.
- DDL generation — the target schema must already exist (create it via `MigrationStrategy`).
- Referential-integrity ordering across tables. If your target enforces FKs at insert time, either defer constraint checks for the migration or sort tables topologically yourself.
- Idempotence. A failed table aborts only that table; rerunning will double-insert unless the target's unique constraints stop it.

The migrator depends only on JDBC, so it works standalone — you can run it from an admin command, a bootstrap flag, or a separate CLI jar.

---

### Testing your backend

Run the server against a throwaway world copy. First-boot signs the swap took effect:

```
[DatabaseBackendRegistry] Registered backend: postgres (com.example.postgres.PostgresBackend)
```

…appearing **before** any `DbConnector` log lines. If you see vanilla SQLite initialization in the log before the registry registration message, your mod's `@SubscribeEvent` handler is running too late — check that the handler is attached during early init (not lazily on first player login or similar).

After startup, sanity-check that connections are live and the schemas migrated:

```
LOG=<wurm-server-dir>/logs/wurmmodloader.0.log
grep DatabaseBackendRegistry "$LOG"
grep DatabaseConnectionOpenedEvent "$LOG"
grep DatabaseMigration "$LOG"
```

If vanilla SQLite messages still appear (`JournalMode.WAL`, `SynchronousMode.NORMAL`), the backend didn't take — usually because registration happened after `DbConnector.initialize()` ran.

---

### Related reading

- [`extending-framework.md`](extending-framework.md) — how new events and patches are added to the framework (for understanding the plumbing, not required for backend authors).
- [`event-bus.md`](event-bus.md) — `@SubscribeEvent` mechanics, priority, cancellation.
- Decompiled WU reference: `com.wurmonline.server.DbConnector`, `com.wurmonline.server.database.ConnectionFactory`, `com.wurmonline.server.database.migrations.MigrationStrategy`. Inspect signatures via `javap -s -p -classpath server.jar com.wurmonline.server.DbConnector`.

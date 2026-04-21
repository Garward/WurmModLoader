package com.garward.wurmmodloader.mods.postgresbackend;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Lifecycle wrapper around an embedded Postgres process. Boots on
 * {@link #start(PostgresConfig)} and stops on {@link #stop()}. A "connection
 * hint" file is written next to the mod so {@code connect.sh} / {@code
 * connect.bat} can locate the extracted {@code psql} binary and connect
 * without any further configuration.
 */
public final class PostgresServer {

    private static final Logger logger = Logger.getLogger(PostgresServer.class.getName());

    private EmbeddedPostgres pg;       // first-boot / initdb path
    private Process directPg;          // subsequent-boots direct path
    private Path directDataDir;        // data dir for the direct-start path (for pg_ctl stop)
    private Path directBinariesDir;    // binaries root to locate pg_ctl

    public synchronized void start(PostgresConfig cfg) throws IOException {
        Path dataDir = Paths.get(cfg.embeddedDataDir).toAbsolutePath();
        Path binariesDir = Paths.get(cfg.embeddedBinariesDir).toAbsolutePath();
        Files.createDirectories(dataDir);
        Files.createDirectories(binariesDir);

        logger.info("[PostgresBackend] Embedded mode — starting Postgres");
        logger.info("[PostgresBackend]   data dir     : " + dataDir);
        logger.info("[PostgresBackend]   binaries dir : " + binariesDir);
        logger.info("[PostgresBackend]   port         : " + cfg.embeddedPort);

        boolean alreadyInitialized = Files.exists(dataDir.resolve("PG_VERSION"));
        if (alreadyInitialized) {
            // zonky 2.0.7 unconditionally runs initdb, which errors on a
            // populated data dir. Start postgres directly via the binary that
            // was extracted on first boot.
            startDirect(cfg, dataDir, binariesDir);
        } else {
            this.pg = EmbeddedPostgres.builder()
                .setPort(cfg.embeddedPort)
                .setDataDirectory(dataDir)
                .setOverrideWorkingDirectory(binariesDir.toFile())
                .setPgBinaryResolver(new LazyBinaryResolver(binariesDir, cfg.embeddedPostgresVersion))
                .setServerConfig("listen_addresses", "localhost")
                .start();
        }

        // Overwrite the connection fields so the rest of the mod (factories, Flyway)
        // sees the running embedded server.
        cfg.host = "localhost";
        cfg.port = cfg.embeddedPort;
        cfg.database = "postgres";
        cfg.user = "postgres";
        cfg.password = "";
        cfg.sslmode = "disable";

        logger.info("[PostgresBackend] Embedded Postgres listening on localhost:" + cfg.port);

        writeConnectionHint(cfg, dataDir);
    }

    public synchronized void stop() {
        if (pg != null) {
            try {
                pg.close();
                logger.info("[PostgresBackend] Embedded Postgres stopped cleanly");
            } catch (IOException e) {
                logger.warning("[PostgresBackend] Error stopping embedded Postgres: " + e.getMessage());
            } finally {
                pg = null;
            }
        }
        if (directPg != null) {
            try {
                // Use pg_ctl — it reads <dataDir>/postmaster.pid to find the real
                // postmaster (which may not match Process.pid on all kernels) and
                // sends the right signal itself. -m fast = SIGINT = fast shutdown:
                // abort in-flight txns, kick clients, checkpoint, clean exit.
                boolean stopped = pgCtlStopFast(directBinariesDir, directDataDir);
                if (!stopped) {
                    logger.warning("[PostgresBackend] pg_ctl stop failed; falling back to Process.destroy()");
                    directPg.destroy();
                }
                if (!directPg.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warning("[PostgresBackend] Postgres didn't exit in 10s; destroying forcibly");
                    directPg.destroyForcibly();
                }
                logger.info("[PostgresBackend] Direct-start Postgres stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                directPg = null;
                directDataDir = null;
                directBinariesDir = null;
            }
        }
    }

    private static boolean pgCtlStopFast(Path binariesDir, Path dataDir) {
        if (binariesDir == null || dataDir == null) return false;
        try {
            Path pgCtl = findBinary(binariesDir, "pg_ctl");
            if (pgCtl == null) {
                logger.warning("[PostgresBackend] pg_ctl binary not found under " + binariesDir);
                return false;
            }
            ProcessBuilder pb = new ProcessBuilder(
                pgCtl.toString(), "stop", "-m", "fast", "-w", "-t", "30",
                "-D", dataDir.toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            // Drain output so the process doesn't block on a full pipe.
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    logger.info("[PostgresBackend][pg_ctl] " + line);
                }
            }
            int rc = p.waitFor();
            return rc == 0;
        } catch (Exception e) {
            logger.warning("[PostgresBackend] pg_ctl stop threw: " + e.getMessage());
            return false;
        }
    }

    private void startDirect(PostgresConfig cfg, Path dataDir, Path binariesDir) throws IOException {
        Path postgresBin = findBinary(binariesDir, "postgres");
        if (postgresBin == null) {
            throw new IOException("Could not find bin/postgres under " + binariesDir
                + " — delete the data dir to force a clean re-init.");
        }
        logger.info("[PostgresBackend] Direct-start (data dir already initialized): " + postgresBin);

        ProcessBuilder pb = new ProcessBuilder(
            postgresBin.toString(),
            "-D", dataDir.toString(),
            "-p", String.valueOf(cfg.embeddedPort),
            "-c", "listen_addresses=localhost",
            // Data-dir path easily exceeds the 107-byte Unix-socket limit under
            // Steam's deeply-nested install path. Fall back to /tmp (matches
            // zonky's default).
            "-c", "unix_socket_directories=" + System.getProperty("java.io.tmpdir", "/tmp")
        );
        pb.redirectErrorStream(true);
        pb.redirectOutput(dataDir.resolve("postgres.log").toFile());
        this.directPg = pb.start();
        this.directDataDir = dataDir;
        this.directBinariesDir = binariesDir;

        // Mutate cfg early so the connection-hint below reflects reality.
        cfg.host = "localhost";
        cfg.port = cfg.embeddedPort;
        cfg.database = "postgres";
        cfg.user = "postgres";
        cfg.password = "";
        cfg.sslmode = "disable";

        waitForReady(cfg);
    }

    private static Path findBinary(Path binariesDir, String name) throws IOException {
        if (!Files.isDirectory(binariesDir)) return null;
        try (Stream<Path> walk = Files.walk(binariesDir)) {
            return walk.filter(p -> p.getFileName().toString().equals(name)
                    && p.getParent() != null
                    && p.getParent().getFileName().toString().equals("bin"))
                .findFirst().orElse(null);
        }
    }

    private void waitForReady(PostgresConfig cfg) throws IOException {
        try {
            // pgjdbc is in the mod URLClassLoader; the thread's context
            // classloader (Wurm's) doesn't see it, so DriverManager can't
            // auto-register via ServiceLoader. Force registration here.
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IOException("pgjdbc not on mod classpath", e);
        }
        String url = "jdbc:postgresql://" + cfg.host + ":" + cfg.port + "/" + cfg.database + "?sslmode=disable";
        Properties props = new Properties();
        props.setProperty("user", cfg.user);
        long deadline = System.currentTimeMillis() + 30_000L;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            if (!directPg.isAlive()) {
                throw new IOException("postgres process exited before accepting connections — check "
                    + cfg.embeddedDataDir + "/postgres.log");
            }
            try (Connection c = DriverManager.getConnection(url, props)) {
                logger.info("[PostgresBackend] Direct-start Postgres ready on " + cfg.host + ":" + cfg.port);
                return;
            } catch (Exception e) {
                last = new IOException(e);
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw new IOException("Postgres did not accept connections within 30s", last);
    }

    /**
     * Writes {@code mods/postgresbackend/connection.info} with host/port/user/db
     * plus the absolute path to the extracted {@code psql} binary. The
     * {@code connect.sh}/{@code connect.bat} helpers read this file to attach
     * without any user-supplied configuration.
     */
    private void writeConnectionHint(PostgresConfig cfg, Path dataDir) {
        try {
            Path hintDir = Paths.get("mods/postgresbackend").toAbsolutePath();
            Files.createDirectories(hintDir);
            Path hintFile = hintDir.resolve("connection.info");

            // zonky extracts binaries to <overrideWorkingDirectory>/.embedpostgresql-<hash>/
            // but the exact subdir isn't exposed. Fall back to searching the
            // binaries tree for bin/psql the first time the user runs connect.*.
            Path binariesDir = Paths.get(cfg.embeddedBinariesDir).toAbsolutePath();

            StringBuilder sb = new StringBuilder();
            sb.append("# PostgresBackend runtime connection hint — auto-written on each boot.\n");
            sb.append("# Used by connect.sh / connect.bat. Do not edit by hand.\n");
            sb.append("mode=").append(cfg.mode).append("\n");
            sb.append("host=").append(cfg.host).append("\n");
            sb.append("port=").append(cfg.port).append("\n");
            sb.append("database=").append(cfg.database).append("\n");
            sb.append("user=").append(cfg.user).append("\n");
            sb.append("password=").append(cfg.password).append("\n");
            sb.append("data_dir=").append(dataDir).append("\n");
            sb.append("binaries_dir=").append(binariesDir).append("\n");
            Files.write(hintFile, sb.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            logger.warning("[PostgresBackend] Could not write connection.info: " + e.getMessage());
        }
    }
}

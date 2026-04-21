package com.garward.wurmmodloader.mods.postgresbackend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Configurable auto-backup for the PostgresBackend mod.
 *
 * <p>Invokes {@code pg_dump} in custom format ({@code -Fc}) — the Postgres
 * native, compressed, restorable format. Works uniformly for embedded and
 * external modes: for embedded, finds {@code bin/pg_dump} in the downloaded
 * Postgres tree; for external, uses {@code cfg.pgDumpPath} if set, else
 * falls back to whichever {@code pg_dump} is on {@code PATH}.</p>
 *
 * <p>Three trigger points wired by {@link PostgresBackendMod}:</p>
 * <ul>
 *   <li>Startup — one async backup after bootstrap, if
 *       {@code autoBackupOnStartup=true}.</li>
 *   <li>Interval — repeating every {@code autoBackupIntervalHours} hours
 *       while the server is running (0 disables interval backups).</li>
 *   <li>Shutdown — one synchronous backup on clean shutdown, if
 *       {@code autoBackupOnShutdown=true}.</li>
 * </ul>
 *
 * <p>Files are written as
 * {@code <autoBackupDir>/wurm-YYYYMMDD-HHMMSS.dump}. After each successful
 * backup, older files are pruned to {@code autoBackupRetain} most recent
 * (by modification time).</p>
 *
 * <p>Restoring: {@code pg_restore -d <targetdb> --clean --if-exists
 * /path/to/wurm-YYYYMMDD-HHMMSS.dump}. See README for details.</p>
 */
final class PostgresBackup {

    private static final Logger logger = Logger.getLogger(PostgresBackup.class.getName());
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> scheduledHandle;

    static void startSchedule(PostgresConfig cfg) {
        if (!cfg.autoBackupEnabled) {
            logger.info("[PostgresBackend][backup] auto-backup disabled (autoBackupEnabled=false)");
            return;
        }
        logger.info("[PostgresBackend][backup] enabled — dir=" + cfg.autoBackupDir
            + " retain=" + cfg.autoBackupRetain
            + " intervalHours=" + cfg.autoBackupIntervalHours
            + " onStartup=" + cfg.autoBackupOnStartup
            + " onShutdown=" + cfg.autoBackupOnShutdown);

        if (cfg.autoBackupOnStartup) {
            Thread t = new Thread(() -> runBackupSafe(cfg, "startup"), "postgresbackend-backup-startup");
            t.setDaemon(true);
            t.start();
        }
        if (cfg.autoBackupIntervalHours > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "postgresbackend-backup-scheduler");
                t.setDaemon(true);
                return t;
            });
            long intervalSec = cfg.autoBackupIntervalHours * 3600L;
            scheduledHandle = scheduler.scheduleAtFixedRate(
                () -> runBackupSafe(cfg, "interval"),
                intervalSec, intervalSec, TimeUnit.SECONDS);
            logger.info("[PostgresBackend][backup] scheduled recurring backups every "
                + cfg.autoBackupIntervalHours + "h");
        }
    }

    static void stopSchedule() {
        if (scheduledHandle != null) {
            scheduledHandle.cancel(false);
            scheduledHandle = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
            scheduler = null;
        }
    }

    static void runShutdownBackup(PostgresConfig cfg) {
        if (!cfg.autoBackupEnabled || !cfg.autoBackupOnShutdown) return;
        runBackupSafe(cfg, "shutdown");
    }

    private static void runBackupSafe(PostgresConfig cfg, String reason) {
        try {
            runBackup(cfg, reason);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[PostgresBackend][backup] " + reason + " backup failed", t);
        }
    }

    private static void runBackup(PostgresConfig cfg, String reason) throws IOException, InterruptedException {
        Path outDir = Paths.get(cfg.autoBackupDir);
        Files.createDirectories(outDir);

        Path pgDump = resolvePgDump(cfg);
        if (pgDump == null) {
            logger.warning("[PostgresBackend][backup] pg_dump not found — set pgDumpPath in config "
                + "or ensure it's on PATH. Skipping " + reason + " backup.");
            return;
        }

        String stamp = LocalDateTime.now().format(STAMP);
        Path outFile = outDir.resolve("wurm-" + stamp + ".dump");

        List<String> cmd = new ArrayList<>();
        cmd.add(pgDump.toString());
        cmd.add("-h"); cmd.add(cfg.host);
        cmd.add("-p"); cmd.add(String.valueOf(cfg.port));
        cmd.add("-U"); cmd.add(cfg.user);
        cmd.add("-d"); cmd.add(cfg.database);
        cmd.add("-Fc");                      // custom format (compressed, pg_restore-compatible)
        cmd.add("--no-owner");               // portable across roles
        cmd.add("--no-privileges");
        cmd.add("-f"); cmd.add(outFile.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        if (cfg.password != null && !cfg.password.isEmpty()) {
            pb.environment().put("PGPASSWORD", cfg.password);
        }

        long t0 = System.currentTimeMillis();
        logger.info("[PostgresBackend][backup] " + reason + " — dumping to " + outFile);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                logger.fine("[PostgresBackend][backup][pg_dump] " + line);
            }
        }
        int rc = p.waitFor();
        long ms = System.currentTimeMillis() - t0;
        if (rc != 0) {
            try { Files.deleteIfExists(outFile); } catch (IOException ignored) {}
            throw new IOException("pg_dump exited " + rc + " — check PATH/auth and the dir "
                + cfg.autoBackupDir);
        }
        long bytes = Files.size(outFile);
        logger.info(String.format("[PostgresBackend][backup] %s OK (%d KB in %.1fs) — %s",
            reason, bytes / 1024, ms / 1000.0, outFile.getFileName()));

        pruneOld(outDir, cfg.autoBackupRetain);
    }

    /**
     * Find {@code pg_dump}. Priority:
     * <ol>
     *   <li>{@code cfg.pgDumpPath} if set (explicit override).</li>
     *   <li>{@code bin/pg_dump} under {@code embeddedBinariesDir} — populated
     *       by the embedded-postgres download. Works whether mode is
     *       embedded or external (once the binaries exist).</li>
     *   <li>System {@code PATH}.</li>
     * </ol>
     */
    private static Path resolvePgDump(PostgresConfig cfg) {
        if (cfg.pgDumpPath != null && !cfg.pgDumpPath.isEmpty()) {
            Path p = Paths.get(cfg.pgDumpPath);
            if (Files.isExecutable(p)) return p;
            logger.warning("[PostgresBackend][backup] pgDumpPath=" + cfg.pgDumpPath
                + " not executable — falling back");
        }
        Path embedded = findBinary(Paths.get(cfg.embeddedBinariesDir), "pg_dump");
        if (embedded != null) return embedded;

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
                Path candidate = Paths.get(dir, "pg_dump");
                if (Files.isExecutable(candidate)) return candidate;
            }
        }
        return null;
    }

    private static Path findBinary(Path binariesDir, String name) {
        if (!Files.isDirectory(binariesDir)) return null;
        try (Stream<Path> walk = Files.walk(binariesDir)) {
            return walk.filter(p -> p.getFileName().toString().equals(name)
                    && p.getParent() != null
                    && p.getParent().getFileName().toString().equals("bin"))
                .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static void pruneOld(Path dir, int retain) {
        if (retain <= 0) return;
        List<Path> dumps = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "wurm-*.dump")) {
            for (Path p : ds) dumps.add(p);
        } catch (IOException e) {
            logger.log(Level.WARNING, "[PostgresBackend][backup] prune scan failed", e);
            return;
        }
        if (dumps.size() <= retain) return;
        dumps.sort(Comparator.comparing(PostgresBackup::mtime).reversed()); // newest first
        for (int i = retain; i < dumps.size(); i++) {
            Path victim = dumps.get(i);
            try {
                Files.delete(victim);
                logger.info("[PostgresBackend][backup] pruned old backup " + victim.getFileName());
            } catch (IOException e) {
                logger.log(Level.WARNING, "[PostgresBackend][backup] failed to prune " + victim, e);
            }
        }
    }

    private static FileTime mtime(Path p) {
        try { return Files.getLastModifiedTime(p); }
        catch (IOException e) { return FileTime.fromMillis(0L); }
    }

    private PostgresBackup() {}
}

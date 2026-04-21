package com.garward.wurmmodloader.mods.postgresbackend;

import io.zonky.test.db.postgres.embedded.PgBinaryResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link PgBinaryResolver} that lazily downloads zonky's per-platform Postgres
 * binary jar from Maven Central on first boot, caches it on disk, and serves
 * the embedded {@code .txz} tarball from the cached copy on subsequent boots.
 *
 * <p>Avoids shipping ~80MB of binaries with the mod. Costs one HTTP GET
 * (~25MB) the first time the mod runs on a given host.</p>
 */
public final class LazyBinaryResolver implements PgBinaryResolver {

    private static final Logger logger = Logger.getLogger(LazyBinaryResolver.class.getName());

    private static final String MAVEN_BASE =
        "https://repo1.maven.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-";

    private final Path cacheDir;
    private final String version;

    public LazyBinaryResolver(Path cacheDir, String version) {
        this.cacheDir = cacheDir;
        this.version = version;
    }

    @Override
    public InputStream getPgBinary(String system, String machineHardware) throws IOException {
        String sysKey = normalizeSystem(system);
        String archKey = normalizeArch(machineHardware);
        String platform = sysKey + "-" + archKey;

        String jarName = "embedded-postgres-binaries-" + platform + "-" + version + ".jar";
        Path cachedJar = cacheDir.resolve(jarName);

        if (!Files.exists(cachedJar)) {
            Files.createDirectories(cacheDir);
            String url = MAVEN_BASE + platform + "/" + version + "/" + jarName;
            logger.info("[PostgresBackend] First-boot: downloading Postgres binary ("
                + platform + " " + version + ") from " + url);
            Path tmp = Files.createTempFile(cacheDir, jarName + ".", ".part");
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Files.deleteIfExists(tmp);
                throw new IOException("Failed to download " + url + " — check internet "
                    + "connectivity or switch to mode=external in postgresbackend.config", e);
            }
            Files.move(tmp, cachedJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            logger.info("[PostgresBackend] Cached " + jarName + " (" + Files.size(cachedJar) + " bytes)");
        } else {
            logger.fine("[PostgresBackend] Using cached binary jar: " + cachedJar);
        }

        // The jar contains a single entry like "postgres-<sys>-<arch>.txz".
        JarFile jar = new JarFile(cachedJar.toFile());
        JarEntry txz = null;
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (e.getName().endsWith(".txz") || e.getName().endsWith(".tar.xz")) {
                txz = e;
                break;
            }
        }
        if (txz == null) {
            try { jar.close(); } catch (IOException ignored) {}
            throw new IOException("No .txz entry found inside " + cachedJar);
        }
        // Return an InputStream that closes the jar when itself closes.
        InputStream raw = jar.getInputStream(txz);
        final JarFile jarRef = jar;
        return new InputStream() {
            @Override public int read() throws IOException { return raw.read(); }
            @Override public int read(byte[] b, int off, int len) throws IOException { return raw.read(b, off, len); }
            @Override public int available() throws IOException { return raw.available(); }
            @Override public void close() throws IOException {
                try { raw.close(); } finally {
                    try { jarRef.close(); } catch (IOException ex) {
                        logger.log(Level.FINE, "jar close", ex);
                    }
                }
            }
        };
    }

    private static String normalizeSystem(String s) {
        String lc = s.toLowerCase();
        if (lc.contains("linux")) return "linux";
        if (lc.contains("mac") || lc.contains("darwin")) return "darwin";
        if (lc.contains("windows")) return "windows";
        return lc;
    }

    private static String normalizeArch(String a) {
        String lc = a.toLowerCase();
        if (lc.equals("amd64") || lc.equals("x86_64") || lc.equals("x64")) return "amd64";
        if (lc.equals("aarch64") || lc.equals("arm64")) return "arm64v8";
        if (lc.equals("i386") || lc.equals("i686") || lc.equals("x86")) return "i386";
        return lc;
    }
}

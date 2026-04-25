package com.garward.wurmmodloader.mods.livemap.renderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.wurmonline.mesh.MeshIO;

/**
 * Generates a single-zoom grid of map tiles to disk. Mirrors WurmMapGen's
 * output layout ({@code images/{x}-{y}.png}, one tile per {@code tileSize}
 * pixels, 1:1 mesh-to-pixel) but delegates per-pixel rendering to our
 * {@link LiveMapRenderer} so output keeps our depth-graded water + hillshade.
 *
 * <p>Leaflet handles all zoom client-side from this single base layer — no
 * pyramid required.
 */
public final class TileGridBuilder {

    private static final Logger LOGGER = Logger.getLogger(TileGridBuilder.class.getName());

    private final MeshIO mesh;
    private final LiveMapRenderer renderer;
    private final Path outputDir;
    private final int tileSize;
    private final int threads;

    public TileGridBuilder(MeshIO mesh, LiveMapRenderer renderer,
                           Path outputDir, int tileSize, int threads) {
        this.mesh = mesh;
        this.renderer = renderer;
        this.outputDir = outputDir;
        this.tileSize = tileSize;
        this.threads = Math.max(1, threads);
    }

    /** Number of tiles along one axis for the current mesh + tileSize. */
    public int tilesPerSide() {
        int size = mesh.getSize();
        return (size + tileSize - 1) / tileSize;
    }

    /** True if the output dir already contains a tile grid. */
    public boolean isAlreadyBuilt() {
        Path probe = outputDir.resolve("0-0.png");
        return Files.exists(probe);
    }

    /**
     * Generate the full tile grid synchronously on the calling thread. Uses an
     * internal threadpool for parallel tile rendering.
     */
    public void generate() throws IOException {
        long start = System.currentTimeMillis();
        Files.createDirectories(outputDir);

        final int tps = tilesPerSide();
        final int total = tps * tps;
        final AtomicInteger done = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);

        LOGGER.info("[LiveMap] Generating " + total + " tiles ("
                + tps + "x" + tps + ", " + tileSize + "px each, "
                + threads + " threads)...");

        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "LiveMap-TileGen");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        try {
            for (int tx = 0; tx < tps; tx++) {
                for (int ty = 0; ty < tps; ty++) {
                    final int fx = tx;
                    final int fy = ty;
                    pool.execute(() -> {
                        try {
                            renderTile(fx, fy);
                            int n = done.incrementAndGet();
                            if (n % 100 == 0 || n == total) {
                                LOGGER.info("[LiveMap] Tile progress: " + n + "/" + total);
                            }
                        } catch (Throwable t) {
                            failed.incrementAndGet();
                            LOGGER.log(Level.WARNING, "[LiveMap] Tile " + fx + "-" + fy + " failed", t);
                        }
                    });
                }
            }
            pool.shutdown();
            try {
                pool.awaitTermination(30, TimeUnit.MINUTES);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
                throw new IOException("Tile generation interrupted", ie);
            }
        } finally {
            if (!pool.isTerminated()) pool.shutdownNow();
        }

        long elapsed = System.currentTimeMillis() - start;
        LOGGER.info("[LiveMap] Tile generation complete: " + done.get() + "/" + total
                + " in " + elapsed + "ms (" + failed.get() + " failed)");
    }

    private void renderTile(int tileX, int tileY) throws IOException {
        int worldX = tileX * tileSize;
        int worldY = tileY * tileSize;
        // Strided render at native resolution — for the base grid, world == pixel,
        // so strided and direct produce identical output. Using the scaled overload
        // future-proofs against tileSize != world-step setups.
        BufferedImage img = renderer.createMapDumpScaled(
                worldX, worldY, tileSize, tileSize, tileSize, tileSize);
        Path out = outputDir.resolve(tileX + "-" + tileY + ".png");
        Path tmp = outputDir.resolve("." + tileX + "-" + tileY + ".png.tmp");
        ImageIO.write(img, "PNG", tmp.toFile());
        Files.move(tmp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}

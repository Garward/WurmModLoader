package com.garward.wurmmodloader.mods.livemap.renderer;

import java.awt.Color;
import java.awt.image.BufferedImage;

import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.mesh.Tiles.Tile;

/**
 * WurmMapGen-style top-down renderer.
 *
 * <p>Key differences from the prior slope-shade-only renderer:
 * <ul>
 *   <li>Directional hillshade from surface normal dotted with a NW light vector —
 *       gives visible relief even across tiles with the same tile-type.</li>
 *   <li>Depth-graded water: colour interpolates shallow→deep based on mesh depth
 *       rather than a flat blue overlay.</li>
 *   <li>Elevation tint on land: a tiny brightness ramp with height so flat but
 *       elevated plateaus are distinguishable from flat low ground.</li>
 * </ul>
 *
 * <p>Heights are stored in mesh units (1 unit = 0.1m). {@code getSurfaceHeight}
 * returns the raw short unchanged; the prior implementation multiplied by 10,
 * which broke normal-based shading. Do not reintroduce that scaling.
 */
public class LiveMapRenderer {

    private static final float LIGHT_X = -0.577f;
    private static final float LIGHT_Y = -0.577f;
    private static final float LIGHT_Z =  0.577f;

    private static final Color WATER_SHALLOW = new Color(120, 180, 210);
    private static final Color WATER_DEEP    = new Color(10,  25,  70);
    private static final int   DEEP_MESH_UNITS = 400;

    private final MeshIO mesh;

    public LiveMapRenderer(MeshIO mesh) {
        this.mesh = mesh;
    }

    protected short getSurfaceHeight(int x, int y) {
        if (x < 0 || y < 0 || x >= mesh.getSize() || y >= mesh.getSize()) {
            return 0;
        }
        return Tiles.decodeHeight(mesh.getTile(x, y));
    }

    protected Tile getTileType(int x, int y) {
        if (x < 0 || y < 0 || x >= mesh.getSize() || y >= mesh.getSize()) {
            return Tile.TILE_DIRT;
        }
        byte type = Tiles.decodeType(mesh.getTile(x, y));
        Tile t = Tiles.getTile(type);
        return t != null ? t : Tile.TILE_DIRT;
    }

    public BufferedImage createMapDump(int xo, int yo, int lWidth, int lHeight, int highlightX, int highlightY) {
        if (yo < 0) yo = 0;
        if (xo < 0) xo = 0;

        final BufferedImage out = new BufferedImage(lWidth, lHeight, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < lHeight; y++) {
            for (int x = 0; x < lWidth; x++) {
                int worldX = x + xo;
                int worldY = y + yo;

                short h  = getSurfaceHeight(worldX,     worldY);
                short hR = getSurfaceHeight(worldX + 1, worldY);
                short hD = getSurfaceHeight(worldX,     worldY + 1);

                int rgb;
                if (h < 0) {
                    rgb = waterPixel(h);
                } else {
                    rgb = landPixel(worldX, worldY, h, hR, hD);
                }

                if (highlightX == worldX && highlightY == worldY) {
                    rgb = 0xFF0000;
                }

                out.setRGB(x, y, rgb);
            }
        }

        return out;
    }

    public BufferedImage createMapDump(int xo, int yo, int lWidth, int lHeight) {
        return createMapDump(xo, yo, lWidth, lHeight, -1, -1);
    }

    /**
     * Directional hillshade from the surface normal. The normal of a mesh
     * corner is (h - hR, h - hD, 1) (height increases to the right/down
     * producing a normal pointing up-and-away). Normalize and dot with the
     * fixed NW light vector.
     */
    private int landPixel(int wx, int wy, short h, short hR, short hD) {
        float dx = (float) (h - hR);
        float dy = (float) (h - hD);
        float nz = 1.0f;
        float len = (float) Math.sqrt(dx * dx + dy * dy + nz * nz);
        float nxN = dx / len;
        float nyN = dy / len;
        float nzN = nz / len;

        float shade = nxN * LIGHT_X + nyN * LIGHT_Y + nzN * LIGHT_Z;
        if (shade < 0.55f) shade = 0.55f;
        if (shade > 1.35f) shade = 1.35f;

        // Elevation brightness ramp — ~±8% across the 0..800 mesh-unit range
        // so flat plateaus don't all map to the same pixel as flat lowlands.
        float elevBias = 1.0f + Math.min(0.08f, Math.max(-0.04f, (h - 50) / 10000.0f));

        Tile tile = getTileType(wx, wy);
        Color c = tile.getColor();

        float r = (c.getRed()   / 255.0f) * shade * elevBias;
        float g = (c.getGreen() / 255.0f) * shade * elevBias;
        float b = (c.getBlue()  / 255.0f) * shade * elevBias;

        return pack(r, g, b);
    }

    /**
     * Depth-graded water. {@code h} is the mesh short (negative = below sea).
     * Interpolate between shallow and deep water colours by -h / DEEP limit.
     */
    private int waterPixel(short h) {
        float t = Math.min(1.0f, (-h) / (float) DEEP_MESH_UNITS);
        int rr = lerp(WATER_SHALLOW.getRed(),   WATER_DEEP.getRed(),   t);
        int gg = lerp(WATER_SHALLOW.getGreen(), WATER_DEEP.getGreen(), t);
        int bb = lerp(WATER_SHALLOW.getBlue(),  WATER_DEEP.getBlue(),  t);
        return (rr << 16) | (gg << 8) | bb;
    }

    private static int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * t);
    }

    private static int pack(float r, float g, float b) {
        if (r < 0) r = 0; if (r > 1) r = 1;
        if (g < 0) g = 0; if (g > 1) g = 1;
        if (b < 0) b = 0; if (b > 1) b = 1;
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }
}

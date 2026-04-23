package com.garward.wurmmodloader.mods.blueprints;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Minimal DeedPlanner-2 {@code .dpl} reader. Decodes the outer envelope
 * (base64 → gzip → XML, or plain XML) and surfaces a structural summary.
 *
 * <p>Full asset translation (DP shortname → WU template id) lives in a
 * separate mapping layer (TODO); this iteration is parse-only so we can
 * validate files before writing the placer.
 *
 * <p>Reference: {@code deedplanner-2/src/.../data/Map.java#parseMap}.
 */
final class BlueprintLoader {

    private BlueprintLoader() {}

    static Blueprint load(File file) throws Exception {
        byte[] raw = Files.readAllBytes(file.toPath());
        Document doc = parseEnvelope(raw);
        return Blueprint.from(doc);
    }

    private static Document parseEnvelope(byte[] raw) throws Exception {
        // Try base64 → gzip → XML (the modern DP2 compressed format).
        try {
            byte[] decoded = Base64.getDecoder().decode(raw);
            if (decoded.length > 2 && (decoded[0] & 0xff) == 0x1f && (decoded[1] & 0xff) == 0x8b) {
                try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(decoded))) {
                    return newDocBuilder().parse(new ByteArrayInputStream(readAll(gz)));
                }
            }
        } catch (IllegalArgumentException ignored) {
            // not base64 — fall through to plain XML
        }
        // Fall back to plain XML.
        return newDocBuilder().parse(new ByteArrayInputStream(raw));
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static DocumentBuilder newDocBuilder() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        return f.newDocumentBuilder();
    }

    /** Parsed blueprint structure. Placement TBD. */
    static final class Blueprint {
        final int width;
        final int height;
        final List<TileEntry> tiles = new ArrayList<>();

        Blueprint(int w, int h) { this.width = w; this.height = h; }

        static Blueprint from(Document doc) {
            Element root = doc.getDocumentElement();
            int w = intAttr(root, "width", 0);
            int h = intAttr(root, "height", 0);
            Blueprint bp = new Blueprint(w, h);

            NodeList tiles = root.getElementsByTagName("tile");
            for (int i = 0; i < tiles.getLength(); i++) {
                Node n = tiles.item(i);
                if (!(n instanceof Element)) continue;
                Element te = (Element) n;
                TileEntry t = new TileEntry(
                    intAttr(te, "x", 0),
                    intAttr(te, "y", 0),
                    floatAttr(te, "height", 0f));
                t.walls = te.getElementsByTagName("hWall").getLength()
                    + te.getElementsByTagName("vWall").getLength();
                t.borders = te.getElementsByTagName("hBorder").getLength()
                    + te.getElementsByTagName("vBorder").getLength();
                t.objects = te.getElementsByTagName("object").getLength();
                t.floors = te.getElementsByTagName("floor").getLength()
                    + te.getElementsByTagName("roof").getLength();
                bp.tiles.add(t);
            }
            return bp;
        }

        String summarize() {
            int walls = 0, borders = 0, objects = 0, floors = 0;
            for (TileEntry t : tiles) {
                walls += t.walls; borders += t.borders;
                objects += t.objects; floors += t.floors;
            }
            return "size=" + width + "x" + height
                + " tiles=" + tiles.size()
                + " walls=" + walls
                + " borders=" + borders
                + " floors/roofs=" + floors
                + " objects=" + objects;
        }
    }

    static final class TileEntry {
        final int x, y;
        final float height;
        int walls, borders, objects, floors;
        TileEntry(int x, int y, float height) { this.x = x; this.y = y; this.height = height; }
    }

    private static int intAttr(Element e, String name, int def) {
        String v = e.getAttribute(name);
        if (v == null || v.isEmpty()) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException ignored) { return def; }
    }

    private static float floatAttr(Element e, String name, float def) {
        String v = e.getAttribute(name);
        if (v == null || v.isEmpty()) return def;
        try { return Float.parseFloat(v); } catch (NumberFormatException ignored) { return def; }
    }
}

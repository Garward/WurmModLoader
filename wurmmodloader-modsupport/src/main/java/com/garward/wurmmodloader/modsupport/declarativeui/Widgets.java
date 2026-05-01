package com.garward.wurmmodloader.modsupport.declarativeui;

/** Convenience factories for common {@link WidgetNode} shapes. */
public final class Widgets {

    private Widgets() {}

    public static WidgetNode stack(String direction, int gap, int padding, WidgetNode... children) {
        WidgetNode n = new WidgetNode(WidgetNode.STACK_PANEL)
                .prop("direction", direction)
                .prop("gap", gap)
                .prop("padding", padding);
        for (WidgetNode c : children) n.child(c);
        return n;
    }

    public static WidgetNode label(String text) {
        return new WidgetNode(WidgetNode.LABEL).prop("text", text);
    }

    /** Label whose text is replaced when the server sends a BIND update for {@code bindKey}. */
    public static WidgetNode bindLabel(String bindKey, String initialText) {
        return new WidgetNode(WidgetNode.LABEL)
                .prop("text", initialText)
                .prop("bind", bindKey);
    }

    public static WidgetNode button(String label, String action, String hoverText) {
        return new WidgetNode(WidgetNode.BUTTON)
                .prop("label", label)
                .prop("action", action)
                .prop("hover", hoverText);
    }

    public static WidgetNode spacer(int width, int height) {
        return new WidgetNode(WidgetNode.SPACER)
                .prop("width", width)
                .prop("height", height);
    }

    /**
     * Absolute-positioned container. Children read {@code x}/{@code y} (top-left
     * relative to the canvas) and optional {@code width}/{@code height} props.
     * Use for skill trees, maps, free-form layouts.
     */
    public static WidgetNode canvas(int width, int height, WidgetNode... children) {
        WidgetNode n = new WidgetNode(WidgetNode.CANVAS)
                .prop("width", width)
                .prop("height", height);
        for (WidgetNode c : children) n.child(c);
        return n;
    }

    /**
     * Pan-on-drag absolute-positioned container — same child placement rules as
     * {@link #canvas} (children use {@code x}/{@code y}, blips center on point,
     * edges self-size to their bounding box) but the contents scroll on mouse
     * drag of empty viewport space. Use for skill trees, large node graphs, or
     * any layout that overflows its window.
     */
    public static WidgetNode viewport(int width, int height, WidgetNode... children) {
        WidgetNode n = new WidgetNode(WidgetNode.VIEWPORT)
                .prop("width", width)
                .prop("height", height);
        for (WidgetNode c : children) n.child(c);
        return n;
    }

    /**
     * Viewport with a fixed background image that always fills the viewport
     * regardless of pan/zoom. Same {@code src} URI rules as {@link #image}.
     */
    public static WidgetNode viewport(int width, int height, String bgSrc, WidgetNode... children) {
        WidgetNode n = viewport(width, height, children);
        if (bgSrc != null && !bgSrc.isEmpty()) n.prop("bg", bgSrc);
        return n;
    }

    /** Override the viewport's wheel-zoom bounds (defaults: 0.25, 4.0, 1.1). */
    public static WidgetNode zoomBounds(WidgetNode viewport, double min, double max, double step) {
        return viewport.prop("zoomMin", min).prop("zoomMax", max).prop("zoomStep", step);
    }

    /**
     * Switch the viewport's zoom anchor — {@code "center"} (default) keeps
     * content symmetric around the middle; {@code "cursor"} pins the world
     * point under the pointer (familiar map-zoom behaviour).
     */
    public static WidgetNode zoomAnchor(WidgetNode viewport, String mode) {
        return viewport.prop("zoomAnchor", mode);
    }

    /** Initial scale + pan so the window opens already framed on its content. */
    public static WidgetNode initialView(WidgetNode viewport, double scale, int panX, int panY) {
        return viewport.prop("initScale", scale).prop("initPanX", panX).prop("initPanY", panY);
    }

    /** Place an existing node at an absolute (x,y) inside its parent canvas. */
    public static WidgetNode at(int x, int y, WidgetNode child) {
        return child.prop("x", x).prop("y", y);
    }

    /** Same as {@link #at} but also sets explicit width/height on the child. */
    public static WidgetNode at(int x, int y, int width, int height, WidgetNode child) {
        return child.prop("x", x).prop("y", y).prop("width", width).prop("height", height);
    }

    /**
     * Offset placement: position {@code child} at ({@code refX + dx},
     * {@code refY + dy}) relative to a reference point. Sugar over {@link #at}
     * for layouts that build off of named anchors (a node center, a region
     * origin) — keeps the offset arithmetic local to the call site instead of
     * scattered across {@code at(x, y, ...)} computations.
     */
    public static WidgetNode atOffset(int refX, int refY, int dx, int dy, WidgetNode child) {
        return at(refX + dx, refY + dy, child);
    }

    /** Offset placement with explicit width/height. */
    public static WidgetNode atOffset(int refX, int refY, int dx, int dy,
                                      int width, int height, WidgetNode child) {
        return at(refX + dx, refY + dy, width, height, child);
    }

    /**
     * Grid placement: position {@code child} at integer-cell ({@code col},
     * {@code row}) coordinates, snapping to a fixed cell size. Empty cells
     * become visual gaps; adjacent cells touch exactly. Use a single
     * {@code (cellW, cellH)} pair across an entire layout to guarantee every
     * node lands on the same grid lattice.
     *
     * <pre>
     *   final int CELL = 40;
     *   atGrid(origin, origin, 0, 0, CELL, CELL, ...);  // touching
     *   atGrid(origin, origin, 2, 0, CELL, CELL, ...);  // 1-cell gap (X o X)
     *   atGrid(origin, origin, 4, 0, CELL, CELL, ...);  // 3-cell gap (X o o o X)
     * </pre>
     */
    public static WidgetNode atGrid(int originX, int originY, int col, int row,
                                    int cellW, int cellH, WidgetNode child) {
        return at(originX + col * cellW, originY + row * cellH, child);
    }

    /**
     * Static image. {@code src} is one of:
     * <ul>
     *   <li>{@code "<name>"} — short form, resolved against declarativeui's
     *       bundled {@code declarativeui/images/} resource dir
     *       (e.g. {@code "space_bg.jpg"})</li>
     *   <li>{@code "classpath:/path/to.png"} — from any mod's classpath</li>
     *   <li>{@code "file:/abs/path.png"} — from disk</li>
     *   <li>{@code "pack:<packId>/<path>"} — from a serverpack</li>
     * </ul>
     * {@code tint} is "r,g,b,a" (default white).
     */
    public static WidgetNode image(String src, int width, int height, String tint) {
        return new WidgetNode(WidgetNode.IMAGE)
                .prop("src", src)
                .prop("width", width)
                .prop("height", height)
                .prop("tint", tint == null ? "1,1,1,1" : tint);
    }

    public static WidgetNode image(String src, int width, int height) {
        return image(src, width, height, null);
    }

    /**
     * Line primitive. Coordinates are in canvas-local pixels. {@code color} is
     * "r,g,b,a" floats in 0..1 (default white).
     */
    public static WidgetNode edge(int x1, int y1, int x2, int y2, int thickness, String color) {
        return new WidgetNode(WidgetNode.EDGE)
                .prop("x1", x1).prop("y1", y1)
                .prop("x2", x2).prop("y2", y2)
                .prop("thickness", Math.max(1, thickness))
                .prop("color", color == null ? "1,1,1,1" : color);
    }

    /**
     * Filled circle marker — small textured "blip" for skill-tree nodes,
     * map pins, status dots. {@code fill} is "r,g,b,a" (default white).
     * Set a tooltip by chaining {@link WidgetNode#tooltip(String)} on the
     * returned node — Wurm's HUD popup will show on hover.
     */
    public static WidgetNode blip(int diameter, String fill) {
        return new WidgetNode(WidgetNode.BLIP)
                .prop("diameter", Math.max(2, diameter))
                .prop("fill", fill == null ? "1,1,1,1" : fill);
    }

    /**
     * Filled circle with outline. {@code fill} and {@code outline} are
     * "r,g,b,a" floats in 0..1.
     */
    public static WidgetNode blip(int diameter, String fill, int outlineThickness, String outline) {
        return new WidgetNode(WidgetNode.BLIP)
                .prop("diameter", Math.max(2, diameter))
                .prop("fill", fill == null ? "1,1,1,1" : fill)
                .prop("outlineThickness", Math.max(0, outlineThickness))
                .prop("outline", outline == null ? "0,0,0,1" : outline);
    }

    /**
     * Make a widget clickable. Sets the {@code action} prop, which the client
     * dispatches as a {@code ui:action} ModActionEvent on left-click. Currently
     * supported on {@code blip}, {@code frame} (and implicitly on {@code button},
     * where action is set at construction).
     */
    public static WidgetNode action(WidgetNode node, String action) {
        if (action != null && !action.isEmpty()) node.prop("action", action);
        return node;
    }

    /**
     * Add a soft glow pass behind an {@link #edge}. {@code glowThickness} is
     * the full glow width and should be larger than the edge's own thickness
     * or it disappears under the inner line. {@code glowColor} is "r,g,b,a" —
     * a low-alpha tint of the edge color is the usual choice.
     */
    public static WidgetNode glowEdge(WidgetNode edge, int glowThickness, String glowColor) {
        return edge.prop("glowThickness", Math.max(0, glowThickness))
                .prop("glow", glowColor == null ? "1,1,1,0.4" : glowColor);
    }

    /**
     * Square or circle frame container. Holds at most one child, sized to fit
     * inside the border. Use shape {@code "square"} or {@code "circle"}; any
     * other value falls back to square. {@code bg} is "r,g,b,a" for the inner
     * fill (set alpha 0 for transparent), {@code outline} for the border.
     *
     * <p>Make the frame clickable by chaining {@link #action(WidgetNode, String)}
     * — without an action it's input-transparent so a containing
     * {@link #viewport} keeps drag-to-pan over the frame body.
     */
    public static WidgetNode frame(String shape, int width, int height,
                                   int borderThickness,
                                   String bg, String outline,
                                   WidgetNode child) {
        WidgetNode n = new WidgetNode(WidgetNode.FRAME)
                .prop("shape", shape == null ? "square" : shape)
                .prop("width", width)
                .prop("height", height)
                .prop("borderThickness", Math.max(1, borderThickness))
                .prop("bg", bg == null ? "0,0,0,0" : bg)
                .prop("outline", outline == null ? "1,1,1,1" : outline);
        if (child != null) n.child(child);
        return n;
    }

    /** Convenience: square frame with no fill. */
    public static WidgetNode squareFrame(int width, int height, int borderThickness,
                                         String outline, WidgetNode child) {
        return frame("square", width, height, borderThickness, "0,0,0,0", outline, child);
    }

    /** Convenience: circle frame with no fill. */
    public static WidgetNode circleFrame(int diameter, int borderThickness,
                                         String outline, WidgetNode child) {
        return frame("circle", diameter, diameter, borderThickness, "0,0,0,0", outline, child);
    }

    /**
     * Soft radial-gradient glow circle — drops behind a node to suggest status
     * (selected, available, on cooldown). {@code color} is "r,g,b,a" applied
     * uniformly; the rim alpha-fades to transparent regardless. Always
     * input-transparent so clicks pass through to whatever is layered on top.
     */
    public static WidgetNode halo(int diameter, String color) {
        return new WidgetNode(WidgetNode.HALO)
                .prop("diameter", Math.max(4, diameter))
                .prop("color", color == null ? "1,1,1,0.6" : color);
    }
}

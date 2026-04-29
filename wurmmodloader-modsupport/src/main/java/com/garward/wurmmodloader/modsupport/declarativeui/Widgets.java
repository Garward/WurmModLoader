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
}

package com.garward.wurmmodloader.modsupport.declarativeui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side widget tree node. Mirror of the client's internal representation,
 * exposed as public API so server mods can build trees to ship to clients via
 * the {@code com.garward.ui} ModComm channel.
 *
 * <p>Prefer {@link Widgets} for concise construction:
 * <pre>
 *   WidgetNode root = Widgets.stack("vertical", 4, 4,
 *       Widgets.label("Hello"),
 *       Widgets.bindLabel("score", "0"),
 *       Widgets.button("Close", "close_window", null));
 * </pre>
 */
public final class WidgetNode {

    public static final String LABEL = "Label";
    public static final String BUTTON = "Button";
    public static final String STACK_PANEL = "StackPanel";
    public static final String SPACER = "Spacer";
    public static final String CANVAS = "Canvas";
    public static final String EDGE = "Edge";
    public static final String IMAGE = "Image";
    public static final String BLIP = "Blip";
    public static final String VIEWPORT = "Viewport";

    public final String type;
    public final Map<String, String> props = new LinkedHashMap<>();
    public final List<WidgetNode> children = new ArrayList<>();

    public WidgetNode(String type) {
        this.type = type;
    }

    /**
     * Set the hover tooltip on this widget. Multi-line: separate with {@code \n}.
     * Honored by every visible widget (Label/Button/Image/Edge/Blip/...).
     */
    public WidgetNode tooltip(String text) {
        if (text != null && !text.isEmpty()) props.put("tooltip", text);
        return this;
    }

    public WidgetNode prop(String key, String value) {
        if (value != null) props.put(key, value);
        return this;
    }

    public WidgetNode prop(String key, int value) {
        props.put(key, Integer.toString(value));
        return this;
    }

    public WidgetNode prop(String key, double value) {
        props.put(key, Double.toString(value));
        return this;
    }

    public WidgetNode child(WidgetNode node) {
        if (node != null) children.add(node);
        return this;
    }
}

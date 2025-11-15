package org.gotti.wurmunlimited.modsupport.bml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.List;

/**
 * Legacy BmlBuilder wrapper for backward compatibility.
 * Delegates all methods to the new implementation.
 */
public final class BmlBuilder {

    private final com.garward.wurmmodloader.modsupport.bml.BmlBuilder internal;

    private BmlBuilder() {
        this.internal = com.garward.wurmmodloader.modsupport.bml.BmlBuilder.builder();
    }

    public static BmlBuilder builder() {
        return new BmlBuilder();
    }

    public BmlBuilder wrapAsDialog(String title, boolean horizontalScroll, boolean verticalScroll, boolean rescale) {
        internal.wrapAsDialog(title, horizontalScroll, verticalScroll, rescale);
        return this;
    }

    public static BmlNodeBuilder border(BmlNodeBuilder north, BmlNodeBuilder west, BmlNodeBuilder center, BmlNodeBuilder east, BmlNodeBuilder south) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.border(
            north != null ? north.internal : null,
            west != null ? west.internal : null,
            center != null ? center.internal : null,
            east != null ? east.internal : null,
            south != null ? south.internal : null
        ));
    }

    public static BmlNodeBuilder label(String text) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.label(text));
    }

    public static BmlNodeBuilder label(String text, TextStyle style) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.label(text,
            com.garward.wurmmodloader.modsupport.bml.TextStyle.valueOf(style.name())));
    }

    public static BmlNodeBuilder text(String text) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.text(text));
    }

    public static BmlNodeBuilder text(String text, TextStyle style) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.text(text,
            com.garward.wurmmodloader.modsupport.bml.TextStyle.valueOf(style.name())));
    }

    public static BmlNodeBuilder header(String text) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.header(text));
    }

    public static BmlNodeBuilder center(BmlNodeBuilder node) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.center(node.internal));
    }

    public static BmlNodeBuilder left(BmlNodeBuilder node) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.left(node.internal));
    }

    public static BmlNodeBuilder right(BmlNodeBuilder node) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.right(node.internal));
    }

    public static BmlNodeBuilder passthrough(String id, String text) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.passthough(id, text));
    }

    public static BmlNodeBuilder input(String id) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.input(id));
    }

    public static BmlNodeBuilder dropdown(String id, String... options) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.dropdown(id, options));
    }

    public static BmlNodeBuilder button(String id, String text) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.button(id, text));
    }

    public static BmlNodeBuilder radio(String id, String text) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.radio(id, text));
    }

    public static BmlNodeBuilder checkbox(String id, String text) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.checkbox(id, text));
    }

    public static BmlNodeBuilder table(int cols) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.table(cols));
    }

    public static BmlNodeBuilder harray() {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.harray());
    }

    public static BmlNodeBuilder harray(boolean rescale) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.harray(rescale));
    }

    public static BmlNodeBuilder varray(boolean rescale) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.varray(rescale));
    }

    public static BmlNodeBuilder scroll(boolean horizontal, boolean vertical, BmlNodeBuilder node) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlBuilder.scroll(horizontal, vertical, node.internal));
    }

    public BmlBuilder withNode(BmlNodeBuilder nodeBuilder) {
        internal.withNode(nodeBuilder.internal);
        return this;
    }

    public BmlBuilder withNodes(BmlBuilder nodes) {
        if (nodes != null) {
            for (com.garward.wurmmodloader.modsupport.bml.BmlNodeBuilder node : nodes.internal.getNodeBuilders()) {
                internal.withNode(node);
            }
        }
        return this;
    }

    public String buildBml() {
        return internal.buildBml();
    }

    public List<BmlNodeBuilder> getNodeBuilders() {
        return internal.getNodeBuilders().stream()
            .map(BmlNodeBuilder::new)
            .collect(java.util.stream.Collectors.toList());
    }

    public Element build(Document document) {
        return internal.build(document);
    }
}

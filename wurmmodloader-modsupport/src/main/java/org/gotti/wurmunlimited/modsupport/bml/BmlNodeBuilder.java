package org.gotti.wurmunlimited.modsupport.bml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.List;

/**
 * Legacy BmlNodeBuilder wrapper for backward compatibility.
 * Wraps the internal implementation.
 */
public class BmlNodeBuilder {

    final com.garward.wurmmodloader.modsupport.bml.BmlNodeBuilder internal;

    BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlNodeBuilder internal) {
        this.internal = internal;
    }

    public static BmlNodeBuilder builder(String type) {
        return new BmlNodeBuilder(com.garward.wurmmodloader.modsupport.bml.BmlNodeBuilder.builder(type));
    }

    public BmlNodeBuilder withAttribute(String name, String value) {
        internal.withAttribute(name, value);
        return this;
    }

    public BmlNodeBuilder withAttribute(String name, int value) {
        internal.withAttribute(name, value);
        return this;
    }

    public BmlNodeBuilder withAttribute(String name, boolean value) {
        internal.withAttribute(name, value);
        return this;
    }

    public BmlNodeBuilder red() {
        internal.red();
        return this;
    }

    public BmlNodeBuilder green() {
        internal.green();
        return this;
    }

    public BmlNodeBuilder blue() {
        internal.blue();
        return this;
    }

    public BmlNodeBuilder color(int red, int green, int blue) {
        internal.color(red, green, blue);
        return this;
    }

    public BmlNodeBuilder size(int width, int height) {
        internal.size(width, height);
        return this;
    }

    public Element build(Document document) {
        return internal.build(document);
    }

    public BmlNodeBuilder withNode(BmlNodeBuilder nodeBuilder) {
        internal.withNode(nodeBuilder != null ? nodeBuilder.internal : null);
        return this;
    }

    public BmlNodeBuilder withNode(String type) {
        internal.withNode(type);
        return this;
    }

    public BmlNodeBuilder withNodes(BmlBuilder bmlBuilder) {
        if (bmlBuilder != null) {
            for (BmlNodeBuilder node : bmlBuilder.getNodeBuilders()) {
                withNode(node);
            }
        }
        return this;
    }

    public List<BmlNodeBuilder> buildNodes() {
        return internal.buildNodes().stream()
            .map(BmlNodeBuilder::new)
            .collect(java.util.stream.Collectors.toList());
    }

    public String buildBml() {
        return internal.buildBml();
    }
}

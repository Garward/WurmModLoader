package com.garward.wurmmodloader.modsupport.bml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A builder class for creating BML (Blackmark XML) nodes that can be used to construct
 * user interface elements for Wurm Unlimited mods.
 * 
 * <p>This class provides a fluent API for building BML nodes with attributes and child nodes.
 * It supports both XML DOM element creation and BML string generation.</p>
 * 
 * <p><strong>Usage example:</strong></p>
 * <pre><code>
 * // Create a simple label with attributes
 * BmlNodeBuilder label = BmlNodeBuilder.builder("label")
 *     .withAttribute("text", "Hello World")
 *     .withAttribute("font", "sansserif")
 *     .red();
 * 
 * // Create a container with child nodes
 * BmlNodeBuilder container = BmlNodeBuilder.builder("div")
 *     .withAttribute("id", "main-container")
 *     .size(400, 300)
 *     .withNode(label);
 * 
 * // Generate BML string
 * String bmlString = container.buildBml();
 * 
 * // Or build as XML DOM element
 * Document doc = ... // obtain XML document
 * Element xmlElement = container.build(doc);
 * </code></pre>
 * 
 * <p><strong>Thread Safety:</strong> This class is NOT thread-safe. Each instance should
 * only be accessed by a single thread. Multiple threads can safely create separate instances.</p>
 * 
 * @since 1.0.0
 * @see BmlBuilder
 */
public class BmlNodeBuilder {

	private final String type;
	private LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
	private List<BmlNodeBuilder> nodes = new ArrayList<>();
	
	/**
	 * Constructs a new BmlNodeBuilder with the specified node type.
	 * 
	 * <p>This constructor is private to enforce the use of the {@link #builder(String)} factory method.</p>
	 * 
	 * @param type the type of BML node to create (e.g., "label", "button", "div")
	 * @since 1.0.0
	 */
	private BmlNodeBuilder(String type) {
		this.type = type;
	}

	/**
	 * Creates a new BmlNodeBuilder instance for the specified node type.
	 * 
	 * <p>This is the preferred way to create new BmlNodeBuilder instances.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder button = BmlNodeBuilder.builder("button");
	 * BmlNodeBuilder label = BmlNodeBuilder.builder("label");
	 * </code></pre>
	 * 
	 * @param type the type of BML node to create
	 * @return a new BmlNodeBuilder instance
	 * @throws IllegalArgumentException if type is null or empty
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder builder(String type) {
		return new BmlNodeBuilder(type);
	}

	/**
	 * Adds a string attribute to this node.
	 * 
	 * <p>If an attribute with the same name already exists, it will be replaced.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder label = BmlNodeBuilder.builder("label")
	 *     .withAttribute("text", "Hello World")
	 *     .withAttribute("id", "greeting-label");
	 * </code></pre>
	 * 
	 * @param name the attribute name
	 * @param value the attribute value
	 * @return this builder instance for method chaining
	 * @throws IllegalArgumentException if name is null or empty
	 * @since 1.0.0
	 */
	public BmlNodeBuilder withAttribute(String name, String value) {
		attributes.put(name, value);
		return this;
	}
	
	/**
	 * Adds an integer attribute to this node.
	 * 
	 * <p>The integer value will be converted to its string representation.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder input = BmlNodeBuilder.builder("input")
	 *     .withAttribute("maxlength", 100)
	 *     .withAttribute("tabindex", 1);
	 * </code></pre>
	 * 
	 * @param name the attribute name
	 * @param value the attribute value as an integer
	 * @return this builder instance for method chaining
	 * @throws IllegalArgumentException if name is null or empty
	 * @since 1.0.0
	 */
	public BmlNodeBuilder withAttribute(String name, int value) {
		attributes.put(name, String.valueOf(value));
		return this;
	}

	/**
	 * Adds a boolean attribute to this node.
	 * 
	 * <p>The boolean value will be converted to its string representation ("true" or "false").</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder button = BmlNodeBuilder.builder("button")
	 *     .withAttribute("enabled", true)
	 *     .withAttribute("visible", false);
	 * </code></pre>
	 * 
	 * @param name the attribute name
	 * @param value the attribute value as a boolean
	 * @return this builder instance for method chaining
	 * @throws IllegalArgumentException if name is null or empty
	 * @since 1.0.0
	 */
	public BmlNodeBuilder withAttribute(String name, boolean value) {
		attributes.put(name, String.valueOf(value));
		return this;
	}
	
	/**
	 * Sets the color attribute to red (255,0,0).
	 * 
	 * <p>This is a convenience method for quickly setting a red color.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder label = BmlNodeBuilder.builder("label")
	 *     .withAttribute("text", "Error!")
	 *     .red();
	 * </code></pre>
	 * 
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see #color(int, int, int)
	 */
	public BmlNodeBuilder red() {
		return color(255, 0, 0);
	}
	
	/**
	 * Sets the color attribute to green (0,255,0).
	 * 
	 * <p>This is a convenience method for quickly setting a green color.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder label = BmlNodeBuilder.builder("label")
	 *     .withAttribute("text", "Success!")
	 *     .green();
	 * </code></pre>
	 * 
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see #color(int, int, int)
	 */
	public BmlNodeBuilder green() {
		return color(0, 255, 0);
	}
	
	/**
	 * Sets the color attribute to blue (0,0,255).
	 * 
	 * <p>This is a convenience method for quickly setting a blue color.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder label = BmlNodeBuilder.builder("label")
	 *     .withAttribute("text", "Information")
	 *     .blue();
	 * </code></pre>
	 * 
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see #color(int, int, int)
	 */
	public BmlNodeBuilder blue() {
		return color(0, 0, 255);
	}
	
	/**
	 * Sets the color attribute using RGB values.
	 * 
	 * <p>The color will be formatted as "red,green,blue" (e.g., "255,128,0").</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder label = BmlNodeBuilder.builder("label")
	 *     .withAttribute("text", "Custom Color")
	 *     .color(255, 128, 0); // Orange
	 * </code></pre>
	 * 
	 * @param red the red component (0-255)
	 * @param green the green component (0-255)
	 * @param blue the blue component (0-255)
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see #red()
	 * @see #green()
	 * @see #blue()
	 */
	public BmlNodeBuilder color(int red, int green, int blue) {
		return withAttribute("color", String.join(",", String.valueOf(red), String.valueOf(green), String.valueOf(blue)));
	}
	
	/**
	 * Sets the size attribute using width and height values.
	 * 
	 * <p>The size will be formatted as "width,height" (e.g., "400,300").</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder panel = BmlNodeBuilder.builder("div")
	 *     .size(800, 600);
	 * </code></pre>
	 * 
	 * @param width the width in pixels
	 * @param height the height in pixels
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public BmlNodeBuilder size(int width, int height) {
		return withAttribute("size", String.join(",", String.valueOf(width), String.valueOf(height)));
	}

	/**
	 * Builds this node as an XML DOM Element.
	 * 
	 * <p>This method recursively builds all child nodes and adds them to the element.
	 * All attributes are set on the created element.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
	 * BmlNodeBuilder node = BmlNodeBuilder.builder("label").withAttribute("text", "Hello");
	 * Element element = node.build(document);
	 * </code></pre>
	 * 
	 * @param document the XML Document to use for creating the Element
	 * @return the constructed XML Element
	 * @throws IllegalArgumentException if document is null
	 * @since 1.0.0
	 */
	public Element build(Document document) {
		Element element = document.createElement(type);
		attributes.forEach((name, value) -> element.setAttribute(name, value));
		nodes.forEach(child -> element.appendChild(child.build(document)));
		return element;
	}

	/**
	 * Adds a child node to this node.
	 * 
	 * <p>If the provided node builder is null, a "null" node will be added instead.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder child = BmlNodeBuilder.builder("label").withAttribute("text", "Child");
	 * BmlNodeBuilder parent = BmlNodeBuilder.builder("div").withNode(child);
	 * </code></pre>
	 * 
	 * @param nodeBuilder the child node builder to add
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see #withNode(String)
	 */
	public BmlNodeBuilder withNode(BmlNodeBuilder nodeBuilder) {
		if (nodeBuilder == null) {
			return withNode("null");
		} else {
			nodes.add(nodeBuilder);
			return this;
		}
	}

	/**
	 * Adds a child node with the specified type to this node.
	 * 
	 * <p>This is a convenience method that creates a new builder for the specified type
	 * and adds it as a child node.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder parent = BmlNodeBuilder.builder("div")
	 *     .withNode("label")
	 *     .withNode("button");
	 * </code></pre>
	 * 
	 * @param type the type of child node to create and add
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see #withNode(BmlNodeBuilder)
	 */
	public BmlNodeBuilder withNode(String type) {
		return withNode(BmlNodeBuilder.builder(type));
	}

	/**
	 * Adds all nodes from the specified BmlBuilder as child nodes.
	 * 
	 * <p>This method allows combining nodes from another BmlBuilder instance.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlBuilder otherBuilder = ... // some other builder with nodes
	 * BmlNodeBuilder container = BmlNodeBuilder.builder("div")
	 *     .withNodes(otherBuilder);
	 * </code></pre>
	 * 
	 * @param bmlBuilder the BmlBuilder whose nodes should be added
	 * @return this builder instance for method chaining
	 * @throws IllegalArgumentException if bmlBuilder is null
	 * @since 1.0.0
	 * @see BmlBuilder#getNodeBuilders()
	 */
	public BmlNodeBuilder withNodes(BmlBuilder bmlBuilder) {
		nodes.addAll(bmlBuilder.getNodeBuilders());
		return this;
	}

	/**
	 * Returns an unmodifiable list of child node builders.
	 * 
	 * <p>This method provides read-only access to the child nodes of this builder.
	 * Modifications to the returned list will not affect the internal state of this builder.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder parent = BmlNodeBuilder.builder("div")
	 *     .withNode("label")
	 *     .withNode("button");
	 * 
	 * List<BmlNodeBuilder> children = parent.buildNodes();
	 * System.out.println("Number of children: " + children.size());
	 * </code></pre>
	 * 
	 * @return an unmodifiable list of child node builders
	 * @since 1.0.0
	 */
	public List<BmlNodeBuilder> buildNodes() {
		return Collections.unmodifiableList(nodes);
	}

	/**
	 * Builds this node and all its children into a BML string representation.
	 * 
	 * <p>The generated BML follows the format: type{attribute="value";child1;child2{...};}</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre><code>
	 * BmlNodeBuilder node = BmlNodeBuilder.builder("label")
	 *     .withAttribute("text", "Hello")
	 *     .withAttribute("color", "255,0,0");
	 * 
	 * String bml = node.buildBml();
	 * // Result: label{text="Hello";color="255,0,0";}
	 * </code></pre>
	 * 
	 * @return the BML string representation of this node and its children
	 * @since 1.0.0
	 */
	public String buildBml() {
		StringBuilder builder = new StringBuilder();
		if (!nodes.isEmpty() || !attributes.isEmpty()) {
			builder.append(type);
			builder.append("{");
			attributes.forEach((name, value) -> builder.append(name + "=\"" + value + "\";"));
			nodes.forEach(node -> builder.append(node.buildBml()));
			builder.append("}");
		} else {
			builder.append(type + ";");
		}
		return builder.toString();
	}
}
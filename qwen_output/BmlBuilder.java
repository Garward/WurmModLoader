package com.garward.wurmmodloader.modsupport.bml;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A builder class for creating BML (Browser Markup Language) structures used in Wurm Unlimited modding.
 * 
 * <p>This class provides a fluent API for constructing BML documents that can be used to create
 * user interfaces in Wurm Unlimited. It follows the builder pattern to allow for easy composition
 * of complex UI structures.</p>
 * 
 * <p>The builder creates a hierarchical structure of {@link BmlNodeBuilder} instances that can be
 * converted to BML strings or DOM elements for use in the game.</p>
 * 
 * <h3>Usage Examples:</h3>
 * 
 * <pre>{@code
 * // Create a simple dialog with text
 * String bml = BmlBuilder.builder()
 *     .withNode(BmlBuilder.label("Hello World"))
 *     .buildBml();
 * 
 * // Create a more complex dialog with multiple elements
 * String bml = BmlBuilder.builder()
 *     .withNode(BmlBuilder.border(
 *         BmlBuilder.header("My Dialog"),
 *         null,
 *         BmlBuilder.scroll(true, true, 
 *             BmlBuilder.varray(true)
 *                 .withNode(BmlBuilder.label("First line"))
 *                 .withNode(BmlBuilder.label("Second line"))),
 *         null,
 *         BmlBuilder.button("ok", "OK")))
 *     .buildBml();
 * }</pre>
 * 
 * <p>This class is not thread-safe. Each instance should only be used by a single thread at a time.
 * However, multiple instances can be safely used in parallel.</p>
 * 
 * @since 1.0.0
 * @see BmlNodeBuilder
 * @see TextStyle
 */
public class BmlBuilder {
	
	/**
	 * Root node to work on.
	 */
	private final BmlNodeBuilder root = BmlNodeBuilder.builder("");
	
	/**
	 * Private constructor to enforce use of the {@link #builder()} factory method.
	 * 
	 * <p>This constructor initializes an empty BML builder with a root node that has
	 * an empty name.</p>
	 * 
	 * @since 1.0.0
	 */
	private BmlBuilder() {
	}
	
	/**
	 * Creates a new empty BML builder instance.
	 * 
	 * <p>This is the primary factory method for creating new BML builders. It returns
	 * a fresh instance that can be used to construct BML structures.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlBuilder builder = BmlBuilder.builder();
	 * }</pre>
	 * 
	 * @return a new {@code BmlBuilder} instance
	 * @since 1.0.0
	 */
	public static BmlBuilder builder() {
		return new BmlBuilder();
	}
	
	/**
	 * Wraps the current builder contents in a standard dialog structure.
	 * 
	 * <p>This method creates a border layout with a header containing the title,
	 * a scrollable content area in a vertical array, and empty side components.
	 * This is a convenience method for creating standard dialogs.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * String bml = BmlBuilder.builder()
	 *     .withNode(BmlBuilder.label("Dialog content"))
	 *     .wrapAsDialog("My Dialog", false, true, true)
	 *     .buildBml();
	 * }</pre>
	 * 
	 * @param title the dialog title to display in the header
	 * @param horizontalScroll whether to enable horizontal scrolling in the content area
	 * @param verticalScroll whether to enable vertical scrolling in the content area
	 * @param rescale whether to enable vertical rescaling of the content area
	 * @return a new {@code BmlBuilder} instance containing the wrapped dialog structure
	 * @since 1.0.0
	 */
	public BmlBuilder wrapAsDialog(String title, boolean horizontalScroll, boolean verticalScroll, boolean rescale) {
		final BmlNodeBuilder border = border(
				center(text(title, TextStyle.BOLD)),
				null,
				scroll(horizontalScroll, verticalScroll, varray(rescale).withNodes(this)),
				null,
				null);
		return builder().withNode(border);
	}
	
	/**
	 * Creates a border layout node builder with the specified components.
	 * 
	 * <p>A border layout arranges components in five regions: north, south, east, west, and center.
	 * Each region can contain at most one component. This is commonly used for dialog layouts.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder dialog = BmlBuilder.border(
	 *     BmlBuilder.header("Title"),     // north
	 *     null,                           // west
	 *     BmlBuilder.label("Content"),    // center
	 *     null,                           // east
	 *     BmlBuilder.button("ok", "OK")); // south
	 * }</pre>
	 * 
	 * @param north the component to place in the north region (top), or null for none
	 * @param west the component to place in the west region (left), or null for none
	 * @param center the component to place in the center region, or null for none
	 * @param east the component to place in the east region (right), or null for none
	 * @param south the component to place in the south region (bottom), or null for none
	 * @return a new {@code BmlNodeBuilder} configured as a border layout
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder border(BmlNodeBuilder north, BmlNodeBuilder west, BmlNodeBuilder center, BmlNodeBuilder east, BmlNodeBuilder south) {
		return BmlNodeBuilder.builder("border")
				.withNode(north)
				.withNode(west)
				.withNode(center)
				.withNode(east)
				.withNode(south);
	}

	/**
	 * Creates a label node builder with the specified text.
	 * 
	 * <p>A label is a simple text display component that shows static text to the user.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder label = BmlBuilder.label("This is a label");
	 * }</pre>
	 * 
	 * @param text the text to display in the label
	 * @return a new {@code BmlNodeBuilder} configured as a label
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder label(String text) {
		return BmlNodeBuilder.builder("label").withAttribute("text", text);
	}
	
	/**
	 * Creates a label node builder with the specified text and style.
	 * 
	 * <p>A label is a simple text display component that shows static text to the user
	 * with the specified formatting style.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder label = BmlBuilder.label("This is a bold label", TextStyle.BOLD);
	 * }</pre>
	 * 
	 * @param text the text to display in the label
	 * @param style the text style to apply to the label
	 * @return a new {@code BmlNodeBuilder} configured as a styled label
	 * @since 1.0.0
	 * @see TextStyle
	 */
	public static BmlNodeBuilder label(String text, TextStyle style) {
		return label(text).withAttribute("type", style.getType());
	}
	
	/**
	 * Creates a text node builder with the specified text.
	 * 
	 * <p>A text component is similar to a label but may have different rendering characteristics
	 * depending on the BML parser implementation.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder text = BmlBuilder.text("This is text content");
	 * }</pre>
	 * 
	 * @param text the text to display
	 * @return a new {@code BmlNodeBuilder} configured as a text component
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder text(String text) {
		return BmlNodeBuilder.builder("text").withAttribute("text", text);
	}
	
	/**
	 * Creates a text node builder with the specified text and style.
	 * 
	 * <p>A text component is similar to a label but may have different rendering characteristics
	 * depending on the BML parser implementation, with the specified formatting style.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder text = BmlBuilder.text("This is bold text", TextStyle.BOLD);
	 * }</pre>
	 * 
	 * @param text the text to display
	 * @param style the text style to apply
	 * @return a new {@code BmlNodeBuilder} configured as a styled text component
	 * @since 1.0.0
	 * @see TextStyle
	 */
	public static BmlNodeBuilder text(String text, TextStyle style) {
		return text(text).withAttribute("type", style.getType());
	}
	
	/**
	 * Creates a header node builder with the specified text.
	 * 
	 * <p>A header is typically used for section titles or dialog headers and may be
	 * rendered with larger or emphasized text.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder header = BmlBuilder.header("Section Title");
	 * }</pre>
	 * 
	 * @param text the header text to display
	 * @return a new {@code BmlNodeBuilder} configured as a header
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder header(String text) {
		return BmlNodeBuilder.builder("header").withAttribute("text", text);
	}
	
	/**
	 * Creates a center alignment node builder containing the specified child node.
	 * 
	 * <p>This component centers its child horizontally and vertically within its container.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder centered = BmlBuilder.center(BmlBuilder.label("Centered Text"));
	 * }</pre>
	 * 
	 * @param node the child node to center
	 * @return a new {@code BmlNodeBuilder} configured as a center alignment container
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder center(BmlNodeBuilder node) {
		return BmlNodeBuilder.builder("center").withNode(node);
	}
	
	/**
	 * Creates a left alignment node builder containing the specified child node.
	 * 
	 * <p>This component aligns its child to the left edge of its container.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder leftAligned = BmlBuilder.left(BmlBuilder.label("Left-aligned Text"));
	 * }</pre>
	 * 
	 * @param node the child node to left-align
	 * @return a new {@code BmlNodeBuilder} configured as a left alignment container
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder left(BmlNodeBuilder node) {
		return BmlNodeBuilder.builder("left").withNode(node);
	}

	/**
	 * Creates a right alignment node builder containing the specified child node.
	 * 
	 * <p>This component aligns its child to the right edge of its container.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder rightAligned = BmlBuilder.right(BmlBuilder.label("Right-aligned Text"));
	 * }</pre>
	 * 
	 * @param node the child node to right-align
	 * @return a new {@code BmlNodeBuilder} configured as a right alignment container
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder right(BmlNodeBuilder node) {
		return BmlNodeBuilder.builder("right").withNode(node);
	}
	
	/**
	 * Creates a passthrough node builder with the specified ID and text value.
	 * 
	 * <p>A passthrough component is used to pass data values that can be referenced
	 * by ID in event handling or other processing.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder passthrough = BmlBuilder.passthough("dataId", "dataValue");
	 * }</pre>
	 * 
	 * @param id the identifier for this passthrough component
	 * @param text the value to pass through
	 * @return a new {@code BmlNodeBuilder} configured as a passthrough component
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder passthough(String id, String text) {
		return BmlNodeBuilder.builder("passthrough").withAttribute("id", "id").withAttribute("text", text);
	}
	
	/**
	 * Creates a text input node builder with the specified ID.
	 * 
	 * <p>An input component allows the user to enter text. The ID is used to reference
	 * the input value in event handling.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder input = BmlBuilder.input("username");
	 * }</pre>
	 * 
	 * @param id the identifier for this input component
	 * @return a new {@code BmlNodeBuilder} configured as a text input
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder input(String id) {
		return BmlNodeBuilder.builder("input").withAttribute("id", id);
	}
	
	/**
	 * Creates a dropdown node builder with the specified ID and options.
	 * 
	 * <p>A dropdown component presents a list of options from which the user can select one.
	 * The options are provided as a comma-separated string.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder dropdown = BmlBuilder.dropdown("color", "Red", "Green", "Blue");
	 * }</pre>
	 * 
	 * @param id the identifier for this dropdown component
	 * @param options the available selection options
	 * @return a new {@code BmlNodeBuilder} configured as a dropdown
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder dropdown(String id, String... options) {
		return BmlNodeBuilder.builder("dropdown").withAttribute("id", id).withAttribute("options", String.join(",", options));
	}
	
	/**
	 * Creates a button node builder with the specified ID and text.
	 * 
	 * <p>A button component can be clicked by the user to trigger actions. The ID is
	 * used to identify which button was clicked in event handling.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder button = BmlBuilder.button("submit", "Submit");
	 * }</pre>
	 * 
	 * @param id the identifier for this button component
	 * @param text the button label text
	 * @return a new {@code BmlNodeBuilder} configured as a button
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder button(String id, String text) {
		return BmlNodeBuilder.builder("button").withAttribute("text", text).withAttribute("id", id);
	}
	
	/**
	 * Creates a radio button node builder with the specified ID and text.
	 * 
	 * <p>A radio button is part of a group where only one button can be selected at a time.
	 * Radio buttons with the same ID are considered part of the same group.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder radio = BmlBuilder.radio("choice", "Option 1");
	 * }</pre>
	 * 
	 * @param id the identifier for this radio button component
	 * @param text the radio button label text
	 * @return a new {@code BmlNodeBuilder} configured as a radio button
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder radio(String id, String text) {
		return BmlNodeBuilder.builder("radio").withAttribute("id", id).withAttribute("text", text);
	}
	
	/**
	 * Creates a checkbox node builder with the specified ID and text.
	 * 
	 * <p>A checkbox can be independently checked or unchecked. Multiple checkboxes
	 * can be selected simultaneously.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder checkbox = BmlBuilder.checkbox("agree", "I agree to the terms");
	 * }</pre>
	 * 
	 * @param id the identifier for this checkbox component
	 * @param text the checkbox label text
	 * @return a new {@code BmlNodeBuilder} configured as a checkbox
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder checkbox(String id, String text) {
		return BmlNodeBuilder.builder("checkbox").withAttribute("id", id).withAttribute("text", text);
	}
	
	/**
	 * Creates a table node builder with the specified number of columns.
	 * 
	 * <p>A table arranges its child components in a grid with the specified number
	 * of columns. Child nodes are added in row-major order.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder table = BmlBuilder.table(3)
	 *     .withNode(BmlBuilder.label("Cell 1"))
	 *     .withNode(BmlBuilder.label("Cell 2"))
	 *     .withNode(BmlBuilder.label("Cell 3"));
	 * }</pre>
	 * 
	 * @param cols the number of columns in the table
	 * @return a new {@code BmlNodeBuilder} configured as a table
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder table(int cols) {
		return BmlNodeBuilder.builder("table").withAttribute("cols", cols);
	}
	
	/**
	 * Creates a horizontal array node builder.
	 * 
	 * <p>A horizontal array arranges its child components in a horizontal line
	 * from left to right.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder harray = BmlBuilder.harray()
	 *     .withNode(BmlBuilder.button("yes", "Yes"))
	 *     .withNode(BmlBuilder.button("no", "No"));
	 * }</pre>
	 * 
	 * @return a new {@code BmlNodeBuilder} configured as a horizontal array
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder harray() {
		return BmlNodeBuilder.builder("harray");
	}
	
	/**
	 * Creates a horizontal array node builder with rescaling enabled or disabled.
	 * 
	 * <p>A horizontal array arranges its child components in a horizontal line
	 * from left to right. When rescaling is enabled, the components will resize
	 * to fill the available space.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder harray = BmlBuilder.harray(true)
	 *     .withNode(BmlBuilder.button("yes", "Yes"))
	 *     .withNode(BmlBuilder.button("no", "No"));
	 * }</pre>
	 * 
	 * @param rescale whether to enable rescaling of child components
	 * @return a new {@code BmlNodeBuilder} configured as a horizontal array
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder harray(boolean rescale) {
		return harray().withAttribute("rescale", rescale);
	}
	
	/**
	 * Creates a vertical array node builder.
	 * 
	 * <p>A vertical array arranges its child components in a vertical stack
	 * from top to bottom.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder varray = BmlBuilder.varray()
	 *     .withNode(BmlBuilder.label("First line"))
	 *     .withNode(BmlBuilder.label("Second line"));
	 * }</pre>
	 * 
	 * @return a new {@code BmlNodeBuilder} configured as a vertical array
	 * @since 1.0.0
	 */
	private static BmlNodeBuilder varray() {
		return BmlNodeBuilder.builder("varray");
	}
	
	/**
	 * Creates a vertical array node builder with rescaling enabled or disabled.
	 * 
	 * <p>A vertical array arranges its child components in a vertical stack
	 * from top to bottom. When rescaling is enabled, the components will resize
	 * to fill the available space.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder varray = BmlBuilder.varray(true)
	 *     .withNode(BmlBuilder.label("First line"))
	 *     .withNode(BmlBuilder.label("Second line"));
	 * }</pre>
	 * 
	 * @param rescale whether to enable rescaling of child components
	 * @return a new {@code BmlNodeBuilder} configured as a vertical array
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder varray(boolean rescale) {
		return varray().withAttribute("rescale", rescale);
	}

	
	/**
	 * Creates a scroll area node builder with the specified scrolling behavior and content.
	 * 
	 * <p>A scroll area provides scrolling functionality for its child component when
	 * the content exceeds the available space.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlNodeBuilder scrollArea = BmlBuilder.scroll(true, true, 
	 *     BmlBuilder.varray().withNode(BmlBuilder.label("Long content...")));
	 * }</pre>
	 * 
	 * @param horizontal whether to enable horizontal scrolling
	 * @param vertical whether to enable vertical scrolling
	 * @param node the child component that should be scrollable
	 * @return a new {@code BmlNodeBuilder} configured as a scroll area
	 * @since 1.0.0
	 */
	public static BmlNodeBuilder scroll(boolean horizontal, boolean vertical, BmlNodeBuilder node) {
		return BmlNodeBuilder.builder("scroll").withAttribute("horizontal", horizontal).withAttribute("vertical", vertical).withNode(node);
	}
	
	/**
	 * Adds a node builder to this builder's child list.
	 * 
	 * <p>This method allows for chaining multiple node additions to build complex
	 * hierarchical structures.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlBuilder builder = BmlBuilder.builder()
	 *     .withNode(BmlBuilder.label("First"))
	 *     .withNode(BmlBuilder.label("Second"));
	 * }</pre>
	 * 
	 * @param nodeBuilder the node builder to add as a child
	 * @return this {@code BmlBuilder} instance for method chaining
	 * @since 1.0.0
	 */
	public BmlBuilder withNode(BmlNodeBuilder nodeBuilder) {
		root.withNode(nodeBuilder);
		return this;
	}
	
	/**
	 * Adds all nodes from another BML builder to this builder's child list.
	 * 
	 * <p>This method allows for combining the contents of two builders, effectively
	 * merging their child node lists.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * BmlBuilder content1 = BmlBuilder.builder().withNode(BmlBuilder.label("First"));
	 * BmlBuilder content2 = BmlBuilder.builder().withNode(BmlBuilder.label("Second"));
	 * BmlBuilder combined = BmlBuilder.builder()
	 *     .withNodes(content1)
	 *     .withNodes(content2);
	 * }</pre>
	 * 
	 * @param nodes the BML builder whose nodes should be added
	 * @return this {@code BmlBuilder} instance for method chaining
	 * @since 1.0.0
	 */
	public BmlBuilder withNodes(BmlBuilder nodes) {
		root.withNodes(nodes);
		return this;
	}
	
	/**
	 * Builds the BML string representation of this builder's content.
	 * 
	 * <p>This method recursively builds all child nodes and concatenates their
	 * BML representations into a single string that can be used in Wurm Unlimited.</p>
	 * 
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * String bml = BmlBuilder.builder()
	 *     .withNode(BmlBuilder.label("Hello"))
	 *     .buildBml();
	 * }</pre>
	 * 
	 * @return the BML string representation of this builder's content
	 * @since 1.0.0
	 */
	public String buildBml() {
		StringBuilder builder = new StringBuilder();
		root.buildNodes().forEach(node -> builder.append(node.buildBml()));
		return builder.toString();
	}
	
	/**
	 * Gets the list of node builders that are direct children of this builder's root.
	 * 
	 * <p>This method provides access to the internal list of node builders for
	 * advanced manipulation or inspection.</p>
	 * 
	 * @return a list of {@code BmlNodeBuilder} instances that are direct children
	 * @since 1.0.0
	 */
	public List<BmlNodeBuilder> getNodeBuilders() {
		return root.buildNodes();
	}
	
	/**
	 * Builds this builder's content as a DOM element.
	 * 
	 * <p>This method creates a DOM representation of the BML structure that can
	 * be used with XML processing tools.</p>
	 * 
	 * @param document the DOM document to use for element creation
	 * @return the DOM element representing this builder's content
	 * @since 1.0.0
	 */
	public Element build(Document document) {
		return root.build(document);
	}
}
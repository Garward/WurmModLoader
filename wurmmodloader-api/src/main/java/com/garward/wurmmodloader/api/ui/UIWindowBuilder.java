package com.garward.wurmmodloader.api.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * Builder for constructing UIWindow instances with fluent API.
 *
 * <p>This builder provides a simple way to create BML windows without writing
 * raw BML strings. It handles all the boilerplate of building valid BML.</p>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public class UIWindowBuilder {

    private final String title;
    private int width = 400;
    private int height = 300;
    private final List<BMLElement> elements = new ArrayList<>();
    private BiConsumer<Object, Properties> answerCallback;

    public UIWindowBuilder(String title) {
        this.title = title;
    }

    /**
     * Sets the window width.
     * @param width Width in pixels
     * @return This builder
     */
    public UIWindowBuilder width(int width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the window height.
     * @param height Height in pixels
     * @return This builder
     */
    public UIWindowBuilder height(int height) {
        this.height = height;
        return this;
    }

    /**
     * Adds a text element.
     *
     * @param text The text to display
     * @return This builder
     */
    public UIWindowBuilder addText(String text) {
        return addText(text, false);
    }

    /**
     * Adds a text element.
     *
     * @param text The text to display
     * @param bold Whether the text should be bold
     * @return This builder
     */
    public UIWindowBuilder addText(String text, boolean bold) {
        elements.add(new TextElement(text, bold));
        return this;
    }

    /**
     * Adds a header (bold text with spacing).
     *
     * @param text The header text
     * @return This builder
     */
    public UIWindowBuilder addHeader(String text) {
        elements.add(new TextElement("", false)); // Spacer
        elements.add(new TextElement(text, true));
        elements.add(new TextElement("", false)); // Spacer
        return this;
    }

    /**
     * Adds a horizontal separator line.
     *
     * @return This builder
     */
    public UIWindowBuilder addSeparator() {
        elements.add(new TextElement("═══════════════════════════════════", false));
        return this;
    }

    /**
     * Adds a button.
     *
     * @param id The button ID (used in callback)
     * @param label The button label
     * @return This builder
     */
    public UIWindowBuilder addButton(String id, String label) {
        elements.add(new ButtonElement(id, label));
        return this;
    }

    /**
     * Adds a text input field.
     *
     * @param id The input ID (used in callback)
     * @param label The input label
     * @param defaultValue The default value
     * @return This builder
     */
    public UIWindowBuilder addInput(String id, String label, String defaultValue) {
        elements.add(new InputElement(id, label, defaultValue));
        return this;
    }

    /**
     * Adds a dropdown selection.
     *
     * @param id The dropdown ID (used in callback)
     * @param label The dropdown label
     * @param options The available options
     * @return This builder
     */
    public UIWindowBuilder addDropdown(String id, String label, String... options) {
        elements.add(new DropdownElement(id, label, options));
        return this;
    }

    /**
     * Adds a radio button to a radio group.
     *
     * <p>Multiple radio buttons with the same group name form a radio group where
     * only one can be selected at a time. The selected radio button's ID is
     * returned in the callback Properties under the group name key.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * window.addRadioButton("choice", "option1", "Option 1")
     *       .addRadioButton("choice", "option2", "Option 2")
     *       .addRadioButton("choice", "option3", "Option 3")
     *       .onAnswer((player, answers) -> {
     *           String selected = answers.getProperty("choice"); // Returns "option1", "option2", etc.
     *       });
     * </pre>
     *
     * @param group The radio group name (used as key in callback)
     * @param id The unique ID for this radio button option
     * @param label The label text displayed next to the radio button
     * @return This builder
     */
    public UIWindowBuilder addRadioButton(String group, String id, String label) {
        elements.add(new RadioElement(group, id, label));
        return this;
    }

    /**
     * Sets the callback for when the player submits the window.
     *
     * @param callback The callback to invoke
     * @return This builder
     */
    public UIWindowBuilder onAnswer(BiConsumer<Object, Properties> callback) {
        this.answerCallback = callback;
        return this;
    }

    /**
     * Builds the UIWindow.
     *
     * @return The constructed window
     */
    public UIWindow build() {
        return new SimpleUIWindow(title, width, height, elements, answerCallback);
    }

    // ========================================================================
    // BML Element Classes
    // ========================================================================

    static abstract class BMLElement {
        abstract String toBML();
    }

    static class TextElement extends BMLElement {
        final String text;
        final boolean bold;

        TextElement(String text, boolean bold) {
            this.text = text;
            this.bold = bold;
        }

        @Override
        String toBML() {
            if (bold) {
                return "text{type='bold';text='" + escape(text) + "'}";
            }
            return "text{text='" + escape(text) + "'}";
        }
    }

    static class ButtonElement extends BMLElement {
        final String id;
        final String label;

        ButtonElement(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        String toBML() {
            return "button{id='" + id + "';text='" + escape(label) + "'}";
        }
    }

    static class InputElement extends BMLElement {
        final String id;
        final String label;
        final String defaultValue;

        InputElement(String id, String label, String defaultValue) {
            this.id = id;
            this.label = label;
            this.defaultValue = defaultValue;
        }

        @Override
        String toBML() {
            return "label{text='" + escape(label) + "'}input{id='" + id + "';text='" +
                   escape(defaultValue) + "'}";
        }
    }

    static class DropdownElement extends BMLElement {
        final String id;
        final String label;
        final String[] options;

        DropdownElement(String id, String label, String[] options) {
            this.id = id;
            this.label = label;
            this.options = options;
        }

        @Override
        String toBML() {
            StringBuilder sb = new StringBuilder();
            sb.append("label{text='").append(escape(label)).append("'}");
            sb.append("dropdown{id='").append(id).append("';options='");
            for (int i = 0; i < options.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(escape(options[i]));
            }
            sb.append("'}");
            return sb.toString();
        }
    }

    static class RadioElement extends BMLElement {
        final String group;
        final String id;
        final String label;

        RadioElement(String group, String id, String label) {
            this.group = group;
            this.id = id;
            this.label = label;
        }

        @Override
        String toBML() {
            return "radio{group='" + group + "';id='" + id + "';text='" + escape(label) + "'}";
        }
    }

    /**
     * Escapes BML special characters.
     */
    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("'", "\\'")
                   .replace("{", "\\{")
                   .replace("}", "\\}");
    }

    // ========================================================================
    // Simple UIWindow Implementation
    // ========================================================================

    static class SimpleUIWindow implements UIWindow {
        private final String title;
        private final int width;
        private final int height;
        private final List<BMLElement> elements;
        private final BiConsumer<Object, Properties> answerCallback;

        SimpleUIWindow(String title, int width, int height, List<BMLElement> elements,
                      BiConsumer<Object, Properties> answerCallback) {
            this.title = title;
            this.width = width;
            this.height = height;
            this.elements = new ArrayList<>(elements);
            this.answerCallback = answerCallback;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public String getBML(Object player) {
            StringBuilder bml = new StringBuilder();
            for (BMLElement element : elements) {
                bml.append(element.toBML());
            }
            return bml.toString();
        }

        @Override
        public void onAnswer(Object player, Properties answers) {
            if (answerCallback != null) {
                answerCallback.accept(player, answers);
            }
        }
    }
}

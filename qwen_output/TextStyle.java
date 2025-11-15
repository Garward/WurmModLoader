package com.garward.wurmmodloader.modsupport.bml;

import java.util.Locale;

/**
 * Enumeration representing text styling options for BML (Better Markup Language) formatting.
 *
 * <p>This enum provides standardized text styling constants that can be used when formatting
 * text in Wurm Unlimited mods. Each style corresponds to a specific visual presentation
 * when rendered in the game's UI.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Apply bold styling to text
 * String styledText = "<" + TextStyle.BOLD.getType() + ">Important Message</" + TextStyle.BOLD.getType() + ">";
 *
 * // Check if a particular style is supported
 * if (TextStyle.BOLD != null) {
 *     // Use bold styling
 * }
 * }</pre>
 * </p>
 *
 * <p>This enum is thread-safe as it only contains immutable enum constants and has no
 * mutable state. It can be safely accessed from multiple threads without synchronization.</p>
 *
 * @since 1.0.0
 * @see com.garward.wurmmodloader.modsupport.bml.BMLFormatter
 */
public enum TextStyle {

	/**
	 * Bold text styling.
	 *
	 * <p>Represents text that should be displayed with increased font weight,
	 * typically rendered as thicker characters.</p>
	 *
	 * @since 1.0.0
	 */
	BOLD,

	/**
	 * Italic text styling.
	 *
	 * <p>Represents text that should be displayed with a slanted appearance,
	 * typically rendered with characters that slope to the right.</p>
	 *
	 * @since 1.0.0
	 */
	ITALIC,

	/**
	 * Bold and italic text styling.
	 *
	 * <p>Represents text that should be displayed with both increased font weight
	 * and slanted appearance, combining the effects of {@link #BOLD} and {@link #ITALIC}.</p>
	 *
	 * @since 1.0.0
	 */
	BOLDITALIC,
	;

	/**
	 * Gets the lowercase string representation of this text style.
	 *
	 * <p>This method returns the enum constant name converted to lowercase using
	 * the root locale. The returned string can be used as the tag name in BML formatting.</p>
	 *
	 * <p>Usage example:
	 * <pre>{@code
	 * TextStyle style = TextStyle.BOLD;
	 * String tag = style.getType(); // Returns "bold"
	 * String formattedText = "<" + tag + ">Bold Text</" + tag + ">";
	 * }</pre>
	 * </p>
	 *
	 * @return the lowercase string representation of this text style
	 * @since 1.0.0
	 */
	public String getType() {
		return name().toLowerCase(Locale.ROOT);
	}
}
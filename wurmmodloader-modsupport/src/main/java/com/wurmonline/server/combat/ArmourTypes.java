package com.wurmonline.server.combat;
/**
 * Defines the various types of armour available in the game.
 * 
 * <p>This interface provides constants for different armour types that can be used
 * to identify and categorize armour pieces within the combat system. Each constant
 * represents a distinct armour type with specific properties and characteristics.</p>
 * 
 * <p>Usage example:
 * <pre>@code
 * if (armour.getArmourType() == ArmourTypes.ARMOUR_PLATE) {
 *     // Handle plate armour specific logic
 * }
 * </pre></p>
 * 
 * <p><strong>Thread Safety:</strong> This interface is thread-safe as it only contains
 * immutable constant values.</p>
 * 
 * <p><strong>Lifecycle:</strong> This interface is deprecated and may be removed in future versions.
 * New implementations should use the updated armour system.</p>
 * 
 * @since 1.0.0
 * @deprecated This interface is deprecated and will be replaced by newer armour handling mechanisms
 */

@Deprecated
public interface ArmourTypes {
	public static final int ARMOUR_LEATHER = 1;
	public static final int ARMOUR_STUDDED = 2;
	public static final int ARMOUR_CHAIN = 3;
	public static final int ARMOUR_PLATE = 4;
	public static final int ARMOUR_RING = 5;
	public static final int ARMOUR_CLOTH = 6;
	public static final int ARMOUR_SCALE = 7;
	public static final int ARMOUR_SPLINT = 8;
	public static final int ARMOUR_LEATHER_DRAGON = 9;
	public static final int ARMOUR_SCALE_DRAGON = 10;
	public static final int ARMOUR_NONE = -1;
}

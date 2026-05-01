package com.garward.wurmmodloader.api.icon;

import com.garward.wurmmodloader.api.registry.ResourceLocation;

import java.util.Objects;

/**
 * Represents a registered icon with associated metadata.
 *
 * <p>Icons are identified by a {@link ResourceLocation} and mapped to numeric icon IDs
 * for use in the game's sprite sheet system. This class provides metadata about the icon's
 * position within the sheet system and its distribution requirements.</p>
 *
 * <p>Icon System Overview:</p>
 * <ul>
 *   <li><strong>Sheets:</strong> Icons are organized into sprite sheets (0-N)</li>
 *   <li><strong>Icons per sheet:</strong> 240 icons per sheet (20 wide × 12 tall)</li>
 *   <li><strong>Vanilla range:</strong> Icons 0-1679 (sheets 0-6)</li>
 *   <li><strong>Custom range:</strong> Icons 1680+ (sheet 7+)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Create an icon
 * ResourceLocation id = new ResourceLocation("mymod", "flame_sword");
 * Icon icon = new Icon(id, 1680, "mymod_icons.png", IconType.CUSTOM);
 *
 * // Get metadata
 * int iconId = icon.getIconId();           // 1680
 * int sheet = icon.getSheetNumber();       // 7
 * int position = icon.getPositionInSheet(); // 0
 * int row = icon.getRow();                 // 0
 * int column = icon.getColumn();           // 0
 * }</pre>
 * </p>
 *
 * <p>Thread Safety: This class is immutable and thread-safe.</p>
 *
 * @since 1.0.0
 */
public final class Icon {
    /**
     * Number of icons per sprite sheet in the game's icon system.
     */
    public static final int ICONS_PER_SHEET = 240;

    /**
     * Number of icons per row in a sprite sheet (width).
     */
    public static final int ICONS_PER_ROW = 20;

    /**
     * Number of rows per sprite sheet (height).
     */
    public static final int ROWS_PER_SHEET = 12;

    /**
     * Maximum vanilla icon ID (inclusive).
     */
    public static final int MAX_VANILLA_ICON = 1679;

    /**
     * First custom icon ID (start of mod icon range).
     *
     * <p>Custom icons live on sheets 7+ via {@code IconLoaderMergePatch} extension;
     * the wire protocol caps at {@link Short#MAX_VALUE} (32767), giving 137 sheets
     * × 240 = ~30k addressable custom slots above the vanilla range.
     */
    public static final int FIRST_CUSTOM_ICON = 1680;

    /**
     * Maximum custom icon ID (inclusive). Bound by the {@code short} wire protocol.
     */
    public static final int MAX_CUSTOM_ICON = Short.MAX_VALUE;

    /**
     * Default fallback icon ID (question mark).
     */
    public static final int FALLBACK_ICON_ID = 60;

    private final ResourceLocation id;
    private final int iconId;
    private final int sheetNumber;
    private final int positionInSheet;
    private final String packFile;
    private final IconType type;

    /**
     * Constructs a new Icon with the specified properties.
     *
     * @param id the resource location identifier
     * @param iconId the numeric icon ID (0-N)
     * @param packFile the icon pack file name containing this icon
     * @param type the type of icon
     * @throws IllegalArgumentException if id or packFile is null, or iconId is negative
     * @since 1.0.0
     */
    public Icon(ResourceLocation id, int iconId, String packFile, IconType type) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        if (iconId < 0) {
            throw new IllegalArgumentException("Icon ID cannot be negative: " + iconId);
        }
        if (packFile == null) {
            throw new IllegalArgumentException("Pack file cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Icon type cannot be null");
        }

        this.id = id;
        this.iconId = iconId;
        this.sheetNumber = iconId / ICONS_PER_SHEET;
        this.positionInSheet = iconId % ICONS_PER_SHEET;
        this.packFile = packFile;
        this.type = type;
    }

    /**
     * Gets the resource location identifier for this icon.
     *
     * @return the resource location (e.g., "mymod:flame_sword")
     * @since 1.0.0
     */
    public ResourceLocation getId() {
        return id;
    }

    /**
     * Gets the numeric icon ID used in the game's sprite sheet system.
     *
     * @return the icon ID (0-N)
     * @since 1.0.0
     */
    public int getIconId() {
        return iconId;
    }

    /**
     * Gets the sprite sheet number containing this icon.
     *
     * <p>Sheet numbers:
     * <ul>
     *   <li>0-6: Vanilla sheets</li>
     *   <li>7+: Custom mod sheets</li>
     * </ul>
     * </p>
     *
     * @return the sheet number (0-N)
     * @since 1.0.0
     */
    public int getSheetNumber() {
        return sheetNumber;
    }

    /**
     * Gets the position of this icon within its sprite sheet.
     *
     * @return the position (0-239)
     * @since 1.0.0
     */
    public int getPositionInSheet() {
        return positionInSheet;
    }

    /**
     * Gets the row of this icon within its sprite sheet.
     *
     * @return the row (0-11)
     * @since 1.0.0
     */
    public int getRow() {
        return positionInSheet / ICONS_PER_ROW;
    }

    /**
     * Gets the column of this icon within its sprite sheet.
     *
     * @return the column (0-19)
     * @since 1.0.0
     */
    public int getColumn() {
        return positionInSheet % ICONS_PER_ROW;
    }

    /**
     * Gets the icon pack file name containing this icon's image data.
     *
     * <p>For vanilla icons, this is typically "vanilla". For custom icons,
     * this is the mod's icon pack file (e.g., "mymod_icons.png").</p>
     *
     * @return the pack file name
     * @since 1.0.0
     */
    public String getPackFile() {
        return packFile;
    }

    /**
     * Gets the type classification of this icon.
     *
     * @return the icon type
     * @since 1.0.0
     */
    public IconType getType() {
        return type;
    }

    /**
     * Checks if this is a vanilla icon (ID 0-1679).
     *
     * @return true if this is a vanilla icon
     * @since 1.0.0
     */
    public boolean isVanilla() {
        return iconId <= MAX_VANILLA_ICON;
    }

    /**
     * Checks if this is a custom mod icon (ID 1680+).
     *
     * @return true if this is a custom icon
     * @since 1.0.0
     */
    public boolean isCustom() {
        return iconId >= FIRST_CUSTOM_ICON;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Icon icon = (Icon) o;
        return iconId == icon.iconId &&
               Objects.equals(id, icon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, iconId);
    }

    @Override
    public String toString() {
        return "Icon{" +
               "id=" + id +
               ", iconId=" + iconId +
               ", sheet=" + sheetNumber +
               ", position=" + positionInSheet +
               ", type=" + type +
               '}';
    }
}

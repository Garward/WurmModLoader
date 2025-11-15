package com.garward.wurmmodloader.api.ui;

/**
 * Defines where a menu entry can appear.
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public enum MenuTarget {
    /**
     * Body part context menus (right-click on body).
     */
    BODY,

    /**
     * Item context menus (right-click on items).
     */
    ITEM,

    /**
     * Creature context menus (right-click on creatures).
     */
    CREATURE,

    /**
     * Tile context menus (right-click on ground).
     */
    TILE
}

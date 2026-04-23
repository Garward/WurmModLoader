package com.garward.wurmmodloader.modsupport.actions.area;

import com.garward.wurmmodloader.api.events.eventlogic.area.AreaActionType;
import com.garward.wurmmodloader.api.events.eventlogic.area.ItemAreaHandler;
import com.garward.wurmmodloader.api.events.eventlogic.area.TileAreaHandler;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Process-wide registry of tile and item area-action handlers. Mods register
 * handlers here; {@link AreaActionMenus} and {@link AreaActionPerformer} look
 * them up by tile type / item template.
 */
public final class AreaActionRegistry {

    private static final Logger logger = Logger.getLogger(AreaActionRegistry.class.getName());

    private static final Map<Integer, Map<AreaActionType, ItemAreaHandler>> ITEM_HANDLERS = new HashMap<>();
    private static final Map<Byte, Map<AreaActionType, TileAreaHandler>> TILE_HANDLERS = new HashMap<>();

    private AreaActionRegistry() {}

    public static void registerItemHandler(int templateId, AreaActionType action, ItemAreaHandler handler) {
        try {
            ItemTemplate tpl = ItemTemplateFactory.getInstance().getTemplate(templateId);
            logger.fine(String.format("Added item area-handler for %s (%s) - %s",
                    tpl.getName(), action, handler.getClass().getName()));
            ITEM_HANDLERS.computeIfAbsent(templateId, t -> new HashMap<>()).put(action, handler);
        } catch (NoSuchTemplateException e) {
            logger.warning(String.format("Attempt to register area-handler for missing template %d - %s",
                    templateId, handler.getClass().getName()));
        }
    }

    public static void registerTileHandler(byte tileType, AreaActionType action, TileAreaHandler handler) {
        Tiles.Tile type = Tiles.getTile(tileType);
        if (type == null) {
            logger.warning(String.format("Attempt to register area-handler for missing tile type %d - %s",
                    tileType, handler.getClass().getName()));
            return;
        }
        logger.fine(String.format("Added tile area-handler for %s (%s) - %s",
                type.getName(), action, handler.getClass().getName()));
        TILE_HANDLERS.computeIfAbsent(tileType, t -> new HashMap<>()).put(action, handler);
    }

    public static Map<AreaActionType, ItemAreaHandler> getItemHandlers(Item item) {
        return ITEM_HANDLERS.get(item.getTemplateId());
    }

    public static Map<AreaActionType, TileAreaHandler> getTileHandlers(byte tileType) {
        return TILE_HANDLERS.get(tileType);
    }
}

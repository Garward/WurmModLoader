package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when item templates have been created by the server.
 *
 * <p>This event is fired during server initialization, after all vanilla
 * item templates have been registered. Mods should use this event to register
 * custom item templates or modify existing ones.
 *
 * <p>This event is <b>not cancellable</b>.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
 *     // Register custom items using ItemTemplateBuilder
 *     ItemTemplate magicSword = new ItemTemplateBuilder("mymod:magic_sword")
 *         .name("Magic Sword", "Magic Swords", "A powerful enchanted blade")
 *         .weightGrams(2500)
 *         .build();
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code ItemTemplatesCreatedListener} interface. New mods should use this
 * event instead of the listener interface.
 *
 * @since 1.0.0
 */
public class ItemTemplatesCreatedEvent extends Event {

    /**
     * Creates a new ItemTemplatesCreatedEvent.
     */
    public ItemTemplatesCreatedEvent() {
        super(false); // Not cancellable
    }
}

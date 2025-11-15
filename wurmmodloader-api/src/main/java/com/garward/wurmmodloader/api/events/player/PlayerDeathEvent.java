package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;


import com.wurmonline.server.players.Player;

/**
 * Fired when a player dies.
 *
 * <p>This event is fired after the player has died but before death penalties
 * are applied. Mods can use this to implement custom death mechanics.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class PlayerDeathEvent extends Event {

    private final Player player;
    private final com.wurmonline.server.creatures.Creature killer; // May be null

    public PlayerDeathEvent(Player player, com.wurmonline.server.creatures.Creature killer) {
        this.player = player;
        this.killer = killer;
    }

    /**
     * Get the player who died.
     *
     * @return The dead player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the creature that killed the player.
     *
     * @return The killer, or null if death was environmental
     */
    public com.wurmonline.server.creatures.Creature getKiller() {
        return killer;
    }

    /**
     * Check if the player was killed by another player.
     *
     * @return true if killer was a player (PvP death)
     */
    public boolean isPvPDeath() {
        return killer != null && killer.isPlayer();
    }

    /**
     * Check if the player was killed by an NPC.
     *
     * @return true if killer was an NPC
     */
    public boolean isNPCDeath() {
        return killer != null && !killer.isPlayer();
    }

    /**
     * Check if the death was environmental (no killer).
     *
     * @return true if no killer (fall damage, drowning, etc.)
     */
    public boolean isEnvironmentalDeath() {
        return killer == null;
    }
}

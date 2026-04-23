package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Communicator;

import java.util.List;

/**
 * Fired from {@code BehaviourDispatcher.sendRequestResponse(...)} right before
 * the available-actions list is packed and sent to the client. This is the
 * single chokepoint for surface-tile, creature, item, floor, cave-tile,
 * wall, fence, bridge, illusion and tile-corner menus — roughly every
 * right-click menu in the game.
 *
 * <p>The {@link #getAvailableActions()} list is the live one that will be
 * written onto the wire; listeners may add, remove or reorder entries in
 * place. {@link #getHelpString()} is the context label (target-type name
 * with underscores).</p>
 *
 * <p>A handful of specialized menus (wounds, planets, mission-performed,
 * bridge parts, tickets) bypass {@code sendRequestResponse} and invoke
 * {@code Communicator.sendAvailableActions} directly — those are not covered
 * by this event.</p>
 */
public class ActionMenuBuildEvent extends Event {

    private final Communicator communicator;
    private final List<ActionEntry> availableActions;
    private final String helpString;
    private final boolean sendToSelectBar;

    public ActionMenuBuildEvent(Communicator communicator, List<ActionEntry> availableActions,
                                String helpString, boolean sendToSelectBar) {
        this.communicator = communicator;
        this.availableActions = availableActions;
        this.helpString = helpString;
        this.sendToSelectBar = sendToSelectBar;
    }

    public Communicator getCommunicator()       { return communicator; }
    public List<ActionEntry> getAvailableActions() { return availableActions; }
    public String getHelpString()               { return helpString; }
    public boolean isSendToSelectBar()          { return sendToSelectBar; }
}

package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired from {@code BehaviourDispatcher.action(...)} every time a client sends
 * a context-menu action request. This is the single chokepoint for all
 * player-initiated actions — mining, digging, chat, fighting, opening,
 * crafting, mounting, etc. — identified by the short action number.
 *
 * <p>Listeners see the raw wire-level identity triple (subjectWurmId,
 * targetWurmId, actionShort) before any {@link com.wurmonline.server.behaviours.Action}
 * object is constructed. Cancelling aborts the dispatch: no queued action,
 * no poll, no side effect. Because this fires early, listeners must resolve
 * subject/target items themselves (via {@code Items.getItem} etc.) if they
 * need object references.</p>
 *
 * <p>Two dispatcher overloads exist (single-target and multi-target); both
 * fire this event, with the multi-target overload firing once per target id.</p>
 */
public class ActionPerformRequestEvent extends CancellableEvent {

    private final Creature performer;
    private final long subjectWurmId;
    private final long targetWurmId;
    private final short action;

    public ActionPerformRequestEvent(Creature performer, long subjectWurmId, long targetWurmId, short action) {
        this.performer = performer;
        this.subjectWurmId = subjectWurmId;
        this.targetWurmId = targetWurmId;
        this.action = action;
    }

    public Creature getPerformer()   { return performer; }
    public long getSubjectWurmId()   { return subjectWurmId; }
    public long getTargetWurmId()    { return targetWurmId; }
    public short getAction()         { return action; }
}

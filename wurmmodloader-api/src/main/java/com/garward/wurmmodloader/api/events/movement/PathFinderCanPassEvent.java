package com.garward.wurmmodloader.api.events.movement;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.ai.PathTile;

/**
 * Fired at the tail of {@code PathFinder.canPass(PathTile, PathTile)}, the
 * gatekeeper for every A* tile transition during pathfinding. Vanilla checks
 * solid caves, lava, building walls, doors (with permissions), bridges, and
 * water-depth restrictions for submerged creatures.
 *
 * <p>Listeners may flip the result via {@link #setCanPass(boolean)} to veto or
 * permit a tile transition. {@link #getCreature()} may be {@code null} —
 * PathFinder can be built without a creature context (though production paths
 * almost always carry one). Not cancellable; the return value is the output.</p>
 *
 * <p>Hot path: this fires on every A* expansion. Keep listeners cheap.</p>
 */
public class PathFinderCanPassEvent extends Event {

    private final Creature creature;
    private final PathTile from;
    private final PathTile to;
    private boolean canPass;

    public PathFinderCanPassEvent(Creature creature, PathTile from, PathTile to, boolean canPass) {
        this.creature = creature;
        this.from = from;
        this.to = to;
        this.canPass = canPass;
    }

    public Creature getCreature() { return creature; }
    public PathTile getFrom()     { return from; }
    public PathTile getTo()       { return to; }
    public boolean canPass()      { return canPass; }
    public void setCanPass(boolean v) { this.canPass = v; }
}

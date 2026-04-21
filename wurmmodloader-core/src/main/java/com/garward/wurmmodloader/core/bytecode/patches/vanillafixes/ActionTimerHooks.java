package com.garward.wurmmodloader.core.bytecode.patches.vanillafixes;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.spells.Spell;

/**
 * Static hooks invoked from bytecode patched by {@link ActionTimerFix}.
 *
 * <p>Split out so the patched call sites can reference a stable public API instead of
 * inlining fragile Javassist source snippets.</p>
 */
public final class ActionTimerHooks {

    private ActionTimerHooks() {}

    /**
     * Flatten-action tick decision. Ported from bdew TimerFix (GPL-2.0) — the vanilla
     * flatten loop hardcodes the tick interval and ignores the server action-timer
     * multiplier.
     */
    public static boolean shouldFlattenTick(Action act, boolean insta, float counter, byte type, boolean first) {
        if (insta) return true;

        float tickTimes = 5;
        if (act.getNumber() == 150) {
            tickTimes = 10;
        }
        if (type == Tiles.Tile.TILE_CLAY.id || type == Tiles.Tile.TILE_TAR.id || type == Tiles.Tile.TILE_PEAT.id) {
            tickTimes = 30;
        }
        tickTimes = tickTimes / Servers.localServer.getActionTimer();

        if (counter == 1 && first) {
            act.setNextTick(counter + tickTimes);
            return true;
        } else if (act.getNextTick() < counter) {
            if (!first) {
                act.setNextTick(counter + tickTimes);
            }
            return true;
        }
        return false;
    }

    /**
     * Spell casting-time replacement. Divides the base by the server action-timer
     * multiplier and clamps to {@link VanillaFixesSettings#actionTimerMinSpell()}.
     */
    public static int getCastingTime(Spell spell, int base) {
        int scaled = (int) (base / Servers.localServer.getActionTimer());
        return Math.max(scaled, VanillaFixesSettings.actionTimerMinSpell());
    }
}

package com.garward.wurmmodloader.api.ui;

/**
 * Predicate for testing whether a UI element should be visible to a player.
 *
 * <p>This is a functional interface that can be used to filter menu entries,
 * buttons, or other UI components based on player state.</p>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
@FunctionalInterface
public interface VisibilityPredicate {

    /**
     * Tests if the UI element should be visible for the given player.
     *
     * @param player The player to test (Creature object)
     * @return true if visible, false otherwise
     */
    boolean test(Object player);

    /**
     * Always visible.
     */
    VisibilityPredicate ALWAYS = player -> true;

    /**
     * Never visible.
     */
    VisibilityPredicate NEVER = player -> false;

    /**
     * Combines this predicate with another using AND logic.
     *
     * @param other The other predicate
     * @return Combined predicate
     */
    default VisibilityPredicate and(VisibilityPredicate other) {
        return player -> this.test(player) && other.test(player);
    }

    /**
     * Combines this predicate with another using OR logic.
     *
     * @param other The other predicate
     * @return Combined predicate
     */
    default VisibilityPredicate or(VisibilityPredicate other) {
        return player -> this.test(player) || other.test(player);
    }

    /**
     * Negates this predicate.
     *
     * @return Negated predicate
     */
    default VisibilityPredicate negate() {
        return player -> !this.test(player);
    }
}

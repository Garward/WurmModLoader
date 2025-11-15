package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired when creatures are being bred.
 *
 * <p>This event fires during the breeding process in {@code MethodsCreatures.breed()}.
 * The counter parameter indicates breeding progress (1.0f = completing).</p>
 *
 * <p><b>Thread Safety:</b> This event is fired on the game thread.</p>
 *
 * <p><b>Null Safety:</b></p>
 * <ul>
 *   <li>performer - Always non-null (creature performing breed action)</li>
 *   <li>target - Always non-null (creature being bred)</li>
 *   <li>action - May be null in some breeding contexts</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CreatureBreedEvent extends CancellableEvent {
    private final Creature performer;
    private final Creature target;
    private final short breedType;
    private final Action action;  // Nullable
    private final float counter;

    /**
     * Constructs a new CreatureBreedEvent.
     *
     * @param performer The creature performing the breed action (non-null)
     * @param target The creature being bred (non-null)
     * @param breedType The breed type identifier
     * @param action The action instance (may be null)
     * @param counter The breeding progress counter (1.0f = completing)
     */
    public CreatureBreedEvent(Creature performer, Creature target, short breedType, Action action, float counter) {
        if (performer == null) {
            throw new IllegalArgumentException("performer cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        this.performer = performer;
        this.target = target;
        this.breedType = breedType;
        this.action = action;
        this.counter = counter;
    }

    /**
     * Gets the creature performing the breed action.
     *
     * @return The performer creature (never null)
     */
    public Creature getPerformer() {
        return performer;
    }

    /**
     * Gets the creature being bred.
     *
     * @return The target creature (never null)
     */
    public Creature getTarget() {
        return target;
    }

    /**
     * Gets the breed type identifier.
     *
     * @return The breed type
     */
    public short getBreedType() {
        return breedType;
    }

    /**
     * Gets the action instance for this breeding.
     *
     * <p><b>Warning:</b> May be null in some breeding contexts.</p>
     *
     * @return The action instance, or null if not available
     */
    public Action getAction() {
        return action;
    }

    /**
     * Gets the breeding progress counter.
     *
     * <p>The counter typically progresses from 0.0f to 1.0f during the breeding action.
     * A value of 1.0f usually indicates the breeding is completing.</p>
     *
     * @return The breeding progress counter
     */
    public float getCounter() {
        return counter;
    }

    /**
     * Checks if the breeding action is completing (counter == 1.0f).
     *
     * @return true if breeding is completing, false otherwise
     */
    public boolean isCompleting() {
        return counter == 1.0f;
    }

    /**
     * Gets the creature that will be the mother (sex == 1).
     *
     * <p>Determines which creature is female and returns it.</p>
     *
     * @return The mother creature, or null if neither is female
     */
    public Creature getMother() {
        if (performer.getSex() == 1) {
            return performer;
        } else if (target.getSex() == 1) {
            return target;
        }
        return null;
    }

    /**
     * Gets the creature that will be the father (sex != 1).
     *
     * <p>Determines which creature is male and returns it.</p>
     *
     * @return The father creature, or null if neither is male or mother is null
     */
    public Creature getFather() {
        Creature mother = getMother();
        if (mother == null) {
            return null;
        }
        return (mother == performer) ? target : performer;
    }

    /**
     * Checks if the breeding pair appears to be related (inbreeding).
     *
     * <p>Checks for common parentage between the breeding pair by comparing:
     * <ul>
     *   <li>Same father</li>
     *   <li>Same mother</li>
     *   <li>Father is mother's father</li>
     *   <li>Father's mother is mother</li>
     * </ul>
     * </p>
     *
     * @return true if the creatures appear to be related, false otherwise
     */
    public boolean isInbreeding() {
        Creature mother = getMother();
        Creature father = getFather();

        if (mother == null || father == null) {
            return false;
        }

        long fatherParent = father.getFather();
        long motherParent = mother.getFather();
        long fatherMother = father.getMother();
        long motherMother = mother.getMother();
        long fatherId = father.getWurmId();
        long motherId = mother.getWurmId();

        // Same father
        if (fatherParent != -10 && fatherParent == motherParent) {
            return true;
        }
        // Same mother
        if (fatherMother != -10 && fatherMother == motherMother) {
            return true;
        }
        // Father is mother's father
        if (fatherId == motherParent) {
            return true;
        }
        // Father's mother is mother
        if (fatherMother == motherId) {
            return true;
        }

        return false;
    }
}

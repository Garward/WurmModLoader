package com.garward.wurmmodloader.modsupport.loot;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builder for creating loot rules that trigger on creature death.
 *
 * <p>Loot rules can have various requirements that must be met for the rule to trigger,
 * and can execute multiple triggers when those requirements are met.</p>
 *
 * @since 1.0.0
 */
public class LootRule {
    private Predicate<Creature> creatureReq = (c) -> true;
    private LootPredicate lootReq = (c, k) -> true;

    private final List<LootTrigger> triggers = new LinkedList<LootTrigger>();

    /**
     * Create a new loot rule builder.
     *
     * @return A new loot rule
     */
    public static LootRule create() {
        return new LootRule();
    }

    /**
     * Add creature requirement, those are checked only once when the creature died
     * if any requirement return false the whole rule is ignored
     *
     * @param req The predicate to test against the creature
     * @return This rule for chaining
     */
    public LootRule requireCreature(Predicate<Creature> req) {
        creatureReq = req.and(creatureReq);
        return this;
    }

    /**
     * Add general requirement, those are checked for each killer
     * if any requirement return false the rule will not trigger for that player
     *
     * @param req The predicate to test
     * @return This rule for chaining
     */
    public LootRule require(LootPredicate req) {
        lootReq = req.and(lootReq);
        return this;
    }

    /**
     * Add player requirement, those are checked for each killer
     * if any requirement return false the rule will not trigger for that player
     *
     * @param req The predicate to test against the killer
     * @return This rule for chaining
     */
    public LootRule requireKiller(Predicate<Player> req) {
        return require(((c, k) -> req.test(k)));
    }

    /**
     * Add a trigger that will run once and receive a set of all players matching the requirements
     *
     * @param t The trigger to add
     * @return This rule for chaining
     */
    public LootRule addTriggerOnce(LootTrigger t) {
        triggers.add(t);
        return this;
    }

    /**
     * Add trigger, those run for each player when all requirements are met
     *
     * @param t The trigger to add
     * @return This rule for chaining
     */
    public LootRule addTrigger(LootPlayerTrigger t) {
        return addTriggerOnce((c, killers) -> killers.forEach(k -> t.trigger(c, k)));
    }

    /**
     * Make this rule only apply to certain creature template ids
     *
     * @param templates templates that should match
     * @return This rule for chaining
     */
    public LootRule requireTemplateIds(int... templates) {
        Set<Integer> s = Arrays.stream(templates).boxed().collect(Collectors.toSet());
        return requireCreature((c) -> s.contains(c.getTemplateId()));
    }

    /**
     * Make this rule apply only to uniques
     *
     * @return This rule for chaining
     */
    public LootRule requireUnique() {
        return requireCreature(Creature::isUnique);
    }

    /**
     * Add flat chance check to this rule
     *
     * @param chance The chance (0.0 to 1.0)
     * @return This rule for chaining
     */
    public LootRule chance(float chance) {
        return require((c, k) -> Server.rand.nextFloat() < chance);
    }

    /**
     * Add chance check to this rule, based on creature
     *
     * @param chanceFunc Function that returns the chance for a given creature
     * @return This rule for chaining
     */
    public LootRule chance(Function<Creature, Float> chanceFunc) {
        return require((c, k) -> Server.rand.nextFloat() < chanceFunc.apply(c));
    }

    /**
     * Add drop generating function
     *
     * @param g The function that generates items
     * @return This rule for chaining
     * @see LootDrop
     */
    public LootRule addDrop(LootFunction<Collection<Item>> g) {
        return addTrigger(((c, k) -> g.apply(c, k).forEach(i -> k.getInventory().insertItem(i))));
    }

    /**
     * Add trigger that is only called for connected player and gets their communicator
     * This is a shortcut for sending messages etc.
     *
     * @param trigger The communicator trigger
     * @return This rule for chaining
     */
    public LootRule commTrigger(LootCommTrigger trigger) {
        return addTrigger((c, k) -> {
            if (k.hasLink())
                trigger.trigger(c, k.getCommunicator());
        });
    }

    /**
     * Add a sub-rule, that rule will only be processed if all requirements match for this rule
     *
     * @param sub The sub-rule to add
     * @return This rule for chaining
     */
    public LootRule addSubRule(LootRule sub) {
        return addTriggerOnce((creature, killers) -> {
            if (sub.checkCreature(creature))
                sub.run(creature, killers);
        });
    }

    //=== Internal

    boolean checkCreature(Creature deadCreature) {
        return creatureReq.test(deadCreature);
    }

    void run(Creature deadCreature, Set<Player> killers) {
        Set<Player> matchKillers = killers.stream().filter(p -> lootReq.test(deadCreature, p)).collect(Collectors.toSet());
        if (matchKillers.size() > 0) {
            triggers.forEach(t -> t.trigger(deadCreature, matchKillers));
        }
    }
}

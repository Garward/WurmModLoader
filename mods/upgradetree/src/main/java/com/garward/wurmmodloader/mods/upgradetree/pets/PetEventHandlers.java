package com.garward.wurmmodloader.mods.upgradetree.pets;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.creature.CombatDamageEvent;
import com.garward.wurmmodloader.api.events.creature.PetReleasedEvent;
import com.garward.wurmmodloader.api.events.creature.TameAttemptEvent;
import com.garward.wurmmodloader.api.events.creature.TameCompleteEvent;
import com.garward.wurmmodloader.api.events.player.PlayerLoginEvent;
import com.garward.wurmmodloader.core.capability.CapabilityManager;
import com.garward.wurmmodloader.mods.upgradetree.UpgradeTreeModifiers;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.NoSuchPlayerException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Wires upgrade-tree effects into the taming lifecycle and pet combat.
 *
 * <ul>
 *   <li>{@link TameAttemptEvent} — veto over-cap or tier-locked tames.</li>
 *   <li>{@link TameCompleteEvent} — add the new pet to the player's roster.</li>
 *   <li>{@link PetReleasedEvent} — remove on death / untame / release.</li>
 *   <li>{@link CombatDamageEvent} — apply {@code pet_damage_mult} / {@code pet_damage_taken_reduction}.</li>
 * </ul>
 *
 * <p>All gameplay knobs live in upgrade JSON — this file just reads them.</p>
 */
public class PetEventHandlers {

    private static final Logger logger = Logger.getLogger(PetEventHandlers.class.getName());

    @SubscribeEvent
    public void onTameAttempt(TameAttemptEvent event) {
        Creature performer = event.getPerformer();
        if (performer == null || !performer.isPlayer()) return;
        long ownerId = performer.getWurmId();

        int max = UpgradeTreeModifiers.getMaxPetSlots(ownerId);
        PlayerPets pets = CapabilityManager.getInstance()
            .getPlayerCapability(ownerId, PlayerPetsCapability.INSTANCE);

        // If the target was already one of this player's pets, re-binding is fine
        // (vanilla uses Charm/Dominate to bump loyalty on the current pet too).
        long targetId = event.getTarget().getWurmId();
        if (pets.contains(targetId)) return;

        if (pets.size() >= max) {
            performer.getCommunicator().sendNormalServerMessage(
                "You have reached your pet limit (" + max + "). Release one before taming another.",
                (byte) 3);
            event.cancel();
        }
    }

    @SubscribeEvent
    public void onTameComplete(TameCompleteEvent event) {
        Creature performer = event.getPerformer();
        if (performer == null || !performer.isPlayer()) return;
        long ownerId = performer.getWurmId();
        long targetId = event.getTarget().getWurmId();

        PlayerPets pets = CapabilityManager.getInstance()
            .getPlayerCapability(ownerId, PlayerPetsCapability.INSTANCE);

        if (pets.add(targetId)) {
            logger.fine("[UpgradeTree/Pets] Added pet " + targetId + " to " + performer.getName()
                + " (" + pets.size() + "/" + UpgradeTreeModifiers.getMaxPetSlots(ownerId) + ")");
        }

        // Vanilla kicks the previous pet before assigning the new one — re-attach
        // every other roster member so multi-active truly means multi-active.
        reattachRoster(performer, ownerId, pets, targetId);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoginEvent event) {
        Object p = event.getPlayer();
        if (!(p instanceof Creature)) return;
        Creature player = (Creature) p;
        long ownerId = player.getWurmId();

        PlayerPets pets = CapabilityManager.getInstance()
            .getPlayerCapability(ownerId, PlayerPetsCapability.INSTANCE);
        if (pets.size() == 0) return;

        reattachRoster(player, ownerId, pets, -1L);
    }

    /**
     * Re-attach every roster member (except {@code skipId}) as an active pet of
     * {@code owner}: restore dominator, restore leader, prune dead ones.
     */
    private static void reattachRoster(Creature owner, long ownerId, PlayerPets pets, long skipId) {
        List<Long> toDrop = new ArrayList<>();
        for (long petId : pets.getWurmIds()) {
            if (petId == skipId) continue;
            Creature pet;
            try {
                pet = Server.getInstance().getCreature(petId);
            } catch (NoSuchCreatureException | NoSuchPlayerException e) {
                toDrop.add(petId);
                continue;
            }
            if (pet == null) { toDrop.add(petId); continue; }

            try {
                // Ownership — safe to set even if already correct
                if (!pet.isDominated() || pet.getDominator() == null
                        || pet.getDominator().getWurmId() != ownerId) {
                    pet.setDominator(ownerId);
                }
                // Follow-link
                if (pet.getLeader() != owner) {
                    pet.setLeader(owner);
                }
            } catch (Throwable t) {
                logger.warning("[UpgradeTree/Pets] Failed to re-attach pet " + petId
                    + " to " + owner.getName() + ": " + t);
            }
        }
        for (long dead : toDrop) {
            pets.remove(dead);
            logger.fine("[UpgradeTree/Pets] Pruned missing pet " + dead + " from " + owner.getName());
        }
    }

    @SubscribeEvent
    public void onPetReleased(PetReleasedEvent event) {
        long ownerId = event.getFormerOwnerId();
        if (ownerId <= 0L) return;
        long petId = event.getPet().getWurmId();

        PlayerPets pets = CapabilityManager.getInstance()
            .getPlayerCapability(ownerId, PlayerPetsCapability.INSTANCE);

        if (pets.remove(petId)) {
            logger.fine("[UpgradeTree/Pets] Removed pet " + petId + " from owner " + ownerId
                + " (reason=" + event.getReason() + ")");
        }
    }

    @SubscribeEvent
    public void onCombatDamage(CombatDamageEvent event) {
        Creature attacker = event.getAttacker();
        Creature defender = event.getDefender();
        if (attacker == null || defender == null) return;

        double damage = event.getDamage();
        boolean changed = false;

        // Outgoing: is the attacker a player-owned pet?
        long attackerOwner = ownerOf(attacker);
        if (attackerOwner > 0L) {
            double mult = UpgradeTreeModifiers.getPetDamageMultiplier(attackerOwner);
            if (mult != 1.0) {
                damage *= mult;
                changed = true;
            }
        }

        // Incoming: is the defender a player-owned pet?
        long defenderOwner = ownerOf(defender);
        if (defenderOwner > 0L) {
            double mult = UpgradeTreeModifiers.getPetDamageTakenMultiplier(defenderOwner);
            if (mult != 1.0) {
                damage *= mult;
                changed = true;
            }
        }

        if (changed) {
            event.setDamage(damage);
        }
    }

    /**
     * Resolve the player wurmId that owns this creature via the roster capability,
     * or -1 if the creature isn't anyone's tracked pet.
     *
     * <p>We key off the pet's own dominator field (vanilla truth) and then confirm
     * the capability list still contains it. This avoids a full player-scan on
     * every damage event.</p>
     */
    private static long ownerOf(Creature creature) {
        if (creature == null || creature.isPlayer()) return -1L;
        try {
            if (!creature.isDominated() || creature.getDominator() == null) return -1L;
            long ownerId = creature.getDominator().getWurmId();

            PlayerPets pets = CapabilityManager.getInstance()
                .getPlayerCapability(ownerId, PlayerPetsCapability.INSTANCE);

            return pets.contains(creature.getWurmId()) ? ownerId : -1L;
        } catch (Throwable t) {
            return -1L;
        }
    }
}

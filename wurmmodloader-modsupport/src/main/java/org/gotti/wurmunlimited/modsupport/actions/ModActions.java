package org.gotti.wurmunlimited.modsupport.actions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Behaviour;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.skills.Skill;
import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.ActionPerformerBase;
import com.garward.wurmmodloader.modsupport.actions.BehaviourProvider;
import com.garward.wurmmodloader.modsupport.actions.ModAction;

/**
 * Legacy ModActions wrapper for backward compatibility.
 * Delegates all calls to the new implementation.
 */
public class ModActions {

	public static int getNextActionId() {
		return com.garward.wurmmodloader.modsupport.actions.ModActions.getNextActionId();
	}

	public static short getLastServerActionId() {
		return com.garward.wurmmodloader.modsupport.actions.ModActions.getLastServerActionId();
	}

	public static void registerAction(ActionEntry actionEntry) {
		com.garward.wurmmodloader.modsupport.actions.ModActions.registerAction(actionEntry);
	}

	public static void registerAction(ModAction testAction) {
		com.garward.wurmmodloader.modsupport.actions.ModActions.registerAction(testAction);
	}

	public static void registerActionPerformer(ActionPerformer actionPerformer) {
		com.garward.wurmmodloader.modsupport.actions.ModActions.registerActionPerformer(actionPerformer);
	}

	public static void registerBehaviourProvider(BehaviourProvider behaviourProvider) {
		com.garward.wurmmodloader.modsupport.actions.ModActions.registerBehaviourProvider(behaviourProvider);
	}

	public static void init() {
		com.garward.wurmmodloader.modsupport.actions.ModActions.init();
	}

	public static ActionPerformerBase getActionPerformer(Action action) {
		return com.garward.wurmmodloader.modsupport.actions.ModActions.getActionPerformer(action);
	}

	public static BehaviourProvider getBehaviourProvider(Behaviour behaviour) {
		return com.garward.wurmmodloader.modsupport.actions.ModActions.getBehaviourProvider(behaviour);
	}

	public static Skill getSkillOrNull(Creature creature, int skillId) {
		return com.garward.wurmmodloader.modsupport.actions.ModActions.getSkillOrNull(creature, skillId);
	}
}

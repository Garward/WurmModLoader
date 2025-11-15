package com.wurmonline.server.questions;

import java.util.Properties;

import com.garward.wurmmodloader.modsupport.questions.ModQuestion;

import com.wurmonline.server.creatures.Creature;

	/**
	 * A wrapper class that bridges the gap between the Wurm server's Question system and modded question implementations.
	 * 
	 * <p>This class allows mod authors to create custom questions that integrate seamlessly with the Wurm questioning system.
	 * It wraps a {@link ModQuestion} instance and delegates the actual question handling to it.</p>
	 *
	 * <p>Usage example:
	 * <pre>@code
	 * ModQuestion customQuestion = new ModQuestion() {
	 *     public void answer(Question question, Properties answers) {
	 *         // Handle the answer
	 *     }
	 *     
	 *     public void sendQuestion(Question question) {
	 *         // Send custom question to player
	 *     }
	 * };
	 * ModQuestionImpl modQuestionImpl = new ModQuestionImpl(player, "Title", "Question?", 0, -10L, customQuestion);
	 * modQuestionImpl.sendQuestion();
	 * </pre>
	 *
	 * @since 1.0.0
	 */
public class ModQuestionImpl extends Question {
	
	private ModQuestion modQuestion;
	/**
	 * Constructs a new ModQuestionImpl with the specified parameters.
	 *
	 * @param responder the creature that will respond to the question
	 * @param title the title of the question dialog
	 * @param question the question text to display
	 * @param type the type of question (see constants in Question class)
	 * @param target the target ID for this question
	 * @param modQuestion the mod question implementation that will handle the actual logic
	 * @since 1.0.0
	 */

	public ModQuestionImpl(final Creature responder, final String title, final String question, final int type, final long target, ModQuestion modQuestion) {
	/**
	 * Processes the answer to this question by delegating to the wrapped ModQuestion.
	 *
	 * <p>This method is called when the player submits their response to the question.
	 * It first sets the answer properties on this instance and then delegates to the
	 * mod question implementation for custom handling.</p>
	 *
	 * @param answers the properties containing the player's answers
	 * @since 1.0.0
	 */
		super(responder, title, question, type, target);
		this.modQuestion = modQuestion;
	/**
	 * Sends the question to the player by delegating to the wrapped ModQuestion.
	 *
	 * <p>This method is called to display the question dialog to the player.
	 * The actual presentation logic is handled by the mod question implementation.</p>
	 *
	 * @since 1.0.0
	 */
	}

	@Override
	public void answer(Properties answers) {
		this.setAnswer(answers);
		modQuestion.answer(this, answers);
	}

	@Override
	public void sendQuestion() {
		modQuestion.sendQuestion(this);
	}
}

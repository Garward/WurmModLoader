package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.questions.Question;

import java.util.Properties;

/**
 * Fired immediately before a {@link Question#answer(Properties)} dispatch in
 * {@code Communicator}. Lets dialog-tweaking mods intercept or veto answers
 * (village upkeep menu, buyer management, remove-item, ...).
 *
 * <p>Cancellation skips the call to {@code answer}. The question is still
 * removed from the active-question registry afterwards, matching vanilla
 * lifecycle.</p>
 *
 * <p>Listeners can mutate the {@link Properties} payload in place to rewrite
 * answers before they reach the vanilla handler.</p>
 */
public class QuestionAnswerEvent extends CancellableEvent {

    private final Question question;
    private final Properties answers;

    public QuestionAnswerEvent(Question question, Properties answers) {
        this.question = question;
        this.answers = answers;
    }

    public Question getQuestion() {
        return question;
    }

    public Properties getAnswers() {
        return answers;
    }
}

package org.gotti.wurmunlimited.modsupport.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

/**
 * Legacy ModQuestions wrapper for backward compatibility.
 * Delegates all static methods to the new implementation.
 */
public final class ModQuestions {

    private ModQuestions() {
        // Prevent instantiation
    }

    public static Question createQuestion(Creature responder, String title, String question, long target, ModQuestion modQuestion) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.createQuestion(
            responder, title, question, target,
            (com.garward.wurmmodloader.modsupport.questions.ModQuestion) modQuestion);
    }

    public static String getBmlHeader(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.getBmlHeader(question);
    }

    public static String getBmlHeaderNoQuestion(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.getBmlHeaderNoQuestion(question);
    }

    public static String getBmlHeaderWithScroll(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.getBmlHeaderWithScroll(question);
    }

    public static String getBmlHeaderWithScrollAndQuestion(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.getBmlHeaderWithScrollAndQuestion(question);
    }

    public static String getBmlHeaderScrollOnly(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.getBmlHeaderScrollOnly(question);
    }

    public static String createAnswerButtonForNoBorder(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.createAnswerButtonForNoBorder(question);
    }

    public static String createAnswerButton2(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.createAnswerButton2(question);
    }

    public static String createAnswerButton2(Question question, String sendText) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.createAnswerButton2(question, sendText);
    }

    public static String createOkAnswerButton(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.createOkAnswerButton(question);
    }

    public static String createBackAnswerButton(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.createBackAnswerButton(question);
    }

    public static String createAnswerButton3(Question question) {
        return com.garward.wurmmodloader.modsupport.questions.ModQuestions.createAnswerButton3(question);
    }
}

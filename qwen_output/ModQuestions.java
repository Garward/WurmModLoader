package com.garward.wurmmodloader.modsupport.questions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.ModQuestionImpl;
import com.wurmonline.server.questions.Question;

/**
 * Utility class for working with {@link Question} objects in Wurm Unlimited mods.
 * 
 * <p>This class provides convenient access to private methods in the {@link Question} class
 * through reflection, allowing modders to create custom question interfaces with various
 * BML (Basic Markup Language) headers and answer buttons.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * // Create a custom question
 * Question question = ModQuestions.createQuestion(
 *     player, 
 *     "Custom Title", 
 *     "Custom question text", 
 *     playerId, 
 *     new MyModQuestion()
 * );
 * 
 * // Get BML header with scroll
 * String bml = ModQuestions.getBmlHeaderWithScroll(question);
 * 
 * // Add custom answer button
 * bml += ModQuestions.createAnswerButton2(question, "Custom Action");
 * 
 * // Send to player
 * question.getResponder().getCommunicator().sendBml(800, 600, true, true, bml, 200, 200, question.getTitle());
 * }</pre>
 * </p>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe as all methods are static
 * and do not maintain state. However, individual {@link Question} instances should be
 * used in a thread-safe manner according to Wurm server threading model.</p>
 * 
 * <p><strong>Lifecycle:</strong> This class should be initialized during mod loading.
 * The static initializer will fail-fast if required reflection methods are not available,
 * indicating an incompatible Wurm server version.</p>
 * 
 * @since 1.0.0
 * @see Question
 * @see ModQuestionImpl
 */
public class ModQuestions {

	private static Method getBmlHeader;
	private static Method getBmlHeaderNoQuestion;
	private static Method getBmlHeaderWithScroll;
	private static Method getBmlHeaderWithScrollAndQuestion;
	private static Method getBmlHeaderScrollOnly;
	private static Method createAnswerButtonForNoBorder;
	private static Method createAnswerButton2;
	private static Method createAnswerButton2_String;
	private static Method createOkAnswerButton;
	private static Method createBackAnswerButton;
	private static Method createAnswerButton3;

	static {
		try {
			getBmlHeader = ReflectionUtil.getMethod(Question.class, "getBmlHeader");
			getBmlHeaderNoQuestion = ReflectionUtil.getMethod(Question.class,  "getBmlHeaderNoQuestion");

			getBmlHeaderWithScroll = ReflectionUtil.getMethod(Question.class,  "getBmlHeaderWithScroll");
			getBmlHeaderWithScrollAndQuestion = ReflectionUtil.getMethod(Question.class,  "getBmlHeaderWithScrollAndQuestion");
			getBmlHeaderScrollOnly = ReflectionUtil.getMethod(Question.class,  "getBmlHeaderScrollOnly");
			createAnswerButtonForNoBorder = ReflectionUtil.getMethod(Question.class,  "createAnswerButtonForNoBorder");
			createAnswerButton2 = ReflectionUtil.getMethod(Question.class,  "createAnswerButton2", new Class<?>[0]);
			createAnswerButton2_String = ReflectionUtil.getMethod(Question.class,  "createAnswerButton2", new Class<?>[] { String.class });
			createOkAnswerButton = ReflectionUtil.getMethod(Question.class,  "createOkAnswerButton");
			createBackAnswerButton = ReflectionUtil.getMethod(Question.class,  "createBackAnswerButton");
			createAnswerButton3 = ReflectionUtil.getMethod(Question.class,  "createAnswerButton3");
			
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new {@link Question} instance using the provided parameters.
	 * 
	 * <p>This method wraps the creation of {@link ModQuestionImpl} to provide a
	 * consistent interface for modders creating custom questions.</p>
	 * 
	 * @param responder the creature that will respond to the question
	 * @param title the title of the question dialog
	 * @param question the question text to display
	 * @param target the target ID for the question (typically player or object ID)
	 * @param modQuestion the {@link ModQuestion} implementation handling the response
	 * @return a new {@link Question} instance
	 * @since 1.0.0
	 * @see ModQuestionImpl
	 */
	public static Question createQuestion(Creature responder, String title, String question, long target, ModQuestion modQuestion) {
		return new ModQuestionImpl(responder, title, question, modQuestion.getType(), target, modQuestion);
	}

	/**
	 * Calls a private method on a {@link Question} instance using reflection.
	 * 
	 * <p>This is an internal utility method used by the public methods in this class
	 * to access private {@link Question} methods.</p>
	 * 
	 * @param <T> the return type of the method
	 * @param question the question instance on which to call the method
	 * @param method the method to call
	 * @param args the arguments to pass to the method
	 * @return the result of the method call
	 * @throws RuntimeException if the method call fails due to reflection issues
	 * @since 1.0.0
	 */
	private static <T> T callPrivateMethod(Question question, Method method, Object... args) {
		try {
			return ReflectionUtil.callPrivateMethod(question, method, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the standard BML header for a question.
	 * 
	 * <p>This header includes the basic question formatting without scroll functionality.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the header
	 * @since 1.0.0
	 * @see #getBmlHeaderWithScroll(Question)
	 */
	public static String getBmlHeader(Question question) {
		return callPrivateMethod(question, getBmlHeader);
	}

	/**
	 * Gets the BML header without question text.
	 * 
	 * <p>This header provides the basic formatting structure but omits the question text display.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the header without question text
	 * @since 1.0.0
	 */
	public static String getBmlHeaderNoQuestion(Question question) {
		return callPrivateMethod(question, getBmlHeaderNoQuestion);
	}

	/**
	 * Gets the BML header with scroll functionality.
	 * 
	 * <p>This header includes scroll bars for longer content, making it suitable for
	 * questions with extensive text.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the header with scroll functionality
	 * @since 1.0.0
	 * @see #getBmlHeader(Question)
	 * @see #getBmlHeaderWithScrollAndQuestion(Question)
	 */
	public static String getBmlHeaderWithScroll(Question question) {
		return callPrivateMethod(question, getBmlHeaderWithScroll);
	}

	/**
	 * Gets the BML header with scroll functionality and question display.
	 * 
	 * <p>This combines scroll functionality with explicit question text display.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the header with scroll and question
	 * @since 1.0.0
	 * @see #getBmlHeaderWithScroll(Question)
	 */
	public static String getBmlHeaderWithScrollAndQuestion(Question question) {
		return callPrivateMethod(question, getBmlHeaderWithScrollAndQuestion);
	}

	/**
	 * Gets the BML header with scroll only (no additional formatting).
	 * 
	 * <p>This provides minimal scroll-only functionality without additional header elements.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing scroll-only header
	 * @since 1.0.0
	 */
	public static String getBmlHeaderScrollOnly(Question question) {
		return callPrivateMethod(question, getBmlHeaderScrollOnly);
	}

	/**
	 * Creates an answer button for no-border display.
	 * 
	 * <p>This creates a button without the standard border styling, useful for
	 * custom UI layouts.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the no-border answer button
	 * @since 1.0.0
	 */
	public static String createAnswerButtonForNoBorder(Question question) {
		return callPrivateMethod(question, createAnswerButtonForNoBorder);
	}

	/**
	 * Creates a standard answer button (version 2).
	 * 
	 * <p>This creates a default-styled answer button with standard behavior.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the answer button
	 * @since 1.0.0
	 * @see #createAnswerButton2(Question, String)
	 */
	public static String createAnswerButton2(Question question) {
		return callPrivateMethod(question, createAnswerButton2);
	}

	/**
	 * Creates a customized answer button (version 2) with specific text.
	 * 
	 * <p>This creates an answer button with custom text, allowing for more descriptive
	 * action buttons.</p>
	 * 
	 * <p>Usage example:
	 * <pre>{@code
	 * String bml = ModQuestions.getBmlHeader(question);
	 * bml += ModQuestions.createAnswerButton2(question, "Accept Quest");
	 * bml += ModQuestions.createAnswerButton2(question, "Decline Quest");
	 * }</pre>
	 * </p>
	 * 
	 * @param question the question instance
	 * @param sendText the text to display on the button
	 * @return BML string representing the customized answer button
	 * @since 1.0.0
	 * @see #createAnswerButton2(Question)
	 */
	public static String createAnswerButton2(Question question, String sendText) {
		return callPrivateMethod(question, createAnswerButton2_String, sendText);
	}

	/**
	 * Creates an OK answer button.
	 * 
	 * <p>This creates a standard OK button, typically used for confirmation dialogs.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the OK button
	 * @since 1.0.0
	 * @see #createBackAnswerButton(Question)
	 */
	public static String createOkAnswerButton(Question question) {
		return callPrivateMethod(question, createOkAnswerButton);
	}

	/**
	 * Creates a Back answer button.
	 * 
	 * <p>This creates a standard Back button, typically used for navigation between
	 * question dialogs.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the Back button
	 * @since 1.0.0
	 * @see #createOkAnswerButton(Question)
	 */
	public static String createBackAnswerButton(Question question) {
		return callPrivateMethod(question, createBackAnswerButton);
	}

	/**
	 * Creates an answer button (version 3).
	 * 
	 * <p>This creates an alternative styled answer button, providing additional
	 * visual options for UI design.</p>
	 * 
	 * @param question the question instance
	 * @return BML string representing the answer button
	 * @since 1.0.0
	 * @see #createAnswerButton2(Question)
	 */
	public static String createAnswerButton3(Question question) {
		return callPrivateMethod(question, createAnswerButton3);
	}

}
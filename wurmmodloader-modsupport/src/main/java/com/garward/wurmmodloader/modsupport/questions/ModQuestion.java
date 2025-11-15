package com.garward.wurmmodloader.modsupport.questions;

import java.util.Properties;

import com.wurmonline.server.questions.Question;

/**
 * Interface for mod questions that handle custom question interactions in Wurm Unlimited.
 * 
 * <p>This interface provides a standardized way for mods to handle question responses and
 * send custom questions to players. Implementing classes can define custom behavior for
 * answering questions and sending question interfaces to players.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * public class CustomModQuestion implements ModQuestion {
 *     
 *     @Override
 *     public void answer(Question question, Properties answers) {
 *         // Handle the question response
 *         String response = answers.getProperty("response");
 *         // Process the response...
 *     }
 *     
 *     @Override
 *     public void sendQuestion(Question question) {
 *         // Send custom BML content
 *         sendBml(question, 300, 300, true, true, 
 *             "text{string=\"Enter your response:\"}input{id=\"response\";maxchars=\"100\"}");
 *     }
 * }
 * }</pre>
 * 
 * <p>Thread-safety: Implementations should be thread-safe as question handling may occur
 * on different server threads. The default methods are thread-safe as they only call
 * external APIs.</p>
 * 
 * @since 1.0.0
 * @see Question
 * @see Properties
 */
public interface ModQuestion {
	
	/**
	 * Handles the response to a question.
	 * 
	 * <p>This method is called when a player submits a response to a question. The
	 * implementation should process the provided answers and perform appropriate
	 * actions based on the response.</p>
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * public void answer(Question question, Properties answers) {
	 *     String action = answers.getProperty("action");
	 *     if ("accept".equals(action)) {
	 *         // Handle acceptance
	 *     } else if ("decline".equals(action)) {
	 *         // Handle decline
	 *     }
	 * }
	 * }</pre>
	 * 
	 * <p>Thread-safety: Implementations should be thread-safe as this method may be
	 * called from different server threads.</p>
	 * 
	 * @param question the question that was answered
	 * @param answers the properties containing the answer data
	 * @since 1.0.0
	 * @see Question
	 * @see Properties
	 */
	public void answer(Question question, final Properties answers);
	
	/**
	 * Sends the question interface to the player.
	 * 
	 * <p>This method is responsible for constructing and sending the BML (Body Markup Language)
	 * content that defines the question interface. The implementation should use one of the
	 * {@link #sendBml(Question, int, int, boolean, boolean, CharSequence)} methods to send
	 * the content.</p>
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * public void sendQuestion(Question question) {
	 *     StringBuilder bml = new StringBuilder();
	 *     bml.append("text{string=\"Do you accept the quest?\"}");
	 *     bml.append("harray{button{id=\"accept\";text=\"Accept\"};button{id=\"decline\";text=\"Decline\"}}");
	 *     sendBml(question, 250, 150, true, true, bml.toString());
	 * }
	 * }</pre>
	 * 
	 * <p>Thread-safety: Implementations should be thread-safe as this method may be
	 * called from different server threads.</p>
	 * 
	 * @param question the question to send
	 * @since 1.0.0
	 * @see Question
	 * @see #sendBml(Question, int, int, boolean, boolean, CharSequence)
	 */
	public void sendQuestion(Question question);
	
	/**
	 * Gets the type identifier for this question.
	 * 
	 * <p>The type identifier is used by the Wurm server to distinguish between different
	 * question types. By default, this returns 0, but implementations can override this
	 * to provide a custom type identifier.</p>
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * public int getType() {
	 *     return CUSTOM_QUEST_QUESTION_TYPE;
	 * }
	 * }</pre>
	 * 
	 * <p>Thread-safety: This method is thread-safe as it only returns a constant value
	 * in the default implementation.</p>
	 * 
	 * @return the type identifier for this question
	 * @since 1.0.0
	 */
	public default int getType() {
		return 0;
	}
	
	/**
	 * Sends BML content to the player with full customization options.
	 * 
	 * <p>This method sends BML (Body Markup Language) content to the player who posed
	 * the question. It provides full control over the window appearance including
	 * dimensions, resizeability, closeability, and color settings.</p>
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * sendBml(question, 400, 300, true, true, 
	 *     "text{string=\"Custom content\"}button{id=\"ok\";text=\"OK\"}", 
	 *     255, 200, 150, "Custom Title");
	 * }</pre>
	 * 
	 * <p>Thread-safety: This method is thread-safe as it only calls external APIs.</p>
	 * 
	 * @param question the question context
	 * @param width the width of the window in pixels
	 * @param height the height of the window in pixels
	 * @param resizeable whether the window can be resized by the player
	 * @param closeable whether the window can be closed by the player
	 * @param content the BML content to display
	 * @param red the red component of the window color (0-255)
	 * @param green the green component of the window color (0-255)
	 * @param blue the blue component of the window color (0-255)
	 * @param title the window title
	 * @since 1.0.0
	 * @see Question
	 * @see #sendBml(Question, int, int, boolean, boolean, CharSequence)
	 */
	public default void sendBml(Question question, int width, int height, boolean resizeable, boolean closeable, CharSequence content, int red, int green, int blue, String title) {
		question.getResponder().getCommunicator().sendBml(width, height, resizeable, closeable, content.toString(), red, green, blue, title);
	}
	
	/**
	 * Sends BML content to the player with default styling.
	 * 
	 * <p>This method sends BML (Body Markup Language) content to the player who posed
	 * the question. It uses default styling parameters including gray color (200,200,200)
	 * and the question's title. This is a convenience method for {@link #sendBml(Question, int, int, boolean, boolean, CharSequence, int, int, int, String)}.</p>
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * sendBml(question, 300, 200, true, true, 
	 *     "text{string=\"Simple message\"}button{id=\"ok\";text=\"OK\"}");
	 * }</pre>
	 * 
	 * <p>Thread-safety: This method is thread-safe as it only calls external APIs.</p>
	 * 
	 * @param question the question context
	 * @param width the width of the window in pixels
	 * @param height the height of the window in pixels
	 * @param resizeable whether the window can be resized by the player
	 * @param closeable whether the window can be closed by the player
	 * @param content the BML content to display
	 * @since 1.0.0
	 * @see Question
	 * @see #sendBml(Question, int, int, boolean, boolean, CharSequence, int, int, int, String)
	 */
	public default void sendBml(Question question, int width, int height, boolean resizeable, boolean closeable, CharSequence content) {
		sendBml(question, width, height, resizeable, closeable, content, 200, 200, 200, question.getTitle());
	}
}
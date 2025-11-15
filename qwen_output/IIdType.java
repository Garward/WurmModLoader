package com.garward.wurmmodloader.modsupport;

/**
 * Interface for defining ID type behavior in the Wurm modding system.
 *
 * <p>This interface provides a contract for managing different types of IDs used within
 * the Wurm game engine. Implementations define how IDs are generated, tracked, and managed
 * for various game entities.</p>
 *
 * <p>Implementations of this interface are typically used by the {@link IdFactory} to
 * generate unique identifiers for game objects. Each implementation represents a specific
 * type of ID (e.g., item IDs, creature IDs, etc.) and defines the generation strategy.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * public class CustomItemIdType implements IIdType {
 *     private int lastUsedId = 0;
 *
 *     @Override
 *     public int startValue() {
 *         return 0;
 *     }
 *
 *     @Override
 *     public boolean isCountingDown() {
 *         return false;
 *     }
 *
 *     @Override
 *     public String typeName() {
 *         return "customItem";
 *     }
 *
 *     @Override
 *     public void updateLastUsedId(int id) {
 *         this.lastUsedId = id;
 *     }
 *
 *     @Override
 *     public int getLastUsedId() {
 *         return this.lastUsedId;
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
public interface IIdType {

	/**
	 * Returns the starting value for generated IDs.
	 *
	 * <p>For server IDs, the start value should be the highest possible value for the ID.
	 * The IdFactory will count down towards the real server IDs. For non-server IDs,
	 * the value should be 0 and {@link #isCountingDown()} should return false.</p>
	 *
	 * <p>This method is typically called during the initialization phase of the ID generation system.</p>
	 *
	 * @return the starting value for ID generation
	 * @since 1.0.0
	 */
	int startValue();

	/**
	 * Indicates whether generated IDs will increase or decrease.
	 *
	 * <p>When this method returns {@code true}, IDs are generated in descending order
	 * (counting down from {@link #startValue()}). When it returns {@code false}, IDs
	 * are generated in ascending order starting from {@link #startValue()}.</p>
	 *
	 * @return {@code true} if IDs are counting down, {@code false} if counting up
	 * @since 1.0.0
	 */
	boolean isCountingDown();

	/**
	 * Returns the property name associated with this ID type.
	 *
	 * <p>This name is typically used for configuration purposes and logging.
	 * It should be a unique identifier for this specific ID type within the system.</p>
	 *
	 * @return the property name for this ID type, never {@code null}
	 * @since 1.0.0
	 */
	String typeName();

	/**
	 * Updates the last currently used item ID.
	 *
	 * <p>This method is called by the ID generation system to track the most recently
	 * used ID value. Implementations should store this value for future reference.</p>
	 *
	 * <p>This method may be called from multiple threads during ID generation, so
	 * implementations should ensure thread-safety if shared state is modified.</p>
	 *
	 * @param id the ID value to set as the last used ID
	 * @since 1.0.0
	 */
	void updateLastUsedId(int id);

	/**
	 * Returns the last used ID.
	 *
	 * <p>This can be the lowest or highest used ID depending on {@link #isCountingDown()}.
	 * When counting down, this represents the highest ID that has been used. When counting
	 * up, this represents the lowest ID that has been used.</p>
	 *
	 * <p>Implementations should ensure this method is thread-safe as it may be called
	 * concurrently during ID generation.</p>
	 *
	 * @return the last used ID value
	 * @since 1.0.0
	 */
	int getLastUsedId();
}
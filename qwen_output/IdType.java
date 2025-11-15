package com.garward.wurmmodloader.modsupport;

/**
 * Default id types used for managing unique identifiers within the Wurm modding system.
 *
 * <p>This enum provides predefined {@link IIdType} implementations for common Wurm game objects
 * such as items, creatures, skills, and player properties. Each type defines its own starting
 * value and counting direction to ensure proper ID allocation without conflicts.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Get the next available item template ID
 * int newItemId = IdType.ITEMTEMPLATE.getNextId();
 *
 * // Update the last used ID for a creature template
 * IdType.CREATURETEMPLATE.updateLastUsedId(1000000);
 *
 * // Check the current last used skill ID
 * int lastSkillId = IdType.SKILL.getLastUsedId();
 * }</pre>
 * </p>
 *
 * <p><strong>Thread Safety:</strong> This enum is thread-safe for read operations. However,
 * the {@link #updateLastUsedId(int)} method modifies internal state and should be called
 * from a single thread or properly synchronized in multi-threaded environments.</p>
 *
 * <p><strong>Lifecycle:</strong> Instances of this enum are created during class loading
 * and remain available throughout the application lifecycle.</p>
 *
 * @since 1.0.0
 * @see IIdType
 */
public enum IdType implements IIdType {
	/**
	 * Item template ID type.
	 *
	 * <p>Starts counting from 22767 and counts downward to avoid conflicts with
	 * vanilla Wurm item templates.</p>
	 *
	 * @since 1.0.0
	 */
	ITEMTEMPLATE {
		@Override
		public int startValue() {
			return 22767;
		}
	},
	
	/**
	 * Creature template ID type.
	 *
	 * <p>Starts counting from {@link Integer#MAX_VALUE} and counts downward to ensure
	 * no conflicts with vanilla creature templates.</p>
	 *
	 * @since 1.0.0
	 */
	CREATURETEMPLATE {
		@Override
		public int startValue() {
			return Integer.MAX_VALUE;
		}
	},
	
	/**
	 * Skill ID type.
	 *
	 * <p>Starts counting from {@link Integer#MAX_VALUE} and counts downward to ensure
	 * no conflicts with vanilla skills.</p>
	 *
	 * @since 1.0.0
	 */
	SKILL {
		@Override
		public int startValue() {
			return Integer.MAX_VALUE;
		}
	},
	
	/**
	 * Player property ID type.
	 *
	 * <p>Starts counting from 0 and counts upward. This type does not use the default
	 * counting down behavior.</p>
	 *
	 * @since 1.0.0
	 */
	PLAYERPROPERTY {
		@Override
		public int startValue() {
			return 0;
		}

		@Override
		public boolean isCountingDown() {
			return false;
		}
	};

	/**
	 * The last used ID for this type. This value is updated when {@link #updateLastUsedId(int)}
	 * is called and is used to determine the next available ID.
	 *
	 * @since 1.0.0
	 */
	private int lastUsedId = startValue();

	/**
	 * Returns the starting value for this ID type.
	 *
	 * <p>Each ID type has a specific starting value that determines where ID allocation
	 * begins. For downward counting types, this is the highest possible value. For upward
	 * counting types, this is the lowest possible value.</p>
	 *
	 * @return the starting value for ID allocation
	 * @since 1.0.0
	 */
	@Override
	public abstract int startValue();

	/**
	 * Returns whether this ID type counts downward or upward.
	 *
	 * <p>Most ID types count downward from their starting value to avoid conflicts with
	 * vanilla Wurm IDs which typically start from lower values and count upward. The
	 * {@link #PLAYERPROPERTY} type is an exception that counts upward.</p>
	 *
	 * @return {@code true} if this type counts downward, {@code false} if it counts upward
	 * @since 1.0.0
	 */
	@Override
	public boolean isCountingDown() {
		return true;
	}

	/**
	 * Returns the type name for this ID type.
	 *
	 * <p>The type name is used for identification and logging purposes. By default,
	 * this returns the enum constant name.</p>
	 *
	 * @return the type name as a string
	 * @since 1.0.0
	 */
	@Override
	public String typeName() {
		return name();
	}

	/**
	 * Updates the last used ID for this type.
	 *
	 * <p>If this type counts downward, the last used ID is set to the minimum of the
	 * current last used ID and the provided ID. If this type counts upward, the last
	 * used ID is set to the maximum of the current last used ID and the provided ID.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method modifies internal state and should
	 * be called from a single thread or properly synchronized in multi-threaded environments.</p>
	 *
	 * @param id the ID to compare with the current last used ID
	 * @since 1.0.0
	 */
	@Override
	public void updateLastUsedId(int id) {
		if (isCountingDown()) {
			lastUsedId = Math.min(lastUsedId, id);
		} else {
			lastUsedId = Math.max(lastUsedId, id);
		}
	}

	/**
	 * Returns the last used ID for this type.
	 *
	 * <p>This value represents the boundary for ID allocation. For downward counting
	 * types, IDs should be allocated below this value. For upward counting types,
	 * IDs should be allocated above this value.</p>
	 *
	 * @return the last used ID
	 * @since 1.0.0
	 */
	@Override
	public int getLastUsedId() {
		return lastUsedId;
	}
}
package com.garward.wurmmodloader.modsupport.properties;

/**
 * Represents a property that can store different types of values with expiration support.
 *
 * <p>This class provides a flexible way to store properties with various value types including
 * integer, string, and numeric (float) values. Each property has a unique identifier, creation
 * timestamp, and optional expiration time.</p>
 *
 * <p>Properties can be used to store configuration data, player preferences, or any other
 * key-value pairs that may need to expire after a certain period of time.</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Create a simple property
 * Property prop = new Property();
 * prop.setId(12345L);
 * prop.setStrValue("example_value");
 * prop.setCreated(System.currentTimeMillis());
 *
 * // Create an expiring property
 * Property tempProp = new Property();
 * tempProp.setId(67890L);
 * tempProp.setIntValue(42L);
 * tempProp.setCreated(System.currentTimeMillis());
 * tempProp.setExpires(System.currentTimeMillis() + 3600000); // Expires in 1 hour
 *
 * // Check if property is expired
 * if (System.currentTimeMillis() > tempProp.getExpires()) {
 *     // Handle expired property
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class Property {

	private long id;
	private long created;
	private long expires = Long.MAX_VALUE;
	
	private Long intValue;
	private String strValue;
	private Float numValue;
	
	/**
	 * Gets the unique identifier for this property.
	 *
	 * @return the property ID
	 * @since 1.0.0
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Sets the unique identifier for this property.
	 *
	 * @param id the property ID to set
	 * @since 1.0.0
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	/**
	 * Gets the integer value of this property.
	 *
	 * @return the integer value, or {@code null} if no integer value is set
	 * @since 1.0.0
	 */
	public Long getIntValue() {
		return intValue;
	}
	
	/**
	 * Sets the integer value of this property.
	 *
	 * @param intValue the integer value to set, or {@code null} to clear the value
	 * @since 1.0.0
	 */
	public void setIntValue(Long intValue) {
		this.intValue = intValue;
	}
	
	/**
	 * Gets the string value of this property.
	 *
	 * @return the string value, or {@code null} if no string value is set
	 * @since 1.0.0
	 */
	public String getStrValue() {
		return strValue;
	}
	
	/**
	 * Sets the string value of this property.
	 *
	 * @param strValue the string value to set, or {@code null} to clear the value
	 * @since 1.0.0
	 */
	public void setStrValue(String strValue) {
		this.strValue = strValue;
	}
	
	/**
	 * Gets the numeric (float) value of this property.
	 *
	 * @return the numeric value, or {@code null} if no numeric value is set
	 * @since 1.0.0
	 */
	public Float getNumValue() {
		return numValue;
	}
	
	/**
	 * Sets the numeric (float) value of this property.
	 *
	 * @param numValue the numeric value to set, or {@code null} to clear the value
	 * @since 1.0.0
	 */
	public void setNumValue(Float numValue) {
		this.numValue = numValue;
	}
	
	/**
	 * Gets the creation timestamp of this property.
	 *
	 * @return the creation timestamp as milliseconds since epoch
	 * @since 1.0.0
	 */
	public long getCreated() {
		return created;
	}
	
	/**
	 * Sets the creation timestamp of this property.
	 *
	 * @param created the creation timestamp as milliseconds since epoch
	 * @since 1.0.0
	 */
	public void setCreated(long created) {
		this.created = created;
	}
	
	/**
	 * Gets the expiration timestamp of this property.
	 *
	 * <p>By default, properties are created with {@link Long#MAX_VALUE} as expiration,
	 * meaning they never expire unless explicitly set otherwise.</p>
	 *
	 * @return the expiration timestamp as milliseconds since epoch, or {@link Long#MAX_VALUE}
	 *         if the property does not expire
	 * @since 1.0.0
	 */
	public long getExpires() {
		return expires;
	}
	
	/**
	 * Sets the expiration timestamp of this property.
	 *
	 * <p>Set to {@link Long#MAX_VALUE} to make the property never expire (default).</p>
	 *
	 * @param expires the expiration timestamp as milliseconds since epoch, or {@link Long#MAX_VALUE}
	 *                to make the property never expire
	 * @since 1.0.0
	 */
	public void setExpires(long expires) {
		this.expires = expires;
	}
}
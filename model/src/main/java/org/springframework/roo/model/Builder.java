package org.springframework.roo.model;

/**
 * Indicates an implementation that helps users build objects from scratch or from existing objects.
 * 
 * <p>
 * {@link Builder} objects are used in Spring Roo because most Roo types are intentionally and strictly immutable. This can make
 * such objects tedious to create and modify. {@link Builder}s help create new instances using simple and convenient methods
 * which reflect default values. They also help users edit existing instances.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface Builder<T, R extends Builder<T, R>> {

	/**
	 * @return the immutable object this builder creates (never returns null, but may throw an exception)
	 */
	T build();

	/**
	 * @return this instance cast to the correct concrete implementation (never returns null)
	 */
	R getThis();
}

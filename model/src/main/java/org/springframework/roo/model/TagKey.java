package org.springframework.roo.model;

/**
 * A type safe type that provides a key for tagging and validating
 * {@link org.springframework.roo.model.CustomDataAccessor} objects.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public interface TagKey<T> {

	void validate(T taggedInstance) throws IllegalStateException;
}

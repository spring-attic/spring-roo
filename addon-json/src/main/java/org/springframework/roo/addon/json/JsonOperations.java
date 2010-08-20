package org.springframework.roo.addon.json;

import org.springframework.roo.model.JavaType;

/**
 * Interface of operations for addon-json operations.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface JsonOperations {

	/**
	 * Indicates whether this commands for this addon should be available
	 * 
	 * @return true if commands are available
	 */
	boolean isCommandAvailable();

	/**
	 * Annotate a given {@link JavaType} with @{@link RooJson} annotation.
	 * 
	 * @param type The type to annotate
	 */
	void annotateType(JavaType type);
}
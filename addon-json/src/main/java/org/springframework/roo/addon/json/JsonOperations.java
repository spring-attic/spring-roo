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
	 * @param type The type to annotate (required)
	 * @param rootName The root name which should be used to wrap the JSON document (optional)
	 */
	void annotateType(JavaType type, String rootName);
	
	/**
	 * Annotate all types in the project which are annotated with @{@link org.springframework.roo.addon.javabean.RooJavaBean}.
	 */
	void annotateAll();
}
package org.springframework.roo.addon.jsf;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Provides JSF managed bean operations.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public interface JsfOperations {

	boolean isSetupJsfAvailable();
	
	boolean isJsfAvailable();

	void setupJsf(JsfImplementation jsfImplementation);

	void generateAll(JavaPackage destinationPackage);

	void createManagedBean(JavaType managedBean, JavaType entity);
}
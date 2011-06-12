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

	boolean isSetupAvailable();

	boolean isScaffoldAvailable();

	void setup(JsfImplementation jsfImplementation);

	void changeJsfImplementation(JsfImplementation jsfImplementation);

	void generateAll(JavaPackage destinationPackage);

	void createManagedBean(JavaType managedBean, JavaType entity, boolean includeOnMenu);
}
package org.springframework.roo.addon.jsf;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
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

	void setup(JsfImplementation jsfImplementation, Theme theme);

	void generateAll(JavaPackage destinationPackage);

	void createManagedBean(JavaType managedBean, JavaType entity, String beanName, boolean includeOnMenu, boolean createConverter);

	void addFileUploadField(JavaSymbolName fieldName, JavaType typeName, String fileName, UploadedFileContentType contentType, String column, Boolean notNull, boolean permitReservedWords);
}
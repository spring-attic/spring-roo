package org.springframework.roo.addon.jsf;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;

/**
 * Provides JSF managed-bean operations.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public interface JsfOperations extends Feature {

	boolean isJsfInstallationPossible();

	boolean isScaffoldOrMediaAdditionAvailable();

	void setup(JsfImplementation jsfImplementation, JsfLibrary jsfLibrary, Theme theme);

	void generateAll(JavaPackage destinationPackage);

	void createManagedBean(JavaType managedBean, JavaType entity, String beanName, boolean includeOnMenu);

	void addMediaSuurce(String url, MediaPlayer mediaPlayer);
}
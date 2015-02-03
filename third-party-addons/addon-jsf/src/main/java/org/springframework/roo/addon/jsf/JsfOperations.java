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

    void addMediaSuurce(String url, MediaPlayer mediaPlayer);

    void createManagedBean(JavaType managedBean, JavaType entity,
            String beanName, boolean includeOnMenu);

    void generateAll(JavaPackage destinationPackage);

    boolean isJsfInstallationPossible();

    boolean isScaffoldOrMediaAdditionAvailable();

    void setup(JsfImplementation jsfImplementation, JsfLibrary jsfLibrary,
            Theme theme);
}
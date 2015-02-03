package org.springframework.roo.addon.gwt;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;

/**
 * Provides GWT operations.
 * 
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.1
 */
public interface GwtOperations extends Feature {

    /**
     * The delimiter for multi-level paths specified by a "<source path="..." />
     * element in a module's *.gwt.xml file.
     */
    String PATH_DELIMITER = "/";

    boolean isGwtInstallationPossible();

    boolean isScaffoldAvailable();

    void proxyAll(JavaPackage proxyPackage);

    void proxyAndRequestAll(JavaPackage proxyAndRequestPackage);

    void proxyAndRequestType(JavaPackage proxyAndRequestPackage, JavaType type);

    void proxyType(JavaPackage proxyPackage, JavaType type);

    void requestAll(JavaPackage requestPackage);

    void requestType(JavaPackage requestPackage, JavaType type);

    void scaffoldAll(JavaPackage proxyPackage, JavaPackage requestPackage);

    void scaffoldType(JavaPackage proxyPackage, JavaPackage requestPackage,
            JavaType type);

    void setup();

    void updateGaeConfiguration();
}
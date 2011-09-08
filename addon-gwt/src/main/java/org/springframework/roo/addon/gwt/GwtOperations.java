package org.springframework.roo.addon.gwt;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Provides GWT installation services.
 *
 * @author Ben Alex
 * @since 1.1
 */
public interface GwtOperations {

	boolean isSetupAvailable();

	boolean isGwtEnabled();

	void setup();

	void proxyAll(JavaPackage proxyPackage);

	void proxyType(JavaPackage proxyPackage, JavaType type);

	void requestAll(JavaPackage requestPackage);

	void requestType(JavaPackage requestPackage, JavaType type);

	void proxyAndRequestAll(JavaPackage proxyAndRequestPackage);

	void proxyAndRequestType(JavaPackage proxyAndRequestPackage, JavaType type);

	void scaffoldAll();

	void scaffoldType(JavaType type);

	void updateGaeConfiguration();
}
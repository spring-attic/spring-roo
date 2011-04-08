package org.springframework.roo.addon.gwt;

/**
 * Provides GWT installation services.
 *
 * @author Ben Alex
 * @since 1.1
 */
public interface GwtOperations {

	boolean isSetupAvailable();

	void setup();
}
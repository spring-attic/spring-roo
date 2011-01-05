package org.springframework.roo.addon.gwt;

/**
 * Interface for {@link GwtOperationsImpl}.
 *
 * @author Ben Alex
 */
public interface GwtOperations {

	boolean isSetupGwtAvailable();

	void setupGwt();
}
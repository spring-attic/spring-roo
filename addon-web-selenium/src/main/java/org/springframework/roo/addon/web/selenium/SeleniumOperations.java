package org.springframework.roo.addon.web.selenium;

import org.springframework.roo.model.JavaType;

/**
 * Provides Selenium operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface SeleniumOperations {

    /**
     * Creates a new Selenium testcase
     * 
     * @param controller the JavaType of the controller under test (required)
     * @param name the name of the test case (optional)
     * @param serverURL the URL of the Selenium server (optional)
     */
    void generateTest(JavaType controller, String name, String serverURL);

    boolean isSeleniumInstallationPossible();
}
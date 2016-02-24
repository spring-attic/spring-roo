package org.springframework.roo.addon.web.selenium;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'selenium' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class SeleniumCommands implements CommandMarker {

  @Reference
  private SeleniumOperations seleniumOperations;

  @CliAvailabilityIndicator({"selenium all", "selenium test"})
  public boolean isJdkFieldManagementAvailable() {
    return seleniumOperations.isSeleniumInstallationPossible();
  }

  @CliCommand(value = "selenium test",
      help = "Creates a new Selenium test for a particular controller")
  public void generateTest(
      @CliOption(key = "controller", mandatory = true,
          help = "Controller to create a Selenium test for") final JavaType controller,
      @CliOption(key = "name", mandatory = false, help = "Name of the test") final String name,
      @CliOption(
          key = "serverUrl",
          mandatory = false,
          unspecifiedDefaultValue = "http://localhost:8080/",
          specifiedDefaultValue = "http://localhost:8080/",
          help = "URL of the server where the web application is available, including protocol, port and hostname") final String url) {

    seleniumOperations.generateTest(controller, name, url);
  }

  @CliCommand(value = "selenium all", help = "Creates a Selenium tests for all controllers")
  public void generateAll(
      @CliOption(
          key = "serverUrl",
          mandatory = false,
          unspecifiedDefaultValue = "http://localhost:8080/",
          specifiedDefaultValue = "http://localhost:8080/",
          help = "URL of the server where the web application is available, including protocol, port and hostname") final String url) {

    seleniumOperations.generateAll(url);
  }



}

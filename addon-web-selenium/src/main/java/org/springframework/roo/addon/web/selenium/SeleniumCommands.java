package org.springframework.roo.addon.web.selenium;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'selenium' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class SeleniumCommands implements CommandMarker {
	
	private SeleniumOperations seleniumOperations;
	
	public SeleniumCommands(SeleniumOperations seleniumOperations) {
		Assert.notNull(seleniumOperations, "Selenium operations required");
		this.seleniumOperations = seleniumOperations;
	}
	
	@CliAvailabilityIndicator({"selenium test"})
	public boolean isJdkFieldManagementAvailable() {
		return seleniumOperations.isProjectAvailable();
	}
	
	@CliCommand(value="selenium test", help="Creates a new Selenium test for a particular controller")
	public void generateTest(
			@CliOption(key="controller", mandatory=true, help="Controller to create a Selenium test for") JavaType controller, 
			@CliOption(key="name", mandatory=false, help="Name of the test") String name,
			@CliOption(key="serverUrl", mandatory=false, unspecifiedDefaultValue="http://localhost:8080/", specifiedDefaultValue="http://localhost:8080/", help="URL of the server where the web application is available, including protocol, port and hostname") String url){
		seleniumOperations.generateTest(controller, name, url);
	}
}
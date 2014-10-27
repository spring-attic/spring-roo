package org.springframework.roo.addon.cloud;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.providers.CloudProviderId;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Provides commands to install Cloud Provider that provides functions to deploy
 * Spring Roo Application on Cloud Servers.
 * 
 * @author Juan Carlos García del Canto
 * @since 1.2.6
 */
@Component
@Service
public class CloudCommands implements CommandMarker {

	/**
	 * Get a reference to the CloudOperations from the underlying OSGi container
	 */
	@Reference
	private CloudOperations operations;

	@Reference
	private TypeLocationService typeLocationService;

	/**
	 * This method checks if the setup method is available
	 * 
	 * @return true (default) if the command should be visible at this stage,
	 *         false otherwise
	 */
	@CliAvailabilityIndicator("cloud setup")
	public boolean isSetupCommandAvailable() {
		return operations.isSetupCommandAvailable();
	}

	/**
	 * This method registers a command with the Roo shell. It also offers two
	 * command attributes, a mandatory one and an optional command which has a
	 * default value.
	 * 
	 * @param provider
	 * @param configuration
	 */
	@CliCommand(value = "cloud setup", help = "Setup Cloud Provider on Spring Roo Project")
	public void setup(
			@CliOption(key = "provider", mandatory = true, help = "Cloud Provider's Name") CloudProviderId provider,
			@CliOption(key = "configuration", mandatory = false, help = "Plugin Configuration. Add configuration by command like 'key=value,key2=value2,key3=value3'") String configuration) {
		operations.installProvider(provider, configuration);
	}

}
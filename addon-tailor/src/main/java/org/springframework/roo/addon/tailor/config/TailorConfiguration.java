package org.springframework.roo.addon.tailor.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Data container for a tailor configuration.
 * Defines a set of {@link CommandConfiguration} objects that define
 * which actions should be triggered by which commands when this
 * configuration is activated
 * 
 * @author Birgitta Boeckeler
 * @since 1.2.0
 */
public class TailorConfiguration {

	private List<CommandConfiguration> commandConfigs = new ArrayList<CommandConfiguration>();
	
	private String name;
	
	private String description;
	
	/**
	 * Constructor
	 * 
	 * @param name
	 *            Name of the configuration. Should be unique over all
	 *            TailorConfiguration instances in the container
	 */
	public TailorConfiguration(String name) {
		this.name = name;
	}
	
	public TailorConfiguration(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
	/**
	 * Looks up the CommandConfiguration for a specific command.
	 * 
	 * @param fullCommandString
	 *            The command string to check
	 * @return CommandConfiguration for the command in this TailorConfiguration;
	 *         null if no configuration present for the command
	 */
	public CommandConfiguration getCommandConfigFor(String fullCommandString) {
		for (CommandConfiguration config : commandConfigs) {
			if (fullCommandString.startsWith(config.getCommandName())) {
				return config;
			}
		}
		return null;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void addCommandConfig(CommandConfiguration newConfig) {
		this.commandConfigs.add(newConfig);
	}
	
	public List<CommandConfiguration> getCommandConfigs() {
		return this.commandConfigs;
	}
	
}

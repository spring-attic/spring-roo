package org.springframework.roo.shell.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.shell.CliCommand;

/**
 * Provides a simple representation of an {@link CliCommand}.
 * 
 * <p>
 * Immutable once constructed.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public class CommandInfo {
	private List<String> commandNames = new ArrayList<String>();
	private String help = "";
	private List<CommandOption> commandOptions = new ArrayList<CommandOption>();
	
	public List<String> getCommandNames() {
		return Collections.unmodifiableList(commandNames);
	}

	public String getHelp() {
		return help;
	}

	public List<CommandOption> getCommandOptions() {
		return Collections.unmodifiableList(commandOptions);
	}

	public static CommandInfoBuilder builder() {
		return new CommandInfo().new CommandInfoBuilder();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((commandNames == null) ? 0 : commandNames.hashCode());
		result = prime * result
				+ ((commandOptions == null) ? 0 : commandOptions.hashCode());
		result = prime * result + ((help == null) ? 0 : help.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommandInfo other = (CommandInfo) obj;
		if (commandNames == null) {
			if (other.commandNames != null)
				return false;
		} else if (!commandNames.equals(other.commandNames))
			return false;
		if (commandOptions == null) {
			if (other.commandOptions != null)
				return false;
		} else if (!commandOptions.equals(other.commandOptions))
			return false;
		if (help == null) {
			if (other.help != null)
				return false;
		} else if (!help.equals(other.help))
			return false;
		return true;
	}

	public class CommandInfoBuilder {
		public CommandInfoBuilder() {}
		
		public CommandInfoBuilder addCommandName(String commandName) {
			if (commandName == null || "".equals(commandName)) throw new IllegalStateException("Command name required");
			commandNames.add(commandName);
			return this;
		}

		public CommandInfoBuilder addCommandOption(CommandOption commandOption) {
			if (commandOption == null) throw new IllegalStateException("Command option required");
			commandOptions.add(commandOption);
			return this;
		}
		
		public CommandInfoBuilder setHelp(String newHelp) {
			help = newHelp;
			return this;
		}
		
		public CommandInfo build() {
			CommandInfo result = new CommandInfo();
			result.commandNames = commandNames;
			result.commandOptions = commandOptions;
			result.help = help;
			return result;
		}
	}

	@Override
	public String toString() {
		return "CommandInfo [commandNames=" + commandNames
				+ ", commandOptions=" + commandOptions + ", help=" + help + "]";
	}
	
}
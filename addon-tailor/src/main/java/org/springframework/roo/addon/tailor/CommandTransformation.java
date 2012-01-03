package org.springframework.roo.addon.tailor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.roo.shell.ParserUtils;

/**
 * Data container to transport an input command and its
 * arguments through all configured actions, while those
 * actions fill up a list of outputCommands.
 *
 * @author Vladimir Tihomirov
 * @author Birgitta Boeckeler
 */
public class CommandTransformation {
	
	/**
	 * The full input command string, incl. arguments
	 */
	private String inputCommand;
	
	/**
	 * A list of output commands, result of the transformation
	 * the inputCommand goes through by action executions.
	 */
	private List<String> outputCommands = new ArrayList<String>();

	/**
	 * Parsed tokens of the command
	 * <br>- Arguments will be represented with key=argumentname without "--", value=argumentvalue
	 * <br>- The command elements before the actual "--" arguments will be in an entry without a key
	 */
	private Map <String, String> arguments; 
	
	public CommandTransformation(String command) {
		this.setInputCommand(command.trim());
		this.setArguments(ParserUtils.tokenize(this.inputCommand));
	}
	
	public String getInputCommand() {
		return inputCommand;
	}

	public void setInputCommand(String command) {
		this.inputCommand = command;
	}

	public Map<String, String> getArguments() {
		return arguments;
	}

	public void setArguments(Map<String, String> options) {
		this.arguments = options;
	}
	
	public void addOutputCommand(String... commandFragments) {
		String outputCommand = "";
		for (String arg : commandFragments) {
			outputCommand = outputCommand.concat(arg);
		}
		this.outputCommands.add(outputCommand);
	}
	
	public List<String> getOutputCommands() {
		return this.outputCommands;
	}

	public void clearCommands() {
		this.outputCommands.clear();
	}
}

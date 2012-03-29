package org.springframework.roo.addon.tailor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.roo.shell.ParserUtils;

/**
 * Data container to transport an input command and its arguments through all
 * configured actions, while those actions fill up a list of outputCommands.
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
     * A list of output commands, result of the transformation the inputCommand
     * goes through by action executions.
     */
    private final List<String> outputCommands = new ArrayList<String>();

    /**
     * Parsed tokens of the command <br>
     * - Arguments will be represented with key=argumentname without "--",
     * value=argumentvalue <br>
     * - The command elements before the actual "--" arguments will be in an
     * entry without a key
     */
    private Map<String, String> arguments;

    public CommandTransformation(final String command) {
        setInputCommand(command.trim());
        // ParserUtils.tokenize expects single blanks to split the command:
        // Make sure that there are no obsolete blanks in the command string
        while (inputCommand.contains("  ")) {
            inputCommand = inputCommand.replace("  ", " ");
        }
        setArguments(ParserUtils.tokenize(inputCommand));
    }

    public void addOutputCommand(final String... commandFragments) {
        String outputCommand = "";
        for (final String arg : commandFragments) {
            outputCommand = outputCommand.concat(arg);
        }
        outputCommands.add(outputCommand);
    }

    public void clearCommands() {
        outputCommands.clear();
    }

    public Map<String, String> getArguments() {
        return arguments;
    }

    public String getInputCommand() {
        return inputCommand;
    }

    public List<String> getOutputCommands() {
        return outputCommands;
    }

    public void setArguments(final Map<String, String> options) {
        arguments = options;
    }

    public void setInputCommand(final String command) {
        inputCommand = command;
    }
}

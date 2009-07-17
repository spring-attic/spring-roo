package org.springframework.roo.shell;

import org.springframework.roo.shell.event.ShellStatusProvider;

/**
 * Specifies the contract for an interactive shell.
 * 
 * <p>
 * Any interactive shell class which implements these methods can be launched by the roo-bootstrap mechanism.
 * 
 * <p>
 * It is envisaged implementations will be provided for JLine initially, with possible implementations for
 * Eclipse in the future.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface Shell extends ShellStatusProvider, ShellPromptAccessor {

	/**
	 * Presents a console prompt and allows the user to interact with the shell. The shell should not return
	 * to the caller until the user has finished their session (by way of a "quit" or similar command).
	 */
	void promptLoop();

	/**
	 * Runs the specified command. Control will return to the caller after the command is run. 
	 * 
	 * @param line to execute (required)
	 * @return true if the command was successful, false if there was an exception
	 */
	boolean executeCommand(String line);

	/**
	 * Only attempts to exit after user's next command has been completed.
	 */
	void requestExit();
	
	/**
	 * @return the parser (never null)
	 */
	SimpleParser getParser();
	
	/**
	 * Indicates the shell should switch into a lower-level development mode. The exact meaning varies by
	 * shell implementation.
	 * 
	 * @param developmentMode true if development mode should be enabled, false otherwise
	 */
	void setDevelopmentMode(boolean developmentMode);

}
package org.springframework.roo.shell;

/**
 * 
 * All components that need to run code before the execution of 
 * a command, when command completes successfully or when command
 * fails, needs to implement this Interface.
 * 
 * @author Juan Carlos García
 * @since 1.3.1
 *
 */
public interface CommandListener {
	
	/**
	 * This method will be executed when some command finish 
	 * success  
	 */
	public void onCommandSuccess();
	
	/**
	 * This method will be executed when some command fails
	 */
	public void onCommandFails();

	/**
	 * This method will be executed before command execution
	 * 
	 * @param parseResult 
	 */
	public void onCommandBegin(ParseResult parseResult);

}

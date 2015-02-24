package org.springframework.roo.shell;

import org.springframework.shell.core.Shell;

/**
 * Extends Spring Shell with extra methods
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public interface RooShell extends Shell {

	/**
	 * To add new CommandListener on Shell object, use addListener method.
	 *
	 * @param listener
	 */
	void addListerner(CommandListener listener);

	/**
	 * To remove CommandListener on Shell object, use removeListener method
	 *
	 * @param listener
	 */
	void removeListener(CommandListener listener);
}
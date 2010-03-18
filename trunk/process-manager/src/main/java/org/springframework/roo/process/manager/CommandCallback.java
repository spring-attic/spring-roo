package org.springframework.roo.process.manager;

/**
 * An interface used to execute a command within a {@link ProcessManager} "transaction-like" operation.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface CommandCallback<T> {

	/**
	 * Execute the user-defined logic.
	 * 
	 * @return a result of the logic (can be null)
	 */
	T callback();
}

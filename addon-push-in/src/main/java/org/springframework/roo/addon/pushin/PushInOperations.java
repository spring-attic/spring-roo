package org.springframework.roo.addon.pushin;

import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link PushInOperationsImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface PushInOperations {

	/**
	 * Method that checks if push-in operation is available or not.
	 * 
	 * "push-in" command will be available only if some project was generated.
	 * 
	 * @return true if some project was created on focused directory.
	 */
	boolean isPushInCommandAvailable();

	/**
	 * Method that register push-in command on Spring Roo Shell.
	 * 
	 * Push-in all methods and fields declared on project ITDs to its .java
	 * files.
	 */
	void pushInAll();

	/**
	 * Method that register "push-in class" command on Spring Roo Shell.
	 * 
	 * Push-in all methods and fields declared on an specified class ITDs to its
	 * .java files.
	 * 
	 * @param klass
	 *            JavaType with the specified class where developer wants to
	 *            make push-in
	 * 
	 */
	void pushInClass(JavaType klass);
}
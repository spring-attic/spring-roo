package org.springframework.roo.felix.help;


/**
 * Provides Help Operations
 * 
 * @author Juan Carlos García
 * @since 1.3
 */
public interface HelpService {

	/**
	 * Obtains Help Reference Guide
	 */
	void helpReferenceGuide();
	
	/**
	 * Obtains Help
	 */
	void obtainHelp(String buffer);

}

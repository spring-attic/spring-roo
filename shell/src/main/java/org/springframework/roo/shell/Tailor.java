package org.springframework.roo.shell;

import java.util.List;


public interface Tailor {

	/**
	 * Single point of rules execution 
	 * 
	 * @param command - roo command line
	 * @return - adjusted command or list of commands. null if command is not tailored
	 */
	List<String> sew(String command);
}

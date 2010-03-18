package org.springframework.roo.support.classloader;

import java.net.URL;
import java.util.List;

/**
 * Allows searching for classpath resources.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ClasspathSearcher {
	/**
	 * Locates all resources on the classpath which match the Ant pattern given.
	 * 
	 * @param antPath an Ant pattern (required)
	 * @return zero or more resources that were found
	 */
	List<URL> findMatchingClasspathResources(String antPath);
}

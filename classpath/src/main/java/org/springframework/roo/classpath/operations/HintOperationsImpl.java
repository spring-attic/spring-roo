package org.springframework.roo.classpath.operations;

import java.io.File;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.AbstractShell;

/**
 * Base implementation of {@link HintOperations}.
 * 
 * <p>
 * This implementation relies on a predefined resource bundle containing all of this hints.
 * This is likely to be replaced in the future with a more extensible implementation so
 * third-party add-ons can provide their own hints (see ROO-610 for details). 
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Service
@Component
public class HintOperationsImpl implements HintOperations {
	private static final String ANT_MATCH_DIRECTORY_PATTERN = File.separator + "**" + File.separator;
	private static ResourceBundle bundle = ResourceBundle.getBundle(HintCommands.class.getName());
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public String hint(String topic) {
		if (topic == null || "".equals(topic)) {
			topic = determineTopic();
		}
		try {
			String message = bundle.getString(topic);
			return message.replace("\r", System.getProperty("line.separator")).replace("${completion_key}", AbstractShell.completionKeys);
		} catch (MissingResourceException exception) {
			return "Cannot find topic '" + topic + "'";
		}
	}

	public SortedSet<String> getCurrentTopics() {
		SortedSet<String> result = new TreeSet<String>();
		String topic = determineTopic();
		if ("general".equals(topic)) {
			for (Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
				result.add(keys.nextElement());
			}
			// result.addAll(bundle.keySet()); ResourceBundle.keySet() method in JDK 6+
		} else {
			result.add(topic);
		}
		return result;
	}

	private String determineTopic() {
		if (!projectOperations.isProjectAvailable()) {
			return "start";
		}

		PathResolver pathResolver = projectOperations.getPathResolver();
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"))) {
			return "jpa";
		}

		int entityCount = fileManager.findMatchingAntPath(pathResolver.getRoot(Path.SRC_MAIN_JAVA) + ANT_MATCH_DIRECTORY_PATTERN + "*_Roo_Entity.aj").size();
		if (entityCount == 0) {
			return "entities";
		}

		int javaBeanCount = fileManager.findMatchingAntPath(pathResolver.getRoot(Path.SRC_MAIN_JAVA) + ANT_MATCH_DIRECTORY_PATTERN + "*_Roo_JavaBean.aj").size();
		if (javaBeanCount == 0) {
			return "fields";
		}

		return "general";
	}
}

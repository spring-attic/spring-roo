package org.springframework.roo.classpath.operations;

import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands for hinting services.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class HintCommands implements CommandMarker {
	
	private static final String ANT_MATCH_DIRECTORY_PATTERN = File.separator + "**" + File.separator; 

	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;
	private static ResourceBundle bundle = ResourceBundle.getBundle(HintCommands.class.getName());
	
	@CliCommand(value="hint", help="Provides step-by-step hints and context-sensitive guidance")
	public String hint(@CliOption(key={"", "topic"}, mandatory=false, optionContext="topics", help="The topic for which advice should be provided") String topic) {
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
	
	private PathResolver getPathResolver() {
		ProjectMetadata metadata = (ProjectMetadata) this.metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (metadata == null) {
			return null;
		}
		return metadata.getPathResolver();
	}
	
	private String determineTopic() {
		PathResolver pathResolver = getPathResolver();
		
		if (pathResolver == null) {
			return "start";
		}
		
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

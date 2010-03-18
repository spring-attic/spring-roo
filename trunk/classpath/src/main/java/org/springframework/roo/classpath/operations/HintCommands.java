package org.springframework.roo.classpath.operations;

import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.internal.AbstractShell;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Shell commands for hinting services.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class HintCommands implements CommandMarker {
	
	private static final String ANT_MATCH_DIRECTORY_PATTERN = File.separator + "**" + File.separator; 

	private MetadataService metadataService;
	private FileManager fileManager;
	private static ResourceBundle bundle = ResourceBundle.getBundle(HintCommands.class.getName());
	
	public HintCommands(MetadataService metadataService, FileManager fileManager) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "File manager required");
		this.metadataService = metadataService;
		this.fileManager = fileManager;
		Assert.notNull(bundle, "Could not open hint resource bundle");
	}
	
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

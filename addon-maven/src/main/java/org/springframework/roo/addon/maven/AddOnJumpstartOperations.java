package org.springframework.roo.addon.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Provides a series of template files for add-on development.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class AddOnJumpstartOperations {
	private MetadataService metadataService;
	private FileManager fileManager;
	
	public AddOnJumpstartOperations(MetadataService metadataService, FileManager fileManager) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "File manager required");
		this.metadataService = metadataService;
		this.fileManager = fileManager;
	}

	public void install(Template template) {
		Assert.notNull(template, "Template required");
		Assert.isTrue(template.isAddOn(), "Add-on jumpstarts are only applicable for add-on templates");
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
		installIfNeeded("Commands.java", projectMetadata);
		installIfNeeded("Operations.java", projectMetadata);
		installIfNeeded("PropertyName.java", projectMetadata);
		installIfNeeded("assembly.xml", projectMetadata);
		writeTextFile("readme.txt", "welcome to my addon!", projectMetadata);
		writeTextFile("legal/LICENSE.TXT", "Your license goes here", projectMetadata);
	}
	
	private void installIfNeeded(String targetFilename, ProjectMetadata projectMetadata) {
		String packagePath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName().replace('.', '/');
		String destinationFile = projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, packagePath + "/" + targetFilename);
		
		// Different destination for assembly.xml
		if ("assembly.xml".equals(targetFilename)) {
			destinationFile = projectMetadata.getPathResolver().getIdentifier(Path.ROOT, "src/main/assembly/" + targetFilename);
		}
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), targetFilename + "-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());
				
				// Output the file for the user
				MutableFile mutableFile = fileManager.createFile(destinationFile);
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
			}
		}
		
	}
	
	private void writeTextFile(String fullPathFromRoot, String message, ProjectMetadata projectMetadata) {
		Assert.hasText(fullPathFromRoot, "Text file name to write is required");
		Assert.hasText(message, "Message required");
		Assert.notNull(projectMetadata, "Project metadata required");
		String path = projectMetadata.getPathResolver().getIdentifier(Path.ROOT, fullPathFromRoot);
		File file = new File(path);
		MutableFile mutableFile;
		if (file.exists()) {
			mutableFile = fileManager.updateFile(path);
		} else {
			mutableFile = fileManager.createFile(path);
		}
		byte[] input = message.getBytes();
		try {
			FileCopyUtils.copy(input, mutableFile.getOutputStream());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	
}

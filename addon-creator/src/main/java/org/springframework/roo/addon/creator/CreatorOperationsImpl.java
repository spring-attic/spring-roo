package org.springframework.roo.addon.creator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides addon generation operations.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class CreatorOperationsImpl implements CreatorOperations {
	
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private PathResolver pathResolver;
		
	public boolean isCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) == null;
	}
	
	private enum Type {SIMPLE, ADVANCED, I18N};
	
	public void createAdvancedAddon(JavaPackage topLevelPackage, String description) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		
		createProject(topLevelPackage, Type.ADVANCED, description);
		
		installIfNeeded("Commands.java", topLevelPackage, Type.ADVANCED);
		installIfNeeded("Operations.java", topLevelPackage, Type.ADVANCED);
		installIfNeeded("OperationsImpl.java", topLevelPackage, Type.ADVANCED);
		installIfNeeded("Metadata.java", topLevelPackage, Type.ADVANCED);
		installIfNeeded("MetadataProvider.java", topLevelPackage, Type.ADVANCED);
		installIfNeeded("RooAnnotation.java", topLevelPackage, Type.ADVANCED);
		installIfNeeded("assembly.xml", topLevelPackage, Type.ADVANCED);
		installIfNeeded("configuration.xml", topLevelPackage, Type.ADVANCED);
	}
	
	public void createSimpleAddon(JavaPackage topLevelPackage, String description) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		
		createProject(topLevelPackage, Type.SIMPLE, description);
		
		installIfNeeded("Commands.java", topLevelPackage, Type.SIMPLE);
		installIfNeeded("Operations.java", topLevelPackage, Type.SIMPLE);
		installIfNeeded("OperationsImpl.java", topLevelPackage, Type.SIMPLE);
		installIfNeeded("PropertyName.java", topLevelPackage, Type.SIMPLE);
		installIfNeeded("assembly.xml", topLevelPackage, Type.SIMPLE);
	}
	
	public void createI18nAddon(JavaPackage topLevelPackage, String language, Locale locale, File messageBundle, File flagGraphic, String description) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		Assert.hasLength(language, "Language specification required");
		Assert.notNull(locale, "Locale required");
		Assert.notNull(messageBundle, "Message Bundle required");
		Assert.notNull(flagGraphic, "Flag graphic required");
		
		String languageName = StringUtils.capitalize(language.replaceAll("\\s+", ""));
		String packagePath = topLevelPackage.getFullyQualifiedPackageName().replace('.', '/');
		
		if (description == null || description.length() == 0) {
			description = languageName + " language support for Spring Roo Web MVC JSP Scaffolding";
		}
		
		createProject(topLevelPackage, Type.I18N, description);

		installIfNeeded("assembly.xml", topLevelPackage, Type.I18N);
		
		try {
			FileCopyUtils.copy(new FileInputStream(messageBundle), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/" + messageBundle.getName())).getOutputStream());
			FileCopyUtils.copy(new FileInputStream(flagGraphic), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/" + flagGraphic.getName())).getOutputStream());
		} catch (IOException e) {
			throw new IllegalStateException("Could not copy addon resources into project");
		}
		
		String destinationFile = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, packagePath + "/" + languageName + "Language.java");
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), Type.I18N.name().toLowerCase() + "/Language.java-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
				input = input.replace("__APP_NAME__", languageName);
				input = input.replace("__LOCALE__", locale.toString());
				input = input.replace("__LANGUAGE__", StringUtils.capitalize(language));
				input = input.replace("__FLAG_FILE__", flagGraphic.getName());
				input = input.replace("__MESSAGE_BUNDLE__", messageBundle.getName());
				
				// Output the file for the user
				MutableFile mutableFile = fileManager.createFile(destinationFile);
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to create '" + languageName + "Language.java'", ioe);
			}
		}		
	}
	
	private void createProject(JavaPackage topLevelPackage, Type type, String description) {
		String projectName = topLevelPackage.getFullyQualifiedPackageName();
		
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/roo-addon-" + type.name().toLowerCase() + "-template.xml"));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = pom.getDocumentElement();
		XmlUtils.findRequiredElement("/project/artifactId", rootElement).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/groupId", rootElement).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/name", rootElement).setTextContent(projectName);
		if (description != null && description.length() != 0) {
			XmlUtils.findRequiredElement("/project/description", rootElement).setTextContent(description);
		}

		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");

		writeTextFile("readme.txt", "welcome to my addon!", projectMetadata);
		writeTextFile("legal/LICENSE.TXT", "Your license goes here", projectMetadata);

		fileManager.scan();
	}
	
	private void installIfNeeded(String targetFilename, JavaPackage topLevelPackage, Type type) {
		String tlp = topLevelPackage.getFullyQualifiedPackageName();
		String packagePath = tlp.replace('.', '/');
		String fileName = StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)) + targetFilename;
		String destinationFile = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, packagePath + "/" + fileName);
		
		// Different destination for assembly.xml
		if ("assembly.xml".equals(targetFilename)) {
			destinationFile = pathResolver.getIdentifier(Path.ROOT, "src/main/assembly/" + targetFilename);
		}
		
		// Different destination for configuration.xml
		else if ("configuration.xml".equals(targetFilename)) {
			destinationFile = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/" + targetFilename);
		}
		
		// Adjust name for Roo Annotation
		else if (targetFilename.startsWith("RooAnnotation")) {
			destinationFile = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, packagePath + "/Roo" + StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)) + ".java");
		}
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/" + targetFilename + "-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
				input = input.replace("__APP_NAME__", StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)));
				
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

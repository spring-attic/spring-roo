package org.springframework.roo.addon.creator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectMetadataProvider;
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
	@Reference private ProjectMetadataProvider projectMetadataProvider;
		
	public boolean isCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) == null;
	}
	
	private enum Type {SIMPLE, ADVANCED, I18N};
	
	public void createI18nAddon(JavaPackage topLevelPackage, String language, String locale, File messageBundle, File flagGraphic) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		Assert.hasLength(language, "Language specification required");
		Assert.hasLength(locale, "Locale required");
		Assert.notNull(messageBundle, "Message Bundle required");
		Assert.notNull(flagGraphic, "Flag graphic required");
		
		createProject(topLevelPackage, Type.I18N);
	}
	
	public void createAdvancedAddon(JavaPackage topLevelPackage) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		
		createProject(topLevelPackage, Type.ADVANCED);
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
	
		installIfNeeded("Commands.java", projectMetadata, Type.ADVANCED);
		installIfNeeded("Operations.java", projectMetadata, Type.ADVANCED);
		installIfNeeded("OperationsImpl.java", projectMetadata, Type.ADVANCED);
		installIfNeeded("Metadata.java", projectMetadata, Type.ADVANCED);
		installIfNeeded("MetadataProvider.java", projectMetadata, Type.ADVANCED);
		installIfNeeded("RooAnnotation.java", projectMetadata, Type.ADVANCED);
		installIfNeeded("assembly.xml", projectMetadata, Type.ADVANCED);
		installIfNeeded("configuration.xml", projectMetadata, Type.ADVANCED);
	}
	
	public void createSimpleAddon(JavaPackage topLevelPackage) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		
		createProject(topLevelPackage, Type.SIMPLE);
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
	
		installIfNeeded("Commands.java", projectMetadata, Type.SIMPLE);
		installIfNeeded("Operations.java", projectMetadata, Type.SIMPLE);
		installIfNeeded("OperationsImpl.java", projectMetadata, Type.SIMPLE);
		installIfNeeded("PropertyName.java", projectMetadata, Type.SIMPLE);
		installIfNeeded("assembly.xml", projectMetadata, Type.SIMPLE);
	}
	
	private void createProject(JavaPackage topLevelPackage, Type type) {
		String packageName = topLevelPackage.getFullyQualifiedPackageName();
		String projectName = null;
		int lastIndex = packageName.lastIndexOf(".");
		if (lastIndex == -1) {
			projectName = packageName;
		} else {
			projectName = packageName.substring(lastIndex + 1);
		}
		
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/roo-addon-" + type.name().toLowerCase() + "-template.xml"));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = pom.getDocumentElement();
		XmlUtils.findRequiredElement("/project/artifactId", rootElement).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/groupId", rootElement).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/name", rootElement).setTextContent(projectName);

		List<Element> versionElements = XmlUtils.findElements("//*[.='JAVA_VERSION']", rootElement);
		for (Element e : versionElements) {
			e.setTextContent("1.5");
		}

		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
		
		projectMetadataProvider.addDependency(new Dependency("javax.annotation", "jsr250-api", "1.0"));

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");

		writeTextFile("readme.txt", "welcome to my addon!", projectMetadata);
		writeTextFile("legal/LICENSE.TXT", "Your license goes here", projectMetadata);

		fileManager.scan();
	}
	
	private void installIfNeeded(String targetFilename, ProjectMetadata projectMetadata, Type type) {
		String tlp = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName();
		String packagePath = tlp.replace('.', '/');
		String fileName = StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)) + targetFilename;
		String destinationFile = projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, packagePath + "/" + fileName);
		
		// Different destination for assembly.xml
		if ("assembly.xml".equals(targetFilename)) {
			destinationFile = projectMetadata.getPathResolver().getIdentifier(Path.ROOT, "src/main/assembly/" + targetFilename);
		}
		
		// Different destination for configuration.xml
		else if ("configuration.xml".equals(targetFilename)) {
			destinationFile = projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/" + targetFilename);
		}
		
		// Adjust name for Roo Annotation
		else if (targetFilename.startsWith("RooAnnotation")) {
			destinationFile = projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, packagePath + "/Roo" + StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)) + ".java");
		}
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/" + targetFilename + "-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());
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

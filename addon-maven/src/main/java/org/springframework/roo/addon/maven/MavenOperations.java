package org.springframework.roo.addon.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectMetadataProvider;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Maven project operations. 
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class MavenOperations extends ProjectOperations {
	
	private static final Logger logger = HandlerUtils.getLogger(MavenOperations.class);
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ApplicationContextOperations applicationContextOperations;
	private AddOnJumpstartOperations addOnJumpstartOperations;

	public MavenOperations(MetadataService metadataService, ProjectMetadataProvider projectMetadataProvider, FileManager fileManager, PathResolver pathResolver, ApplicationContextOperations applicationContextOperations, AddOnJumpstartOperations addOnJumpstartOperations) {
		super(metadataService, projectMetadataProvider);
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(applicationContextOperations, "Application context operations required");
		Assert.notNull(addOnJumpstartOperations, "Add-on jumpstart operations required");
		this.metadataService = metadataService;
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.applicationContextOperations = applicationContextOperations;
		this.addOnJumpstartOperations = addOnJumpstartOperations;
	}
	
	public boolean isCreateProjectAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) == null;
	}
	
	public String getProjectRoot() {
		return pathResolver.getRoot(Path.ROOT);
	}
	
	public void createProject(Template template, JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion) {
		Assert.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
		Assert.notNull(template, "Template required");
		Assert.notNull(topLevelPackage, "Top level package required");
		
		// Note the Template.getKey() provides the Maven POM template filename to read
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), template.getKey());

		if (majorJavaVersion == null || (majorJavaVersion < 5 || majorJavaVersion > 7)) {
			// We need to detect the major Java version to use
			String ver = System.getProperty("java.version");
			if (ver.indexOf("1.7.") > -1) {
				majorJavaVersion = 7;
			}
			else if (ver.indexOf("1.6.") > -1) {
				majorJavaVersion = 6;
			}
			else {
				// To be running Roo they must be on Java 5 or above
				majorJavaVersion = 5;
			}
			// Always discard the above and use Java 5 if this is an add-on
			if (template.isAddOn()) {
				majorJavaVersion = 5;
			}
		}

		if (projectName == null) {
			String packageName = topLevelPackage.getFullyQualifiedPackageName();
			int lastIndex = packageName.lastIndexOf(".");
			if (lastIndex == -1) {
				projectName = packageName;
			} else {
				projectName = packageName.substring(lastIndex+1);
			}
			// Always discard the above and use the package name as the name if this is an add-on
			if (template.isAddOn()) {
				projectName = topLevelPackage.getFullyQualifiedPackageName();
			}
		}

		// Apply special add-on convention rules
		if (Template.ROO_ADDON_SIMPLE.equals(template)) {
			Assert.isTrue(majorJavaVersion == 5, "Roo add-ons must be Java 5 only");
			Assert.isTrue(topLevelPackage.getFullyQualifiedPackageName().startsWith("com.") || topLevelPackage.getFullyQualifiedPackageName().startsWith("org.") || topLevelPackage.getFullyQualifiedPackageName().startsWith("net."), "Roo add-ons must have a top-level package starting with .com or .net or .org; eg com.mycompany.myproject.roo.addon");
			Assert.isTrue(topLevelPackage.getFullyQualifiedPackageName().endsWith(".roo.addon"), "Roo add-ons must have a package name ending in .roo.addon; eg com.mycompany.myproject.roo.addon");
			Assert.isTrue(topLevelPackage.getFullyQualifiedPackageName().equals(projectName), "Roo add-ons must have the same project name as the top-level-package name");
		}

		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = pom.getDocumentElement();
		XmlUtils.findRequiredElement("/project/artifactId", rootElement).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/groupId", rootElement).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/name", rootElement).setTextContent(projectName);
		
		List<Element> versionElements = XmlUtils.findElements("//*[.='JAVA_VERSION']", rootElement) ;
		for (Element e : versionElements) {
			e.setTextContent("1." + majorJavaVersion);
		}
		
		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
		
		// Java 5 needs the javax.annotation library (it's included in Java 6 and above)
		if (majorJavaVersion == 5 && !template.isAddOn()) {
			dependencyUpdate(new Dependency("javax.annotation", "com.springsource.javax.annotation", "1.0.0"));
		}

		if (template.isAddOn()) {
			addOnJumpstartOperations.install(template);
		}
		
		fileManager.scan();
		
		applicationContextOperations.createMiddleTierApplicationContext();
	
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "log4j.properties-template"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties")).getOutputStream());
		} catch (IOException e1) {
			logger.warning("Unable to install log4j logging configuration");
		}
	}
}

package org.springframework.roo.addon.maven;

import java.io.InputStream;
import java.util.List;

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
import org.springframework.roo.support.util.Assert;
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
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	
	public MavenOperations(MetadataService metadataService, ProjectMetadataProvider projectMetadataProvider, FileManager fileManager, PathResolver pathResolver) {
		super(metadataService, projectMetadataProvider);
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		this.metadataService = metadataService;
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
	}
	
	public boolean isCreateProjectAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) == null;
	}
	
	public String getProjectRoot() {
		return pathResolver.getRoot(Path.ROOT);
	}
	
	public void createProject(InputStream templateInputStream, JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion) {
		Assert.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
		Assert.notNull(templateInputStream, "Could not acquire template POM");
		Assert.notNull(topLevelPackage, "Top level package required");
		Assert.hasText(projectName, "Project name required");
		
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
		
		fileManager.scan();
		
		// Finally, Java 5 needs the javax.annotation library (it's included in Java 6 and above)
		if (majorJavaVersion == 5) {
			dependencyUpdate(new Dependency("javax.annotation", "com.springsource.javax.annotation", "1.0.0"));
		}
	}
	
}

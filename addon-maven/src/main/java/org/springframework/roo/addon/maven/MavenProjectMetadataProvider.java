package org.springframework.roo.addon.maven;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectMetadataProvider;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides {@link ProjectMetadata}.
 *
 * <p>
 * For simplicity of operation, this is the only implementation shipping with ROO that supports
 * {@link ProjectMetadata}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopment
public class MavenProjectMetadataProvider implements ProjectMetadataProvider, FileEventListener {

	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(ProjectMetadata.getProjectIdentifier()));
	
	private PathResolver pathResolver;
	private FileManager fileManager;
	private MetadataService metadataService;
	private MetadataDependencyRegistry metadataDependencyRegistry;

	private String pom;
	
	public MavenProjectMetadataProvider(PathResolver pathResolver, FileManager fileManager, FileMonitorService fileMonitorService, MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(fileMonitorService, "File monitor service required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(metadataDependencyRegistry, "Metadata dependency registry required");
		this.pathResolver = pathResolver;
		this.fileManager = fileManager;
		this.pom = pathResolver.getIdentifier(Path.ROOT, "/pom.xml");
		this.metadataService = metadataService;
		this.metadataDependencyRegistry = metadataDependencyRegistry;
		fileMonitorService.addFileEventListener(this);
		metadataService.register(this);
	}

	public MetadataItem get(String metadataIdentificationString) {
		Assert.isTrue(ProjectMetadata.getProjectIdentifier().equals(metadataIdentificationString), "Unexpected metadata request '" + metadataIdentificationString + "' for this provider");
		
		// Just rebuild on demand. We always do this as we expect MetadataService to cache on our behalf

		// Read the file, if it is available
		if (!fileManager.exists(pom)) {
			return null;
		}
		InputStream inputStream = fileManager.getInputStream(pom);
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(inputStream);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element rootElement = (Element) document.getFirstChild();
		
		// Obtain project name
		Element artifactIdElement = XmlUtils.findFirstElement("/project/artifactId", rootElement);
		String artifactId = artifactIdElement.getTextContent();
		Assert.hasText(artifactId, "Project name could not be determined from POM '" + pom + "'");
		String projectName = artifactId;
		
		// Obtain top level package
		Element groupIdElement = XmlUtils.findFirstElement("/project/groupId", rootElement);
		String topLevelPackageString = groupIdElement.getTextContent();
		Assert.hasText(topLevelPackageString, "Top level package name could not be determined from POM '" + pom + "'");
		Assert.isTrue(!topLevelPackageString.endsWith("."), "Top level package name cannot end with a period (was '" + topLevelPackageString + "')");
		JavaPackage topLevelPackage = new JavaPackage(topLevelPackageString);
		
		// Build dependencies list
		Set<Dependency> dependencies = new HashSet<Dependency>();
		
		for(Element dependency : XmlUtils.findElements("/project/dependencies/dependency", rootElement)) {
			Dependency d = new Dependency(dependency);
			dependencies.add(d);
		}

		return new ProjectMetadata(topLevelPackage, projectName, dependencies, pathResolver);
	}

	public String getProvidesType() {
		return PROVIDES_TYPE;
	}

	public void addDependency(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (md.isDependencyRegistered(dependency)) {
			return;
		}
		
		MutableFile mutableFile = fileManager.updateFile(pom);
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element rootElement = (Element) document.getFirstChild();
		Element dependencies = XmlUtils.findFirstElement("/project/dependencies", rootElement);
		Assert.notNull(dependencies, "Dependencies unable to be found");
		
		Element depElement = document.createElement("dependency");
		Element groupId = document.createElement("groupId");
		Element artifactId = document.createElement("artifactId");
		Element version = document.createElement("version");
		Element type = document.createElement("type");
		
		groupId.setTextContent(dependency.getGroupId().getFullyQualifiedPackageName());
		artifactId.setTextContent(dependency.getArtifactId().getSymbolName());
		version.setTextContent(dependency.getVersionId());
		type.setTextContent(dependency.getType().toString());
		
		depElement.appendChild(groupId);
		depElement.appendChild(artifactId);
		depElement.appendChild(version);
		
		if (!DependencyType.JAR.equals(dependency.getType())) {
			// Keep the XML short, we don't need "JAR" given it's the default
			depElement.appendChild(type);
		}
		
		//add exclusions if they are defined
		List<Dependency> exclusions = dependency.getExclusions();
 		if (exclusions.size() > 0) { 			
 			Element exclusionsElement = document.createElement("exclusions");
 			for (Dependency exclusion: exclusions) {
 				Element exclusionElement = document.createElement("exclusion");
 				Element exclusionId = document.createElement("groupId");
 				exclusionId.setTextContent(exclusion.getGroupId().getFullyQualifiedPackageName());
 				Element exclusionArtifactId = document.createElement("artifactId");
 				exclusionArtifactId.setTextContent(exclusion.getArtifactId().getSymbolName());
 				exclusionElement.appendChild(exclusionId);
 				exclusionElement.appendChild(exclusionArtifactId);
 				exclusionsElement.appendChild(exclusionElement); 				
 			}
 			depElement.appendChild(exclusionsElement);
 		}
		
		dependencies.appendChild(depElement);

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);	
	}

	public void removeDependency(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to remove required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (!md.isDependencyRegistered(dependency)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element rootElement = (Element) document.getFirstChild();
		Element dependencies = XmlUtils.findFirstElement("/project/dependencies", rootElement);
		
		for(Element candidate : XmlUtils.findElements("/project/dependencies/dependency", rootElement)) {
			if (dependency.equals(new Dependency(candidate))) {
				// Found it
				dependencies.removeChild(candidate);
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);	
	}

	public void onFileEvent(FileEvent fileEvent) {
		Assert.notNull(fileEvent, "File event required");

		if (fileEvent.getFileDetails().getCanonicalPath().equals(pom)) {
			// Something happened to the POM
			
			// Don't notify if we're shutting down
			if (fileEvent.getOperation() == FileOperation.MONITORING_FINISH) {
				return;
			}
			
			// Otherwise let everyone know something has happened of interest, plus evict any cached entries from the MetadataService
			metadataService.evict(ProjectMetadata.getProjectIdentifier());
			metadataDependencyRegistry.notifyDownstream(ProjectMetadata.getProjectIdentifier());
		}
	}
	
}

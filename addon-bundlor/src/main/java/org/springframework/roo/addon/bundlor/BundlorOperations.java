package org.springframework.roo.addon.bundlor;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyListener;
import org.springframework.roo.project.Execution;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Provides Bundlor installation and manifest management services.
 * 
 * @author Adrian Colyer
 * @since 1.0
 *
 */
@ScopeDevelopment
public class BundlorOperations implements DependencyListener {

	private static final Dependency PLUGIN_DEPENDENCY = new Dependency("com.springsource.bundlor", "com.springsource.bundlor.maven", "1.0.0.M5");
	private static final String INFINITY="INF";
	
	Logger logger = Logger.getLogger(BundlorOperations.class.getName());
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ProjectOperations projectOperations;
	private ResourceBundle dependencyMap;

	public BundlorOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectOperations, "Project operations required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.projectOperations = projectOperations;
		this.dependencyMap = ResourceBundle.getBundle("org.springframework.roo.addon.bundlor.dependencyMap");
	}
	
	public boolean isInstallBundlorAvailable() {
		ProjectMetadata project = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (project == null) {
			return false;
		}
		// only permit installation if they don't already have some version of Bundlor installed
		return project.getBuildPluginDependenciesExcludingVersion(PLUGIN_DEPENDENCY).isEmpty();
	}

	public void installBundlor(String bundleName) {
		projectOperations.buildPluginUpdate(PLUGIN_DEPENDENCY,new Execution("bundlor","transform"));
		
		// copy the template across
		ProjectMetadata project = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		String destination = pathResolver.getIdentifier(Path.ROOT, "template.mf");
		if (!fileManager.exists(destination)) {
			writeTemplateFile(destination,bundleName,project.getDependencies());
		}
		
		// monitor any dependencies added or removed by other addons
		projectOperations.addDependencyListener(this);

	}
	
	public boolean isTemplateManagementAvailable() {
		String template = pathResolver.getIdentifier(Path.ROOT, "template.mf");
		return (new File(template).exists());
	}

	private void writeTemplateFile(String destination,String bundleName,Set<Dependency> dependencies) {
		try {
			ProjectMetadata project = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			String mfFile = FileCopyUtils.copyToString(new InputStreamReader(TemplateUtils.getTemplate(getClass(), "template.mf")));
			mfFile = mfFile.replaceAll("#topLevelPackage#", project.getTopLevelPackage().toString());
			String bundleNameReplacement = project.getProjectName();
			if (null != bundleName) {
				bundleNameReplacement = bundleName;
			}
			mfFile = mfFile.replaceAll("#bundleName#",bundleNameReplacement);
			for (Dependency d : dependencies) {
				String bundleImport = bundleImportFor(d);
				if (null != bundleImport) {
					mfFile += (bundleImport + "\n");
				}
			}
			FileCopyUtils.copy(mfFile,new OutputStreamWriter(fileManager.createFile(destination).getOutputStream()));
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
	
	public void excludeFromExport(String packagePattern) {
		getManifestTemplate()
			.addExcludeExport(packagePattern)
			.writeOut();
	}
	
	public void versionExports(String packagePattern, String version) {
		getManifestTemplate()
			.addExportTemplate(packagePattern,version)
			.writeOut();
	}

	public void configureImports(String packagePattern, String fromVersion, String toVersion, boolean inclusiveUpper, boolean optional) {
		String versionRange = makeVersionRange(fromVersion,toVersion,inclusiveUpper);
		getManifestTemplate()
			.addImportTemplate(packagePattern,versionRange,optional)
			.writeOut();
	}
	
	public void addExplicitImport(String packageName, String fromVersion, String toVersion, boolean inclusiveUpper, boolean optional) {
		String versionRange = makeVersionRange(fromVersion,toVersion,inclusiveUpper);
		getManifestTemplate()
			.addImportPackage(packageName,versionRange,optional)
			.writeOut();
	}
	
	public void dependencyAdded(Dependency dependency) {
		String bundleImport = bundleImportFor(dependency);
		if (null != bundleImport) {
			getManifestTemplate()
				.addImportBundle(bundleImport)
				.writeOut();
		}
	}
	
	public void dependencyRemoved(Dependency dependency) {
		String bundleImport = bundleImportFor(dependency);
		if (null != bundleImport) {
			getManifestTemplate()
				.removeImportBundle(bundleImport)
				.writeOut();
		}		
	}
	
	private String bundleImportFor(Dependency dependency) {
		String key = dependency.getGroupId() + ":" + 
				     dependency.getArtifactId() + ":" + 
				     dependency.getVersionId();
		try {
			return dependencyMap.getString(key);
		}
		catch (MissingResourceException ex) {
			return null;
		}
	}

	private ManifestTemplate getManifestTemplate() {
		return new ManifestTemplate(fileManager,pathResolver.getIdentifier(Path.ROOT, "template.mf"));
	}

	private String makeVersionRange(String fromVersion, String toVersion, boolean inclusiveUpper) {
		if (fromVersion.equals(toVersion)) {
			// always use inclusive upper
			return "[" + fromVersion + "," + toVersion + "]";
		}
		else {
			if (INFINITY.equals(toVersion)) {
				return fromVersion;
			}
			else {
				return "[" + fromVersion + "," + toVersion  +
						(inclusiveUpper? "]" : ")");
			}
		}
	}

}

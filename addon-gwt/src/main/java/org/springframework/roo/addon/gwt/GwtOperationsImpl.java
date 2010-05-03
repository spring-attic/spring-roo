package org.springframework.roo.addon.gwt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides GWT installation services.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class GwtOperationsImpl implements GwtOperations {
	
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	private ComponentContext context;
	
	protected void activate(ComponentContext context) {
		this.context = context;
	}
	
	public boolean isSetupGwtAvailable() {
		ProjectMetadata project = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (project == null) {
			return false;
		}
		// Do not permit installation unless they have a web project
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml"))) {
			return false;
		}
		// Do not permit installation if they have a gwt package already in their project
		String root = GwtPath.GWT_ROOT.canonicalFileSystemPath(project);
		return !fileManager.exists(root);
	}

	public void setupGwt() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project could not be retrieved");
		
		Element configuration = getConfiguration();

		// Add dependencies
		List<Element> dependencies = XmlUtils.findElements("/configuration/dependencies/dependency", configuration);
		for (Element dependency : dependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
		
		// Add POM plugin
		List<Element> plugins = XmlUtils.findElements("/configuration/plugins/plugin", configuration);
		for (Element plugin : plugins) {
			projectOperations.addBuildPlugin(new Plugin(plugin));
		}
		
		// Add GWT natures and builder names to maven eclipse plugin
		updateMavenEclipsePlugin();
		
		// Update web.xml
		updateWebXml(projectMetadata);
		
		// Copy "static" directories
		for (GwtPath path : GwtPath.values()) {
			copyDirectoryContents(path, projectMetadata);
		}
		
		// TODO: This is crazy!
		// Kill all controllers - temporary until we get the classpath issues with Hibernate Validator sorted out
		{
			String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*Controller.java";
			for (FileDetails fd: fileManager.findMatchingAntPath(antPath)) {
				fileManager.delete(fd.getCanonicalPath());
			}
		}
		
		// Do a "get" for every .java file, thus ensuring the metadata is fired
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		for (FileDetails fd: fileManager.findMatchingAntPath(antPath)) {
			String fullPath = srcRoot.getRelativeSegment(fd.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);
			String id = GwtMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
			metadataService.get(id);
		}
	}

	private void copyDirectoryContents(GwtPath gwtPath, ProjectMetadata projectMetadata) {
		String sourceAntPath = gwtPath.sourceAntPath();
		String targetDirectory = gwtPath.canonicalFileSystemPath(projectMetadata);
		
		if (!targetDirectory.endsWith("/")) {
			targetDirectory += "/";
		}
		
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}
		
		String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
		Set<URL> urls = TemplateUtils.findMatchingClasspathResources(context.getBundleContext(), path);
		Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");
		for (URL url: urls) {
			String fileName =  url.getPath().substring(url.getPath().lastIndexOf("/") + 1).replace("-template", "");
			String targetFilename = targetDirectory + fileName;
			if (!fileManager.exists(targetFilename)) {
				try {
					// Read template and insert the user's package
					String input = FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));
					input = input.replace("__TOP_LEVEL_PACKAGE__", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());
					
					// Output the file for the user
					MutableFile mutableFile = fileManager.createFile(targetFilename);
					FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
				} catch (IOException ioe) {
					throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
				}
			}
		}
	}
	
	private Element getConfiguration() {
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "configuration.xml");
		Assert.notNull(templateInputStream, "Could not acquire configuration.xml file");
		Document dependencyDoc;
		try {
			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return (Element) dependencyDoc.getFirstChild();
	}
	
	private void updateMavenEclipsePlugin() {
		String pom = pathResolver.getIdentifier(Path.ROOT, "pom.xml");
		Assert.isTrue(fileManager.exists(pom), "pom.xml not found; cannot continue");
		
		Document pomDoc;
		InputStream is = null;
		MutableFile mutablePom = null;
		try {
			mutablePom = fileManager.updateFile(pom);
			is = mutablePom.getInputStream();
			pomDoc = XmlUtils.getDocumentBuilder().parse(is);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		
		Element pomRoot = (Element) pomDoc.getFirstChild();
		List<Element> pluginElements = XmlUtils.findElements("/project/build/plugins/plugin", pomRoot);
		for (Element pluginElement : pluginElements) {
			Plugin plugin = new Plugin(pluginElement);
			if ("maven-eclipse-plugin".equals(plugin.getArtifactId().getSymbolName()) && "org.apache.maven.plugins".equals(plugin.getGroupId().getFullyQualifiedPackageName())) {
				// add in the builder configuration
				Element newEntry = new XmlElementBuilder("buildCommand", pomDoc).addChild(new XmlElementBuilder("name", pomDoc).setText("com.google.gwt.eclipse.core.gwtProjectValidator").build()).build();
				Element ctx = XmlUtils.findRequiredElement("configuration/additionalBuildcommands/buildCommand[last()]", pluginElement);
				ctx.getParentNode().appendChild(newEntry);

				// add in the additional nature
				newEntry = new XmlElementBuilder("projectnature", pomDoc).setText("com.google.gwt.eclipse.core.gwtNature").build();
				ctx = XmlUtils.findRequiredElement("configuration/additionalProjectnatures/projectnature[last()]", pluginElement);
				ctx.getParentNode().appendChild(newEntry);
				
				plugin = new Plugin(pluginElement);
				projectOperations.removeBuildPlugin(plugin);
				projectOperations.addBuildPlugin(plugin);
			}
 		}
		
		// Fix output directory
		Element outputDirectory = XmlUtils.findFirstElement("/project/build/outputDirectory", pomRoot);
		if (outputDirectory != null) {
			outputDirectory.setTextContent("${project.build.directory}/${project.build.finalName}/WEB-INF/classes");
		}
		else {
			Element newEntry = new XmlElementBuilder("outputDirectory", pomDoc).setText("${project.build.directory}/${project.build.finalName}/WEB-INF/classes").build();
			Element ctx = XmlUtils.findRequiredElement("/project/build", pomRoot);
			ctx.appendChild(newEntry);
		}
		// TODO CD is there a better way of doing this here?
		XmlUtils.writeXml(mutablePom.getOutputStream(), pomDoc);
	}

	
	private void updateWebXml(ProjectMetadata projectMetadata) {
		String webXml = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		Assert.isTrue(fileManager.exists(webXml), "web.xml not found; cannot continue");
		
		MutableFile mutableWebXml = null;
		Document webXmlDoc;
		try {
			mutableWebXml = fileManager.updateFile(webXml);
			webXmlDoc = XmlUtils.getDocumentBuilder().parse(mutableWebXml.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		Element webXmlRoot = webXmlDoc.getDocumentElement();
		
		WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam("servlet.serverOperation", GwtPath.GWT_REQUEST.packageName(projectMetadata) + ".ApplicationRequestServerSideOperations"), webXmlDoc, null);
		WebXmlUtils.addListener(GwtPath.SERVER.packageName(projectMetadata) + ".ForceInitializationOfMavenClasspathContainerEntries_Roo_Listener", webXmlDoc, "Temporary workaround");
		WebXmlUtils.addServlet("requestFactory", "com.google.gwt.requestfactory.server.RequestFactoryServlet", "/expenses/data", null, webXmlDoc, null);
		
		// TODO: This is crazy!
		removeIfFound("/web-app/filter-mapping[filter-name='UrlRewriteFilter']", webXmlRoot);  // temporary (need to discuss with Stefan whether we need to rewrite everything)
		// TODO: This is crazy!
		removeIfFound("/web-app/servlet[servlet-class='org.springframework.web.servlet.DispatcherServlet']", webXmlRoot);  // temporary (due to JSR 303 being used and classloader issues in m2eclipse)
		// TODO: This is crazy!
		removeIfFound("/web-app/servlet-mapping[url-pattern='/app/*']", webXmlRoot);  // temporary (due to dispatcher servlet removal)

		XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
	}

	private void removeIfFound(String xpath, Element webXmlRoot) {
		Element toRemove = XmlUtils.findFirstElement(xpath, webXmlRoot);
		if (toRemove != null) {
			toRemove.getParentNode().removeChild(toRemove);
			toRemove = null;
		}
	}
}

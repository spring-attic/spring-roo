package org.springframework.roo.addon.security;

import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides security installation services.
 *
 * @author Ben Alex
 * @since 1.0
 */
@ScopeDevelopment
public class SecurityOperations {
	
	Logger logger = Logger.getLogger(SecurityOperations.class.getName());
		
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ProjectOperations projectOperations;
	
	private static final Dependency DEPENDENCY = new Dependency("org.springframework.security", "org.springframework.security", "2.0.4.A");
	
	public SecurityOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectOperations, "Project operations required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.projectOperations = projectOperations;
	}
	
	public boolean isInstallSecurityAvailable() {
		ProjectMetadata project = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (project == null) {
			return false;
		}
		// only permit installation if they don't already have some version of Spring Security installed
		return project.getDependenciesExcludingVersion(DEPENDENCY).size() == 0;
	}
	
	public void installSecurity() {
		// add to POM
		projectOperations.dependencyUpdate(DEPENDENCY);
		
		// copy the template across
		String destination = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-security.xml");
		if (!fileManager.exists(destination)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-security-template.xml"), fileManager.createFile(destination).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
		
		// copy the template across
		String loginPage = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "login.jsp");
		if (!fileManager.exists(loginPage)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "login-template.jsp"), fileManager.createFile(loginPage).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
		
		String webXml = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		
		MutableFile mutableWebXml = null;
		Document webXmlDoc;
		try {
			if (fileManager.exists(webXml)) {
				mutableWebXml = fileManager.updateFile(webXml);
				webXmlDoc = XmlUtils.getDocumentBuilder().parse(mutableWebXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + webXml);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		Element root = (Element) webXmlDoc.getFirstChild();

		Element paramValue = XmlUtils.findRequiredElement("/web-app/context-param[param-value='classpath:applicationContext.xml']/param-value", root);
		String currentContent = paramValue.getTextContent();
		paramValue.setTextContent(currentContent + System.getProperty("line.separator") + "\t\tclasspath:applicationContext-security.xml");
		
		Element filter = webXmlDoc.createElement("filter");
		Element filterName = webXmlDoc.createElement("filter-name");
		filterName.setTextContent("springSecurityFilterChain");
		Element filterClass = webXmlDoc.createElement("filter-class");
		filterClass.setTextContent("org.springframework.web.filter.DelegatingFilterProxy");
		filter.appendChild(filterName);
		filter.appendChild(filterClass);
		
		Element filterMapping = webXmlDoc.createElement("filter-mapping");
		Element urlPattern = webXmlDoc.createElement("url-pattern");
		Element filterName2 = webXmlDoc.createElement("filter-name");
		filterName2.setTextContent("springSecurityFilterChain");
		urlPattern.setTextContent("/*");
		filterMapping.appendChild(filterName2);
		filterMapping.appendChild(urlPattern);
		
		Element listener = XmlUtils.findRequiredElement("//listener", root);
		Assert.notNull(listener, "Could not find the first listener element in web.xml");
		listener.getParentNode().insertBefore(filter, listener);
		listener.getParentNode().insertBefore(filterMapping, listener);
		
		XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
	}
}

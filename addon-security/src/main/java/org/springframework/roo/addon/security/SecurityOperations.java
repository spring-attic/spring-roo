package org.springframework.roo.addon.security;

import java.io.IOException;

import org.springframework.roo.addon.mvc.jsp.TilesOperations;
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
import org.springframework.roo.support.util.XmlElementBuilder;
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
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ProjectOperations projectOperations;
	
	private static final Dependency DEPENDENCY_CORE = new Dependency("org.springframework.security", "org.springframework.security.core", "3.0.0.RELEASE");
	private static final Dependency DEPENDENCY_CONFIG = new Dependency("org.springframework.security", "org.springframework.security.config", "3.0.0.RELEASE");
	private static final Dependency DEPENDENCY_WEB = new Dependency("org.springframework.security", "org.springframework.security.web", "3.0.0.RELEASE");
	private static final Dependency DEPENDENCY_TAGLIBS = new Dependency("org.springframework.security", "org.springframework.security.taglibs", "3.0.0.RELEASE");
	
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
		// do not permit installation unless they have a web project (as per ROO-342)
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml"))) {
			return false;
		}
		// only permit installation if they don't already have some version of Spring Security installed
		return project.getDependenciesExcludingVersion(DEPENDENCY_CORE).size() == 0;
	}
	
	public void installSecurity() {
		// add to POM
		projectOperations.dependencyUpdate(DEPENDENCY_CORE);
		projectOperations.dependencyUpdate(DEPENDENCY_CONFIG);
		projectOperations.dependencyUpdate(DEPENDENCY_WEB);
		projectOperations.dependencyUpdate(DEPENDENCY_TAGLIBS);
		
		// copy the template across
		String destination = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-security.xml");
		if (!fileManager.exists(destination)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-security-template.xml"), fileManager.createFile(destination).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
		
		// copy the template across
		String loginPage = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/login.jspx");
		if (!fileManager.exists(loginPage)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "login.jspx"), fileManager.createFile(loginPage).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
		
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/views.xml"))) {
			TilesOperations tilesOperations = new TilesOperations("/", fileManager, pathResolver, "config/webmvc-config.xml");
			tilesOperations.addViewDefinition("login", TilesOperations.PUBLIC_TEMPLATE, "/WEB-INF/views/login.jspx");
			tilesOperations.writeToDiskIfNecessary();
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
		
		Element ctx = XmlUtils.findRequiredElement("/web-app/context-param[last()]", root);
		ctx.getParentNode().insertBefore(filter, ctx.getNextSibling());

		Element fm = XmlUtils.findRequiredElement("/web-app/filter-mapping[filter-name='Spring OpenEntityManagerInViewFilter']", root);
		fm.getParentNode().insertBefore(filterMapping, fm.getNextSibling());
		
		XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
		
		//include static view controller handler to webmvc-config.xml
		String webMvc = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		
		MutableFile mutableConfigXml = null;
		Document webConfigDoc;
		try {
			if (fileManager.exists(webMvc)) {
				mutableConfigXml = fileManager.updateFile(webMvc);
				webConfigDoc = XmlUtils.getDocumentBuilder().parse(mutableConfigXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + webMvc);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 
		
		Element viewController = XmlUtils.findFirstElementByName("mvc:view-controller", webConfigDoc.getDocumentElement());
		Assert.notNull(viewController, "Could not find mvc:view-controller in " + webMvc);
		
		viewController.getParentNode().insertBefore(new XmlElementBuilder("mvc:view-controller", webConfigDoc).addAttribute("path", "/login").build(), viewController);
		XmlUtils.writeXml(mutableConfigXml.getOutputStream(), webConfigDoc);
	}
}

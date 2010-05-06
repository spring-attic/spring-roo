package org.springframework.roo.addon.security;

import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.mvc.jsp.TilesOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
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
 * Provides security installation services.
 *
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class SecurityOperationsImpl implements SecurityOperations {
	
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private TilesOperations tilesOperations;
	
	private static final Dependency DEPENDENCY_CORE = new Dependency("org.springframework.security", "spring-security-core", "3.0.2.RELEASE");
	private static final Dependency DEPENDENCY_CONFIG = new Dependency("org.springframework.security", "spring-security-config", "3.0.2.RELEASE");
	private static final Dependency DEPENDENCY_WEB = new Dependency("org.springframework.security", "spring-security-web", "3.0.2.RELEASE");
	private static final Dependency DEPENDENCY_TAGLIBS = new Dependency("org.springframework.security", "spring-security-taglibs", "3.0.2.RELEASE");
	
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
			tilesOperations.addViewDefinition("", "login", TilesOperations.PUBLIC_TEMPLATE, "/WEB-INF/views/login.jspx");
		}
				
		String webXml = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		
		try {
			if (fileManager.exists(webXml)) {
				MutableFile mutableWebXml = fileManager.updateFile(webXml);
				Document webXmlDoc = XmlUtils.getDocumentBuilder().parse(mutableWebXml.getInputStream());
				WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.BEFORE, null, WebMvcOperations.CHARACTER_ENCODING_FILTER_NAME, SecurityOperations.SECURITY_FILTER_NAME, "org.springframework.web.filter.DelegatingFilterProxy", "/*", webXmlDoc, null);		
				XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
			} else {
				throw new IllegalStateException("Could not acquire " + webXml);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 			
		
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

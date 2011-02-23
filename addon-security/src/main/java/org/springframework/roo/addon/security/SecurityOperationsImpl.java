package org.springframework.roo.addon.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
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
 * @author Alan Stewart
 * @since 1.0
 */
@Component 
@Service 
public class SecurityOperationsImpl implements SecurityOperations {
	private static final String SECURITY_VERSION = "3.0.5.RELEASE";
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private TilesOperations tilesOperations;

	public boolean isInstallSecurityAvailable() {
		// Do not permit installation unless they have a web project (as per ROO-342)
		// and only permit installation if they don't already have some version of Spring Security installed
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml")) && projectOperations.getProjectMetadata().getDependenciesExcludingVersion(new Dependency("org.springframework.security", "spring-security-core", SECURITY_VERSION)).size() == 0;
	}

	public void installSecurity() {
		// Parse the configuration.xml file
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Add POM properties
		updatePomProperties(configuration);

		// Add dependencies to POM
		updateDependencies(configuration);

		PathResolver pathResolver = projectOperations.getPathResolver();
		
		// Copy the template across
		String destination = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-security.xml");
		if (!fileManager.exists(destination)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-security-template.xml"), fileManager.createFile(destination).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}

		// Copy the template across
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
				WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.BETWEEN, WebMvcOperations.HTTP_METHOD_FILTER_NAME, WebMvcOperations.OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME, SecurityOperations.SECURITY_FILTER_NAME, "org.springframework.web.filter.DelegatingFilterProxy", "/*", webXmlDoc, null);
				XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
			} else {
				throw new IllegalStateException("Could not acquire " + webXml);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		// Include static view controller handler to webmvc-config.xml
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

	private void updatePomProperties(Element configuration) {
		List<Element> databaseProperties = XmlUtils.findElements("/configuration/spring-security/properties/*", configuration);
		for (Element property : databaseProperties) {
			projectOperations.addProperty(new Property(property));
		}
	}

	private void updateDependencies(Element configuration) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> securityDependencies = XmlUtils.findElements("/configuration/spring-security/dependencies/dependency", configuration);
		for (Element dependencyElement : securityDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(dependencies);
	}
}

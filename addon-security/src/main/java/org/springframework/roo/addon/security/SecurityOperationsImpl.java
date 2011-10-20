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
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
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

	// Constants
	private static final String SECURITY_VERSION = "3.0.5.RELEASE";

	// Fields
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;
	@Reference private TilesOperations tilesOperations;

	public boolean isInstallSecurityAvailable() {
		// Do not permit installation unless they have a web project (as per ROO-342)
		// and only permit installation if they don't already have some version of Spring Security installed
		return projectOperations.isFocusedProjectAvailable() && fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml")) && projectOperations.getFocusedModule().getDependenciesExcludingVersion(new Dependency("org.springframework.security", "spring-security-core", SECURITY_VERSION)).isEmpty();
	}

	public void installSecurity() {
		// Parse the configuration.xml file
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Add POM properties
		updatePomProperties(configuration, projectOperations.getFocusedModuleName());

		// Add dependencies to POM
		updateDependencies(configuration, projectOperations.getFocusedModuleName());

		// Copy the template across
		String destination = pathResolver.getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-security.xml");
		if (!fileManager.exists(destination)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-security-template.xml"), fileManager.createFile(destination).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}

		// Copy the template across
		String loginPage = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/login.jspx");
		if (!fileManager.exists(loginPage)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "login.jspx"), fileManager.createFile(loginPage).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}

		if (fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/views.xml"))) {
			tilesOperations.addViewDefinition("", pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP), "login", TilesOperations.PUBLIC_TEMPLATE, "WEB-INF/views/login.jspx");
		}

		String webXmlPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		Document webXmlDocument = XmlUtils.readXml(fileManager.getInputStream(webXmlPath));
		WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.BETWEEN, WebMvcOperations.HTTP_METHOD_FILTER_NAME, WebMvcOperations.OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME, SecurityOperations.SECURITY_FILTER_NAME, "org.springframework.web.filter.DelegatingFilterProxy", "/*", webXmlDocument, null);
		fileManager.createOrUpdateTextFileIfRequired(webXmlPath, XmlUtils.nodeToString(webXmlDocument), false);

		// Include static view controller handler to webmvc-config.xml
		String webConfigPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		Document webConfigDocument = XmlUtils.readXml(fileManager.getInputStream(webConfigPath));
		Element webConfig = webConfigDocument.getDocumentElement();
		Element viewController = DomUtils.findFirstElementByName("mvc:view-controller", webConfig);
		Assert.notNull(viewController, "Could not find mvc:view-controller in " + webConfig);
		viewController.getParentNode().insertBefore(new XmlElementBuilder("mvc:view-controller", webConfigDocument).addAttribute("path", "/login").build(), viewController);
		fileManager.createOrUpdateTextFileIfRequired(webConfigPath, XmlUtils.nodeToString(webConfigDocument), false);
	}

	private void updatePomProperties(Element configuration, final String moduleName) {
		List<Element> databaseProperties = XmlUtils.findElements("/configuration/spring-security/properties/*", configuration);
		for (Element property : databaseProperties) {
			projectOperations.addProperty(moduleName, new Property(property));
		}
	}

	private void updateDependencies(Element configuration, final String moduleName) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> securityDependencies = XmlUtils.findElements("/configuration/spring-security/dependencies/dependency", configuration);
		for (Element dependencyElement : securityDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(moduleName, dependencies);
	}
}

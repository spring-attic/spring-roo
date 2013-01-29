package org.springframework.roo.addon.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
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

    private static final Dependency SPRING_SECURITY = new Dependency(
            "org.springframework.security", "spring-security-core",
            "3.1.0.RELEASE");

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TilesOperations tilesOperations;

    public void installSecurity() {
        // Parse the configuration.xml file
        final Element configuration = XmlUtils.getConfiguration(getClass());

        // Add POM properties
        updatePomProperties(configuration,
                projectOperations.getFocusedModuleName());

        // Add dependencies to POM
        updateDependencies(configuration,
                projectOperations.getFocusedModuleName());

        // Copy the template across
        final String destination = pathResolver.getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, "applicationContext-security.xml");
        if (!fileManager.exists(destination)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(),
                        "applicationContext-security-template.xml");
                outputStream = fileManager.createFile(destination)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        // Copy the template across
        final String loginPage = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views/login.jspx");
        if (!fileManager.exists(loginPage)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils
                        .getInputStream(getClass(), "login.jspx");
                outputStream = fileManager.createFile(loginPage)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        if (fileManager.exists(pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views/views.xml"))) {
            tilesOperations.addViewDefinition("",
                    pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP), "login",
                    TilesOperations.PUBLIC_TEMPLATE,
                    "/WEB-INF/views/login.jspx");
        }

        final String webXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
        final Document webXmlDocument = XmlUtils.readXml(fileManager
                .getInputStream(webXmlPath));
        WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.BETWEEN,
                WebMvcOperations.HTTP_METHOD_FILTER_NAME,
                WebMvcOperations.OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME,
                SecurityOperations.SECURITY_FILTER_NAME,
                "org.springframework.web.filter.DelegatingFilterProxy", "/*",
                webXmlDocument, null);
        fileManager.createOrUpdateTextFileIfRequired(webXmlPath,
                XmlUtils.nodeToString(webXmlDocument), false);

        // Include static view controller handler to webmvc-config.xml
        final String webConfigPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
        final Document webConfigDocument = XmlUtils.readXml(fileManager
                .getInputStream(webConfigPath));
        final Element webConfig = webConfigDocument.getDocumentElement();
        final Element viewController = DomUtils.findFirstElementByName(
                "mvc:view-controller", webConfig);
        Validate.notNull(viewController,
                "Could not find mvc:view-controller in %s", webConfig);
        viewController.getParentNode()
                .insertBefore(
                        new XmlElementBuilder("mvc:view-controller",
                                webConfigDocument).addAttribute("path",
                                "/login").build(), viewController);
        fileManager.createOrUpdateTextFileIfRequired(webConfigPath,
                XmlUtils.nodeToString(webConfigDocument), false);
    }

    public boolean isSecurityInstallationPossible() {
        // Permit installation if they have a web project (as per ROO-342) and
        // no version of Spring Security is already installed.
        return projectOperations.isFocusedProjectAvailable()
                && fileManager.exists(pathResolver.getFocusedIdentifier(
                        Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"))
                && !projectOperations.getFocusedModule()
                        .hasDependencyExcludingVersion(SPRING_SECURITY)
                && !projectOperations.isFeatureInstalledInFocusedModule(
                        FeatureNames.JSF, FeatureNames.GWT);
    }

    private void updateDependencies(final Element configuration,
            final String moduleName) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> securityDependencies = XmlUtils.findElements(
                "/configuration/spring-security/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : securityDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }
        projectOperations.addDependencies(moduleName, dependencies);
    }

    private void updatePomProperties(final Element configuration,
            final String moduleName) {
        final List<Element> databaseProperties = XmlUtils.findElements(
                "/configuration/spring-security/properties/*", configuration);
        for (final Element property : databaseProperties) {
            projectOperations.addProperty(moduleName, new Property(property));
        }
    }
}

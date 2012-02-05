package org.springframework.roo.addon.web.flow;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.flow.XmlTemplate.DomElementCallback;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.JspOperations;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Web Flow configuration operations.
 * 
 * @author Stefan Schmidt
 * @author Rossen Stoyanchev
 * @since 1.0
 */
@Component
@Service
public class WebFlowOperationsImpl implements WebFlowOperations {

    @Reference private FileManager fileManager;
    @Reference private JspOperations jspOperations;
    @Reference private MenuOperations menuOperations;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TilesOperations tilesOperations;
    @Reference private WebMvcOperations webMvcOperations;

    private void copyTemplate(final String templateFileName,
            final String resolvedTargetDirectoryPath) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = FileUtils
                    .getInputStream(getClass(), templateFileName);
            outputStream = fileManager.createFile(
                    resolvedTargetDirectoryPath + "/" + templateFileName)
                    .getOutputStream();
            IOUtils.copy(inputStream, outputStream);
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Encountered an error during copying of resources for Web Flow addon.",
                    e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    private String getFlowId(String flowName) {
        flowName = StringUtils.defaultIfEmpty(flowName, "sample-flow");
        if (flowName.startsWith("/")) {
            flowName = flowName.substring(1);
        }
        return flowName.replaceAll("[^a-zA-Z/_]", "");
    }

    /**
     * See {@link WebFlowOperations#installWebFlow(String)}.
     */
    public void installWebFlow(final String flowName) {
        installWebFlowConfiguration();

        final String flowId = getFlowId(flowName);
        final String webRelativeFlowPath = "/WEB-INF/views/" + flowId;
        final String resolvedFlowPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, webRelativeFlowPath);
        final String resolvedFlowDefinitionPath = resolvedFlowPath
                + "/flow.xml";

        if (fileManager.exists(resolvedFlowPath)) {
            throw new IllegalStateException("Flow directory already exists: "
                    + resolvedFlowPath);
        }
        fileManager.createDirectory(resolvedFlowPath);

        copyTemplate("flow.xml", resolvedFlowPath);
        copyTemplate("view-state-1.jspx", resolvedFlowPath);
        copyTemplate("view-state-2.jspx", resolvedFlowPath);
        copyTemplate("end-state.jspx", resolvedFlowPath);

        new XmlTemplate(fileManager).update(resolvedFlowDefinitionPath,
                new DomElementCallback() {
                    public boolean doWithElement(final Document document,
                            final Element root) {
                        final List<Element> states = XmlUtils.findElements(
                                "/flow/view-state|end-state", root);
                        for (final Element state : states) {
                            state.setAttribute("view",
                                    flowId + "/" + state.getAttribute("id"));
                        }
                        return true;
                    }
                });

        final JavaSymbolName flowMenuCategory = new JavaSymbolName("Flows");
        final JavaSymbolName flowMenuName = new JavaSymbolName(flowId.replace(
                "/", "_"));
        menuOperations.addMenuItem(flowMenuCategory, flowMenuName,
                flowMenuName.getReadableSymbolName(), "webflow_menu_enter", "/"
                        + flowId, null,
                pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP));

        tilesOperations.addViewDefinition(flowId,
                pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP), flowId
                        + "/*", TilesOperations.DEFAULT_TEMPLATE,
                webRelativeFlowPath + "/{1}.jspx");

        updateConfiguration();
        webMvcOperations.registerWebFlowConversionServiceExposingInterceptor();
    }

    private void installWebFlowConfiguration() {
        final String resolvedSpringConfigPath = pathResolver
                .getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring");
        if (fileManager
                .exists(resolvedSpringConfigPath + "/webflow-config.xml")) {
            return;
        }

        copyTemplate("webflow-config.xml", resolvedSpringConfigPath);

        final String webMvcConfigPath = resolvedSpringConfigPath
                + "/webmvc-config.xml";
        if (!fileManager.exists(webMvcConfigPath)) {
            webMvcOperations.installAllWebMvcArtifacts();
        }

        jspOperations.installCommonViewArtefacts();

        new XmlTemplate(fileManager).update(webMvcConfigPath,
                new DomElementCallback() {
                    public boolean doWithElement(final Document document,
                            final Element root) {
                        if (null == XmlUtils
                                .findFirstElement(
                                        "/beans/import[@resource='webflow-config.xml']",
                                        root)) {
                            final Element importSWF = document
                                    .createElement("import");
                            importSWF.setAttribute("resource",
                                    "webflow-config.xml");
                            root.appendChild(importSWF);
                            return true;
                        }
                        return false;
                    }
                });
    }

    public boolean isWebFlowInstallationPossible() {
        return projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.MVC)
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    private void updateConfiguration() {
        final Element configuration = XmlUtils.getConfiguration(getClass());
        final String focusedModuleName = projectOperations
                .getFocusedModuleName();

        final List<Dependency> dependencyElements = new ArrayList<Dependency>();
        for (final Element webFlowDependencyElement : XmlUtils.findElements(
                "/configuration/springWebFlow/dependencies/dependency",
                configuration)) {
            dependencyElements.add(new Dependency(webFlowDependencyElement));
        }
        projectOperations
                .addDependencies(focusedModuleName, dependencyElements);

        final List<Repository> repositoryElements = new ArrayList<Repository>();
        for (final Element repositoryElement : XmlUtils.findElements(
                "/configuration/springWebFlow/repositories/repository",
                configuration)) {
            repositoryElements.add(new Repository(repositoryElement));
        }
        projectOperations
                .addRepositories(focusedModuleName, repositoryElements);

        projectOperations.updateProjectType(focusedModuleName, ProjectType.WAR);
    }
}
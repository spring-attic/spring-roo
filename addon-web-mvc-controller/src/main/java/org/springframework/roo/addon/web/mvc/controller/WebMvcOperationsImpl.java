package org.springframework.roo.addon.web.mvc.controller;

import static org.springframework.roo.model.JdkJavaType.EXCEPTION;
import static org.springframework.roo.model.SpringJavaType.CHARACTER_ENCODING_FILTER;
import static org.springframework.roo.model.SpringJavaType.CONTEXT_LOADER_LISTENER;
import static org.springframework.roo.model.SpringJavaType.CONVERSION_SERVICE_EXPOSING_INTERCEPTOR;
import static org.springframework.roo.model.SpringJavaType.DISPATCHER_SERVLET;
import static org.springframework.roo.model.SpringJavaType.FLOW_HANDLER_MAPPING;
import static org.springframework.roo.model.SpringJavaType.HIDDEN_HTTP_METHOD_FILTER;
import static org.springframework.roo.model.SpringJavaType.OPEN_ENTITY_MANAGER_IN_VIEW_FILTER;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link WebMvcOperations}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class WebMvcOperationsImpl implements WebMvcOperations {

    private static final String CONVERSION_SERVICE_BEAN_NAME = "applicationConversionService";
    private static final String CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME = "conversionServiceExposingInterceptor";
    private static final String CONVERSION_SERVICE_SIMPLE_TYPE = "ApplicationConversionServiceFactoryBean";
    private static final String WEB_XML = "WEB-INF/web.xml";
    private static final String WEBMVC_CONFIG_XML = "WEB-INF/spring/webmvc-config.xml";

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;

    private void copyWebXml() {
        Validate.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");

        // Verify the servlet application context already exists
        final String servletCtxFilename = WEBMVC_CONFIG_XML;
        Validate.isTrue(fileManager.exists(pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, servletCtxFilename)),
                "'%s' does not exist", servletCtxFilename);

        final String webXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEB_XML);
        if (fileManager.exists(webXmlPath)) {
            // File exists, so nothing to do
            return;
        }

        final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), "web-template.xml");
        Validate.notNull(templateInputStream,
                "Could not acquire web.xml template");
        final Document document = XmlUtils.readXml(templateInputStream);

        final String projectName = projectOperations
                .getProjectName(projectOperations.getFocusedModuleName());
        WebXmlUtils.setDisplayName(projectName, document, null);
        WebXmlUtils.setDescription("Roo generated " + projectName
                + " application", document, null);

        fileManager.createOrUpdateTextFileIfRequired(webXmlPath,
                XmlUtils.nodeToString(document), true);
    }

    private void createWebApplicationContext() {
        Validate.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");
        final String webConfigFile = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        final Document document = readOrCreateSpringWebConfigFile(webConfigFile);
        setBasePackageForComponentScan(document);
        fileManager.createOrUpdateTextFileIfRequired(webConfigFile,
                XmlUtils.nodeToString(document), true);
    }

    public void installAllWebMvcArtifacts() {
        installMinimalWebArtifacts();
        manageWebXml();
        updateConfiguration();
    }

    public void installConversionService(final JavaPackage destinationPackage) {
        final String webMvcConfigPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        Validate.isTrue(fileManager.exists(webMvcConfigPath),
                "'%s' does not exist", webMvcConfigPath);

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(webMvcConfigPath));
        final Element root = document.getDocumentElement();

        final Element annotationDriven = DomUtils.findFirstElementByName(
                "mvc:annotation-driven", root);
        if (isConversionServiceConfigured(root, annotationDriven)) {
            // Conversion service already defined, moving on.
            return;
        }
        annotationDriven.setAttribute("conversion-service",
                CONVERSION_SERVICE_BEAN_NAME);

        final Element conversionServiceBean = new XmlElementBuilder("bean",
                document)
                .addAttribute("id", CONVERSION_SERVICE_BEAN_NAME)
                .addAttribute(
                        "class",
                        destinationPackage.getFullyQualifiedPackageName() + "."
                                + CONVERSION_SERVICE_SIMPLE_TYPE).build();
        root.appendChild(conversionServiceBean);

        fileManager.createOrUpdateTextFileIfRequired(webMvcConfigPath,
                XmlUtils.nodeToString(document), false);

        installConversionServiceJavaClass(destinationPackage);

        registerWebFlowConversionServiceExposingInterceptor();
    }

    private void installConversionServiceJavaClass(final JavaPackage thePackage) {
        final JavaType javaType = new JavaType(
                thePackage.getFullyQualifiedPackageName()
                        + ".ApplicationConversionServiceFactoryBean");
        final String physicalPath = pathResolver.getFocusedCanonicalPath(
                Path.SRC_MAIN_JAVA, javaType);
        if (fileManager.exists(physicalPath)) {
            return;
        }
        InputStream inputStream = null;
        try {
            inputStream = FileUtils
                    .getInputStream(getClass(),
                            "converter/ApplicationConversionServiceFactoryBean-template._java");
            String input = IOUtils.toString(inputStream);
            input = input.replace("__PACKAGE__",
                    thePackage.getFullyQualifiedPackageName());
            fileManager.createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);
        }
        catch (final IOException e) {
            throw new IllegalStateException("Unable to create '" + physicalPath
                    + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public void installMinimalWebArtifacts() {
        // Note that the sequence matters here as some of these artifacts are
        // loaded further down the line
        createWebApplicationContext();
        copyWebXml();
    }

    private boolean isConversionServiceConfigured() {
        final String webMvcConfigPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        Validate.isTrue(fileManager.exists(webMvcConfigPath),
                "'%s'  doesn't exist", webMvcConfigPath);

        final MutableFile mutableFile = fileManager
                .updateFile(webMvcConfigPath);
        final Document document = XmlUtils
                .readXml(mutableFile.getInputStream());
        final Element root = document.getDocumentElement();

        final Element annotationDrivenElement = DomUtils
                .findFirstElementByName("mvc:annotation-driven", root);
        return isConversionServiceConfigured(root, annotationDrivenElement);
    }

    private boolean isConversionServiceConfigured(final Element root,
            final Element annotationDrivenElement) {
        final String beanName = annotationDrivenElement
                .getAttribute("conversion-service");
        if (StringUtils.isBlank(beanName)) {
            return false;
        }

        final Element bean = XmlUtils.findFirstElement("/beans/bean[@id=\""
                + beanName + "\"]", root);
        final String classAttribute = bean.getAttribute("class");
        final StringBuilder sb = new StringBuilder(
                "Found custom ConversionService installed in webmvc-config.xml. ");
        sb.append("Remove the conversion-service attribute, let Spring ROO 1.1.1 (or higher), install the new application-wide ");
        sb.append("ApplicationConversionServiceFactoryBean and then use that to register your custom converters and formatters.");
        Validate.isTrue(
                classAttribute.endsWith(CONVERSION_SERVICE_SIMPLE_TYPE),
                sb.toString());
        return true;
    }

    private void manageWebXml() {
        Validate.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");

        // Verify that the web.xml already exists
        final String webXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEB_XML);
        Validate.isTrue(fileManager.exists(webXmlPath), "'%s' does not exist",
                webXmlPath);

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(webXmlPath));

        WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam(
                "defaultHtmlEscape", "true"), document,
                "Enable escaping of form submission contents");
        WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam(
                "contextConfigLocation",
                "classpath*:META-INF/spring/applicationContext*.xml"),
                document, null);
        WebXmlUtils.addFilter(CHARACTER_ENCODING_FILTER_NAME,
                CHARACTER_ENCODING_FILTER.getFullyQualifiedTypeName(), "/*",
                document, null,
                new WebXmlUtils.WebXmlParam("encoding", "UTF-8"),
                new WebXmlUtils.WebXmlParam("forceEncoding", "true"));
        WebXmlUtils.addFilter(HTTP_METHOD_FILTER_NAME,
                HIDDEN_HTTP_METHOD_FILTER.getFullyQualifiedTypeName(), "/*",
                document, null);
        if (projectOperations.isFeatureInstalled(FeatureNames.JPA)) {
            WebXmlUtils.addFilter(OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME,
                    OPEN_ENTITY_MANAGER_IN_VIEW_FILTER
                            .getFullyQualifiedTypeName(), "/*", document, null);
        }
        WebXmlUtils
                .addListener(
                        CONTEXT_LOADER_LISTENER.getFullyQualifiedTypeName(),
                        document,
                        "Creates the Spring Container shared by all Servlets and Filters");
        WebXmlUtils.addServlet(projectOperations.getFocusedProjectName(),
                DISPATCHER_SERVLET.getFullyQualifiedTypeName(), "/", 1,
                document, "Handles Spring requests",
                new WebXmlUtils.WebXmlParam("contextConfigLocation",
                        WEBMVC_CONFIG_XML));
        WebXmlUtils.setSessionTimeout(10, document, null);
        WebXmlUtils.addExceptionType(EXCEPTION.getFullyQualifiedTypeName(),
                "/uncaughtException", document, null);
        WebXmlUtils.addErrorCode(new Integer(404), "/resourceNotFound",
                document, null);

        fileManager.createOrUpdateTextFileIfRequired(webXmlPath,
                XmlUtils.nodeToString(document), false);
    }

    private Document readOrCreateSpringWebConfigFile(final String webConfigFile) {
        final InputStream inputStream;
        if (fileManager.exists(webConfigFile)) {
            inputStream = fileManager.getInputStream(webConfigFile);
        }
        else {
            inputStream = FileUtils.getInputStream(getClass(),
                    "webmvc-config.xml");
            Validate.notNull(inputStream, "Could not acquire web.xml template");
        }
        return XmlUtils.readXml(inputStream);
    }

    public void registerWebFlowConversionServiceExposingInterceptor() {
        final String webFlowConfigPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webflow-config.xml");
        if (!fileManager.exists(webFlowConfigPath)) {
            // No web flow configured, moving on.
            return;
        }

        if (!isConversionServiceConfigured()) {
            // We only need to install the ConversionServiceExposingInterceptor
            // for Web Flow if a custom conversion service is present.
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(webFlowConfigPath));
        final Element root = document.getDocumentElement();

        if (XmlUtils.findFirstElement("/beans/bean[@id='"
                + CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME + "']", root) == null) {
            final Element conversionServiceExposingInterceptor = new XmlElementBuilder(
                    "bean", document)
                    .addAttribute(
                            "class",
                            CONVERSION_SERVICE_EXPOSING_INTERCEPTOR
                                    .getFullyQualifiedTypeName())
                    .addAttribute("id",
                            CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME)
                    .addChild(
                            new XmlElementBuilder("constructor-arg", document)
                                    .addAttribute("ref",
                                            CONVERSION_SERVICE_BEAN_NAME)
                                    .build()).build();
            root.appendChild(conversionServiceExposingInterceptor);
        }
        final Element flowHandlerMapping = XmlUtils.findFirstElement(
                "/beans/bean[@class='"
                        + FLOW_HANDLER_MAPPING.getFullyQualifiedTypeName()
                        + "']", root);
        if (flowHandlerMapping != null) {
            if (XmlUtils.findFirstElement(
                    "property[@name='interceptors']/array/ref[@bean='"
                            + CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME
                            + "']", flowHandlerMapping) == null) {
                final Element interceptors = new XmlElementBuilder("property",
                        document)
                        .addAttribute("name", "interceptors")
                        .addChild(
                                new XmlElementBuilder("array", document)
                                        .addChild(
                                                new XmlElementBuilder("ref",
                                                        document)
                                                        .addAttribute("bean",
                                                                CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME)
                                                        .build()).build())
                        .build();
                flowHandlerMapping.appendChild(interceptors);
            }
        }

        fileManager.createOrUpdateTextFileIfRequired(webFlowConfigPath,
                XmlUtils.nodeToString(document), false);
    }

    private void setBasePackageForComponentScan(final Document document) {
        final Element componentScanElement = DomUtils.findFirstElementByName(
                "context:component-scan", (Element) document.getFirstChild());
        final JavaPackage topLevelPackage = projectOperations
                .getTopLevelPackage(projectOperations.getFocusedModuleName());
        componentScanElement.setAttribute("base-package",
                topLevelPackage.getFullyQualifiedPackageName());
    }

    private void updateConfiguration() {
        // Update webmvc-config.xml if needed.
        final String webConfigFile = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        Validate.isTrue(fileManager.exists(webConfigFile),
                "Aborting: Unable to find %s", webConfigFile);
        InputStream webMvcConfigInputStream = null;
        try {
            webMvcConfigInputStream = fileManager.getInputStream(webConfigFile);
            Validate.notNull(webMvcConfigInputStream,
                    "Aborting: Unable to acquire webmvc-config.xml file");
            final Document webMvcConfig = XmlUtils
                    .readXml(webMvcConfigInputStream);
            final Element root = webMvcConfig.getDocumentElement();
            if (XmlUtils.findFirstElement("/beans/interceptors", root) == null) {
                final InputStream templateInputStream = FileUtils
                        .getInputStream(getClass(),
                                "webmvc-config-additions.xml");
                Validate.notNull(templateInputStream,
                        "Could not acquire webmvc-config-additions.xml template");
                final Document webMvcConfigAdditions = XmlUtils
                        .readXml(templateInputStream);
                final NodeList nodes = webMvcConfigAdditions
                        .getDocumentElement().getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    root.appendChild(webMvcConfig.importNode(nodes.item(i),
                            true));
                }
                fileManager.createOrUpdateTextFileIfRequired(webConfigFile,
                        XmlUtils.nodeToString(webMvcConfig), true);
            }
        }
        finally {
            IOUtils.closeQuietly(webMvcConfigInputStream);
        }

        // Add MVC dependencies.
        final boolean isGaeEnabled = projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.GAE);
        final Element configuration = XmlUtils.getConfiguration(getClass());

        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> springDependencies = XmlUtils.findElements(
                "/configuration/springWebMvc/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : springDependencies) {
            final Dependency dependency = new Dependency(dependencyElement);
            if (isGaeEnabled
                    && dependency.getGroupId().equals("org.glassfish.web")
                    && dependency.getArtifactId().equals("jstl-impl")) {
                dependencies.add(new Dependency(dependency.getGroupId(),
                        dependency.getArtifactId(), dependency.getVersion(),
                        DependencyType.JAR, DependencyScope.PROVIDED));
            }
            else {
                dependencies.add(dependency);
            }
        }

        projectOperations.addDependencies(
                projectOperations.getFocusedModuleName(), dependencies);

        projectOperations.updateProjectType(
                projectOperations.getFocusedModuleName(), ProjectType.WAR);
    }
}

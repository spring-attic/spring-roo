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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.StringUtils;
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

    // Constants
    private static final String CONVERSION_SERVICE_SIMPLE_TYPE = "ApplicationConversionServiceFactoryBean";
    private static final String CONVERSION_SERVICE_BEAN_NAME = "applicationConversionService";
    private static final String CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME = "conversionServiceExposingInterceptor";
    private static final String WEB_XML = "WEB-INF/web.xml";
    private static final String WEBMVC_CONFIG_XML = "WEB-INF/spring/webmvc-config.xml";

    // Fields
    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;

    public void installMinimalWebArtifacts() {
        // Note that the sequence matters here as some of these artifacts are
        // loaded further down the line
        createWebApplicationContext();
        copyWebXml();
    }

    public void installAllWebMvcArtifacts() {
        installMinimalWebArtifacts();
        manageWebXml();
        updateConfiguration();
    }

    public void installConversionService(final JavaPackage destinationPackage) {
        String webMvcConfigPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        Assert.isTrue(fileManager.exists(webMvcConfigPath), "'"
                + webMvcConfigPath + "' does not exist");

        Document document = XmlUtils.readXml(fileManager
                .getInputStream(webMvcConfigPath));
        Element root = document.getDocumentElement();

        Element annotationDriven = DomUtils.findFirstElementByName(
                "mvc:annotation-driven", root);
        if (isConversionServiceConfigured(root, annotationDriven)) {
            // Conversion service already defined, moving on.
            return;
        }
        annotationDriven.setAttribute("conversion-service",
                CONVERSION_SERVICE_BEAN_NAME);

        Element conversionServiceBean = new XmlElementBuilder("bean", document)
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

    public void registerWebFlowConversionServiceExposingInterceptor() {
        String webFlowConfigPath = pathResolver.getFocusedIdentifier(
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

        Document document = XmlUtils.readXml(fileManager
                .getInputStream(webFlowConfigPath));
        Element root = document.getDocumentElement();

        if (XmlUtils.findFirstElement("/beans/bean[@id='"
                + CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME + "']", root) == null) {
            Element conversionServiceExposingInterceptor = new XmlElementBuilder(
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
        Element flowHandlerMapping = XmlUtils.findFirstElement(
                "/beans/bean[@class='"
                        + FLOW_HANDLER_MAPPING.getFullyQualifiedTypeName()
                        + "']", root);
        if (flowHandlerMapping != null) {
            if (XmlUtils.findFirstElement(
                    "property[@name='interceptors']/array/ref[@bean='"
                            + CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME
                            + "']", flowHandlerMapping) == null) {
                Element interceptors = new XmlElementBuilder("property",
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

    private void copyWebXml() {
        Assert.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");

        // Verify the servlet application context already exists
        String servletCtxFilename = WEBMVC_CONFIG_XML;
        Assert.isTrue(fileManager.exists(pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, servletCtxFilename)), "'"
                + servletCtxFilename + "' does not exist");

        String webXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEB_XML);
        if (fileManager.exists(webXmlPath)) {
            // File exists, so nothing to do
            return;
        }

        final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), "web-template.xml");
        Assert.notNull(templateInputStream,
                "Could not acquire web.xml template");
        final Document document = XmlUtils.readXml(templateInputStream);

        String projectName = projectOperations.getProjectName(projectOperations
                .getFocusedModuleName());
        WebXmlUtils.setDisplayName(projectName, document, null);
        WebXmlUtils.setDescription("Roo generated " + projectName
                + " application", document, null);

        fileManager.createOrUpdateTextFileIfRequired(webXmlPath,
                XmlUtils.nodeToString(document), true);
    }

    private void manageWebXml() {
        Assert.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");

        // Verify that the web.xml already exists
        String webXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEB_XML);
        Assert.isTrue(fileManager.exists(webXmlPath), "'" + webXmlPath
                + "' does not exist");

        Document document = XmlUtils.readXml(fileManager
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

    private void createWebApplicationContext() {
        Assert.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");
        final String webConfigFile = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        final Document document = readOrCreateSpringWebConfigFile(webConfigFile);
        setBasePackageForComponentScan(document);
        fileManager.createOrUpdateTextFileIfRequired(webConfigFile,
                XmlUtils.nodeToString(document), true);
    }

    private void setBasePackageForComponentScan(final Document document) {
        final Element componentScanElement = DomUtils.findFirstElementByName(
                "context:component-scan", (Element) document.getFirstChild());
        final JavaPackage topLevelPackage = projectOperations
                .getTopLevelPackage(projectOperations.getFocusedModuleName());
        componentScanElement.setAttribute("base-package",
                topLevelPackage.getFullyQualifiedPackageName());
    }

    private Document readOrCreateSpringWebConfigFile(final String webConfigFile) {
        final InputStream inputStream;
        if (fileManager.exists(webConfigFile)) {
            inputStream = fileManager.getInputStream(webConfigFile);
        }
        else {
            inputStream = FileUtils.getInputStream(getClass(),
                    "webmvc-config.xml");
            Assert.notNull(inputStream, "Could not acquire web.xml template");
        }
        return XmlUtils.readXml(inputStream);
    }

    private void updateConfiguration() {
        // Update webmvc-config.xml if needed.
        String webConfigFile = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        Assert.isTrue(fileManager.exists(webConfigFile),
                "Aborting: Unable to find " + webConfigFile);
        InputStream webMvcConfigInputStream = null;
        try {
            webMvcConfigInputStream = fileManager.getInputStream(webConfigFile);
            Assert.notNull(webMvcConfigInputStream,
                    "Aborting: Unable to acquire webmvc-config.xml file");
            Document webMvcConfig = XmlUtils.readXml(webMvcConfigInputStream);
            Element root = webMvcConfig.getDocumentElement();
            if (XmlUtils.findFirstElement("/beans/interceptors", root) == null) {
                InputStream templateInputStream = FileUtils.getInputStream(
                        getClass(), "webmvc-config-additions.xml");
                Assert.notNull(templateInputStream,
                        "Could not acquire webmvc-config-additions.xml template");
                Document webMvcConfigAdditions = XmlUtils
                        .readXml(templateInputStream);
                NodeList nodes = webMvcConfigAdditions.getDocumentElement()
                        .getChildNodes();
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
        boolean isGaeEnabled = projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.GAE);
        Element configuration = XmlUtils.getConfiguration(getClass());

        List<Dependency> dependencies = new ArrayList<Dependency>();
        List<Element> springDependencies = XmlUtils.findElements(
                "/configuration/springWebMvc/dependencies/dependency",
                configuration);
        for (Element dependencyElement : springDependencies) {
            Dependency dependency = new Dependency(dependencyElement);
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

    private boolean isConversionServiceConfigured() {
        String webMvcConfigPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, WEBMVC_CONFIG_XML);
        Assert.isTrue(fileManager.exists(webMvcConfigPath), webMvcConfigPath
                + " doesn't exist");

        MutableFile mutableFile = fileManager.updateFile(webMvcConfigPath);
        Document document = XmlUtils.readXml(mutableFile.getInputStream());
        Element root = document.getDocumentElement();

        Element annotationDrivenElement = DomUtils.findFirstElementByName(
                "mvc:annotation-driven", root);
        return isConversionServiceConfigured(root, annotationDrivenElement);
    }

    private boolean isConversionServiceConfigured(final Element root,
            final Element annotationDrivenElement) {
        String beanName = annotationDrivenElement
                .getAttribute("conversion-service");
        if (StringUtils.isBlank(beanName)) {
            return false;
        }

        Element bean = XmlUtils.findFirstElement("/beans/bean[@id=\""
                + beanName + "\"]", root);
        String classAttribute = bean.getAttribute("class");
        StringBuilder sb = new StringBuilder(
                "Found custom ConversionService installed in webmvc-config.xml. ");
        sb.append("Remove the conversion-service attribute, let Spring ROO 1.1.1 (or higher), install the new application-wide ");
        sb.append("ApplicationConversionServiceFactoryBean and then use that to register your custom converters and formatters.");
        Assert.isTrue(classAttribute.endsWith(CONVERSION_SERVICE_SIMPLE_TYPE),
                sb.toString());
        return true;
    }

    private void installConversionServiceJavaClass(final JavaPackage thePackage) {
        JavaType javaType = new JavaType(
                thePackage.getFullyQualifiedPackageName()
                        + ".ApplicationConversionServiceFactoryBean");
        String physicalPath = pathResolver.getFocusedCanonicalPath(
                Path.SRC_MAIN_JAVA, javaType);
        if (fileManager.exists(physicalPath)) {
            return;
        }
        try {
            InputStream template = FileUtils
                    .getInputStream(getClass(),
                            "converter/ApplicationConversionServiceFactoryBean-template._java");
            String input = FileCopyUtils.copyToString(new InputStreamReader(
                    template));
            input = input.replace("__PACKAGE__",
                    thePackage.getFullyQualifiedPackageName());
            fileManager.createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to create '" + physicalPath
                    + "'", e);
        }
    }
}

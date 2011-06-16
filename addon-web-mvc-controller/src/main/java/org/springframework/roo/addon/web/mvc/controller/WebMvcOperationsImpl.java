package org.springframework.roo.addon.web.mvc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
@Component 
@Service
public class WebMvcOperationsImpl implements WebMvcOperations {
	private static final String CONVERSION_SERVICE_SIMPLE_TYPE = "ApplicationConversionServiceFactoryBean";
	private static final String CONVERSION_SERVICE_BEAN_NAME = "applicationConversionService";
	private static final String CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME = "conversionServiceExposingInterceptor";
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;

	public void installMinmalWebArtefacts() {
		// Note that the sequence matters here as some of these artifacts are loaded further down the line
		createWebApplicationContext();
		copyWebXml();
	}

	public void installAllWebMvcArtifacts() {
		installMinmalWebArtefacts();
		manageWebXml();
		updateConfiguration();
	}
	
	public void installConversionService(final JavaPackage destinationPackage) {
		String webMvcConfigPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		Assert.isTrue(fileManager.exists(webMvcConfigPath), "'" + webMvcConfigPath + "' does not exist");

		Document document = XmlUtils.readXml(fileManager.getInputStream(webMvcConfigPath));
		Element root = document.getDocumentElement();
		
		Element annotationDriven = XmlUtils.findFirstElementByName("mvc:annotation-driven", root);
		if (isConversionServiceConfigured(root, annotationDriven)) {
			// Conversion service already defined, moving on.
			return;
		}
		annotationDriven.setAttribute("conversion-service", CONVERSION_SERVICE_BEAN_NAME);
		
		Element conversionServiceBean = new XmlElementBuilder("bean", document).addAttribute("id", CONVERSION_SERVICE_BEAN_NAME).addAttribute("class", destinationPackage.getFullyQualifiedPackageName() + "." + CONVERSION_SERVICE_SIMPLE_TYPE).build();
		root.appendChild(conversionServiceBean);
		
		fileManager.createOrUpdateTextFileIfRequired(webMvcConfigPath, XmlUtils.nodeToString(document), false);
		
		installConversionServiceJavaClass(destinationPackage);

		registerWebFlowConversionServiceExposingInterceptor();
	}
	
	public void registerWebFlowConversionServiceExposingInterceptor() {
		String webFlowConfigPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webflow-config.xml");
		if (! fileManager.exists(webFlowConfigPath)) {
			// No web flow configured, moving on.
			return;
		}
		
		if (!isConversionServiceConfigured()) {
			// We only need to install the ConversionServiceExposingInterceptor for Web Flow if a custom conversion service is present.
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(webFlowConfigPath));
		Element root = document.getDocumentElement();
		
		if (XmlUtils.findFirstElement("/beans/bean[@id='" + CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME + "']", root) == null) {
			Element conversionServiceExposingInterceptor = new XmlElementBuilder("bean", document).addAttribute("class", "org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor").addAttribute("id", CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME).addChild(new XmlElementBuilder("constructor-arg", document).addAttribute("ref", CONVERSION_SERVICE_BEAN_NAME).build()).build();
			root.appendChild(conversionServiceExposingInterceptor);
		}
		Element flowHandlerMapping = XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.webflow.mvc.servlet.FlowHandlerMapping']", root);
		if (flowHandlerMapping != null) {
			if (XmlUtils.findFirstElement("property[@name='interceptors']/array/ref[@bean='" + CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME + "']", flowHandlerMapping) == null) {
				Element interceptors = new XmlElementBuilder("property", document).addAttribute("name", "interceptors").addChild(new XmlElementBuilder("array", document).addChild(new XmlElementBuilder("ref", document).addAttribute("bean", CONVERSION_SERVICE_EXPOSING_INTERCEPTOR_NAME).build()).build()).build();
				flowHandlerMapping.appendChild(interceptors);
			}
		}
		
		fileManager.createOrUpdateTextFileIfRequired(webFlowConfigPath, XmlUtils.nodeToString(document), false);
	}

	private void copyWebXml() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		PathResolver pathResolver = projectOperations.getPathResolver();
		
		// Verify the servlet application context already exists
		String servletCtxFilename = "WEB-INF/spring/webmvc-config.xml";
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename)), "'" + servletCtxFilename + "' does not exist");

		String webXmlPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		if (fileManager.exists(webXmlPath)) {
			// File exists, so nothing to do
			return;
		}

		Document document;
		try {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "web-template.xml");
			Assert.notNull(templateInputStream, "Could not acquire web.xml template");
			document = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		String projectName = projectOperations.getProjectMetadata().getProjectName();
		WebXmlUtils.setDisplayName(projectName, document, null);
		WebXmlUtils.setDescription("Roo generated " + projectName + " application", document, null);

		fileManager.createOrUpdateTextFileIfRequired(webXmlPath, XmlUtils.nodeToString(document), true);
	}

	private void manageWebXml() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		PathResolver pathResolver = projectOperations.getPathResolver();

		// Verify that the web.xml already exists
		String webXmlPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		Assert.isTrue(fileManager.exists(webXmlPath), "'" + webXmlPath + "' does not exist");

		Document document = XmlUtils.readXml(fileManager.getInputStream(webXmlPath));

		WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam("defaultHtmlEscape", "true"), document, "Enable escaping of form submission contents");
		WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam("contextConfigLocation", "classpath*:META-INF/spring/applicationContext*.xml"), document, null);
		WebXmlUtils.addFilter(WebMvcOperations.CHARACTER_ENCODING_FILTER_NAME, "org.springframework.web.filter.CharacterEncodingFilter", "/*", document, null, new WebXmlUtils.WebXmlParam("encoding", "UTF-8"), new WebXmlUtils.WebXmlParam("forceEncoding", "true"));
		WebXmlUtils.addFilter(WebMvcOperations.HTTP_METHOD_FILTER_NAME, "org.springframework.web.filter.HiddenHttpMethodFilter", "/*", document, null);
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"))) {
			WebXmlUtils.addFilter(WebMvcOperations.OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME, "org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter", "/*", document, null);
		}
		WebXmlUtils.addListener("org.springframework.web.context.ContextLoaderListener", document, "Creates the Spring Container shared by all Servlets and Filters");
		WebXmlUtils.addServlet(projectOperations.getProjectMetadata().getProjectName(), "org.springframework.web.servlet.DispatcherServlet", "/", new Integer(1), document, "Handles Spring requests", new WebXmlUtils.WebXmlParam("contextConfigLocation", "/WEB-INF/spring/webmvc-config.xml"));
		WebXmlUtils.setSessionTimeout(new Integer(10), document, null);
		// WebXmlUtils.addWelcomeFile("/", webXml, null);
		WebXmlUtils.addExceptionType("java.lang.Exception", "/uncaughtException", document, null);
		WebXmlUtils.addErrorCode(new Integer(404), "/resourceNotFound", document, null);

		fileManager.createOrUpdateTextFileIfRequired(webXmlPath, XmlUtils.nodeToString(document), false);
	}

	private void createWebApplicationContext() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		PathResolver pathResolver = projectOperations.getPathResolver();

		// Verify the middle tier application context already exists
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml")), "Application context does not exist");

		String webConfigFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		if (fileManager.exists(webConfigFile)) {
			// This file already exists, nothing to do
			return;
		}

		Document document;
		try {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "webmvc-config.xml");
			Assert.notNull(templateInputStream, "Could not acquire web.xml template");
			document = XmlUtils.readXml(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = (Element) document.getFirstChild();
		XmlUtils.findFirstElementByName("context:component-scan", root).setAttribute("base-package", projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName());
		
		fileManager.createOrUpdateTextFileIfRequired(webConfigFile, XmlUtils.nodeToString(document), true);
	}

	private void updateConfiguration() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> springDependencies = XmlUtils.findElements("/configuration/springWebMvc/dependencies/dependency", configuration);
		for (Element dependency : springDependencies) {
			dependencies.add(new Dependency(dependency));
		}
		projectOperations.addDependencies(dependencies);
		
		projectOperations.updateProjectType(ProjectType.WAR);
	}

	private boolean isConversionServiceConfigured() {
		String webMvcConfigPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		Assert.isTrue(fileManager.exists(webMvcConfigPath), webMvcConfigPath + " doesn't exist");
		
		MutableFile mutableFile = fileManager.updateFile(webMvcConfigPath);
		Document document = XmlUtils.readXml(mutableFile.getInputStream());
		Element root = document.getDocumentElement();
		
		Element annotationDrivenElement = XmlUtils.findFirstElementByName("mvc:annotation-driven", root);
		return isConversionServiceConfigured(root, annotationDrivenElement);
	}
	
	private boolean isConversionServiceConfigured(Element root, Element annotationDrivenElement) {
		String beanName = annotationDrivenElement.getAttribute("conversion-service");
		if (!StringUtils.hasText(beanName)) {
			return false;
		}
		
		Element bean = XmlUtils.findFirstElement("/beans/bean[@id=\"" + beanName + "\"]", root);
		String classAttribute = bean.getAttribute("class");
		StringBuilder sb = new StringBuilder("Found custom ConversionService installed in webmvc-config.xml. ");
		sb.append("Remove the conversion-service attribute, let Spring ROO 1.1.1 (or higher), install the new application-wide ");
		sb.append("ApplicationConversionServiceFactoryBean and then use that to register your custom converters and formatters.");
		Assert.isTrue(classAttribute.endsWith(CONVERSION_SERVICE_SIMPLE_TYPE), sb.toString());
		return true;
	}
	
	private void installConversionServiceJavaClass(JavaPackage thePackage) {
		JavaType javaType = new JavaType(thePackage.getFullyQualifiedPackageName() + ".ApplicationConversionServiceFactoryBean");
		String physicalPath = typeLocationService.getPhysicalLocationCanonicalPath(javaType, Path.SRC_MAIN_JAVA);
		if (fileManager.exists(physicalPath)) {
			return;
		}
		try {
			InputStream template = TemplateUtils.getTemplate(getClass(), "converter/ApplicationConversionServiceFactoryBean-template._java");
			String input = FileCopyUtils.copyToString(new InputStreamReader(template));
			input = input.replace("__PACKAGE__", thePackage.getFullyQualifiedPackageName());
			fileManager.createOrUpdateTextFileIfRequired(physicalPath, input, false);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
		}
	}
}

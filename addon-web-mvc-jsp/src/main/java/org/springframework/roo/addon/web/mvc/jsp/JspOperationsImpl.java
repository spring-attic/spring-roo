package org.springframework.roo.addon.web.mvc.jsp;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.backup.BackupOperations;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18n;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18nSupport;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperationsImpl;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @author Jeremy Grelle
 * @since 1.0
 */
@Component 
@Service 
public class JspOperationsImpl extends AbstractOperations implements JspOperations {
	@Reference private TypeManagementService typeManagementService;
	@Reference private TypeLocationService typeLocationService;
	@Reference private WebMvcOperations webMvcOperations;
	@Reference private MenuOperations menuOperations;
	@Reference private TilesOperations tilesOperations;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	@Reference private I18nSupport i18nSupport;
	@Reference private UaaRegistrationService uaaRegistrationService;
	@Reference private BackupOperations backupOperations;
	
	public boolean isControllerAvailable() {
		return fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml"));
	}

	private boolean isProjectAvailable() {
		return projectOperations.isProjectAvailable();
	}
	
	public boolean isSetupAvailable() {
		return isProjectAvailable() && !isControllerAvailable();
	}

	public boolean isInstallLanguageCommandAvailable() {
		return isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/footer.jspx"));
	}

	public void installCommonViewArtefacts() {
		Assert.isTrue(isProjectAvailable(), "Project metadata required");
		
		if (!isControllerAvailable()) {
			webMvcOperations.installAllWebMvcArtifacts();
		}
		
		PathResolver pathResolver = projectOperations.getPathResolver();

		// Install tiles config
		updateConfiguration();

		// Install styles
		copyDirectoryContents("images/*.*", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/images"), false);

		// Install styles
		copyDirectoryContents("styles/*.css", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/styles"), false);
		copyDirectoryContents("styles/*.properties", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/classes"), false);

		// Install layout
		copyDirectoryContents("tiles/default.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/"), false);
		copyDirectoryContents("tiles/layouts.xml", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/"), false);
		copyDirectoryContents("tiles/header.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"), false);
		copyDirectoryContents("tiles/footer.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"), false);
		copyDirectoryContents("tiles/views.xml", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"), false);

		// Install common view files
		copyDirectoryContents("*.jspx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/"), false);

		// Install tags
		copyDirectoryContents("tags/form/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/form"), false);
		copyDirectoryContents("tags/form/fields/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/form/fields"), false);
		copyDirectoryContents("tags/menu/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu"), false);
		copyDirectoryContents("tags/util/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/util"), false);

		// Install default language 'en'
		installI18n(i18nSupport.getLanguage(Locale.ENGLISH));

		String i18nDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties");
		if (!fileManager.exists(i18nDirectory)) {
			try {
				String projectName = projectOperations.getProjectMetadata().getProjectName();
				fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties"));
				propFileOperations.addPropertyIfNotExists(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", "application_name", projectName.substring(0, 1).toUpperCase() + projectName.substring(1), true);
			} catch (Exception e) {
				throw new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}

	public void installView(String path, String viewName, String title, String category) {
		installView(path, viewName, title, category, null, true);
	}

	public void installView(String path, String viewName, String title, String category, Document document) {
		installView(path, viewName, title, category, document, true);
	}

	private void installView(String path, String viewName, String title, String category, Document document, boolean registerStaticController) {
		Assert.hasText(path, "Path required");
		Assert.hasText(viewName, "View name required");
		Assert.hasText(title, "Title required");
		path = cleanPath(path);
		viewName = cleanViewName(viewName);
		String lcViewName = viewName.toLowerCase();
		if (document == null) {
			try {
				document = getDocumentTemplate("index-template.jspx");
				XmlUtils.findRequiredElement("/div/message", document.getDocumentElement()).setAttribute("code", "label" + path.replace("/", "_") + "_" + lcViewName);
			} catch (Exception e) {
				throw new IllegalStateException("Encountered an error during copying of resources for controller class.", e);
			}
		}

		String viewFile = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views" + path + "/" + lcViewName + ".jspx");
		fileManager.createOrUpdateTextFileIfRequired(viewFile, XmlUtils.nodeToString(document), false);

		installView(new JavaSymbolName(viewName), path, title, category, registerStaticController);
	}

	/**
	 * Creates a new Spring MVC static view.
	 * 
	 * @param viewName the mapping this view should adopt (required, ie 'index')
	 * @param folderName the folder name
	 * @param category the category
	 * @param registerStaticController whether to register a static controller
	 */
	private void installView(JavaSymbolName viewName, String folderName, String title, String category, boolean registerStaticController) {
		// Probe if common web artifacts exist, and install them if needed
		PathResolver pathResolver = projectOperations.getPathResolver();
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/default.jspx"))) {
			installCommonViewArtefacts();
		}
		String lcViewName = viewName.getSymbolName().toLowerCase();
		propFileOperations.addPropertyIfNotExists(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", "label" + folderName.replace("/", "_") + "_" + lcViewName, title, true);
		menuOperations.addMenuItem(new JavaSymbolName(category), new JavaSymbolName(folderName.replace("/", "_") + lcViewName + "_id"), title, "global_generic", folderName + "/" + lcViewName, null);
		tilesOperations.addViewDefinition(folderName, folderName + "/" + lcViewName, TilesOperationsImpl.DEFAULT_TEMPLATE, "/WEB-INF/views" + folderName + "/" + lcViewName + ".jspx");

		String mvcConfig = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webmvc-config.xml");
		if (registerStaticController && fileManager.exists(mvcConfig)) {
			Document document = XmlUtils.readXml(fileManager.getInputStream(mvcConfig));

			if (null == XmlUtils.findFirstElement("/beans/view-controller[@path='" + folderName + "/" + lcViewName + "']", document.getDocumentElement())) {
				Element sibling = XmlUtils.findFirstElement("/beans/view-controller", document.getDocumentElement());
				Element view = new XmlElementBuilder("mvc:view-controller", document).addAttribute("path", folderName + "/" + lcViewName).build();
				if (sibling != null) {
					sibling.getParentNode().insertBefore(view, sibling);
				} else {
					document.getDocumentElement().appendChild(view);
				}
				fileManager.createOrUpdateTextFileIfRequired(mvcConfig, XmlUtils.nodeToString(document), false);
			}
		}
	}
	
	public void updateTags(boolean backup) {
		if (backup) {
			backupOperations.backup();
		}
		PathResolver pathResolver = projectOperations.getPathResolver();
		// Update tags
		copyDirectoryContents("tags/form/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/form"), true);
		copyDirectoryContents("tags/form/fields/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/form/fields"), true);
		copyDirectoryContents("tags/menu/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu"), true);
		copyDirectoryContents("tags/util/*.tagx", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/util"), true);
	}

	/**
	 * Creates a new Spring MVC controller.
	 * <p>
	 * Request mappings assigned by this method will always commence with "/" and end with "/**". You may present this prefix and/or this suffix if you wish, although it will automatically be added
	 * should it not be provided.
	 * 
	 * @param controller the controller class to create (required)
	 * @param preferredMapping the mapping this controller should adopt (optional; if unspecified it will be based on the controller name)
	 */
	public void createManualController(JavaType controller, String preferredMapping) {
		Assert.notNull(controller, "Controller Java Type required");

		String resourceIdentifier = typeLocationService.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);
		String folderName = null;

		// Create annotation @RequestMapping("/myobject/**")
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		if (preferredMapping == null || preferredMapping.length() == 0) {
			String typeName = controller.getSimpleTypeName();
			int dropFrom = typeName.lastIndexOf("Controller");
			if (dropFrom > -1) {
				typeName = typeName.substring(0, dropFrom);
			}
			folderName = typeName.toLowerCase();
			preferredMapping = "/" + folderName + "/**";
		}
		if (!preferredMapping.startsWith("/")) {
			folderName = preferredMapping;
			preferredMapping = "/" + preferredMapping;
		} else {
			folderName = preferredMapping.substring(1);
		}
		if (preferredMapping.endsWith("/")) {
			folderName = folderName.substring(0, folderName.length() - 1);
			preferredMapping = preferredMapping + "**";
		} else if (!preferredMapping.endsWith("/**")) {
			preferredMapping = preferredMapping + "/**";
		} else {
			folderName = folderName.replace("/**", "");
		}
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(controller, projectOperations.getPathResolver().getPath(resourceIdentifier));
		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		
		// Add HTTP get method
		methods.add(getHttpGetMethod(declaredByMetadataId));

		// Add HTTP post method
		methods.add(getHttpPostMethod(declaredByMetadataId));
		
		// Add index method
		methods.add(getIndexMethod(folderName, declaredByMetadataId));

		// Create Type definition
		List<AnnotationMetadataBuilder> typeAnnotations = new ArrayList<AnnotationMetadataBuilder>();

		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), preferredMapping));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		typeAnnotations.add(requestMapping);

		// Create annotation @Controller
		List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		AnnotationMetadataBuilder controllerAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.stereotype.Controller"), controllerAttributes);
		typeAnnotations.add(controllerAnnotation);
		
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, controller, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(typeAnnotations);
		typeDetailsBuilder.setDeclaredMethods(methods);
		typeManagementService.generateClassFile(typeDetailsBuilder.build());

		installView(folderName, "/index", new JavaSymbolName(controller.getSimpleTypeName()).getReadableSymbolName() + " View", "Controller", null, false);
	}

	private MethodMetadataBuilder getIndexMethod(String folderName, String declaredByMetadataId) {
		List<AnnotationMetadataBuilder> indexMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		indexMethodAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), new ArrayList<AnnotationAttributeValue<?>>()));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return \"" + folderName + "/index\";");
		MethodMetadataBuilder indexMethodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("index"), JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		indexMethodBuilder.setAnnotations(indexMethodAnnotations);
		return indexMethodBuilder;
	}

	private MethodMetadataBuilder getHttpPostMethod(String declaredByMetadataId) {
		List<AnnotationMetadataBuilder> postMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> postMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		postMethodAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		postMethodAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "{id}"));
		postMethodAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), postMethodAttributes));

		List<AnnotatedJavaType> postParamTypes = new ArrayList<AnnotatedJavaType>();
		List<AnnotationMetadata> idParamAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder idParamAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"));
		idParamAnnotations.add(idParamAnnotation.build());
		postParamTypes.add(new AnnotatedJavaType(new JavaType("java.lang.Long"), idParamAnnotations));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), null));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse"), null));
		
		List<JavaSymbolName> postParamNames = new ArrayList<JavaSymbolName>();
		postParamNames.add(new JavaSymbolName("id"));
		postParamNames.add(new JavaSymbolName("modelMap"));
		postParamNames.add(new JavaSymbolName("request"));
		postParamNames.add(new JavaSymbolName("response"));

		MethodMetadataBuilder postMethodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("post"), JavaType.VOID_PRIMITIVE, postParamTypes, postParamNames, new InvocableMemberBodyBuilder());
		postMethodBuilder.setAnnotations(postMethodAnnotations);
		return postMethodBuilder;
	}

	private MethodMetadataBuilder getHttpGetMethod(String declaredByMetadataId) {
		List<AnnotationMetadataBuilder> getMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> getMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		getMethodAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), getMethodAttributes));
		
		List<AnnotatedJavaType> getParamTypes = new ArrayList<AnnotatedJavaType>();
		getParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), null));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse"), null));
		
		List<JavaSymbolName> getParamNames = new ArrayList<JavaSymbolName>();
		getParamNames.add(new JavaSymbolName("modelMap"));
		getParamNames.add(new JavaSymbolName("request"));
		getParamNames.add(new JavaSymbolName("response"));

		MethodMetadataBuilder getMethodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("get"), JavaType.VOID_PRIMITIVE, getParamTypes, getParamNames, new InvocableMemberBodyBuilder());
		getMethodBuilder.setAnnotations(getMethodAnnotations);
		return getMethodBuilder;
	}

	/**
	 * Adds Tiles Maven dependencies and updates the MVC config to include Tiles view support
	 */
	private void updateConfiguration() {
		// Add tiles dependencies to pom
		Element configuration = XmlUtils.getConfiguration(getClass(), "tiles/configuration.xml");

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> springDependencies = XmlUtils.findElements("/configuration/tiles/dependencies/dependency", configuration);
		for (Element dependencyElement : springDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(dependencies);

		// Add config to MVC app context
		String mvcConfig = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		Document mvcConfigDocument = XmlUtils.readXml(fileManager.getInputStream(mvcConfig));
		Element beans = mvcConfigDocument.getDocumentElement();

		if (null != XmlUtils.findFirstElement("/beans/bean[@id = 'tilesViewResolver']", beans) || null != XmlUtils.findFirstElement("/beans/bean[@id = 'tilesConfigurer']", beans)) {
			return; // Tiles is already configured, nothing to do
		}
		Document configDoc = getDocumentTemplate("tiles/tiles-mvc-config-template.xml");
		Element configElement = configDoc.getDocumentElement();
		List<Element> tilesConfig = XmlUtils.findElements("/config/bean", configElement);
		for (Element bean : tilesConfig) {
			Node importedBean = mvcConfigDocument.importNode(bean, true);
			beans.appendChild(importedBean);
		}
		fileManager.createOrUpdateTextFileIfRequired(mvcConfig, XmlUtils.nodeToString(mvcConfigDocument), true);
	}

	public void installI18n(I18n i18n) {
		Assert.notNull(i18n, "Language choice required");

		if (i18n.getLocale() == null) {
			logger.warning("could not parse language choice");
			return;
		}

		String targetDirectory = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "");
		
		// Install message bundle
		String messageBundle = targetDirectory + "/WEB-INF/i18n/messages_" + i18n.getLocale().getLanguage() /*+ country*/ + ".properties";
		// Special case for english locale (default)
		if (i18n.getLocale().equals(Locale.ENGLISH)) {
			messageBundle = targetDirectory + "/WEB-INF/i18n/messages.properties";
		}
		if (!fileManager.exists(messageBundle)) {
			try {
				FileCopyUtils.copy(i18n.getMessageBundle(), fileManager.createFile(messageBundle).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Encountered an error during copying of message bundle MVC JSP addon.", e);
			}
		}

		// Install flag
		String flagGraphic = targetDirectory + "/images/" + i18n.getLocale().getLanguage() /*+ country*/ + ".png";
		if (!fileManager.exists(flagGraphic)) {
			try {
				FileCopyUtils.copy(i18n.getFlagGraphic(), fileManager.createFile(flagGraphic).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Encountered an error during copying of flag graphic for MVC JSP addon.", e);
			}
		}

		// Setup language definition in languages.jspx
		String footerFileLocation = targetDirectory + "/WEB-INF/views/footer.jspx";
		Document footer = XmlUtils.readXml(fileManager.getInputStream(footerFileLocation));

		if (null == XmlUtils.findFirstElement("//span[@id='language']/language[@locale='" + i18n.getLocale().getLanguage() + "']", footer.getDocumentElement())) {
			Element span = XmlUtils.findRequiredElement("//span[@id='language']", footer.getDocumentElement());
			span.appendChild(new XmlElementBuilder("util:language", footer).addAttribute("locale", i18n.getLocale().getLanguage()).addAttribute("label", i18n.getLanguage()).build());
			fileManager.createOrUpdateTextFileIfRequired(footerFileLocation, XmlUtils.nodeToString(footer), false);
		}
		
		// Record use of add-on (most languages are implemented via public add-ons)
		String bundleSymbolicName = BundleFindingUtils.findFirstBundleForTypeName(context.getBundleContext(), i18n.getClass().getName());
		if (bundleSymbolicName != null) {
			uaaRegistrationService.registerBundleSymbolicNameUse(bundleSymbolicName, null);
		}
	}

	private String cleanPath(String path) {
		if (path.equals("/")) {
			return "";
		}
		if (!path.startsWith("/")) {
			path = "/".concat(path);
		}
		if (path.contains(".")) {
			path = path.substring(0, path.indexOf(".") - 1);
		}
		return path.toLowerCase();
	}

	private String cleanViewName(String viewName) {
		if (viewName.startsWith("/")) {
			viewName = viewName.substring(1);
		}
		if (viewName.contains(".")) {
			viewName = viewName.substring(0, viewName.indexOf(".") - 1);
		}
		return viewName;
	}
}

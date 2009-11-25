package org.springframework.roo.addon.mvc.jsp;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.springframework.roo.addon.web.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopment
public class JspOperations {

	private FileManager fileManager;
	private MetadataService metadataService;
	private ClasspathOperations classpathOperations;
	private WebMvcOperations webMvcOperations;
	private PathResolver pathResolver;
	private MenuOperations menuOperations;

	public JspOperations(FileManager fileManager, MetadataService metadataService, ClasspathOperations classpathOperations, WebMvcOperations mvcOperations, PathResolver pathResolver, MenuOperations menuOperations) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(classpathOperations, "Classpath operations");
		Assert.notNull(mvcOperations, "Web MVC Operations required");
		Assert.notNull(pathResolver, "Pathresolver required");
		Assert.notNull(menuOperations, "Menu Operations required");

		this.fileManager = fileManager;
		this.metadataService = metadataService;			
		this.classpathOperations = classpathOperations;
		this.webMvcOperations = mvcOperations;
		this.pathResolver = pathResolver;
		this.menuOperations = menuOperations;
	}
	
	public boolean isControllerCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}

	public void installCommonViewArtefacts() {			
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Unable to obtain project metadata");

		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.notNull(projectMetadata, "Unable to obtain path resolver");	

		String imagesDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images");
		if (!fileManager.exists(imagesDirectory)) {
			fileManager.createDirectory(imagesDirectory);
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/banner-graphic.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/banner-graphic.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/springsource-logo.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/springsource-logo.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_first.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_first.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_next.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_next.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_previous.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_previous.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_last.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_last.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/gb.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/gb.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/de.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/de.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/sv.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/sv.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/es.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/es.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/it.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/it.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/list.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/list.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/add.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/add.png")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}

		String cssDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles");
		if (!fileManager.exists(cssDirectory)) {
			fileManager.createDirectory(cssDirectory);
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/roo-menu-left.css"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles/roo-menu-left.css")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/roo-menu-right.css"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles/roo-menu-right.css")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/left.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/classes/left.properties")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/right.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/classes/right.properties")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for view layer.", e);
			}
		}
		
		String layoutsDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts");
		if (!fileManager.exists(layoutsDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "layout/layouts.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/layouts.xml")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "layout/default.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/default.jspx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}		

		String jspDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views");
		if (!fileManager.exists(jspDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "dataAccessFailure.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/dataAccessFailure.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "resourceNotFound.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/resourceNotFound.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "uncaughtException.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/uncaughtException.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "index.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/index.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "layout/views.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/views.xml")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
		
		String tagsDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/tags");
		if (!fileManager.exists(tagsDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "tags/pagination.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/pagination.tagx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "tags/language.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/language.tagx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "tags/theme.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/theme.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
		
		String i18nDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n");
		if (!fileManager.exists(i18nDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());
				
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages_de.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());
				
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages_sv.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());
				
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages_es.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_es.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_es.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_es.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());
				
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages_it.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_it.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_it.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_it.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());

			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}	
	
	
	/**
	 * Creates a new Spring MVC controller.
	 * 
	 * <p>
	 * Request mappings assigned by this method will always commence with "/" and end with "/**".
	 * You may present this prefix and/or this suffix if you wish, although it will automatically be added
	 * should it not be provided.
	 * 
	 * @param controller the controller class to create (required)
	 * @param preferredMapping the mapping this controller should adopt (optional; if unspecified it will be based on the controller name)
	 */
	public void createManualController(JavaType controller, String preferredMapping) {
		Assert.notNull(controller, "Controller Java Type required");
		
		String resourceIdentifier = classpathOperations.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);
		String folderName = null;
		
		//create annotation @RequestMapping("/myobject/**")
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
		if (!preferredMapping.endsWith("/**")) {
			preferredMapping = preferredMapping + "/**";
		} else {
			folderName = folderName.replace("/**", "");
		}
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), preferredMapping));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		//create annotation @Controller
		List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		AnnotationMetadata controllerAnnotation = new DefaultAnnotationMetadata(new JavaType("org.springframework.stereotype.Controller"), controllerAttributes);

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(controller, pathResolver.getPath(resourceIdentifier));
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
		annotations.add(controllerAnnotation);
		
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();

		List<AnnotationMetadata> getMethodAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> getMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		getMethodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), getMethodAttributes));
		List<AnnotatedJavaType> getParamTypes = new ArrayList<AnnotatedJavaType>();
		getParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), null));
		getParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse"), null));
		List<JavaSymbolName> getParamNames = new ArrayList<JavaSymbolName>();
		getParamNames.add(new JavaSymbolName("modelMap"));
		getParamNames.add(new JavaSymbolName("request"));
		getParamNames.add(new JavaSymbolName("response"));
		MethodMetadata getMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("get"), JavaType.VOID_PRIMITIVE, getParamTypes, getParamNames, getMethodAnnotations, null, null);
		methods.add(getMethod);

		List<AnnotationMetadata> postMethodAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> postMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		postMethodAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		postMethodAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "{id}"));
		postMethodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), postMethodAttributes));
		List<AnnotatedJavaType> postParamTypes = new ArrayList<AnnotatedJavaType>();
		List<AnnotationMetadata> idParamAnnotations = new ArrayList<AnnotationMetadata>();
		idParamAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.PathVariable"), new ArrayList<AnnotationAttributeValue<?>>()));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("java.lang.Long"), idParamAnnotations));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), null));
		postParamTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse"), null));
		List<JavaSymbolName> postParamNames = new ArrayList<JavaSymbolName>();
		postParamNames.add(new JavaSymbolName("id"));
		postParamNames.add(new JavaSymbolName("modelMap"));
		postParamNames.add(new JavaSymbolName("request"));
		postParamNames.add(new JavaSymbolName("response"));
		MethodMetadata postMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("post"), JavaType.VOID_PRIMITIVE, postParamTypes, postParamNames, postMethodAnnotations, null, null);
		methods.add(postMethod);
		
		List<AnnotationMetadata> indexMethodAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> indexMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		indexMethodAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), indexMethodAttributes));
		List<AnnotatedJavaType> indexParamTypes = new ArrayList<AnnotatedJavaType>();
		MethodMetadata indexMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("index"), JavaType.STRING_OBJECT, indexParamTypes, postParamNames, indexMethodAnnotations, null, "return \"" + folderName + "/index\";");
		methods.add(indexMethod);

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, controller, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, methods, null, null, null, annotations, null);
		
		classpathOperations.generateClassFile(details);
		
		webMvcOperations.installMvcArtefacts();
		
		installCommonViewArtefacts();
		
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "controller-index.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + folderName + "/index.jspx")).getOutputStream());
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for controller class.", e);
		}
		
		setProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties"), "label." + folderName, new JavaSymbolName(controller.getSimpleTypeName()).getReadableSymbolName());
		
		menuOperations.addMenuItem("web_mvc_jsp_manual_controller_category", 
				new JavaSymbolName("Controller"), 
				"web_mvc_jsp_list_" + folderName + "_menu_item", 
				new JavaSymbolName(controller.getSimpleTypeName()), 
				"label." + folderName, 
				"/" + folderName + "/index");
		
		TilesOperations tilesOperations = new TilesOperations(folderName, fileManager, pathResolver, "config/webmvc-config.xml");
		tilesOperations.addViewDefinition(folderName + "/index", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + folderName + "/index.jspx");
		tilesOperations.writeToDiskIfNecessary();
	}
	
	private void setProperties(String fileIdentifier, String propKey, String value) {
		MutableFile mutableFile = fileManager.updateFile(fileIdentifier);
		Properties props = new Properties();
		try {
			props.load(mutableFile.getInputStream());
			props.setProperty(propKey, value);
			props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
		}	
	}
	
	private void changeProperties(String fileIdentifier, String propKey, String replace, String with) {
		MutableFile mutableFile = fileManager.updateFile(fileIdentifier);
		Properties props = new Properties();
		try {
			props.load(mutableFile.getInputStream());
			String welcome = (String) props.get(propKey);
			if(null != welcome && welcome.length() > 0) {
				props.setProperty(propKey, welcome.replace(replace, with));
				props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
			}
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
		}	
	}
}

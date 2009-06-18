package org.springframework.roo.addon.web.mvc.controller;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
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
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Controller configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class ControllerOperations {
	
	Logger logger = Logger.getLogger(ControllerOperations.class.getName());
		
	private PathResolver pathResolver;
	private FileManager fileManager;
	private MetadataService metadataService;
	private ClasspathOperations classpathOperations;
	private ProjectOperations projectOperations;
	private WebMvcOperations webMvcOperations;
	
	public ControllerOperations(PathResolver pathResolver, FileManager fileManager, MetadataService metadataService, ClasspathOperations classpathOperations, ProjectOperations projectOperations, WebMvcOperations webMvcOperations) {		
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(metadataService, "Metadata service required");		
		Assert.notNull(classpathOperations, "ClassPath operations required");	
		Assert.notNull(projectOperations, "Project operations required");	
		Assert.notNull(webMvcOperations, "Web XML operations required");
		this.pathResolver = pathResolver;
		this.fileManager = fileManager;
		this.metadataService = metadataService;		
		this.classpathOperations = classpathOperations;
		this.projectOperations = projectOperations;
		this.webMvcOperations = webMvcOperations;
	}
	
	public boolean isNewControllerAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}
	
	/**
	 * Creates a new Spring MVC controller which will be automatically scaffolded.
	 * 
	 * <p>
	 * Request mappings assigned by this method will always commence with "/" and end with "/**".
	 * You may present this prefix and/or this suffix if you wish, although it will automatically be added
	 * should it not be provided.
	 * 
	 * @param controller the controller class to create (required)
	 * @param entity the entity this controller should edit (required)
	 */
	public void createAutomaticController(JavaType controller, JavaType entity) {
		String ressourceIdentifier = classpathOperations.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);		
		
		//create annotation @RooWebScaffold(automaticallyMaintainView = true, formBackingObject = MyObject.class)
		List<AnnotationAttributeValue<?>> rooWebScaffoldAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		rooWebScaffoldAttributes.add(new BooleanAttributeValue(new JavaSymbolName("automaticallyMaintainView"), true));
		rooWebScaffoldAttributes.add(new ClassAttributeValue(new JavaSymbolName("formBackingObject"), entity));
		AnnotationMetadata rooWebScaffold = new DefaultAnnotationMetadata(new JavaType(RooWebScaffold.class.getName()), rooWebScaffoldAttributes);
		
		//create annotation @RequestMapping("/myobject/**")
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + entity.getSimpleTypeName().toLowerCase() + "/**"));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		//create annotation @Controller
		List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		AnnotationMetadata controllerAnnotation = new DefaultAnnotationMetadata(new JavaType("org.springframework.stereotype.Controller"), controllerAttributes);

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(controller, pathResolver.getPath(ressourceIdentifier));
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(rooWebScaffold);
		annotations.add(requestMapping);
		annotations.add(controllerAnnotation);
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, controller, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, annotations);

		classpathOperations.generateClassFile(details);
		
		createWebApplicationContext();
		
		webMvcOperations.createWebXml();
		webMvcOperations.createIndexJsp();
		webMvcOperations.copyUrlRewrite();
		webMvcOperations.updateJpaWebXml();
		
		updateDependencies();		
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
		String resourceIdentifier = classpathOperations.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);		
		
		//create annotation @RequestMapping("/myobject/**")
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		if (preferredMapping == null || preferredMapping.length() == 0) {
			String typeName = controller.getSimpleTypeName();
			int dropFrom = typeName.lastIndexOf("Controller");
			if (dropFrom > -1) {
				typeName = typeName.substring(0, dropFrom);
			}
			preferredMapping = "/" + typeName.toLowerCase() + "/**";
		}
		if (!preferredMapping.startsWith("/")) {
			preferredMapping = "/" + preferredMapping;
		}
		if (!preferredMapping.endsWith("/**")) {
			preferredMapping = preferredMapping + "/**";
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
		MethodMetadata getMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("get"), JavaType.VOID_PRIMITIVE, getParamTypes, getParamNames, getMethodAnnotations, null);
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
		MethodMetadata postMethod = new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("post"), JavaType.VOID_PRIMITIVE, postParamTypes, postParamNames, postMethodAnnotations, null);
		methods.add(postMethod);

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, controller, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, methods, null, null, null, annotations, null);

		classpathOperations.generateClassFile(details);
		
		createWebApplicationContext();
		
		webMvcOperations.createWebXml();
		webMvcOperations.createIndexJsp();
		webMvcOperations.copyUrlRewrite();
		webMvcOperations.updateJpaWebXml();
		
		updateDependencies();
	}

	public void createPropertyEditors(Set<JavaType> types) {
		for(JavaType type : types) {
			//do nothing if we are dealing with an enum
			if(isEnumType(type)) {
				continue;
			}
			JavaType newType = new JavaType(type.getFullyQualifiedTypeName() + "Editor");
			String ressourceIdentifier = classpathOperations.getPhysicalLocationCanonicalPath(newType, Path.SRC_MAIN_JAVA);		
			
			//create annotation @RooEditor(providePropertyEditorFor = MyObject.class)
			List<AnnotationAttributeValue<?>> editorAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			editorAttributes.add(new ClassAttributeValue(new JavaSymbolName("providePropertyEditorFor"), type));
			AnnotationMetadata editorAnnotation = new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.property.editor.RooEditor"), editorAttributes);
		
			String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(newType, pathResolver.getPath(ressourceIdentifier));
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(editorAnnotation);
			ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, newType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, annotations);

			classpathOperations.generateClassFile(details);
		}
	}
	
	private boolean isEnumType(JavaType type) {
		PhysicalTypeMetadata physicalTypeMetadata  = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifierNamingUtils.createIdentifier(PhysicalTypeIdentifier.class.getName(), type, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata != null) {
			PhysicalTypeDetails details = physicalTypeMetadata.getPhysicalTypeDetails();
			if (details != null) {
				if (details.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void createWebApplicationContext() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.isTrue(projectMetadata != null, "Project metadata required");
		
		// Verify the middle tier application context already exists
		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext.xml")), "Application context does not exist");
		
		String servletCtxFilename = "WEB-INF/" + projectMetadata.getProjectName() + "-servlet.xml";
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename))) {
			//this file already exists, nothing to do
			return;
		}
		
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "roo-servlet-template.xml");
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = (Element) pom.getFirstChild();
		XmlUtils.findFirstElementByName("context:component-scan", rootElement).setAttribute("base-package", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());
		
		MutableFile mutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename));
		XmlUtils.writeXml(mutableFile.getOutputStream(), pom);

		fileManager.scanAll();
	}
	
	private void updateDependencies() {	
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dependencies.xml");
		Assert.notNull(templateInputStream, "Could not acquire dependencies.xml file");
		Document dependencyDoc;
		try {
			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element dependenciesElement = (Element) dependencyDoc.getFirstChild();
		
		List<Element> springDependencies = XmlUtils.findElements("/dependencies/springWebMvc/dependency", dependenciesElement);
		for(Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
	}
}

package org.springframework.roo.addon.web.mvc.controller;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.ItdMetadataScanner;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

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
	private MetadataService metadataService;
	private ClasspathOperations classpathOperations;
	private WebMvcOperations webMvcOperations;
	private FileManager fileManager;
	private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	private ItdMetadataScanner itdMetadataScanner;
	private MetadataDependencyRegistry dependencyRegistry;
	
	public ControllerOperations(PathResolver pathResolver, MetadataService metadataService, ClasspathOperations classpathOperations, WebMvcOperations webMvcOperations, FileManager fileManager, PhysicalTypeMetadataProvider physicalTypeMetadataProvider, ItdMetadataScanner itdMetadataScanner, MetadataDependencyRegistry dependencyRegistry) {		
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");		
		Assert.notNull(classpathOperations, "ClassPath operations required");	
		Assert.notNull(webMvcOperations, "Web XML operations required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");
		Assert.notNull(itdMetadataScanner, "ITD metadata scanner required");
		Assert.notNull(dependencyRegistry, "Dependency registry required");
		
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;		
		this.classpathOperations = classpathOperations;
		this.webMvcOperations = webMvcOperations;
		this.fileManager = fileManager;
		this.physicalTypeMetadataProvider = physicalTypeMetadataProvider;
		this.itdMetadataScanner = itdMetadataScanner;
		this.dependencyRegistry = dependencyRegistry;
	}
	
	public void generateAll(JavaPackage javaPackage) {
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		each_file:
		for (FileDetails file : entries) {
			String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);
			String id = physicalTypeMetadataProvider.findIdentifier(javaType);
			if (id != null) {
				PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
				if (ptm == null || ptm.getPhysicalTypeDetails() == null || !(ptm.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
					continue;
				}
				
				ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) ptm.getPhysicalTypeDetails();
				if (Modifier.isAbstract(cid.getModifier())) {
					continue;
				}
				
				Set<MetadataItem> metadata = itdMetadataScanner.getMetadata(id);
				for (MetadataItem item : metadata) {
					if (item instanceof EntityMetadata) {
						EntityMetadata em = (EntityMetadata) item;
						Set<String> downstream = dependencyRegistry.getDownstream(em.getId());
						// check to see if this entity metadata has a web scaffold metadata listening to it
						for (String ds : downstream) {
							if (WebScaffoldMetadata.isValid(ds)) {
								// there is already a controller for this entity
								continue each_file;
							}
						}
						// to get here, there is no listening controller, so add on
						JavaType controller = new JavaType(javaPackage.getFullyQualifiedPackageName() + "." + javaType.getSimpleTypeName() + "Controller");
						JavaType entity = javaType;
						Set<String> disallowedOperations = new HashSet<String>();
						String path = entity.getSimpleTypeName().toLowerCase();
						String dateFormat = null;
						createAutomaticController(controller, entity, disallowedOperations, path, dateFormat);
						break;
					}
				}
				
			}
		}
		return;
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
	 * @param set of disallowed operations (required, but can be empty)
	 */
	public void createAutomaticController(JavaType controller, JavaType entity, Set<String> disallowedOperations, String path, String dateFormat) {		
		Assert.notNull(controller, "Controller Java Type required");
		Assert.notNull(entity, "Entity Java Type required");
		Assert.notNull(disallowedOperations, "Set of disallowed operations required");
		Assert.hasText(path, "Controller base path required");
		
		String ressourceIdentifier = classpathOperations.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);		
		
		//create annotation @RooWebScaffold(automaticallyMaintainView = true, formBackingObject = MyObject.class)
		List<AnnotationAttributeValue<?>> rooWebScaffoldAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		rooWebScaffoldAttributes.add(new StringAttributeValue(new JavaSymbolName("path"), path));
		rooWebScaffoldAttributes.add(new BooleanAttributeValue(new JavaSymbolName("automaticallyMaintainView"), true));
		rooWebScaffoldAttributes.add(new ClassAttributeValue(new JavaSymbolName("formBackingObject"), entity));
		if (null != dateFormat && dateFormat.length() > 0) {
			rooWebScaffoldAttributes.add(new StringAttributeValue(new JavaSymbolName("dateFormat"), dateFormat));
		}
		for(String operation: disallowedOperations) {
			rooWebScaffoldAttributes.add(new BooleanAttributeValue(new JavaSymbolName(operation), false));
		}
		AnnotationMetadata rooWebScaffold = new DefaultAnnotationMetadata(new JavaType(RooWebScaffold.class.getName()), rooWebScaffoldAttributes);
		
		//create annotation @RequestMapping("/myobject/**")
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + path + "/**"));
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
		
		webMvcOperations.installMvcArtefacts();
	}
}

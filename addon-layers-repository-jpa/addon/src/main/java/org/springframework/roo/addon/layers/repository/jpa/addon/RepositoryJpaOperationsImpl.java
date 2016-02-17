package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_READ_ONLY_REPOSITORY;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * The {@link RepositoryJpaOperations} implementation.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaOperationsImpl implements RepositoryJpaOperations {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(RepositoryJpaOperationsImpl.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private FileManager fileManager;
    private PathResolver pathResolver;
    private ProjectOperations projectOperations;
    private TypeManagementService typeManagementService;
    private TypeLocationService typeLocationService;
    private MemberDetailsScanner memberDetailsScanner;
    private MetadataService metadataService;
    
    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }
    
    @Override
    public boolean isRepositoryInstallationPossible() {
        return isInstalledInModule(getProjectOperations().getFocusedModuleName())
                && !getProjectOperations()
                        .isFeatureInstalledInFocusedModule(FeatureNames.MONGO);
    }

    @Override
    public void addRepository(final JavaType interfaceType,
            final JavaType domainType) {
        Validate.notNull(interfaceType, "ERROR: You must specify an interface repository type.");
        Validate.notNull(domainType, "ERROR: You must specify a valid Entity. ");

        // Check if new interface exists yet
        final String interfaceIdentifier = getPathResolver()
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);

        if (getFileManager().exists(interfaceIdentifier)) {
            // Type already exists - show error and print warning with
            // instructions.
            throw new RuntimeException(String.format(
                    "Class '%s' already exists and cannot be created. Try to use a different repository name on --class parameter.",
                    interfaceType));
        }
        
        // Check if entity provided type is annotated with @RooJpaEntity
        ClassOrInterfaceTypeDetails entityDetails = null;
        Set<ClassOrInterfaceTypeDetails> entities = getTypeLocationService()
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY);
        Iterator<ClassOrInterfaceTypeDetails> it = entities.iterator();
        while (it.hasNext()) {
            ClassOrInterfaceTypeDetails details = it.next();
            if (details.getName().equals((domainType))) {
                entityDetails = details;
                break;
            }
        }

        // Show an error indicating that entity should be annotated with @RooJpaEntity
        Validate.notNull(entityDetails,
                "ERROR: Provided entity should be annotated with @RooJpaEntity");
        
        // Check if current entity is defined as "readOnly".
        AnnotationAttributeValue<Boolean> readOnlyAttr = entityDetails
                .getAnnotation(ROO_JPA_ENTITY).getAttribute("readOnly");

        JavaType readOnlyRepository = null;
        boolean readOnly = readOnlyAttr != null && readOnlyAttr.getValue()
                ? true : false;
        
        // If is readOnly entity, generates common ReadOnlyRepository interface
        if (readOnly) {
            readOnlyRepository = generateReadOnlyRepository(
                    interfaceType.getPackage());
        }
        
        // TODO: By default, generate CustomRepository that allow developers to
        // include its dynamic queries using QueryDSL
        
        // Generates repository interface
        addRepositoryInterface(interfaceType, domainType, entityDetails, interfaceIdentifier, readOnly, readOnlyRepository);

    }
   
    /**
     * 
     * Method that generates the repository interface.
     * 
     * This method takes in mind if entity is defined as readOnly or not.
     * 
     * @param interfaceType
     * @param domainType
     * @param entityDetails
     * @param interfaceIdentifier
     * @param readOnly
     * @param readOnlyRepository
     */
    private void addRepositoryInterface(JavaType interfaceType,
            JavaType domainType, ClassOrInterfaceTypeDetails entityDetails,
            String interfaceIdentifier,
            boolean readOnly,
            JavaType readOnlyRepository) {
        // Generates @RooJpaRepository annotation with referenced entity value
        final AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_REPOSITORY_JPA);
        interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(
                new JavaSymbolName("entity"), domainType));
        // Generating interface
        final String interfaceMdId = PhysicalTypeIdentifier.createIdentifier(
                interfaceType, getPathResolver().getPath(interfaceIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                interfaceMdId, Modifier.PUBLIC, interfaceType,
                PhysicalTypeCategory.INTERFACE);

        // Check if is necessary to extends ReadOnlyRepository
        if (readOnly) {

            // Is necessary that ReadOnlyRepository interface exists
            Validate.notNull(readOnlyRepository,
                    "ERROR: Interface ReadOnlyRepository doesn't exists for this project");

            // Getting identifier field type of current entity
            JavaType idFieldType = getIdentifierFieldType(entityDetails);

            Validate.notNull(idFieldType,
                    String.format(
                            "ERROR: Entity %s doesn't have any field annotated with @Id.",
                            entityDetails.getType()));

            // Extends interface
            JavaType extendsType = new JavaType(
                    readOnlyRepository.getFullyQualifiedTypeName(), 0,
                    DataType.TYPE, null,
                    Arrays.asList(domainType, idFieldType));
            cidBuilder.addExtendsTypes(extendsType);

        }

        // Annotate repository interface
        cidBuilder.addAnnotation(interfaceAnnotationMetadata.build());

        // Save new repository on disk
        getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    }

    /**
     * Method that returns field annotated with @Id from given entity details
     * 
     * @param entityDetails
     * @return
     */
    private JavaType getIdentifierFieldType(
            ClassOrInterfaceTypeDetails entityDetails) {
        JavaType idAnnotation = new JavaType("javax.persistence.Id");
        
        // Getting entity member details
        final String physicalTypeIdentifier = entityDetails
                .getDeclaredByMetadataId();
        final PhysicalTypeMetadata targetTypeMetadata = (PhysicalTypeMetadata) getMetadataService()
                .get(physicalTypeIdentifier);
        Validate.notNull(
                targetTypeMetadata,
                "The specified target '--entity' does not exist or can not be found. Please create this type first.");
        final PhysicalTypeDetails targetPtd = targetTypeMetadata
                .getMemberHoldingTypeDetails();
        Validate.isInstanceOf(MemberHoldingTypeDetails.class, targetPtd);

        final ClassOrInterfaceTypeDetails targetTypeCid = (ClassOrInterfaceTypeDetails) targetPtd;
        final MemberDetails memberDetails = getMemberDetailsScanner()
                .getMemberDetails(this.getClass().getName(), targetTypeCid);
        
        List<FieldMetadata> entityFields = memberDetails.getFields();
        
        Validate.notEmpty(entityFields,
                String.format(
                        "ERROR: Entity %s doesn't have any field",
                        entityDetails.getType()));
        
        for(FieldMetadata field : entityFields){
            // Check if it's field with annotation @Id
            if(field.getAnnotation(idAnnotation) != null){
                return field.getFieldType();
            }
        }
        
        return null;
    }

    /**
     * Method that generates ReadOnlyRepository interface on current package. If
     * ReadOnlyRepository already exists in this or other package, will not be
     * generated.
     * 
     * @param repositoryPackage Package where ReadOnlyRepository should be
     *            generated
     * @return JavaType with existing or new ReadOnlyRepository
     */
    private JavaType generateReadOnlyRepository(JavaPackage repositoryPackage) {
        
        // First of all, check if already exists a @RooReadOnlyRepository
        // interface on current project
        Set<JavaType> readOnlyRepositories = getTypeLocationService()
                .findTypesWithAnnotation(ROO_READ_ONLY_REPOSITORY);

        if (!readOnlyRepositories.isEmpty()) {
            Iterator<JavaType> it = readOnlyRepositories.iterator();
            while(it.hasNext()){
                return it.next();
            }
        }
        
        final JavaType javaType = new JavaType(
                String.format("%s.ReadOnlyRepository", repositoryPackage));
        final String physicalPath = pathResolver
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, javaType);

        // Including ReadOnlyRepository interface
        InputStream inputStream = null;
        try {
            // Use defined template
            inputStream = FileUtils.getInputStream(getClass(), "ReadOnlyRepository-template._java");
            String input = IOUtils.toString(inputStream);
            // Replacing package
            input = input.replace("__PACKAGE__", repositoryPackage.getFullyQualifiedPackageName());

            // Creating ReadOnlyRepository interface
            fileManager.createOrUpdateTextFileIfRequired(physicalPath, input, false);
        } catch (final IOException e) {
            throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        
        return javaType;

    }

    public FileManager getFileManager(){
    	if(fileManager == null){
    		// Get all Services implement FileManager interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				fileManager = (FileManager) this.context.getService(ref);
    				return fileManager;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load FileManager on RepositoryJpaOperationsImpl.");
    			return null;
    		}
    	}else{
    		return fileManager;
    	}
    }
    
    public PathResolver getPathResolver(){
    	if(pathResolver == null){
    		// Get all Services implement PathResolver interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(PathResolver.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				pathResolver = (PathResolver) this.context.getService(ref);
    				return pathResolver;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PathResolver on RepositoryJpaOperationsImpl.");
    			return null;
    		}
    	}else{
    		return pathResolver;
    	}
    }
    
    public ProjectOperations getProjectOperations(){
    	if(projectOperations == null){
    		// Get all Services implement ProjectOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				projectOperations = (ProjectOperations) this.context.getService(ref);
    				return projectOperations;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load ProjectOperations on RepositoryJpaOperationsImpl.");
    			return null;
    		}
    	}else{
    		return projectOperations;
    	}
    }
    
    public TypeManagementService getTypeManagementService(){
    	if(typeManagementService == null){
    		// Get all Services implement TypeManagementService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				typeManagementService = (TypeManagementService) this.context.getService(ref);
    				return typeManagementService;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TypeManagementService on RepositoryJpaOperationsImpl.");
    			return null;
    		}
    	}else{
    		return typeManagementService;
    	}
    }
    
    public TypeLocationService getTypeLocationService(){
        if(typeLocationService == null){
            // Get all Services implement TypeLocationService interface
            try {
                ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);
                
                for(ServiceReference<?> ref : references){
                    typeLocationService = (TypeLocationService) this.context.getService(ref);
                    return typeLocationService;
                }
                
                return null;
                
            } catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load TypeLocationService on RepositoryJpaOperationsImpl.");
                return null;
            }
        }else{
            return typeLocationService;
        }
    }
    
    public MemberDetailsScanner getMemberDetailsScanner(){
        if(memberDetailsScanner == null){
            // Get all Services implement MemberDetailsScanner interface
            try {
                ServiceReference<?>[] references = this.context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);
                
                for(ServiceReference<?> ref : references){
                    memberDetailsScanner = (MemberDetailsScanner) this.context.getService(ref);
                    return memberDetailsScanner;
                }
                
                return null;
                
            } catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load MemberDetailsScanner on RepositoryJpaOperationsImpl.");
                return null;
            }
        }else{
            return memberDetailsScanner;
        }
    }
    
    public MetadataService getMetadataService(){
        if(metadataService == null){
            // Get all Services implement MetadataService interface
            try {
                ServiceReference<?>[] references = this.context.getAllServiceReferences(MetadataService.class.getName(), null);
                
                for(ServiceReference<?> ref : references){
                    metadataService = (MetadataService) this.context.getService(ref);
                    return metadataService;
                }
                
                return null;
                
            } catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load MetadataService on RepositoryJpaOperationsImpl.");
                return null;
            }
        }else{
            return metadataService;
        }
    }
    
    // Feature methods
    
    public String getName() {
        return FeatureNames.JPA;
    }

    public boolean isInstalledInModule(final String moduleName) {
        // Check if spring-boot-starter-data-jpa has been included
        Set<Dependency> dependencies = getProjectOperations()
                .getFocusedProjectMetadata().getPom().getDependencies();

        Dependency starter = new Dependency("org.springframework.boot",
                "spring-boot-starter-data-jpa", "");

        boolean hasStarter = dependencies.contains(starter);

        return getProjectOperations().isFocusedProjectAvailable() && hasStarter;
    }
}

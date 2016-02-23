package org.springframework.roo.addon.layers.service.addon;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE_IMPL;

import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;

/**
 * Class that implements {@link ServiceOperations}.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class ServiceOperationsImpl implements ServiceOperations {

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeManagementService typeManagementService;
    @Reference private TypeLocationService typeLocationService;

    @Override
    public boolean areServiceCommandsAvailable() {
        return projectOperations.isFocusedProjectAvailable();
    }
    
    @Override
    public void addAllServices(JavaPackage apiPackage,
            JavaPackage implPackage) {

        // Getting all generated entities
        Set<ClassOrInterfaceTypeDetails> entities = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY);
        for (final ClassOrInterfaceTypeDetails domainType : entities) {

            // Creating service interfaces for every entity
            JavaType interfaceType = new JavaType(String.format("%s.%sService",
                    apiPackage.getFullyQualifiedPackageName(),
                    domainType.getName().getSimpleTypeName()));
            
            // Creating service implementation for every entity
            JavaType implType = new JavaType(String.format("%s.%sServiceImpl",
                    implPackage.getFullyQualifiedPackageName(),
                    domainType.getName().getSimpleTypeName()));

            // Delegates on individual service creator
            addService(domainType.getType(), interfaceType, implType);
        }
    }

    @Override
    public void addService(final JavaType domainType,
            final JavaType interfaceType, final JavaType implType) {
        Validate.notNull(interfaceType,
                "ERROR: Interface type required to be able to generate service.");
        Validate.notNull(domainType,
                "ERROR: Domain type required to be able to generate service.");

        // Generating service interface
        createServiceInterface(domainType, interfaceType);

        // Generating service implementation
        createServiceImplementation(interfaceType, implType);
    }

    private void createServiceInterface(final JavaType domainType,
            final JavaType interfaceType) {

        // Checks if new service interface already exists.
        final String interfaceIdentifier = pathResolver
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);
        if (fileManager.exists(interfaceIdentifier)) {
            return; // Type already exists - nothing to do
        }

        // Validate that user provides a valid entity
        Validate.notNull(domainType,
                "ERROR: Domain type required to generate service");
        ClassOrInterfaceTypeDetails entityDetails = typeLocationService
                .getTypeDetails(domainType);
        Validate.notNull(
                entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
                "ERROR: Provided entity should be annotated with @RooJpaEntity");

        // Generating @RooService annotation
        final AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_SERVICE);
        interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(
                new JavaSymbolName("entity"), domainType));

        // Creating interface builder
        final String interfaceMid = PhysicalTypeIdentifier.createIdentifier(
                interfaceType, pathResolver.getPath(interfaceIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder interfaceTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                interfaceMid, PUBLIC, interfaceType, PhysicalTypeCategory.INTERFACE);
        // Adding @RooService annotation to current interface
        interfaceTypeBuilder.addAnnotation(interfaceAnnotationMetadata.build());

        // Write service interface on disk
        typeManagementService
                .createOrUpdateTypeOnDisk(interfaceTypeBuilder.build());
    }

    private void createServiceImplementation(final JavaType interfaceType,
            JavaType implType) {
        Validate.notNull(interfaceType,
                "ERROR: Interface should be provided to be able to generate its implementation");
        
        // Generating implementation JavaType if needed
        if(implType == null){
            implType = new JavaType(String.format("%sImpl",
                    interfaceType.getFullyQualifiedTypeName()));
        }
        
        // Checks if new service interface already exists.
        final String implIdentifier = pathResolver
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, implType);
        if (fileManager.exists(implIdentifier)) {
            return; // Type already exists - nothing to do
        }
        
        // Generating @RooServiceImpl annotation
        final AnnotationMetadataBuilder implAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_SERVICE_IMPL);
        implAnnotationMetadata.addAttribute(new ClassAttributeValue(
                new JavaSymbolName("service"), interfaceType));
        
        // Creating class builder
        final String implMid = PhysicalTypeIdentifier.createIdentifier(
                implType, pathResolver.getPath(implIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder implTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                implMid, PUBLIC, implType, PhysicalTypeCategory.CLASS);
        // Adding @RooService annotation to current interface
        implTypeBuilder.addAnnotation(implAnnotationMetadata.build());
        
        // Write service implementation on disk
        typeManagementService
                .createOrUpdateTypeOnDisk(implTypeBuilder.build());
    }

}

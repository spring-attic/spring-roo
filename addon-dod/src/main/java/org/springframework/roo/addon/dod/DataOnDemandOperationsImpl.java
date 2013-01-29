package org.springframework.roo.addon.dod;

import static org.springframework.roo.model.JpaJavaType.ENTITY;
import static org.springframework.roo.model.SpringJavaType.PERSISTENT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of {@link DataOnDemandOperations}.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
@Component
@Service
public class DataOnDemandOperationsImpl implements DataOnDemandOperations {

    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private MetadataService metadataService;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    /**
     * @param entity the entity to lookup required
     * @return the type details (never null; throws an exception if it cannot be
     *         obtained or parsed)
     */
    private ClassOrInterfaceTypeDetails getEntity(final JavaType entity) {
        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(entity);
        Validate.notNull(cid,
                "Java source code details unavailable for type '%s'", entity);
        return cid;
    }

    public boolean isDataOnDemandInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && projectOperations.isFeatureInstalledInFocusedModule(
                        FeatureNames.JPA, FeatureNames.MONGO);
    }

    public void newDod(final JavaType entity, final JavaType name) {
        Validate.notNull(entity,
                "Entity to produce a data on demand provider for is required");
        Validate.notNull(name,
                "Name of the new data on demand provider is required");

        final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA,
                projectOperations.getFocusedModuleName());
        Validate.notNull(path,
                "Location of the new data on demand provider is required");

        // Verify the requested entity actually exists as a class and is not
        // abstract
        final ClassOrInterfaceTypeDetails cid = getEntity(entity);
        Validate.isTrue(
                cid.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS,
                "Type %s is not a class", entity.getFullyQualifiedTypeName());
        Validate.isTrue(!Modifier.isAbstract(cid.getModifier()),
                "Type %s is abstract", entity.getFullyQualifiedTypeName());

        // Check if the requested entity is a JPA @Entity
        final MemberDetails memberDetails = memberDetailsScanner
                .getMemberDetails(DataOnDemandOperationsImpl.class.getName(),
                        cid);
        final AnnotationMetadata entityAnnotation = memberDetails
                .getAnnotation(ENTITY);
        final AnnotationMetadata persistentAnnotation = memberDetails
                .getAnnotation(PERSISTENT);
        Validate.isTrue(entityAnnotation != null
                || persistentAnnotation != null,
                "Type %s must be a persistent type",
                entity.getFullyQualifiedTypeName());

        // Everything is OK to proceed
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(name, path);

        if (metadataService.get(declaredByMetadataId) != null) {
            // The file already exists
            return;
        }

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final List<AnnotationAttributeValue<?>> dodConfig = new ArrayList<AnnotationAttributeValue<?>>();
        dodConfig.add(new ClassAttributeValue(new JavaSymbolName("entity"),
                entity));
        annotations.add(new AnnotationMetadataBuilder(
                RooJavaType.ROO_DATA_ON_DEMAND, dodConfig));

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, name,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(annotations);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }
}

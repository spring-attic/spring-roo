package org.springframework.roo.addon.dod.addon;

import static org.springframework.roo.model.JpaJavaType.ENTITY;
import static org.springframework.roo.model.SpringJavaType.PERSISTENT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
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

  @Reference
  private MemberDetailsScanner memberDetailsScanner;
  @Reference
  private MetadataService metadataService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private TypeManagementService typeManagementService;

  /**
   * Returns the {@link ClassOrInterfaceTypeDetails} for the provided entity.
   * 
   * @param entity
   *            the entity to lookup required
   * @return the ClassOrInterfaceTypeDetails type details (never null; throws
   *         an exception if it cannot be obtained or parsed)
   */
  private ClassOrInterfaceTypeDetails getEntityDetails(final JavaType entity) {
    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(entity);
    Validate.notNull(cid, "Java source code details unavailable for type '%s'", entity);
    return cid;
  }

  public boolean isDataOnDemandInstallationPossible() {
    return projectOperations.isFocusedProjectAvailable()
        && projectOperations.isFeatureInstalled(FeatureNames.JPA, FeatureNames.MONGO);
  }

  public void newDod(final JavaType entity, final JavaType name) {
    Validate.notNull(entity, "Entity to produce a data on demand provider for is required");
    Validate.notNull(name, "Name of the new data on demand provider is required");

    final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA, name.getModule());
    Validate.notNull(path, "Location of the new data on demand provider is required");

    // Verify the requested entity actually exists as a class and is not
    // abstract
    final ClassOrInterfaceTypeDetails cid = getEntityDetails(entity);
    Validate.isTrue(cid.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS,
        "Type %s is not a class", entity.getFullyQualifiedTypeName());
    Validate.isTrue(!Modifier.isAbstract(cid.getModifier()), "Type %s is abstract",
        entity.getFullyQualifiedTypeName());

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(DataOnDemandOperationsImpl.class.getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    final AnnotationMetadata persistentAnnotation = memberDetails.getAnnotation(PERSISTENT);
    Validate.isTrue(entityAnnotation != null || persistentAnnotation != null,
        "Type %s must be a persistent type", entity.getFullyQualifiedTypeName());

    // Everything is OK to proceed
    final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);

    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    // Create configuration class for DoD
    createDodConfigClassIfNotExists(path, name.getPackage());

    // Create entity factories for current entity and its related entities
    createEntityFactories(entity, path, name.getPackage());

    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    final List<AnnotationAttributeValue<?>> dodConfig =
        new ArrayList<AnnotationAttributeValue<?>>();
    dodConfig.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
    annotations.add(new AnnotationMetadataBuilder(RooJavaType.ROO_DATA_ON_DEMAND, dodConfig));

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Creates a DoD configuration class if it still doesn't exist in provided
   * {@link LogicalPath}. This class will be annotated with
   * `@RooDataOnDemandConfiguration`.
   * 
   * @param path
   *            the LogicalPath where test classes are going to be created.
   * @param the
   *            JavaPackage to use for creating the configuration class.
   */
  private void createDodConfigClassIfNotExists(LogicalPath path, JavaPackage javaPackage) {

    // Create the JavaType for the configuration class
    JavaType dodConfigurationClass =
        new JavaType(String.format("%s.DataOnDemandConfiguration",
            javaPackage.getFullyQualifiedPackageName()), path.getModule());

    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(dodConfigurationClass, path);
    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    // Create the CID builder
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
            dodConfigurationClass, PhysicalTypeCategory.CLASS);
    cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
        RooJavaType.ROO_DATA_ON_DEMAND_CONFIGURATION));

    // Write changes to disk
    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Creates a factory class for the given entity and its related entities,
   * These factories are used for creating transient instances to use in
   * tests.
   * 
   * @param the
   *            JavaType of provided entity.
   * @param path
   *            the LogicalPath where test classes are going to be created.
   * @param javaPackage
   *            the JavaPackage used to generathe the entity factory.
   */
  private void createEntityFactories(JavaType currentEntity, LogicalPath path,
      JavaPackage javaPackage) {

    // Get related entities
    List<JavaType> entities = getEntityAndRelatedEntitiesList(currentEntity);
    for (JavaType entity : entities) {

      // Create the JavaType for the configuration class
      JavaType factoryClass =
          new JavaType(String.format("%s.%sFactory", javaPackage.getFullyQualifiedPackageName(),
              entity.getSimpleTypeName()), entity.getModule());

      final String declaredByMetadataId =
          PhysicalTypeIdentifier.createIdentifier(factoryClass, path);
      if (metadataService.get(declaredByMetadataId) != null) {
        // The file already exists
        return;
      }

      // Create the CID builder
      ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
              factoryClass, PhysicalTypeCategory.CLASS);

      // Add @RooEntityFactory annotation
      AnnotationMetadataBuilder entityFactoryAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_ENTITY_FACTORY);
      entityFactoryAnnotation.addClassAttribute("entity", entity);
      cidBuilder.addAnnotation(entityFactoryAnnotation);

      // Write changes to disk
      typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }
  }

  /**
   * Searches the related entities of provided entity and returns a
   * {@link List} with all the related entities plus the provided entity.
   * 
   * @param entity
   *            the entity JavaType to search for its related entities.
   * @return a List with all the related entities.
   */
  private List<JavaType> getEntityAndRelatedEntitiesList(JavaType entity) {
    ClassOrInterfaceTypeDetails entityDetails = getEntityDetails(entity);
    JpaEntityMetadata entityMetadata =
        metadataService.get(JpaEntityMetadata.createIdentifier(entityDetails));
    List<JavaType> entitiesToCreateFactories = new ArrayList<JavaType>();
    entitiesToCreateFactories.add(entity);

    // Get related child entities
    for (RelationInfo info : entityMetadata.getRelationInfos().values()) {

      // Add to list
      if (!entitiesToCreateFactories.contains(info.childType)) {
        entitiesToCreateFactories.add(info.childType);
      }
    }

    // We need as well to get related parent entities
    for (FieldMetadata parentEntityField : entityMetadata.getRelationsAsChild().values()) {
      JavaType parentEntity = null;
      if (parentEntityField.getFieldType().isCommonCollectionType()) {

        // Get wrappedType
        parentEntity = parentEntityField.getFieldType().getBaseType();
      } else {
        parentEntity = parentEntityField.getFieldType();
      }

      // Add parent entity to list
      if (!entitiesToCreateFactories.contains(parentEntity)) {
        entitiesToCreateFactories.add(parentEntity);
      }
    }

    return entitiesToCreateFactories;
  }

}

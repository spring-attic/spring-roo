package org.springframework.roo.addon.jpa.addon.dod;

import static org.springframework.roo.model.JpaJavaType.ENTITY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.test.addon.providers.DataOnDemandCreatorProvider;
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
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of {@link DataOnDemandOperations}, based on old 
 * DataOnDemandOperationsImpl.
 *
 * @author Alan Stewart
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JpaDataOnDemandCreator implements DataOnDemandCreatorProvider {

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

  @Override
  public boolean isValid(JavaType javaType) {
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);
    if (cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
      return true;
    }
    return false;
  }

  @Override
  public void createDataOnDemand(JavaType entity) {
    Validate.notNull(entity, "Entity to produce a data on demand provider for is required");

    // Create the JavaType for DoD class
    JavaType name =
        new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand", entity.getModule());

    // Obatain test path for the module of the new class
    final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA, name.getModule());
    Validate.notNull(path, "Location of the new data on demand provider is required");

    // Create DoD configuration class
    addDataOnDemandConfigurationClass(path, name.getPackage());

    // Create entity factories for the given entity and its related entities
    newEntityFactory(entity, path, name.getPackage());

    // Create data on demand class
    newDataOnDemandClass(entity, name);
  }

  /**
   * Creates a DoD configuration class if it still doesn't exist in provided
   * {@link LogicalPath} and with the provided {@link JavaPackage}. This class 
   * will be in carry of injecting every DataOnDemand class into the Spring 
   * context when required.
   * 
   * @param path the {@link LogicalPath} where test classes are going to be created.
   * @param the {@link JavaPackage} to use for creating the configuration class.
   */
  public void addDataOnDemandConfigurationClass(LogicalPath path, JavaPackage javaPackage) {

    // Create the JavaType for the configuration class
    JavaType dodConfigurationClass =
        new JavaType(String.format("%s.JpaDataOnDemandConfiguration",
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
        RooJavaType.ROO_JPA_DATA_ON_DEMAND_CONFIGURATION));

    // Write changes to disk
    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Creates a new data-on-demand provider for an entity. Silently returns 
   * if the DoD class already exists.
   * 
   * @param entity to produce a DoD provider for
   * @param name the name of the new DoD class
   */
  public void newDataOnDemandClass(JavaType entity, JavaType name) {
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
        memberDetailsScanner.getMemberDetails(JpaDataOnDemandCreator.class.getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    Validate.isTrue(entityAnnotation != null, "Type %s must be a JPA entity type",
        entity.getFullyQualifiedTypeName());

    // Everything is OK to proceed
    final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);

    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    final List<AnnotationAttributeValue<?>> dodConfig =
        new ArrayList<AnnotationAttributeValue<?>>();
    dodConfig.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
    annotations.add(new AnnotationMetadataBuilder(RooJavaType.ROO_JPA_DATA_ON_DEMAND, dodConfig));

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Creates a new entity factory class for the entity and its related entities 
   * (one factory for each entity). These factories are used for creating 
   * transient instances to use in tests. Silently returns if the entity 
   * factory class already exists.
   * 
   * @param entity the {@link JavaType} to produce a factory for.
   * @param path the {@link LogicalPath} where test classes are going to be created.
   * @param javaPackage the {@link JavaPackage} used to generate the entity factory.
   */
  public void newEntityFactory(JavaType currentEntity, LogicalPath path, JavaPackage javaPackage) {

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
        continue;
      }

      // Create the CID builder
      ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
              factoryClass, PhysicalTypeCategory.CLASS);

      // Add @RooEntityFactory annotation
      AnnotationMetadataBuilder entityFactoryAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_JPA_ENTITY_FACTORY);
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

    return entitiesToCreateFactories;
  }

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

}

package org.springframework.roo.addon.jpa.addon.dod;

import static org.springframework.roo.model.JpaJavaType.ENTITY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.test.providers.DataOnDemandCreatorProvider;
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
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

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

  private static final Dependency VALIDATION_API_DEPENDENCY = new Dependency("javax.validation",
      "validation-api", null);
  private static final Dependency SPRING_BOOT_TEST_DEPENDENCY = new Dependency(
      "org.springframework.boot", "spring-boot-test", null, DependencyType.JAR,
      DependencyScope.TEST);

  private static final String MAVEN_JAR_PLUGIN = "maven-jar-plugin";

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
  public JavaType createDataOnDemand(JavaType entity) {
    Validate.notNull(entity, "Entity to produce a data on demand provider for is required");

    JavaType dodClass = getDataOnDemand(entity);
    if (dodClass != null) {
      return dodClass;
    }

    // Add plugin to generate test jar
    addMavenJarPlugin(entity.getModule());

    // Create the JavaType for DoD class
    JavaType name =
        new JavaType(entity.getPackage().getFullyQualifiedPackageName().concat(".dod.")
            .concat(entity.getSimpleTypeName()).concat("DataOnDemand"), entity.getModule());

    // Obatain test path for the module of the new class
    final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA, name.getModule());
    Validate.notNull(path, "Location of the new data on demand provider is required");

    // Create DoD configuration class
    createDataOnDemandConfiguration(entity.getModule());

    // Create entity factories for the given entity and its related entities
    createEntityFactory(entity);

    // Create data on demand class
    return newDataOnDemandClass(entity, name);
  }

  @Override
  public JavaType createDataOnDemandConfiguration(String moduleName) {

    // Check if alreafy exists
    JavaType dodConfig = getDataOnDemandConfiguration();
    if (dodConfig != null) {
      return dodConfig;
    }

    // Add spring-boot-test dependency with test scope
    projectOperations.addDependency(moduleName, SPRING_BOOT_TEST_DEPENDENCY);

    // Get Pom
    final Pom module = projectOperations.getPomFromModuleName(moduleName);

    // Get test Path for module
    final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA, moduleName);

    // Create the JavaType for the configuration class
    JavaType dodConfigurationClass =
        new JavaType(String.format("%s.dod.DataOnDemandConfiguration",
            typeLocationService.getTopLevelPackageForModule(module), moduleName));

    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(dodConfigurationClass, path);
    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId).getName();
    }

    // Create the CID builder
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
            dodConfigurationClass, PhysicalTypeCategory.CLASS);
    cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
        RooJavaType.ROO_JPA_DATA_ON_DEMAND_CONFIGURATION));

    // Write changes to disk
    final ClassOrInterfaceTypeDetails configDodCid = cidBuilder.build();
    typeManagementService.createOrUpdateTypeOnDisk(configDodCid);

    return configDodCid.getName();
  }

  @Override
  public JavaType createEntityFactory(JavaType currentEntity) {
    Validate.notNull(currentEntity, "Entity to produce a data on demand provider for is required");

    // Verify the requested entity actually exists as a class and is not
    // abstract
    final ClassOrInterfaceTypeDetails cid = getEntityDetails(currentEntity);
    Validate.isTrue(cid.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS,
        "Type %s is not a class", currentEntity.getFullyQualifiedTypeName());
    Validate.isTrue(!Modifier.isAbstract(cid.getModifier()), "Type %s is abstract",
        currentEntity.getFullyQualifiedTypeName());

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(JpaDataOnDemandCreator.class.getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    Validate.isTrue(entityAnnotation != null, "Type %s must be a JPA entity type",
        currentEntity.getFullyQualifiedTypeName());

    // Get related entities
    List<JavaType> entities = getEntityAndRelatedEntitiesList(currentEntity);

    // Get test Path for module
    final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA, currentEntity.getModule());

    JavaType currentEntityFactory = null;
    for (JavaType entity : entities) {

      // Create the JavaType for the configuration class
      JavaType factoryClass =
          new JavaType(String.format("%s.dod.%sFactory", entity.getPackage()
              .getFullyQualifiedPackageName(), entity.getSimpleTypeName()), entity.getModule());

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

      // First entity is current entity
      if (currentEntityFactory == null) {
        currentEntityFactory = cidBuilder.getName();
      }
    }

    return currentEntityFactory;
  }

  @Override
  public JavaType getDataOnDemand(JavaType entity) {
    Set<ClassOrInterfaceTypeDetails> dataOnDemandCids =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_DATA_ON_DEMAND);
    JavaType typeToReturn = null;
    for (ClassOrInterfaceTypeDetails cid : dataOnDemandCids) {
      if (entity.equals((JavaType) cid.getAnnotation(RooJavaType.ROO_JPA_DATA_ON_DEMAND)
          .getAttribute("entity").getValue())) {
        typeToReturn = cid.getName();
        break;
      }
    }
    return typeToReturn;
  }

  @Override
  public JavaType getDataOnDemandConfiguration() {
    Set<JavaType> dodConfigurationTypes =
        typeLocationService
            .findTypesWithAnnotation(RooJavaType.ROO_JPA_DATA_ON_DEMAND_CONFIGURATION);
    if (!dodConfigurationTypes.isEmpty()) {
      return dodConfigurationTypes.iterator().next();
    }
    return null;
  }

  @Override
  public JavaType getDataOnDemandConfiguration(String moduleName) {
    Set<JavaType> dodConfigurationTypes =
        typeLocationService
            .findTypesWithAnnotation(RooJavaType.ROO_JPA_DATA_ON_DEMAND_CONFIGURATION);
    Iterator<JavaType> it = dodConfigurationTypes.iterator();
    while (it.hasNext()) {
      JavaType dodConfigType = it.next();
      if (dodConfigType.getModule().equals(moduleName)) {
        return dodConfigType;
      }
    }
    return null;
  }

  @Override
  public JavaType getEntityFactory(JavaType entity) {
    Set<ClassOrInterfaceTypeDetails> dataOnDemandCids =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY_FACTORY);
    JavaType typeToReturn = null;
    for (ClassOrInterfaceTypeDetails cid : dataOnDemandCids) {
      if (entity.equals((JavaType) cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY_FACTORY)
          .getAttribute("entity").getValue())) {
        typeToReturn = cid.getName();
        break;
      }
    }
    return typeToReturn;
  }

  /**
   * Add maven-jar-plugin to provided module.
   * 
   * @param moduleName the name of the module.
   */
  private void addMavenJarPlugin(String moduleName) {

    // Add plugin maven-jar-plugin
    Pom module = projectOperations.getPomFromModuleName(moduleName);
    // Stop if the plugin is already installed
    for (final Plugin plugin : module.getBuildPlugins()) {
      if (plugin.getArtifactId().equals(MAVEN_JAR_PLUGIN)) {
        return;
      }
    }

    final Element configuration = XmlUtils.getConfiguration(getClass());
    final Element plugin = XmlUtils.findFirstElement("/configuration/plugin", configuration);

    // Now install the plugin itself
    if (plugin != null) {
      projectOperations.addBuildPlugin(moduleName, new Plugin(plugin), false);
    }
  }

  /**
   * Creates a new data-on-demand provider for an entity. Silently returns 
   * if the DoD class already exists.
   * 
   * @param entity to produce a DoD provider for
   * @param name the name of the new DoD class
   */
  private JavaType newDataOnDemandClass(JavaType entity, JavaType name) {
    Validate.notNull(entity, "Entity to produce a data on demand provider for is required");
    Validate.notNull(name, "Name of the new data on demand provider is required");

    final LogicalPath path = LogicalPath.getInstance(Path.SRC_TEST_JAVA, name.getModule());
    Validate.notNull(path, "Location of the new data on demand provider is required");

    // Add javax validation dependency
    projectOperations.addDependency(name.getModule(), VALIDATION_API_DEPENDENCY);

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
      return new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId).getName();
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

    // Write changes on disk
    final ClassOrInterfaceTypeDetails dodClassCid = cidBuilder.build();
    typeManagementService.createOrUpdateTypeOnDisk(dodClassCid);

    return cid.getName();
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

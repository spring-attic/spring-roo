package org.springframework.roo.addon.web.mvc.controller.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.test.providers.DataOnDemandCreatorProvider;
import org.springframework.roo.addon.test.providers.TestCreatorProvider;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides convenience methods that can be used to create mock tests 
 * for JPA Repositories.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JsonControllerTestCreator implements TestCreatorProvider {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JsonControllerTestCreator.class);

  private final static Dependency SPRINGLETS_BOOT_STARTER_TEST_DEPENDENCY = new Dependency(
      "io.springlets", "springlets-boot-starter-test", "${springlets.version}", DependencyType.JAR,
      DependencyScope.TEST);
  private final static Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.2.0.RC1");

  private BundleContext context;

  @Reference
  private MetadataService metadataService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private TypeLocationService typeLocationService;

  //DataOnDemandCreatorProvider implementations
  private List<DataOnDemandCreatorProvider> dodCreators =
      new ArrayList<DataOnDemandCreatorProvider>();

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
  }

  @Override
  public List<JavaType> getValidTypes() {
    List<JavaType> validTypes = new ArrayList<JavaType>();
    validTypes.add(RooJavaType.ROO_JSON);
    return validTypes;
  }

  @Override
  public boolean isValid(JavaType javaType) {
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);
    if (cid.getAnnotation(RooJavaType.ROO_JSON) != null) {
      return true;
    }
    return false;
  }

  @Override
  public boolean isUnitTestCreationAvailable() {
    return false;
  }

  @Override
  public void createUnitTest(final JavaType projectType) {
    throw new IllegalArgumentException("Unit test operations are not "
        + "available for JSON controllers.");
  }

  @Override
  public boolean isIntegrationTestCreationAvailable() {
    Set<JavaType> jsonControllers =
        typeLocationService.findTypesWithAnnotation(RooJavaType.ROO_JSON);
    return projectOperations.isFocusedProjectAvailable()
        && projectOperations.isFeatureInstalled(FeatureNames.MVC) && !jsonControllers.isEmpty();
  }

  @Override
  public void createIntegrationTest(JavaType type, Pom module) {
    Validate.notNull(type, "Class to produce an integration test class for is required");

    // Check if provided JavaType is a JSON Controller
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(type);
    Validate.notNull(cid.getAnnotation(RooJavaType.ROO_CONTROLLER),
        "Type must be a Roo controller.");
    Validate
        .notNull(cid.getAnnotation(RooJavaType.ROO_JSON), "Type must be a Roo JSON controller.");

    // Add springlets-boot-starter-test dependency
    projectOperations.addProperty("", SPRINGLETS_VERSION_PROPERTY);
    projectOperations
        .addDependency(module.getModuleName(), SPRINGLETS_BOOT_STARTER_TEST_DEPENDENCY);

    // Get the controller managed entity
    ControllerAnnotationValues controllerAnnotationValues = new ControllerAnnotationValues(cid);
    JavaType managedEntity = controllerAnnotationValues.getEntity();

    // Workaround to get a JavaType with not null module when recovering it 
    // from a ClassAttributeValue
    managedEntity =
        new JavaType(managedEntity.getFullyQualifiedTypeName(), managedEntity.getArray(),
            managedEntity.getDataType(), managedEntity.getArgName(), managedEntity.getParameters(),
            typeLocationService.getTypeDetails(managedEntity).getType().getModule());

    // Create Data On Demand artifacts for managed entity
    List<DataOnDemandCreatorProvider> dodCreators =
        getValidDataOnDemandCreatorsForType(managedEntity);
    Validate.isTrue(!dodCreators.isEmpty(),
        "Couldn't find any 'DataOnDemandCreatorProvider' for JSON controllers.");
    Validate
        .isTrue(
            dodCreators.size() == 1,
            "More than 1 valid 'DataOnDemandCreatorProvider' found for JSON controllers. %s can't decide which one to use.",
            this.getClass().getName());
    DataOnDemandCreatorProvider creator = dodCreators.get(0);
    creator.createDataOnDemand(managedEntity);

    // Add module dependency with test-jar dependency
    if (projectOperations.isMultimoduleProject()) {
      String managedEntityModuleName = managedEntity.getModule();
      Pom managedEntityModule = projectOperations.getPomFromModuleName(managedEntityModuleName);
      projectOperations.addDependency(module.getModuleName(),
          new Dependency(managedEntityModule.getGroupId(), managedEntityModule.getArtifactId(),
              "${project.version}", DependencyType.valueOfTypeCode("test-jar"),
              DependencyScope.TEST), true, true);
    }

    // Create integration test class
    final JavaType name = new JavaType(type + "IT", module.getModuleName());
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            Path.SRC_TEST_JAVA.getModulePathId(module.getModuleName()));
    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    // Add @RooJsonControllerIntegrationTest to source file
    AnnotationMetadataBuilder rooIntegrationTestAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_JSON_CONTROLLER_INTEGRATION_TEST);
    rooIntegrationTestAnnotation.addClassAttribute("targetClass", type);

    // Create integration test class
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.addAnnotation(rooIntegrationTestAnnotation);

    // Write changes to disk
    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Gets all the valid implementations of DataOnDemandCreatorProvider for a JavaType.
   *
   * @param type the JavaType to get the valid implementations.
   * @return a `List` with the {@link DataOnDemandCreatorProvider} valid 
   *            implementations. Never `null`.
   */
  private List<DataOnDemandCreatorProvider> getValidDataOnDemandCreatorsForType(JavaType type) {

    // Get all Services implement DataOnDemandCreatorProvider interface
    if (this.dodCreators.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(DataOnDemandCreatorProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          DataOnDemandCreatorProvider dodCreatorProvider =
              (DataOnDemandCreatorProvider) this.context.getService(ref);
          this.dodCreators.add(dodCreatorProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load DataOnDemandCreatorProvider on JsonControllerTestCreator.");
        return null;
      }
    }

    List<DataOnDemandCreatorProvider> validDoDCreators =
        new ArrayList<DataOnDemandCreatorProvider>();
    for (DataOnDemandCreatorProvider provider : this.dodCreators) {
      if (provider.isValid(type)) {
        validDoDCreators.add(provider);
      }
    }

    return validDoDCreators;
  }

}

package org.springframework.roo.addon.jpa.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides convenience methods that can be used to create mock tests 
 * for JPA Entities.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JpaTestCreator implements TestCreatorProvider {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JpaTestCreator.class);

  private BundleContext context;

  @Reference
  private MemberDetailsScanner memberDetailsScanner;
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
    validTypes.add(RooJavaType.ROO_JPA_ENTITY);
    return validTypes;
  }

  @Override
  public boolean isValid(JavaType javaType) {
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);
    if (cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
      return true;
    }
    return false;
  }

  @Override
  public boolean isUnitTestCreationAvailable() {
    return projectOperations.isFocusedProjectAvailable()
        && projectOperations.isFeatureInstalled(FeatureNames.JPA);
  }

  @Override
  public void createUnitTest(final JavaType entity) {
    Validate.notNull(entity, "Class to produce an unit test class for is required");

    // Check if provided JavaType is a Repository
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(entity);
    Validate.notNull(cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
        "Type must be a Roo JPA Entity type.");

    // Create JPA DataOnDemand artifacts
    List<DataOnDemandCreatorProvider> dodCreators = getValidDataOnDemandCreatorsForType(entity);
    for (DataOnDemandCreatorProvider dodCreator : dodCreators) {
      dodCreator.createDataOnDemand(entity);
    }

    final JavaType name = new JavaType(entity + "Test", entity.getModule());
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            Path.SRC_TEST_JAVA.getModulePathId(entity.getModule()));

    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    // Add @RooUnitTest to source file
    AnnotationMetadataBuilder rooUnitTestAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_JPA_UNIT_TEST);
    rooUnitTestAnnotation.addClassAttribute("targetClass", entity);

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.addAnnotation(rooUnitTestAnnotation);

    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  @Override
  public boolean isIntegrationTestCreationAvailable() {
    return false;
  }

  @Override
  public void createIntegrationTest(JavaType projectType, Pom module) {
    throw new IllegalArgumentException("Integration test operations are not "
        + "available for JPA entities.");
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
        LOGGER.warning("Cannot load DataOnDemandCreatorProvider on TestOperationsImpl.");
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

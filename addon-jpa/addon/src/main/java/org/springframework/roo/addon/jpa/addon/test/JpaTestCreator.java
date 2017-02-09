package org.springframework.roo.addon.jpa.addon.test;

import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.test.addon.providers.TestCreatorProvider;
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
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
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

  private static final Dependency JUNIT_DEPENDENCY = new Dependency("junit", "junit", null,
      DependencyType.JAR, DependencyScope.TEST);
  private static final Dependency ASSERTJ_CORE_DEPENDENCY = new Dependency("org.assertj",
      "assertj-core", null, DependencyType.JAR, DependencyScope.TEST);
  private static final Dependency MOCKITO_CORE_DEPENDENCY = new Dependency("org.mockito",
      "mockito-core", null, DependencyType.JAR, DependencyScope.TEST);
  private static final Dependency SPRING_TEST_DEPENDENCY = new Dependency("org.springframework",
      "spring-test", null, DependencyType.JAR, DependencyScope.TEST);
  private static final Plugin MAVEN_SUREFIRE_PLUGIN = new Plugin("org.apache.maven.plugins",
      "maven-surefire-plugin", null);

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
  public void createUnitTest(final JavaType projectType) {
    Validate.notNull(projectType, "Class to produce a unit test class for is required");

    final JavaType name = new JavaType(projectType + "Test", projectType.getModule());
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            Path.SRC_TEST_JAVA.getModulePathId(projectType.getModule()));

    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    // Add dependencies if needed
    projectOperations.addDependency(projectType.getModule(), JUNIT_DEPENDENCY);
    projectOperations.addDependency(projectType.getModule(), ASSERTJ_CORE_DEPENDENCY);
    projectOperations.addDependency(projectType.getModule(), MOCKITO_CORE_DEPENDENCY);
    projectOperations.addDependency(projectType.getModule(), SPRING_TEST_DEPENDENCY);

    // Add plugins if needed
    projectOperations.addBuildPlugin(projectType.getModule(), MAVEN_SUREFIRE_PLUGIN);

    // Add @RooUnitTest to source file
    AnnotationMetadataBuilder rooUnitTestAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_JPA_UNIT_TEST);
    rooUnitTestAnnotation.addClassAttribute("targetClass", projectType);

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.addAnnotation(rooUnitTestAnnotation);

    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  @Override
  public boolean isIntegrationTestCreationAvailable() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void createIntegrationTest(JavaType projectType) {
    // TODO Auto-generated method stub

  }

}

package org.springframework.roo.addon.test.addon.integration;

import static org.springframework.roo.model.SpringJavaType.MOCK_STATIC_ENTITY_METHODS;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Provides convenience methods that can be used to create mock tests.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class IntegrationTestOperationsImpl implements IntegrationTestOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(IntegrationTestOperationsImpl.class);

  private static final JavaType JUNIT_4 = new JavaType("org.junit.runners.JUnit4");
  private static final JavaType RUN_WITH = new JavaType("org.junit.runner.RunWith");
  private static final JavaType TEST = new JavaType("org.junit.Test");

  // Dependencies
  private static final Dependency DEPENDENCY_SPRING_BOOT_STARTER_TEST = new Dependency(
      "org.springframework.boot", "spring-boot-starter-test", null, DependencyType.JAR,
      DependencyScope.TEST);

  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
    this.serviceInstaceManager.activate(this.context);
  }

  public boolean isIntegrationTestInstallationPossible() {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled(FeatureNames.JPA, FeatureNames.MONGO);
  }

  public void newIntegrationTest(JavaType klass, Pom module) {
    //    Validate
    //        .isTrue(
    //            getTypeLocationService().hasModuleFeature(module, ModuleFeatureName.APPLICATION),
    //            "ERROR: You are trying to generate an integration test inside module that doesn't match with APPLICATION modules features.");
    //
    //    Validate.notNull(klass, "Repository to produce an integration test for is required");
    //
    //    // Verify the requested entity actually exists as a class and is not
    //    // abstract
    //    final ClassOrInterfaceTypeDetails cidRepository =
    //        getTypeLocationService().getTypeDetails(klass);
    //    Validate.notNull(cidRepository, "Java source code details unavailable for type %s",
    //        cidRepository);
    //    Validate.isTrue(!Modifier.isAbstract(cidRepository.getModifier()), "Type %s is abstract",
    //        klass.getFullyQualifiedTypeName());
    //
    //    // Get entity related
    //    AnnotationMetadata annotationRepository =
    //        cidRepository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
    //    AnnotationAttributeValue<Object> entityAttribute = annotationRepository.getAttribute("entity");
    //    JavaType entity = (JavaType) entityAttribute.getValue();
    //
    //    JavaPackage topLevelPackage = getProjectOperations().getTopLevelPackage(module.getModuleName());
    //    String dodPath = topLevelPackage.getFullyQualifiedPackageName().concat(".dod.");
    //    getDataOnDemandOperations().newDod(
    //        entity,
    //        new JavaType(dodPath.concat(entity.getSimpleTypeName()).concat("DataOnDemand"), module
    //            .getModuleName()));
    //
    //    final JavaType name = new JavaType(klass + "IT");
    //    final String declaredByMetadataId =
    //        PhysicalTypeIdentifier.createIdentifier(name,
    //            Path.SRC_TEST_JAVA.getModulePathId(module.getModuleName()));
    //
    //    if (getMetadataService().get(declaredByMetadataId) != null) {
    //
    //      // The file already exists
    //      LOGGER
    //          .log(
    //              Level.SEVERE,
    //              String
    //                  .format("The class selected already has defined an integration test. Please, select another"));
    //      return;
    //    }
    //
    //    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    //    final List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
    //    config.add(new ClassAttributeValue(new JavaSymbolName("source"), klass));
    //    annotations.add(new AnnotationMetadataBuilder(ROO_INTEGRATION_TEST, config));
    //
    //    final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
    //    final List<AnnotationMetadataBuilder> methodAnnotations =
    //        new ArrayList<AnnotationMetadataBuilder>();
    //    methodAnnotations.add(new AnnotationMetadataBuilder(TEST));
    //    final MethodMetadataBuilder methodBuilder =
    //        new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(
    //            "testMarkerMethod"), JavaType.VOID_PRIMITIVE, new InvocableMemberBodyBuilder());
    //    methodBuilder.setAnnotations(methodAnnotations);
    //    methods.add(methodBuilder);
    //
    //    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
    //        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
    //            PhysicalTypeCategory.CLASS);
    //    cidBuilder.setAnnotations(annotations);
    //    cidBuilder.setDeclaredMethods(methods);
    //
    //    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    //
    //    // Add spring-boot-test dependency
    //    getProjectOperations().addDependency(module.getModuleName(),
    //        DEPENDENCY_SPRING_BOOT_STARTER_TEST);
    //
    //    // Add plugin maven-failsafe-plugin
    //    // Stop if the plugin is already installed
    //    for (final Plugin plugin : module.getBuildPlugins()) {
    //      if (plugin.getArtifactId().equals("maven-failsafe-plugin")) {
    //        return;
    //      }
    //    }
    //
    //    final Element configuration = XmlUtils.getConfiguration(getClass());
    //    final Element plugin = XmlUtils.findFirstElement("/configuration/plugin", configuration);
    //
    //    // Now install the plugin itself
    //    if (plugin != null) {
    //      getProjectOperations().addBuildPlugin(module.getModuleName(), new Plugin(plugin));
    //    }

  }

  /**
   * Creates a unit test for the project class. Silently returns if the unit
   * test file already exists.
   *
   * @param projectType
   *            to produce a unit test class (required)
   */
  public void newUnitTest(final JavaType projectType) {
    Validate.notNull(projectType, "Class to produce a unit test class for is required");

    final JavaType name = new JavaType(projectType + "Test", projectType.getModule());
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            Path.SRC_TEST_JAVA.getModulePathId(projectType.getModule()));

    if (getMetadataService().get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    // Determine if the mocking infrastructure needs installing
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    final List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
    config.add(new ClassAttributeValue(new JavaSymbolName("value"), JUNIT_4));
    annotations.add(new AnnotationMetadataBuilder(RUN_WITH, config));
    annotations.add(new AnnotationMetadataBuilder(MOCK_STATIC_ENTITY_METHODS));

    final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
    final List<AnnotationMetadataBuilder> methodAnnotations =
        new ArrayList<AnnotationMetadataBuilder>();
    methodAnnotations.add(new AnnotationMetadataBuilder(TEST));

    // Get the entity so we can hopefully make a demo method that will be
    // usable
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(projectType);
    if (cid != null) {
      final MemberDetails memberDetails =
          getMemberDetailsScanner().getMemberDetails(IntegrationTestOperationsImpl.class.getName(),
              cid);
      final List<MethodMetadata> countMethods =
          memberDetails.getMethodsWithTag(CustomDataKeys.COUNT_ALL_METHOD);
      if (countMethods.size() == 1) {
        final String countMethod =
            projectType.getSimpleTypeName() + "."
                + countMethods.get(0).getMethodName().getSymbolName() + "()";
        bodyBuilder.appendFormalLine("int expectedCount = 13;");
        bodyBuilder.appendFormalLine(countMethod + ";");
        bodyBuilder
            .appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.expectReturn(expectedCount);");
        bodyBuilder
            .appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.playback();");
        bodyBuilder.appendFormalLine("org.junit.Assert.assertEquals(expectedCount, " + countMethod
            + ");");
      }
    }

    final MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(
            "testMethod"), JavaType.VOID_PRIMITIVE, bodyBuilder);
    methodBuilder.setAnnotations(methodAnnotations);
    methods.add(methodBuilder);

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);
    cidBuilder.setDeclaredMethods(methods);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  // Methods to obtain OSGi Services

  private TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  private MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

}

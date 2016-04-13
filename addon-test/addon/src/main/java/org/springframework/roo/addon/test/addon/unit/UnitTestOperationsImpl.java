package org.springframework.roo.addon.test.addon.unit;

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
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Provides convenience methods that can be used to create mock tests.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class UnitTestOperationsImpl implements UnitTestOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(UnitTestOperationsImpl.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private MemberDetailsScanner memberDetailsScanner;
  @Reference
  private MetadataService metadataService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeManagementService typeManagementService;

  @Override
  public boolean isUnitTestInstallationPossible() {
    return projectOperations.isFocusedProjectAvailable()
        && projectOperations.isFeatureInstalled(FeatureNames.JPA, FeatureNames.MONGO);
  }

  public void newUnitTest(final JavaType projectType) {
    Validate.notNull(projectType, "Class to produce a unit test class for is required");

    final JavaType name = new JavaType(projectType + "Test", projectType.getModule());
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            Path.SRC_TEST_JAVA.getModulePathId(projectType.getModule()));

    if (metadataService.get(declaredByMetadataId) != null) {
      // The file already exists
      return;
    }

    // Add dependencies from configuration file if needed
    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Dependency> dependencies = new ArrayList<Dependency>();
    final List<Element> unitTestDependencies =
        XmlUtils.findElements("/configuration/dependencies/dependency", configuration);
    for (final Element dependencyElement : unitTestDependencies) {
      dependencies.add(new Dependency(dependencyElement));
    }
    getProjectOperations().addDependencies(projectType.getModule(), dependencies);

    // Add plugins from configuration file if needed. Need to check if already exists, 
    // otherwise it can overwrite plugin configuration in single module projects
    final List<Element> unitTestPlugins =
        XmlUtils.findElements("/configuration/plugins/plugin", configuration);
    for (final Element pluginElement : unitTestPlugins) {
      getProjectOperations().updateBuildPlugin(projectType.getModule(), new Plugin(pluginElement));
    }

    // Add @RooUnitTest to source file
    AnnotationMetadataBuilder rooUnitTestAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_UNIT_TEST);
    rooUnitTestAnnotation.addClassAttribute("targetClass", projectType);

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.addAnnotation(rooUnitTestAnnotation);

    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (ProjectOperations) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

}

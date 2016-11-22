package org.springframework.roo.addon.security.addon.security.providers;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implementation of SecurityProvider to work with the domain
 * model during the authentication process.
 * 
 * The name of this provider is "SPRINGLETS_JPA" and must be unique. It will be used to 
 * recognize this Spring Security Provider.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class SpringletsJpaSecurityProvider implements SecurityProvider {

  private static final Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.0.0.BUILD-SNAPSHOT");
  private static final Dependency SPRINGLETS_SECURITY_AUTHENTICATION_STARTER = new Dependency(
      "io.springlets", "springlets-boot-starter-authentication", "${springlets.version}");

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(SpringletsJpaSecurityProvider.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  private ProjectOperations projectOperations;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  @Override
  public String getName() {
    return "SPRINGLETS_JPA";
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    boolean isInstalledInModule = false;
    Pom module = getProjectOperations().getPomFromModuleName(moduleName);
    Set<Dependency> starter =
        module.getDependenciesExcludingVersion(SPRINGLETS_SECURITY_AUTHENTICATION_STARTER);

    if (!starter.isEmpty()) {
      isInstalledInModule = true;
    }

    return isInstalledInModule;
  }

  @Override
  public boolean isInstallationAvailable() {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled(FeatureNames.MVC)
        && !getProjectOperations().isFeatureInstalled("SPRINGLETS_JPA");
  }

  @Override
  public void install(Pom module) {

    // Include Springlets Starter project dependencies and properties
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);
    getProjectOperations().addDependency(module.getModuleName(),
        SPRINGLETS_SECURITY_AUTHENTICATION_STARTER);

    // Add property security.enable-csrf with true value to enable CSRF
    getApplicationConfigService().addProperty(module.getModuleName(), "security.enable-csrf",
        "true", "", true);
    getApplicationConfigService().addProperty(module.getModuleName(), "security.enable-csrf",
        "true", "dev", true);

    // Add thymeleaf-extras-springsecurity4 dependency with Thymeleaf 3 support
    getProjectOperations().addProperty(module.getModuleName(),
        new Property("thymeleaf-extras-springsecurity4.version", "3.0.0.RELEASE"));
    getProjectOperations().addDependency(module.getModuleName(),
        new Dependency("org.thymeleaf.extras", "thymeleaf-extras-springsecurity4", null));

    // Add @EnableJpaRepositories and @EntityScan annotations to the @SpringBootApplication class
    Set<ClassOrInterfaceTypeDetails> springBootApplicationClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            SpringJavaType.SPRING_BOOT_APPLICATION);
    if (!springBootApplicationClasses.isEmpty()) {
      for (ClassOrInterfaceTypeDetails springBootApplicationClass : springBootApplicationClasses) {
        ClassOrInterfaceTypeDetailsBuilder cidBuilder =
            new ClassOrInterfaceTypeDetailsBuilder(springBootApplicationClass);
        cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
            SpringJavaType.ENABLE_JPA_REPOSITORIES));
        cidBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.ENTITY_SCAN));

        getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
        break;
      }
    }
  }

  // Service references
  public ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private ApplicationConfigService getApplicationConfigService() {
    return serviceInstaceManager.getServiceInstance(this, ApplicationConfigService.class);
  }

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

}

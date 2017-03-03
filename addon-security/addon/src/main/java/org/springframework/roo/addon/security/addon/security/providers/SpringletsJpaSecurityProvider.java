package org.springframework.roo.addon.security.addon.security.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaConfigurationMetadata;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of SecurityProvider to work with the domain model during the
 * authentication process. The name of this provider is "SPRINGLETS_JPA" and
 * must be unique. It will be used to recognize this Spring Security Provider.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class SpringletsJpaSecurityProvider implements SecurityProvider {

  private static final Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.2.0.RC1");

  private static final Dependency SPRINGLETS_SECURITY_AUTHENTICATION_STARTER = new Dependency(
      "io.springlets", "springlets-boot-starter-authentication", "${springlets.version}");

  private static final String THYMELEAF_SPRING_SECURITY_VERSION = "3.0.0.RELEASE";

  private static final Property THYMELEAF_SPRING_SECURITY_VERSION_PROPERTY = new Property(
      "thymeleaf-extras-springsecurity4.version", THYMELEAF_SPRING_SECURITY_VERSION);

  private static final Dependency THYMELEAF_SPRING_SECURITY = new Dependency(
      "org.thymeleaf.extras", "thymeleaf-extras-springsecurity4", null);

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(SpringletsJpaSecurityProvider.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

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
    getProjectOperations().addProperty("", THYMELEAF_SPRING_SECURITY_VERSION_PROPERTY);
    getProjectOperations().addDependency(module.getModuleName(), THYMELEAF_SPRING_SECURITY);

    // Do changes to files now
    getFileManager().commit();

    // If a special JPA repositories configuration exists, get its metadata 
    // to allow implementing needed changes
    Set<ClassOrInterfaceTypeDetails> repositoryConfigClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_REPOSITORY_CONFIGURATION);
    if (!repositoryConfigClasses.isEmpty()) {

      // We should evict Pom metadata for being aware of recently added starter
      getMetadataService().evict(
          getProjectOperations().getProjectMetadata(module.getModuleName()).getId());

      // Evict and get RepositoryJpaConfigurationMetadata
      ClassOrInterfaceTypeDetails repoConfigDetails = repositoryConfigClasses.iterator().next();
      String metadataIdentifier =
          RepositoryJpaConfigurationMetadata.createIdentifier(repoConfigDetails);
      getMetadataService().evictAndGet(metadataIdentifier);
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

  public MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

  public FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

}

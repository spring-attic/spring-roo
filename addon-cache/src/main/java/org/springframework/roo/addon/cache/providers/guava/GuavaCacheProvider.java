package org.springframework.roo.addon.cache.providers.guava;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cache.providers.CacheProvider;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;

import java.util.List;

/**
 * Provides implementation of {@link CacheProvider} interface by installing Guava 
 * as intermediate memory manager.
 *
 * @author Sergio Clares
 */
@Component
@Service
public class GuavaCacheProvider implements CacheProvider {

  private static final String GUAVA_PROVIDER_NAME = "GUAVA";
  private static final Dependency GUAVA_DEPENDENCY = new Dependency("com.google.guava", "guava",
      null);
  private static final String GUAVA_CACHE_SPEC_PROPERTY_KEY = "spring.cache.guava.spec";
  private static final String GUAVA_CACHE_SPEC_PROPERTY_VALUE =
      "maximumSize=3,expireAfterAccess=60m,expireAfterWrite=1h";
  private static final String CACHE_TYPE_PROPERTY_VALUE = "guava";

  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ApplicationConfigService applicationConfigService;

  List<Pom> applicationModules = null;

  @Override
  public String getName() {
    return GUAVA_PROVIDER_NAME;
  }

  @Override
  public boolean isInstalled() {
    if (this.applicationModules == null) {
      this.applicationModules =
          (List<Pom>) typeLocationService.getModules(ModuleFeatureName.APPLICATION);
    }

    // Look if the Guava dependency has been installed
    for (Pom module : applicationModules) {
      if (module.hasDependencyExcludingVersion(GUAVA_DEPENDENCY)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void setup(String profile) {
    if (this.applicationModules == null) {
      this.applicationModules =
          (List<Pom>) typeLocationService.getModules(ModuleFeatureName.APPLICATION);
    }

    // Add Guava dependency to each application module
    for (Pom module : applicationModules) {
      projectOperations.addDependency(module.getModuleName(), GUAVA_DEPENDENCY);

      // Add Guava specific application properties
      applicationConfigService.addProperty(module.getModuleName(), CACHE_TYPE_PROPERTY_KEY,
          CACHE_TYPE_PROPERTY_VALUE, profile, false);
      applicationConfigService.addProperty(module.getModuleName(), GUAVA_CACHE_SPEC_PROPERTY_KEY,
          GUAVA_CACHE_SPEC_PROPERTY_VALUE, profile, false);
    }

  }

}

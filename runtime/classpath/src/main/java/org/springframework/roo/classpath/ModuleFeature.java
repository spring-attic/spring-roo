package org.springframework.roo.classpath;

import java.util.List;

import org.springframework.roo.project.maven.Pom;

/**
 * Implemented by the different module types to identify and get the modules that have features.
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public interface ModuleFeature {

  /**
   * Returns module feature name
   * 
   * @return
   */
  ModuleFeatureName getName();

  /**
   * Gets the list of modules that have this module feature installed
   * @return 
   */
  List<Pom> getModules();

  /**
   * Gets the list of module names that have this module feature installed
   * @return
   */
  List<String> getModuleNames();

  /**
   * Indicates whether the specified module in has installed this module feature.
   * 
   * @param module
   * @return
   */
  boolean hasModuleFeature(Pom module);
}

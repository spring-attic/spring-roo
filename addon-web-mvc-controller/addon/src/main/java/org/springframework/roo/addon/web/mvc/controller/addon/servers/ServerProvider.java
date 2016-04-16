package org.springframework.roo.addon.web.mvc.controller.addon.servers;

import org.springframework.roo.project.Feature;
import org.springframework.roo.project.maven.Pom;

/**
 * Implemented by classes to manage the application configuration for a particular type of server
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public interface ServerProvider extends Feature {

  /**
  * Provides an operation to setup the configuration of this server on current project.
  */
  public void setup(Pom module);

}

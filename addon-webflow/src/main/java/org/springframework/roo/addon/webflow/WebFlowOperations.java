package org.springframework.roo.addon.webflow;

import org.springframework.roo.model.JavaType;

/**
 * Interface for {@link WebFlowOperationsImpl}.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * 
 * @since 1.0
 */
public interface WebFlowOperations {

  /**
   * Installs a new flow in its own directory under /src/main/resources/templates.
   * For example if the flow name is "main" then all flow artifacts will be in
   * /src/main/resources/templates/main. The first time a flow is installed, Web 
   * Flow related configuration will also be added. The flow directory is expected 
   * to be used exclusively for flow-related artifacts. A new flow will not be
   * created if the flow directory already exists.
   * 
   * @param flowName the name of the flow to install
   * @param moduleName the module where install the flow
   * @param klass the class used to mainly bind flow views
   * @throws IllegalStateException if the directory for the flow already
   *             exists.
   */
  void installWebFlow(String flowName, String moduleName, JavaType klass);

  boolean isWebFlowInstallationPossible();
}

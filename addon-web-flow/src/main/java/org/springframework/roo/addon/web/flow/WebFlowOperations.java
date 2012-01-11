package org.springframework.roo.addon.web.flow;

/**
 * Interface for {@link WebFlowOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface WebFlowOperations {

    /**
     * Installs a new flow in its own directory under /WEB-INF/views. For
     * example if the flow name is "main" then all flow artifacts will be in
     * /WEB-INF/views/main. The first time a flow is installed, Web Flow related
     * configuration will also be added. The flow directory is expected to be
     * used exclusively for flow-related artifacts. A new flow will not be
     * created if the flow directory already exists.
     * 
     * @param flowName the name of the flow to install
     * @throws IllegalStateException if the directory for the flow already
     *             exists.
     */
    void installWebFlow(String flowName);

    boolean isWebFlowInstallationPossible();
}
package org.springframework.roo.addon.web.flow;

/**
 * Interface for {@link WebFlowOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface WebFlowOperations {

	boolean isInstallWebFlowAvailable();

	boolean isManageWebFlowAvailable();

	/**
	 * 
	 * @param flowName
	 */
	void installWebFlow(String flowName);

}
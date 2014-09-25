package org.springframework.roo.addon.cloud.providers.cloudfoundry;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.providers.CloudProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * 
 * This Cloud Provider provides you the functionalities to deploy Spring Roo
 * application on Cloud Foundry
 * 
 * @author Juan Carlos García del Canto
 * @since 1.2.6
 */
@Component
@Service
public class CloudFoundryProvider implements CloudProvider {

	@Reference
	private FileManager fileManager;

	@Reference
	private PathResolver pathResolver;

	@Reference
	private TypeLocationService typeLocationService;

	@Reference
	private TypeManagementService typeManagementService;

	@Reference
	private ProjectOperations projectOperations;

	/**
	 * DECLARING CONSTANTS
	 */

	public static final String NAME = "CLOUD_FOUNDRY";

	public static final String DESCRIPTION = "Setup Cloud Foundry Maven Plugin on your Spring Roo Application";

	private static final Logger LOGGER = Logger
			.getLogger(CloudFoundryProvider.class.getName());

	/**
	 * This method configure your project with Cloud Foundry Maven Plugin
	 */
	@Override
	public void setup() {
		// Adding Cloud Foundry Maven Plugin with out config
        final Element configuration = XmlUtils.getConfiguration(getClass());
        // Add POM plugin
        updatePlugins(configuration,
                "/configuration/build/plugins/plugin", projectOperations);
	}
	
	
	/**
     * 
     * This method update plugins with the added to configuration.xml file
     * 
     * @param configuration
     * @param moduleName
     * @param projectOperations
     */
    public static void updatePlugins(final Element configuration,
            String path, ProjectOperations projectOperations) {
        final List<Plugin> plugins = new ArrayList<Plugin>();
        final List<Element> cloudFoundryMvnPlugin = XmlUtils.findElements(path,
                configuration);
        for (final Element pluginElement : cloudFoundryMvnPlugin) {
        	plugins.add(new Plugin(pluginElement));
        }
        projectOperations.addBuildPlugins(
                projectOperations.getFocusedModuleName(), plugins);
    }

	/**
	 * PROVIDER CONFIGURATION METHODS
	 */

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

}

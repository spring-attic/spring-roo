package org.springframework.roo.addon.cloud.providers.cloudfoundry;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.providers.CloudProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Configuration;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
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
	 * 
	 * @param configuration
	 */
	@Override
	public void setup(String pluginConfiguration) {
		// Adding Cloud Foundry Maven Plugin 
		updatePlugins(pluginConfiguration, projectOperations);
		// TODO: Showing INFO about how to use CF Maven Plugin
	}

	/**
	 * 
	 * This method update plugins with the added to configuration.xml file
	 * 
	 * @param configuration
	 * @param moduleName
	 * @param projectOperations
	 */
	public static void updatePlugins(String pluginConfiguration,
			ProjectOperations projectOperations) {

		Configuration conf = null;
		
		// Generating configuration if necessary
		if (StringUtils.isNotBlank(pluginConfiguration)) {
			try {

				DocumentBuilderFactory docFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				
				Document doc = docBuilder.newDocument();
				Element configElement = doc.createElement("configuration");
				
				// Getting all configurations
				String[] configurationTags = pluginConfiguration.split(",");

				for(String configurationTag : configurationTags){
					String[] keyValue = configurationTag.split("=");
					if(keyValue.length == 2){
						Element element = doc.createElement(keyValue[0]);
						element.setTextContent(keyValue[1]);
						configElement.appendChild(element);
					}
				}
				
				conf = new Configuration(configElement);
				
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "[ERROR] " + e);
			}
		}

		// Generate Plugin
		Plugin cloudFoundryMvnPlugin = new Plugin("org.cloudfoundry",
				"cf-maven-plugin", "1.0.4", conf, null, null);

		// Adding plugin
		projectOperations
				.addBuildPlugin(projectOperations.getFocusedModuleName(),
						cloudFoundryMvnPlugin);
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

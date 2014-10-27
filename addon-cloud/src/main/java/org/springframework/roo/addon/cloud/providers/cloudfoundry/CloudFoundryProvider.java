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
		// Showing INFO about how to use CF Maven Plugin
		showInfo(pluginConfiguration);
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

				for (String configurationTag : configurationTags) {
					String[] keyValue = configurationTag.split("=");
					if (keyValue.length == 2) {
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
	 * This method shows info about how to use Cloud Foundry
	 * 
	 * @param configuration
	 */
	public static void showInfo(String configuration) {
		// Showing congrats message
		LOGGER.log(
				Level.INFO,
				"Congratulations! Now you can use Cloud Foundry Maven Plugin to deploy your applications!");
		LOGGER.log(Level.INFO, "");
		// Showing current config if necessary
		if (StringUtils.isNotBlank(configuration)) {
			LOGGER.log(Level.INFO, "This is your current configuration:");
			LOGGER.log(Level.INFO, "");
			LOGGER.log(Level.INFO, "<configuration>");
			// Getting all configurations
			String[] configurationTags = configuration.split(",");

			for (String configurationTag : configurationTags) {
				String[] keyValue = configurationTag.split("=");
				if (keyValue.length == 2) {
					LOGGER.log(Level.INFO, String.format("   <%s>%s</%s>",
							keyValue[0], keyValue[1], keyValue[0]));
				}
			}
			LOGGER.log(Level.INFO, "</configuration>");
		} else {
			LOGGER.log(Level.INFO,
					"WARNING: You don't specify any configuration.");
		}
		// Showing commands you can use with maven CF Plugin
		LOGGER.log(Level.INFO, "");
		LOGGER.log(
				Level.INFO,
				"You can use the following Cloud Foundry Maven Plugin commands with \"perform command --mavenCommand\" on ROO Shell or using \"mvn\" on OS command line.");
		LOGGER.log(Level.INFO, "");
		LOGGER.log(Level.INFO, "Command           			Description");
		LOGGER.log(Level.INFO, "----------------------------------------");
		LOGGER.log(Level.INFO, "cf:apps  	      			List deployed applications.");
		LOGGER.log(Level.INFO,
				"cf:app 	          			Show details of an application.");
		LOGGER.log(Level.INFO, "cf:delete 	      			Delete an application.");
		LOGGER.log(Level.INFO,
				"cf:env 	          			Show an application's environment variables.");
		LOGGER.log(Level.INFO,
				"cf:help 	      			Show documentation for all available commands.");
		LOGGER.log(Level.INFO,
				"cf:push 	      			Push and optionally start an application.");
		LOGGER.log(
				Level.INFO,
				"cf:push-only      			Push and optionally start an application, without packaging.");
		LOGGER.log(Level.INFO, "cf:restart 	      			Restart an application.");
		LOGGER.log(Level.INFO, "cf:start 	      			Start an application.");
		LOGGER.log(Level.INFO, "cf:stop 	             	Stop an application.");
		LOGGER.log(
				Level.INFO,
				"cf:target 	                Show information about the target Cloud Foundry service.");
		LOGGER.log(Level.INFO,
				"cf:logs 	                Tail application logs.");
		LOGGER.log(Level.INFO,
				"cf:recentLogs               Show recent application logs.");
		LOGGER.log(Level.INFO,
				"cf:scale 	                Scale the application instances up or down.");
		LOGGER.log(Level.INFO,
				"cf:services                 Show a list of provisioned services.");
		LOGGER.log(Level.INFO,
				"cf:service-plans 	        Show a list of available service plans.");
		LOGGER.log(Level.INFO,
				"cf:create-services 	        Create services defined in the pom.");
		LOGGER.log(Level.INFO,
				"cf:delete-services 	        Delete services defined in the pom.");
		LOGGER.log(Level.INFO,
				"cf:bind-services 	        Bind services to an application.");
		LOGGER.log(Level.INFO,
				"cf:unbind-services 	        Unbind services from an application.");
		LOGGER.log(
				Level.INFO,
				"cf:delete-orphaned-routes 	Delete all routes that are not bound to any application.");
		LOGGER.log(
				Level.INFO,
				"cf:login 	                Log in to the target Cloud Foundry service and save access tokens.");
		LOGGER.log(
				Level.INFO,
				"cf:logout 	                Log out of the target Cloud Foundry service and remove access tokens.");
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

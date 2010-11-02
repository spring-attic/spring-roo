package org.springframework.roo.addon.web.mvc.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Commands for the 'mvc controller' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component 
@Service
public class ControllerCommands implements CommandMarker {
	private static Logger logger = HandlerUtils.getLogger(ControllerCommands.class);
	@Reference private ControllerOperations controllerOperations;
	@Reference private MetadataService metadataService;

	@CliAvailabilityIndicator({ "controller scaffold", "controller all" }) 
	public boolean isNewControllerAvailable() {
		return controllerOperations.isNewControllerAvailable();
	}

	@CliCommand(value = "controller all", help = "Scaffold a controller for all entities without an existing controller") 
	public void generateAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which new controllers will be placed") JavaPackage javaPackage) {
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Could not obtain ProjectMetadata");
		if (!javaPackage.getFullyQualifiedPackageName().startsWith(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName())) {
			logger.warning("Your controller was created outside of the project's top level package and is therefore not included in the preconfigured component scanning. Please adjust your component scanning manually in webmvc-config.xml");
		}
		controllerOperations.generateAll(javaPackage);
	}

	@CliCommand(value = "controller scaffold", help = "Create a new scaffold Controller (ie where we maintain CRUD automatically)") 
	public void newController(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the controller object to be created") JavaType controller, 
		@CliOption(key = "entity", mandatory = false, optionContext = "update,project", unspecifiedDefaultValue = "*", help = "The name of the entity object which the controller exposes to the web tier") JavaType entity, 
		@CliOption(key = "path", mandatory = false, help = "The base path under which the controller listens for RESTful requests (defaults to the simple name of the form backing object)") String path, 
		@CliOption(key = "disallowedOperations", mandatory = false, help = "A comma separated list of operations (only create, update, delete allowed) that should not be generated in the controller") String disallowedOperations) {

		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(entity, Path.SRC_MAIN_JAVA));
		if (entityMetadata == null) {
			logger.warning("The specified entity can not be resolved to an Entity type in your project");
			return;
		}
		
		if (controller.getSimpleTypeName().equalsIgnoreCase(entity.getSimpleTypeName())) {
			logger.warning("Controller class name needs to be different from the class name of the form backing object (suggestion: '" + entity.getSimpleTypeName() + "Controller')");
			return;
		}

		Set<String> disallowedOperationSet = new HashSet<String>();
		if (!"".equals(disallowedOperations)) {
			for (String operation : StringUtils.commaDelimitedListToSet(disallowedOperations)) {
				if (!("create".equals(operation) || "update".equals(operation) || "delete".equals(operation))) {
					logger.warning("-disallowedOperations options can only contain 'create', 'update', 'delete': -disallowedOperations update,delete");
					return;
				}
				disallowedOperationSet.add(operation.toLowerCase());
			}
		}

		if (path == null || path.length() == 0) {
			PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entity, Path.SRC_MAIN_JAVA));
			Assert.notNull(pluralMetadata, "Could not determine plural for '" + entity.getSimpleTypeName() + "'");
			path = pluralMetadata.getPlural().toLowerCase();
		} else if (path.startsWith("/")) {
			path = path.substring(1);
		}

		controllerOperations.createAutomaticController(controller, entity, disallowedOperationSet, path);
	}
}
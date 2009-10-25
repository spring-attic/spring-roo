package org.springframework.roo.addon.web.mvc.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Commands for the 'mvc controller' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class ControllerCommands implements CommandMarker {
	
	private static Logger logger = Logger.getLogger(ControllerCommands.class.getName());
	
	private ControllerOperations controllerOperations;
	private ProjectMetadata projectMetadata;
	
	public ControllerCommands(ControllerOperations controllerOperations, MetadataService metadataService) {
		Assert.notNull(controllerOperations, "ControllerOperations instance required");
		this.controllerOperations = controllerOperations;
		ProjectMetadata metadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(metadata, "Could not obtain Project Metadata");
		this.projectMetadata = metadata;
	}
	
	@CliAvailabilityIndicator({"controller automatic", "controller manual", "controller all"})
	public boolean isNewControllerAvailable() {
		return controllerOperations.isNewControllerAvailable();
	}

	@CliCommand(value="controller all", help="Scaffold a controller for all entities without an existing controller")
	public void generateAll(@CliOption(key="package", mandatory=true, help="The package in which new controllers will be placed") JavaPackage javaPackage) {
		if (!javaPackage.getFullyQualifiedPackageName().startsWith(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName())) {
			logger.warning("Your controller was created outside of the project's top level package and is therefore not included in the preconfigured component scanning. Please adjust your component scanning manually in webmvc-config.xml");
		}
		controllerOperations.generateAll(javaPackage);
	}
	
	@CliCommand(value="controller scaffold", help="Create a new scaffold Controller (ie where we maintain CRUD automatically)")
	public void newController(
			@CliOption(key={"name",""}, mandatory=true, help="The path and name of the controller object to be created") JavaType controller,
			@CliOption(key="entity", mandatory=false, optionContext="update,project", unspecifiedDefaultValue="*", help="The name of the entity object which the controller exposes to the web tier") JavaType entity,
			@CliOption(key="path", mandatory=false, help="The base path under which the controller listens for RESTful requests (defaults to the simple name of the form backing object)") String path,
			@CliOption(key="disallowedOperations", mandatory=false, help="A comma separated list of operations (only create, update, delete allowed) that should not be generated in the controller") String disallowedOperations,
			@CliOption(key="dateFormat", mandatory=false, help="The date format defaults to the current locale, use this parameter to customize the date format") String dateFormat) {
		
		if (controller.getSimpleTypeName().equalsIgnoreCase(entity.getSimpleTypeName())) {
			logger.warning("Controller class name needs to be different from the class name of the form backing object (suggestion: '" + entity.getSimpleTypeName() + "Controller')");
			return;
		}
		
		Set<String> disallowedOperationSet = new HashSet<String>();
		if (!"".equals(disallowedOperations)) {
			for (String operation : StringUtils.commaDelimitedListToSet(disallowedOperations)) {
				if(!("create".equals(operation) || "update".equals(operation) || "delete".equals(operation))) {
					logger.warning("-disallowedOperations options can only contain 'create', 'update', 'delete': -disallowedOperations update,delete");
					return;
				}					
				disallowedOperationSet.add(operation.toLowerCase());
			}
		}
		
		if (path == null || path.length() == 0) {
			path = entity.getSimpleTypeName().toLowerCase();
		} else if (path.startsWith("/")) {
			path = path.substring(1);
		}
		
		controllerOperations.createAutomaticController(controller, entity, disallowedOperationSet, path, dateFormat);
	}

	@CliCommand(value="controller class", help="Create a new manual Controller (ie where you write the methods)")
	public void newController(
			@CliOption(key={"name",""}, mandatory=true, help="The path and name of the controller object to be created") JavaType controller,
			@CliOption(key="preferredMapping", mandatory=false, help="Indicates a specific request mapping path for this controller (eg /foo/)") String preferredMapping) {
		controllerOperations.createManualController(controller, preferredMapping);
	}
}
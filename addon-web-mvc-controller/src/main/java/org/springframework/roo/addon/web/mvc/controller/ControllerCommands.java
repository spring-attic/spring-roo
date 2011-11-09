package org.springframework.roo.addon.web.mvc.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.converters.JavaTypeConverter;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.ProjectOperations;
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

	// Constants
	private static Logger logger = HandlerUtils.getLogger(ControllerCommands.class);

	// Fields
	@Reference private ControllerOperations controllerOperations;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	
	@CliAvailabilityIndicator({ "web mvc all", "web mvc scaffold" }) 
	public boolean isScaffoldAvailable() {
		return controllerOperations.isScaffoldAvailable();
	}

	@Deprecated
	@CliAvailabilityIndicator({ "controller scaffold", "controller all" })
	public boolean isNewControllerAvailable() {
		return controllerOperations.isNewControllerAvailable();
	}

	@CliCommand(value = "web mvc all", help = "Scaffold Spring MVC controllers for all project entities without an existing controller")
	public void webMvcAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which new controllers will be placed") final JavaPackage javaPackage) {
		Assert.isTrue(projectOperations.isFocusedProjectAvailable(), "Could not obtain ProjectMetadata");
		if (!javaPackage.getFullyQualifiedPackageName().startsWith(projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName())) {
			logger.warning("Your controller was created outside of the project's top level package and is therefore not included in the preconfigured component scanning. Please adjust your component scanning manually in webmvc-config.xml");
		}
		controllerOperations.generateAll(javaPackage);
	}

	@CliCommand(value = "web mvc scaffold", help = "Create a new scaffold Controller (ie where Roo maintains CRUD functionality automatically)")
	public void webMvcScaffold(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the controller object to be created") final JavaType controller,
		@CliOption(key = "backingType", mandatory = false, optionContext = JavaTypeConverter.PROJECT, unspecifiedDefaultValue = "*", help = "The name of the form backing type which the controller exposes to the web tier") final JavaType backingType,
		@CliOption(key = "path", mandatory = false, help = "The base path under which the controller listens for RESTful requests (defaults to the simple name of the form backing object)") String path,
		@CliOption(key = "disallowedOperations", mandatory = false, help = "A comma separated list of operations (only create, update, delete allowed) that should not be generated in the controller") final String disallowedOperations) {

		String targetMid = typeLocationService.getPhysicalTypeIdentifier(backingType);
		if (targetMid == null) {
			logger.warning("The specified entity can not be resolved to a type in your project");
			return;
		}


		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.getTypeDetails(backingType);
		if (classOrInterfaceTypeDetails == null) {
			logger.warning("The specified entity can not be resolved to a type in your project");
			return;
		}

		if (controller.getSimpleTypeName().equalsIgnoreCase(backingType.getSimpleTypeName())) {
			logger.warning("Controller class name needs to be different from the class name of the form backing object (suggestion: '" + backingType.getSimpleTypeName() + "Controller')");
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

		if (!StringUtils.hasText(path)) {
			ContextualPath targetPath = PhysicalTypeIdentifier.getPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
			PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(backingType, targetPath));
			Assert.notNull(pluralMetadata, "Could not determine plural for '" + backingType.getSimpleTypeName() + "'");
			path = pluralMetadata.getPlural().toLowerCase();
		} else if (path.equals("/") || path.equals("/*")) {
			logger.warning("Your application already contains a mapping to '/' or '/*' by default. Please provide a different path.");
			return;
		} else if (path.startsWith("/")) {
			path = path.substring(1);
		}

		controllerOperations.createAutomaticController(controller, backingType, disallowedOperationSet, path);
	}

	@Deprecated
	@CliCommand(value = "controller all", help = "Scaffold controllers for all project entities without an existing controller - deprecated, use 'web mvc setup' + 'web mvc all' instead")
	public void generateAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which new controllers will be placed") final JavaPackage javaPackage) {
		logger.warning("This command has been deprecated and will be disabled soon! Please use 'web mvc setup' followed by 'web mvc all --package ' instead.");
		controllerOperations.setup();
		webMvcAll(javaPackage);
	}

	@Deprecated
	@CliCommand(value = "controller scaffold", help = "Create a new scaffold Controller (ie where we maintain CRUD automatically) - deprecated, use 'web mvc scaffold' instead")
	public void newController(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the controller object to be created") final JavaType controller,
		@CliOption(key = "entity", mandatory = false, optionContext = "update,project", unspecifiedDefaultValue = "*", help = "The name of the entity object which the controller exposes to the web tier") final JavaType entity,
		@CliOption(key = "path", mandatory = false, help = "The base path under which the controller listens for RESTful requests (defaults to the simple name of the form backing object)") final String path,
		@CliOption(key = "disallowedOperations", mandatory = false, help = "A comma separated list of operations (only create, update, delete allowed) that should not be generated in the controller") final String disallowedOperations) {
		logger.warning("This command has been deprecated and will be disabled soon! Please use 'web mvc setup' followed by 'web mvc scaffold' instead.");
		controllerOperations.setup();
		webMvcScaffold(controller, entity, path, disallowedOperations);
	}
}
package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

/**
 * This class defines available commands to manage service layer. Allows to
 * create new service related with some specific entity or generate services for
 * every entity defined on generated project. Always generates interface and its
 * implementation
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class ServiceCommands implements CommandMarker {

  @Reference
  private ServiceOperations serviceOperations;
  @Reference
  private ProjectOperations projectOperations;

  @CliAvailabilityIndicator({"service add", "service all"})
  public boolean isServiceCommandAvailable() {
    return serviceOperations.areServiceCommandsAvailable();
  }

  // ROO-3717: Service secure methods will be updated on future commits
  /*@CliAvailabilityIndicator({ "service secure type", "service secure all" })
  public boolean isSecureServiceCommandAvailable() {
      return serviceOperations.isSecureServiceInstallationPossible();
  }*/

  @CliOptionVisibilityIndicator(command = "service add", params = "interface",
      help = "--interface parameter is not availbale if you don't specify --entity parameter")
  public boolean isIterfaceVisible(ShellContext shellContext) {

    // Get all defined parameters
    Map<String, String> parameters = shellContext.getParameters();

    // If --entity has been defined, show --class parameter
    if (parameters.containsKey("entity") && StringUtils.isNotBlank(parameters.get("entity"))) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "service add", help = "Creates new service interface and its implementation.")
  public void service(@CliOption(key = "entity", unspecifiedDefaultValue = "*",
      optionContext = PROJECT, mandatory = true,
      help = "The domain entity this service should expose") final JavaType domainType,
      @CliOption(key = "interface", mandatory = true,
          help = "The service interface to be generated") final JavaType interfaceType,
      @CliOption(key = "class", mandatory = false,
          help = "The service implementation to be generated") final JavaType implType) {

    serviceOperations.addService(domainType, interfaceType, implType);
  }

  @CliCommand(
      value = "service all",
      help = "Creates new service interface and its implementation for every entity of generated project.")
  public void service(
      @CliOption(key = "apiPackage", mandatory = true, help = "The java interface package") final JavaPackage apiPackage,
      @CliOption(key = "implPackage", mandatory = true,
          help = "The java package of the implementation classes for the interfaces") JavaPackage implPackage) {

    serviceOperations.addAllServices(apiPackage, implPackage);
  }

  // ROO-3717: Service secure methods will be updated on future commits
  /*@CliCommand(value = "service secure type", help = "Adds @RooService annotation to target type with options for authentication, authorization, and a permission evaluator")
  public void secureService(
          @CliOption(key = "interface", mandatory = true, help = "The java interface to apply this annotation to") final JavaType interfaceType,
          @CliOption(key = "class", mandatory = false, help = "Implementation class for the specified interface") JavaType classType,
          @CliOption(key = "entity", unspecifiedDefaultValue = "*", optionContext = PROJECT, mandatory = false, help = "The domain entity this service should expose") final JavaType domainType,
          @CliOption(key = "requireAuthentication", unspecifiedDefaultValue = "false", specifiedDefaultValue = "ture", mandatory = false, help = "Whether or not users must be authenticated to use the service") final boolean requireAuthentication,
          @CliOption(key = "authorizedRoles", mandatory = false, help = "The role authorized the use the methods in the service") final String role,
          @CliOption(key = "usePermissionEvaluator", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", mandatory = false, help = "Whether or not to use a PermissionEvaluator") final boolean usePermissionEvaluator,
          @CliOption(key = "useXmlConfiguration", mandatory = false, help = "When true, Spring Roo will configure services using XML.") Boolean useXmlConfiguration) {

      if (classType == null) {
          classType = new JavaType(interfaceType.getFullyQualifiedTypeName()
                  + "Impl");
      }
      if (useXmlConfiguration == null) {
          useXmlConfiguration = Boolean.FALSE;
      }
      serviceOperations.setupService(interfaceType, classType, domainType,
              requireAuthentication, role, usePermissionEvaluator,
              useXmlConfiguration);
  }

  @CliCommand(value = "service secure all", help = "Adds @RooService annotation to all entities with options for authentication, authorization, and a permission evaluator")
  public void secureServiceAll(
          @CliOption(key = "interfacePackage", mandatory = true, help = "The java interface package") final JavaPackage interfacePackage,
          @CliOption(key = "classPackage", mandatory = false, help = "The java package of the implementation classes for the interfaces") JavaPackage classPackage,
          @CliOption(key = "requireAuthentication", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", mandatory = false, help = "Whether or not users must be authenticated to use the service") final boolean requireAuthentication,
          @CliOption(key = "authorizedRole", mandatory = false, help = "The role authorized the use the methods in the service (additional roles can be added after creation)") final String role,
          @CliOption(key = "usePermissionEvaluator", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", mandatory = false, help = "Whether or not to use a PermissionEvaluator") final boolean usePermissionEvaluator,
          @CliOption(key = "useXmlConfiguration", mandatory = false, help = "When true, Spring Roo will configure services using XML.") Boolean useXmlConfiguration) {

      if (classPackage == null) {
          classPackage = interfacePackage;
      }
      if (useXmlConfiguration == null) {
          useXmlConfiguration = Boolean.FALSE;
      }
      serviceOperations.setupAllServices(interfacePackage, classPackage,
              requireAuthentication, role, usePermissionEvaluator,
              useXmlConfiguration);
  }*/
}

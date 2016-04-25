package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

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

  private static final Logger LOGGER = HandlerUtils.getLogger(ServiceCommands.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private ServiceOperations serviceOperations;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeLocationService typeLocationService;

  private Converter<JavaType> javaTypeConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }


  @CliAvailabilityIndicator({"service add", "service all"})
  public boolean isServiceCommandAvailable() {
    return serviceOperations.areServiceCommandsAvailable();
  }

  // ROO-3717: Service secure methods will be updated on future commits
  /*@CliAvailabilityIndicator({ "service secure type", "service secure all" })
  public boolean isSecureServiceCommandAvailable() {
      return serviceOperations.isSecureServiceInstallationPossible();
  }*/

  @CliOptionVisibilityIndicator(
      command = "service add",
      params = "interface",
      help = "--interface parameter is not available if you don't specify --entity or --repository parameters")
  public boolean isIterfaceVisible(ShellContext shellContext) {

    // Get all defined parameters
    Map<String, String> parameters = shellContext.getParameters();

    // If --entity and --repository have been defined, show --class parameter
    if (parameters.containsKey("entity") && StringUtils.isNotBlank(parameters.get("entity"))
        && parameters.containsKey("repository")
        && StringUtils.isNotBlank(parameters.get("repository"))) {
      return true;
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "service add", params = "repository",
      help = "--repository parameter is not available if you don't specify --entity parameter")
  public boolean isRepositoryVisible(ShellContext shellContext) {

    // Get all defined parameters
    Map<String, String> parameters = shellContext.getParameters();

    // If --entity has been defined, show --class parameter
    if (parameters.containsKey("entity") && StringUtils.isNotBlank(parameters.get("entity"))) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(
      command = "service add",
      param = "repository",
      help = "--repository parameter must  be the repository associated to the entity specified in --entity parameter. Please, write a valid value using autocomplete feature (TAB or CTRL + Space)")
  public List<String> returnRepositories(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("repository");

    List<String> allPossibleValues = new ArrayList<String>();

    // Get all defined parameters
    Map<String, String> contextParameters = shellContext.getParameters();

    // Getting entity
    String entity = contextParameters.get("entity");

    if (StringUtils.isNotBlank(entity)) {

      JavaType domainEntity =
          getJavaTypeConverter().convertFromText(entity, JavaType.class, PROJECT);

      // Check if current entity has valid repository
      Set<ClassOrInterfaceTypeDetails> repositories =
          typeLocationService
              .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);

      for (ClassOrInterfaceTypeDetails repository : repositories) {
        AnnotationAttributeValue<JavaType> entityAttr =
            repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");

        if (entityAttr != null && entityAttr.getValue().equals(domainEntity)) {
          String replacedValue = replaceTopLevelPackageString(repository, currentText);
          allPossibleValues.add(replacedValue);
        }
      }

      if (allPossibleValues.isEmpty()) {
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "ERROR: Entity '%s' does not have any repository generated. Use 'repository' commands to generate a valid repository and then try again.",
                        entity));
        allPossibleValues.add("");
      }
    }

    return allPossibleValues;
  }


  @CliCommand(value = "service add", help = "Creates new service interface and its implementation.")
  public void service(@CliOption(key = "entity", unspecifiedDefaultValue = "*",
      optionContext = PROJECT, mandatory = true,
      help = "The domain entity this service should expose") final JavaType domainType, @CliOption(
      key = "repository", optionContext = PROJECT, mandatory = true,
      help = "The repository this service should expose") final JavaType repositoryType,
      @CliOption(key = "interface", mandatory = true,
          help = "The service interface to be generated") final JavaType interfaceType,
      @CliOption(key = "class", mandatory = false,
          help = "The service implementation to be generated") final JavaType implType) {

    serviceOperations.addService(domainType, repositoryType, interfaceType, implType);
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

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for TopLevelPackage
   * 
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value  
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }

  @SuppressWarnings("unchecked")
  public Converter<JavaType> getJavaTypeConverter() {
    if (javaTypeConverter == null) {
      // Get all Services implement JavaTypeConverter interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Converter.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          Converter<?> converter = (Converter<?>) this.context.getService(ref);
          if (converter.supports(JavaType.class, PROJECT)) {
            javaTypeConverter = (Converter<JavaType>) converter;
            return javaTypeConverter;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("ERROR: Cannot load JavaTypeConverter on FinderCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
    }
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

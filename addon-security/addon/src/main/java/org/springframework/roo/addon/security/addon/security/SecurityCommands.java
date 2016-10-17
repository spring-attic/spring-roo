package org.springframework.roo.addon.security.addon.security;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;
import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.roo.addon.security.addon.security.providers.SecurityProvider;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands for the security add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class SecurityCommands implements CommandMarker {

  private static Logger LOGGER = HandlerUtils.getLogger(SecurityCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private SecurityOperations securityOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private LastUsed lastUsed;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  private Converter<JavaType> javaTypeConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @CliAvailabilityIndicator("security setup")
  public boolean isInstallSecurityAvailable() {
    // If some SecurityProvider is available to be installed, this command
    // will be available
    // showing only these ones.
    List<SecurityProvider> securityProviders = securityOperations.getAllSecurityProviders();
    for (SecurityProvider provider : securityProviders) {
      if (provider.isInstallationAvailable()) {
        return true;
      }
    }

    return false;
  }

  @CliOptionVisibilityIndicator(command = "security setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(params = "module", command = "security setup")
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisible(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliOptionAutocompleteIndicator(command = "security setup", param = "type",
      help = "You must select a valid security provider.", validate = true)
  public List<String> getAllSecurityProviders(ShellContext context) {

    List<String> results = new ArrayList<String>();

    List<SecurityProvider> securityProviders = securityOperations.getAllSecurityProviders();
    for (SecurityProvider provider : securityProviders) {
      if (provider.isInstallationAvailable()) {
        results.add(provider.getName());
      }
    }

    return results;

  }

  @CliCommand(value = "security setup", help = "Install Spring Security into your project")
  public void installSecurity(
      @CliOption(key = "type", mandatory = false,
          help = "The Spring Security provider to install.", unspecifiedDefaultValue = "DEFAULT",
          specifiedDefaultValue = "DEFAULT") String type,
      @CliOption(key = "module", mandatory = true,
          help = "The application module where to install the persistence",
          unspecifiedDefaultValue = ".", optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module) {

    securityOperations.installSecurity(getSecurityProviderFromName(type), module);

  }

  @CliAvailabilityIndicator("security authorize")
  public boolean isAuthorizeOperationAvailable() {
    return projectOperations.isFeatureInstalled(FeatureNames.SECURITY);
  }

  @CliOptionAutocompleteIndicator(command = "security authorize", param = "class",
      help = "You must select a valid Service class", validate = true)
  public List<String> getAllServiceClassesAndInterfaces(ShellContext context) {

    List<String> results = new ArrayList<String>();

    Set<ClassOrInterfaceTypeDetails> services =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_SERVICE,
            RooJavaType.ROO_SERVICE_IMPL);
    for (ClassOrInterfaceTypeDetails service : services) {
      results.add(replaceTopLevelPackageString(service, context.getParameters().get("class")));
    }
    return results;
  }

  @CliOptionAutocompleteIndicator(
      command = "security authorize",
      param = "method",
      help = "You must select a valid method from the provided class or a regular expression that match with existing methods",
      validate = false)
  public List<String> getAllMethodsRelatedWithProvidedClass(ShellContext context) {

    List<String> results = new ArrayList<String>();

    String service = context.getParameters().get("class");
    JavaType type = null;
    if (service != null) {
      type = getJavaTypeConverter().convertFromText(service, JavaType.class, PROJECT);
    } else {
      type = lastUsed.getJavaType();
    }

    MemberDetails serviceDetails =
        memberDetailsScanner.getMemberDetails(getClass().getName(),
            typeLocationService.getTypeDetails(type));

    List<MethodMetadata> methods = serviceDetails.getMethods();

    for (MethodMetadata method : methods) {

      String methodName = method.getMethodName().getSymbolName();

      List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();

      methodName = methodName.concat("(");

      for (int i = 0; i < parameterTypes.size(); i++) {
        String paramType = parameterTypes.get(i).getJavaType().getSimpleTypeName();
        methodName = methodName.concat(paramType).concat(",");
      }

      if (!parameterTypes.isEmpty()) {
        methodName = methodName.substring(0, methodName.length() - 1).concat(")");
      } else {
        methodName = methodName.concat(")");
      }

      results.add(methodName);
    }

    return results;
  }

  @CliCommand(value = "security authorize",
      help = "Include @PreAuthorize annotation to an specific method.")
  public void authorizeOperation(
      @CliOption(
          key = "class",
          mandatory = true,
          help = "The service class to annotate with @PreAuthorize or the service class that contains the method to annotate with @PreAuthorize.") JavaType klass,
      @CliOption(
          key = "method",
          mandatory = true,
          help = "The service method name and its params that will be annotated with @PreAuthorize. Is possible to specify a regular expression.") String methodName,
      @CliOption(key = "roles", mandatory = false,
          help = "Comma separated list with all the roles to add inside 'hasAnyRole' instruction. ") String roles) {

    if (StringUtils.isEmpty(roles)) {
      LOGGER.log(Level.INFO, "ERROR: You should provide almost one role on --roles parameter.");
      return;
    }

    // Calculate the @PreAuthorize annotation value by provided roles
    String value = getPreAuthorizeAnnotationValue(roles);

    // Include the @PreAuthorize annotation with the calculated value
    securityOperations.addPreAuthorizeAnnotation(klass, methodName, value);
  }

  /**
   * This method calculate the @PreAuthorize annotation value by the provided
   * roles
   * 
   * @param roles
   *            Comma separated list with all the roles
   * 
   * @return A String with the value to include in @PreAuthorize annotation
   */
  private String getPreAuthorizeAnnotationValue(String roles) {

    String value = "";

    if (StringUtils.isNotEmpty(roles)) {

      // First of all, obtain the comma separated list
      // that contains all roles
      String[] rolesList = roles.split(",");

      // Now, check if there's more than one role
      if (rolesList.length > 1) {
        // create the hasAnyRole expression
        value = "hasAnyRole(";
      } else {
        // create the hasRole expression
        value = "hasRole(";
      }

      for (String role : rolesList) {
        value = value.concat("'").concat(role).concat("'").concat(",");
      }
      value = value.substring(0, value.length() - 1).concat(")");
    }

    return value;
  }

  /**
   * This method obtains the implementation of the Spring Security Provider
   * using the provided name.
   * 
   * @param name
   *            The name of the SecurityProvider to obtain
   * 
   * @return A SecurityProvider with the same name as the provided one.
   */
  private SecurityProvider getSecurityProviderFromName(String name) {

    List<SecurityProvider> securityProviders = securityOperations.getAllSecurityProviders();
    for (SecurityProvider type : securityProviders) {
      if (type.getName().equals(name)) {
        return type;
      }
    }

    return null;
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   * 
   * @param cid
   *            ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText
   *            String current text for option value
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
        LOGGER.warning("ERROR: Cannot load JavaTypeConverter on FieldCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
    }
  }

}

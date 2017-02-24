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

  private static final String PRE_FILTER = "PRE";
  private static final String POST_FILTER = "POST";

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

  @CliOptionAutocompleteIndicator(command = "security setup", param = "provider",
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

  @CliCommand(value = "security setup", help = "Install Spring Security into your project.")
  public void installSecurity(
      @CliOption(
          key = "provider",
          mandatory = false,
          help = "The Spring Security provider to install. "
              + "Possible values are: `DEFAULT` (default Spring Security configuration provided by "
              + "Spring Boot will be used), and `SPRINGLETS_JPA` (advanced Spring Security configuration "
              + "will be included using Springlets JPA Authentication).",
          unspecifiedDefaultValue = "DEFAULT", specifiedDefaultValue = "DEFAULT") String type,
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where to install the security support. "
              + "This option is mandatory if the focus is not set in an application module, that is, a "
              + "module containing an `@SpringBootApplication` class. "
              + "This option is available only if there are more than one application module and none of"
              + " them is focused. "
              + "Default if option not present: the unique 'application' module, or focused 'application'"
              + " module.", unspecifiedDefaultValue = ".",
          optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module) {

    securityOperations.installSecurity(getSecurityProviderFromName(type), module);

  }

  @CliAvailabilityIndicator("security authorize")
  public boolean isAuthorizeOperationAvailable() {
    return projectOperations.isFeatureInstalled(FeatureNames.SECURITY);
  }

  @CliOptionAutocompleteIndicator(command = "security authorize", param = "class",
      help = "You must select a valid Service class", validate = true)
  public List<String> getAllServiceClassesAndInterfacesForAuthorize(ShellContext context) {

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
  public List<String> getAllMethodsRelatedWithProvidedClassForAuthorize(ShellContext context) {

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

  @CliCommand(
      value = "security authorize",
      help = "Includes `@PreAuthorize` annotation to an specific method for controlling access to its "
          + "invocation.")
  public void authorizeOperation(
      @CliOption(
          key = "class",
          mandatory = true,
          help = "The service class that contains the method to annotate with `@PreAuthorize`. When working"
              + " on a single module project, simply specify the name of the class. If you consider it "
              + "necessary, you can also specify the package. Ex.: `--class ~.service.MyClass` (where `~` "
              + "is the base package). When working with multiple modules, you should specify the name of the class and the "
              + "module where it is. Ex.: `--class service:~.MyClass`. If the module is not specified, "
              + "it is assumed that the class is in the module which has the focus."
              + "Possible values are: any of the service classes in the project.") JavaType klass,
      @CliOption(
          key = "method",
          mandatory = true,
          help = "The service method name (including its params) that will be annotated with "
              + "@PreAuthorize. Is possible to specify a regular expression. "
              + "Possible values are: any of the existing methods of the class specified in `--class` "
              + "option, or regular expression.") String methodName,
      @CliOption(key = "roles", mandatory = false,
          help = "Comma separated list with all the roles to add inside 'hasAnyRole' instruction. "
              + "This option is mandatory if `--usernames` is not specified.") String roles,
      @CliOption(
          key = "usernames",
          mandatory = false,
          help = "Comma separated list with all the usernames to add inside Spring Security annotation. "
              + "This option is mandatory if `--roles` is not specified.") String usernames) {

    if (StringUtils.isEmpty(roles) && StringUtils.isEmpty(usernames)) {
      LOGGER
          .log(
              Level.INFO,
              "ERROR: You should provide almost one role on --roles parameter or almost one username on --usernames parameter..");
      return;
    }

    securityOperations.generateAuthorizeAnnotations(klass, methodName, roles, usernames);
  }

  @CliAvailabilityIndicator("security filtering")
  public boolean isFilteringOperationAvailable() {
    return projectOperations.isFeatureInstalled(FeatureNames.SECURITY);
  }

  @CliOptionAutocompleteIndicator(command = "security filtering", param = "class",
      help = "You must select a valid Service class", validate = true)
  public List<String> getAllServiceClassesAndInterfacesForFiltering(ShellContext context) {

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
      command = "security filtering",
      param = "method",
      help = "You must select a valid method from the provided class or a regular expression that match with existing methods",
      validate = false)
  public List<String> getAllMethodsRelatedWithProvidedClassForFiltering(ShellContext context) {

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

  @CliOptionAutocompleteIndicator(command = "security filtering", param = "when",
      help = "You must select " + PRE_FILTER + " to include @PreFilter or " + POST_FILTER
          + " to include @PostFilter", validate = true)
  public List<String> getWhenOptions(ShellContext context) {
    List<String> results = new ArrayList<String>();
    results.add(PRE_FILTER);
    results.add(POST_FILTER);
    return results;
  }

  @CliCommand(
      value = "security filtering",
      help = "Include `@PreFilter`/`@PostFilter` annotation to an specific method to filter results of a "
          + "method invocation based on an expression.")
  public void filterOperation(
      @CliOption(
          key = "class",
          mandatory = true,
          help = "The service class that contains the method to annotate. When working on a single module"
              + " project, simply specify the name of the class. If you consider it necessary, you can "
              + "also specify the package. Ex.: `--class ~.service.MyClass` (where `~` is the base "
              + "package). When working with multiple modules, you should specify the name of the class "
              + "and the module where it is. Ex.: `--class service:~.MyClass`. If the module is not "
              + "specified, it is assumed that the class is in the module which has the focus.") JavaType klass,
      @CliOption(
          key = "method",
          mandatory = true,
          help = "The service method name (including its params), that will be annotated with "
              + "`@PreFilter`/`@PostFilter`. Is possible to specify a regular expression. "
              + "Possible values are: any of the existing methods of the class specified in `--class` "
              + "option, or regular expression.") String methodName,
      @CliOption(key = "roles", mandatory = false,
          help = "Comma separated list with all the roles to add inside 'hasAnyRole' instruction. "
              + "This option is mandatory if `--usernames` is not specified.") String roles,
      @CliOption(
          key = "usernames",
          mandatory = false,
          help = "Comma separated list with all the usernames to add inside Spring Security annotation. "
              + "This option is mandatory if `--roles` is not specified.") String usernames,
      @CliOption(
          key = "when",
          mandatory = false,
          unspecifiedDefaultValue = PRE_FILTER,
          specifiedDefaultValue = PRE_FILTER,
          help = "Indicates if filtering should be after or before to execute the operation. Depends of "
              + "the specified value, `@PreFilter` annotation or `@PostFilter` annotation will be included. "
              + "Possible values are: `PRE` and `POST`." + "Default: `PRE`.") String when) {

    if (StringUtils.isEmpty(roles) && StringUtils.isEmpty(usernames)) {
      LOGGER
          .log(
              Level.INFO,
              "ERROR: You should provide almost one role on --roles parameter or almost one username on --usernames parameter..");
      return;
    }

    securityOperations.generateFilterAnnotations(klass, methodName, roles, usernames, when);
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

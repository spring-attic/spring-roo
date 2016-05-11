package org.springframework.roo.addon.pushin;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
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
 * Commands for the 'push-in' add-on to be used by the ROO shell.
 * 
 * This command marker will provide necessary operations to make push-in of all
 * methods, fields and annotations declared on ITDs.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PushInCommands implements CommandMarker {

  private static final Logger LOGGER = HandlerUtils.getLogger(PushInCommands.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private PushInOperations pushInOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  private Converter<JavaType> javaTypeConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  /**
   * Method that checks if push-in operation is available or not.
   * 
   * "push-in" command will be available only if some project was generated.
   * 
   * @return true if some project was created on focused directory.
   */
  @CliAvailabilityIndicator("push-in")
  public boolean isPushInCommandAvailable() {
    return pushInOperations.isPushInCommandAvailable();
  }

  /**
   * Method that checks visibility of --all parameter.
   * 
   * This parameter will be available only if --package, --class or --method 
   * parameter has not been specified before.
   * 
   * @param context
   *            ShellContext used to obtain specified parameters
   * @return true if --all parameter is visible, false if not.
   */
  @CliOptionVisibilityIndicator(
      command = "push-in",
      params = "all",
      help = "--all parameter is not available if --package, --class or --method parameter has been specified.")
  public boolean isAllParameterVisible(ShellContext context) {
    Map<String, String> specifiedParameters = context.getParameters();

    if (specifiedParameters.containsKey("package") || specifiedParameters.containsKey("class")
        || specifiedParameters.containsKey("method")) {
      return false;
    }

    return true;

  }

  /**
   * Method that checks visibility of --package, --class and --method parameters.
   * 
   * This parameter will be available only if --all parameter 
   * has not been specified before.
   * 
   * @param context
   *            ShellContext used to obtain specified parameters
   * @return true if parameters are visible, false if not.
   */
  @CliOptionVisibilityIndicator(
      command = "push-in",
      params = {"package", "class", "method"},
      help = "--package, --class and --method parameters are not available if --all parameter has been specified.")
  public boolean isOtherParametersVisible(ShellContext context) {
    Map<String, String> specifiedParameters = context.getParameters();

    if (specifiedParameters.containsKey("all")) {
      return false;
    }

    return true;
  }

  /**
   * Method that returns all defined methods for provided class on --class parameter.
   * 
   * @param context context
   *            ShellContext used to obtain specified parameters
   * @return List with available methods. Empty List if class has not been specified.
   */
  @CliOptionAutocompleteIndicator(command = "push-in", param = "method", validate = false,
      help = "Provides possible methods names if some class parameter has been specified")
  public List<String> getAllPossibleMethods(ShellContext context) {
    List<String> allPossibleMethods = new ArrayList<String>();

    // Getting all introduces parameters
    Map<String, String> specifiedParameters = context.getParameters();

    // Check if class parameter has been specified
    if (specifiedParameters.containsKey("class")) {
      String specifiedClass = specifiedParameters.get("class");
      JavaType klass =
          getJavaTypeConverter().convertFromText(specifiedClass, JavaType.class, PROJECT);

      // TODO: Class details should be cached to prevent load MemberDetails everytime. 
      // The problem is that if some element is cached, and then, new  method is added 
      // to .aj file, this parameter will not autocomplete it.
      MemberDetails klassDetails =
          memberDetailsScanner.getMemberDetails(getClass().getName(),
              typeLocationService.getTypeDetails(klass));

      if (klassDetails != null) {
        List<MethodMetadata> definedMethods = klassDetails.getMethods();

        for (MethodMetadata method : definedMethods) {
          // Check if method has been defined on current class and check
          // if current method has been pushed before.
          String declaredByMetadataID = method.getDeclaredByMetadataId();
          if (StringUtils.isNotBlank(declaredByMetadataID)
              && declaredByMetadataID.split("\\?").length > 1
              && declaredByMetadataID.split("\\#").length > 0
              && !declaredByMetadataID.split("\\#")[0]
                  .equals("MID:org.springframework.roo.classpath.PhysicalTypeIdentifier")
              && declaredByMetadataID.split("\\?")[1].equals(klass.getFullyQualifiedTypeName())) {
            allPossibleMethods.add(method.getMethodName().getSymbolName());
          }
        }
      }
    }

    return allPossibleMethods;
  }

  /**
   * Method that register "push-in" command on Spring Roo Shell.
   * 
   * Push-in all methods, fields, annotations, imports, extends, etc.. declared on 
   * ITDs to its .java files. You could specify --all parameter to apply push-in on every
   * component of generated project, or you could define package, class or method where wants 
   * to apply push-in.
   * 
   * @param all
   *            String that indicates if push-in process should be applied to entire project. All specified
   *            values will be ignored.
   * @param package 
   *            JavaPackage with the specified package where developers wants to make 
   *            push-in
   * @param klass
   *            JavaType with the specified class where developer wants to
   *            make push-in
   * @param method
   *            String with the specified name of the method that
   *            developer wants to push-in
   * @param shellContext
   *            ShellContext used to know if --force parameter has been used by developer
   *    
   */
  @CliCommand(
      value = "push-in",
      help = "Push-in all methods, fields, annotations, imports, extends, etc.. declared on  ITDs to its .java files. You could specify --all parameter to apply push-in on every component of generated project, or you could define package, class or method where wants to apply push-in.")
  public void pushIn(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "",
          help = "Parameter that indicates if push-in process should be applied to entire project. If specified, all the other parameters will be unavailable. It doesn't allow any value.") String all,
      @CliOption(key = "package", mandatory = false,
          help = "JavaPackage with the specified package where developers wants to make push-in") JavaPackage specifiedPackage,
      @CliOption(key = "class", mandatory = false,
          help = "JavaType with the specified class where developer wants to make push-in") final JavaType klass,
      @CliOption(
          key = "method",
          mandatory = false,
          help = "String with the specified name of the method that developer wants to push-in. You could use a Regular Expression to make push-in of more than one method on the same execution.") String method,
      ShellContext shellContext) {

    // Developer must specify at least one parameter
    if (all == null && specifiedPackage == null && klass == null && method == null) {
      LOGGER.log(Level.WARNING, "ERROR: You must specify at least one parameter. ");
      return;
    }

    // Check if all parameter contains value
    if (all != null && StringUtils.isNotEmpty(all)) {
      LOGGER.log(Level.WARNING, "ERROR: --all parameter doesn't allow any value.");
      return;
    }

    if (method == null && shellContext.getParameters().get("method") != null) {
      LOGGER
          .log(Level.WARNING,
              "ERROR: You must provide a valid value (method name or Regular Expression) on --method parameter.");
      return;
    }

    // Check if developer wants to apply push-in on every component of generated project
    if (all != null) {
      pushInOperations.pushInAll(shellContext.isForce());
    } else {
      pushInOperations.pushIn(specifiedPackage, klass, method);
    }

  }

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


}

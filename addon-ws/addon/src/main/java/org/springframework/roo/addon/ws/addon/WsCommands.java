package org.springframework.roo.addon.ws.addon;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.ws.annotations.SoapBindingType;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Provides commands to generate Web Service endpoints and Web Service clients.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class WsCommands implements CommandMarker {

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  /**
   * This method is an availability indicator of the Web Service commands. It indicates
   * if the command is available or not.
   *
   * Delegates in WSOperations to know if the command is available or not.
   * 
   * @return true if it is available and false if not.
   */
  @CliAvailabilityIndicator(value = {"ws client", "ws endpoint"})
  public boolean areWsCommandsAvailable() {
    return getWsOperations().areWsCommandsAvailable();
  }

  /**
   * This method is an autocomplete indicator of 'ws client' command.
   * 
   * It provides all existing .wsdl files inside /src/main/resources of
   * each existing module.
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(command = "ws client", param = "wsdl",
      help = "By default, Spring Roo searches .wsdl files "
          + "in the 'src/main/resources/' folder of the existing modules. "
          + "Include your .wsdl file in 'src/main/resources' to be able to generate" + " a client.")
  public List<String> getAllWsdlFiles(ShellContext context) {
    List<String> existingWsdlFiles = new ArrayList<String>();

    Collection<String> existingModules = getProjectOperations().getModuleNames();
    String rootPath = getPathResolver().getRoot();
    for (String moduleName : existingModules) {

      String moduleResourcesFolder =
          rootPath.concat("/").concat(moduleName).concat("/src/main/resources");

      // To prevent errors, check if src main resources folder exists in current module
      if (getFileManager().exists(moduleResourcesFolder)) {

        File folder = new File(moduleResourcesFolder);
        for (final File fileEntry : folder.listFiles()) {
          if (fileEntry.isFile() && fileEntry.getName().endsWith(".wsdl")) {
            String fileName = fileEntry.getName();
            if (StringUtils.isNotEmpty(moduleName)) {
              fileName = moduleName.concat(":").concat(fileName);
            }
            existingWsdlFiles.add(fileName);
          }
        }
      }
    }

    return existingWsdlFiles;
  }

  /**
   * This method is a visibility indicator of 'ws client' command.
   * 
   * It indicates if --endpoint parameter is visible or not, depending
   * of the command state.
   * 
   * @param context
   * @return true if endpoint parameter is visible
   */
  @CliOptionVisibilityIndicator(command = "ws client", params = "endpoint",
      help = "--endpoint parameter is not available if --wsdl parameter has not been specified")
  public boolean isEndPointParameterVisibleForClient(ShellContext context) {
    // Getting value of wsdlParameter
    String wsdlParameter = context.getParameters().get("wsdl");
    // Is wsdl value is empty, --endpoint parameter is not available
    if (StringUtils.isEmpty(wsdlParameter)) {
      return false;
    }
    return true;
  }

  /**
   * This method is an autocomplete indicator of 'ws client' command.
   * 
   * It provides all existing endpoints inside the provided .wsdl file.
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(
      command = "ws client",
      param = "endpoint",
      help = "--endpoint parameter should be completed with the attribute 'name' "
          + "of the 'port' element inside the 'service' element of the provided .wsdl file."
          + " If the provided .wsdl file doesn't exists in 'src/main/resources', this parameter will not be autocompleted."
          + " If the provided .wsdl exists and any --endpoint is autocompleted, check if your wsdl file has a valid format.")
  public List<String> getEndPointForProvidedWsdl(ShellContext context) {
    List<String> availableEndPoints = new ArrayList<String>();


    // First of all, get the provided .wsdl file
    String wsdlParam = context.getParameters().get("wsdl");

    // Check if the provided .wsdl file exists
    String wsdlLocation = getWsOperations().getWsdlAbsolutePathFromWsdlName(wsdlParam);

    // If exists, obtain the available enpoints
    if (getFileManager().exists(wsdlLocation)) {
      availableEndPoints.addAll(getWsOperations().getEndPointsFromWsdlFile(wsdlLocation));
    }

    return availableEndPoints;
  }

  /**
   * This method is a visibility indicator of 'ws client' command.
   * 
   * It indicates if --class parameter is visible or not, depending of the
   * command state.
   * 
   * @param context
   * @return true if class parameter is visible
   */
  @CliOptionVisibilityIndicator(command = "ws client", params = "class",
      help = "--class parameter is not available if --endpoint parameter has not been specified")
  public boolean isClassParameterVisibleForClient(ShellContext context) {
    // Getting value of endpoint
    String endPointParameter = context.getParameters().get("endpoint");
    // Is endpoint value is empty, --class parameter is not available
    if (StringUtils.isEmpty(endPointParameter)) {
      return false;
    }
    return true;
  }

  /**
   * This method is a visibility indicator of 'ws client' command.
   * 
   * It indicates if --binding parameter and --serviceUrl parameter
   * are visible or not, depending of the command state.
   * 
   * @param context
   * @return true if --binding and --serviceUrl parameter should be visible
   */
  @CliOptionVisibilityIndicator(
      command = "ws client",
      params = {"binding", "serviceUrl"},
      help = "`--binding` and `--serviceUrl` parameters are not available if `--class` parameter has not been specified")
  public boolean areBindingAndServiceUrlParameterVisibleForClient(ShellContext context) {
    // Getting value of class
    String classParameter = context.getParameters().get("class");
    // Is clas value is empty, --class parameter is not available
    if (StringUtils.isEmpty(classParameter)) {
      return false;
    }
    return true;
  }

  /**
   * This method defines the "ws client" command.
   * 
   * Delegates on WsOperations to create the new client and to install the necessary dependencies
   * and configuration properties.
   * 
   * @param wsdlLocation location of the .wsdl file
   * @param endpoint endpoint provided by the .wsdl file
   * @param class the configuration class that should include the method to define
   * 		the Web Service client
   * @param binding the binding type to be used.
   * @param serviceUrl the service URL to be used.
   * @param context provides the default global parameters --force and --profile
   */
  @CliCommand(value = "ws client",
      help = "Generates a new Web Service client by the provided WSDL file.")
  public void addWsClient(
      @CliOption(
          key = "wsdl",
          mandatory = true,
          help = "WSDL file located in some specific module. By default, Spring Roo searches .wsdl files "
              + "in the 'src/main/resources/' folder of the existing modules.") String wsdlLocation,
      @CliOption(
          key = "endpoint",
          mandatory = true,
          help = "Select some endpoint defined in the .wsdl file provided before. "
              + "This parameter will be autocompleted with the attribute 'name' of the 'port' element inside "
              + "the 'service' element.") String endPoint,
      @CliOption(key = "class", mandatory = true,
          help = "Configuration class that will include the method to define "
              + "the Web Service client. You could provide a new class.") JavaType configClass,
      @CliOption(key = "binding", mandatory = false,
          help = "The binding type to be used. You could choose between SOAP11 and SOAP12. "
              + "If not specified, it will be calculated using the .wsdl file namespace.") SoapBindingType bindingType,
      @CliOption(
          key = "serviceUrl",
          mandatory = false,
          help = "The service URL to be used. If This option is not specified, "
              + "default location provided by the .wsdl file will be used. This default location will be obtained from the 'location' "
              + "attribute of the 'address' element located inside the 'port' element provided in the '--endpoint' parameter.") String serviceUrl,
      ShellContext context) {

    // Delegates on WsOperations to create new client
    getWsOperations().addWsClient(wsdlLocation, endPoint, configClass, bindingType, serviceUrl,
        context.getProfile());

  }

  /**
   * This method is an autocomplete indicator of the 'ws endpoint' command.
   * 
   * This method provides all existing classes annotated with @RooService
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(
      command = "ws endpoint",
      param = "service",
      help = "--service parameter should be autocomplete with some existing class annotated with @RooService.")
  public List<String> existingServicesInterfaces(ShellContext context) {

    // Getting currentText
    String currentText = context.getParameters().get("service");

    List<String> existingServicesInterfaces = new ArrayList<String>();
    Set<ClassOrInterfaceTypeDetails> allServices =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);

    for (ClassOrInterfaceTypeDetails service : allServices) {
      String name = getClasspathOperations().replaceTopLevelPackageString(service, currentText);
      if (!existingServicesInterfaces.contains(name)) {
        existingServicesInterfaces.add(name);
      }
    }

    return existingServicesInterfaces;
  }

  /**
   * This method is a visibility indicator of 'sei' parameter from
   * the 'ws endpoint' command.
   * 
   * --sei parameter is not visible if --service parameter has not been specified
   * or if --service parameter has been specified with empty value
   * 
   * @param context
   * @return
   */
  @CliOptionVisibilityIndicator(command = "ws endpoint", params = "sei",
      help = "--sei parameter is not visible if --service parameter has not been specified "
          + "or if --service parameter has been specified with empty value.")
  public boolean isSEIParameterVisible(ShellContext context) {
    // Getting current value of --service parameter
    String serviceValue = context.getParameters().get("service");
    if (StringUtils.isEmpty(serviceValue)) {
      return false;
    }
    return true;
  }

  /**
   * This method is an autocomplete indicator for 'sei' parameter
   * of the 'ws endpoint' command.
   * 
   * It only provides the existing modules in the generated project
   * to make easy the SEI specification
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(command = "ws endpoint", param = "sei",
      help = "--sei parameter should be a new class. You must not provide an existing class.",
      validate = false)
  public List<String> getAllModulesForSei(ShellContext context) {

    List<String> availableModules = new ArrayList<String>();

    Collection<String> modules = getProjectOperations().getModuleNames();
    for (String moduleName : modules) {
      if (StringUtils.isNotBlank(moduleName)) {
        availableModules.add(moduleName.concat(":"));
      }
    }
    return availableModules;
  }

  /**
   * This method is a visibility indicator for --class and --config parameter
   * of the 'ws endpoint' command.
   * 
   * @param context
   * @return
   */
  @CliOptionVisibilityIndicator(
      command = "ws endpoint",
      params = {"class", "config"},
      help = "--class parameter and --config parameter are not available if --sei parameter has not been specified")
  public boolean areClassAndConfigVisible(ShellContext context) {
    // Getting current value of --sei parameter 
    String seiValue = context.getParameters().get("sei");
    if (StringUtils.isEmpty(seiValue)) {
      return false;
    }
    return true;
  }


  /**
   * This method is an autocomplete indicator for 'class' parameter
   * of the 'ws endpoint' command.
   * 
   * It only provides the existing modules in the generated project
   * to make easy the class specification
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(command = "ws endpoint", param = "class",
      help = "--class parameter should be a new class. You must not provide an existing class.",
      validate = false)
  public List<String> getAllModulesForClass(ShellContext context) {

    List<String> availableModules = new ArrayList<String>();

    Collection<String> modules = getProjectOperations().getModuleNames();
    for (String moduleName : modules) {
      if (StringUtils.isNotBlank(moduleName)) {
        availableModules.add(moduleName.concat(":"));
      }
    }
    return availableModules;
  }

  /**
   * This method defines the "ws endpoint" command.
   * 
   * Delegates on WsOperations to create the new SEIs and its implementarions. Also
   * install the necessary dependencies and configuration classes.
   * 
   * @param service JavaType annotated with @RooService
   * @param sei JavaType of the new interface to be generated
   * @param endpointClass JavaType of the new endpoint class to be generated
   * @param configClass JavaType with the existing or new configuration class to register the new
   * 		generated enpoint
   * @param context provides the profile to be used
   */
  @CliCommand(value = "ws endpoint",
      help = "Generates a new Service Endpoint Interface (SEI) and its implementation.")
  public void addSEI(
      @CliOption(key = "service", mandatory = true,
          help = "Existing service annotated with `@RooService` that will be used to generate "
              + "the new SEI. The new generated SEI will include all defined operations in "
              + "the provided service interface. "
              + "Possible values are: any of the project service classes, annotated "
              + "with `@RooService`") JavaType service,
      @CliOption(key = "sei", mandatory = true,
          help = "New Service Endpoint Interface to generate. It's not possible to indicate "
              + "an existing class.") JavaType sei,
      @CliOption(key = "class", mandatory = false,
          help = "New class that will implement the new generated SEI. If not specified, "
              + "a new implementation class will be generated in the same module using the "
              + "SEI name and the 'Endpoint' suffix.") JavaType endpointClass,
      @CliOption(
          key = "config",
          mandatory = false,
          help = "Configuration class that will register the new endpoint. You could specify an "
              + "existing `@Configuration` class or indicates a new one to be generated. If not specified, "
              + "a new `@Configuration` class will be generated in the same module using the "
              + "SEI name and the 'Configuration' suffix.") JavaType configClass,
      ShellContext context) {

    // Delegates on WsOperations to create new SEI
    getWsOperations().addSEI(service, sei, endpointClass, configClass, context.getProfile(),
        context.isForce());
  }

  // Obtaining OSGi services using ServiceInstaceManager utility

  public WsOperations getWsOperations() {
    return serviceInstaceManager.getServiceInstance(this, WsOperations.class);
  }

  public ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  public PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  public FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public ClasspathOperations getClasspathOperations() {
    return serviceInstaceManager.getServiceInstance(this, ClasspathOperations.class);
  }

}

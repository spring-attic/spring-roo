package org.springframework.roo.addon.jms;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.PRIVATE;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

/**
 * Provides JMS configuration operations.
 *
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Manuel Iborra
 * @since 1.0
 */
@Component
@Service
public class JmsOperationsImpl implements JmsOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(JmsOperations.class);

  private static final String JMS_PROPERTY_DESTINATION_NAME_PREFIX = "jms.destination.";
  //private static final String SPRINGLETS_JMS_PROPERTY_DESTINATION_NAME_PREFIX = "springlets.jms.destination.";
  private static final String JMS_PROPERTY_DESTINATION_NAME_SUFIX = ".jndi-name";
  private static final String JMS_VAR_DESTINATION_NAME_PREFIX = "destination";
  private static final String JMS_VAR_DESTINATION_NAME_SUFIX = "JndiName";
  private static final String JMS_PROPERTY_JNDI_NAME = "spring.jms.jndi-name";
  private static final String JNDI_PREFIX = "java:comp/env/";

  // Dependencies
  private static final Dependency DEPENDENCY_JMS = new Dependency("org.springframework",
      "spring-jms", null);
  private static final Dependency DEPENDENCY_SPRINGLETS_STARTER_JMS = new Dependency(
      "io.springlets", "springlets-boot-starter-jms", "${springlets.version}");

  private static final Dependency DEPENDENCY_SPRINGLETS_JMS = new Dependency("io.springlets",
      "springlets-jms", "${springlets.version}");

  // Properties
  private static final Property PROPERTY_SPRINGLETS_VERSION = new Property("springlets.version",
      "1.0.0.RELEASE");

  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
    this.serviceInstaceManager.activate(this.context);
  }

  public boolean isJmsInstallationPossible() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @Override
  public void addJmsReceiver(String destinationName, JavaType endpointService,
      String jndiConnectionFactory, String profile, boolean force) {

    boolean isApplicationModule = false;
    // Check that the module of the service is type application
    Collection<Pom> modules = getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
    for (Pom pom : modules) {
      if (pom.getModuleName().equals(endpointService.getModule())) {
        isApplicationModule = true;
      }
    }

    if (!isApplicationModule) {
      LOGGER
          .log(
              Level.SEVERE,
              String
                  .format("The module selected where JMS service will be configured must be of type application"));
      return;
    }

    // Check if Service already exists and --force is included
    final String serviceFilePathIdentifier =
        getPathResolver().getCanonicalPath(endpointService.getModule(), Path.SRC_MAIN_JAVA,
            endpointService);
    if (getFileManager().exists(serviceFilePathIdentifier) && force) {
      getFileManager().delete(serviceFilePathIdentifier);
    } else if (getFileManager().exists(serviceFilePathIdentifier)) {
      throw new IllegalArgumentException(String.format(
          "Endpoint '%s' already exists and cannot be created. Try to use a "
              + "different name on --endpoint parameter or use this command with '--force' "
              + "to overwrite the current service.", endpointService));
    }

    // Set destionation property name
    StringBuffer destinationNamePropertyName =
        new StringBuffer(JMS_PROPERTY_DESTINATION_NAME_PREFIX);
    destinationNamePropertyName.append(destinationName.replaceAll("/", "."));
    destinationNamePropertyName.append(JMS_PROPERTY_DESTINATION_NAME_SUFIX);

    // Set properties
    setProperties(destinationName, destinationNamePropertyName.toString(), jndiConnectionFactory,
        endpointService.getModule(), profile, force);

    // Create service
    createReceiverJmsService(endpointService, destinationNamePropertyName.toString());

    // Add jms dependecy in module
    getProjectOperations().addDependency(endpointService.getModule(), DEPENDENCY_JMS);

    // Add annotation @EnableJms to application class of the module
    Set<ClassOrInterfaceTypeDetails> applicationClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            SpringJavaType.SPRING_BOOT_APPLICATION);
    for (ClassOrInterfaceTypeDetails applicationClass : applicationClasses) {

      if (applicationClass.getType().getModule().equals(endpointService.getModule())) {

        // Check if annotation exists
        boolean annotationNotExists = true;
        for (AnnotationMetadata annotation : applicationClass.getAnnotations()) {
          if (annotation.getAnnotationType().equals(SpringJavaType.ENABLE_JMS)) {
            annotationNotExists = false;
            break;
          }
        }

        if (annotationNotExists) {
          ClassOrInterfaceTypeDetailsBuilder builder =
              new ClassOrInterfaceTypeDetailsBuilder(applicationClass);
          builder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.ENABLE_JMS));
          getTypeManagementService().createOrUpdateTypeOnDisk(builder.build());
        }
        break;
      }
    }
  }

  private void createReceiverJmsService(JavaType service, String destinationProperty) {
    // Create new service class
    final String serviceClassIdentifier =
        getPathResolver().getCanonicalPath(service.getModule(), Path.SRC_MAIN_JAVA, service);
    final String mid =
        PhysicalTypeIdentifier.createIdentifier(service,
            getPathResolver().getPath(serviceClassIdentifier));
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(mid, Modifier.PUBLIC, service,
            PhysicalTypeCategory.CLASS);

    // Create new @Service annotation
    AnnotationMetadataBuilder serviceAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.SERVICE);
    cidBuilder.addAnnotation(serviceAnnotation);

    // Add method receiveJmsMessage

    // @JmsListener(destination =
    // "${application.jms.queue.plaintext.jndi-name}")
    // public void receiveJmsMessage(String msg) {
    //
    // }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("receiveJmsMessage");

    // Define parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(JavaType.STRING));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("msg"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @JmsListener annotation
    AnnotationMetadataBuilder jmsListenerAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.JMS_LISTENER);
    jmsListenerAnnotation.addStringAttribute("destination", "${".concat(destinationProperty)
        .concat("}"));
    annotations.add(jmsListenerAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(" // To be implemented");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(mid, Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    cidBuilder.addMethod(methodBuilder);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

  }

  @Override
  public void addJmsSender(String destinationName, JavaType classSelected,
      String jndiConnectionFactory, String profile, boolean force) {

    // Check that module included in destionationName is an application module
    String module = "";
    String destination = "";
    if (getProjectOperations().isMultimoduleProject()) {
      Collection<String> moduleNames =
          getTypeLocationService().getModuleNames(ModuleFeatureName.APPLICATION);

      // if only have one module, select this, else check the parameter
      if (moduleNames.size() > 1) {

        // user select a module
        if (destinationName.contains(":")) {
          int charSeparation = destinationName.indexOf(":");
          if (charSeparation > 0 && destinationName.length() > charSeparation) {
            module = destinationName.substring(0, charSeparation);
            destination = destinationName.substring(charSeparation + 1, destinationName.length());
            if (!moduleNames.contains(module)) {

              // error, is necessary select an application module
              throw new IllegalArgumentException(
                  String
                      .format(
                          "Module '%s' must be of application type. Select one in --destinationName parameter",
                          module));
            }

          } else {

            // error, is necessary select an application module and destination
            throw new IllegalArgumentException(
                String
                    .format("--destinationName parameter must be composed by [application type module]:[destination] or focus module must be application type module and only write the destination name"));
          }
        } else {

          // module not selected, check if focus module is application type
          Pom focusedModule = getProjectOperations().getFocusedModule();
          if (getTypeLocationService().hasModuleFeature(focusedModule,
              ModuleFeatureName.APPLICATION)) {
            module = focusedModule.getModuleName();
            destination = destinationName;
          } else {
            throw new IllegalArgumentException(
                String
                    .format("--destinationName parameter must be composed by [application type module]:[destination] or focus module must be application type module and only write the destination name"));

          }
        }
      } else {

        if (moduleNames.isEmpty()) {

          // error, is necessary select an application module
          throw new IllegalArgumentException(
              String.format("Is necessary to have at least an application type module."));

        } else {
          module = moduleNames.iterator().next();
          destination = destinationName;
        }
      }
    } else {
      destination = destinationName;
    }

    // Add jms springlets dependecies
    getProjectOperations().addDependency(classSelected.getModule(), DEPENDENCY_SPRINGLETS_JMS);

    // Include springlets-boot-starter-mail in module
    getProjectOperations().addDependency(module, DEPENDENCY_SPRINGLETS_STARTER_JMS);

    // Include property version
    getProjectOperations().addProperty("", PROPERTY_SPRINGLETS_VERSION);

    // Add instance of springlets service to service defined by parameter
    // Add JavaMailSender to service
    final ClassOrInterfaceTypeDetails serviceTypeDetails =
        getTypeLocationService().getTypeDetails(classSelected);
    Validate.isTrue(serviceTypeDetails != null, "Cannot locate source for '%s'",
        classSelected.getFullyQualifiedTypeName());

    final String declaredByMetadataId = serviceTypeDetails.getDeclaredByMetadataId();
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(serviceTypeDetails);

    // Create service field
    cidBuilder.addField(new FieldMetadataBuilder(declaredByMetadataId, PRIVATE, Arrays
        .asList(new AnnotationMetadataBuilder(AUTOWIRED)), new JavaSymbolName("jmsSendingService"),
        SpringletsJavaType.SPRINGLETS_JMS_SENDING_SERVICE));

    // Set destionation property name
    StringBuffer destinationNamePropertyName =
        new StringBuffer(JMS_PROPERTY_DESTINATION_NAME_PREFIX);
    destinationNamePropertyName.append(destination.replaceAll("/", "."));
    destinationNamePropertyName.append(JMS_PROPERTY_DESTINATION_NAME_SUFIX);

    StringBuffer destionationNameVar = new StringBuffer(JMS_VAR_DESTINATION_NAME_PREFIX);
    if (destination.contains("/")) {

      // Delete char '/' and each word
      String[] destinationNameArray = destination.split("/");
      for (String destinationFragment : destinationNameArray) {
        destionationNameVar.append(StringUtils.capitalize(destinationFragment));
      }

    } else {
      destionationNameVar.append(StringUtils.capitalize(destination));
    }
    destionationNameVar.append(JMS_VAR_DESTINATION_NAME_SUFIX);

    // Adding @Value annotation
    AnnotationMetadataBuilder valueAnnotation = new AnnotationMetadataBuilder(SpringJavaType.VALUE);
    valueAnnotation.addStringAttribute("value", "${".concat(destinationNamePropertyName.toString())
        .concat("}"));

    // Add instance of destination name
    cidBuilder.addField(new FieldMetadataBuilder(declaredByMetadataId, PRIVATE, Arrays
        .asList(valueAnnotation), new JavaSymbolName(destionationNameVar.toString()),
        JavaType.STRING));

    // Write both, springlets service and destination instance
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    // Set properties
    setProperties(destination, destinationNamePropertyName.toString(), jndiConnectionFactory,
        module, profile, force);

  }

  /**
   * Set properties in corresponding properties profile file
   *
   * @param destinationName Value of destinationName property
   * @param destinationNamePropertyName Name of destinationName property
   * @param jndiConnectionFactory Value of 'spring.jms.jndi-name' property
   * @param module Application module where the properties file is located
   * @param profile The profile where the properties will be set
   * @param force Indicate if the properties will be overwritten
   */
  private void setProperties(String destinationName, String destinationNamePropertyName,
      String jndiConnectionFactory, String module, String profile, boolean force) {

    // Check and add profile properties
    if (jndiConnectionFactory != null) {
      if (getApplicationConfigService().existsSpringConfigFile(module, profile)) {
        String propertyJndiName =
            getApplicationConfigService().getProperty(module, JMS_PROPERTY_JNDI_NAME, profile);
        if (propertyJndiName != null && !force) {
          throw new IllegalArgumentException(
              String
                  .format("JNDI Connection Factory for JMS already exists and cannot be created. "
                      + "Using this command with '--force' will overwrite the current value."));
        }
      }

      // add 'java:comp/env' prefix to properties if don't have it.
      if (!jndiConnectionFactory.startsWith(JNDI_PREFIX)) {
        jndiConnectionFactory = JNDI_PREFIX.concat(jndiConnectionFactory);
      }

      getApplicationConfigService().addProperty(module, JMS_PROPERTY_JNDI_NAME,
          jndiConnectionFactory, profile, true);

      if (!destinationName.startsWith(JNDI_PREFIX)) {
        destinationName = JNDI_PREFIX.concat(destinationName);
      }
    }

    // Set property destinationName in file
    getApplicationConfigService().addProperty(module, destinationNamePropertyName, destinationName,
        profile, true);
  }

  // Methods to obtain OSGi Services
  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  private FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  private PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  private ApplicationConfigService getApplicationConfigService() {
    return serviceInstaceManager.getServiceInstance(this, ApplicationConfigService.class);
  }
}

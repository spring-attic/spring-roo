package org.springframework.roo.addon.jms;

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
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides JMS configuration operations.
 *
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class JmsOperationsImpl implements JmsOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(JmsOperations.class);

  private static final String JMS_PROPERTY_DESTINATION_NAME_PREFIX = "jms.destination.";
  private static final String JMS_PROPERTY_DESTINATION_NAME_SUFIX = ".name";
  private static final String JMS_PROPERTY_JNDI_NAME = "spring.jms.jndi-name";
  private static final String JNDI_PREFIX = "java:comp/env/";

  // Dependencies
  private static final Dependency DEPENDENCY_JMS = new Dependency("org.springframework",
      "spring-jms", null);

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
  public void addJmsReceiver(String destinationName, JavaType service,
      String jndiConnectionFactory, String profile, boolean force) {

    boolean isApplicationModule = false;
    // Check that the module of the service is type application
    Collection<Pom> modules = getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
    for (Pom pom : modules) {
      if (pom.getModuleName().equals(service.getModule())) {
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
        getPathResolver().getCanonicalPath(service.getModule(), Path.SRC_MAIN_JAVA, service);
    if (getFileManager().exists(serviceFilePathIdentifier) && force) {
      getFileManager().delete(serviceFilePathIdentifier);
    } else if (getFileManager().exists(serviceFilePathIdentifier)) {
      throw new IllegalArgumentException(String.format(
          "Service '%s' already exists and cannot be created. Try to use a "
              + "different name on --service parameter or use this command with '--force' "
              + "to overwrite the current service.", service));
    }

    // Transform properties values
    String destinationNamePropertyName =
        JMS_PROPERTY_DESTINATION_NAME_PREFIX.concat(destinationName.replaceAll("/", ".")).concat(
            JMS_PROPERTY_DESTINATION_NAME_SUFIX);

    // Check and add profile properties
    if (jndiConnectionFactory != null) {
      if (getApplicationConfigService().existsSpringConfigFile(service.getModule(), profile)) {
        String propertyJndiName =
            getApplicationConfigService().getProperty(service.getModule(), JMS_PROPERTY_JNDI_NAME,
                profile);
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
      getApplicationConfigService().addProperty(service.getModule(), JMS_PROPERTY_JNDI_NAME,
          jndiConnectionFactory, profile, true);

      if (!destinationName.startsWith(JNDI_PREFIX)) {
        destinationName = JNDI_PREFIX.concat(destinationName);
      }
    }

    // Set property destinationName in file
    getApplicationConfigService().addProperty(service.getModule(), destinationNamePropertyName,
        destinationName, profile, true);

    // Create service
    createReceiverJmsService(service, destinationNamePropertyName);

    // Add jms dependecy in module
    getProjectOperations().addDependency(service.getModule(), DEPENDENCY_JMS);

    // Add annotation @EnableJms to application class of the module
    Set<ClassOrInterfaceTypeDetails> applicationClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            SpringJavaType.SPRING_BOOT_APPLICATION);
    for (ClassOrInterfaceTypeDetails applicationClass : applicationClasses) {

      if (applicationClass.getType().getModule().equals(service.getModule())) {

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

    // Adding @PutMapping annotation
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
  public void addJmsSender(String name, Pom module, JavaType service, ShellContext shellContext) {
    // Add jms springlets dependecy

    // Check if service exist
    if (true) {

      // Add instance of service JMS
    } else {

      // Create method
    }

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

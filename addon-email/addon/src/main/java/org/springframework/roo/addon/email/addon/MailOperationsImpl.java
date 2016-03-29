package org.springframework.roo.addon.email.addon;

import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.MAIL_SENDER;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.email.annotations.RooSimpleMailMessageConfig;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Implementation of {@link MailOperationsImpl}.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class MailOperationsImpl implements MailOperations {


  private static final Logger LOGGER = HandlerUtils.getLogger(MailOperationsImpl.class);

  private static final String SPRING_BOOT_STARTER_MAIL = "spring-boot-starter-mail";
  private static final int PRIVATE_TRANSIENT = Modifier.PRIVATE | Modifier.TRANSIENT;
  private static final String TEMPLATE_MESSAGE_FIELD = "templateMessage";

  private BundleContext context;

  private PathResolver pathResolver;
  private ProjectOperations projectOperations;
  private PropFileOperations propFileOperations;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  @Override
  public boolean isEmailInstallationPossible() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @Override
  public boolean isManageEmailAvailable() {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled("email");
  }

  @Override
  public void installEmail(final String hostServer, final MailProtocol protocol, final String port,
      final String encoding, final String username, final String password) {
    Validate.notBlank(hostServer, "Host server name required");

    // Including Spring Boot Mail Starter
    includeSpringBootStarter(getProjectOperations().getFocusedModuleName());

    // Including Spring Boot configuration properties
    final Map<String, String> props = new HashMap<String, String>();

    if (StringUtils.isNotBlank(hostServer)) {
      props.put("spring.mail.host", hostServer);
    }

    if (protocol != null) {
      props.put("spring.mail.protocol", protocol.getProtocol());
    }

    if (StringUtils.isNotBlank(port)) {
      props.put("spring.mail.port", port);
    }

    if (StringUtils.isNotBlank(encoding)) {
      props.put("spring.mail.default-encoding", encoding);
    }

    if (StringUtils.isNotBlank(username)) {
      props.put("spring.mail.username", username);
    }

    if (StringUtils.isNotBlank(password)) {
      props.put("spring.mail.password", password);
    }

    /*        getPropFileOperations().addProperties(Path.SRC_MAIN_RESOURCES
                    .getModulePathId(getProjectOperations().getFocusedModuleName()),
                    "application.properties", props, true, true);*/
  }


  @Override
  public void configureTemplateMessage(String from, String subject) {

    // Generating template message 
    int modifier = Modifier.PUBLIC;
    JavaType target =
        new JavaType(getProjectOperations().getFocusedTopLevelPackage()
            .getFullyQualifiedPackageName().concat(".config.SimpleMailMessageConfig"));
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(target,
            getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA));
    File targetFile =
        new File(getTypeLocationService().getPhysicalTypeCanonicalPath(declaredByMetadataId));

    if (!targetFile.exists()) {
      // Prepare class builder
      final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, target,
              PhysicalTypeCategory.CLASS);

      // Including @Configuration annotation
      cidBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(
          "org.springframework.context.annotation.Configuration")));

      // Including @RooSimpleMailMessageConfig 
      cidBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(
          RooSimpleMailMessageConfig.class)));

      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    // Including Spring Boot configuration properties
    final Map<String, String> props = new HashMap<String, String>();

    if (StringUtils.isNotBlank(from)) {
      props.put("email.from", from);
    }

    if (StringUtils.isNotBlank(subject)) {
      props.put("email.subject", subject);
    }

    /*        getPropFileOperations().addProperties(Path.SRC_MAIN_RESOURCES
                    .getModulePathId(getProjectOperations().getFocusedModuleName()),
                    "application.properties", props, true, true);*/

  }

  @Override
  public void injectEmailTemplate(final JavaType targetType, final JavaSymbolName fieldName,
      final boolean async) {
    Validate.notNull(targetType, "Java type required");
    Validate.notNull(fieldName, "Field name required");

    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));

    // Obtain the physical type and its mutable class details
    final String declaredByMetadataId =
        getTypeLocationService().getPhysicalTypeIdentifier(targetType);
    final ClassOrInterfaceTypeDetails existing =
        getTypeLocationService().getTypeDetails(targetType);
    if (existing == null) {
      LOGGER.warning("Aborting: Unable to find metadata for target type '"
          + targetType.getFullyQualifiedTypeName() + "'");
      return;
    }
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(existing);

    // Add the MailSender field
    final FieldMetadataBuilder mailSenderFieldBuilder =
        new FieldMetadataBuilder(declaredByMetadataId, PRIVATE_TRANSIENT, annotations, fieldName,
            MAIL_SENDER);
    cidBuilder.addField(mailSenderFieldBuilder);

    // Add the "sendMessage" method
    cidBuilder.addMethod(getSendMethod(fieldName, async, declaredByMetadataId, cidBuilder));
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Generates the "send email" method to be added to the domain type
   * 
   * @param mailSenderName the name of the MailSender field (required)
   * @param async whether to send the email asynchronously
   * @param targetClassMID the MID of the class to receive the method
   * @param mutableTypeDetails the type to which the method is being added
   *            (required)
   * @return a non-<code>null</code> method
   */
  private MethodMetadataBuilder getSendMethod(final JavaSymbolName mailSenderName,
      final boolean async, final String targetClassMID,
      final ClassOrInterfaceTypeDetailsBuilder cidBuilder) {
    /*final String contextPath = getApplicationContextPath();
    final Document document = XmlUtils.readXml(fileManager
            .getInputStream(contextPath));
    final Element root = document.getDocumentElement();

    // Make a builder for the created method's body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Collect the types and names of the created method's parameters
    final PairList<AnnotatedJavaType, JavaSymbolName> parameters = new PairList<AnnotatedJavaType, JavaSymbolName>();

    if (getSimpleMailMessageBean(root) == null) {
        // There's no SimpleMailMessage bean; use a local variable
        bodyBuilder.appendFormalLine(SIMPLE_MAIL_MESSAGE
                .getFullyQualifiedTypeName()
                + " "
                + LOCAL_MESSAGE_VARIABLE
                + " = new "
                + SIMPLE_MAIL_MESSAGE.getFullyQualifiedTypeName() + "();");
        // Set the from address
        parameters.add(STRING, new JavaSymbolName("mailFrom"));
        bodyBuilder.appendFormalLine(LOCAL_MESSAGE_VARIABLE
                + ".setFrom(mailFrom);");
        // Set the subject
        parameters.add(STRING, new JavaSymbolName("subject"));
        bodyBuilder.appendFormalLine(LOCAL_MESSAGE_VARIABLE
                + ".setSubject(subject);");
    }
    else {
        // A SimpleMailMessage bean exists; auto-wire it into the entity and
        // use it as a template
        final List<AnnotationMetadataBuilder> smmAnnotations = Arrays
                .asList(new AnnotationMetadataBuilder(AUTOWIRED));
        final FieldMetadataBuilder smmFieldBuilder = new FieldMetadataBuilder(
                targetClassMID, PRIVATE_TRANSIENT, smmAnnotations,
                new JavaSymbolName(TEMPLATE_MESSAGE_FIELD),
                SIMPLE_MAIL_MESSAGE);
        cidBuilder.addField(smmFieldBuilder);
        // Use the injected bean as a template (for thread safety)
        bodyBuilder.appendFormalLine(SIMPLE_MAIL_MESSAGE
                .getFullyQualifiedTypeName()
                + " "
                + LOCAL_MESSAGE_VARIABLE
                + " = new "
                + SIMPLE_MAIL_MESSAGE.getFullyQualifiedTypeName()
                + "("
                + TEMPLATE_MESSAGE_FIELD + ");");
    }

    // Set the to address
    parameters.add(STRING, new JavaSymbolName("mailTo"));
    bodyBuilder
            .appendFormalLine(LOCAL_MESSAGE_VARIABLE + ".setTo(mailTo);");

    // Set the message body
    parameters.add(STRING, new JavaSymbolName("message"));
    bodyBuilder.appendFormalLine(LOCAL_MESSAGE_VARIABLE
            + ".setText(message);");

    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(mailSenderName + ".send("
            + LOCAL_MESSAGE_VARIABLE + ");");

    final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
            targetClassMID, Modifier.PUBLIC, new JavaSymbolName(
                    "sendMessage"), JavaType.VOID_PRIMITIVE,
            parameters.getKeys(), parameters.getValues(), bodyBuilder);

    if (async) {
        if (DomUtils.findFirstElementByName("task:annotation-driven", root) == null) {
            // Add asynchronous email support to the application
            if (StringUtils.isBlank(root.getAttribute("xmlns:task"))) {
                // Add the "task" namespace to the Spring config file
                root.setAttribute("xmlns:task", SPRING_TASK_NS);
                root.setAttribute("xsi:schemaLocation",
                        root.getAttribute("xsi:schemaLocation") + "  "
                                + SPRING_TASK_NS + " " + SPRING_TASK_XSD);
            }
            root.appendChild(new XmlElementBuilder(
                    "task:annotation-driven", document).addAttribute(
                    "executor", "asyncExecutor").build());
            root.appendChild(new XmlElementBuilder("task:executor",
                    document).addAttribute("id", "asyncExecutor")
                    .addAttribute("pool-size", "${executor.poolSize}")
                    .build());
            // Write out the new Spring config file
            fileManager.createOrUpdateTextFileIfRequired(contextPath,
                    XmlUtils.nodeToString(document), false);
            // Update the email properties file
            propFileOperations.addPropertyIfNotExists(
                    pathResolver.getFocusedPath(Path.SPRING_CONFIG_ROOT),
                    "email.properties", "executor.poolSize", "10", true);
        }
        methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
    }
    return methodBuilder;*/
    return null;
  }

  /**
   * Method to include Spring Boot Mail starter
   * 
   * @param moduleName
   */
  private void includeSpringBootStarter(final String moduleName) {
    final Element configuration = XmlUtils.getConfiguration(getClass());

    final List<Dependency> dependencies = new ArrayList<Dependency>();
    final List<Element> emailDependencies =
        XmlUtils.findElements("/configuration/email/dependencies/dependency", configuration);
    for (final Element dependencyElement : emailDependencies) {
      dependencies.add(new Dependency(dependencyElement));
    }
    getProjectOperations().addDependencies(moduleName, dependencies);
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) context.getService(ref);
          return pathResolver;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on MailOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) context.getService(ref);
          return projectOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on MailOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public PropFileOperations getPropFileOperations() {
    if (propFileOperations == null) {
      // Get all Services implement PropFileOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(PropFileOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          propFileOperations = (PropFileOperations) context.getService(ref);
          return propFileOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PropFileOperations on MailOperationsImpl.");
        return null;
      }
    } else {
      return propFileOperations;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) context.getService(ref);
          return typeLocationService;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on MailOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) context.getService(ref);
          return typeManagementService;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on MailOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }


  // FEATURE METHODS

  @Override
  public String getName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    Pom currentPom = getProjectOperations().getPomFromModuleName(moduleName);
    Set<Dependency> dependencies = currentPom.getDependencies();
    Iterator<Dependency> it = dependencies.iterator();
    while (it.hasNext()) {
      Dependency dependency = it.next();
      if (dependency.getArtifactId().equals(SPRING_BOOT_STARTER_MAIL)) {
        return true;
      }
    }
    return false;
  }

}

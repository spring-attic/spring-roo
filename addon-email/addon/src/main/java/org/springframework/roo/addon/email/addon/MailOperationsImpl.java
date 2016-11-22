package org.springframework.roo.addon.email.addon;

import static java.lang.reflect.Modifier.PRIVATE;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import org.springframework.roo.project.Property;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link MailOperationsImpl}.
 *
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Manuel Iborra
 * @since 1.0
 */
@Component
@Service
public class MailOperationsImpl implements MailOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(MailOperationsImpl.class);

  // Dependencies
  private static final Dependency DEPENDENCY_SPRING_CONTEXT_SUPPORT = new Dependency(
      "org.springframework", "spring-context-support", null);

  private static final Dependency DEPENDENCY_SPRING_BOOT_STARTER_MAIL = new Dependency(
      "org.springframework.boot", "spring-boot-starter-mail", null);

  private static final Dependency DEPENDENCY_SPRINGLETS_STARTER_MAIL = new Dependency(
      "io.springlets", "springlets-boot-starter-mail", "${springlets.version}");

  private static final Dependency DEPENDENCY_SPRINGLETS_MAIL = new Dependency("io.springlets",
      "springlets-mail", "${springlets.version}");

  // Properties
  private static final Property PROPERTY_SPRINGLETS_VERSION = new Property("springlets.version",
      "1.0.0.RELEASE");

  private static final String SEND_MAIL_PREFIX = "spring.mail";

  private static final String RECEIVE_MAIL_PREFIX = "springlets.mail.receiver";

  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
    this.serviceInstaceManager.activate(this.context);
  }

  @Override
  public boolean isEmailInstallationPossible() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @Override
  public void installSendEmailSupport(String host, String port, String protocol, String username,
      String password, Boolean starttls, String jndiName, String profile, Pom module,
      JavaType service, boolean force) {

    // Include spring-boot-starter-mail in module
    getProjectOperations().addDependency(module.getModuleName(),
        DEPENDENCY_SPRING_BOOT_STARTER_MAIL);

    // check if exists any property
    if (getApplicationConfigService().existsSpringConfigFile(module.getModuleName(), profile)
        && areDefinedSendEmailProperties(module.getModuleName(), profile) && !force) {

      // Send error message to user. He needs to set --force parameter
      String profileStr = "profile " + profile;
      if (profile == null) {
        profileStr = "default profile";
      }

      String moduleStr = " and module " + module.getModuleName();
      if (StringUtils.isEmpty(module.getModuleName())) {
        moduleStr = "";
      }
      LOGGER.log(Level.INFO, String.format("There are defined the mail properties to %s"
          + "%s. Using this command with '--force' " + "will overwrite the current values.",
          profileStr, moduleStr));

    } else {
      Map<String, String> propertiesFormattedToInsert =
          getSendEmailPropertiesFormattedToInsert(host, port, protocol, username, password,
              starttls, jndiName);

      // Set properties in file
      getApplicationConfigService().addProperties(module.getModuleName(),
          propertiesFormattedToInsert, profile, force);
    }

    if (service != null) {

      // Add dependency spring-context-support to the module of the selected Service
      getProjectOperations().addDependency(service.getModule(), DEPENDENCY_SPRING_CONTEXT_SUPPORT);

      // Add JavaMailSender to service
      final ClassOrInterfaceTypeDetails serviceTypeDetails =
          getTypeLocationService().getTypeDetails(service);
      Validate.isTrue(serviceTypeDetails != null, "Cannot locate source for '%s'",
          service.getFullyQualifiedTypeName());

      final String declaredByMetadataId = serviceTypeDetails.getDeclaredByMetadataId();
      final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(serviceTypeDetails);

      // Create the field
      cidBuilder.addField(new FieldMetadataBuilder(declaredByMetadataId, PRIVATE, Arrays
          .asList(new AnnotationMetadataBuilder(AUTOWIRED)), new JavaSymbolName("mailSender"),
          SpringJavaType.JAVA_MAIL_SENDER));

      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }
  }

  private Map<String, String> getSendEmailPropertiesFormattedToInsert(String host, String port,
      String protocol, String username, String password, Boolean starttls, String jndiName) {
    String starttlsStr = String.valueOf(starttls);
    if (starttls == null) {
      starttlsStr = null;
    }

    Map<String, String> props = new HashMap<String, String>();
    props.put("spring.mail.host", StringUtils.stripToEmpty(host));
    props.put("spring.mail.port", StringUtils.stripToEmpty(port));
    props.put("spring.mail.protocol", StringUtils.stripToEmpty(protocol));
    props.put("spring.mail.username", StringUtils.stripToEmpty(username));
    props.put("spring.mail.password", StringUtils.stripToEmpty(password));
    props.put("spring.mail.properties.mail.smtp.starttls.enable",
        StringUtils.stripToEmpty(starttlsStr));
    props.put("spring.mail.jndi-name", StringUtils.stripToEmpty(jndiName));
    return props;
  }

  @Override
  public void installReceiveEmailSupport(String host, String port, String protocol,
      String username, String password, Boolean starttls, String jndiName, String profile,
      Pom module, JavaType service, boolean force) {

    // Include springlets-boot-starter-mail in module
    getProjectOperations()
        .addDependency(module.getModuleName(), DEPENDENCY_SPRINGLETS_STARTER_MAIL);

    // Include property version
    getProjectOperations().addProperty("", PROPERTY_SPRINGLETS_VERSION);

    // check if exists any property
    if (getApplicationConfigService().existsSpringConfigFile(module.getModuleName(), profile)
        && areDefinedReceiveEmailProperties(module.getModuleName(), profile) && !force) {

      // Send error message to user. He needs to set --force parameter
      String profileStr = "profile " + profile;
      if (profile == null) {
        profileStr = "default profile";
      }

      String moduleStr = " and module " + module.getModuleName();
      if (StringUtils.isEmpty(module.getModuleName())) {
        moduleStr = "";
      }
      LOGGER.log(Level.INFO, String.format("There are defined the mail properties to %s"
          + "%s. Using this command with '--force' " + "will overwrite the current values.",
          profileStr, moduleStr));

    } else {
      Map<String, String> propertiesFormattedToInsert =
          getReceiveEmailPropertiesFormattedToInsert(host, port, protocol, username, password,
              starttls, jndiName);

      // Set properties in file
      getApplicationConfigService().addProperties(module.getModuleName(),
          propertiesFormattedToInsert, profile, force);
    }

    if (service != null) {

      // Add dependency springlets-mail to the module of the selected Service
      if (!service.getModule().equals(StringUtils.EMPTY)) {
        getProjectOperations().addDependency(service.getModule(), DEPENDENCY_SPRINGLETS_MAIL);
      }

      // Add MailReceiverService to service
      final ClassOrInterfaceTypeDetails serviceTypeDetails =
          getTypeLocationService().getTypeDetails(service);
      Validate.isTrue(serviceTypeDetails != null, "Cannot locate source for '%s'",
          service.getFullyQualifiedTypeName());

      final String declaredByMetadataId = serviceTypeDetails.getDeclaredByMetadataId();
      final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(serviceTypeDetails);

      // Create the field
      cidBuilder.addField(new FieldMetadataBuilder(declaredByMetadataId, PRIVATE, Arrays
          .asList(new AnnotationMetadataBuilder(AUTOWIRED)), new JavaSymbolName("mailReceiver"),
          SpringletsJavaType.SPRINGLETS_MAIL_RECEIVER_SERVICE));

      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }
  }

  private Map<String, String> getReceiveEmailPropertiesFormattedToInsert(String host, String port,
      String protocol, String username, String password, Boolean starttls, String jndiName) {
    String starttlsStr = String.valueOf(starttls);
    if (starttls == null) {
      starttlsStr = null;
    }

    Map<String, String> props = new HashMap<String, String>();
    props.put("springlets.mail.receiver.host", StringUtils.stripToEmpty(host));
    props.put("springlets.mail.receiver.port", StringUtils.stripToEmpty(port));
    props.put("springlets.mail.receiver.protocol", StringUtils.stripToEmpty(protocol));
    props.put("springlets.mail.receiver.username", StringUtils.stripToEmpty(username));
    props.put("springlets.mail.receiver.password", StringUtils.stripToEmpty(password));
    props.put("springlets.mail.receiver.starttls-enable", StringUtils.stripToEmpty(starttlsStr));
    props.put("springlets.mail.receiver.jndi-name", StringUtils.stripToEmpty(jndiName));
    return props;
  }

  private ApplicationConfigService getApplicationConfigService() {
    return serviceInstaceManager.getServiceInstance(this, ApplicationConfigService.class);
  }

  private boolean areDefinedReceiveEmailProperties(String moduleName, String profile) {
    return !getApplicationConfigService().getPropertyKeys(moduleName, RECEIVE_MAIL_PREFIX, true,
        profile).isEmpty();
  }

  private boolean areDefinedSendEmailProperties(String moduleName, String profile) {
    return !getApplicationConfigService().getPropertyKeys(moduleName, SEND_MAIL_PREFIX, true,
        profile).isEmpty();
  }

  public List<String> getAllServiceImpl(String currentService) {

    List<String> allPossibleValues = new ArrayList<String>();

    Collection<ClassOrInterfaceTypeDetails> services =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE_IMPL);

    for (ClassOrInterfaceTypeDetails service : services) {
      String replacedValue = replaceTopLevelPackageString(service, currentService);
      allPossibleValues.add(replacedValue);
    }

    if (allPossibleValues.isEmpty()) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format("INFO: There aren't services availables. Use 'service' commands to generate any."));
      allPossibleValues.add("");
    }

    return allPossibleValues;
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
        && !cid.getType().getModule().equals(getProjectOperations().getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(getProjectOperations().getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          getProjectOperations().getFocusedTopLevelPackage().getFullyQualifiedPackageName();
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

  // FEATURE METHODS

  @Override
  public String getName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    Pom currentPom = getProjectOperations().getPomFromModuleName(moduleName);
    List<Dependency> dependencies = new ArrayList<Dependency>();
    dependencies.add(DEPENDENCY_SPRING_BOOT_STARTER_MAIL);
    dependencies.add(DEPENDENCY_SPRINGLETS_STARTER_MAIL);
    return currentPom.isAllDependenciesRegistered(dependencies);
  }

}

package org.springframework.roo.addon.web.mvc.i18n;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.i18n.components.I18n;
import org.springframework.roo.addon.web.mvc.i18n.components.I18nSupport;
import org.springframework.roo.addon.web.mvc.i18n.languages.EnglishLanguage;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.XmlUtils;

/**
 * Implementation of {@link I18nOperations}.
 * 
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class I18nOperationsImpl implements I18nOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(I18nOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  @Override
  public boolean isInstallLanguageCommandAvailable() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  @Override
  public void installLanguage(final I18n language, final boolean useAsDefault, final Pom module) {

    // Check if provided module match with application modules features
    Validate.isTrue(getTypeLocationService()
        .hasModuleFeature(module, ModuleFeatureName.APPLICATION),
        "ERROR: Provided module doesn't match with application modules features. "
            + "Execute this operation again and provide a valid application module.");

    Validate.notNull(language, "ERROR: You should provide a valid language code.");

    if (language.getLocale() == null) {
      LOGGER.warning("ERROR: Provided language is not valid.");
      return;
    }

    final LogicalPath resourcesPath =
        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, module.getModuleName());

    final String targetDirectory = getPathResolver().getIdentifier(resourcesPath, "");

    // Getting message.properties file
    String messageBundle = "";

    if (language.getLocale().equals(Locale.ENGLISH)) {
      messageBundle = targetDirectory + "messages.properties";
    } else {
      messageBundle =
          targetDirectory.concat("messages_").concat(
              language.getLocale().getLanguage().concat(".properties"));
    }

    if (!getFileManager().exists(messageBundle)) {
      InputStream inputStream = null;
      OutputStream outputStream = null;
      try {
        inputStream = language.getMessageBundle();
        outputStream = getFileManager().createFile(messageBundle).getOutputStream();
        IOUtils.copy(inputStream, outputStream);
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Encountered an error during copying of message bundle MVC JSP addon.", e);
      } finally {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
      }
    }

    // Install flag
    final String flagGraphic =
        targetDirectory.concat("static/public/img/").concat(language.getLocale().getLanguage())
            .concat(".png");
    if (!getFileManager().exists(flagGraphic)) {
      InputStream inputStream = null;
      OutputStream outputStream = null;
      try {
        inputStream = language.getFlagGraphic();
        outputStream = getFileManager().createFile(flagGraphic).getOutputStream();
        IOUtils.copy(inputStream, outputStream);
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Encountered an error during copying of flag graphic for MVC JSP addon.", e);
      } finally {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
      }
    }

    // Update @WebMvcConfiguration annotation defining defaultLanguage
    // attribute
    if (useAsDefault) {

      // Obtain all existing configuration classes annotated with
      // @RooWebMvcConfiguration
      Set<ClassOrInterfaceTypeDetails> configurationClasses =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              RooJavaType.ROO_WEB_MVC_CONFIGURATION);

      for (ClassOrInterfaceTypeDetails configurationClass : configurationClasses) {
        // If configuration class is located in the provided module
        if (configurationClass.getType().getModule().equals(module.getModuleName())) {
          ClassOrInterfaceTypeDetailsBuilder cidBuilder =
              new ClassOrInterfaceTypeDetailsBuilder(configurationClass);
          AnnotationMetadataBuilder annotation =
              cidBuilder.getDeclaredTypeAnnotation(RooJavaType.ROO_WEB_MVC_CONFIGURATION);
          annotation.addStringAttribute("defaultLanguage", language.getLocale().getLanguage());

          // Update configuration class
          getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

        }
      }

      LOGGER.log(
          Level.INFO,
          String.format("INFO: Default language of your project has been changed to %s.",
              language.getLanguage()));

    }

    // Get response types for updating views with new language
    List<ControllerMVCResponseService> responseTypes = getControllerMVCResponseTypes(true);
    for (ControllerMVCResponseService responseType : responseTypes) {
      JavaType responseAnnotationType = responseType.getAnnotation();
      if (responseAnnotationType != null) {
        Set<ClassOrInterfaceTypeDetails> responseTypeClassesDetails =
            getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
                responseAnnotationType);

        // We only need one class to get Metadata
        for (ClassOrInterfaceTypeDetails responseTypeClassDetails : responseTypeClassesDetails) {
          MemberDetails responseTypDetails =
              getMemberDetailsScanner().getMemberDetails(getClass().getName(),
                  responseTypeClassDetails);
          if (responseTypDetails != null) {
            List<MemberHoldingTypeDetails> details = responseTypDetails.getDetails();
            for (MemberHoldingTypeDetails detail : details) {
              getMetadataService().evictAndGet(detail.getDeclaredByMetadataId());
            }
            break;
          }
        }
      }
    }

    // Get all controllers and update its message bundles
    Set<ClassOrInterfaceTypeDetails> controllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);
    for (ClassOrInterfaceTypeDetails controller : controllers) {
      AnnotationMetadata controllerAnnotation =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
      if (controllerAnnotation.getAttribute("entity") != null) {
        JavaType entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();
        ClassOrInterfaceTypeDetails entityCid = getTypeLocationService().getTypeDetails(entity);
        MemberDetails entityDetails =
            getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), entityCid);
        updateI18n(entityDetails, entity, controller.getType().getModule());
      }
    }

    // Add application property
    getApplicationConfigService().addProperty(module.getModuleName(),
        "spring.messages.fallback-to-system-locale", "false", "", true);
  }

  /**
   * Adds the entity properties as labels into i18n messages file
   * 
   * @param entityDetails
   *            MemberDetails where entity properties are defined
   * @param entity
   *            JavaType representing the entity binded to controller
   */
  public void updateI18n(MemberDetails entityDetails, JavaType entity, String moduleName) {

    final Map<String, String> properties = new LinkedHashMap<String, String>();

    final LogicalPath resourcesPath = LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, moduleName);
    final String targetDirectory = getPathResolver().getIdentifier(resourcesPath, "");

    final String entityName = entity.getSimpleTypeName();

    properties.put(buildLabel(entityName), new JavaSymbolName(entity.getSimpleTypeName()
        .toLowerCase()).getReadableSymbolName());

    final String pluralResourceId = buildLabel(entity.getSimpleTypeName(), "plural");
    final String plural = getPluralService().getPlural(entity);
    properties.put(pluralResourceId, new JavaSymbolName(plural).getReadableSymbolName());

    final List<FieldMetadata> javaTypePersistenceMetadataDetails =
        getPersistenceMemberLocator().getIdentifierFields(entity);

    if (!javaTypePersistenceMetadataDetails.isEmpty()) {
      for (final FieldMetadata idField : javaTypePersistenceMetadataDetails) {
        properties.put(buildLabel(entityName, idField.getFieldName().getSymbolName()), idField
            .getFieldName().getReadableSymbolName());
      }
    }

    for (final FieldMetadata field : entityDetails.getFields()) {
      final String fieldResourceId = buildLabel(entityName, field.getFieldName().getSymbolName());

      properties.put(fieldResourceId, field.getFieldName().getReadableSymbolName());

      // Add related entity fields
      if (field.getFieldType().getFullyQualifiedTypeName().equals(Set.class.getName())
          || field.getFieldType().getFullyQualifiedTypeName().equals(List.class.getName())) {

        // Getting inner type
        JavaType referencedEntity = field.getFieldType().getBaseType();

        ClassOrInterfaceTypeDetails referencedEntityDetails =
            getTypeLocationService().getTypeDetails(referencedEntity);

        if (referencedEntityDetails != null
            && referencedEntityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {

          for (final FieldMetadata referencedEntityField : getMemberDetailsScanner()
              .getMemberDetails(this.getClass().getName(), referencedEntityDetails).getFields()) {

            final String referenceEntityFieldResourceId =
                buildLabel(entityName, field.getFieldName().getSymbolName(), referencedEntityField
                    .getFieldName().getSymbolName());
            properties.put(referenceEntityFieldResourceId, referencedEntityField.getFieldName()
                .getReadableSymbolName());
          }
        }
      }
    }

    // Update messages bundles of each installed language
    Set<I18n> supportedLanguages = getI18nSupport().getSupportedLanguages();
    for (I18n i18n : supportedLanguages) {
      String messageBundle =
          String.format("messages_%s.properties", i18n.getLocale().getLanguage());
      String bundlePath =
          String.format("%s%s%s", targetDirectory, AntPathMatcher.DEFAULT_PATH_SEPARATOR,
              messageBundle);

      if (getFileManager().exists(bundlePath)) {
        getPropFilesManager().addProperties(resourcesPath, messageBundle, properties, true, false);
      }
    }

    // Allways update english message bundles
    getPropFilesManager().addProperties(resourcesPath, "messages.properties", properties, true,
        false);
  }

  /**
   * Builds the label of the specified field by joining its names and adding
   * it to the entity label
   * 
   * @param entity
   *            the entity name
   * @param fieldNames
   *            list of fields
   * @return label
   */
  private static String buildLabel(String entityName, String... fieldNames) {
    String label = XmlUtils.convertId("label." + entityName.toLowerCase());

    for (String fieldName : fieldNames) {
      label = XmlUtils.convertId(label.concat(".").concat(fieldName.toLowerCase()));
    }
    return label;
  }

  /**
   * Return a list of installed languages in the provided application module.
   * 
   * @param moduleName
   *            the module name to search for installed languages.
   * @return a list with the available languages.
   */
  public List<I18n> getInstalledLanguages(String moduleName) {

    final LogicalPath resourcesPath = LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, moduleName);
    final String targetDirectory = getPathResolver().getIdentifier(resourcesPath, "");

    // Create list for installed languages
    List<I18n> installedLanguages = new ArrayList<I18n>();

    // Get all available languages
    Set<I18n> supportedLanguages = getI18nSupport().getSupportedLanguages();
    for (I18n i18n : supportedLanguages) {
      String messageBundle =
          String.format("messages_%s.properties", i18n.getLocale().getLanguage());
      String bundlePath =
          String.format("%s%s%s", targetDirectory, AntPathMatcher.DEFAULT_PATH_SEPARATOR,
              messageBundle);

      if (getFileManager().exists(bundlePath)) {
        installedLanguages.add(i18n);
      }
    }

    // Always add english language as default
    installedLanguages.add(new EnglishLanguage());

    return installedLanguages;
  }

  /**
   * This method gets all implementations of ControllerMVCResponseService
   * interface to be able to locate all ControllerMVCResponseService. Uses
   * param installed to obtain only the installed or not installed response
   * types.
   * 
   * @param installed
   *            indicates if returned responseType should be installed or not.
   * 
   * @return Map with responseTypes identifier and the
   *         ControllerMVCResponseService implementation
   */
  private List<ControllerMVCResponseService> getControllerMVCResponseTypes(boolean installed) {
    List<ControllerMVCResponseService> responseTypes =
        new ArrayList<ControllerMVCResponseService>();

    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(ControllerMVCResponseService.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        ControllerMVCResponseService responseTypeService =
            (ControllerMVCResponseService) this.context.getService(ref);
        boolean isAbleToInstall = false;
        for (Pom module : getProjectOperations().getPoms()) {
          if (responseTypeService.isInstalledInModule(module.getModuleName()) == installed) {
            isAbleToInstall = true;
            break;
          }
        }
        if (isAbleToInstall) {
          responseTypes.add(responseTypeService);
        }
      }
      return responseTypes;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load ControllerMVCResponseService on I18nOperationsImpl.");
      return null;
    }
  }

  // Get OSGi services

  private TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private I18nSupport getI18nSupport() {
    return serviceInstaceManager.getServiceInstance(this, I18nSupport.class);
  }

  private PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  private FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private PropFilesManagerService getPropFilesManager() {
    return serviceInstaceManager.getServiceInstance(this, PropFilesManagerService.class);
  }

  private PersistenceMemberLocator getPersistenceMemberLocator() {
    return serviceInstaceManager.getServiceInstance(this, PersistenceMemberLocator.class);
  }

  private MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  public ApplicationConfigService getApplicationConfigService() {
    return serviceInstaceManager.getServiceInstance(this, ApplicationConfigService.class);
  }

  public MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

  public TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  public PluralService getPluralService() {
    return serviceInstaceManager.getServiceInstance(this, PluralService.class);
  }

}

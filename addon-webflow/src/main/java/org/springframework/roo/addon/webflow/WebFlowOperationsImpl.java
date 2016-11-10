package org.springframework.roo.addon.webflow;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.annotations.RooJavaBean;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.i18n.components.I18n;
import org.springframework.roo.addon.web.mvc.i18n.components.I18nSupport;
import org.springframework.roo.addon.web.mvc.i18n.languages.EnglishLanguage;
import org.springframework.roo.addon.web.mvc.thymeleaf.addon.ThymeleafMVCViewResponseService;
import org.springframework.roo.addon.web.mvc.thymeleaf.addon.ThymeleafMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Web Flow configuration operations.
 * 
 * @author Stefan Schmidt
 * @author Rossen Stoyanchev
 * @author Sergio Clares
 * 
 * @since 1.0
 */
@Component
@Service
public class WebFlowOperationsImpl extends AbstractOperations implements WebFlowOperations {

  private static final Dependency SPRINGLETS_WEBFLOW_STARTER = new Dependency("io.springlets",
      "springlets-boot-starter-webflow", "${springlets.version}");

  /**
   * Get a reference to ProjectOperations to be able to manage generated project.
   */
  @Reference
  private ProjectOperations projectOperations;

  /**
   * Get a reference to PathResolver to be able to resolve resources directory.
   */
  @Reference
  private PathResolver pathResolver;

  /**
   * Get a reference to I18nSupport to be able to get Locale codes.
   */
  @Reference
  private I18nSupport i18nSupport;

  /**
   * Get a reference to PropFilesManagerService to be able to manage locale message bundles.
   */
  @Reference
  private PropFilesManagerService propFilesManagerService;

  /**
   * Instantiate ServiceInstanceManager for an easy acces to Spring Roo services. 
   * It should be activated from context. 
   */
  private ServiceInstaceManager serviceManager = new ServiceInstaceManager();

  private String flowName = "";

  /**
   * The activate method for this OSGi component, this will be called by the OSGi 
   * container upon bundle activation (result of the 'addon install' command).
   * 
   * @param context the component context can be used to get access to the OSGi 
   * container (ie find out if certain bundles are active).
   */
  protected void activate(ComponentContext context) {
    super.activate(context);
    this.serviceManager.activate(this.context);
  }

  /**
   * See {@link WebFlowOperations#installWebFlow(String, String)}.
   */
  @Override
  public void installWebFlow(final String flowName, final String moduleName, JavaType klass) {
    this.flowName = flowName.toLowerCase();

    // Add WebFlow project configuration
    installWebFlowConfiguration(moduleName);

    String targetDirectory =
        pathResolver.getIdentifier(moduleName, Path.SRC_MAIN_RESOURCES,
            "/templates/".concat(this.flowName));
    if (fileManager.exists(targetDirectory)) {
      throw new IllegalStateException("Flow directory already exists: " + targetDirectory);
    }

    // Copy Web Flow template views and *-flow.xml to project
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("__WEBFLOW-ID__", this.flowName);
    copyDirectoryContents("*.html", targetDirectory, replacements, klass);
    createWebFlowFromTemplate(targetDirectory, replacements, klass);

    // Add localized messages for Web Flow labels
    addLocalizedMessages(moduleName);

    // Getting all thymeleaf controllers
    Set<ClassOrInterfaceTypeDetails> thymeleafControllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_THYMELEAF);
    if (thymeleafControllers.isEmpty()) {
      LOGGER
          .log(Level.INFO,
              "WARNING: Menu view has not been updated because doesn't exists any Thymeleaf controller.");
      return;
    }

    // Update menu calling to the thymeleaf Metadata of the annotated @RooThymeleaf
    Iterator<ClassOrInterfaceTypeDetails> it = thymeleafControllers.iterator();
    ClassOrInterfaceTypeDetails thymeleafController = it.next();
    String controllerMetadataKey = ThymeleafMetadata.createIdentifier(thymeleafController);
    getMetadataService().evictAndGet(controllerMetadataKey);

  }

  /**
   * Add this add-on localized messages from its message bundles to the project's 
   * message bundles, for each installed language, plus English. Existing messages 
   * will be replaced.
   * 
   * @param moduleName the module name where the message bundles are.
   * @param flowName the name/id of the flow to prefix the messages.
   */
  private void addLocalizedMessages(String moduleName) {

    // Install localized messages for each installed language
    for (I18n i18n : i18nSupport.getSupportedLanguages()) {
      if (i18n.getLanguage().equals(new EnglishLanguage().getLanguage())) {
        continue;
      }

      // Get theme specific messages
      InputStream themeMessagesInputStream = null;
      try {
        themeMessagesInputStream =
            FileUtils.getInputStream(getClass(),
                String.format("messages_%s.properties", i18n.getLocale()));
      } catch (NullPointerException ex) {
        LOGGER
            .warning(String
                .format(
                    "There aren't translations for %1$s language. Adding english messages to messages_%1$s.properties instead.",
                    i18n.getLocale()));
        themeMessagesInputStream =
            FileUtils.getInputStream(getClass(),
                String.format("messages.properties", i18n.getLocale()));
      }
      final Properties loadedProperties =
          propFilesManagerService.loadProperties(themeMessagesInputStream);

      // Add theme messages to localized message bundle
      final LogicalPath resourcesPath =
          LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, moduleName);
      final String targetDirectory = pathResolver.getIdentifier(resourcesPath, "");
      String bundlePath =
          String.format("%s%smessages_%s.properties", targetDirectory,
              AntPathMatcher.DEFAULT_PATH_SEPARATOR, i18n.getLocale());
      if (fileManager.exists(bundlePath)) {
        Map<String, String> newProperties = new HashMap<String, String>();
        for (Entry<Object, Object> entry : loadedProperties.entrySet()) {
          String key = (String) entry.getKey();
          String value = (String) entry.getValue();

          // Prefix with flow name
          key = String.format("%s_%s", this.flowName.toLowerCase(), key);
          newProperties.put(key, value);
          newProperties.put("label_".concat(this.flowName.toLowerCase()),
              StringUtils.capitalize(this.flowName));
        }
        propFilesManagerService.addProperties(resourcesPath,
            String.format("messages_%s.properties", i18n.getLocale()), newProperties, true, true);
      }

      // Close InputStream
      try {
        themeMessagesInputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Always install english messages
    InputStream themeMessagesInputStream =
        FileUtils.getInputStream(getClass(), "messages.properties");
    EnglishLanguage english = new EnglishLanguage();
    final Properties loadedProperties =
        propFilesManagerService.loadProperties(themeMessagesInputStream);

    // Add theme messages to localized message bundle
    final LogicalPath resourcesPath = LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, moduleName);
    final String targetDirectory = pathResolver.getIdentifier(resourcesPath, "");
    String bundlePath =
        String.format("%s%smessages.properties", targetDirectory,
            AntPathMatcher.DEFAULT_PATH_SEPARATOR, english.getLocale());
    if (fileManager.exists(bundlePath)) {
      Map<String, String> newProperties = new HashMap<String, String>();
      for (Entry<Object, Object> entry : loadedProperties.entrySet()) {
        String key = (String) entry.getKey();
        String value = (String) entry.getValue();

        // Prefix with flow name
        key = String.format("%s_%s", this.flowName.toLowerCase(), key);
        newProperties.put(key, value);
        newProperties.put("label_".concat(this.flowName.toLowerCase()),
            StringUtils.capitalize(this.flowName));
      }
      propFilesManagerService.addProperties(resourcesPath, "messages.properties", newProperties,
          true, true);
    }

    // Close InputStream
    try {
      themeMessagesInputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Add model object to each flow view, using flowScope and "model" attribute
   * 
   * @param inputStream the InputStream the file whose contents should be modified
   * @param modelObject the object used to create the element's attribute values
   * 
   * @return the Document representing the file to write
   */
  private Document addModelObjectToFlow(InputStream inputStream, JavaType modelObject) {
    Document document = XmlUtils.readXml(inputStream);
    Element documentElement = document.getDocumentElement();

    // Find first view state
    Element firstViewState = XmlUtils.findFirstElement("//view-state", documentElement);

    // Set model object name to use
    String modelObjectName = StringUtils.uncapitalize(modelObject.getSimpleTypeName());

    // Create 'on-start' element
    Element onStartElement = document.createElement("on-start");
    Element setElement = document.createElement("set");
    setElement.setAttribute("name", String.format("flowScope.%s", modelObjectName));
    setElement.setAttribute("value",
        String.format("new %s()", modelObject.getFullyQualifiedTypeName()));
    onStartElement.appendChild(setElement);

    // Insert the element
    documentElement.insertBefore(onStartElement, firstViewState.getPreviousSibling()
        .getPreviousSibling());

    // Add model attribute to views
    List<Element> viewElements = XmlUtils.findElements("//view-state", documentElement);
    for (Element element : viewElements) {
      element.setAttribute("model", modelObjectName);
    }

    return document;
  }

  /**
   * Creates a new *-flow.xml for a given flow name and target directory, following
   * the default template.
   *
   * @param targetDirectory the directory path where create the file.
   * @param replacements the map with String replacements to do in the template
   */
  private void createWebFlowFromTemplate(String targetDirectory, Map<String, String> replacements,
      JavaType modelObject) {
    String fileIdentifier = targetDirectory.concat(String.format("/%s-flow.xml", this.flowName));
    InputStream inputStream = FileUtils.getInputStream(this.getClass(), "flow-template.xml");

    // Create new file in project with specific name
    OutputStream outputStream = fileManager.createFile(fileIdentifier).getOutputStream();
    try {
      String contents = IOUtils.toString(inputStream);

      // Do replacements if needed
      if (!replacements.isEmpty()) {
        for (Entry<String, String> entry : replacements.entrySet()) {
          contents = contents.replace(entry.getKey(), entry.getValue());
        }
        inputStream = IOUtils.toInputStream(contents);
      }

      if (modelObject != null) {

        // Add model object (if any) to flow
        XmlUtils.writeXml(outputStream, addModelObjectToFlow(inputStream, modelObject));
      } else {

        // Copy input to output file
        IOUtils.copy(inputStream, outputStream);
      }
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", fileIdentifier), e);
    }
    IOUtils.closeQuietly(inputStream);
    IOUtils.closeQuietly(outputStream);
  }

  /**
   * Add Springlets Web Flow dependency to project, which manages WebFlow 
   * configuration of the project.
   * 
   * @param moduleName the module name where dependency should be added.
   */
  private void installWebFlowConfiguration(String moduleName) {
    projectOperations.addDependency(moduleName, SPRINGLETS_WEBFLOW_STARTER);
  }

  /**
   * This method will copy the contents of a directory to another if the
   * resource does not already exist in the target directory. Also, it makes 
   * replacements of strings which could exist with the provided Map.
   * 
   * @param sourceAntPath the source path
   * @param targetDirectory the target directory
   * @param replacements the Map with replacements to do in the content
   */
  public void copyDirectoryContents(final String sourceAntPath, String targetDirectory,
      Map<String, String> replacements, JavaType modelObject) {
    Validate.notBlank(sourceAntPath, "Source path required");
    Validate.notBlank(targetDirectory, "Target directory required");

    if (!targetDirectory.endsWith("/")) {
      targetDirectory += "/";
    }

    if (!fileManager.exists(targetDirectory)) {
      fileManager.createDirectory(targetDirectory);
    }

    // Check if should do replacements
    boolean doReplacements = false;
    if (!replacements.isEmpty()) {
      doReplacements = true;
    }

    final String path = FileUtils.getPath(getClass(), sourceAntPath);
    final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(context, path);
    Validate.notNull(urls, "Could not search bundles for resources for Ant Path '%s'", path);
    for (final URL url : urls) {
      final String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
      try {
        String contents = IOUtils.toString(url);

        // Add model object to view form
        if (modelObject != null && contents.contains("data-th-action=\"${flowExecutionUrl}\"")) {
          contents =
              StringUtils.replace(contents, "data-th-action=\"${flowExecutionUrl}\"", String
                  .format("data-th-action=\"${flowExecutionUrl}\" data-th-object=\"${%s}\"",
                      StringUtils.uncapitalize(modelObject.getSimpleTypeName())));
        }

        // Do replacements if necessary
        if (doReplacements) {
          for (Entry<String, String> entry : replacements.entrySet()) {
            contents = contents.replace(entry.getKey(), entry.getValue());
          }
        }

        fileManager.createOrUpdateTextFileIfRequired(targetDirectory + fileName, contents, false);
      } catch (final Exception e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Override
  public boolean isWebFlowInstallationPossible() {
    if (getThymeleafViewResponseService() == null) {
      return false;
    }

    // Check if Thymeleaf view support is installed in any module
    boolean thymeleafInstalled = false;
    List<ControllerMVCResponseService> responseServices = getThymeleafViewResponseService();
    for (String moduleName : projectOperations.getModuleNames()) {
      for (ControllerMVCResponseService responseService : responseServices) {
        if (responseService.isInstalledInModule(moduleName)) {
          thymeleafInstalled = true;
          break;
        }
      }
    }
    return projectOperations.isFeatureInstalled(FeatureNames.MVC) && thymeleafInstalled;
  }

  /**
   * Returns {@link ThymeleafMVCViewResponseService} if available.
   * 
   * @return a list with {@link ControllerMVCResponseService} that match with 
   *            ThymeleafMVCViewResponseService (usually one).
   */
  public List<ControllerMVCResponseService> getThymeleafViewResponseService() {
    return this.serviceManager.getServiceInstance(this, ControllerMVCResponseService.class,
        new ServiceInstaceManager.Matcher<ControllerMVCResponseService>() {

          @Override
          public boolean match(ControllerMVCResponseService service) {
            if (service instanceof ThymeleafMVCViewResponseService) {
              return true;
            }
            return false;
          }

        });
  }

  public MetadataService getMetadataService() {
    return this.serviceManager.getServiceInstance(this, MetadataService.class);
  }

  public TypeLocationService getTypeLocationService() {
    return this.serviceManager.getServiceInstance(this, TypeLocationService.class);
  }
}

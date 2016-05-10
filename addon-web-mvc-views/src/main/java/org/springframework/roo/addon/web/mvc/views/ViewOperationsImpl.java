package org.springframework.roo.addon.web.mvc.views;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.views.i18n.I18nSupport;
import org.springframework.roo.addon.web.mvc.views.i18n.I18n;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link ViewOperations}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ViewOperationsImpl implements ViewOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(ViewOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private TypeLocationService typeLocationService;
  private I18nSupport i18nSupport;
  private PathResolver pathResolver;
  private FileManager fileManager;
  private ProjectOperations projectOperations;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void setup(ControllerMVCResponseService responseType, Pom module) {
    // Check if provided module match with application modules features
    Validate.isTrue(getTypeLocationService()
        .hasModuleFeature(module, ModuleFeatureName.APPLICATION),
        "ERROR: Provided module doesn't match with application modules features. "
            + "Execute this operation again and provide a valid application module.");

    // Delegate on the selected response type to install
    // all necessary elements
    responseType.install(module);
  }

  @Override
  public boolean isInstallLanguageCommandAvailable() {
    List<ControllerMVCResponseService> responseTypes = getControllerMVCResponseTypes(true);
    for (ControllerMVCResponseService type : responseTypes) {
      if (getMVCViewGenerationService(type.getResponseType()) != null) {
        return true;
      }
    }
    return false;
  }

  public void installI18n(final I18n i18n) {
    Validate.notNull(i18n, "Language choice required");

    if (i18n.getLocale() == null) {
      LOGGER.warning("could not parse language choice");
      return;
    }

    // Find application module
    List<Pom> modules =
        (List<Pom>) getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
    if (modules.size() == 0) {
      throw new RuntimeException(String.format("ERROR: Not found a module with %s feature",
          ModuleFeatureName.APPLICATION));
    }
    Pom module = modules.get(0);
    LogicalPath resourcesPath =
        getPathResolver().getPath(module.getModuleName(), Path.SRC_MAIN_RESOURCES);

    final String targetDirectory = getPathResolver().getIdentifier(resourcesPath, "");

    // Install message bundle
    String messageBundle = targetDirectory + "/messages_" + i18n.getLocale().getLanguage() /* + country */
        + ".properties";

    // Special case for english locale (default)
    if (i18n.getLocale().equals(Locale.ENGLISH)) {
      messageBundle = targetDirectory + "/messages.properties";
    }
    if (!getFileManager().exists(messageBundle)) {
      InputStream inputStream = null;
      OutputStream outputStream = null;
      try {
        inputStream = i18n.getMessageBundle();
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
        targetDirectory + "/public/img/" + i18n.getLocale().getLanguage() /* + country */+ ".png";
    if (!getFileManager().exists(flagGraphic)) {
      InputStream inputStream = null;
      OutputStream outputStream = null;
      try {
        inputStream = i18n.getFlagGraphic();
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

    // Setup language definition in languages.jspx
    //    final String footerFileLocation = targetDirectory + "/WEB-INF/views/footer.jspx";
    //    final Document footer = XmlUtils.readXml(getFileManager().getInputStream(footerFileLocation));
    //
    //    if (XmlUtils.findFirstElement("//span[@id='language']/language[@locale='"
    //        + i18n.getLocale().getLanguage() + "']", footer.getDocumentElement()) == null) {
    //      final Element span =
    //          XmlUtils.findRequiredElement("//span[@id='language']", footer.getDocumentElement());
    //      span.appendChild(new XmlElementBuilder("util:language", footer)
    //          .addAttribute("locale", i18n.getLocale().getLanguage())
    //          .addAttribute("label", i18n.getLanguage()).build());
    //      getFileManager().createOrUpdateTextFileIfRequired(footerFileLocation,
    //          XmlUtils.nodeToString(footer), false);
    //    }
  }

  /**
   * This method gets all implementations of ControllerMVCResponseService interface to be able
   * to locate all ControllerMVCResponseService. Uses param installed to obtain only the installed
   * or not installed response types.
   * 
   * @param installed indicates if returned responseType should be installed or not.
   * 
   * @return Map with responseTypes identifier and the ControllerMVCResponseService implementation
   */
  public List<ControllerMVCResponseService> getControllerMVCResponseTypes(boolean installed) {
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
      LOGGER.warning("Cannot load ControllerMVCResponseService on ViewCommands.");
      return null;
    }
  }

  /**
   * This method gets MVCViewGenerationService implementation that contains necessary operations
   * to install templates inside generated project.
   * 
   * @param type
   * @return
   */
  public MVCViewGenerationService getMVCViewGenerationService(String type) {
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(MVCViewGenerationService.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        MVCViewGenerationService viewGenerationService =
            (MVCViewGenerationService) this.context.getService(ref);
        if (viewGenerationService.getName().equals(type)) {
          return viewGenerationService;
        }
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load MVCViewGenerationService on ViewCommands.");
      return null;
    }
  }

  // Get OSGi services

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on ViewOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public I18nSupport getI18nSupport() {
    if (i18nSupport == null) {
      // Get all Services implement I18nSupport interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(I18nSupport.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          i18nSupport = (I18nSupport) context.getService(ref);
          return i18nSupport;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load I18nSupport on JspOperationsImpl.");
        return null;
      }
    } else {
      return i18nSupport;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (PathResolver) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (FileManager) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return fileManager;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

}

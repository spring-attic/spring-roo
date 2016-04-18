package org.springframework.roo.addon.web.mvc.controller.addon.servers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provider to manage the application configuration for web logic servers
 * 
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class WebLogicServerProvider implements ServerProvider {

  private static Logger LOGGER = HandlerUtils.getLogger(EmbeddedServerProvider.class);

  private static final String WEBLOGIC_XML = "WEB-INF/weblogic.xml";
  private static final String WEB_XML = "WEB-INF/web.xml";


  //------------ OSGi component attributes ----------------
  private BundleContext context;

  ProjectOperations projectOperations;
  PathResolver pathResolver;
  FileManager fileManager;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void setup(Pom module) {
    addPlugins(module);
    installWebLogic(module);
    installWeb(module);
  }

  private void installWebLogic(Pom module) {
    final String webLogicPath =
        getPathResolver().getIdentifier(module.getModuleName(), Path.SRC_MAIN_WEBAPP, WEBLOGIC_XML);

    final InputStream templateInputStream =
        FileUtils.getInputStream(getClass(), "weblogic/weblogic-template.xml");
    Validate.notNull(templateInputStream, "Could not acquire weblogic.xml template");
    final Document document = XmlUtils.readXml(templateInputStream);

    getFileManager().createOrUpdateTextFileIfRequired(webLogicPath,
        XmlUtils.nodeToString(document), true);
  }

  private void installWeb(Pom module) {
    final String webPath =
        getPathResolver().getIdentifier(module.getModuleName(), Path.SRC_MAIN_WEBAPP, WEB_XML);

    final InputStream templateInputStream =
        FileUtils.getInputStream(getClass(), "weblogic/web-template.xml");
    Validate.notNull(templateInputStream, "Could not acquire web.xml template");
    final Document document = XmlUtils.readXml(templateInputStream);

    getFileManager().createOrUpdateTextFileIfRequired(webPath, XmlUtils.nodeToString(document),
        true);
  }


  private void addPlugins(Pom module) {
    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Element> plugins =
        XmlUtils.findElements("/configuration/server [@id = '" + getName() + "']/plugins/plugin",
            configuration);
    for (final Element pluginElement : plugins) {
      getProjectOperations().addBuildPlugin(module.getModuleName(), new Plugin(pluginElement));
    }
  }

  @Override
  public String getName() {
    return "WEBLOGIC";
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {

    if (!getProjectOperations().isFeatureInstalled(FeatureNames.MVC)) {
      return false;
    }

    final String webLogicPath =
        getPathResolver().getIdentifier(moduleName, Path.SRC_MAIN_WEBAPP, WEBLOGIC_XML);
    if (!getFileManager().exists(webLogicPath)) {
      return false;
    }

    final String webPath =
        getPathResolver().getIdentifier(moduleName, Path.SRC_MAIN_WEBAPP, WEB_XML);
    if (!getFileManager().exists(webPath)) {
      return false;
    }

    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Plugin> plugins = new ArrayList<Plugin>();
    final List<Element> pluginElements =
        XmlUtils.findElements("/configuration/server [@id = '" + getName() + "']/plugins/plugin",
            configuration);

    for (final Element pluginElement : pluginElements) {
      plugins.add(new Plugin(pluginElement));
    }

    return getProjectOperations().getPomFromModuleName(moduleName).isAllPluginsRegistered(plugins);
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
        LOGGER.warning("Cannot load ProjectOperations on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) this.context.getService(ref);
          return pathResolver;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on ControllerOperationsImpl.");
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
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return fileManager;
    }
  }


}

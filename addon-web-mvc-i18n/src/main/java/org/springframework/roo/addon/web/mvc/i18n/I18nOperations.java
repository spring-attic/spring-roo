package org.springframework.roo.addon.web.mvc.i18n;

import org.springframework.roo.addon.web.mvc.i18n.components.I18n;
import org.springframework.roo.project.maven.Pom;

/**
 * Provides an API with the available Operations to include views on generated
 * project 
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public interface I18nOperations {

  /**
   * Checks if project has MVC installed.
   * 
   * @return <code>true</code> if MVC is installed in project.
   */
  boolean isInstallLanguageCommandAvailable();

  /**
   * Installs additional languages into Web MVC app.
   * 
   * @param language the language
   * @param module the module where to install the message bundles
   */
  void installI18n(I18n i18n, Pom module);

}

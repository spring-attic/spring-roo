package org.springframework.roo.addon.web.mvc.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.addon.web.mvc.i18n.components.I18n;
import org.springframework.roo.addon.web.mvc.i18n.languages.EnglishLanguage;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.ant.AntPathMatcher;

/**
 * Provides an API with the available Operations to include languages on generated
 * project
 *
 * @author Sergio Clares
 * @author Juan Carlos García
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
   * @param useAsDefault boolean that indicates that provided language should be
   * used as default language on this application
   * @param module the module where to install the message bundles
   */
  void installLanguage(I18n language, boolean useAsDefault, Pom module);

  /**
   * Add or update labels of all installed languages
   *
   * @param moduleName
   * @param labels
   */
  void addOrUpdateLabels(String moduleName, final Map<String, String> labels);

  List<I18n> getInstalledLanguages(String moduleName);

}

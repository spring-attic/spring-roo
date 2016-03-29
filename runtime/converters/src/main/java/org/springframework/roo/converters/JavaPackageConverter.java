package org.springframework.roo.converters;

import static org.springframework.roo.converters.JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL;
import static org.springframework.roo.project.LogicalPath.MODULE_PATH_SEPARATOR;
import static org.springframework.roo.shell.OptionContexts.UPDATE;
import static org.springframework.roo.support.util.AnsiEscapeCode.FG_CYAN;
import static org.springframework.roo.support.util.AnsiEscapeCode.decorate;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * A {@link Converter} for {@link JavaPackage}s, with support for using
 * {@value #TOP_LEVEL_PACKAGE_SYMBOL} to denote the user's top-level package.
 * 
 * @author Ben Alex
 * @author Paula Navarro
 * @since 1.0
 */
@Component
@Service
public class JavaPackageConverter implements Converter<JavaPackage> {

  /**
   * The shell character that represents the current project or module's top
   * level Java package.
   */
  public static final String TOP_LEVEL_PACKAGE_SYMBOL = "~";

  @Reference
  FileManager fileManager;
  @Reference
  LastUsed lastUsed;
  @Reference
  ProjectOperations projectOperations;
  @Reference
  TypeLocationService typeLocationService;

  public JavaPackage convertFromText(String value, final Class<?> requiredType,
      final String optionContext) {
    if (StringUtils.isBlank(value)) {
      return null;
    }

    Pom module = projectOperations.getFocusedModule();

    if (value.contains(MODULE_PATH_SEPARATOR)) {
      final String moduleName = value.substring(0, value.indexOf(MODULE_PATH_SEPARATOR));
      module = projectOperations.getPomFromModuleName(moduleName);
      value = value.substring(value.indexOf(MODULE_PATH_SEPARATOR) + 1, value.length()).trim();
      if (StringUtils.contains(optionContext, UPDATE)) {
        projectOperations.setModule(module);
      }
    }

    String moduleName = module == null ? null : module.getModuleName();

    JavaPackage result = new JavaPackage(convertToFullyQualifiedPackageName(value), moduleName);
    if (optionContext != null && optionContext.contains(UPDATE)) {
      lastUsed.setPackage(result);
    }
    return result;
  }

  private String convertToFullyQualifiedPackageName(final String text) {
    final String normalisedText = StringUtils.removeEnd(text, ".").toLowerCase();
    if (normalisedText.startsWith(TOP_LEVEL_PACKAGE_SYMBOL)) {
      return replaceTopLevelPackageSymbol(normalisedText);
    }
    return normalisedText;
  }

  public boolean getAllPossibleValues(final List<Completion> completions,
      final Class<?> requiredType, final String existingData, final String optionContext,
      final MethodTarget target) {
    if (!projectOperations.isFocusedProjectAvailable()) {
      return false;
    }

    final Pom targetModule;
    final String heading;
    final String prefix;
    final String formattedPrefix;
    if (existingData != null && existingData.contains(MODULE_PATH_SEPARATOR)) {
      // Looking for a type in another module
      final String targetModuleName =
          existingData.substring(0, existingData.indexOf(MODULE_PATH_SEPARATOR));
      targetModule = projectOperations.getPomFromModuleName(targetModuleName);
      heading = "";
      prefix = targetModuleName + MODULE_PATH_SEPARATOR;
      formattedPrefix = decorate(targetModuleName + MODULE_PATH_SEPARATOR, FG_CYAN);
    } else {
      // Looking for a type in the currently focused module
      targetModule = projectOperations.getFocusedModule();
      heading = targetModule.getModuleName();
      prefix = "";
      formattedPrefix = "";
    }

    addCompletionsForOtherModuleNames(completions, targetModule);

    if (!"pom".equals(targetModule.getPackaging())) {
      addCompletionsForPackagesInTargetModule(completions, targetModule, heading, prefix,
          formattedPrefix);
    }
    return false;
  }

  private void addCompletionsForPackagesInTargetModule(final Collection<Completion> completions,
      final Pom targetModule, final String heading, final String prefix,
      final String formattedPrefix) {

    final String topLevelPackage = typeLocationService.getTopLevelPackageForModule(targetModule);
    completions.add(new Completion(prefix + topLevelPackage, formattedPrefix + topLevelPackage,
        heading, 1));

    for (final JavaType javaType : typeLocationService.getTypesForModule(targetModule)) {
      String type = javaType.getFullyQualifiedTypeName();
      completions.add(new Completion(prefix + type.substring(0, type.lastIndexOf('.')),
          formattedPrefix + type.substring(0, type.lastIndexOf('.')), heading, 1));
    }
  }

  private void addCompletionsForOtherModuleNames(final Collection<Completion> completions,
      final Pom targetModule) {
    for (final Pom pom : projectOperations.getPoms()) {
      if (StringUtils.isNotBlank(pom.getModuleName())
          && !pom.getModuleName().equals(targetModule.getModuleName())) {
        completions.add(new Completion(pom.getModuleName() + MODULE_PATH_SEPARATOR, decorate(
            pom.getModuleName() + MODULE_PATH_SEPARATOR, FG_CYAN), "Modules", 0));
      }
    }
  }

  private String getTopLevelPackage() {
    if (projectOperations.isFocusedProjectAvailable()) {
      return typeLocationService.getTopLevelPackageForModule(projectOperations.getFocusedModule());
    }
    // Shouldn't happen if there's a project, i.e. most of the time
    return "";
  }

  /**
   * Replaces the {@link #TOP_LEVEL_PACKAGE_SYMBOL} at the beginning of the
   * given text with the current project/module's top-level package
   * 
   * @param text
   * @return a well-formed Java package name (might have a trailing dot)
   */
  private String replaceTopLevelPackageSymbol(final String text) {
    final String topLevelPackage = getTopLevelPackage();
    if (TOP_LEVEL_PACKAGE_SYMBOL.equals(text)) {
      return topLevelPackage;
    }
    final String textWithoutSymbol = StringUtils.removeStart(text, TOP_LEVEL_PACKAGE_SYMBOL);
    return topLevelPackage + "." + StringUtils.removeStart(textWithoutSymbol, ".");
  }

  public boolean supports(final Class<?> requiredType, final String optionContext) {
    return JavaPackage.class.isAssignableFrom(requiredType);
  }
}

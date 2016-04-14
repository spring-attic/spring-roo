package org.springframework.roo.converters;

import static org.springframework.roo.project.maven.Pom.ROOT_MODULE_SYMBOL;
import static org.springframework.roo.shell.OptionContexts.FEATURE;
import static org.springframework.roo.shell.OptionContexts.INCLUDE_CURRENT_MODULE;
import static org.springframework.roo.shell.OptionContexts.UPDATE;
import static org.springframework.roo.shell.OptionContexts.UPDATELAST;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

@Component
@Service
public class PomConverter implements Converter<Pom> {

  /**
   * The value that converts to the most recently used {@link Pom}.
   */
  static final String LAST_USED_INDICATOR = "*";

  /**
   * The value that converts to focused module {@link Pom}.
   */
  static final String FOCUSED_INDICATOR = ".";

  final Pattern pattern = Pattern.compile(FEATURE + "\\[(.+?)\\]");

  @Reference
  ProjectOperations projectOperations;
  @Reference
  TypeLocationService typeLocationService;
  @Reference
  LastUsed lastUsed;

  private void addCompletion(final String moduleName, final List<Completion> completions) {
    final String nonEmptyModuleName = StringUtils.defaultIfEmpty(moduleName, ROOT_MODULE_SYMBOL);
    completions.add(new Completion(nonEmptyModuleName));
  }

  public Pom convertFromText(final String value, final Class<?> targetType,
      final String optionContext) {
    final String moduleName;
    Pom result;
    ModuleFeatureName moduleFeatureName = null;

    // Get module feature
    if (optionContext != null) {
      final Matcher matcher = pattern.matcher(optionContext);
      if (matcher.find()) {
        moduleFeatureName = ModuleFeatureName.valueOf(matcher.group(1));
      }
    }

    if (LAST_USED_INDICATOR.equals(value)) {
      result = lastUsed.getModule();
      if (result == null) {
        throw new IllegalStateException(
            "Unknown pom; please indicate the module as a command option (ie --xxxx)");
      }
    } else if (FOCUSED_INDICATOR.equals(value)) {
      result = projectOperations.getFocusedModule();

      if (moduleFeatureName != null
          && !typeLocationService.hasModuleFeature(result, moduleFeatureName)) {

        // Get valid module
        List<Pom> modules = (List<Pom>) typeLocationService.getModules(moduleFeatureName);
        if (modules.size() == 0) {
          throw new RuntimeException(String.format("ERROR: Not exists a module with %s feature",
              moduleFeatureName));
        } else {
          result = modules.get(0);
        }
      }
    } else {
      if (ROOT_MODULE_SYMBOL.equals(value)) {
        moduleName = "";
      } else {
        moduleName = value;
      }
      result = projectOperations.getPomFromModuleName(moduleName);
      Validate.notNull(result, String.format("Module %s not found", moduleName));
    }

    // Validate feature
    if (moduleFeatureName != null
        && !typeLocationService.hasModuleFeature(result, moduleFeatureName)) {
      return null;
    }

    if (StringUtils.contains(optionContext, UPDATE)
        || StringUtils.contains(optionContext, UPDATELAST)) {
      lastUsed.setTypeNotVerified(null, result);
    }
    return result;

  }


  public boolean getAllPossibleValues(final List<Completion> completions,
      final Class<?> targetType, final String existingData, final String optionContext,
      final MethodTarget target) {

    boolean filteredByFeature = false;

    if (optionContext != null) {
      for (ModuleFeatureName moduleFeatureName : ModuleFeatureName.values()) {
        if (optionContext.contains(moduleFeatureName.name())) {
          filteredByFeature = true;
          addModules(completions, optionContext,
              typeLocationService.getModuleNames(moduleFeatureName));
        }
      }
    }

    if (!filteredByFeature) {
      addModules(completions, optionContext, projectOperations.getModuleNames());
    }
    return true;
  }

  private void addModules(final List<Completion> completions, final String optionContext,
      final Collection<String> moduleNames) {
    final String focusedModuleName = projectOperations.getFocusedModuleName();
    for (final String moduleName : moduleNames) {
      if (isModuleRelevant(moduleName, focusedModuleName, optionContext)) {
        addCompletion(moduleName, completions);
      }
    }
  }

  private boolean isModuleRelevant(final String moduleName, final String focusedModuleName,
      final String optionContext) {
    return StringUtils.contains(optionContext, INCLUDE_CURRENT_MODULE)
        || !moduleName.equals(focusedModuleName);
  }

  public boolean supports(final Class<?> type, final String optionContext) {
    return Pom.class.isAssignableFrom(type);
  }
}

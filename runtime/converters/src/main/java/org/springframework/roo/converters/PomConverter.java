package org.springframework.roo.converters;

import static org.springframework.roo.classpath.MetadataCommands.INCLUDE_CURRENT_MODULE;
import static org.springframework.roo.project.maven.Pom.ROOT_MODULE_SYMBOL;
import static org.springframework.roo.shell.OptionContexts.UPDATE;
import static org.springframework.roo.shell.OptionContexts.UPDATELAST;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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

  @Reference
  ProjectOperations projectOperations;
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

    if (LAST_USED_INDICATOR.equals(value)) {
      result = lastUsed.getModule();
      if (result == null) {
        throw new IllegalStateException(
            "Unknown pom; please indicate the module as a command option (ie --xxxx)");
      }
      return result;
    }

    if (ROOT_MODULE_SYMBOL.equals(value)) {
      moduleName = "";
    } else {
      moduleName = value;
    }
    result = projectOperations.getPomFromModuleName(moduleName);

    Validate.notNull(result, String.format("Module %s not found", moduleName));

    if (StringUtils.contains(optionContext, UPDATE)
        || StringUtils.contains(optionContext, UPDATELAST)) {
      lastUsed.setTypeNotVerified(null, result);
    }
    return result;
  }

  public boolean getAllPossibleValues(final List<Completion> completions,
      final Class<?> targetType, final String existingData, final String optionContext,
      final MethodTarget target) {
    final String focusedModuleName = projectOperations.getFocusedModuleName();
    for (final String moduleName : projectOperations.getModuleNames()) {
      if (isModuleRelevant(moduleName, focusedModuleName, optionContext)) {
        addCompletion(moduleName, completions);
      }
    }
    return true;
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

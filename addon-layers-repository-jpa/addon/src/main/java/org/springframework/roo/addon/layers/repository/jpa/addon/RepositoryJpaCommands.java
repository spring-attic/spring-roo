package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;
import static org.springframework.roo.shell.OptionContexts.UPDATE;
import static org.springframework.roo.shell.OptionContexts.UPDATELAST_INTERFACE;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

/**
 * Commands for the JPA repository add-on.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaCommands implements CommandMarker {

  @Reference
  private RepositoryJpaOperations repositoryJpaOperations;

  @CliAvailabilityIndicator({"repository jpa add", "repository jpa all"})
  public boolean isRepositoryCommandAvailable() {
    return repositoryJpaOperations.isRepositoryInstallationPossible();
  }

  @CliCommand(value = "repository jpa all",
      help = "Generates new Spring Data repository for all entities.")
  public void all(
      @CliOption(key = "package", mandatory = true, optionContext = UPDATE,
          help = "The package where repositories will be generated") final JavaPackage repositoriesPackage) {

    repositoryJpaOperations.generateAllRepositories(repositoriesPackage);
  }

  @CliOptionVisibilityIndicator(params = {"interface"}, command = "repository jpa add",
      help = "You should specify --entity param to be able to specify repository name")
  public boolean isClassVisible(ShellContext shellContext) {
    // Get all defined parameters
    Map<String, String> parameters = shellContext.getParameters();

    // If --entity has been defined, show --class parameter
    if (parameters.containsKey("entity") && StringUtils.isNotBlank(parameters.get("entity"))) {
      return true;
    }

    return false;
  }

  @CliCommand(value = "repository jpa add",
      help = "Generates new Spring Data repository for specified entity.")
  public void repository(
      @CliOption(key = "interface", mandatory = true, optionContext = UPDATELAST_INTERFACE,
          help = "The java Spring Data repository to generate.") final JavaType interfaceType,
      @CliOption(key = "entity", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = PROJECT, help = "The domain entity this repository should expose") final JavaType domainType) {

    repositoryJpaOperations.addRepository(interfaceType, domainType);
  }
}

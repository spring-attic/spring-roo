package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

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

    @Reference private RepositoryJpaOperations repositoryJpaOperations;

    @CliAvailabilityIndicator("repository jpa")
    public boolean isRepositoryCommandAvailable() {
        return repositoryJpaOperations.isRepositoryInstallationPossible();
    }

    @CliCommand(value = "repository jpa", help = "Generates new Spring Data repository for specified entity.")
    public void repository(
            @CliOption(key = "class", mandatory = true, help = "The java Spring Data repository to generate (Will be an interface.)") final JavaType interfaceType,
            @CliOption(key = "entity", unspecifiedDefaultValue = "*", optionContext = PROJECT, mandatory = false, help = "The domain entity this repository should expose") final JavaType domainType) {

        repositoryJpaOperations.addRepository(interfaceType, domainType);
    }
}

package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.shell.OptionContexts.PROJECT;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.math.BigInteger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the MongoDB repository add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class MongoCommands implements CommandMarker {

    @Reference private MongoOperations mongoOperations;

    @CliAvailabilityIndicator("mongo setup")
    public boolean isMongoSetupAvailable() {
        return mongoOperations.isMongoInstallationPossible();
    }

    @CliAvailabilityIndicator({ "repository mongo", "entity mongo" })
    public boolean isRepositoryCommandAvailable() {
        return mongoOperations.isRepositoryInstallationPossible();
    }

    @CliCommand(value = "repository mongo", help = "Adds @RooMongoRepository annotation to target type")
    public void repository(
            @CliOption(key = "interface", mandatory = true, help = "The java interface to apply this annotation to") final JavaType interfaceType,
            @CliOption(key = "entity", unspecifiedDefaultValue = "*", optionContext = PROJECT, mandatory = false, help = "The domain entity this repository should expose") final JavaType domainType) {

        mongoOperations.setupRepository(interfaceType, domainType);
    }

    @CliCommand(value = "mongo setup", help = "Configures the project for MongoDB peristence.")
    public void setup(
            @CliOption(key = "username", mandatory = false, help = "Username for accessing the database (defaults to '')") final String username,
            @CliOption(key = "password", mandatory = false, help = "Password for accessing the database (defaults to '')") final String password,
            @CliOption(key = "databaseName", mandatory = false, help = "Name of the database (defaults to project name)") final String name,
            @CliOption(key = "port", mandatory = false, help = "Port for the database (defaults to '27017')") final String port,
            @CliOption(key = "host", mandatory = false, help = "Host for the database (defaults to '127.0.0.1')") final String host,
            @CliOption(key = "cloudFoundry", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Deploy to CloudFoundry (defaults to 'false')") final boolean cloudFoundry) {

        mongoOperations.setup(username, password, name, port, host,
                cloudFoundry);
    }

    @CliCommand(value = "entity mongo", help = "Creates a domain entity which can be backed by a MongoDB repository")
    public void type(
            @CliOption(key = "class", mandatory = true, optionContext = UPDATE_PROJECT, help = "Implementation class for the specified interface") final JavaType classType,
            @CliOption(key = "identifierType", mandatory = false, help = "The ID type to be used for this domain type (defaults to BigInteger)") MongoIdType idType,
            @CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for this entity") final boolean testAutomatically) {

        if (idType == null) {
            idType = new MongoIdType(BigInteger.class.getName());
        }
        mongoOperations.createType(classType, idType.getJavaType(),
                testAutomatically);
    }
}

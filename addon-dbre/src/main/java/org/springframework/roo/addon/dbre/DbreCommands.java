package org.springframework.roo.addon.dbre;

import java.io.File;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands for {@link DbreOperations}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreCommands implements CommandMarker {

    @Reference private DbreOperations dbreOperations;

    @CliCommand(value = "database introspect", help = "Displays database metadata")
    public void displayDatabaseMetadata(
            @CliOption(key = "schema", mandatory = true, optionContext = "schema", help = "The database schema names. Multiple schema names must be a double-quoted list separated by spaces") final Set<Schema> schemas,
            @CliOption(key = "file", mandatory = false, help = "The file to save the metadata to") final File file,
            @CliOption(key = "enableViews", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Display database views") final boolean view) {

        dbreOperations.displayDatabaseMetadata(schemas, file, view);
    }

    @CliAvailabilityIndicator({ "database introspect",
            "database reverse engineer" })
    public boolean isDbreAvailable() {
        return dbreOperations.isDbreInstallationPossible();
    }

    @CliCommand(value = "database reverse engineer", help = "Create and update entities based on database metadata")
    public void serializeDatabaseMetadata(
            @CliOption(key = "schema", mandatory = true, optionContext = "schema", help = "The database schema names. Multiple schema names must be a double-quoted list separated by spaces") final Set<Schema> schemas,
            @CliOption(key = "package", mandatory = false, help = "The package in which new entities will be placed") final JavaPackage destinationPackage,
            @CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for entities") final boolean testAutomatically,
            @CliOption(key = "enableViews", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Reverse engineer database views") final boolean view,
            @CliOption(key = "includeTables", mandatory = false, specifiedDefaultValue = "", optionContext = "include-tables", help = "The tables to include in reverse engineering. Multiple table names must be a double-quoted list separated by spaces") final Set<String> includeTables,
            @CliOption(key = "excludeTables", mandatory = false, specifiedDefaultValue = "", optionContext = "exclude-tables", help = "The tables to exclude from reverse engineering. Multiple table names must be a double-quoted list separated by spaces") final Set<String> excludeTables,
            @CliOption(key = "includeNonPortableAttributes", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Include non-portable JPA @Column attributes such as 'columnDefinition'") final boolean includeNonPortableAttributes,
            @CliOption(key = "activeRecord", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Generate CRUD active record methods for each entity") final boolean activeRecord) {

        dbreOperations.reverseEngineerDatabase(schemas, destinationPackage,
                testAutomatically, view, includeTables, excludeTables,
                includeNonPortableAttributes, activeRecord);
    }
}
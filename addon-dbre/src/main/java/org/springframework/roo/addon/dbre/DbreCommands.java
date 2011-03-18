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

	@CliAvailabilityIndicator({ "database introspect", "database reverse engineer" })
	public boolean isDbreAvailable() {
		return dbreOperations.isDbreAvailable();
	}

	@CliCommand(value = "database introspect", help = "Displays database metadata")
	public void displayDatabaseMetadata(
		@CliOption(key = "schema", mandatory = true, help = "The database schema name") Schema schema, 
		@CliOption(key = "file", mandatory = false, help = "The file to save the metadata to") File file,
		@CliOption(key = "enableViews", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Display database views") boolean view) {

		dbreOperations.displayDatabaseMetadata(schema, file, view);
	}

	@CliCommand(value = "database reverse engineer", help = "Create and update entities based on database metadata")
	public void serializeDatabaseMetadata(
		@CliOption(key = "schema", mandatory = true, help = "The database schema name") Schema schema, 
		@CliOption(key = "package", mandatory = false, help = "The package in which new entities will be placed") JavaPackage destinationPackage,
		@CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for entities") boolean testAutomatically, 
		@CliOption(key = "enableViews", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Reverse engineer database views") boolean view,
		@CliOption(key = "includeTables", mandatory = false, specifiedDefaultValue = "", optionContext = "include-tables", help = "The tables to include in reverse engineering. Must be separated by spaces and enclosed by pairs of double quotes") Set<String> includeTables,
		@CliOption(key = "excludeTables", mandatory = false, specifiedDefaultValue = "", optionContext = "exclude-tables", help = "The tables to exclude from reverse engineering. Must be separated by spaces and enclosed by pairs of double quotes") Set<String> excludeTables,
		@CliOption(key = "includeNonPortableAttributes", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Include non-portable JPA @Column attributes such as 'columnDefinition'") boolean includeNonPortableAttributes) {
		
		dbreOperations.reverseEngineerDatabase(schema, destinationPackage, testAutomatically, view, includeTables, excludeTables, includeNonPortableAttributes);
	}
}
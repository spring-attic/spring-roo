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
 * Commands for the DBRE addon to be used by the ROO shell.
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
		@CliOption(key = "file", mandatory = false, help = "The file to save the metadata to") File file) {

		dbreOperations.displayDatabaseMetadata(schema, file);
	}

	@CliCommand(value = "database reverse engineer", help = "Create and updates entities based on te database metadata")
	public void serializeDatabaseMetadata(
		@CliOption(key = "schema", mandatory = true, help = "The database schema name") Schema schema, 
		@CliOption(key = "package", mandatory = false, help = "The package in which new entities will be placed") JavaPackage destinationPackage,
		@CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for entities") boolean testAutomatically, 
		@CliOption(key = "includeTables", mandatory = false, specifiedDefaultValue = "", optionContext = "include-tables", help = "The tables to include in reverse engineering. Must be separated by spaces and enclosed by pairs of double quotes") Set<String> includeTables,
		@CliOption(key = "excludeTables", mandatory = false, specifiedDefaultValue = "", optionContext = "exclude-tables", help = "The tables to exclude from reverse engineering. Must be separated by spaces and enclosed by pairs of double quotes") Set<String> excludeTables) {
		
		dbreOperations.reverseEngineerDatabase(schema, destinationPackage, testAutomatically, includeTables, excludeTables);
	}
}
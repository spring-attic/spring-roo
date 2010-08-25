package org.springframework.roo.addon.dbre;

import java.io.File;

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

	@CliCommand(value = "database reverse engineer", help = "Create and updates entities based on the database metadata")
	public void serializeDatabaseMetadata(
		@CliOption(key = "schema", mandatory = false, help = "The database schema name") Schema schema, 
		@CliOption(key = "package", mandatory = false, optionContext = "update", help = "The package in which new entities will be placed") JavaPackage destinationPackage) {

		dbreOperations.reverseEngineerDatabase(schema, destinationPackage);
	}
}
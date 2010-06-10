package org.springframework.roo.addon.dbre;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the dbre add-on to be used by the ROO shell.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreCommands implements CommandMarker {
	@Reference private DbreOperations dbreOperations;
	@Reference private TableModelService tableModelService;
	
	@CliAvailabilityIndicator( { "database introspect", "database reverse engineer" })
	public boolean isDbreAvailable() {
		return dbreOperations.isDbreAvailable();
	}

	@CliCommand(value = "database model dump", help = "Displays discovered table to entity info")
	public String entityModel () {
		return tableModelService.dump();
	}

	@CliCommand(value = "database introspect", help = "Displays database metadata")
	public void displayDatabaseMetadata(
			@CliOption(key = "table", mandatory = false, help = "The table name") String table,  
			@CliOption(key = "file", mandatory = false, help = "The file to save the metadata to") String file) { 
				
		dbreOperations.displayDbMetadata(table, file);
	}
	
	@CliCommand(value = "database reverse engineer", help = "Generates and updates entities based on the database schema")
	public void updateDbreXml(
			@CliOption(key = "package", mandatory = false, help = "The package in which new entities will be placed") JavaPackage javaPackage) { 

		dbreOperations.updateDbreXml(javaPackage);
	}
}
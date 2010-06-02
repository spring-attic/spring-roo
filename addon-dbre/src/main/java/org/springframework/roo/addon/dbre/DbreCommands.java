package org.springframework.roo.addon.dbre;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands for the 'dbre' add-on to be used by the ROO shell.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreCommands implements CommandMarker {
	private static Logger logger = HandlerUtils.getLogger(DbreCommands.class);
	@Reference private DbreOperations dbreOperations;
	@Reference private PropFileOperations propFileOperations;

	@CliAvailabilityIndicator( { "database introspect", "database reverse engineer" })
	public boolean isDbreAvailable() {
		return dbreOperations.isDbreAvailable();
	}

	@CliCommand(value = "database introspect", help = "Displays database metadata")
	public void databaseMetadata(
			@CliOption(key = "table", mandatory = false, help = "The table name") String table) { 
				
		dbreOperations.displayDatabaseMetadata(table);
	}
}
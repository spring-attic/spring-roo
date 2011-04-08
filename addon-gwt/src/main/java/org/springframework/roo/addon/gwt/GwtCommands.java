package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the GWT add-on to be used by the Roo shell.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class GwtCommands implements CommandMarker {
	@Reference private GwtOperations gwtOperations;

	@CliAvailabilityIndicator({ "web gwt setup", "gwt setup" })
	public boolean isSetupAvailable() {
		return gwtOperations.isSetupAvailable();
	}

	@CliCommand(value = "web gwt setup", help = "Install Google Web Toolkit (GWT) into your project")
	public void webGwtSetup() {
		gwtOperations.setup();
	}
	
	@Deprecated
	@CliCommand(value = "gwt setup", help = "Install Google Web Toolkit (GWT) into your project - deprecated, use 'web gwt setup' instead")
	public void installGwt() {
		gwtOperations.setup();
	}
}
package org.springframework.roo.addon.jsf;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the JSF add-on to be used by the ROO shell.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component 
@Service 
public class JsfCommands implements CommandMarker {
	@Reference private JsfOperations jsfOperations;

	@CliAvailabilityIndicator({ "web jsf setup" }) 
	public boolean isSetupAvailable() {
		return jsfOperations.isSetupAvailable();
	}

	@CliAvailabilityIndicator({ "web jsf all", "web jsf scaffold" }) 
	public boolean isJsfAvailable() {
		return jsfOperations.isScaffoldAvailable();
	}

	@CliCommand(value = "web jsf setup", help = "Set up JSF environment") 
	public void webJsfSetup(
		@CliOption(key = "implementation", mandatory = false, help = "The JSF implementation to use") JsfImplementation jsfImplementation) {
		
		if (jsfImplementation == null) {
			jsfImplementation = JsfImplementation.ORACLE_MOJARRA;
		}

		jsfOperations.setup(jsfImplementation);
	}

	@CliCommand(value = "web jsf all", help = "Create JSF managed beans for all entities") 
	public void webJsfAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which new JSF managed beans will be placed") JavaPackage destinationPackage) {
		
		jsfOperations.generateAll(destinationPackage);
	}
	
	@CliCommand(value = "web jsf scaffold", help = "Create a JSF managed bean for an entity") 
	public void webJsfScaffold(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the JSF managed bean to be created") JavaType managedBean, 
		@CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The entity which this JSF managed bean class will create and modify as required") JavaType entity) { 
		
		jsfOperations.createManagedBean(managedBean, entity);
	}
}
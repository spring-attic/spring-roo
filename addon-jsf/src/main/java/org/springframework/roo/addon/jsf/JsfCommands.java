package org.springframework.roo.addon.jsf;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
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

	@CliAvailabilityIndicator({ "web jsf implementation", "web jsf all", "web jsf scaffold", "field file" }) 
	public boolean isJsfAvailable() {
		return jsfOperations.isScaffoldAvailable();
	}

	@CliCommand(value = "web jsf setup", help = "Set up JSF environment") 
	public void webJsfSetup(
		@CliOption(key = "implementation", mandatory = false, help = "The JSF implementation to use") JsfImplementation jsfImplementation,
		@CliOption(key = "theme", mandatory = false, help = "The name of the theme") Theme theme) {
		
		jsfOperations.setup(jsfImplementation, theme);
	}

	@CliCommand(value = "web jsf all", help = "Create JSF managed beans for all entities") 
	public void webJsfAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which new JSF managed beans will be placed") JavaPackage destinationPackage) {
		
		jsfOperations.generateAll(destinationPackage);
	}
	
	@CliCommand(value = "web jsf scaffold", help = "Create JSF managed bean for an entity") 
	public void webJsfScaffold(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the JSF managed bean to be created") JavaType managedBean, 
		@CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The entity which this JSF managed bean class will create and modify as required") JavaType entity, 
		@CliOption(key = "includeOnMenu", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Include this entity on the generated JSF menu") boolean includeOnMenu, 
		@CliOption(key = "createConverter", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Create JSF converter for the entity") boolean createConverter) {
		
		jsfOperations.createManagedBean(managedBean, entity, includeOnMenu, createConverter);
	}
	
	@CliCommand(value = "field file", help ="Adds a field for storing uploaded file contents")	
	public void addFileUploadField(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the file upload field to add") JavaSymbolName fieldName, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "fileName", mandatory = false, help = "The file name") String fileName, 
		@CliOption(key = "contentType", mandatory = true, help = "The content type of the file") String contentType, 
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") String column, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		jsfOperations.addFileUploadField(fieldName, typeName, fileName, contentType, column, notNull, permitReservedWords);
	}
}
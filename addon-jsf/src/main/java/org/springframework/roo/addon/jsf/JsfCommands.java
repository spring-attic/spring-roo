package org.springframework.roo.addon.jsf;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jsf.model.UploadedFileContentType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
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
	
	// Fields
	@Reference private JsfOperations jsfOperations;
	@Reference ProjectOperations projectOperations;

	@CliAvailabilityIndicator({ "web jsf setup" })
	public boolean isJsfSetupAvailable() {
		return !jsfOperations.isInstalledInModule(projectOperations.getFocusedModuleName()) && !projectOperations.isFeatureInstalledInFocusedModule("MVC");
	}

	@CliAvailabilityIndicator({ "web jsf all", "web jsf scaffold", "web jsf media" })
	public boolean isJsfInstalled() {
		return jsfOperations.isScaffoldOrMediaAdditionAvailable();
	}

	@CliCommand(value = "web jsf setup", help = "Set up JSF environment")
	public void webJsfSetup(
		@CliOption(key = "implementation", mandatory = false, help = "The JSF implementation to use") final JsfImplementation jsfImplementation,
		@CliOption(key = "theme", mandatory = false, help = "The name of the theme") final Theme theme) {

		jsfOperations.setup(jsfImplementation, theme);
	}

	@CliCommand(value = "web jsf all", help = "Create JSF managed beans for all entities")
	public void webJsfAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which new JSF managed beans will be placed") final JavaPackage destinationPackage) {

		jsfOperations.generateAll(destinationPackage);
	}

	@CliCommand(value = "web jsf scaffold", help = "Create JSF managed bean for an entity")
	public void webJsfScaffold(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the JSF managed bean to be created") final JavaType managedBean,
		@CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The entity which this JSF managed bean class will create and modify as required") final JavaType entity,
		@CliOption(key = "beanName", mandatory = false, help = "The name of the managed bean to use in the 'name' attribute of the @ManagedBean annotation") final String beanName,
		@CliOption(key = "includeOnMenu", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Include this entity on the generated JSF menu") final boolean includeOnMenu,
		@CliOption(key = "createConverter", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Create JSF converter for the entity") final boolean createConverter) {

		jsfOperations.createManagedBean(managedBean, entity, beanName, includeOnMenu, createConverter);
	}

	@CliCommand(value = "web jsf media", help = "Add a cross-browser generic player to embed multimedia content")
	public void webJsfMedia(
		@CliOption(key = "url", mandatory = true, help = "The url of the media source") final String url, 
		@CliOption(key = "player", mandatory = false, help = "The name of the media player") final MediaPlayer mediaPlayer) {

		jsfOperations.addMediaSuurce(url, mediaPlayer);
	}

	@CliCommand(value = "field file", help ="Adds a field for storing uploaded file contents")
	public void addFileUploadField(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the file upload field to add") final JavaSymbolName fieldName,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "contentType", mandatory = true, help = "The content type of the file") final UploadedFileContentType contentType,
		@CliOption(key = "autoUpload", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether the file is uploaded automatically when selected") final boolean autoUpload,
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") final String column,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		jsfOperations.addFileUploadField(fieldName, typeName, contentType, autoUpload, column, notNull, permitReservedWords);
	}
}
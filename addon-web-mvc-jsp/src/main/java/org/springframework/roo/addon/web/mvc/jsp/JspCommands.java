package org.springframework.roo.addon.web.mvc.jsp;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18n;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for Web-related add-on to be used by the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component 
@Service 
public class JspCommands implements CommandMarker {
	private Logger log = Logger.getLogger(getClass().getName());
	@Reference private JspOperations jspOperations;

	@CliAvailabilityIndicator({ "web mvc controller", "controller class", "web mvc install view", "web mvc view", "web mvc update tags" }) 
	public boolean isControllerClassAvailable() {
		return jspOperations.isControllerAvailable();
	}
	
	@CliAvailabilityIndicator({ "web mvc setup" })
	public boolean isProjectAvailable() {
		return jspOperations.isSetupAvailable();
	}

	@CliCommand(value = "web mvc setup", help = "Setup a basic project structure for a Spring MVC / JSP application") 
	public void webMvcSetup() {
		jspOperations.installCommonViewArtefacts();
	}

	@CliCommand(value = "web mvc controller", help = "Create a new manual Controller (ie where you write the methods)") 
	public void newMvcArtifact(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the controller object to be created") JavaType controller, 
		@CliOption(key = "preferredMapping", mandatory = false, help = "Indicates a specific request mapping path for this controller (eg /foo/)") String preferredMapping) {
		
		jspOperations.createManualController(controller, preferredMapping);
	}
	
	@Deprecated
	@CliCommand(value = "controller class", help = "Create a new manual Controller (ie where you write the methods)") 
	public void newController(
		@CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the controller object to be created") JavaType controller, 
		@CliOption(key = "preferredMapping", mandatory = false, help = "Indicates a specific request mapping path for this controller (eg /foo/)") String preferredMapping) {
		
		newMvcArtifact(controller, preferredMapping);
	}

	@CliAvailabilityIndicator({ "web mvc install language", "web mvc language" }) 
	public boolean isInstallLanguageAvailable() {
		return jspOperations.isInstallLanguageCommandAvailable();
	}
	
	@CliCommand(value = "web mvc language", help = "Install new internationalization bundle for MVC scaffolded UI.") 
	public void language(
		@CliOption(key = { "", "code" }, mandatory = true, help = "The language code for the desired bundle") I18n i18n) {
		if (i18n == null) {
			log.warning("Could not parse language code");
			return;
		}
		jspOperations.installI18n(i18n);
	}
	
	@Deprecated
	@CliCommand(value = "web mvc install language", help = "Install new internationalization bundle for MVC scaffolded UI.") 
	public void lang(
		@CliOption(key = { "", "code" }, mandatory = true, help = "The language code for the desired bundle") I18n i18n) {
		if (i18n == null) {
			log.warning("Could not parse language code");
			return;
		}
		jspOperations.installI18n(i18n);
	}

	@CliCommand(value = "web mvc view", help = "Create a new static view.") 
	public void view(@CliOption(key = "path", mandatory = true, help = "The path the static view to create in (required, ie '/foo/blah')") String path, 
		@CliOption(key = "viewName", mandatory = true, help = "The view name the mapping this view should adopt (required, ie 'index')") String viewName, 
		@CliOption(key = "title", mandatory = true, help = "The title of the view") String title) {
		
		jspOperations.installView(path, viewName, title, "View");
	}
	
	@Deprecated
	@CliCommand(value = "web mvc install view", help = "Create a new static view.") 
	public void installView(
		@CliOption(key = "path", mandatory = true, help = "The path the static view to create in (required, ie '/foo/blah')") String path, 
		@CliOption(key = "viewName", mandatory = true, help = "The view name the mapping this view should adopt (required, ie 'index')") String viewName, 
		@CliOption(key = "title", mandatory = true, help = "The title of the view") String title) {
	
		view(path, viewName, title);
	}
	
	@CliCommand(value = "web mvc update tags", help = "Replace an existing application tagx library with the latest version (use --backup option to backup your application first)") 
	public void update(
			@CliOption(key = "backup", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Backup your application before replacing your existing tag library") boolean backup) {
		
		jspOperations.updateTags(backup);
	}
}
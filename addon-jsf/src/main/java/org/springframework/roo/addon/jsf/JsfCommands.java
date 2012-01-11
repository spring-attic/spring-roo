package org.springframework.roo.addon.jsf;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.converters.JavaTypeConverter;
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

    @CliAvailabilityIndicator({ "web jsf all", "web jsf scaffold",
            "web jsf media" })
    public boolean isJsfInstalled() {
        return jsfOperations.isScaffoldOrMediaAdditionAvailable();
    }

    @CliAvailabilityIndicator({ "web jsf setup" })
    public boolean isJsfSetupAvailable() {
        return jsfOperations.isJsfInstallationPossible();
    }

    @CliCommand(value = "web jsf all", help = "Create JSF managed beans for all entities")
    public void webJsfAll(
            @CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which new JSF managed beans will be placed") final JavaPackage destinationPackage) {

        jsfOperations.generateAll(destinationPackage);
    }

    @CliCommand(value = "web jsf media", help = "Add a cross-browser generic player to embed multimedia content")
    public void webJsfMedia(
            @CliOption(key = "url", mandatory = true, help = "The url of the media source") final String url,
            @CliOption(key = "player", mandatory = false, help = "The name of the media player") final MediaPlayer mediaPlayer) {

        jsfOperations.addMediaSuurce(url, mediaPlayer);
    }

    @CliCommand(value = "web jsf scaffold", help = "Create JSF managed bean for an entity")
    public void webJsfScaffold(
            @CliOption(key = { "class", "" }, mandatory = true, help = "The path and name of the JSF managed bean to be created") final JavaType managedBean,
            @CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = JavaTypeConverter.PROJECT, help = "The entity which this JSF managed bean class will create and modify as required") final JavaType entity,
            @CliOption(key = "beanName", mandatory = false, help = "The name of the managed bean to use in the 'name' attribute of the @ManagedBean annotation") final String beanName,
            @CliOption(key = "includeOnMenu", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Include this entity on the generated JSF menu") final boolean includeOnMenu) {

        jsfOperations.createManagedBean(managedBean, entity, beanName,
                includeOnMenu);
    }

    @CliCommand(value = "web jsf setup", help = "Set up JSF environment")
    public void webJsfSetup(
            @CliOption(key = "implementation", mandatory = false, help = "The JSF implementation to use") final JsfImplementation jsfImplementation,
            @CliOption(key = "library", mandatory = false, help = "The JSF component library to use") final JsfLibrary jsfLibrary,
            @CliOption(key = "theme", mandatory = false, help = "The name of the theme") final Theme theme) {

        jsfOperations.setup(jsfImplementation, jsfLibrary, theme);
    }
}
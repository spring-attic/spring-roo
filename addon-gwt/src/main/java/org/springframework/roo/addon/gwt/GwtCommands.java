package org.springframework.roo.addon.gwt;

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
 * Commands for the GWT add-on to be used by the Roo shell.
 * 
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.1
 */
@Component
@Service
public class GwtCommands implements CommandMarker {

    @Reference protected GwtOperations gwtOperations;

    @Deprecated
    @CliCommand(value = "gwt setup", help = "Install Google Web Toolkit (GWT) into your project - deprecated, use 'web gwt setup' instead")
    public void installGwt() {
        gwtOperations.setup();
    }

    @CliAvailabilityIndicator({ "web gwt setup", "gwt setup" })
    public boolean isGwtSetupAvailable() {
        return gwtOperations.isGwtInstallationPossible();
    }

    @CliAvailabilityIndicator({ "web gwt proxy all", "web gwt proxy type",
            "web gwt request all", "web gwt request type", "web gwt all",
            "web gwt scaffold", "web gwt proxy request all",
            "web gwt proxy request type", "web gwt gae update" })
    public boolean isScaffoldAvailable() {
        return gwtOperations.isScaffoldAvailable();
    }

    @CliCommand(value = "web gwt proxy all", help = "Locates all entities in the project and creates GWT proxies")
    public void proxyAll(
            @CliOption(key = "package", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The package in which created proxies will be placed") final JavaPackage javaPackage) {

        gwtOperations.proxyAll(javaPackage);
    }

    @CliCommand(value = "web gwt proxy request all", help = "Locates all entities in the project and creates GWT requests and proxies")
    public void proxyAndRequestAll(
            @CliOption(key = "package", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The package in which created proxies and requests will be placed") final JavaPackage javaPackage) {

        gwtOperations.proxyAndRequestAll(javaPackage);
    }

    @CliCommand(value = "web gwt proxy request type", help = "Creates a proxy and request based on the specified type")
    public void proxyAndRequestType(
            @CliOption(key = "package", mandatory = true, help = "The package in which created proxies and requests will be placed") final JavaPackage javaPackage,
            @CliOption(key = "type", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The type to base the created proxy and request on") final JavaType type) {

        gwtOperations.proxyAndRequestType(javaPackage, type);
    }

    @CliCommand(value = "web gwt proxy type", help = "Creates a GWT proxy based on the specified type")
    public void proxyType(
            @CliOption(key = "package", mandatory = true, help = "The package in which created proxies will be placed") final JavaPackage javaPackage,
            @CliOption(key = "type", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The type to base the created request on") final JavaType type) {

        gwtOperations.proxyType(javaPackage, type);
    }

    @CliCommand(value = "web gwt request all", help = "Locates all entities in the project and creates GWT requests")
    public void requestAll(
            @CliOption(key = "package", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The package in which created requests will be placed") final JavaPackage javaPackage) {

        gwtOperations.requestAll(javaPackage);
    }

    @CliCommand(value = "web gwt request type", help = "Creates a GWT proxy based on the specified type")
    public void requestType(
            @CliOption(key = "package", mandatory = true, help = "The package in which created requests will be placed") final JavaPackage javaPackage,
            @CliOption(key = "type", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The type to base the created request on") final JavaType type) {

        gwtOperations.requestType(javaPackage, type);
    }

    @CliCommand(value = "web gwt all", help = "Locates all entities in the project and creates GWT requests, proxies and creates the scaffold")
    public void scaffoldAll(
            @CliOption(key = "proxyPackage", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The package in which created proxies will be placed") final JavaPackage proxyPackage,
            @CliOption(key = "requestPackage", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The package in which created requests will be placed") final JavaPackage requestPackage) {

        gwtOperations.scaffoldAll(proxyPackage, requestPackage);
    }

    @CliCommand(value = "web gwt scaffold", help = "Creates a GWT request, proxy and scaffold for the specified")
    public void scaffoldType(
            @CliOption(key = "proxyPackage", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The package in which created proxies will be placed") final JavaPackage proxyPackage,
            @CliOption(key = "requestPackage", mandatory = true, optionContext = JavaTypeConverter.PROJECT, help = "The package in which created requests will be placed") final JavaPackage requestPackage,
            @CliOption(key = "type", mandatory = true, help = "The type to base the created scaffold on") final JavaType type) {

        gwtOperations.scaffoldType(proxyPackage, requestPackage, type);
    }

    @CliCommand(value = "web gwt gae update", help = "Updates the GWT project to support GAE")
    public void updateGaeConfiguration() {
        gwtOperations.updateGaeConfiguration();
    }

    @CliCommand(value = "web gwt setup", help = "Install Google Web Toolkit (GWT) into your project")
    public void webGwtSetup() {
        gwtOperations.setup();
    }
}
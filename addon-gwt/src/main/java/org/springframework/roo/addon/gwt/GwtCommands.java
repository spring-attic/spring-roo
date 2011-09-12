package org.springframework.roo.addon.gwt;

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
 * Commands for the GWT add-on to be used by the Roo shell.
 *
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.1
 */
@Component
@Service
public class GwtCommands implements CommandMarker {
	
	// Fields
	@Reference private GwtOperations gwtOperations;

	@CliAvailabilityIndicator({ "web gwt setup", "gwt setup" })
	public boolean isSetupAvailable() {
		return gwtOperations.isSetupAvailable();
	}

	@CliAvailabilityIndicator({ "web gwt proxy all", "web gwt proxy type", "web gwt request all", "web gwt request", "web gwt all", "web gwt scaffold ", "web gwt proxy request all", "web gwt proxy request type", "web gwt gae update" })
	public boolean isGwtEnabled() {
		return gwtOperations.isGwtEnabled();
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

	@CliCommand(value = "web gwt proxy all")
	public void proxyAll(@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which created proxies will be placed") JavaPackage javaPackage) {
		gwtOperations.proxyAll(javaPackage);
	}

	@CliCommand(value = "web gwt proxy type")
	public void proxyType(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which created proxies will be placed") JavaPackage javaPackage,
		@CliOption(key = "type", mandatory = true, help = "The type to base the created request on") JavaType type) {
		gwtOperations.proxyType(javaPackage, type);
	}

	@CliCommand(value = "web gwt request all")
	public void requestAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which created requests will be placed") JavaPackage javaPackage) {
		gwtOperations.requestAll(javaPackage);
	}

	@CliCommand(value = "web gwt request type")
	public void requestType(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which created requests will be placed") JavaPackage javaPackage,
		@CliOption(key = "type", mandatory = true, help = "The type to base the created request on") JavaType type) {
		gwtOperations.requestType(javaPackage, type);
	}

	@CliCommand(value = "web gwt proxy request all")
	public void proxyAndRequestAll(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which created proxies and requests will be placed") JavaPackage javaPackage) {
		gwtOperations.proxyAndRequestAll(javaPackage);
	}

	@CliCommand(value = "web gwt proxy request type")
	public void proxyAndRequestType(
		@CliOption(key = "package", mandatory = true, optionContext = "update", help = "The package in which created proxies and requests will be placed") JavaPackage javaPackage,
		@CliOption(key = "type", mandatory = true, help = "The type to base the created proxy and request on") JavaType type) {
		gwtOperations.proxyAndRequestType(javaPackage, type);
	}

	@CliCommand(value = "web gwt all")
	public void scaffoldAll(
		@CliOption(key = "proxyPackage", mandatory = true, optionContext = "update", help = "The package in which created proxies will be placed") JavaPackage proxyPackage,
		@CliOption(key = "requestPackage", mandatory = true, optionContext = "update", help = "The package in which created requests will be placed") JavaPackage requestPackage) {
		gwtOperations.scaffoldAll(proxyPackage, requestPackage);
	}

	@CliCommand(value = "web gwt scaffold")
	public void scaffoldType(
		@CliOption(key = "proxyPackage", mandatory = true, optionContext = "update", help = "The package in which created proxies will be placed") JavaPackage proxyPackage,
		@CliOption(key = "requestPackage", mandatory = true, optionContext = "update", help = "The package in which created requests will be placed") JavaPackage requestPackage,
		@CliOption(key = "type", mandatory = true, help = "The type to base the created scaffold on") JavaType type) {
		gwtOperations.scaffoldType(proxyPackage, requestPackage, type);
	}

	@CliCommand(value = "web gwt gae update")
	public void updateGae() {
		gwtOperations.updateGaeConfiguration();
	}
}
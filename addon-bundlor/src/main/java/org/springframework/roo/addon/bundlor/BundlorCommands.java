package org.springframework.roo.addon.bundlor;

import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the Bundlor add-on to be used by the Roo shell
 * 
 * @author Adrian Colyer
 * @since 1.0
 *
 */
@ScopeDevelopment
public class BundlorCommands implements CommandMarker {

	private final BundlorOperations bundlorOperations;
	
	public BundlorCommands(BundlorOperations bundlorOperations) {
		Assert.notNull(bundlorOperations, "bundlor operations required");
		this.bundlorOperations = bundlorOperations;
	}
	
	// install bundlor
	
	@CliAvailabilityIndicator("install bundlor")
	public boolean isInstallBundlorAvailable() {
		return bundlorOperations.isInstallBundlorAvailable();	 
	}
	
	@CliCommand(value="install bundlor", 
			   help="automatically create and manage an OSGi manifest for this project using Bundlor")
	public void installBundlor(
		@CliOption(key={"bundleName"},mandatory=false,help="Human readable name for the bundle") String bundleName)
	{
		bundlorOperations.installBundlor(bundleName);
	}
	
	// template management
	
	@CliAvailabilityIndicator(
			{"bundlor non-exported packages",
			 "bundlor version package exports",
		     "bundlor configure imports",
		     "bundlor add explicit import",
		     "bundlor show template"})
	public boolean isTemplateManagementAvailable() {
		return bundlorOperations.isTemplateManagementAvailable();
	}
	
	
	@CliCommand(value="bundlor non-exported packages",
				help="specify packages that should not be exported by the bundle")
	public void bundlorMakePrivate(
			@CliOption(key="packagePattern",mandatory=true,help="package name with optional .* postfix")
			String packagePattern) {
		bundlorOperations.excludeFromExport(packagePattern);
	}
	
	@CliCommand(value="bundlor version package exports",
			help="specify version to export individual packages at (defaults to bundle version)")
	public void bundlorVersionExports(
		@CliOption(key="packagePattern",mandatory=true,help="package name with optional .* postfix")
		String packagePattern,
		@CliOption(key="version",mandatory=true,help="version to export package at")
		String version) {
		bundlorOperations.versionExports(packagePattern, version);	
	}

	@CliCommand(value="bundlor configure imports",
			help="specify version range for package imports, and whether they are optional")
	public void bundlorConfigureImports(
		@CliOption(key="packagePattern",mandatory=true,help="package name with optional .* postfix")
		String packagePattern,
		@CliOption(key="fromVersion",mandatory=true,help="minimum version to accept")
		String fromVersion,
		@CliOption(key="toVersion",mandatory=true,help="version range ceiling, use INF for infinity")
		String toVersion,
		@CliOption(key="inclusiveUpperBound",mandatory=false,specifiedDefaultValue="true",unspecifiedDefaultValue="false",help="should upper version be inclusive (default = exclusive)")
		boolean inclusiveUpperBound,
		@CliOption(key="optional",mandatory=false,specifiedDefaultValue="true",unspecifiedDefaultValue="false",help="treat package import as optional (default = mandatory)")
		boolean optional) {
		bundlorOperations.configureImports(packagePattern, fromVersion, toVersion, inclusiveUpperBound, optional);
	}
	
	@CliCommand(value="bundlor add explicit import",
			help="Add an explicit import for a dependency Bundlor cannot detect")
	public void bundlorAddImport(
			@CliOption(key="package",mandatory=true,help="package to import")
			String packageName,
			@CliOption(key="fromVersion",mandatory=true,help="minimum version to accept")
			String fromVersion,
			@CliOption(key="toVersion",mandatory=true,help="version range ceiling, use INF for infinity")
			String toVersion,
			@CliOption(key="inclusiveUpperBound",mandatory=false,specifiedDefaultValue="true",unspecifiedDefaultValue="false",help="should upper version be inclusive (default = exclusive)")
			boolean inclusiveUpperBound,
			@CliOption(key="optional",mandatory=false,specifiedDefaultValue="true",unspecifiedDefaultValue="false",help="treat package import as optional (default = mandatory)")
			boolean optional) {
		bundlorOperations.addExplicitImport(packageName, fromVersion, toVersion, inclusiveUpperBound, optional);		
	}
	
}

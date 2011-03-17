package org.springframework.roo.project;

import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands for {@link MavenOperations} and also to launch native mvn commands.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class MavenCommands implements CommandMarker {
	@Reference private MavenOperations mavenOperations;

	@CliAvailabilityIndicator("project")
	public boolean isCreateProjectAvailable() {
		return mavenOperations.isCreateProjectAvailable();
	}

	@CliCommand(value = "project", help = "Creates a new Maven project")
	public void createProject(
		@CliOption(key = { "", "topLevelPackage" }, mandatory = true, optionContext = "update", help = "The uppermost package name (this becomes the <groupId> in Maven and also the '~' value when using Roo's shell)") JavaPackage topLevelPackage, 
		@CliOption(key = "projectName", mandatory = false, help = "The name of the project (last segment of package name used as default)") String projectName, 
		@CliOption(key = "java", mandatory = false, help = "Forces a particular major version of Java to be used (will be auto-detected if unspecified; specify 5 or 6 or 7 only)") Integer majorJavaVersion) {
		
		mavenOperations.createProject(topLevelPackage, projectName, majorJavaVersion);
	}

	@CliAvailabilityIndicator({ "dependency add", "dependency remove" })
	public boolean isDependencyModificationAllowed() {
		return mavenOperations.isProjectAvailable();
	}

	@CliCommand(value = "dependency add", help = "Adds a new dependency to the Maven project object model (POM)")
	public void addDependency(
		@CliOption(key = "groupId", mandatory = true, help = "The group ID of the dependency") String groupId, 
		@CliOption(key = "artifactId", mandatory = true, help = "The artifact ID of the dependency") String artifactId, 
		@CliOption(key = "version", mandatory = true, help = "The version of the dependency") String version) {
		
		mavenOperations.addDependency(groupId, artifactId, version);
	}

	@CliCommand(value = "dependency remove", help = "Removes an existing dependency from the Maven project object model (POM)")
	public void removeDependency(
		@CliOption(key = "groupId", mandatory = true, help = "The group ID of the dependency") String groupId, 
		@CliOption(key = "artifactId", mandatory = true, help = "The artifact ID of the dependency") String artifactId, 
		@CliOption(key = "version", mandatory = true, help = "The version of the dependency") String version) {
		
		mavenOperations.removeDependency(groupId, artifactId, version);
	}

	@CliAvailabilityIndicator({ "perform package", "perform eclipse", "perform tests", "perform clean", "perform assembly", "perform command" })
	public boolean isPerformCommandAllowed() {
		return mavenOperations.isProjectAvailable();
	}

	@CliCommand(value = { "perform package" }, help = "Packages the application using Maven, but does not execute any tests")
	public void runPackage() throws IOException {
		mvn("-DskipTests=true package");
	}

	@CliCommand(value = { "perform eclipse" }, help = "Sets up Eclipse configuration via Maven (only necessary if you have not installed the m2eclipse plugin in Eclipse)")
	public void runEclipse() throws IOException {
		mvn("eclipse:clean eclipse:eclipse");
	}

	@CliCommand(value = { "perform tests" }, help = "Executes the tests via Maven")
	public void runTest() throws IOException {
		mvn("test");
	}

	@CliCommand(value = { "perform assembly" }, help = "Executes the assembly goal via Maven")
	public void runAssembly() throws IOException {
		mvn("assembly:assembly");
	}

	@CliCommand(value = { "perform clean" }, help = "Executes a full clean (including Eclipse files) via Maven")
	public void runClean() throws IOException {
		mvn("clean");
	}

	@CliCommand(value = { "perform command" }, help = "Executes a user-specified Maven command")
	public void mvn(
		@CliOption(key = "mavenCommand", mandatory = true, help = "User-specified Maven command (eg test:test)") String extra) throws IOException {
		
		mavenOperations.executeMvnCommand(extra);
	}
}

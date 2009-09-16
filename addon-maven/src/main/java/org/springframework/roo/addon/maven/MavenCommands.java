package org.springframework.roo.addon.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;

@ScopeDevelopmentShell
public class MavenCommands implements CommandMarker {
	
	private MavenOperations mavenOperations;
	private ApplicationContextOperations applicationContextOperations;
	protected final Logger logger = Logger.getLogger(getClass().getName());

	public MavenCommands(MavenOperations mavenOperations, ApplicationContextOperations applicationContextOperations) {
		Assert.notNull(mavenOperations, "Maven operations required");
		Assert.notNull(applicationContextOperations, "Application context operations required");
		this.mavenOperations = mavenOperations;
		this.applicationContextOperations = applicationContextOperations;
	}

	@CliAvailabilityIndicator("create project")
	public boolean isCreateProjectAvailable() {
		return mavenOperations.isCreateProjectAvailable();
	}
	
	@CliCommand(value="create project", help="Creates a new project")
	public void createProject(@CliOption(key={"", "topLevelPackage"}, mandatory=true, help="The uppermost package name") JavaPackage topLevelPackage,
			@CliOption(key="projectName", mandatory=false, help="The name of the project (last segment of package name used as default)") String projectName,
			@CliOption(key="java", mandatory=false, help="Forces a particular major version of Java to be used (will be auto-detected if unspecified; specify 5 or 6 or 7 only)") Integer majorJavaVersion) {
		if (projectName == null) {
			String packageName = topLevelPackage.getFullyQualifiedPackageName();
			int lastIndex = packageName.lastIndexOf(".");
			if (lastIndex == -1) {
				projectName = packageName;
			} else {
				projectName = packageName.substring(lastIndex+1);
			}
		}
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "pom-template.xml");
		mavenOperations.createProject(templateInputStream, topLevelPackage, projectName, majorJavaVersion);
		applicationContextOperations.createMiddleTierApplicationContext();
	}
	
	@CliAvailabilityIndicator({"add dependency", "remove dependency"})
	public boolean isDependencyModificationAllowed() {
		return mavenOperations.isDependencyModificationAllowed();
	}

	@CliCommand(value="add dependency", help="Adds a new dependency to the Maven project object model (POM)")
	public void addDependency(@CliOption(key="groupId", mandatory=true) JavaPackage groupId, @CliOption(key="artifactId", mandatory=true) JavaSymbolName artifactId, @CliOption(key="version", mandatory=true) String version) {
		mavenOperations.addDependency(groupId, artifactId, version);
	}
	
	@CliCommand(value="remove dependency", help="Removes an existing dependency from the Maven project object model (POM)")
	public void removeDependency(@CliOption(key="groupId", mandatory=true) JavaPackage groupId, @CliOption(key="artifactId", mandatory=true) JavaSymbolName artifactId, @CliOption(key="version", mandatory=true) String version) {
		mavenOperations.removeDependency(groupId, artifactId, version);
	}
	
	@CliAvailabilityIndicator({"perform package", "perform eclipse", "perform tests", "perform clean", "perform command"})
	public boolean isPerformCommandAllowed() {
		return mavenOperations.isPerformCommandAllowed();
	}

	@CliCommand(value={"perform package"}, help="Packages the application using Maven, but does not execute any tests")
	public void runPackage() throws IOException {
		mvn("-Dmaven.test.skip=true package");
	}

	@CliCommand(value={"perform eclipse"}, help="Sets up Eclipse configuration via Maven")
	public void runEclipse() throws IOException {
		mvn("eclipse:clean eclipse:eclipse");
	}

	@CliCommand(value={"perform tests"}, help="Executes the tests via Maven")
	public void runTest() throws IOException {
		mvn("test");
	}

	@CliCommand(value={"perform clean"}, help="Executes a full clean (including Eclipse files) via Maven")
	public void runClean() throws IOException {
		mvn("clean eclipse:clean");
	}
	
	@CliCommand(value={"perform command"}, help="Executes a user-specified Maven command")
	public void mvn(@CliOption(key="mavenCommand", mandatory=true) String extra) throws IOException {
		BufferedReader input = null;
		try {
			Process p = Runtime.getRuntime().exec("mvn " + extra);
		    input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    String line;
		    while ((line = input.readLine()) != null) {
		    	logger.info(line);
		    }
	    } catch (IOException ioe) {
	    	if (ioe.getMessage().contains("No such file or directory")) {
	    		logger.log(Level.SEVERE, "Could not locate Maven executable; please ensure mvn command is in your path");
	    		return;
	    	}
	    	throw ioe;
	    } finally {
	    	if (input != null) {
	    		input.close();
	    	}
	    }
	}

}

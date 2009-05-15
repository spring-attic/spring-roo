package org.springframework.roo.addon.maven;

import java.io.InputStream;

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
	private WebXmlOperations webXmlOperations;
	
	public MavenCommands(MavenOperations mavenOperations, ApplicationContextOperations applicationContextOperations, WebXmlOperations webXmlOperations) {
		Assert.notNull(mavenOperations, "Maven operations required");
		Assert.notNull(applicationContextOperations, "Application context operations required");
		Assert.notNull(webXmlOperations, "Web XML operations required");
		this.mavenOperations = mavenOperations;
		this.applicationContextOperations = applicationContextOperations;
		this.webXmlOperations = webXmlOperations;
	}

	@CliAvailabilityIndicator("create project")
	public boolean isCreateProjectAvailable() {
		return mavenOperations.isCreateProjectAvailable();
	}
	
	@CliCommand(value="create project", help="Creates a new project")
	public void createProject(@CliOption(key={"", "topLevelPackage"}, mandatory=true, help="The uppermost package name") JavaPackage topLevelPackage,
			@CliOption(key="projectName", mandatory=false, help="The name of the project (last segment of package name used as default)") String projectName) {
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
		mavenOperations.createProject(templateInputStream, topLevelPackage, projectName);
		
		applicationContextOperations.createMiddleTierApplicationContext();
		applicationContextOperations.createWebApplicationContext();
		webXmlOperations.createWebXml();
		webXmlOperations.createIndexJsp();
		webXmlOperations.copyUrlRewrite();
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
}

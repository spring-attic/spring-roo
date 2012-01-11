package org.springframework.roo.project;

import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.project.packaging.JarPackaging;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands for {@link MavenOperations} and also to launch native mvn
 * commands.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class MavenCommands implements CommandMarker {

    private static final String DEPENDENCY_ADD_COMMAND = "dependency add";
    private static final String DEPENDENCY_REMOVE_COMMAND = "dependency remove";
    private static final String MODULE_CREATE_COMMAND = "module create";
    private static final String MODULE_FOCUS_COMMAND = "module focus";
    private static final String PERFORM_ASSEMBLY_COMMAND = "perform assembly";
    private static final String PERFORM_CLEAN_COMMAND = "perform clean";
    private static final String PERFORM_COMMAND_COMMAND = "perform command";
    private static final String PERFORM_ECLIPSE_COMMAND = "perform eclipse";
    private static final String PERFORM_PACKAGE_COMMAND = "perform package";
    private static final String PERFORM_TESTS_COMMAND = "perform tests";
    private static final String PROJECT_COMMAND = "project";
    private static final String REPOSITORY_ADD_COMMAND = "maven repository add";
    private static final String REPOSITORY_REMOVE_COMMAND = "maven repository remove";

    @Reference private MavenOperations mavenOperations;

    @CliCommand(value = DEPENDENCY_ADD_COMMAND, help = "Adds a new dependency to the Maven project object model (POM)")
    public void addDependency(
            @CliOption(key = "groupId", mandatory = true, help = "The group ID of the dependency") final String groupId,
            @CliOption(key = "artifactId", mandatory = true, help = "The artifact ID of the dependency") final String artifactId,
            @CliOption(key = "version", mandatory = true, help = "The version of the dependency") final String version,
            @CliOption(key = "classifier", help = "The classifier of the dependency") final String classifier,
            @CliOption(key = "scope", help = "The scope of the dependency") final DependencyScope scope) {

        mavenOperations.addDependency(mavenOperations.getFocusedModuleName(),
                groupId, artifactId, version, scope, classifier);
    }

    @CliCommand(value = REPOSITORY_ADD_COMMAND, help = "Adds a new repository to the Maven project object model (POM)")
    public void addRepository(
            @CliOption(key = "id", mandatory = true, help = "The ID of the repository") final String id,
            @CliOption(key = "name", mandatory = false, help = "The name of the repository") final String name,
            @CliOption(key = "url", mandatory = true, help = "The URL of the repository") final String url) {

        mavenOperations.addRepository(mavenOperations.getFocusedModuleName(),
                new Repository(id, name, url));
    }

    @CliCommand(value = MODULE_CREATE_COMMAND, help = "Creates a new Maven module")
    public void createModule(
            @CliOption(key = "moduleName", mandatory = true, help = "The name of the module") final String moduleName,
            @CliOption(key = "topLevelPackage", mandatory = true, optionContext = "update", help = "The uppermost package name (this becomes the <groupId> in Maven and also the '~' value when using Roo's shell)") final JavaPackage topLevelPackage,
            @CliOption(key = "java", help = "Forces a particular major version of Java to be used (will be auto-detected if unspecified; specify 5 or 6 or 7 only)") final Integer majorJavaVersion,
            @CliOption(key = "parent", help = "The Maven coordinates of the parent POM, in the form \"groupId:artifactId:version\"") final GAV parentPom,
            @CliOption(key = "packaging", help = "The Maven packaging of this module", unspecifiedDefaultValue = JarPackaging.NAME) final PackagingProvider packaging) {

        mavenOperations.createModule(topLevelPackage, parentPom, moduleName,
                packaging, majorJavaVersion);
    }

    @CliCommand(value = PROJECT_COMMAND, help = "Creates a new Maven project")
    public void createProject(
            @CliOption(key = { "", "topLevelPackage" }, mandatory = true, optionContext = "update", help = "The uppermost package name (this becomes the <groupId> in Maven and also the '~' value when using Roo's shell)") final JavaPackage topLevelPackage,
            @CliOption(key = "projectName", help = "The name of the project (last segment of package name used as default)") final String projectName,
            @CliOption(key = "java", help = "Forces a particular major version of Java to be used (will be auto-detected if unspecified; specify 5 or 6 or 7 only)") final Integer majorJavaVersion,
            @CliOption(key = "parent", help = "The Maven coordinates of the parent POM, in the form \"groupId:artifactId:version\"") final GAV parentPom,
            @CliOption(key = "packaging", help = "The Maven packaging of this project", unspecifiedDefaultValue = JarPackaging.NAME) final PackagingProvider packaging) {

        mavenOperations.createProject(topLevelPackage, projectName,
                majorJavaVersion, parentPom, packaging);
    }

    @CliCommand(value = MODULE_FOCUS_COMMAND, help = "Changes focus to a different project module")
    public void focusModule(
            @CliOption(key = "moduleName", mandatory = true, help = "The module to focus on") final Pom module) {

        mavenOperations.setModule(module);
    }

    @CliAvailabilityIndicator(PROJECT_COMMAND)
    public boolean isCreateProjectAvailable() {
        return mavenOperations.isCreateProjectAvailable();
    }

    @CliAvailabilityIndicator({ DEPENDENCY_ADD_COMMAND,
            DEPENDENCY_REMOVE_COMMAND })
    public boolean isDependencyModificationAllowed() {
        return mavenOperations.isFocusedProjectAvailable();
    }

    @CliAvailabilityIndicator(MODULE_CREATE_COMMAND)
    public boolean isModuleCreationAllowed() {
        return mavenOperations.isModuleCreationAllowed();
    }

    @CliAvailabilityIndicator(MODULE_FOCUS_COMMAND)
    public boolean isModuleFocusAllowed() {
        return mavenOperations.isModuleFocusAllowed();
    }

    @CliAvailabilityIndicator({ PERFORM_PACKAGE_COMMAND,
            PERFORM_ECLIPSE_COMMAND, PERFORM_TESTS_COMMAND,
            PERFORM_CLEAN_COMMAND, PERFORM_ASSEMBLY_COMMAND,
            PERFORM_COMMAND_COMMAND })
    public boolean isPerformCommandAllowed() {
        return mavenOperations.isFocusedProjectAvailable();
    }

    @CliCommand(value = { PERFORM_COMMAND_COMMAND }, help = "Executes a user-specified Maven command")
    public void mvn(
            @CliOption(key = "mavenCommand", mandatory = true, help = "User-specified Maven command (eg test:test)") final String command)
            throws IOException {

        mavenOperations.executeMvnCommand(command);
    }

    @CliCommand(value = DEPENDENCY_REMOVE_COMMAND, help = "Removes an existing dependency from the Maven project object model (POM)")
    public void removeDependency(
            @CliOption(key = "groupId", mandatory = true, help = "The group ID of the dependency") final String groupId,
            @CliOption(key = "artifactId", mandatory = true, help = "The artifact ID of the dependency") final String artifactId,
            @CliOption(key = "version", mandatory = true, help = "The version of the dependency") final String version,
            @CliOption(key = "classifier", help = "The classifier of the dependency") final String classifier) {

        mavenOperations.removeDependency(
                mavenOperations.getFocusedModuleName(), groupId, artifactId,
                version, classifier);
    }

    @CliCommand(value = REPOSITORY_REMOVE_COMMAND, help = "Removes an existing repository from the Maven project object model (POM)")
    public void removeRepository(
            @CliOption(key = "id", mandatory = true, help = "The ID of the repository") final String id,
            @CliOption(key = "url", mandatory = true, help = "The URL of the repository") final String url) {

        mavenOperations.removeRepository(
                mavenOperations.getFocusedModuleName(), new Repository(id,
                        null, url));
    }

    @CliCommand(value = { PERFORM_ASSEMBLY_COMMAND }, help = "Executes the assembly goal via Maven")
    public void runAssembly() throws IOException {
        mvn("assembly:assembly");
    }

    @CliCommand(value = { PERFORM_CLEAN_COMMAND }, help = "Executes a full clean (including Eclipse files) via Maven")
    public void runClean() throws IOException {
        mvn("clean");
    }

    @CliCommand(value = { PERFORM_ECLIPSE_COMMAND }, help = "Sets up Eclipse configuration via Maven (only necessary if you have not installed the m2eclipse plugin in Eclipse)")
    public void runEclipse() throws IOException {
        mvn("eclipse:clean eclipse:eclipse");
    }

    @CliCommand(value = { PERFORM_PACKAGE_COMMAND }, help = "Packages the application using Maven, but does not execute any tests")
    public void runPackage() throws IOException {
        mvn("-DskipTests=true package");
    }

    @CliCommand(value = { PERFORM_TESTS_COMMAND }, help = "Executes the tests via Maven")
    public void runTest() throws IOException {
        mvn("test");
    }
}

package org.springframework.roo.project;

import static org.springframework.roo.shell.OptionContexts.UPDATE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.project.packaging.JarPackaging;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.packaging.PackagingProviderRegistry;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;



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

  protected final static Logger LOGGER = HandlerUtils.getLogger(MavenCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

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
  private static final String REPOSITORY_ADD_COMMAND = "maven repository add";
  private static final String REPOSITORY_REMOVE_COMMAND = "maven repository remove";

  private MavenOperations mavenOperations;
  private ProjectOperations projectOperations;

  @Reference
  PackagingProviderRegistry packagingProviderRegistry;

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
  }

  /* @CliCommand(value = DEPENDENCY_ADD_COMMAND,
       help = "Adds a new dependency to the Maven project object model (POM)") */
  public void addDependency(
      @CliOption(key = "groupId", mandatory = true, help = "The group ID of the dependency") final String groupId,
      @CliOption(key = "artifactId", mandatory = true, help = "The artifact ID of the dependency") final String artifactId,
      @CliOption(key = "version", mandatory = true, help = "The version of the dependency") final String version,
      @CliOption(key = "classifier", help = "The classifier of the dependency") final String classifier,
      @CliOption(key = "scope", help = "The scope of the dependency") final DependencyScope scope) {

    getMavenOperations().addDependency(getMavenOperations().getFocusedModuleName(), groupId,
        artifactId, version, scope, classifier);
  }

  /*  @CliCommand(value = REPOSITORY_ADD_COMMAND,
        help = "Adds a new repository to the Maven project object model (POM)") */
  public void addRepository(@CliOption(key = "id", mandatory = true,
      help = "The ID of the repository") final String id, @CliOption(key = "name",
      mandatory = false, help = "The name of the repository") final String name, @CliOption(
      key = "url", mandatory = true, help = "The URL of the repository") final String url) {

    getMavenOperations().addRepository(getMavenOperations().getFocusedModuleName(),
        new Repository(id, name, url));
  }

  @CliOptionAutocompleteIndicator(command = MODULE_CREATE_COMMAND, param = "packaging",
      help = "--packaging select one of the available Maven packaging values")
  public List<String> returnPomModules(ShellContext shellContext) {
    List<String> allPossibleValues = new ArrayList<String>();
    Collection<PackagingProvider> allPackagingProviders =
        packagingProviderRegistry.getAllPackagingProviders();

    for (final PackagingProvider packagingProvider : allPackagingProviders) {
      // ROO-3801 Can't create POM children
      if (!"pom".equals(packagingProvider.getId())) {
        allPossibleValues.add(packagingProvider.getId().toUpperCase());
      }
    }
    return allPossibleValues;
  }

  @CliCommand(value = MODULE_CREATE_COMMAND,
      help = "Creates a new Maven module in current *multimodule* project.")
  public void createModule(@CliOption(key = "moduleName", mandatory = true,
      help = "The name of the module to create.") final String moduleName, @CliOption(
      key = "packaging", help = "The Maven packaging of this module."
          + "Possible values are: `BUNDLE`, `EAR`, `ESA`, `JAR` and `WAR`."
          + "Default if option not present: `JAR` (equals to 'jar').",
      unspecifiedDefaultValue = JarPackaging.NAME) final PackagingProvider packaging, @CliOption(
      key = "artifactId", help = "The artifact ID of this module."
          + "Default if option not present: `--moduleName` value.") final String artifactId) {

    getMavenOperations().createModule(moduleName, packaging, artifactId);
  }

  @CliCommand(value = MODULE_FOCUS_COMMAND,
      help = "Changes Roo Shell focus to a different project "
          + "module, when in a multimodule project.")
  public void focusModule(
      @CliOption(key = "moduleName", mandatory = true, optionContext = UPDATE,
          help = "The module name to focus on."
              + "Possible values are: any of the project module names (`~` for root module).") final Pom module) {

    getMavenOperations().setModule(module);
  }

  @CliAvailabilityIndicator({DEPENDENCY_ADD_COMMAND, DEPENDENCY_REMOVE_COMMAND})
  public boolean isDependencyModificationAllowed() {

    return getMavenOperations().isFocusedProjectAvailable();
  }

  @CliAvailabilityIndicator({REPOSITORY_ADD_COMMAND, REPOSITORY_REMOVE_COMMAND})
  public boolean isRepositoryModificationAllowed() {

    return getMavenOperations().isFocusedProjectAvailable();
  }

  @CliAvailabilityIndicator(MODULE_CREATE_COMMAND)
  public boolean isModuleCreationAllowed() {

    return getMavenOperations().isModuleCreationAllowed();
  }

  @CliAvailabilityIndicator(MODULE_FOCUS_COMMAND)
  public boolean isModuleFocusAllowed() {


    return getMavenOperations().isModuleFocusAllowed();
  }

  @CliAvailabilityIndicator({PERFORM_PACKAGE_COMMAND, PERFORM_ECLIPSE_COMMAND,
      PERFORM_TESTS_COMMAND, PERFORM_CLEAN_COMMAND, PERFORM_ASSEMBLY_COMMAND,
      PERFORM_COMMAND_COMMAND})
  public boolean isPerformCommandAllowed() {
    return getMavenOperations().isFocusedProjectAvailable();
  }

  /* @CliCommand(value = {PERFORM_COMMAND_COMMAND}, help = "Executes a user-specified Maven command") */
  public void mvn(@CliOption(key = "mavenCommand", mandatory = true,
      help = "User-specified Maven command (eg test:test)") final String command)
      throws IOException {

    getMavenOperations().executeMvnCommand(command);
  }

  /*  @CliCommand(value = DEPENDENCY_REMOVE_COMMAND,
        help = "Removes an existing dependency from the Maven project object model (POM)") */
  public void removeDependency(
      @CliOption(key = "groupId", mandatory = true, help = "The group ID of the dependency") final String groupId,
      @CliOption(key = "artifactId", mandatory = true, help = "The artifact ID of the dependency") final String artifactId,
      @CliOption(key = "version", mandatory = true, help = "The version of the dependency") final String version,
      @CliOption(key = "classifier", help = "The classifier of the dependency") final String classifier) {


    getMavenOperations().removeDependency(getMavenOperations().getFocusedModuleName(), groupId,
        artifactId, version, classifier);
  }

  /* @CliCommand(value = REPOSITORY_REMOVE_COMMAND,
       help = "Removes an existing repository from the Maven project object model (POM)") */
  public void removeRepository(@CliOption(key = "id", mandatory = true,
      help = "The ID of the repository") final String id, @CliOption(key = "url", mandatory = true,
      help = "The URL of the repository") final String url) {

    getMavenOperations().removeRepository(getMavenOperations().getFocusedModuleName(),
        new Repository(id, null, url));
  }

  /* @CliCommand(value = {PERFORM_ASSEMBLY_COMMAND}, help = "Executes the assembly goal via Maven") */
  public void runAssembly() throws IOException {
    mvn("assembly:assembly");
  }

  /* @CliCommand(value = {PERFORM_CLEAN_COMMAND},
       help = "Executes a full clean (including Eclipse files) via Maven") */
  public void runClean() throws IOException {
    mvn("clean");
  }

  /* @CliCommand(
       value = {PERFORM_ECLIPSE_COMMAND},
       help = "Sets up Eclipse configuration via Maven (only necessary if you have not installed the m2eclipse plugin in Eclipse)") */
  public void runEclipse() throws IOException {
    mvn("eclipse:clean eclipse:eclipse");
  }

  /* @CliCommand(value = {PERFORM_PACKAGE_COMMAND},
       help = "Packages the application using Maven, but does not execute any tests") */
  public void runPackage() throws IOException {
    mvn("-DskipTests=true package");
  }

  /* @CliCommand(value = {PERFORM_TESTS_COMMAND}, help = "Executes the tests via Maven") */
  public void runTest() throws IOException {
    mvn("test");
  }

  public MavenOperations getMavenOperations() {
    if (mavenOperations == null) {
      // Get all Services implement MavenOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MavenOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (MavenOperations) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MavenOperations on MavenCommands.");
        return null;
      }
    } else {
      return mavenOperations;
    }

  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on MavenCommands.");
        return null;
      }
    } else {
      return projectOperations;
    }

  }
}

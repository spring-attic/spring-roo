package org.springframework.roo.addon.jpa.addon.audit;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

/**
 * This class registers Spring Roo commands to include Jpa Audit support
 * in the generated project
 *
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class JpaAuditCommands implements CommandMarker {

  // Project Settings
  private static final String SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME =
      "spring.roo.jpa.require.schema-object-name";

  @Reference
  JpaAuditOperations auditOperations;
  @Reference
  TypeLocationService typeLocationService;
  @Reference
  ProjectOperations projectOperations;
  @Reference
  private ProjectSettingsService projectSettings;

  @CliAvailabilityIndicator("jpa audit setup")
  public boolean isSetupAuditAvailable() {
    return auditOperations.isJpaAuditSetupPossible();
  }

  @CliAvailabilityIndicator("jpa audit add")
  public boolean isAddAuditAvailable() {
    return auditOperations.isJpaAuditAddPossible();
  }

  @CliOptionVisibilityIndicator(command = "jpa audit setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(params = "module", command = "jpa audit setup")
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisible(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }


  @CliOptionMandatoryIndicator(params = "package", command = "jpa audit setup")
  public boolean isPackageRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() <= 1
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliCommand(value = "jpa audit setup",
      help = "Installs audit support into your project, preparing it " + "to audit entity changes.")
  public void setupAudit(
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where to install audit support."
              + "This option is mandatory if the focus is not set in an application module, that is, a "
              + "module containing an `@SpringBootApplication` class."
              + "This option is available only if there are more than one application module and none of "
              + "them is focused."
              + "Default if option not present: the unique 'application' module, or focused 'application'"
              + " module.", unspecifiedDefaultValue = ".",
          optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module) {
    auditOperations.setupJpaAudit(module);
  }

  @CliOptionMandatoryIndicator(command = "jpa audit add", params = {"createdDateColumn",
      "modifiedDateColumn", "createdByColumn", "modifiedByColumn"})
  public boolean areColumnRelatedParamsMandatory(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @CliCommand(value = "jpa audit add",
      help = "Adds support for auditing a JPA entity. This will add JPA "
          + "and Spring listeners to this entity to record the entity changes.")
  public void auditAdd(
      @CliOption(
          key = "entity",
          mandatory = true,
          help = "The entity which should be audited. When working on a mono module project, simply "
              + "specify the name of the entity. If you consider it necessary, you can also specify "
              + "the package. Ex.: `--class ~.domain.MyEntity` (where `~` is the base package). When "
              + "working with multiple modules, you should specify the name of the class and the "
              + "module where it is. Ex.: `--class model:~.domain.MyEntity`. If the module is not "
              + "specified, it is assumed that the entity is in the module which has the focus.") final JavaType entity,
      @CliOption(key = "createdDateColumn", mandatory = true,
          help = "The DB column used for storing the date when each record is created."
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` "
              + "configuration setting exists and it's `true`.") final String createdDateColumn,
      @CliOption(key = "modifiedDateColumn", mandatory = true,
          help = "The DB column used for storing the date when each record is modified."
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` "
              + "configuration setting exists and it's `true`.") final String modifiedDateColumn,
      @CliOption(
          key = "createdByColumn",
          mandatory = true,
          help = "The DB column used for storing information about who creates each record."
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration "
              + "setting exists and it's `true`.") final String createdByColumn,
      @CliOption(
          key = "modifiedByColumn",
          mandatory = true,
          help = "The DB column used for storing information about who modifies each record."
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration "
              + "setting exists and it's `true`.") final String modifiedByColumn) {

    // Check if entity exists
    final ClassOrInterfaceTypeDetails entityDetails = typeLocationService.getTypeDetails(entity);
    Validate.notNull(entityDetails, "ERROR: The type specified, '%s', doesn't exist", entity);

    // Check if entity is a valid entity
    Validate.notNull(entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
        "'%s' is not a valid entity. It should be annotated with @RooEntity", entity);

    auditOperations.addJpaAuditToEntity(entity, createdDateColumn, modifiedDateColumn,
        createdByColumn, modifiedByColumn);
  }
}

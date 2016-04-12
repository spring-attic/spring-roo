package org.springframework.roo.addon.security.addon.audit;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.*;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.shell.*;

/**
 * Commands to be used by the ROO shell for adding audit support.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class AuditCommands implements CommandMarker {

  // Project Settings 
  private static final String SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME =
      "spring.roo.jpa.require.schema-object-name";

  @Reference
  AuditOperations auditOperations;
  @Reference
  TypeLocationService typeLocationService;
  @Reference
  ProjectOperations projectOperations;
  @Reference
  private ProjectSettingsService projectSettings;

  @CliAvailabilityIndicator("audit setup")
  public boolean isSetupAuditAvailable() {
    return auditOperations.isAuditSetupPossible();
  }

  @CliAvailabilityIndicator("audit add")
  public boolean isAddAuditAvailable() {
    return auditOperations.isAuditAddPossible();
  }


  @CliOptionMandatoryIndicator(params = "package", command = "audit setup")
  public boolean isPackageRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() <= 1
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliCommand(value = "audit setup", help = "Install audit support into your project")
  public void setupAudit(
      @CliOption(
          key = "package",
          mandatory = true,
          optionContext = APPLICATION_FEATURE,
          help = "The package in which new classes needed for audit will be placed. Note that module will be ignored as new classes will be installed in app's config module/s") final JavaPackage javaPackage) {
    auditOperations.setupAudit(javaPackage);
  }

  @CliOptionMandatoryIndicator(command = "audit add", params = {"createdDateColumn",
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

  @CliCommand(value = "audit add", help = "Adds support for auditing a JPA entity")
  public void auditAdd(
      @CliOption(key = "entity", mandatory = true, help = "The entity which should be audited") final JavaType entity,
      @CliOption(key = "createdDateColumn", mandatory = true,
          help = "The DB column used for storing created date info") final String createdDateColumn,
      @CliOption(key = "modifiedDateColumn", mandatory = true,
          help = "The DB column used for storing modified date info") final String modifiedDateColumn,
      @CliOption(key = "createdByColumn", mandatory = true,
          help = "The DB column used for storing created by info") final String createdByColumn,
      @CliOption(key = "modifiedByColumn", mandatory = true,
          help = "The DB column used for storing modified by info") final String modifiedByColumn) {

    // Check if entity exists
    final ClassOrInterfaceTypeDetails entityDetails = typeLocationService.getTypeDetails(entity);
    Validate.notNull(entityDetails, "The type specified, '%s', doesn't exist", entity);

    // Check if entity is a valid entity
    Validate.notNull(entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
        "'%s' is not a valid entity. It should be annotated with @RooEntity", entity);

    auditOperations.auditAdd(entity, createdDateColumn, modifiedDateColumn, createdByColumn,
        modifiedByColumn);
  }
}

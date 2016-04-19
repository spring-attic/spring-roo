package org.springframework.roo.addon.jpa.addon.entity;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.addon.field.addon.FieldCreatorProvider;

/**
 * Provides field creation operations support for JPA entities by implementing
 * FieldCreatorProvider.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JpaFieldCreatorProvider implements FieldCreatorProvider {

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private ProjectSettingsService projectSettings;

  private static final String SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME =
      "spring.roo.jpa.require.schema-object-name";
  public static final String ROO_DEFAULT_JOIN_TABLE_NAME = "_ROO_JOIN_TABLE_";

  @Override
  public boolean isValid(JavaType javaType) {
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);
    if (cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null
        || cid.getAnnotation(JpaJavaType.ENTITY) != null) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isFieldManagementAvailable() {
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(JpaJavaType.ENTITY,
            RooJavaType.ROO_JPA_ENTITY);
    if (!entities.isEmpty()) {
      return true;
    }
    return false;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldBoolean(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldBoolean(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldBoolean(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldDate(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldDate(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isPersistenceTypeVisibleForFieldDate(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldDate(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldEnum(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldEnum(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isEnumTypeVisibleForFieldEnum(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldEnum(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldNumber(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldNumber(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isUniqueVisibleForFieldNumber(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldNumber(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldReference(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isJoinColumnNameVisibleForFieldReference(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isReferencedColumnNameVisibleForFieldReference(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isCardinalityVisibleForFieldReference(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isFetchVisibleForFieldReference(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldReference(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isCascadeTypeVisibleForFieldReference(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean areJoinTableParamsMandatoryForFieldSet(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    // See if joinTable param has been specified
    String joinTableParam = shellContext.getParameters().get("joinTable");

    if (joinTableParam != null && requiredSchemaObjectName != null
        && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isJoinTableMandatoryForFieldSet(ShellContext shellContext) {

    String cardinality = shellContext.getParameters().get("cardinality");

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (cardinality != null && cardinality.equals("MANY_TO_MANY")
        && requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean areJoinTableParamsVisibleForFieldSet(ShellContext shellContext) {

    String joinTableParam = shellContext.getParameters().get("joinTable");

    if (joinTableParam != null) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isMappedByVisibleForFieldSet(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isCardinalityVisibleForFieldSet(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isFetchVisibleForFieldSet(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldSet(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isJoinTableVisibleForFieldSet(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean areJoinTableParamsVisibleForFieldList(ShellContext shellContext) {

    String joinTableParam = shellContext.getParameters().get("joinTable");

    if (joinTableParam != null) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isJoinTableMandatoryForFieldList(ShellContext shellContext) {

    String cardinality = shellContext.getParameters().get("cardinality");

    if (cardinality != null && cardinality.equals("MANY_TO_MANY")) {
      return true;
    }

    return false;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean areJoinTableParamsMandatoryForFieldList(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    // See if joinTable param has been specified
    String joinTableParam = shellContext.getParameters().get("joinTable");

    if (joinTableParam != null && requiredSchemaObjectName != null
        && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isMappedByVisibleForFieldList(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isCardinalityVisibleForFieldList(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isFetchVisibleForFieldList(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldList(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isJoinTableVisibleForFieldList(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldString(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldString(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isUniqueVisibleForFieldString(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isTransientVisibleForFieldString(ShellContext shellContext) {
    return true;
  }

  @Override
  public boolean isLobVisibleForFieldString(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldFile(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldFile(ShellContext shellContext) {
    return true;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project settings
   *         and its value is "true". If not, return false.
   */
  public boolean isColumnMandatoryForFieldOther(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  public boolean isColumnVisibleForFieldOther(ShellContext shellContext) {
    return true;
  }

  public boolean isTransientVisibleForFieldOther(ShellContext shellContext) {
    return true;
  }

}

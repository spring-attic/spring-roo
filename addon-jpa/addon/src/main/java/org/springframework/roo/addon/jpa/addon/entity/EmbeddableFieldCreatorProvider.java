package org.springframework.roo.addon.jpa.addon.entity;

import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.ENTITY;
import static org.springframework.roo.model.SpringJavaType.PERSISTENT;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.field.addon.FieldCreatorProvider;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.comments.CommentFormatter;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Cascade;
import org.springframework.roo.classpath.operations.DateTime;
import org.springframework.roo.classpath.operations.EnumType;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.classpath.operations.jsr303.BooleanField;
import org.springframework.roo.classpath.operations.jsr303.CollectionField;
import org.springframework.roo.classpath.operations.jsr303.DateField;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.operations.jsr303.EmbeddedField;
import org.springframework.roo.classpath.operations.jsr303.EnumField;
import org.springframework.roo.classpath.operations.jsr303.ListField;
import org.springframework.roo.classpath.operations.jsr303.NumericField;
import org.springframework.roo.classpath.operations.jsr303.ReferenceField;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.classpath.operations.jsr303.StringField;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileContentType;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileField;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.shell.ShellContext;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Provides field creation operations support for embeddable classes by implementing
 * FieldCreatorProvider.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class EmbeddableFieldCreatorProvider implements FieldCreatorProvider {

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private ProjectSettingsService projectSettings;
  @Reference
  private MetadataService metadataService;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  private static final String SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME =
      "spring.roo.jpa.require.schema-object-name";

  public static final String ROO_DEFAULT_JOIN_TABLE_NAME = "_ROO_JOIN_TABLE_";

  @Override
  public boolean isValid(JavaType javaType) {
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);
    MemberDetails details = memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);
    if (cid.getAnnotation(JpaJavaType.EMBEDDABLE) != null
        || details.getAnnotation(JpaJavaType.EMBEDDABLE) != null) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isFieldManagementAvailable() {
    Set<ClassOrInterfaceTypeDetails> embeddableClasses =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(JpaJavaType.EMBEDDABLE);
    if (!embeddableClasses.isEmpty()) {
      return true;
    }
    return false;
  }

  @Override
  public boolean isFieldEmbeddedAvailable() {
    return false;
  }

  @Override
  public boolean isFieldReferenceAvailable() {
    return false;
  }

  /**
   * ROO-3710: Indicator that checks if exists some project setting that makes
   * table column parameter mandatory.
   * 
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldBoolean(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldDate(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldEnum(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldNumber(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldReference(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean areJoinTableParamsMandatoryForFieldSet(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on project settings
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isJoinTableMandatoryForFieldSet(ShellContext shellContext) {

    String cardinality = shellContext.getParameters().get("cardinality");

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on project settings
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean areJoinTableParamsMandatoryForFieldList(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on project settings
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldString(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @Override
  public boolean isColumnMandatoryForFieldFile(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  public boolean isColumnMandatoryForFieldOther(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
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

  @Override
  public void createBooleanField(ClassOrInterfaceTypeDetails javaTypeDetails, boolean primitive,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean assertFalse,
      boolean assertTrue, String column, String comment, String value, boolean permitReservedWords,
      boolean transientModifier) {

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final BooleanField fieldDetails =
        new BooleanField(physicalTypeIdentifier, primitive ? JavaType.BOOLEAN_PRIMITIVE
            : JavaType.BOOLEAN_OBJECT, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    fieldDetails.setAssertFalse(assertFalse);
    fieldDetails.setAssertTrue(assertTrue);
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createDateField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean future,
      boolean past, DateFieldPersistenceType persistenceType, String column, String comment,
      DateTime dateFormat, DateTime timeFormat, String pattern, String value,
      boolean permitReservedWords, boolean transientModifier) {

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final DateField fieldDetails = new DateField(physicalTypeIdentifier, fieldType, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    fieldDetails.setFuture(future);
    fieldDetails.setPast(past);
    if (JdkJavaType.isDateField(fieldType)) {
      fieldDetails.setPersistenceType(persistenceType != null ? persistenceType
          : DateFieldPersistenceType.JPA_TIMESTAMP);
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (dateFormat != null) {
      fieldDetails.setDateFormat(dateFormat);
    }
    if (timeFormat != null) {
      fieldDetails.setTimeFormat(timeFormat);
    }
    if (pattern != null) {
      fieldDetails.setPattern(pattern);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createEnumField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, String column, boolean notNull, boolean nullRequired,
      EnumType enumType, String comment, boolean permitReservedWords, boolean transientModifier) {

    ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getTypeDetails(fieldType);
    Validate.isTrue(typeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION,
        "The field type is not an enum class.");

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final EnumField fieldDetails = new EnumField(physicalTypeIdentifier, fieldType, fieldName);
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (enumType != null) {
      fieldDetails.setEnumType(enumType);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createEmbeddedField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean permitReservedWords) {

    // Check if the requested entity is a JPA @Entity
    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final PhysicalTypeMetadata targetTypeMetadata =
        (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
    Validate
        .notNull(targetTypeMetadata,
            "The specified target '--class' does not exist or can not be found. Please create this type first.");
    final PhysicalTypeDetails targetPtd = targetTypeMetadata.getMemberHoldingTypeDetails();
    Validate.isInstanceOf(MemberHoldingTypeDetails.class, targetPtd);

    final ClassOrInterfaceTypeDetails targetTypeCid = (ClassOrInterfaceTypeDetails) targetPtd;
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), targetTypeCid);
    Validate
        .isTrue(
            memberDetails.getAnnotation(ENTITY) != null
                || memberDetails.getAnnotation(PERSISTENT) != null,
            "The field embedded command is only applicable to JPA @Entity or Spring Data @Persistent target types.");

    final EmbeddedField fieldDetails =
        new EmbeddedField(physicalTypeIdentifier, fieldType, fieldName);

    insertField(fieldDetails, permitReservedWords, false);
  }

  @Override
  public void createNumericField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      boolean primitive, Set<String> legalNumericPrimitives, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax,
      Integer digitsInteger, Integer digitsFraction, Long min, Long max, String column,
      String comment, boolean unique, String value, boolean permitReservedWords,
      boolean transientModifier) {

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    if (primitive && legalNumericPrimitives.contains(fieldType.getFullyQualifiedTypeName())) {
      fieldType =
          new JavaType(fieldType.getFullyQualifiedTypeName(), 0, DataType.PRIMITIVE, null, null);
    }
    final NumericField fieldDetails =
        new NumericField(physicalTypeIdentifier, fieldType, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (decimalMin != null) {
      fieldDetails.setDecimalMin(decimalMin);
    }
    if (decimalMax != null) {
      fieldDetails.setDecimalMax(decimalMax);
    }
    if (digitsInteger != null) {
      fieldDetails.setDigitsInteger(digitsInteger);
    }
    if (digitsFraction != null) {
      fieldDetails.setDigitsFraction(digitsFraction);
    }
    if (min != null) {
      fieldDetails.setMin(min);
    }
    if (max != null) {
      fieldDetails.setMax(max);
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (unique) {
      fieldDetails.setUnique(true);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    Validate.isTrue(fieldDetails.isDigitsSetCorrectly(),
        "Must specify both --digitsInteger and --digitsFractional for @Digits to be added");

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createReferenceField(ClassOrInterfaceTypeDetails cid, Cardinality cardinality,
      JavaType typeName, JavaType fieldType, JavaSymbolName fieldName, Cascade cascadeType,
      boolean notNull, boolean nullRequired, String joinColumnName, String referencedColumnName,
      Fetch fetch, String comment, boolean permitReservedWords, boolean transientModifier) {

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    final AnnotationMetadata persistentAnnotation = memberDetails.getAnnotation(PERSISTENT);
    Validate
        .isTrue(
            entityAnnotation != null || persistentAnnotation != null,
            "The field reference command is only applicable to JPA @Entity or Spring Data @Persistent target types.");

    Validate.isTrue(
        cardinality == Cardinality.MANY_TO_ONE || cardinality == Cardinality.ONE_TO_ONE,
        "Cardinality must be MANY_TO_ONE or ONE_TO_ONE for the field reference command");

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final ReferenceField fieldDetails =
        new ReferenceField(physicalTypeIdentifier, fieldType, fieldName, cardinality, cascadeType);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (joinColumnName != null) {
      fieldDetails.setJoinColumnName(joinColumnName);
    }
    if (referencedColumnName != null) {
      Validate.notNull(joinColumnName,
          "@JoinColumn name is required if specifying a referencedColumnName");
      fieldDetails.setReferencedColumnName(referencedColumnName);
    }
    if (fetch != null) {
      fieldDetails.setFetch(fetch);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createSetField(ClassOrInterfaceTypeDetails cid, Cardinality cardinality,
      JavaType typeName, JavaType fieldType, JavaSymbolName fieldName, Cascade cascadeType,
      boolean notNull, boolean nullRequired, Integer sizeMin, Integer sizeMax,
      JavaSymbolName mappedBy, Fetch fetch, String comment, String joinTable, String joinColumns,
      String referencedColumns, String inverseJoinColumns, String inverseReferencedColumns,
      boolean permitReservedWords, boolean transientModifier) {

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    final AnnotationMetadata persistentAnnotation = memberDetails.getAnnotation(PERSISTENT);

    if (entityAnnotation != null) {
      Validate.isTrue(cardinality == Cardinality.ONE_TO_MANY
          || cardinality == Cardinality.MANY_TO_MANY,
          "Cardinality must be ONE_TO_MANY or MANY_TO_MANY for the field set command");
    } else if (cid.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION) {
      cardinality = null;
    } else if (persistentAnnotation != null) {
      // Yes, we can deal with that
    } else {
      throw new IllegalStateException(
          "The field set command is only applicable to enum, JPA @Entity or Spring Data @Persistence elements");
    }

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final SetField fieldDetails =
        new SetField(physicalTypeIdentifier, new JavaType(SET.getFullyQualifiedTypeName(), 0,
            DataType.TYPE, null, Arrays.asList(fieldType)), fieldName, fieldType, cardinality,
            cascadeType, false);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (sizeMin != null) {
      fieldDetails.setSizeMin(sizeMin);
    }
    if (sizeMax != null) {
      fieldDetails.setSizeMax(sizeMax);
    }
    if (mappedBy != null) {
      fieldDetails.setMappedBy(mappedBy);
    }
    if (fetch != null) {
      fieldDetails.setFetch(fetch);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (joinTable != null) {

      // Create strings arrays and set @JoinTable annotation
      String[] joinColumnsArray = null;
      String[] referencedColumnsArray = null;
      String[] inverseJoinColumnsArray = null;
      String[] inverseReferencedColumnsArray = null;
      if (joinColumns != null) {
        joinColumnsArray = joinColumns.replace(" ", "").split(",");
      }
      if (referencedColumns != null) {
        referencedColumnsArray = referencedColumns.replace(" ", "").split(",");
      }
      if (inverseJoinColumns != null) {
        inverseJoinColumnsArray = inverseJoinColumns.replace(" ", "").split(",");
      }
      if (inverseReferencedColumns != null) {
        inverseReferencedColumnsArray = inverseReferencedColumns.replace(" ", "").split(",");
      }

      // Validate same number of elements
      if (joinColumnsArray != null && referencedColumnsArray != null) {
        Validate.isTrue(joinColumnsArray.length == referencedColumnsArray.length,
            "--joinColumns and --referencedColumns must have same number of column values");
      }
      if (inverseJoinColumnsArray != null && inverseReferencedColumnsArray != null) {
        Validate
            .isTrue(inverseJoinColumnsArray.length == inverseReferencedColumnsArray.length,
                "--inverseJoinColumns and --inverseReferencedColumns must have same number of column values");
      }

      fieldDetails.setJoinTableAnnotation(joinTable, joinColumnsArray, referencedColumnsArray,
          inverseJoinColumnsArray, inverseReferencedColumnsArray);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createListField(ClassOrInterfaceTypeDetails cid, Cardinality cardinality,
      JavaType typeName, JavaType fieldType, JavaSymbolName fieldName, Cascade cascadeType,
      boolean notNull, boolean nullRequired, Integer sizeMin, Integer sizeMax,
      JavaSymbolName mappedBy, Fetch fetch, String comment, String joinTable, String joinColumns,
      String referencedColumns, String inverseJoinColumns, String inverseReferencedColumns,
      boolean permitReservedWords, boolean transientModifier) {

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    final AnnotationMetadata persistentAnnotation = memberDetails.getAnnotation(PERSISTENT);

    if (entityAnnotation != null) {
      Validate.isTrue(cardinality == Cardinality.ONE_TO_MANY
          || cardinality == Cardinality.MANY_TO_MANY,
          "Cardinality must be ONE_TO_MANY or MANY_TO_MANY for the field list command");
    } else if (cid.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION) {
      cardinality = null;
    } else if (persistentAnnotation != null) {
      // Yes, we can deal with that
    } else {
      throw new IllegalStateException(
          "The field list command is only applicable to enum, JPA @Entity or Spring "
              + "Data @Persistence elements");
    }

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s' doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final ListField fieldDetails =
        new ListField(physicalTypeIdentifier, new JavaType(LIST.getFullyQualifiedTypeName(), 0,
            DataType.TYPE, null, Arrays.asList(fieldType)), fieldName, fieldType, cardinality,
            cascadeType, false);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (sizeMin != null) {
      fieldDetails.setSizeMin(sizeMin);
    }
    if (sizeMax != null) {
      fieldDetails.setSizeMax(sizeMax);
    }
    if (mappedBy != null) {
      fieldDetails.setMappedBy(mappedBy);
    }
    if (fetch != null) {
      fieldDetails.setFetch(fetch);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (joinTable != null) {

      // Create strings arrays and set @JoinTable annotation
      String[] joinColumnsArray = null;
      String[] referencedColumnsArray = null;
      String[] inverseJoinColumnsArray = null;
      String[] inverseReferencedColumnsArray = null;
      if (joinColumns != null) {
        joinColumnsArray = joinColumns.replace(" ", "").split(",");
      }
      if (referencedColumns != null) {
        referencedColumnsArray = referencedColumns.replace(" ", "").split(",");
      }
      if (inverseJoinColumns != null) {
        inverseJoinColumnsArray = inverseJoinColumns.replace(" ", "").split(",");
      }
      if (inverseReferencedColumns != null) {
        inverseReferencedColumnsArray = inverseReferencedColumns.replace(" ", "").split(",");
      }

      // Validate same number of elements
      if (joinColumnsArray != null && referencedColumnsArray != null) {
        Validate.isTrue(joinColumnsArray.length == referencedColumnsArray.length,
            "--joinColumns and --referencedColumns must have same number of column values");
      }
      if (inverseJoinColumnsArray != null && inverseReferencedColumnsArray != null) {
        Validate.isTrue(inverseJoinColumnsArray.length == inverseReferencedColumnsArray.length,
            "--inverseJoinColumns and --inverseReferencedColumns must have same "
                + "number of column values");
      }

      fieldDetails.setJoinTableAnnotation(joinTable, joinColumnsArray, referencedColumnsArray,
          inverseJoinColumnsArray, inverseReferencedColumnsArray);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createStringField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax, Integer sizeMin,
      Integer sizeMax, String regexp, String column, String comment, boolean unique, String value,
      boolean lob, boolean permitReservedWords, boolean transientModifier) {

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final StringField fieldDetails = new StringField(physicalTypeIdentifier, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (decimalMin != null) {
      fieldDetails.setDecimalMin(decimalMin);
    }
    if (decimalMax != null) {
      fieldDetails.setDecimalMax(decimalMax);
    }
    if (sizeMin != null) {
      fieldDetails.setSizeMin(sizeMin);
    }
    if (sizeMax != null) {
      fieldDetails.setSizeMax(sizeMax);
    }
    if (regexp != null) {
      fieldDetails.setRegexp(regexp.replace("\\", "\\\\"));
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (unique) {
      fieldDetails.setUnique(true);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    if (lob) {
      fieldDetails.getInitedAnnotations().add(
          new AnnotationMetadataBuilder("javax.persistence.Lob"));

      // ROO-3722: Add LAZY load in @Lob fields using @Basic
      AnnotationMetadataBuilder basicAnnotation =
          new AnnotationMetadataBuilder("javax.persistence.Basic");
      basicAnnotation.addEnumAttribute("fetch", new EnumDetails(new JavaType(
          "javax.persistence.FetchType"), new JavaSymbolName("LAZY")));
      fieldDetails.getInitedAnnotations().add(basicAnnotation);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @Override
  public void createFileField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      UploadedFileContentType contentType, boolean autoUpload, boolean notNull, String column,
      boolean permitReservedWords) {

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final UploadedFileField fieldDetails =
        new UploadedFileField(physicalTypeIdentifier, fieldName, contentType);
    fieldDetails.setAutoUpload(autoUpload);
    fieldDetails.setNotNull(notNull);
    if (column != null) {
      fieldDetails.setColumn(column);
    }

    insertField(fieldDetails, permitReservedWords, false);
  }

  @Override
  public void createOtherField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, String comment,
      String column, boolean permitReservedWords, boolean transientModifier) {

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final FieldDetails fieldDetails =
        new FieldDetails(physicalTypeIdentifier, fieldType, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  public void insertField(final FieldDetails fieldDetails, final boolean permitReservedWords,
      final boolean transientModifier) {

    String module = null;
    if (!permitReservedWords) {
      ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getFieldName());
      if (fieldDetails.getColumn() != null) {
        ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getColumn());
      }
    }

    final List<AnnotationMetadataBuilder> annotations = fieldDetails.getInitedAnnotations();
    fieldDetails.decorateAnnotationsList(annotations);
    fieldDetails.setAnnotations(annotations);

    if (fieldDetails.getFieldType() != null) {
      module = fieldDetails.getFieldType().getModule();
    }

    String initializer = null;
    if (fieldDetails instanceof CollectionField) {
      final CollectionField collectionField = (CollectionField) fieldDetails;
      module = collectionField.getGenericParameterTypeName().getModule();
      initializer = "new " + collectionField.getInitializer() + "()";
    } else if (fieldDetails instanceof DateField
        && fieldDetails.getFieldName().getSymbolName().equals("created")) {
      initializer = "new Date()";
    }
    int modifier = Modifier.PRIVATE;
    if (transientModifier) {
      modifier += Modifier.TRANSIENT;
    }
    fieldDetails.setModifiers(modifier);

    // Format the passed-in comment (if given)
    formatFieldComment(fieldDetails);

    final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails);
    fieldBuilder.setFieldInitializer(initializer);
    typeManagementService.addField(fieldBuilder.build());

    if (module != null) {
      projectOperations.addModuleDependency(module);
    }
  }

  public void formatFieldComment(FieldDetails fieldDetails) {
    // If a comment was defined, we need to format it
    if (fieldDetails.getComment() != null) {

      // First replace all "" with the proper escape sequence \"
      String unescapedMultiLineComment = fieldDetails.getComment().replaceAll("\"\"", "\\\\\"");

      // Then unescape all characters
      unescapedMultiLineComment = StringEscapeUtils.unescapeJava(unescapedMultiLineComment);

      CommentFormatter commentFormatter = new CommentFormatter();
      String javadocComment = commentFormatter.formatStringAsJavadoc(unescapedMultiLineComment);

      fieldDetails.setComment(commentFormatter.format(javadocComment, 1));
    }
  }

  @Override
  public List<String> getFieldSetTypeAllPossibleValues(ShellContext shellContext) {
    // Get current value of class
    String currentText = shellContext.getParameters().get("type");

    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all existing entities
    Set<ClassOrInterfaceTypeDetails> entitiesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entitiesInProject) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    // Getting all existing dtos
    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails dto : dtosInProject) {
      String name = replaceTopLevelPackageString(dto, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @Override
  public List<String> getFieldListTypeAllPossibleValues(ShellContext shellContext) {
    // Get current value of class
    String currentText = shellContext.getParameters().get("type");

    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all existing entities
    Set<ClassOrInterfaceTypeDetails> entitiesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entitiesInProject) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    // Getting all existing dtos
    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails dto : dtosInProject) {
      String name = replaceTopLevelPackageString(dto, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @Override
  public List<String> getFieldEmbeddedAllPossibleValues(ShellContext shellContext) {

    // field embedded not used for embeddable classes
    return new ArrayList<String>();
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for TopLevelPackage
   * 
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value  
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }

}

package org.springframework.roo.addon.dto.addon;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.field.addon.FieldCreatorProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
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
import org.springframework.roo.classpath.operations.jsr303.EnumField;
import org.springframework.roo.classpath.operations.jsr303.NumericField;
import org.springframework.roo.classpath.operations.jsr303.StringField;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileContentType;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileField;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.ShellContext;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides field creation operations support for DTO classes by implementing
 * FieldCreatorProvider.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class DtoFieldCreatorProvider implements FieldCreatorProvider {

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeManagementService typeManagementService;

  @Override
  public boolean isValid(JavaType javaType) {
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);
    if (cid.getAnnotation(RooJavaType.ROO_DTO) != null) {
      return true;
    }

    return false;
  }

  @Override
  public boolean isFieldManagementAvailable() {
    Set<ClassOrInterfaceTypeDetails> dtoClasses =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    if (!dtoClasses.isEmpty()) {
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

  @Override
  public boolean isFieldCollectionAvailable() {
    return false;
  }

  @Override
  public boolean isColumnMandatoryForFieldBoolean(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldBoolean(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldBoolean(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnMandatoryForFieldDate(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldDate(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isPersistenceTypeVisibleForFieldDate(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldDate(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnMandatoryForFieldEnum(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldEnum(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isEnumTypeVisibleForFieldEnum(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldEnum(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnMandatoryForFieldNumber(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldNumber(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isUniqueVisibleForFieldNumber(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldNumber(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnMandatoryForFieldReference(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isJoinColumnNameVisibleForFieldReference(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isReferencedColumnNameVisibleForFieldReference(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isFetchVisibleForFieldReference(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isCascadeTypeVisibleForFieldReference(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean areJoinTableParamsMandatoryForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isJoinTableMandatoryForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean areJoinTableParamsVisibleForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isMappedByVisibleForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isCardinalityVisibleForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isFetchVisibleForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isJoinTableVisibleForFieldSet(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean areJoinTableParamsVisibleForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isJoinTableMandatoryForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean areJoinTableParamsMandatoryForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isMappedByVisibleForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isCardinalityVisibleForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isFetchVisibleForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isJoinTableVisibleForFieldList(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnMandatoryForFieldString(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldString(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isUniqueVisibleForFieldString(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldString(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isLobVisibleForFieldString(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnMandatoryForFieldFile(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isColumnVisibleForFieldFile(ShellContext shellContext) {
    return false;
  }

  public boolean isColumnMandatoryForFieldOther(ShellContext shellContext) {
    return false;
  }

  public boolean isColumnVisibleForFieldOther(ShellContext shellContext) {
    return false;
  }

  public boolean isTransientVisibleForFieldOther(ShellContext shellContext) {
    return false;
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

    insertField(fieldDetails, permitReservedWords, false);
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
      fieldDetails.setPersistenceType(persistenceType);
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

    insertField(fieldDetails, permitReservedWords, false);
  }

  @Override
  public void createEmbeddedField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean permitReservedWords) {

    throw new IllegalArgumentException("'field embedded' command is not available for DTO classes.");
  }

  @Override
  public void createEnumField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, String column, boolean notNull, boolean nullRequired,
      EnumType enumType, String comment, boolean permitReservedWords, boolean transientModifier) {

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

    insertField(fieldDetails, permitReservedWords, false);
  }

  @Override
  public void createReferenceField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean aggregation, JavaSymbolName mappedBy, Cascade[] cascadeType, boolean notNull,
      String joinColumnName, String referencedColumnName, Fetch fetch, String comment,
      boolean permitReservedWords, Boolean orphanRemoval, boolean isForce) {
    throw new IllegalArgumentException(
        "'field reference' command is not available for DTO classes.");
  }

  @Override
  public void createSetField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      Cardinality cardinality, Cascade[] cascadeType, boolean notNull, Integer sizeMin,
      Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch, String comment, String joinTable,
      String joinColumns, String referencedColumns, String inverseJoinColumns,
      String inverseReferencedColumns, boolean permitReservedWords, Boolean aggregation,
      Boolean orphanRemoval, boolean isForce) {
    throw new IllegalArgumentException("'field set' command is not available for DTO classes.");
  }

  @Override
  public void createListField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      Cardinality cardinality, Cascade[] cascadeType, boolean notNull, Integer sizeMin,
      Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch, String comment, String joinTable,
      String joinColumns, String referencedColumns, String inverseJoinColumns,
      String inverseReferencedColumns, boolean permitReservedWords, Boolean aggregation,
      Boolean orphanRemoval, boolean isForce) {
    throw new IllegalArgumentException("'field list' command is not available for DTO classes.");
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

    insertField(fieldDetails, permitReservedWords, false);
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

    insertField(fieldDetails, permitReservedWords, false);
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

    fieldDetails.setModifiers(Modifier.PRIVATE);

    String initializer = null;
    if (fieldDetails instanceof CollectionField) {
      final CollectionField collectionField = (CollectionField) fieldDetails;
      module = collectionField.getGenericParameterTypeName().getModule();
      initializer = "new " + collectionField.getInitializer() + "()";
    } else if (fieldDetails instanceof DateField
        && fieldDetails.getFieldName().getSymbolName().equals("created")) {
      initializer = "new Date()";
    }

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

    // field embedded not used for DTO's
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

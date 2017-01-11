package org.springframework.roo.addon.field.addon;


import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Cascade;
import org.springframework.roo.classpath.operations.DateTime;
import org.springframework.roo.classpath.operations.EnumType;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileContentType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.ShellContext;

import java.util.List;
import java.util.Set;

/**
 * Provides a field creation API which can be implemented by each add-on that
 * creates classes that potentially use field creation commands.
 * <p>
 * This interface permits field creation via Roo commands with different
 * configurations of parameters visibility and mandatory indicators. e.g.
 * DTO classes don't need database related options in field creation commands,
 * so they use their own implementation for making those options not visible.
 *
 * @author Sergio Clares
 * @since 2.0
 */
public interface FieldCreatorProvider {

  /**
   * Whether an implementation of this interface is valid for the class type
   * where the field is going to be created.
   *
   * @return true if the implementation is valid
   */
  boolean isValid(JavaType javaType);

  /**
   * Whether field management commands are available.
   *
   * @return true if field management commands are available
   */
  boolean isFieldManagementAvailable();

  /**
   * Whether field embedded command is available.
   *
   * @return true if field embedded command is available
   */
  boolean isFieldEmbeddedAvailable();

  /**
   * Whether field reference command is available.
   *
   * @return true if field reference command is available
   */
  boolean isFieldReferenceAvailable();

  /**
   * Whether field list/set commands are available.
   *
   * @return true if fields collection commands are available
   */
  boolean isFieldCollectionAvailable();

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldBoolean(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnVisibleForFieldBoolean(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isTransientVisibleForFieldBoolean(ShellContext shellContext);

  /**
   * Whether `--assertFalse` option should be available.
   * 
   * @param shellContext
   * @return `true` if `--assertFalse` is available, `false` otherwise.
   */
  boolean isAssertFalseVisibleForFieldBoolean(ShellContext shellContext);

  /**
   * Whether `--assertTrue` option should be available.
   * 
   * @param shellContext
   * @return `true` if `--assertTrue` is available, `false` otherwise.
   */
  boolean isAssertTrueVisibleForFieldBoolean(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldDate(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnVisibleForFieldDate(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isPersistenceTypeVisibleForFieldDate(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isTransientVisibleForFieldDate(ShellContext shellContext);

  /**
   * Whether `--future` option should be available.
   * 
   * @param shellContext
   * @return `true` if `--future` is available, `false` otherwise.
   */
  boolean isFutureVisibleForFieldDate(ShellContext shellContext);

  /**
   * Whether `--past` option should be available.
   * 
   * @param shellContext
   * @return `true` if `--past` is available, `false` otherwise.
   */
  boolean isPastVisibleForFieldDate(ShellContext shellContext);

  /**
   * Whether `--dateFormat` and `--timeFormat` options should be available.
   * 
   * @param shellContext
   * @return `true` if `--dateFormat` and `--timeFormat` are available, `false` 
   *            otherwise.
   */
  boolean areDateAndTimeFormatVisibleForFieldDate(ShellContext shellContext);

  /**
   * Whether `--dateTimeFormatPattern` option should be available.
   * 
   * @param shellContext
   * @return `true` if `--dateTimeFormatPattern` is available, `false` otherwise.
   */
  boolean isDateTimeFormatPatternVisibleForFieldDate(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field date`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldDate(ShellContext shellContext);

  /**
   * Whether `--nullRequired` option should be available for `field date`.
   * 
   * @param shellContext
   * @return `true` if `--nullRequired` is available, `false` otherwise.
   */
  boolean isNullRequiredVisibleForFieldDate(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldEnum(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnVisibleForFieldEnum(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isEnumTypeVisibleForFieldEnum(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isTransientVisibleForFieldEnum(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field enum`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldEnum(ShellContext shellContext);

  /**
   * Whether `--nullRequired` option should be available for `field enum`.
   * 
   * @param shellContext
   * @return `true` if `--nullRequired` is available, `false` otherwise.
   */
  boolean isNullRequiredVisibleForFieldEnum(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldNumber(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnVisibleForFieldNumber(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isUniqueVisibleForFieldNumber(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isTransientVisibleForFieldNumber(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field enum`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldNumber(ShellContext shellContext);

  /**
   * Whether `--nullRequired` parameter is visible for `field number` command.
   *
   * @param shellContext
   * @return `true` if `--nullRequired` is visible, `false` otherwise. 
   */
  boolean isNullRequiredVisibleForFieldNumber(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isJoinColumnNameVisibleForFieldReference(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isReferencedColumnNameVisibleForFieldReference(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isFetchVisibleForFieldReference(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isCascadeTypeVisibleForFieldReference(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldReference(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field reference`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldReference(ShellContext shellContext);

  /**
   * Whether `--nullRequired` parameter is visible for `field reference` command.
   *
   * @param shellContext
   * @return `true` if `--nullRequired` is visible, `false` otherwise. 
   */
  boolean isNullRequiredVisibleForFieldReference(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean areOptionalParametersVisibleForFieldSet(ShellContext shellContext);

  /**
  * Whether join column related parameter is mandatory for field set command.
  *
  * @param shellContext
  * @return parameter is mandatory
  */
  boolean isJoinColumnNameMandatoryForFieldSet(ShellContext shellContext);

  /**
   * Whether join column related parameter is visible for field set command.
   *
   * @param shellContext
   * @return parameter is mandatory
   */
  boolean isJoinColumnNameVisibleForFieldSet(ShellContext shellContext);

  /**
   * Whether 'referencedColumn' parameter is visible for field list command.
   *
   * @param shellContext
   * @return true when parameter is mandatory
   */
  boolean isReferencedColumnNameVisibleForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean areJoinTableParamsMandatoryForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isJoinTableMandatoryForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean areJoinTableParamsVisibleForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isMappedByVisibleForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isCardinalityVisibleForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isFetchVisibleForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isJoinTableVisibleForFieldSet(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field set`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldSet(ShellContext shellContext);

  /**
   * Whether `--nullRequired` parameter is visible for `field set` command.
   *
   * @param shellContext
   * @return `true` if `--nullRequired` is visible, `false` otherwise. 
   */
  boolean isNullRequiredVisibleForFieldSet(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean areJoinTableParamsVisibleForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isJoinTableMandatoryForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean areJoinTableParamsMandatoryForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isMappedByVisibleForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isCardinalityVisibleForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isFetchVisibleForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isJoinTableVisibleForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean areOptionalParametersVisibleForFieldList(ShellContext shellContext);

  /**
   * Whether join column related parameter is mandatory for field list command.
   *
   * @param shellContext
   * @return true when parameter is mandatory
   */
  boolean isJoinColumnNameMandatoryForFieldList(ShellContext shellContext);

  /**
   * Whether join column related parameter is visible for field list command.
   *
   * @param shellContext
   * @return true when parameter is mandatory
   */
  boolean isJoinColumnNameVisibleForFieldList(ShellContext shellContext);

  /**
   * Whether 'referencedColumn' parameter is visible for field list command.
   *
   * @param shellContext
   * @return true when parameter is mandatory
   */
  boolean isReferencedColumnNameVisibleForFieldList(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field list`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldList(ShellContext shellContext);

  /**
   * Whether `--nullRequired` parameter is visible for `field list` command.
   *
   * @param shellContext
   * @return `true` if `--nullRequired` is visible, `false` otherwise. 
   */
  boolean isNullRequiredVisibleForFieldList(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldString(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnVisibleForFieldString(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isUniqueVisibleForFieldString(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isTransientVisibleForFieldString(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isLobVisibleForFieldString(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field string`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldString(ShellContext shellContext);

  /**
   * Whether `--nullRequired` parameter is visible for `field string` command.
   *
   * @param shellContext
   * @return `true` if `--nullRequired` is visible, `false` otherwise. 
   */
  boolean isNullRequiredVisibleForFieldString(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldFile(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnVisibleForFieldFile(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldOther(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isColumnVisibleForFieldOther(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  boolean isTransientVisibleForFieldOther(ShellContext shellContext);

  /**
   * Whether `--notNull` option should be available for `field other`.
   * 
   * @param shellContext
   * @return `true` if `--notNull` is available, `false` otherwise.
   */
  boolean isNotNullVisibleForFieldOther(ShellContext shellContext);

  /**
   * Whether `--nullRequired` parameter is visible for `field other` command.
   *
   * @param shellContext
   * @return `true` if `--nullRequired` is visible, `false` otherwise. 
   */
  boolean isNullRequiredVisibleForFieldOther(ShellContext shellContext);

  /**
   * TODO
   *
   * @param javaTypeDetails
   * @param primitive
   * @param fieldName
   * @param notNull
   * @param assertFalse
   * @param assertTrue
   * @param column
   * @param comment
   * @param value
   * @param permitReservedWords
   * @param transientModifier
   */
  void createBooleanField(ClassOrInterfaceTypeDetails javaTypeDetails, boolean primitive,
      JavaSymbolName fieldName, boolean notNull, boolean assertFalse, boolean assertTrue,
      String column, String comment, String value, boolean permitReservedWords,
      boolean transientModifier);

  /**
   * TODO
   *
   * @param javaTypeDetails
   * @param primitive
   * @param fieldName
   * @param notNull
   * @param assertFalse
   * @param assertTrue
   * @param column
   * @param comment
   * @param value
   * @param permitReservedWords
   * @param transientModifier
   * @param extraAnnotations
   */
  void createBooleanField(ClassOrInterfaceTypeDetails javaTypeDetails, boolean primitive,
      JavaSymbolName fieldName, boolean notNull, boolean assertFalse, boolean assertTrue,
      String column, String comment, String value, boolean permitReservedWords,
      boolean transientModifier, List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param javaTypeDetails
   * @param fieldType
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param future
   * @param past
   * @param persistenceType
   * @param column
   * @param comment
   * @param dateFormat
   * @param timeFormat
   * @param pattern
   * @param value
   * @param permitReservedWords
   * @param transientModifier
   */
  void createDateField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean future,
      boolean past, DateFieldPersistenceType persistenceType, String column, String comment,
      DateTime dateFormat, DateTime timeFormat, String pattern, String value,
      boolean permitReservedWords, boolean transientModifier);

  /**
   * TODO
   *
   * @param javaTypeDetails
   * @param fieldType
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param future
   * @param past
   * @param persistenceType
   * @param column
   * @param comment
   * @param dateFormat
   * @param timeFormat
   * @param pattern
   * @param value
   * @param permitReservedWords
   * @param transientModifier
   * @param extraAnnotations
   */
  void createDateField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean future,
      boolean past, DateFieldPersistenceType persistenceType, String column, String comment,
      DateTime dateFormat, DateTime timeFormat, String pattern, String value,
      boolean permitReservedWords, boolean transientModifier,
      List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param typeName
   * @param fieldType
   * @param fieldName
   * @param permitReservedWords
   */
  void createEmbeddedField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean permitReservedWords);

  /**
   * TODO
   *
   * @param typeName
   * @param fieldType
   * @param fieldName
   * @param permitReservedWords
   * @param extraAnnotations
   */
  void createEmbeddedField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean permitReservedWords, List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param cid
   * @param fieldType
   * @param fieldName
   * @param column
   * @param notNull
   * @param nullRequired
   * @param enumType
   * @param comment
   * @param permitReservedWords
   * @param transientModifier
   */
  void createEnumField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, String column, boolean notNull, boolean nullRequired,
      EnumType enumType, String comment, boolean permitReservedWords, boolean transientModifier);

  /**
   * TODO
   *
   * @param cid
   * @param fieldType
   * @param fieldName
   * @param column
   * @param notNull
   * @param nullRequired
   * @param enumType
   * @param comment
   * @param permitReservedWords
   * @param transientModifier
   * @param extraAnnotations
   */
  void createEnumField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, String column, boolean notNull, boolean nullRequired,
      EnumType enumType, String comment, boolean permitReservedWords, boolean transientModifier,
      List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param javaTypeDetails
   * @param fieldType
   * @param primitive
   * @param legalNumericPrimitives
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param decimalMin
   * @param decimalMax
   * @param digitsInteger
   * @param digitsFraction
   * @param min
   * @param max
   * @param column
   * @param comment
   * @param unique
   * @param value
   * @param permitReservedWords
   * @param transientModifier
   */
  void createNumericField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      boolean primitive, Set<String> legalNumericPrimitives, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax,
      Integer digitsInteger, Integer digitsFraction, Long min, Long max, String column,
      String comment, boolean unique, String value, boolean permitReservedWords,
      boolean transientModifier);

  /**
   * TODO
   *
   * @param javaTypeDetails
   * @param fieldType
   * @param primitive
   * @param legalNumericPrimitives
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param decimalMin
   * @param decimalMax
   * @param digitsInteger
   * @param digitsFraction
   * @param min
   * @param max
   * @param column
   * @param comment
   * @param unique
   * @param value
   * @param permitReservedWords
   * @param transientModifier
   * @param extraAnnotations
   */
  void createNumericField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      boolean primitive, Set<String> legalNumericPrimitives, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax,
      Integer digitsInteger, Integer digitsFraction, Long min, Long max, String column,
      String comment, boolean unique, String value, boolean permitReservedWords,
      boolean transientModifier, List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param typeName
   * @param fieldType
   * @param fieldName
   * @param aggregation
   * @param mappedBy
   * @param cascadeType
   * @param notNull
   * @param joinColumnName
   * @param referencedColumnName
   * @param fetch
   * @param comment
   * @param permitReservedWords
   * @param orphanRemoval
   * @param isForce
   * @param formatMessage 
   * @param formatExpression 
   */
  void createReferenceField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean aggregation, JavaSymbolName mappedBy, Cascade cascadeType[], boolean notNull,
      String joinColumnName, String referencedColumnName, Fetch fetch, String comment,
      boolean permitReservedWords, Boolean orphanRemoval, boolean isForce, String formatExpression,
      String formatMessage);

  /**
   * TODO
   *
   * @param typeName
   * @param fieldType
   * @param fieldName
   * @param cardinality
   * @param cascadeType
   * @param notNull
   * @param sizeMin
   * @param sizeMax
   * @param mappedBy
   * @param fetch
   * @param comment
   * @param joinColumnName
   * @param referencedColumnName
   * @param joinTable
   * @param joinColumns
   * @param referencedColumns
   * @param inverseJoinColumns
   * @param inverseReferencedColumns
   * @param permitReservedWords
   * @param aggregation
   * @param orphanRemoval
   * @param isForce
   * @param formatMessage 
   * @param formatExpression 
   */
  void createSetField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      Cardinality cardinality, Cascade cascadeType[], boolean notNull, Integer sizeMin,
      Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch, String comment, String joinColumnName,
      String referencedColumnName, String joinTable, String joinColumns, String referencedColumns,
      String inverseJoinColumns, String inverseReferencedColumns, boolean permitReservedWords,
      Boolean aggregation, Boolean orphanRemoval, boolean isForce, String formatExpression,
      String formatMessage);


  /**
   * TODO
   *
   * @param typeName
   * @param fieldType
   * @param fieldName
   * @param cardinality
   * @param cascadeType
   * @param notNull
   * @param sizeMin
   * @param sizeMax
   * @param mappedBy
   * @param fetch
   * @param comment
   * @param joinColumnName
   * @param referencedColumnName
   * @param joinTable
   * @param joinColumns
   * @param referencedColumns
   * @param inverseJoinColumns
   * @param inverseReferencedColumns
   * @param permitReservedWords
   * @param aggregation
   * @param orphanRemoval
   * @param isForce
   * @param formatMessage 
   * @param formatExpression 
   */
  void createListField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      Cardinality cardinality, Cascade cascadeType[], boolean notNull, Integer sizeMin,
      Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch, String comment, String joinColumnName,
      String referencedColumnName, String joinTable, String joinColumns, String referencedColumns,
      String inverseJoinColumns, String inverseReferencedColumns, boolean permitReservedWords,
      Boolean aggregation, Boolean orphanRemoval, boolean isForce, String formatExpression,
      String formatMessage);


  /**
   * TODO
   *
   * @param cid
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param decimalMin
   * @param decimalMax
   * @param sizeMin
   * @param sizeMax
   * @param regexp
   * @param column
   * @param comment
   * @param unique
   * @param value
   * @param lob
   * @param permitReservedWords
   * @param transientModifier
   */
  void createStringField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax, Integer sizeMin,
      Integer sizeMax, String regexp, String column, String comment, boolean unique, String value,
      boolean lob, boolean permitReservedWords, boolean transientModifier);

  /**
   * TODO
   *
   * @param cid
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param decimalMin
   * @param decimalMax
   * @param sizeMin
   * @param sizeMax
   * @param regexp
   * @param column
   * @param comment
   * @param unique
   * @param value
   * @param lob
   * @param permitReservedWords
   * @param transientModifier
   * @param extraAnnotations
   */
  void createStringField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax, Integer sizeMin,
      Integer sizeMax, String regexp, String column, String comment, boolean unique, String value,
      boolean lob, boolean permitReservedWords, boolean transientModifier,
      List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param cid
   * @param fieldName
   * @param contentType
   * @param autoUpload
   * @param notNull
   * @param column
   * @param permitReservedWords
   */
  void createFileField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      UploadedFileContentType contentType, boolean autoUpload, boolean notNull, String column,
      boolean permitReservedWords);

  /**
   * TODO
   *
   * @param cid
   * @param fieldName
   * @param contentType
   * @param autoUpload
   * @param notNull
   * @param column
   * @param permitReservedWords
   * @param extraAnnotations
   */
  void createFileField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      UploadedFileContentType contentType, boolean autoUpload, boolean notNull, String column,
      boolean permitReservedWords, List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param cid
   * @param fieldType
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param comment
   * @param column
   * @param permitReservedWords
   * @param transientModifier
   */
  void createOtherField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, String comment,
      String column, boolean permitReservedWords, boolean transientModifier);

  /**
   * TODO
   *
   * @param cid
   * @param fieldType
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param comment
   * @param column
   * @param permitReservedWords
   * @param transientModifier
   * @param extraAnnotations
   */
  void createOtherField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, String comment,
      String column, boolean permitReservedWords, boolean transientModifier,
      List<AnnotationMetadataBuilder> extraAnnotations);

  /**
   * TODO
   *
   * @param fieldDetails
   * @param permitReservedWords
   * @param transientModifier
   */
  void insertField(final FieldDetails fieldDetails, final boolean permitReservedWords,
      final boolean transientModifier);

  /**
   * TODO
   *
   * @param fieldDetails
   */
  void formatFieldComment(FieldDetails fieldDetails);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  List<String> getFieldSetTypeAllPossibleValues(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  List<String> getFieldListTypeAllPossibleValues(ShellContext shellContext);

  /**
   * TODO
   *
   * @param shellContext
   * @return
   */
  List<String> getFieldEmbeddedAllPossibleValues(ShellContext shellContext);

}

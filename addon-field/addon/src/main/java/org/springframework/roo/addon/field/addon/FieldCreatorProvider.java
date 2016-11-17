package org.springframework.roo.addon.field.addon;


import java.util.List;
import java.util.Set;

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
   * TODO
   * 
   * @param shellContext
   * @return
   */
  boolean isColumnMandatoryForFieldReference(ShellContext shellContext);

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
   * TODO
   * 
   * @param javaTypeDetails
   * @param primitive
   * @param fieldName
   * @param notNull
   * @param nullRequired
   * @param assertFalse
   * @param assertTrue
   * @param column
   * @param comment
   * @param value
   * @param permitReservedWords
   * @param transientModifier
   */
  void createBooleanField(ClassOrInterfaceTypeDetails javaTypeDetails, boolean primitive,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean assertFalse,
      boolean assertTrue, String column, String comment, String value, boolean permitReservedWords,
      boolean transientModifier);

  /**
   * TODO
   * 
   * @param javaTypeDetails
   * @param primitive
   * @param fieldName
   * @param notNull
   * @param nullRequired
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
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean assertFalse,
      boolean assertTrue, String column, String comment, String value, boolean permitReservedWords,
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
   */
  void createReferenceField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean aggregation, JavaSymbolName mappedBy, Cascade cascadeType[], boolean notNull,
      String joinColumnName, String referencedColumnName, Fetch fetch, String comment,
      boolean permitReservedWords, Boolean orphanRemoval, boolean isForce);

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
   * @param joinTable
   * @param joinColumns
   * @param referencedColumns
   * @param inverseJoinColumns
   * @param inverseReferencedColumns
   * @param permitReservedWords
   * @param aggregation
   * @param orphanRemoval
   * @param isForce
   */
  void createSetField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      Cardinality cardinality, Cascade cascadeType[], boolean notNull, Integer sizeMin,
      Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch, String comment, String joinTable,
      String joinColumns, String referencedColumns, String inverseJoinColumns,
      String inverseReferencedColumns, boolean permitReservedWords, Boolean aggregation,
      Boolean orphanRemoval, boolean isForce);


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
   * @param joinTable
   * @param joinColumns
   * @param referencedColumns
   * @param inverseJoinColumns
   * @param inverseReferencedColumns
   * @param permitReservedWords
   * @param aggregation
   * @param orphanRemoval
   * @param isForce
   */
  void createListField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      Cardinality cardinality, Cascade cascadeType[], boolean notNull, Integer sizeMin,
      Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch, String comment, String joinTable,
      String joinColumns, String referencedColumns, String inverseJoinColumns,
      String inverseReferencedColumns, boolean permitReservedWords, Boolean aggregation,
      Boolean orphanRemoval, boolean isForce);


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

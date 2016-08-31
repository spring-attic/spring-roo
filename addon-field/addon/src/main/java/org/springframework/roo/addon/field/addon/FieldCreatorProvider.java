package org.springframework.roo.addon.field.addon;


import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldDetails;
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

  boolean isColumnMandatoryForFieldBoolean(ShellContext shellContext);

  boolean isColumnVisibleForFieldBoolean(ShellContext shellContext);

  boolean isTransientVisibleForFieldBoolean(ShellContext shellContext);

  boolean isColumnMandatoryForFieldDate(ShellContext shellContext);

  boolean isColumnVisibleForFieldDate(ShellContext shellContext);

  boolean isPersistenceTypeVisibleForFieldDate(ShellContext shellContext);

  boolean isTransientVisibleForFieldDate(ShellContext shellContext);

  boolean isColumnMandatoryForFieldEnum(ShellContext shellContext);

  boolean isColumnVisibleForFieldEnum(ShellContext shellContext);

  boolean isEnumTypeVisibleForFieldEnum(ShellContext shellContext);

  boolean isTransientVisibleForFieldEnum(ShellContext shellContext);

  boolean isColumnMandatoryForFieldNumber(ShellContext shellContext);

  boolean isColumnVisibleForFieldNumber(ShellContext shellContext);

  boolean isUniqueVisibleForFieldNumber(ShellContext shellContext);

  boolean isTransientVisibleForFieldNumber(ShellContext shellContext);

  boolean isColumnMandatoryForFieldReference(ShellContext shellContext);

  boolean isJoinColumnNameVisibleForFieldReference(ShellContext shellContext);

  boolean isReferencedColumnNameVisibleForFieldReference(ShellContext shellContext);

  boolean isCardinalityVisibleForFieldReference(ShellContext shellContext);

  boolean isFetchVisibleForFieldReference(ShellContext shellContext);

  boolean isTransientVisibleForFieldReference(ShellContext shellContext);

  boolean isCascadeTypeVisibleForFieldReference(ShellContext shellContext);

  boolean areJoinTableParamsMandatoryForFieldSet(ShellContext shellContext);

  boolean isJoinTableMandatoryForFieldSet(ShellContext shellContext);

  boolean areJoinTableParamsVisibleForFieldSet(ShellContext shellContext);

  boolean isMappedByVisibleForFieldSet(ShellContext shellContext);

  boolean isCardinalityVisibleForFieldSet(ShellContext shellContext);

  boolean isFetchVisibleForFieldSet(ShellContext shellContext);

  boolean isTransientVisibleForFieldSet(ShellContext shellContext);

  boolean isJoinTableVisibleForFieldSet(ShellContext shellContext);

  boolean areJoinTableParamsVisibleForFieldList(ShellContext shellContext);

  boolean isJoinTableMandatoryForFieldList(ShellContext shellContext);

  boolean areJoinTableParamsMandatoryForFieldList(ShellContext shellContext);

  boolean isMappedByVisibleForFieldList(ShellContext shellContext);

  boolean isCardinalityVisibleForFieldList(ShellContext shellContext);

  boolean isFetchVisibleForFieldList(ShellContext shellContext);

  boolean isTransientVisibleForFieldList(ShellContext shellContext);

  boolean isJoinTableVisibleForFieldList(ShellContext shellContext);

  boolean isColumnMandatoryForFieldString(ShellContext shellContext);

  boolean isColumnVisibleForFieldString(ShellContext shellContext);

  boolean isUniqueVisibleForFieldString(ShellContext shellContext);

  boolean isTransientVisibleForFieldString(ShellContext shellContext);

  boolean isLobVisibleForFieldString(ShellContext shellContext);

  boolean isColumnMandatoryForFieldFile(ShellContext shellContext);

  boolean isColumnVisibleForFieldFile(ShellContext shellContext);

  boolean isColumnMandatoryForFieldOther(ShellContext shellContext);

  boolean isColumnVisibleForFieldOther(ShellContext shellContext);

  boolean isTransientVisibleForFieldOther(ShellContext shellContext);

  void createBooleanField(ClassOrInterfaceTypeDetails javaTypeDetails, boolean primitive,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean assertFalse,
      boolean assertTrue, String column, String comment, String value, boolean permitReservedWords,
      boolean transientModifier);

  void createDateField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, boolean future,
      boolean past, DateFieldPersistenceType persistenceType, String column, String comment,
      DateTime dateFormat, DateTime timeFormat, String pattern, String value,
      boolean permitReservedWords, boolean transientModifier);

  void createEmbeddedField(JavaType typeName, JavaType fieldType, JavaSymbolName fieldName,
      boolean permitReservedWords);

  void createEnumField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, String column, boolean notNull, boolean nullRequired,
      EnumType enumType, String comment, boolean permitReservedWords, boolean transientModifier);

  void createNumericField(ClassOrInterfaceTypeDetails javaTypeDetails, JavaType fieldType,
      boolean primitive, Set<String> legalNumericPrimitives, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax,
      Integer digitsInteger, Integer digitsFraction, Long min, Long max, String column,
      String comment, boolean unique, String value, boolean permitReservedWords,
      boolean transientModifier);

  void createReferenceField(ClassOrInterfaceTypeDetails cid, Cardinality cardinality,
      JavaType typeName, JavaType fieldType, JavaSymbolName fieldName, Cascade cascadeType,
      boolean notNull, boolean nullRequired, String joinColumnName, String referencedColumnName,
      Fetch fetch, String comment, boolean permitReservedWords, boolean transientModifier);

  void createSetField(ClassOrInterfaceTypeDetails cid, Cardinality cardinality, JavaType typeName,
      JavaType fieldType, JavaSymbolName fieldName, Cascade cascadeType, boolean notNull,
      boolean nullRequired, Integer sizeMin, Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch,
      String comment, String joinTable, String joinColumns, String referencedColumns,
      String inverseJoinColumns, String inverseReferencedColumns, boolean permitReservedWords,
      boolean transientModifier);

  void createListField(ClassOrInterfaceTypeDetails cid, Cardinality cardinality, JavaType typeName,
      JavaType fieldType, JavaSymbolName fieldName, Cascade cascadeType, boolean notNull,
      boolean nullRequired, Integer sizeMin, Integer sizeMax, JavaSymbolName mappedBy, Fetch fetch,
      String comment, String joinTable, String joinColumns, String referencedColumns,
      String inverseJoinColumns, String inverseReferencedColumns, boolean permitReservedWords,
      boolean transientModifier);

  void createStringField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      boolean notNull, boolean nullRequired, String decimalMin, String decimalMax, Integer sizeMin,
      Integer sizeMax, String regexp, String column, String comment, boolean unique, String value,
      boolean lob, boolean permitReservedWords, boolean transientModifier);

  void createFileField(ClassOrInterfaceTypeDetails cid, JavaSymbolName fieldName,
      UploadedFileContentType contentType, boolean autoUpload, boolean notNull, String column,
      boolean permitReservedWords);

  void createOtherField(ClassOrInterfaceTypeDetails cid, JavaType fieldType,
      JavaSymbolName fieldName, boolean notNull, boolean nullRequired, String comment,
      String column, boolean permitReservedWords, boolean transientModifier);

  void insertField(final FieldDetails fieldDetails, final boolean permitReservedWords,
      final boolean transientModifier);

  void formatFieldComment(FieldDetails fieldDetails);

  List<String> getFieldSetTypeAllPossibleValues(ShellContext shellContext);

  List<String> getFieldListTypeAllPossibleValues(ShellContext shellContext);

  List<String> getFieldEmbeddedAllPossibleValues(ShellContext shellContext);
}

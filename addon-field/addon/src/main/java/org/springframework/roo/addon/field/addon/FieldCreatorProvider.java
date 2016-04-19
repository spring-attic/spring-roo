package org.springframework.roo.addon.field.addon;


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

}

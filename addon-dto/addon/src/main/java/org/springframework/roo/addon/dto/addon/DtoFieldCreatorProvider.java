package org.springframework.roo.addon.dto.addon;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.addon.field.addon.FieldCreatorProvider;

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
  public boolean isCardinalityVisibleForFieldReference(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isFetchVisibleForFieldReference(ShellContext shellContext) {
    return false;
  }

  @Override
  public boolean isTransientVisibleForFieldReference(ShellContext shellContext) {
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
}

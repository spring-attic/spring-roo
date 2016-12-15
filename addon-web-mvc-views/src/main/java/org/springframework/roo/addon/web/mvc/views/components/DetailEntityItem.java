package org.springframework.roo.addon.web.mvc.views.components;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.support.util.XmlUtils;

import java.util.List;

/**
 * This class contains all necessary information about a detail entity to show it
 * in a page
 *
 * @author Manuel Iborra
 * @since 2.0
 */
public class DetailEntityItem extends EntityItem {

  private EntityItem rootEntity;
  private RelationInfo fieldInfo;
  private String tabLinkCode;
  private String fieldName;
  private String fieldNameCapitalized;
  private DetailEntityItem parentEntity;
  private int level;
  private List<RelationInfo> path;
  private String pathString;
  private String pathStringFieldNames;

  /**
   * Constructs a DetailEntityItem using the fieldName and suffixId
   *
   * @param fieldName
   *            the fieldName that represents the relationship
   * @param suffixId
   *            used to generate field id
   */
  public DetailEntityItem(JpaEntityMetadata childEntityMetadata,
      ControllerMetadata controllerMetadata, String detailSuffix, EntityItem rootEntity) {
    super(childEntityMetadata.getDestination().getSimpleTypeName(), detailSuffix,
        childEntityMetadata.isReadOnly());
    this.level = controllerMetadata.getDetailsFieldInfo().size();
    this.rootEntity = rootEntity;
    this.fieldInfo = controllerMetadata.getLastDetailsInfo();
    this.fieldName = fieldInfo.fieldName;
    this.fieldNameCapitalized = StringUtils.capitalize(fieldName);
    this.tabLinkCode = null;
    this.pathString = controllerMetadata.getDetailsPathAsString("-");
    this.pathStringFieldNames = controllerMetadata.getDetailsPathAsString(".");
    this.z = calculateZ();
    buildDetailItemId(detailSuffix);
  }

  /**
   * Builds the id of the specified detail
   *
   * @param suffix
   *            The suffix to complete the field id
   *
   */
  private void buildDetailItemId(String suffix) {
    String id = XmlUtils.convertId(this.pathString.toLowerCase());

    // If suffix is not blank or null, concatenate it
    if (!StringUtils.isEmpty(suffix)) {
      id = id.concat("-").concat(XmlUtils.convertId(suffix.toLowerCase()));
    }

    this.entityItemId = id;
  }

  /**
   * Calculate the hash code of the path, configuration and fieldName
   *
   * @return hash code
   */
  private String calculateZ() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((entityItemId == null) ? 0 : entityItemId.hashCode());
    result = prime * result + ((getConfiguration() == null) ? 0 : getConfiguration().hashCode());
    result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());

    return Integer.toHexString(result);
  }

  public String getTabLinkCode() {
    return tabLinkCode;
  }

  public void setTabLinkCode(String tabLinkCode) {
    this.tabLinkCode = tabLinkCode;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldNameCapitalized() {
    return fieldNameCapitalized;
  }

  public void setFieldNameCapitalized(String fieldNameCapitalized) {
    this.fieldNameCapitalized = fieldNameCapitalized;
  }

  public EntityItem getRootEntity() {
    return rootEntity;
  }

  public DetailEntityItem getParentEntity() {
    return parentEntity;
  }

  public void setParentEntity(DetailEntityItem parent) {
    this.parentEntity = parent;
  }

  public int getLevel() {
    return level;
  }

  public String getPathString() {
    return pathString;
  }

  public List<RelationInfo> getPath() {
    return path;
  }

  public String getPathStringFieldNames() {
    return pathStringFieldNames;
  }

  public boolean isTheParentEntity(DetailEntityItem parent) {
    String parentPath = parent.getPathString();
    return pathString.equals(parentPath.concat("-").concat(fieldName));
  }

  public RelationInfo getFieldInfo() {
    return fieldInfo;
  }

}

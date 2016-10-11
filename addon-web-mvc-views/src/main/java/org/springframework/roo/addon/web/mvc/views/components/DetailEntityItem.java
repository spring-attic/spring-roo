package org.springframework.roo.addon.web.mvc.views.components;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.support.util.XmlUtils;

/**
 * This class contains all necessary information about a detail entity to show it
 * in a page
 *
 * @author Manuel Iborra
 * @since 2.0
 */
public class DetailEntityItem extends EntityItem {

  private String tabLinkCode;
  private String fieldName;
  private String fieldNameCapitalized;

  /**
   * Constructs a DetailEntityItem using the fieldName and suffixId
   *
   * @param fieldName
   *            the fieldName that represents the relationship
   * @param suffixId
   *            used to generate field id
   */
  public DetailEntityItem(String fieldName, String suffixId) {
    super("", suffixId);
    this.fieldName = fieldName;
    this.fieldNameCapitalized = StringUtils.capitalize(fieldName);
    this.tabLinkCode = null;
    this.z = calculateZ();
    buildDetailItemId(suffixId);
  }

  /**
   * Builds the id of the specified detail
   *
   * @param suffix
   *            The suffix to complete the field id
   *
   */
  public void buildDetailItemId(String suffix) {
    String id = XmlUtils.convertId(this.fieldName.toLowerCase());

    // If suffix is not blank or null, concatenate it
    if (!StringUtils.isEmpty(suffix)) {
      id = id.concat("-").concat(XmlUtils.convertId(suffix.toLowerCase()));
    }

    this.entityItemId = id;
  }

  /**
   * Calculate the hash code of the entityItemId, configuration and fieldName
   *
   * @return hash code
   */
  private int calculateZ() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((entityItemId == null) ? 0 : entityItemId.hashCode());
    result = prime * result + ((getConfiguration() == null) ? 0 : getConfiguration().hashCode());
    result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());

    return result;
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

}

package org.springframework.roo.addon.web.mvc.views.components;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.support.util.XmlUtils;

/**
 * This class contains all necessary information about a field to be displayed
 * on generated views.
 *
 * Will receive a FieldMetadata item to be able to obtain all this necessary
 * information.
 *
 * @author Paula Navarro
 * @autor Juan Carlos García
 * @since 2.0
 */
public class FieldItem {

  private String fieldName;
  private String fieldNameCapitalized;
  private String fieldWithoutCamelCase;
  private String label;
  private String type;
  private Map<String, Object> configuration;
  private boolean userManaged;
  private String codeManaged;
  private String entityName;
  private String fieldId;
  private Map<String, String> javascriptCode;
  private String z;
  private Object legendLabel;

  /**
   * Constructs a FieldItem using the name and entityName
   *
   * @param fieldName
   *            the field name
   * @param entityName
   *            the entity where this field is defined
   * @param type
   *            the field java type
   */
  public FieldItem(String fieldName, String entityName) {
    this.fieldName = fieldName;
    this.fieldNameCapitalized = StringUtils.capitalize(fieldName);
    this.fieldWithoutCamelCase = buildFieldWithoutCamelCase(fieldName);
    this.label = buildLabel(entityName, fieldName);
    this.configuration = new HashMap<String, Object>();
    this.entityName = entityName;
    this.userManaged = false;
    this.codeManaged = "";
    buildId("");

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  /**
     * Constructs a FieldItem using the name, entityName and type of a field
     *
     * @param fieldName
     *            the field name
     * @param entityName
     *            the entity where this field is defined
     * @param type
     *            the field java type
     * @param configuration
     */
  public FieldItem(String fieldName, String entityName, String type,
      Map<String, Object> configuration) {
    this.fieldName = fieldName;
    this.fieldNameCapitalized = StringUtils.capitalize(fieldName);
    this.fieldWithoutCamelCase = buildFieldWithoutCamelCase(fieldName);
    this.label = buildLabel(entityName, fieldName);
    this.type = type;
    this.configuration = configuration;
    this.entityName = entityName;
    this.userManaged = false;
    this.codeManaged = "";
    buildId("");

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  /**
   * Constructs a FieldItem using the name, entityName and suffixId
   *
   * @param fieldName
   *            the field name
   * @param entityName
   *            the entity where this field is defined
   * @param suffixId
   *            used to generate field id
   */
  public FieldItem(String fieldName, String entityName, String suffixId) {
    this.fieldName = fieldName;
    this.fieldNameCapitalized = StringUtils.capitalize(fieldName);
    this.fieldWithoutCamelCase = buildFieldWithoutCamelCase(fieldName);
    this.label = buildLabel(entityName, fieldName);
    this.configuration = new HashMap<String, Object>();
    this.entityName = entityName;
    this.userManaged = false;
    this.codeManaged = "";
    buildId(suffixId);

    // Calculate the Z parameter as the hash code of the other parametersString
    this.z = calculateZ();
  }

  /**
   * Constructs a FieldItem  prepared to show in a parent entity view, using 
   * the name, parentEntityName, parentEntityFieldName, entityName and suffixId.
   *
   * @param fieldName
   *            the field name
   * @param parentEntityName
   *            the parent entity name
   * @param parentEntityFieldName
   *            the field name which references child entity on parent entity
   * @param entityName
   *            the entity where this field is defined
   * @param suffixId
   *            used to generate field id
   */
  public FieldItem(String fieldName, String parentEntityName, String parentEntityFieldName,
      String entityName, String suffixId) {
    this.fieldName = parentEntityFieldName.concat(".").concat(fieldName);
    this.fieldNameCapitalized = StringUtils.capitalize(fieldName);
    this.fieldWithoutCamelCase = buildFieldWithoutCamelCase(fieldName);
    this.configuration = new HashMap<String, Object>();
    this.label = buildLabel(entityName, fieldName);
    this.entityName = entityName;
    this.userManaged = false;
    this.codeManaged = "";
    this.legendLabel = buildLabel(parentEntityName, parentEntityFieldName);
    buildId(suffixId);

    // Calculate the Z parameter as the hash code of the other parametersString
    this.z = calculateZ();
  }

  /**
   * This method builds the fieldname without the camel case style.
   * 
   * @param fieldName
   * @return
   */
  private String buildFieldWithoutCamelCase(String fieldName) {
    String fieldWithoutCamelCase = "";
    String[] camelCase = StringUtils.splitByCharacterTypeCamelCase(fieldName);
    for (String part : camelCase) {
      fieldWithoutCamelCase = fieldWithoutCamelCase.concat(part).concat("-");
    }
    if (fieldWithoutCamelCase.endsWith("-")) {
      fieldWithoutCamelCase =
          fieldWithoutCamelCase.substring(0, fieldWithoutCamelCase.length() - 1);
    } else {
      fieldWithoutCamelCase = fieldName;
    }

    return fieldWithoutCamelCase;

  }

  /**
     * Builds the label of the specified field and adds it to the entity label
     *
     * @param entity
     *            the entity name
     * @param field
     *            the field name
     * @return label
     */
  public static String buildLabel(String entityName, String fieldName) {
    String entityLabel = XmlUtils.convertId("label." + entityName.toLowerCase());

    // If field is blank or null, only entity label will be generated
    if (fieldName != null && StringUtils.isBlank(fieldName)) {
      return entityLabel;
    }

    // Else, is necessary to concat fieldName to generate full field label
    return XmlUtils.convertId(entityLabel.concat(".").concat(fieldName.toLowerCase()));
  }

  /**
   * Builds the id of the specified field and adds it to the entity label
   *
   * @param suffix
   *            The suffix to complete the field id
   *
   * @return label
   */
  public void buildId(String suffix) {
    String id = XmlUtils.convertId(this.entityName.toLowerCase());

    // If field is not blank or null, concatenate it
    if (!StringUtils.isEmpty(this.fieldName)) {
      id = id.concat("-").concat(XmlUtils.convertId(fieldName.toLowerCase()));
    }

    // If suffix is not blank or null, concatenate it
    if (!StringUtils.isEmpty(suffix)) {
      id = id.concat("-").concat(XmlUtils.convertId(suffix.toLowerCase()));
    }

    this.fieldId = id;
  }

  /**
   * Calculate the hash code of the fieldName, label and type properties
   *
   * @return hash code
   */
  private String calculateZ() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
    result = prime * result + ((label == null) ? 0 : label.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return Integer.toHexString(result);
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getLabel() {
    return label;
  }

  public String getType() {
    return type;
  }

  public String getZ() {
    return z;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setZ(String z) {
    this.z = z;
  }

  public Map<String, Object> getConfiguration() {
    return this.configuration;
  }

  public void addConfigurationElement(String key, Object value) {
    this.configuration.put(key, value);
  }

  public String getFieldNameCapitalized() {
    return fieldNameCapitalized;
  }

  public void setFieldNameCapitalized(String fieldNameCapitalized) {
    this.fieldNameCapitalized = fieldNameCapitalized;
  }

  public String getFieldWithoutCamelCase() {
    return this.fieldWithoutCamelCase;
  }

  public void setFieldWithoutCamelCase(String fieldWithoutCamelCase) {
    this.fieldWithoutCamelCase = fieldWithoutCamelCase;
  }

  public boolean isUserManaged() {
    return userManaged;
  }

  public void setUserManaged(boolean userManaged) {
    this.userManaged = userManaged;
  }

  public String getCodeManaged() {
    return codeManaged;
  }

  public void setCodeManaged(String codeManaged) {
    this.codeManaged = codeManaged;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getFieldId() {
    return fieldId;
  }

  public Map<String, String> getJavascriptCode() {
    return javascriptCode;
  }

  public void setJavascriptCode(Map<String, String> javascriptCode) {
    this.javascriptCode = javascriptCode;
  }


  public Object getLegendLabel() {
    return legendLabel;
  }

  public void setLegendLabel(Object legendLabel) {
    this.legendLabel = legendLabel;
  }

}

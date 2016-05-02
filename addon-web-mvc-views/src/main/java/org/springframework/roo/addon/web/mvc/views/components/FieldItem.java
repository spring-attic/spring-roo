package org.springframework.roo.addon.web.mvc.views.components;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.support.util.XmlUtils;

/**
 * FieldMetadata DTO that contains the field data that views will use to display the property
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public class FieldItem {

  private String fieldName;

  private String label;

  private String type;

  private int z;

  /**
   * Constructs a FieldItem based on the specified field properties
   * 
   * @param field the field that will represent the FieldItem
   * @param entity the entity name used to get the field label
   */
  public FieldItem(FieldMetadata field, String entity) {
    this(field.getFieldName().getSymbolName(), buildLabel(entity, field), field.getFieldType()
        .getSimpleTypeName());
  }

  /**
   * Constructs a FieldItem sung the name, label and type of a field
   * 
   * @param name the field name
   * @param label the field label registered on messages.properties
   * @param type the field java type
   */
  public FieldItem(String name, String label, String type) {
    this.fieldName = name;
    this.label = label;
    this.type = type;

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  /**
   * Builds the label of the entity
   * 
   * @param entity the entity name
   * @return label
   */
  public static String buildLabel(String entity) {
    return XmlUtils.convertId("label." + entity.toLowerCase());
  }

  /**
   * Builds the label of the entity and its field
   * 
   * @param entity the entity name
   * @param field the entity field metadata
   * @return label
   */
  static String buildLabel(String entity, FieldMetadata field) {
    return buildFieldLabel(buildLabel(entity), field);
  }

  /**
   * Builds the label of the specified field and adds it to the entity label
   * 
   * @param entityLabel the entity label to concatenate the field name
   * @param field the field metadata
   * @return label
   */
  public static String buildFieldLabel(String entityLabel, FieldMetadata field) {
    return XmlUtils.convertId(entityLabel + "."
        + field.getFieldName().getSymbolName().toLowerCase());
  }

  /**
   * Calculated the hash code of the fiendName, label and type properties
   * 
   * @return hash code
   */
  private int calculateZ() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
    result = prime * result + ((label == null) ? 0 : label.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
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

  public int getZ() {
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

  public void setZ(int z) {
    this.z = z;
  }



}

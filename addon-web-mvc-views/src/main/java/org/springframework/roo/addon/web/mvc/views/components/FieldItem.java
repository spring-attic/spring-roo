package org.springframework.roo.addon.web.mvc.views.components;

import java.util.HashMap;
import java.util.Map;

import org.springframework.roo.classpath.details.FieldMetadata;
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
  private String label;
  private String type;
  private Map<String, String> configuration;
  private int z;

  /**
   * Constructs a FieldItem sung the name and entityName
   * 
   * @param name the field name
   * @param entityName the entity where this field is defined
   * @param type the field java type
   */
  public FieldItem(String name, String entityName) {
    this.fieldName = name;
    this.label = buildLabel(entityName);
    this.configuration = new HashMap<String, String>();

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  /**
   * Constructs a FieldItem sung the name, entityName and type of a field
   * 
   * @param name the field name
   * @param entityName the entity where this field is defined
   * @param type the field java type
   * @param configuration
   */
  public FieldItem(String name, String entityName, String type, Map<String, String> configuration) {
    this.fieldName = name;
    this.label = buildLabel(entityName);
    this.type = type;
    this.configuration = configuration;

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

  public Map<String, String> getConfiguration() {
    return this.configuration;
  }

  public void addConfigurationElement(String key, String value) {
    this.configuration.put(key, value);
  }

}

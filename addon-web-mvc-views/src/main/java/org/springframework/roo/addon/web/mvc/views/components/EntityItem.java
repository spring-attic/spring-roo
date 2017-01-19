package org.springframework.roo.addon.web.mvc.views.components;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.support.util.XmlUtils;

/**
 * This class contains all necessary information about a entity to show it
 * in a page
 *
 * @author Manuel Iborra
 * @author Sergio Clares
 * @since 2.0
 */
public class EntityItem {

  private boolean userManaged;
  private String entityName;
  protected String entityItemId;
  private String codeManaged;
  private Map<String, Object> configuration;
  protected String z;
  private boolean readOnly;
  private String modelAttribute;

  /**
   *
   * Constructs a EntityItem using the entityName, identifierField, controllerPath
   * suffixId, readOnly and versionField
   *
   * @param entityName
   *            the entity where this table is defined
   * @param identifierField
   *            the field used like table identifier
   * @param controllerPath
   *            path where is defined the controller that manage the table
   * @param suffixId
   *            used to generate field id
   * @param readOnly
   *            whether the entity is read only
   * @param versionField
   *            the field name used as version field
   */
  public EntityItem(String entityName, String identifierField, String controllerPath,
      String suffixId, boolean readOnly, String versionField) {
    this.entityName = entityName;
    this.modelAttribute = StringUtils.uncapitalize(entityName);
    this.userManaged = false;
    this.codeManaged = "";
    this.readOnly = readOnly;
    this.configuration = new HashMap<String, Object>();
    this.configuration.put("identifierField", identifierField);
    this.configuration.put("controllerPath", controllerPath);
    this.configuration.put("versionField", versionField);
    buildId(suffixId);

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  public EntityItem(String entityName, String suffixId, boolean readOnly) {
    this.entityName = entityName;
    this.modelAttribute = StringUtils.uncapitalize(entityName);
    this.userManaged = false;
    this.codeManaged = "";
    this.readOnly = readOnly;
    this.configuration = new HashMap<String, Object>();
    buildId(suffixId);

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  public EntityItem(String entityName, Map<String, Object> configuration, String suffixId) {
    this.entityName = entityName;
    this.modelAttribute = StringUtils.uncapitalize(entityName);
    this.configuration = configuration;
    this.userManaged = false;
    this.codeManaged = "";
    buildId(suffixId);

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
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

    // If suffix is not blank or null, concatenate it
    if (!StringUtils.isEmpty(suffix)) {
      id = id.concat("-").concat(XmlUtils.convertId(suffix.toLowerCase()));
    }

    this.entityItemId = id;
  }

  /**
   * Calculate the hash code of the entityName, entityItemId and configuration properties
   *
   * @return hash code
   */
  private String calculateZ() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((entityName == null) ? 0 : entityName.hashCode());
    result = prime * result + ((entityItemId == null) ? 0 : entityItemId.hashCode());
    result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
    return Integer.toHexString(result);
  }

  public void addConfigurationElement(String key, Object value) {
    this.configuration.put(key, value);
  }

  public boolean isUserManaged() {
    return userManaged;
  }

  public void setUserManaged(boolean userManaged) {
    this.userManaged = userManaged;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getCodeManaged() {
    return codeManaged;
  }

  public void setCodeManaged(String codeManaged) {
    this.codeManaged = codeManaged;
  }

  public String getZ() {
    return z;
  }


  public String getEntityItemId() {
    return entityItemId;
  }

  public Map<String, Object> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Map<String, Object> configuration) {
    this.configuration = configuration;
  }

  public boolean getReadOnly() {
    return this.readOnly;
  }

  public String getModelAttribute() {
    return modelAttribute;
  }

  public void setModelAttribute(String modelAttribute) {
    this.modelAttribute = modelAttribute;
  }
}

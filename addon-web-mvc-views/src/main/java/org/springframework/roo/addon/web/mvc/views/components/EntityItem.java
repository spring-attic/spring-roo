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
 * @since 2.0
 */
public class EntityItem {

  private boolean userManaged;
  private String entityName;
  protected String entityItemId;
  private Map<String, String> javascriptCode;
  private String codeManaged;
  private Map<String, Object> configuration;
  protected int z;

  /**
   *
   * Constructs a EntityItem using the entityName, identifierField, controllerPath
   * and suffixId
   *
   * @param entityName
   *            the entity where this table is defined
   * @param identifierField
   *            the field used like table identifier
   * @param controllerPath
   *            path where is defined the controller that manage the table
   * @param suffixId
   *            used to generate field id
   */
  public EntityItem(String entityName, String identifierField, String controllerPath,
      String suffixId) {
    this.entityName = entityName;
    this.userManaged = false;
    this.codeManaged = "";
    this.javascriptCode = new HashMap<String, String>();
    this.configuration = new HashMap<String, Object>();
    this.configuration.put("identifierField", identifierField);
    this.configuration.put("controllerPath", controllerPath);
    buildId(suffixId);

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  public EntityItem(String entityName, String suffixId) {
    this.entityName = entityName;
    this.userManaged = false;
    this.codeManaged = "";
    this.javascriptCode = new HashMap<String, String>();
    this.configuration = new HashMap<String, Object>();
    buildId(suffixId);

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  public EntityItem(String entityName, Map<String, Object> configuration, String suffixId) {
    this.entityName = entityName;
    this.configuration = configuration;
    this.userManaged = false;
    this.codeManaged = "";
    this.javascriptCode = new HashMap<String, String>();
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
  private int calculateZ() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((entityName == null) ? 0 : entityName.hashCode());
    result = prime * result + ((entityItemId == null) ? 0 : entityItemId.hashCode());
    result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
    return result;
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

  public Map<String, String> getJavascriptCode() {
    return javascriptCode;
  }

  public void setJavascriptCode(Map<String, String> javascriptCode) {
    this.javascriptCode = javascriptCode;
  }

  public String getCodeManaged() {
    return codeManaged;
  }

  public void setCodeManaged(String codeManaged) {
    this.codeManaged = codeManaged;
  }

  public int getZ() {
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



}

package org.springframework.roo.addon.web.mvc.views.components;

import java.util.List;

/**
 * This class contains all necessary information about a menu entry.
 *
 * @author Juan Carlos Garcia
 * @since 2.0
 */
public class MenuEntry {

  private String entityName;
  private String path;
  private String pathPrefix;
  private String entityLabel;
  private String entityPluralLabel;
  private List<String> finders;
  private int z;

  public MenuEntry(String entityName, String path, String pathPrefix, String entityLabel,
      String entityPluralLabel, List<String> finders) {
    this.entityName = entityName;
    this.path = path;
    this.pathPrefix = pathPrefix;
    this.entityLabel = entityLabel;
    this.entityPluralLabel = entityPluralLabel;
    this.finders = finders;

    // Calculate the Z parameter as the hash code of the other parameters
    this.z = calculateZ();
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setEntityLabel(String entityLabel) {
    this.entityLabel = entityLabel;
  }

  public void setEntityPluralLabel(String entityPluralLabel) {
    this.entityPluralLabel = entityPluralLabel;
  }

  public String getEntityLabel() {
    return entityLabel;
  }

  public String getEntityPluralLabel() {
    return entityPluralLabel;
  }

  public List<String> getFinders() {
    return finders;
  }

  public void setFinders(List<String> finders) {
    this.finders = finders;
  }

  public String getPathPrefix() {
    return pathPrefix;
  }

  public void setPathPrefix(String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  public int getZ() {
    return z;
  }

  public void setZ(int z) {
    this.z = z;
  }

  /**
   * Calculate the hash code of the entityName, path, pathPrefix, entityLabel and entityPluralLabel properties
   *
   * @return hash code
   */
  private int calculateZ() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((entityName == null) ? 0 : entityName.hashCode());
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    result = prime * result + ((pathPrefix == null) ? 0 : pathPrefix.hashCode());
    result = prime * result + ((entityLabel == null) ? 0 : entityLabel.hashCode());
    result = prime * result + ((entityPluralLabel == null) ? 0 : entityPluralLabel.hashCode());
    return result;
  }



}

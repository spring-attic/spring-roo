package org.springframework.roo.addon.web.mvc.views.components;

/**
 * This class contains all necessary information about a menu entry.
 * 
 * @author Juan Carlos Garcia
 * @since 2.0
 */
public class MenuEntry {

  private String entityName;
  private String path;
  private String entityLabel;
  private String entityPluralLabel;

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

}

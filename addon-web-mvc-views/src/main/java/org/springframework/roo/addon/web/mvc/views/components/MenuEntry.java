package org.springframework.roo.addon.web.mvc.views.components;

/**
 * This class contains all necessary information about a menu entry.
 * 
 * @author Juan Carlos Garcñia
 * @since 2.0
 */
public class MenuEntry {

  private String entityName;
  private String path;

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

}

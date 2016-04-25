package org.springframework.roo.addon.web.mvc.views;

/**
 * This class contains all necessary information about views.
 * 
 * It will be provided to view generator to be able to generate
 * views taking in mind some context parameters if needed.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ViewContext {

  private String controllerPath;

  public String getControllerPath() {
    return controllerPath;
  }

  public void setControllerPath(String controllerPath) {
    this.controllerPath = controllerPath;
  }


}

package org.springframework.roo.addon.web.mvc.views;

import java.util.HashMap;
import java.util.Map;

import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;

/**
 * This class contains all necessary information about views.
 *
 * It will be provided to view generator to be able to generate
 * views taking in mind some context parameters if needed.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class ViewContext<T extends AbstractViewMetadata> {

  // view Metadata

  private T viewMetadata;
  private ControllerMetadata controllerMetadata;

  // Project information
  private String projectName;
  private String description;
  private String version;

  // Controller information
  private String controllerPath;

  // Entity information
  private String identifierField;

  // View information
  private String modelAttribute;
  private String modelAttributeName;
  private String entityName;

  // Custom elements
  private Map<String, Object> extraInformation = new HashMap<String, Object>();

  // Security information
  private boolean securityEnabled;


  public String getControllerPath() {
    return controllerPath;
  }

  public void setControllerPath(String controllerPath) {
    this.controllerPath = controllerPath;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Map<String, Object> getExtraInformation() {
    return extraInformation;
  }

  public void setExtraInformation(Map<String, Object> extraInformation) {
    this.extraInformation = extraInformation;
  }

  public void addExtraParameter(String key, Object value) {
    this.extraInformation.put(key, value);
  }

  public void addExtraParameters(Map<String, Object> extraInformation) {
    this.extraInformation.putAll(extraInformation);
  }

  public String getModelAttribute() {
    return modelAttribute;
  }

  public void setModelAttribute(String modelAttribute) {
    this.modelAttribute = modelAttribute;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getIdentifierField() {
    return identifierField;
  }

  public void setIdentifierField(String identifierField) {
    this.identifierField = identifierField;
  }

  public String getModelAttributeName() {
    return modelAttributeName;
  }

  public void setModelAttributeName(String modelAttributeName) {
    this.modelAttributeName = modelAttributeName;
  }

  public boolean isSecurityEnabled() {
    return securityEnabled;
  }

  public void setSecurityEnabled(boolean securityEnabled) {
    this.securityEnabled = securityEnabled;
  }

  public T getViewMetadata() {
    return viewMetadata;
  }

  public void setViewMetadata(T viewMetadata) {
    this.viewMetadata = viewMetadata;
  }

  public ControllerMetadata getControllerMetadata() {
    return controllerMetadata;
  }

  public void setControllerMetadata(ControllerMetadata controllerMetadata) {
    this.controllerMetadata = controllerMetadata;
  }
}

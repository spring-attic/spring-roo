package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.model.JavaType;

/**
 * Information about detail controller
 *
 * @author Manuel Iborra
 * @since 2.0
 */
public class ControllerDetailInfo {

  private String path;

  private JavaType entity;

  private JavaType service;

  private JavaType parentEntity;

  private JavaType parentIdentifierType;

  private String parentReferenceFieldName;

  private JavaType parentService;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public JavaType getEntity() {
    return entity;
  }

  public void setEntity(JavaType entity) {
    this.entity = entity;
  }

  public JavaType getService() {
    return service;
  }

  public void setService(JavaType service) {
    this.service = service;
  }

  public JavaType getParentEntity() {
    return parentEntity;
  }

  public void setParentEntity(JavaType parentEntity) {
    this.parentEntity = parentEntity;
  }

  public JavaType getParentIdentifierType() {
    return parentIdentifierType;
  }

  public void setParentIdentifierType(JavaType parentIdentifierType) {
    this.parentIdentifierType = parentIdentifierType;
  }

  public String getParentReferenceFieldName() {
    return parentReferenceFieldName;
  }

  public void setParentReferenceFieldName(String parentReferenceFieldName) {
    this.parentReferenceFieldName = parentReferenceFieldName;
  }

  public JavaType getParentService() {
    return parentService;
  }

  public void setParentService(JavaType parentService) {
    this.parentService = parentService;
  }


}

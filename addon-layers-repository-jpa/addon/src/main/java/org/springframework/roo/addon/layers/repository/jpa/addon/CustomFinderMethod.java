/*
 * Copyright 2016 DiSiD Technologies S.L.L. All rights reserved.
 * 
 * Project  : DiSiD org.springframework.roo.addon.layers.repository.jpa.addon 
 * SVN Id   : $Id$
 */
package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Class that contains method structure for finders whose return type are projections.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class CustomFinderMethod {

  private JavaType returnType;
  private JavaSymbolName methodName;
  private JavaType formBean;

  public CustomFinderMethod(JavaType returnType, JavaSymbolName methodName) {
    this.returnType = returnType;
    this.methodName = methodName;
  }

  public CustomFinderMethod(JavaType returnType, JavaSymbolName methodName, JavaType formBean) {
    this.returnType = returnType;
    this.methodName = methodName;
    this.formBean = formBean;
  }

  public JavaType getReturnType() {
    return returnType;
  }

  public void setReturnType(JavaType returnType) {
    this.returnType = returnType;
  }

  public JavaSymbolName getMethodName() {
    return methodName;
  }

  public void setMethodName(JavaSymbolName methodName) {
    this.methodName = methodName;
  }

  public JavaType getFormBean() {
    return formBean;
  }

  public void setFormBean(JavaType formBean) {
    this.formBean = formBean;
  }

}

package org.springframework.roo.addon.finder.addon.parser;

import java.util.List;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Class that contains finder method structure.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class FinderMethod {

  private JavaType returnType;
  private JavaSymbolName methodName;
  private List<FinderParameter> parameters;

  public FinderMethod(JavaType returnType, JavaSymbolName methodName,
      List<FinderParameter> parameters) {
    this.returnType = returnType;
    this.methodName = methodName;
    this.parameters = parameters;
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

  public List<FinderParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<FinderParameter> parameters) {
    this.parameters = parameters;
  }


}

package org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    this.parameters = Collections.unmodifiableList(new ArrayList<FinderParameter>(parameters));
  }

  public FinderMethod(PartTree finder) {
    this.returnType = finder.getReturnType();
    this.methodName = new JavaSymbolName(finder.getOriginalQuery());
    this.parameters =
        Collections.unmodifiableList(new ArrayList<FinderParameter>(finder.getParameters()));
  }

  public JavaType getReturnType() {
    return returnType;
  }

  public JavaSymbolName getMethodName() {
    return methodName;
  }

  public List<FinderParameter> getParameters() {
    return parameters;
  }


}

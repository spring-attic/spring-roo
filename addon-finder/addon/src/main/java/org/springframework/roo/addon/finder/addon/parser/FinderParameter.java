package org.springframework.roo.addon.finder.addon.parser;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Class that contains finder parameter structure.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class FinderParameter {

  private JavaType type;
  private JavaSymbolName name;

  public FinderParameter(JavaType type, JavaSymbolName name) {
    this.type = type;
    this.name = name;
  }

  public JavaType getType() {
    return type;
  }

  public void setType(JavaType type) {
    this.type = type;
  }

  public JavaSymbolName getName() {
    return name;
  }

  public void setName(JavaSymbolName name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "FinderParameter [type=" + type + ", name=" + name + "]";
  }

}

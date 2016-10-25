package org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.Stack;

/**
 * Class that contains finder parameter structure.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class FinderParameter {

  private JavaType type;
  private JavaSymbolName name;
  private Stack<FieldMetadata> path;

  public FinderParameter(JavaType type, JavaSymbolName name, Stack<FieldMetadata> path) {
    this.type = type;
    this.name = name;
    this.path = path;
  }

  public FinderParameter(JavaType type, JavaSymbolName name) {
    this.type = type;
    this.name = name;
    this.path = null;
  }

  public JavaType getType() {
    return type;
  }

  public JavaSymbolName getName() {
    return name;
  }

  void setName(JavaSymbolName name) {
    this.name = name;
  }

  /**
   * Stack of the path to get property from original object
   * @return (if any)
   */
  public Stack<FieldMetadata> getPath() {
    return path;
  }

  @Override
  public String toString() {
    return "FinderParameter [type=" + type + ", name=" + name + "]";
  }

}

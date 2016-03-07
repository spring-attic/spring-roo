package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.FieldMetadata;

/**
 * This class is based on OrPart inner class located inside PartTree.java class from Spring Data commons project.
 * 
 * It has some little changes to be able to work properly on Spring Roo project
 * and make easy Spring Data query parser.
 * 
 * Get more information about original class on:
 * 
 * https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/repository/query/parser/PartTree.java
 * 
 * A part of the parsed source predicate that results from splitting up the search expressions around {@literal Or} keywords. 
 * Consists of {@link Part}s which are the conditions concatenated by {@literal And}.
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public class OrPart {

  private final List<Part> children = new ArrayList<Part>();

  private final PartTree currentPartTreeInstance;

  /**
   * Creates a new {@link OrPart}. 
   * OrPart is composed by several {@link Part}s or conditions, joined by {@literal And}.
   * 
   * @param partTree PartTree instance where current OrPart will be defined
   * @param source the source to split up into {@literal And} parts in turn.
   * @param fields entity properties.
   */
  OrPart(PartTree partTree, String source, List<FieldMetadata> fields) {

    Validate.notNull(partTree, "ERROR: PartTree instance is necessary to generate OrPart.");

    this.currentPartTreeInstance = partTree;

    String[] split = PartTree.split(source, "And", -1);
    for (int i = 0; i < split.length; i++) {

      // Validate previous Parts
      if (i > 0 && children.get(i - 1).getProperty() == null) {
        throw new RuntimeException(
            "ERROR: Missing property in a search condition before And operator");
      }
      children.add(new Part(currentPartTreeInstance, split[i], fields));
    }
  }

  @Override
  public String toString() {
    return StringUtils.join(children, "And");
  }

  public List<Part> getChildren() {
    return children;
  }

  /**
   * Returns true if all its {@link Part} expressions have a property.
   */
  public boolean isValid() {
    for (Part part : children) {
      if (!part.hasProperty()) {
        return false;
      }
    }
    return true;
  }
}

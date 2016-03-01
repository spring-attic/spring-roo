package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.classpath.details.FieldMetadata;

/**
 * A part of the parsed source predicate that results from splitting up the search expressions around {@literal Or} keywords. 
 * Consists of {@link Part}s which are the conditions concatenated by {@literal And}.
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public class OrPart {


  private final List<Part> children = new ArrayList<Part>();

  /**
   * Creates a new {@link OrPart}. 
   * OrPart is composed by several {@link Part}s or conditions, joined by {@literal And}.
   * 
   * @param source the source to split up into {@literal And} parts in turn.
   * @param fields entity properties.
   */
  OrPart(String source, List<FieldMetadata> fields) {

    String[] split = PartTree.split(source, "And", -1);
    for (int i = 0; i < split.length; i++) {

      // Validate previous Parts
      if (i > 0 && children.get(i - 1).getProperty() == null) {
        throw new RuntimeException(
            "ERROR: Missing property in a search condition before And operator");
      }
      children.add(new Part(split[i], fields));
    }
  }

  @Override
  public String toString() {
    return StringUtils.join(children, "And");
  }

  public List<Part> getChildren() {
    return children;
  }
}

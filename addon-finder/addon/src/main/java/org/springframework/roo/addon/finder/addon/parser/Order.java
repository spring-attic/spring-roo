package org.springframework.roo.addon.finder.addon.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.classpath.details.FieldMetadata;

/**
 * Represents an order expression, which is a pair of property and 
 * {@link Direction}. 
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public class Order {

  public static final Direction DEFAULT_DIRECTION = Direction.ASC;

  private final Direction direction;
  private final Pair<FieldMetadata, String> property;


  /**
   * Creates a new {@link Order} instance, which represents an order expression.
   * An order expression needs an entity property and a {@link Direction}.
   * 
   * @param direction the {@link Direction} can be Ascending or Descending
   * @param propertyInfo entity property
    */
  public Order(Direction direction, Pair<FieldMetadata, String> propertyInfo) {

    // Validate order expression. If a direction is specified, property must exist
    if (direction != null && propertyInfo == null) {
      throw new IllegalArgumentException(String.format(
          "ERROR: Missing property before %s direction", direction.getKeyword()));
    }
    this.direction = direction;
    this.property = propertyInfo;
  }

  /**
   * Returns the order that the property shall be sorted for.
   * 
   * @return
   */
  public Direction getDirection() {
    return direction;
  }

  /**
   * Returns the property information to order for.
   * 
   * @return Pair of property metadata and property name
   */
  public Pair<FieldMetadata, String> getProperty() {
    return property;
  }

  /**
   * Returns true if the expression has a property. Otherwise, returns false
   * @return
   */
  public boolean hasProperty() {
    return property != null;
  }

  /**
   * Returns whether sorting for the property shall be ascending.
   * 
   * @return
   */
  public boolean isAscending() {
    return this.direction.equals(Direction.ASC);
  }


  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return (property != null ? StringUtils.capitalize(property.getRight()) : "")
        .concat(this.direction != null ? direction.getKeyword() : "");
  }
}

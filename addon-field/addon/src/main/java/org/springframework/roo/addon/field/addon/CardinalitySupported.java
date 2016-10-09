package org.springframework.roo.addon.field.addon;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.operations.Cardinality;

/**
 * Provides current supported options for "set" and "list" relationships.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public enum CardinalitySupported {
  MANY_TO_MANY, ONE_TO_MANY;


  /**
   * @return {@link Cardinality} value related current item
   */
  public Cardinality getCardinality() {
    return Cardinality.valueOf(this.name());
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name());
    return builder.toString();
  }
}

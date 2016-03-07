package org.springframework.roo.addon.jpa.addon.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * This enum type represents javax.persistence.GenerationType
 * on Spring Roo Shell
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public enum IdentifierStrategy {

  SEQUENCE, TABLE, IDENTITY, AUTO;

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name());
    return builder.toString();
  }

}

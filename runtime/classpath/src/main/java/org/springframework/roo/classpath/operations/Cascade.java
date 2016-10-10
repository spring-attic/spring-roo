package org.springframework.roo.classpath.operations;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * This enum type represents javax.persistence.CascadeType
 * on Spring Roo Shell
 *
 * @author Sergio Clares
 * @since 2.0
 */
public enum Cascade {

  ALL, DETACH, MERGE, PERSIST, REFRESH, REMOVE;


  public static final Cascade[] ALL_ARRAY = {ALL};

  public static final Cascade[] MERGE_PERSIST = {MERGE, PERSIST};

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name());
    return builder.toString();
  }
}

package org.springframework.roo.addon.suite;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Display Addon Suite symbolic name for command completion.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class ObrRepositorySymbolicName implements Comparable<ObrRepositorySymbolicName> {

  /**
   * You can change this field name, but ensure getKey() returns a unique
   * value
   */
  private final String key;

  public ObrRepositorySymbolicName(final String key) {
    Validate.notBlank(key, "bundle symbolic name required");
    this.key = key;
  }

  public final int compareTo(final ObrRepositorySymbolicName o) {
    if (o == null) {
      return -1;
    }
    return key.compareTo(o.key);
  }

  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof ObrRepositorySymbolicName
        && compareTo((ObrRepositorySymbolicName) obj) == 0;
  }

  public String getKey() {
    return key;
  }

  @Override
  public final int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("key", key);
    return builder.toString();
  }
}

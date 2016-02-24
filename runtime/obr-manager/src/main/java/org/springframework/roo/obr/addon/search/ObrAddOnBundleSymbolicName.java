package org.springframework.roo.obr.addon.search;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Display Addon symbolic name for command completion.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class ObrAddOnBundleSymbolicName implements Comparable<ObrAddOnBundleSymbolicName> {

  /**
   * You can change this field name, but ensure getKey() returns a unique
   * value
   */
  private final String key;

  public ObrAddOnBundleSymbolicName(final String key) {
    Validate.notBlank(key, "bundle symbolic name required");
    this.key = key;
  }

  public final int compareTo(final ObrAddOnBundleSymbolicName o) {
    if (o == null) {
      return -1;
    }
    return key.compareTo(o.key);
  }

  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof ObrAddOnBundleSymbolicName
        && compareTo((ObrAddOnBundleSymbolicName) obj) == 0;
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

package org.springframework.roo.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * Default implementation of {@link CustomData}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class CustomDataImpl implements CustomData {

  public static final CustomData NONE = new CustomDataImpl(new LinkedHashMap<Object, Object>());

  private final Map<Object, Object> customData;

  public CustomDataImpl(final Map<Object, Object> customData) {
    Validate.notNull(customData, "Custom data required");
    this.customData = Collections.unmodifiableMap(customData);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final CustomDataImpl other = (CustomDataImpl) obj;
    if (customData == null) {
      if (other.customData != null) {
        return false;
      }
    } else if (!customData.equals(other.customData)) {
      return false;
    }
    return true;
  }

  public Object get(final Object key) {
    return customData.get(key);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    final int result = 1;
    return prime * result + (customData == null ? 0 : customData.hashCode());
  }

  public Iterator<Object> iterator() {
    return customData.keySet().iterator();
  }

  public Set<Object> keySet() {
    return customData.keySet();
  }

  @Override
  public String toString() {
    return customData.toString();
  }
}

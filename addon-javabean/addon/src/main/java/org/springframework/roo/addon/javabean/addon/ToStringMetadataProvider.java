package org.springframework.roo.addon.javabean.addon;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Provides {@link ToStringMetadata}.
 *
 * @author Jose Manuel Vivó Arnal
 * @since 2.0.0.RC3
 */
public interface ToStringMetadataProvider extends ItdTriggerBasedMetadataProvider {

  /**
   * Return the list of fields to take account to generate "toString" method
   *
   * @param governorPhysicalTypeMetadata
   * @param declaredFieldsList
   * @return
   */
  public List<FieldMetadata> getToStringFields(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final List<FieldMetadata> declaredFieldsList);
}

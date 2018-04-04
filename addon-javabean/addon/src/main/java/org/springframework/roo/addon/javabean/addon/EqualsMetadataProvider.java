package org.springframework.roo.addon.javabean.addon;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.model.JavaType;

/**
 * Provides {@link EqualsMetadata}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public interface EqualsMetadataProvider extends ItdTriggerBasedMetadataProvider {

  public List<FieldMetadata> locateFields(final JavaType javaType, final String[] excludeFields,
      final List<FieldMetadata> fields, final String metadataIdentificationString);

  public FieldMetadata getIdentifier(final PhysicalTypeMetadata governorPhysicalTypeMetadata);
}

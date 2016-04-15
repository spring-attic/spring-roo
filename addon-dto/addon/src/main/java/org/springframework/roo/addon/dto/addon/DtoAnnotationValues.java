package org.springframework.roo.addon.dto.addon;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.*;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooDTO
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class DtoAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private boolean immutable;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public DtoAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_DTO);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the value of immutable property.
   * 
   * @return boolean immutable
   */
  public boolean getImmutable() {
    return immutable;
  }

}

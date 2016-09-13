package org.springframework.roo.addon.dto.addon;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.*;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooEntityProjection
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class EntityProjectionAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private Class<?> entity;

  @AutoPopulate
  private String[] fields;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public EntityProjectionAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_ENTITY_PROJECTION);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public Class<?> getEntity() {
    return entity;
  }

  public String[] getFields() {
    return fields;
  }

}

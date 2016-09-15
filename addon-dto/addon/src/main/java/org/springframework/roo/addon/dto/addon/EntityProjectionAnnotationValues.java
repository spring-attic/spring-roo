package org.springframework.roo.addon.dto.addon;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooEntityProjection
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class EntityProjectionAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;

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

  public JavaType getEntity() {
    return entity;
  }

  public String[] getFields() {
    return fields;
  }

}

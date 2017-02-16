package org.springframework.roo.addon.jpa.addon.entity.factories;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooJpaEntityFactory
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class JpaEntityFactoryAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public JpaEntityFactoryAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_JPA_ENTITY_FACTORY);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getEntity() {
    return entity;
  }

}

package org.springframework.roo.addon.layers.service.addon;

import org.springframework.roo.addon.layers.service.annotations.RooService;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a given {@link RooService} annotation.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @author Juan Carlos García
 * @since 1.2.0
 */
public class ServiceAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata to parse (required)
   */
  public ServiceAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_SERVICE);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getEntity() {
    return entity;
  }

}

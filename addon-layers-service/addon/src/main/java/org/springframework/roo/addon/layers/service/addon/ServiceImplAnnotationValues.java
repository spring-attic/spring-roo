package org.springframework.roo.addon.layers.service.addon;

import org.springframework.roo.addon.layers.service.annotations.RooServiceImpl;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a given {@link RooServiceImpl} annotation.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ServiceImplAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType service;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata to parse (required)
   */
  public ServiceImplAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_SERVICE_IMPL);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getService() {
    return service;
  }

}

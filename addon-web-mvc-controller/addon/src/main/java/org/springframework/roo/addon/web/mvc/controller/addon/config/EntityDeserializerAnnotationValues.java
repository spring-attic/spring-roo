package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooDeserializer;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * = _EntityDeserializerAnnotationValues_
 *
 * Maps values of {@link RooDeserializer} annotation
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 *
 */
public class EntityDeserializerAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;


  public EntityDeserializerAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_DESERIALIZER);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public EntityDeserializerAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_DESERIALIZER);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }


  public JavaType getEntity() {
    return entity;
  }

}

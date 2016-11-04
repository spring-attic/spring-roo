package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooJsonMixin;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * = _JsonMixinAnnotationValues_
 *
 * Maps values of {@link RooJsonMixin} annotation
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 *
 */
public class JSONMixinAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;


  public JSONMixinAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_JSON_MIXIN);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JSONMixinAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_JSON_MIXIN);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }


  public JavaType getEntity() {
    return entity;
  }

}

package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * = _ControllerAnnotationValues_
 *
 * Maps values of {@link RooController} annotation
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 *
 */
public class ControllerAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;

  @AutoPopulate
  private String pathPrefix;

  @AutoPopulate
  private ControllerType type;


  public ControllerAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_CONTROLLER);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public ControllerAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_CONTROLLER);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }


  public JavaType getEntity() {
    return entity;
  }


  public String getPathPrefix() {
    return pathPrefix;
  }


  public ControllerType getType() {
    return type;
  }



}

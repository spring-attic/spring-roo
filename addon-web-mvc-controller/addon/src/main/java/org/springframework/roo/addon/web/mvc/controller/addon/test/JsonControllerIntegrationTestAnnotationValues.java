package org.springframework.roo.addon.web.mvc.controller.addon.test;

import org.springframework.roo.addon.web.mvc.controller.annotations.test.RooJsonControllerIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooJsonControllerIntegrationTest} annotation.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class JsonControllerIntegrationTestAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType targetClass = null;

  public JsonControllerIntegrationTestAnnotationValues(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_JSON_CONTROLLER_INTEGRATION_TEST);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JsonControllerIntegrationTestAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_JSON_CONTROLLER_INTEGRATION_TEST);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getTargetClass() {
    return targetClass;
  }

}

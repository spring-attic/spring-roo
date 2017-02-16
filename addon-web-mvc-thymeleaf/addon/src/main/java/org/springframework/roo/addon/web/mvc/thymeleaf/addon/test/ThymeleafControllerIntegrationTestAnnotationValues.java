package org.springframework.roo.addon.web.mvc.thymeleaf.addon.test;

import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.test.RooThymeleafControllerIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooThymeleafControllerIntegrationTest} annotation.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class ThymeleafControllerIntegrationTestAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType targetClass = null;

  public ThymeleafControllerIntegrationTestAnnotationValues(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_THYMELEAF_CONTROLLER_INTEGRATION_TEST);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public ThymeleafControllerIntegrationTestAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_THYMELEAF_CONTROLLER_INTEGRATION_TEST);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getTargetClass() {
    return targetClass;
  }

}

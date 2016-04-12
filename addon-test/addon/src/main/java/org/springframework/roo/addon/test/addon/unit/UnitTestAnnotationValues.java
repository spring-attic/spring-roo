package org.springframework.roo.addon.test.addon.unit;

import org.springframework.roo.addon.test.annotations.RooIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooUnitTest} annotation.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class UnitTestAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType targetClass = null;

  public UnitTestAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_UNIT_TEST);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getTargetClass() {
    return targetClass;
  }
}

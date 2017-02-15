package org.springframework.roo.addon.layers.repository.jpa.addon.test;

import org.springframework.roo.addon.layers.repository.jpa.annotations.test.RooRepositoryJpaIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooRepositoryJpaIntegrationTest} annotation.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class RepositoryJpaIntegrationTestAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType targetClass = null;
  @AutoPopulate
  private JavaType dodConfigurationClass = null;
  @AutoPopulate
  private JavaType dodClass = null;

  public RepositoryJpaIntegrationTestAnnotationValues(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_REPOSITORY_JPA_INTEGRATION_TEST);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public RepositoryJpaIntegrationTestAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_REPOSITORY_JPA_INTEGRATION_TEST);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getTargetClass() {
    return targetClass;
  }

  public JavaType getDodConfigurationClass() {
    return dodConfigurationClass;
  }

  public JavaType getDodClass() {
    return dodClass;
  }
}

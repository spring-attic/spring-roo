package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooJpaRepositoryCustomImpl} annotation.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class RepositoryJpaCustomImplAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType repository;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public RepositoryJpaCustomImplAnnotationValues(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_REPOSITORY_JPA_CUSTOM_IMPL);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the repository type implemented by the annotated class
   * 
   * @return a non-<code>null</code> type
   */
  public JavaType getRepository() {
    return repository;
  }
}

package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustom;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooJpaRepositoryCustom} annotation.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class RepositoryJpaCustomAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;

  @AutoPopulate
  private JavaType defaultReturnType;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public RepositoryJpaCustomAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_REPOSITORY_JPA_CUSTOM);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the search result type returned by the findAll method of the annotated repository
   * 
   * @return a non-<code>null</code> type
   */
  public JavaType getDefaultReturnType() {
    return defaultReturnType;
  }

  /**
   * Returns the entity type managed by the annotated repository
   * 
   * @return a non-<code>null</code> type
   */
  public JavaType getEntity() {
    return entity;
  }
}

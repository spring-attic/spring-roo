package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepository;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooJpaRepository} annotation.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @author Juan Carlos García
 * @since 1.2.0
 */
public class RepositoryJpaAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType entity;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public RepositoryJpaAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_REPOSITORY_JPA);
    AutoPopulationUtils.populate(this, annotationMetadata);
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

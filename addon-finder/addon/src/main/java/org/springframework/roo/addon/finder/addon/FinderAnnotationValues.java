package org.springframework.roo.addon.finder.addon;

import org.springframework.roo.addon.finder.annotations.RooFinders;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooFinders} annotation.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class FinderAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String[] finders;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public FinderAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_FINDER);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the finders managed by the annotated repository
   * 
   * @return a non-<code>null</code> type
   */
  public String[] getFinders() {
    return finders;
  }
}

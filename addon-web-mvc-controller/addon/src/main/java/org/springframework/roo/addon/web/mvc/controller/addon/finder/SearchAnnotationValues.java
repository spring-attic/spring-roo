package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

import java.util.List;

/**
 *
 * Annotation values for @RooSearch
 *
 * @author Sergio Clares
 * @since 2.0
 */
public class SearchAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private List<String> finders;

  public SearchAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_SEARCH);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public List<String> getFinders() {
    return finders;
  }
}

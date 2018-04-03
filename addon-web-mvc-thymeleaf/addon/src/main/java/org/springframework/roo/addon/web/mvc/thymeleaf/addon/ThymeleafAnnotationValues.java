package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF;

import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;

/**
 * Annotation values for {@link RooThymeleaf}
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0RC3
 */
public class ThymeleafAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String[] excludeMethods;

  @AutoPopulate
  private String[] excludeViews;

  /**
   * Constructor
   *
   * @param governorPhysicalTypeMetadata
   */
  public ThymeleafAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, ROO_THYMELEAF);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }


  public String[] getExcludeMethods() {
    return excludeMethods;
  }

  public String[] getExcludeViews() {
    return excludeViews;
  }

}

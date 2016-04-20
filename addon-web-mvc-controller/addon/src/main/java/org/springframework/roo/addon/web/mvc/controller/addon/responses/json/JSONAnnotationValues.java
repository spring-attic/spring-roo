package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooJSON} annotation.
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public class JSONAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String[] finders;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public JSONAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_JSON);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the finders managed by the annotated controller
   * 
   * @return a non-<code>null</code> type
   */
  public String[] getFinders() {
    return finders;
  }

}

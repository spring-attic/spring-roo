package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import static org.springframework.roo.model.RooJavaType.ROO_JSON;

import org.springframework.roo.addon.web.mvc.controller.annotations.responses.json.RooJSON;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;

/**
 * Annotation values for {@link RooJSON}
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0RC3
 */
public class JSONAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String[] excludeMethods;

  /**
   * Constructor
   *
   * @param governorPhysicalTypeMetadata
   */
  public JSONAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, ROO_JSON);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }


  public String[] getExcludeMethods() {
    return excludeMethods;
  }

}

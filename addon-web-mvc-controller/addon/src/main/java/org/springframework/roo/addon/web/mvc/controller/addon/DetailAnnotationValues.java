package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

public class DetailAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String relationField;

  public DetailAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_DETAIL);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public String getRelationField() {
    return relationField;
  }

}

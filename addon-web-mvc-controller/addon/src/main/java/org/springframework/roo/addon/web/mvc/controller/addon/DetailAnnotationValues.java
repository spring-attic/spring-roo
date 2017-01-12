package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
*
* Annotation values for @RooDetail
*
* @author Juan Carlos García
* @since 2.0
*/
public class DetailAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String relationField;

  @AutoPopulate
  private String[] views;

  public DetailAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_DETAIL);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public DetailAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_DETAIL);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public String getRelationField() {
    return relationField;
  }

  public String[] getViews() {
    return views;
  }

}

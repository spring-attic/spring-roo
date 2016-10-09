package org.springframework.roo.addon.field.addon;

import org.springframework.roo.addon.field.annotations.RooRelationManagement;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.*;
import org.springframework.roo.model.RooJavaType;

/**
 * = _ReleationManagementAnnotationValues_
 *
 *  Annotation values for {@link RooRelationManagement}
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 *
 */
public class ReleationManagementAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String[] relationFields;

  /**
   * Constructor
   *
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public ReleationManagementAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_RELATION_MANAGEMENT);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the value of relationFields property.
   *
   * Every value is the name of entity field to be handle
   *
   * @return String[] relation fields
   */
  public String[] getRelationFields() {
    return relationFields;
  }

}

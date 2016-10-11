package org.springframework.roo.addon.security.addon.security;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooWebSecurityConfiguration
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ModelGlobalSecurityConfigAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String profile;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public ModelGlobalSecurityConfigAnnotationValues(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_MODEL_GLOBAL_SECURITY_CONFIG);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the value of profile property
   * 
   * @return String with the profile property
   */
  public String getProfile() {
    return profile;
  }

}

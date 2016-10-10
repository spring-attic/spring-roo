package org.springframework.roo.addon.security.addon.security;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.*;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooWebSecurityConfiguration
 * 
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 2.0
 */
public class WebSecurityConfigurationAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String profile;
  @AutoPopulate
  private boolean enableGlobalMethodSecurity;
  @AutoPopulate
  private boolean enableJpaAuditing;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public WebSecurityConfigurationAnnotationValues(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_WEB_SECURITY_CONFIGURATION);
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

  /**
   * Returns the value of enableGlobalMethodSecurity property.
   * 
   * @return a non-<code>null</code> boolean
   */
  public boolean getEnableGlobalMethodSecurity() {
    return enableGlobalMethodSecurity;
  }

  /**
   * Returns the value of enableJpaAuditing property.
   * 
   * @return a non-<code>null</code> boolean
   */
  public boolean getEnableJpaAuditing() {
    return enableJpaAuditing;
  }
}

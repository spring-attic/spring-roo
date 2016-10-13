package org.springframework.roo.addon.jpa.addon.audit;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooJpaAudit
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class JpaAuditAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String createdDateColumn = "";
  @AutoPopulate
  private String modifiedDateColumn = "";
  @AutoPopulate
  private String createdByColumn = "";
  @AutoPopulate
  private String modifiedByColumn = "";

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public JpaAuditAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_JPA_AUDIT);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the value of createdDateColumn property.
   * 
   * @return a String. It could be empty.
   */
  public String getCreatedDateColumn() {
    return createdDateColumn;
  }

  /**
   * Returns the value of modifiedDateColumn property.
   * 
   * @return a String. It could be empty.
   */
  public String getModifiedDateColumn() {
    return modifiedDateColumn;
  }

  /**
   * Returns the value of createdByColumn property.
   * 
   * @return a String. It could be empty.
   */
  public String getCreatedByColumn() {
    return createdByColumn;
  }

  /**
   * Returns the value of modifiedByColumn property.
   * 
   * @return a String. It could be empty.
   */
  public String getModifiedByColumn() {
    return modifiedByColumn;
  }
}

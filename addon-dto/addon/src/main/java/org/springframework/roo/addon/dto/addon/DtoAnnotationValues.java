package org.springframework.roo.addon.dto.addon;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.*;
import org.springframework.roo.model.RooJavaType;

/**
 * Annotation values for @RooDTO
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class DtoAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private boolean immutable;
  @AutoPopulate
  private String formatMessage = "";
  @AutoPopulate
  private String formatExpression = "";

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public DtoAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_DTO);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the value of immutable property.
   * 
   * @return boolean immutable
   */
  public boolean getImmutable() {
    return immutable;
  }

  /**
   * Returns the value of _formatExpression_ property
   * 
   * @return String formatExpression
   */
  public String getFormatExpression() {
    return formatExpression;
  }

  /**
   * Returns the value of _formatMessage_ property
   * 
   * @return String formatMessage
   */
  public String getFormatMessage() {
    return formatMessage;
  }

}

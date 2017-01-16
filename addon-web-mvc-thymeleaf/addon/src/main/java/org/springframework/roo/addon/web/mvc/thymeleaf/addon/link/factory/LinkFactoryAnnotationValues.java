package org.springframework.roo.addon.web.mvc.thymeleaf.addon.link.factory;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.*;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * = LinkFactoryAnnotationValues
 * 
 * Annotation values for @RooLinkFactory
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class LinkFactoryAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private JavaType controller;

  /**
   * Constructor
   * 
   * @param governorPhysicalTypeMetadata to parse (required)
   */
  public LinkFactoryAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_LINK_FACTORY);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public LinkFactoryAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_LINK_FACTORY);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public JavaType getController() {
    return controller;
  }

}

package org.springframework.roo.addon.jpa.addon.identifier;

import org.springframework.roo.addon.jpa.addon.identifier.IdentifierAnnotationValues;
import org.springframework.roo.addon.jpa.annotations.identifier.RooIdentifier;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link IdentifierAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class IdentifierAnnotationValuesTest extends
    AnnotationValuesTestCase<RooIdentifier, IdentifierAnnotationValues> {

  @Override
  protected Class<RooIdentifier> getAnnotationClass() {
    return RooIdentifier.class;
  }

  @Override
  protected Class<IdentifierAnnotationValues> getValuesClass() {
    return IdentifierAnnotationValues.class;
  }
}

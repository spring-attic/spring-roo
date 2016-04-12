package org.springframework.roo.addon.test.addon;

import org.springframework.roo.addon.test.addon.integration.IntegrationTestAnnotationValues;
import org.springframework.roo.addon.test.annotations.RooIntegrationTest;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link IntegrationTestAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class IntegrationTestAnnotationValuesTest extends
    AnnotationValuesTestCase<RooIntegrationTest, IntegrationTestAnnotationValues> {

  @Override
  protected Class<RooIntegrationTest> getAnnotationClass() {
    return RooIntegrationTest.class;
  }

  @Override
  protected Class<IntegrationTestAnnotationValues> getValuesClass() {
    return IntegrationTestAnnotationValues.class;
  }
}

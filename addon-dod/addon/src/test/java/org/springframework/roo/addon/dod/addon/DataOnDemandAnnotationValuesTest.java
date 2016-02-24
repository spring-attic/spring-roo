package org.springframework.roo.addon.dod.addon;

import org.springframework.roo.addon.dod.annotations.RooDataOnDemand;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link DataOnDemandAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DataOnDemandAnnotationValuesTest extends
    AnnotationValuesTestCase<RooDataOnDemand, DataOnDemandAnnotationValues> {

  @Override
  protected Class<RooDataOnDemand> getAnnotationClass() {
    return RooDataOnDemand.class;
  }

  @Override
  protected Class<DataOnDemandAnnotationValues> getValuesClass() {
    return DataOnDemandAnnotationValues.class;
  }
}

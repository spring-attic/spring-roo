package org.springframework.roo.addon.jpa.addon.dod;

import org.springframework.roo.addon.jpa.addon.dod.JpaDataOnDemandAnnotationValues;
import org.springframework.roo.addon.jpa.annotations.dod.RooJpaDataOnDemand;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link JpaDataOnDemandAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JpaDataOnDemandAnnotationValuesTest extends
    AnnotationValuesTestCase<RooJpaDataOnDemand, JpaDataOnDemandAnnotationValues> {

  @Override
  protected Class<RooJpaDataOnDemand> getAnnotationClass() {
    return RooJpaDataOnDemand.class;
  }

  @Override
  protected Class<JpaDataOnDemandAnnotationValues> getValuesClass() {
    return JpaDataOnDemandAnnotationValues.class;
  }
}

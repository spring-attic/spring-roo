package org.springframework.roo.addon.jpa.addon.entity;

import org.springframework.roo.addon.jpa.addon.entity.JpaEntityAnnotationValues;
import org.springframework.roo.addon.jpa.annotations.entity.RooJpaEntity;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link JpaEntityAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JpaEntityAnnotationValuesTest extends
    AnnotationValuesTestCase<RooJpaEntity, JpaEntityAnnotationValues> {

  @Override
  protected Class<RooJpaEntity> getAnnotationClass() {
    return RooJpaEntity.class;
  }

  @Override
  protected Class<JpaEntityAnnotationValues> getValuesClass() {
    return JpaEntityAnnotationValues.class;
  }
}

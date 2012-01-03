package org.springframework.roo.addon.jpa.entity;

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
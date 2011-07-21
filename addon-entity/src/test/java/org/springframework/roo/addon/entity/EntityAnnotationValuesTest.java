package org.springframework.roo.addon.entity;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link EntityAnnotationValues}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class EntityAnnotationValuesTest extends AnnotationValuesTestCase<RooEntity, EntityAnnotationValues> {

	@Override
	protected Class<RooEntity> getAnnotationClass() {
		return RooEntity.class;
	}

	@Override
	protected Class<EntityAnnotationValues> getValuesClass() {
		return EntityAnnotationValues.class;
	}
}

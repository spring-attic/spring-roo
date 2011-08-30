package org.springframework.roo.addon.layers.repository.jpa;

import org.springframework.roo.addon.layers.repository.jpa.RepositoryJpaAnnotationValues;
import org.springframework.roo.addon.layers.repository.jpa.RooRepositoryJpa;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link RepositoryJpaAnnotationValues}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryJpaAnnotationValuesTest extends AnnotationValuesTestCase<RooRepositoryJpa, RepositoryJpaAnnotationValues> {

	@Override
	protected Class<RooRepositoryJpa> getAnnotationClass() {
		return RooRepositoryJpa.class;
	}

	@Override
	protected Class<RepositoryJpaAnnotationValues> getValuesClass() {
		return RepositoryJpaAnnotationValues.class;
	}
}
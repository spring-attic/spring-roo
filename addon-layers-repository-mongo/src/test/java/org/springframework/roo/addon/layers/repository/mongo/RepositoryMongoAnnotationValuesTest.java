package org.springframework.roo.addon.layers.repository.mongo;

import org.springframework.roo.addon.layers.repository.mongo.RepositoryMongoAnnotationValues;
import org.springframework.roo.addon.layers.repository.mongo.RooRepositoryMongo;
import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link RepositoryMongoAnnotationValues}
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class RepositoryMongoAnnotationValuesTest extends AnnotationValuesTestCase<RooRepositoryMongo, RepositoryMongoAnnotationValues> {

	@Override
	protected Class<RooRepositoryMongo> getAnnotationClass() {
		return RooRepositoryMongo.class;
	}

	@Override
	protected Class<RepositoryMongoAnnotationValues> getValuesClass() {
		return RepositoryMongoAnnotationValues.class;
	}
}
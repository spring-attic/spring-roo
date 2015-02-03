package org.springframework.roo.addon.layers.repository.mongo;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link RepositoryMongoAnnotationValues}
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class RepositoryMongoAnnotationValuesTest
        extends
        AnnotationValuesTestCase<RooMongoRepository, RepositoryMongoAnnotationValues> {

    @Override
    protected Class<RooMongoRepository> getAnnotationClass() {
        return RooMongoRepository.class;
    }

    @Override
    protected Class<RepositoryMongoAnnotationValues> getValuesClass() {
        return RepositoryMongoAnnotationValues.class;
    }
}
package org.springframework.roo.addon.layers.repository.mongo;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link MongoEntityAnnotationValues}
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class MongoEntityAnnotationValuesTest extends
        AnnotationValuesTestCase<RooMongoEntity, MongoEntityAnnotationValues> {

    @Override
    protected Class<RooMongoEntity> getAnnotationClass() {
        return RooMongoEntity.class;
    }

    @Override
    protected Class<MongoEntityAnnotationValues> getValuesClass() {
        return MongoEntityAnnotationValues.class;
    }
}
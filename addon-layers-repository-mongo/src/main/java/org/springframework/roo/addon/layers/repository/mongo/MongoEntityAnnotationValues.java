package org.springframework.roo.addon.layers.repository.mongo;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooMongoRepository} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class MongoEntityAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private JavaType identifierType = JdkJavaType.BIG_INTEGER;

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata the metadata to parse (required)
     */
    public MongoEntityAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_MONGO_ENTITY);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    /**
     * Returns the Identifier type for this domain entity
     * 
     * @return a non-<code>null</code> type
     */
    public JavaType getIdentifierType() {
        return identifierType;
    }
}

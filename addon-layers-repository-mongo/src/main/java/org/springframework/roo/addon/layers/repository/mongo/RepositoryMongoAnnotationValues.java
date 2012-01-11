package org.springframework.roo.addon.layers.repository.mongo;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooMongoRepository} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class RepositoryMongoAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private JavaType domainType;

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata the metadata to parse (required)
     */
    public RepositoryMongoAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_REPOSITORY_MONGO);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    /**
     * Returns the domain type managed by the annotated repository
     * 
     * @return a non-<code>null</code> type
     */
    public JavaType getDomainType() {
        return domainType;
    }
}

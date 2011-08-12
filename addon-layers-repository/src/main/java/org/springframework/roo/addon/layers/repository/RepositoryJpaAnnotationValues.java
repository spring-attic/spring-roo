package org.springframework.roo.addon.layers.repository;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * The values of a {@link RooRepositoryJpa} annotation.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryJpaAnnotationValues extends AbstractAnnotationValues {

	// Fields
	@AutoPopulate private JavaType domainType;
	
	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata the metadata to parse (required)
	 */
	public RepositoryJpaAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooRepositoryJpa.class.getName()));
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

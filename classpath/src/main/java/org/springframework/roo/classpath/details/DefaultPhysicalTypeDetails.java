package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.model.AbstractCustomDataAccessorProvider;
import org.springframework.roo.model.CustomDataImpl;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Simple implementation of {@link PhysicalTypeDetails} that is suitable for {@link PhysicalTypeCategory#OTHER}
 * or sub-classing by category-specific implementations.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultPhysicalTypeDetails extends AbstractCustomDataAccessorProvider implements PhysicalTypeDetails {

	// Fields
	private final PhysicalTypeCategory physicalTypeCategory;
	private final JavaType javaType;

	public DefaultPhysicalTypeDetails(final PhysicalTypeCategory physicalTypeCategory, final JavaType javaType) {
		super(CustomDataImpl.NONE);
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(physicalTypeCategory, "Physical type category required");
		this.javaType = javaType;
		this.physicalTypeCategory = physicalTypeCategory;
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return physicalTypeCategory;
	}

	public JavaType getName() {
		return getType();
	}
	
	public JavaType getType() {
		return javaType;
	}
}

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
 *
 */
public final class DefaultPhysicalTypeDetails extends AbstractCustomDataAccessorProvider implements PhysicalTypeDetails {

	private PhysicalTypeCategory physicalTypeCategory;
	private JavaType name;
	
	public DefaultPhysicalTypeDetails(PhysicalTypeCategory physicalTypeCategory, JavaType name) {
		super(CustomDataImpl.NONE);
		Assert.notNull(physicalTypeCategory, "Physical type category required");
		Assert.notNull(name, "Name required");
		this.physicalTypeCategory = physicalTypeCategory;
		this.name = name;
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return physicalTypeCategory;
	}

	public JavaType getName() {
		return name;
	}

}

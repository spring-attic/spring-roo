package org.springframework.roo.addon.web.mvc.controller;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Comtainer for {@link BeanInfoMetadata} and {@link EntityMetadata} for a given {@link JavaType}.
 * 
 * @author Stefan Schmidt	
 * @since 1.1.2
 *
 */
public class JavaTypeMetadataHolder {

	private JavaType type;
	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private boolean isEnumType;

	public JavaTypeMetadataHolder(JavaType type, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, boolean isEnumType) {
		Assert.notNull(type, "JavaType required");
		Assert.notNull(beanInfoMetadata, "BeanInfo metata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		this.type = type;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.isEnumType = isEnumType;
	}

	public JavaType getType() {
		return type;
	}

	public BeanInfoMetadata getBeanInfoMetadata() {
		return beanInfoMetadata;
	}

	public EntityMetadata getEntityMetadata() {
		return entityMetadata;
	}
	
	public boolean isEnumType() {
		return isEnumType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaTypeMetadataHolder other = (JavaTypeMetadataHolder) obj;

		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}

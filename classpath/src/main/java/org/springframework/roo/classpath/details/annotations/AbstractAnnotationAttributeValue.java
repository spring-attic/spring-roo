package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Abstract base class for annotation attribute values.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractAnnotationAttributeValue<T extends Object> implements AnnotationAttributeValue<T> {
	private JavaSymbolName name;

	public AbstractAnnotationAttributeValue(JavaSymbolName name) {
		Assert.notNull(name, "Annotation attribute name required");
		this.name = name;
	}

	public JavaSymbolName getName() {
		return name;
	}

	@Override 
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getValue() == null) ? 0 : getValue().hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override 
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AbstractAnnotationAttributeValue<?>)) {
			return false;
		}
		AbstractAnnotationAttributeValue<?> other = (AbstractAnnotationAttributeValue<?>) obj;
		if (getValue() == null) {
			if (other.getValue() != null) {
				return false;
			}
		} else if (!getValue().equals(other.getValue())) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}

package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

public abstract class AbstractAnnotationAttributeValue<T extends Object> implements AnnotationAttributeValue<T> {

	private JavaSymbolName name;
	
	public AbstractAnnotationAttributeValue(JavaSymbolName name) {
		Assert.notNull(name, "Annotation attribute name required");
		this.name = name;
	}

	public JavaSymbolName getName() {
		return name;
	}
	
}

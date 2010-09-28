package org.springframework.roo.classpath.details.annotations;

import java.util.Collections;
import java.util.List;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents an array of annotation attribute values.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 * @param <Y> the contents of the array
 */
public class ArrayAttributeValue<Y extends AnnotationAttributeValue<? extends Object>> extends AbstractAnnotationAttributeValue<List<Y>> {
	private List<Y> value;
	
	public ArrayAttributeValue(JavaSymbolName name, List<Y> value) {
		super(name);
		Assert.notNull(value, "Value required");
		this.value = value;
	}

	public List<Y> getValue() {
		return Collections.unmodifiableList(value);
	}

	public String toString() {
		return getName() + " -> {" + StringUtils.collectionToCommaDelimitedString(value) + "}";
	}
}

package org.springframework.roo.addon.finder;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Token which represents a field in an JPA Entity
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
public class FieldToken implements Token, Comparable<FieldToken> {
	private FieldMetadata field;	
	private JavaSymbolName fieldName;

	public FieldToken(FieldMetadata field) {
		Assert.notNull(field, "FieldMetadata required");
		this.field = field;
		this.fieldName = field.getFieldName();
	}
	
	public String getValue() {
		return field.getFieldName().getSymbolNameCapitalisedFirstLetter();
	}
	
	public JavaSymbolName getFieldName() {
		return fieldName;
	}

	public void setFieldName(JavaSymbolName fieldName) {
		this.fieldName = fieldName;
	}

	public FieldMetadata getField() {
		return field;
	}
	
	public int compareTo(FieldToken o) {
		int l = o.getValue().length() - this.getValue().length();
		return l == 0 ? -1 : l;
	}
}

/**
 * 
 */
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
 *
 */
public class FieldToken implements Token {
	
	private FieldMetadata field;	
	
	private JavaSymbolName fieldName;

	public FieldToken(FieldMetadata field) {
		super();
		Assert.notNull(field, "FieldMetadata required");
		this.field = field;
		this.fieldName = field.getFieldName();
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

}

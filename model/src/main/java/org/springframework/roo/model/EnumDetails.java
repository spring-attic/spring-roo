package org.springframework.roo.model;

import org.springframework.roo.support.util.Assert;

/**
 * Immutable representation of an enumeration.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class EnumDetails {
	private JavaType type;
	private JavaSymbolName field;
	
	public EnumDetails(JavaType type, JavaSymbolName field) {
		Assert.notNull(type, "Type required");
		Assert.notNull(field, "Field required");
		this.type = type;
		this.field = field;
	}

	public JavaType getType() {
		return type;
	}

	public JavaSymbolName getField() {
		return field;
	}
	
	public String toString() {
		return type.getFullyQualifiedTypeName() + "." + field.getSymbolName();
	}
	
}
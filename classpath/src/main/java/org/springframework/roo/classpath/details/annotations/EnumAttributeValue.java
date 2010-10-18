package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Represents an enumeration annotation attribute value.
 * 
 * <p>
 * Source code parsers should treat any non-quoted string NOT ending in ".class" as an enumeration,
 * using the segment appearing after the final period in the string as the field name. Anything to the
 * left of that final period is treated as representing the enumeration type, and normal package resolution
 * techniques should be used to resolve the enumeration type.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class EnumAttributeValue extends AbstractAnnotationAttributeValue<EnumDetails> {
	private EnumDetails value;
	
	public EnumAttributeValue(JavaSymbolName name, EnumDetails value) {
		super(name);
		Assert.notNull(value, "Value required");
		this.value = value;
	}

	@SuppressWarnings("all")
	public Enum<?> getAsEnum() throws ClassNotFoundException {
		Class<?> enumType = getClass().getClassLoader().loadClass(this.value.getType().getFullyQualifiedTypeName());
		Assert.isTrue(enumType.isEnum(), "Should have obtained an Enum but failed for type '" + enumType.getName() + "'");
		String name = this.value.getField().getSymbolName();
		return Enum.valueOf((Class<? extends Enum>) enumType, name);
	}
	
	public EnumDetails getValue() {
		return value;
	}

	public String toString() {
		return getName() + " -> " + value.toString();
	}
}

package org.springframework.roo.classpath.details.annotations;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents an enumeration annotation attribute value.
 * <p>
 * Source code parsers should treat any non-quoted string NOT ending in ".class"
 * as an enumeration, using the segment appearing after the final period in the
 * string as the field name. Anything to the left of that final period is
 * treated as representing the enumeration type, and normal package resolution
 * techniques should be used to resolve the enumeration type.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class EnumAttributeValue extends
        AbstractAnnotationAttributeValue<EnumDetails> {
    private final EnumDetails value;

    public EnumAttributeValue(final JavaSymbolName name, final EnumDetails value) {
        super(name);
        Validate.notNull(value, "Value required");
        this.value = value;
    }

    @SuppressWarnings("all")
    public Enum<?> getAsEnum() throws ClassNotFoundException {
        final Class<?> enumType = getClass().getClassLoader().loadClass(
                value.getType().getFullyQualifiedTypeName());
        Validate.isTrue(enumType.isEnum(),
                "Should have obtained an Enum but failed for type '%s'",
                enumType.getName());
        final String name = value.getField().getSymbolName();
        return Enum.valueOf((Class<? extends Enum>) enumType, name);
    }

    public EnumDetails getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + value.toString();
    }
}

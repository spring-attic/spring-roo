package org.springframework.roo.model;

import org.apache.commons.lang3.Validate;

/**
 * Immutable representation of an enumeration.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class EnumDetails {
    private final JavaSymbolName field;
    private final JavaType type;

    public EnumDetails(final JavaType type, final JavaSymbolName field) {
        Validate.notNull(type, "Type required");
        Validate.notNull(field, "Field required");
        this.type = type;
        this.field = field;
    }

    public JavaSymbolName getField() {
        return field;
    }

    public JavaType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.getFullyQualifiedTypeName() + "." + field.getSymbolName();
    }

}
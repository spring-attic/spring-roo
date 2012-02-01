package org.springframework.roo.addon.finder;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Token which represents a field in an JPA Entity
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
public class FieldToken implements Token, Comparable<FieldToken> {

    private final FieldMetadata field;
    private JavaSymbolName fieldName;

    /**
     * Constructor
     * 
     * @param field
     */
    public FieldToken(final FieldMetadata field) {
        Validate.notNull(field, "FieldMetadata required");
        this.field = field;
        fieldName = field.getFieldName();
    }

    public int compareTo(final FieldToken o) {
        final int l = o.getValue().length() - getValue().length();
        return l == 0 ? -1 : l;
    }

    public FieldMetadata getField() {
        return field;
    }

    public JavaSymbolName getFieldName() {
        return fieldName;
    }

    public String getValue() {
        return field.getFieldName().getSymbolNameCapitalisedFirstLetter();
    }

    public void setFieldName(final JavaSymbolName fieldName) {
        this.fieldName = fieldName;
    }
}

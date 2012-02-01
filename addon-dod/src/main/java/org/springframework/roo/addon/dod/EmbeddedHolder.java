package org.springframework.roo.addon.dod;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Holder for embedded attributes
 * 
 * @author Greg Turnquist
 * @since 1.2.0
 */
public class EmbeddedHolder {

    private final FieldMetadata embeddedField;
    private final List<FieldMetadata> fields;

    public EmbeddedHolder(final FieldMetadata embeddedField,
            final List<FieldMetadata> fields) {
        Validate.notNull(embeddedField, "Identifier type required");
        Validate.notNull(fields, "Fields for "
                + embeddedField.getFieldType().getFullyQualifiedTypeName()
                + " required");
        this.embeddedField = embeddedField;
        this.fields = fields;
    }

    public FieldMetadata getEmbeddedField() {
        return embeddedField;
    }

    public JavaSymbolName getEmbeddedMutatorMethodName() {
        return BeanInfoUtils.getMutatorMethodName(embeddedField);
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }
}

package org.springframework.roo.addon.dbre;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Holder for identifier and embedded identifier fields
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class IdentifierHolder {

    // Fields
    private final FieldMetadata identifierField;
    private final boolean embeddedIdField;
    private final List<FieldMetadata> embeddedIdentifierFields;

    public IdentifierHolder(final FieldMetadata identifierField,
            final boolean embeddedIdField,
            final List<FieldMetadata> embeddedIdentifierFields) {
        Assert.notNull(identifierField, "Identifier field required");
        Assert.notNull(embeddedIdentifierFields, "Fields for "
                + identifierField.getFieldType().getFullyQualifiedTypeName()
                + " required");
        this.identifierField = identifierField;
        this.embeddedIdField = embeddedIdField;
        this.embeddedIdentifierFields = embeddedIdentifierFields;
    }

    public FieldMetadata getIdentifierField() {
        return identifierField;
    }

    public boolean isEmbeddedIdField() {
        return embeddedIdField;
    }

    public List<FieldMetadata> getEmbeddedIdentifierFields() {
        return embeddedIdentifierFields;
    }
}
